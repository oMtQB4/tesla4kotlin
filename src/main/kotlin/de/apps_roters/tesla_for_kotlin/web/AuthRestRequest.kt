package de.apps_roters.tesla_for_kotlin.web

import de.apps_roters.tesla_for_kotlin.tesla.TeslaAuth
import org.apache.logging.log4j.LogManager

class AuthRestRequest(private val restRequest: RestRequest, teslaAuth: TeslaAuth) {
    private val teslaAuth: TeslaAuth

    init {
        this.teslaAuth = teslaAuth
    }

    @Throws(Exception::class)
    operator fun get(getURL: String?, headers: Map<String?, String?>?): String? {
        return restRequest[getURL, headers]
    }

    @Throws(Exception::class)
    fun post(postURL: String?, body: String?, headers: Map<String?, String?>?): String? {
        return restRequest.post(postURL, body, headers)
    }

    fun setBearer(b: String?) {
        restRequest.setBearer(b)
    }

    @Throws(Exception::class)
    fun <T : Any?> getJSON(getURL: String?, clazz: Class<T>?): T? {
        return try {
            restRequest.getJSON(getURL, clazz)
        } catch (e: AuthenticationException) {
            teslaAuth.refreshTokens()
            restRequest.getJSON(getURL, clazz)
        }
    }

    @Throws(Exception::class)
    fun <T : Any?> postJson(postURL: String?, body: Any?, clazz: Class<T>?): T? {
        return try {
            restRequest.postJson(postURL, body, clazz)
        } catch (e: AuthenticationException) {
            teslaAuth.refreshTokens()
            restRequest.postJson(postURL, body, clazz)
        }
    }

    companion object {
        val logger = LogManager.getLogger(AuthRestRequest::class.java)
    }
}
