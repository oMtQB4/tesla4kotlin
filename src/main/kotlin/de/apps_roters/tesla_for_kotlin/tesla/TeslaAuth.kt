package de.apps_roters.tesla_for_kotlin.tesla

import de.apps_roters.tesla_for_kotlin.web.RestRequest
import org.apache.logging.log4j.LogManager
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*
import kotlin.collections.HashMap


class TeslaAuth(restRequest: RestRequest, teslaConfiguration: TeslaConfiguration) {
    private val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890"

    private var codeVerifier: String? = null
    private var codeChallenge: String? = null
    private var state: String? = null
    private var accessToken: String?
    private var refreshToken: String?
    private val restRequest: RestRequest
    private val teslaConfiguration: TeslaConfiguration

    init {
        this.restRequest = restRequest
        this.teslaConfiguration = teslaConfiguration
        accessToken = teslaConfiguration.readAccessToken()
        refreshToken = teslaConfiguration.readRefreshToken()
        if (accessToken == null || refreshToken == null) {
            logger.fatal("Tesla API access token and/or refresh token missing, Exiting.")
            System.exit(1)
        }
        restRequest.setBearer(accessToken)
    }

    private fun createRandomString(length: Int): String {
        var result = ""
        for (i in 0 until length) {
            val ch = chars[(Math.random() * chars.length).toInt()]
            result += ch
        }
        return result
    }

    @Throws(NoSuchAlgorithmException::class)
    fun createVerifier() {
        codeVerifier = createRandomString(86)
        val digest = MessageDigest.getInstance("SHA-256")
        val encodedhash = digest.digest(codeVerifier!!.toByteArray(StandardCharsets.UTF_8))
        codeChallenge = Base64.getEncoder().encodeToString(encodedhash)
        /// url-encoding the base64 string. It is intentional to remove the "=" chars
        codeChallenge =
            codeChallenge!!.replace("\\+".toRegex(), "-").replace("/".toRegex(), "_").replace("=".toRegex(), "")
        state = createRandomString(10)

        // logger.info("Codeverifier is " + codeVerifier + ", codeChallenge is " +
        // codeChallenge + ", state is " + state);
    }

    @Throws(UnsupportedEncodingException::class)
    fun prepareAuthUrl(email: String?): String {
        var query = ""
        query += "client_id=ownerapi"
        query += "&code_challenge=" + URLEncoder.encode(codeChallenge, StandardCharsets.UTF_8.toString())
        query += "&code_challenge_method=S256"
        query += ("&redirect_uri=" // +"https://auth.tesla.com/void/calllback";
                + URLEncoder.encode("https://auth.tesla.com/void/callback", StandardCharsets.UTF_8.toString()))
        query += "&response_type=code"
        query += "&scope=" + URLEncoder.encode("openid email offline_access", StandardCharsets.UTF_8.toString())
        query += "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8.toString())
        if (email != null) query += "&login_hint=" + URLEncoder.encode(email, StandardCharsets.UTF_8.toString())
        return "https://auth.tesla.com/oauth2/v3/authorize?$query"
    }

    @Throws(Exception::class)
    fun authorize(email: String?): Map<String, String> {
        val url = prepareAuthUrl(email)
        val result = restRequest.get(url, null)
        // logger.info("authorize returned " + result);
        val res: MutableMap<String, String> = HashMap()
        var idx = 0
        while (true) {
            idx = result!!.indexOf("<input type=\"hidden\" ", idx)
            if (idx < 0) break
            val idx2 = result.indexOf("/>", idx + 6)
            if (idx2 > idx) {
                val field = result.substring(idx + 21, idx2 - 1)
                // logger.info("found " + idx + " and " + idx2 + ": " + field);
                val fields = field.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                res[fields[0].substring(6, fields[0].length - 1)] = fields[1].substring(7, fields[1].length - 1)
            }
            ++idx
        }
        return res
    }

