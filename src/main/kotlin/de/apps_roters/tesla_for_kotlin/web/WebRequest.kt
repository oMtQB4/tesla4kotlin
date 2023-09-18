package de.apps_roters.tesla_for_kotlin.web

import org.apache.logging.log4j.LogManager
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.StringWriter
import java.net.*

class WebRequest {
    protected var bearer: String? = null
    private set

    init {
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL)
        CookieHandler.setDefault(cookieManager)
    }
    /**
     * Set the cookies to be used for requests
     *
     * @param cookies list of Cookies
     */
    //	public void setCookies(ArrayList<Cookie> cookies) {
    //		this.cookies = cookies;
    //	}
    /**
     * Set the bearer token to be used for requests
     *
     * @param b Bearer token string
     */
    fun setBearer(b: String?) {
        bearer = b
    }

    private fun enrichConnection(conn: HttpURLConnection?, headers: Map<String?, String?>?) {
        conn!!.setReadTimeout(30000)
        conn.setConnectTimeout(15000)
        if (headers != null) {
            for ((key, value) in headers) {
                conn.setRequestProperty(key, value)
            }
        }
        if (bearer != null) {
            conn.setRequestProperty("Authorization", "Bearer $bearer")
        }
        if (cookieManager.cookieStore.cookies.size > 0) {
            conn.setRequestProperty("Cookie", join("; ", cookieManager.cookieStore.cookies))
        }

//		Map<String, List<String>> headerFields = conn.getRequestProperties();
//		for (Map.Entry<String, List<String>> entry : headerFields.entrySet()) {
//			for (String value : entry.getValue()) {
//				logger.info("--> " + entry.getKey() + ": " + value);
//			}
//		}
    }

    /**
     * Perform a GET request on a provided URL
     *
     * @param getURL URL to request
     * @return String of content retrieved from the URL
     */
    @Throws(Exception::class)
    operator fun get(getURL: String?, headers: Map<String?, String?>?): String {
        val url: URL
        val returnData = StringWriter()
        var conn: HttpURLConnection? = null
        var ir: InputStreamReader? = null
        logger.debug("GET request to: {}", getURL)
        try {
            url = URL(getURL)
            conn = url.openConnection() as HttpURLConnection
            conn!!.setRequestProperty("Accept", "*/*")
            conn.setRequestProperty("User-Agent", "TeslaCharging")
            //			conn.setRequestProperty("Accept", "application/json, text/plain, */*");
//			conn.setRequestProperty("x-tesla-user-agent", "");
//			conn.setRequestProperty("X-Requested-With", "com.teslamotors.tesla");
            enrichConnection(conn, headers)
            conn.connect()

//			Map<String, List<String>> headerFields = conn.getHeaderFields();
//			for (Map.Entry<String, List<String>> entry : headerFields.entrySet()) {
//				for (String value : entry.getValue()) {
//					logger.info("<-- " + entry.getKey() + ": " + value);
//				}
//			}
            ir = try {
                InputStreamReader(conn.inputStream, "UTF-8")
            } catch (e: IOException) {
                if (e.message!!.contains("401")) throw AuthenticationException(e) else if (e.message!!.contains("408")) // seems the tesla is sleeping
                    throw SleepingCarException(e)
                throw e
            }
            if (ir != null) {
                val buffer = CharArray(1024 * 4)
                var len = 0
                while (ir.read(buffer).also { len = it } != -1) {
                    returnData.write(buffer, 0, len)
                }
            }
            processHeaderCookies(conn)
        } finally {
            if (ir != null) {
                try {
                    ir.close()
                } catch (e: IOException) {
                }
            }
            conn?.disconnect()
        }
        returnData.flush()
        val returnText = returnData.toString()
        returnData.close()
        logger.debug(returnText)
        return returnText
    }
    /**
     * Perform a POST with a body
     *
     * @param postURL URL for POST
     * @param body    String for POST body
     * @return Result of POST
     * @throws Exception When things go wrong
     */
    /**
     * Convenience method to perform a POST that does not require a body.
     *
     * @see post
     * @param postURL URL for POST
     * @return Result of POST
     * @throws Exception When things go wrong
     */
    @JvmOverloads
    @Throws(Exception::class)
    fun post(postURL: String?, body: String? = null, headers: Map<String?, String?>? = null): String {
        val url: URL
        val returnData = StringWriter()
        var conn: HttpURLConnection? = null
        var ir: InputStreamReader? = null
        logger.debug("POST request to: {}", postURL)
        if (body != null) {
            logger.debug("POST body: {}", body)
        }
        try {
            url = URL(postURL)
            conn = url.openConnection() as HttpURLConnection
            conn!!.setRequestMethod("POST")
            conn.setDoOutput(true)
            HttpURLConnection.setFollowRedirects(false)
            //			conn.setRequestProperty("Accept", "application/json, text/plain, */*");
            conn.setRequestProperty("Accept", "*/*")
            //			conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            if (body != null && body.length > 0) {
                conn.setRequestProperty("Content-Length", body.length.toString())
            }
            conn.setRequestProperty("User-Agent", "TeslaCharging")
            //			conn.setRequestProperty("x-tesla-user-agent", "");
//			conn.setRequestProperty("X-Requested-With", "com.teslamotors.tesla");
            enrichConnection(conn, headers)
            conn.connect()
            if (body != null && body.length > 0) {
                var out: OutputStreamWriter? = null
                try {
                    out = OutputStreamWriter(conn.outputStream, "UTF-8")
                    out.write(body)
                    out.flush()
                } finally {
                    out?.close()
                }
            }

//			Map<String, List<String>> headerFields = conn.getHeaderFields();
//			for (Map.Entry<String, List<String>> entry : headerFields.entrySet()) {
//				for (String value : entry.getValue()) {
//					logger.info("<-- " + entry.getKey() + ": " + value);
//				}
//			}
            ir = InputStreamReader(conn.inputStream, "UTF-8")
            if (ir != null) {
                val buffer = CharArray(1024 * 4)
                var len = 0
                while (ir.read(buffer).also { len = it } != -1) {
                    returnData.write(buffer, 0, len)
                }
            }
            processHeaderCookies(conn)
        } finally {
            ir?.close()
            conn?.disconnect()
        }
        returnData.flush()
        val returnText = returnData.toString()
        returnData.close()
        logger.debug(returnText)
        return returnText
    }

    private fun processHeaderCookies(conn: HttpURLConnection?) {
//		Map<String, List<String>> headerFields = conn.getHeaderFields();
//		List<String> headerCookies = headerFields.get("Set-Cookie");
//		if (headerCookies != null) {
//			for (String cookie : headerCookies) {
//				if (cookie.length() == 0)
//					continue;
//				List<HttpCookie> cookies = HttpCookie.parse(cookie);
//				for (HttpCookie c : cookies) {
//					cookieManager.getCookieStore().add(null, c);
//					logger.info("Adding cookie " + c);
//				}
//			}
//		}
    }

    companion object {
        protected var cookieManager = CookieManager()
        private val logger = LogManager.getLogger(WebRequest::class.java)
        private fun join(separator: String, input: List<HttpCookie>?): String {
            if (input == null || input.size <= 0) return ""
            val sb = StringBuilder()
            for (i in input.indices) {
                sb.append(input[i])

                // if not the last item
                if (i != input.size - 1) {
                    sb.append(separator)
                }
            }
            return sb.toString()
        }
    }
}