    @Throws(Exception::class)
    fun postAuthorize(email: String?, password: String?, formParams: Map<String?, String?>) {
        var query = ""
        query += "client_id=ownerapi"
        query += "&code_challenge=" + URLEncoder.encode(codeChallenge, StandardCharsets.UTF_8.toString())
        query += "&code_challenge_method=S256"
        query += ("&redirect_uri=" // +"https://auth.tesla.com/void/calllback";
                + URLEncoder.encode("https://auth.tesla.com/void/callback", StandardCharsets.UTF_8.toString()))
        query += "&response_type=code"
        query += "&scope=" + URLEncoder.encode("openid email offline_access", StandardCharsets.UTF_8.toString())
        query += "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8.toString())
        var body = ""
        body += "identity=" + URLEncoder.encode(email, StandardCharsets.UTF_8.toString())
        // body += "&credential=" + URLEncoder.encode(password,
        // StandardCharsets.UTF_8.toString());
        for ((key, value) in formParams) {
            body += ("&" + URLEncoder.encode(key, StandardCharsets.UTF_8.toString()) + "="
                    + URLEncoder.encode(value, StandardCharsets.UTF_8.toString()))
        }
        // example from Firefox when executing the get-request from the previous method:
        // _csrf=0ERKbSv6-bypXXrIvUPk5CU463Cb7hvIX3XQ&_phase=identity&cancel=&transaction_id=mDjbjsCV&correlation_id=08d8c750-ffef-4366-b381-8e33cc7668c5&identity=tesla%40mschwartz.eu

        // Post2: POST
        // https://auth.tesla.com/oauth2/v3/authorize?client_id=ownerapi&code_challenge=4FKodQTbC6nf1/krK4tRL87Ztqb+fzqd86FpabyTLvQ=&code_challenge_method=S256&redirect_uri=https://auth.tesla.com/void/callback&response_type=code&scope=openid
        // email offline_access&state=YWhia1ZQRlVFZ0VtYzJMbw==
        // Result:
        // _csrf=dbWPcU0k-i0CTX63ewFf3KrHWh6M8gh1C2Z0&_phase=authenticate&_process=1&cancel=&transaction_id=mDjbjsCV&change_identity=&identity=tesla%40mschwartz.eu&correlation_id=08d8c750-ffef-4366-b381-8e33cc7668c5&fingerPrint=%7B%22auth_method%22%3A%22email-login%22%2C%22devicehash%22%3A%22ac638e0a5ba53745eab48b8b556ddd18%22%2C%22client_id%22%3A%22ownerapi%22%2C%22hardware_concurrency%22%3A8%2C%22screen_resolution%22%3A%5B2560%2C1440%5D%2C%22audio%22%3A35.7383295930922%2C%22touch_support%22%3A%226690a7caa6588891494df1e64b3d185b%22%2C%22web_gl%22%3A%22WebGL+1.0%22%2C%22browser_plugins%22%3A%2273ddd9a85fc01dd86982e0a967643420%22%2C%22browser_canvas%22%3A%225d895febb9694d1dd681519a3b5bc80d%22%2C%22browser_font%22%3A%22bb0fb9d455dd87dd8df801bb296959a8%22%7D&credential=pIEbGg9x73wuDOZCAVqn
        logger.info("POST https://auth.tesla.com/oauth2/v3/authorize?$query")
        logger.info(body)
        val result = restRequest.post("https://auth.tesla.com/oauth2/v3/authorize?$query", body, null)
        logger.info(result)
    }

    @Throws(Exception::class)
    fun retrieveTokens(code: String): TokenResponse {
        val tokenRequest: TokenRequest =
            TokenRequest(code, codeVerifier!!)

//		logger.info(response.toString());
        return restRequest.postJson<TokenResponse>(
            "https://auth.tesla.com/oauth2/v3/token", tokenRequest,
            TokenResponse::class.java
        )
    }

    /**
     * Refresh the access and refresh tokens for Tesla API access
     *
     * @throws Exception
     */
    @Throws(Exception::class)
    fun refreshTokens() {
        assert(refreshToken != null)
        val refreshTokenRequest = RefreshTokenRequest(refreshToken!!)
        val response: TokenResponse = restRequest.postJson<TokenResponse>(
            "https://auth.tesla.com/oauth2/v3/token", refreshTokenRequest,
            TokenResponse::class.java
        )
        if ((response.refresh_token != null) and (response.access_token != null)) {
            accessToken = response.access_token
            refreshToken = response.refresh_token
            restRequest.setBearer(accessToken)
            teslaConfiguration.updateTokens(accessToken, refreshToken)
        } else {
            logger.error("Exception while handling new access token for Tesla Owner API: Null new access token")
        }
    }

    /////////////////////////////////////////////////////////////////////////
    inner class RefreshTokenRequest internal constructor(var refresh_token: String) {
        var grant_type = "refresh_token"
        var client_id = "ownerapi"
        var scope = "openid email offline_access"
    }

    /////////////////////////////////////////////////////////////////////////
    inner class TokenRequest internal constructor(var code: String, var code_verifier: String) {
        var grant_type = "authorization_code"
        var client_id = "ownerapi"
        var redirect_uri = "https://auth.tesla.com/void/callback"
    }

    /////////////////////////////////////////////////////////////////////////
    inner class TokenResponse {
        var access_token: String? = null
        var refresh_token: String? = null
        var expires_in: String? = null
        var state: String? = null
        var token_type: String? = null
        var id_token: String? = null
        override fun toString(): String {
            return ("TokenResponse [access_token=" + access_token + ", refresh_token=" + refresh_token + ", expires_in="
                    + expires_in + ", state=" + state + ", token_type=" + token_type + "]")
        }
    }

    companion object {
        private val logger = LogManager.getLogger(TeslaAuth::class.java)
    }
}