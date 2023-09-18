package de.apps_roters.tesla_for_kotlin.web

import com.google.gson.Gson
import org.apache.logging.log4j.LogManager
import org.json.JSONException
import org.json.JSONObject

class RestRequest {
    private val webRequest = WebRequest()

    @Throws(Exception::class)
    operator fun get(getURL: String?, headers: Map<String?, String?>?): String? {
        return webRequest[getURL, headers]
    }

    @Throws(Exception::class)
    fun post(postURL: String?, body: String?, headers: Map<String?, String?>?): String? {
        return webRequest.post(postURL, body, headers)
    }

    fun setBearer(b: String?) {
        webRequest.setBearer(b)
    }

    /**
     * Get a JSONObject representing the content retrieved from the REST endpoint
     *
     * @param method Method to call at endpoint (assumed to be whatever comes after
     * $BASEURL/rest/api/2/)
     * @return JSONObject from retrieved content. Note that if an array is
     * retrieved, a JSONObject of the format {d: content} will be returned.
     */
    @Throws(Exception::class)
    fun <T : Any?> getJSON(getURL: String?, clazz: Class<T>?): T {
        val headers: MutableMap<String, String> = HashMap()
        headers["Accept"] = "application/json, text/plain, */*"
        headers["Content-Type"] = "application/json"
        val gson = Gson()
        val jsonString = webRequest[getURL, null]
        return gson.fromJson(jsonString, clazz)
    }

    /**
     * Convenience method to return JSONObject from GET request.
     *
     * @see get
     *
     * @param getUrl URL to request
     * @return JSONObject from reponse. Null if failure in request or non-JSON
     * response received.
     */
    @Throws(Exception::class)
    fun getJSON(
        getUrl: String?,
        properties: Map<String?, String?>?
    ): JSONObject {
        val responseText = webRequest[getUrl, properties]
        return JSONObject(responseText)
    }

    /**
     * Convert response data string to JSONObject
     *
     * @param json Response data string (assumed to be JSON)
     * @return JSONObject or null if string could not be parsed to JSON.
     */
    private fun processResponseData(json: String?): JSONObject? {
        return if (json != null && (json.startsWith("{") || json.startsWith("["))) {
            try {
                if (json.startsWith("[")) {
                    JSONObject("{d:$json}")
                } else JSONObject(json)
            } catch (e: JSONException) {
                logger.error("Exception while parsing REST JSON response: {}", e)
                throw e
            }
        } else null
    }

    @Throws(Exception::class)
    fun <T : Any?> postJson(postURL: String?, body: Any?, clazz: Class<T>?): T {
        val headers: MutableMap<String?, String?> = HashMap()
        headers["Accept"] = "application/json, text/plain, */*"
        headers["Content-Type"] = "application/json"
        val gson = Gson()
        val jsonString = webRequest.post(postURL, gson.toJson(body), headers)
        return gson.fromJson(jsonString, clazz)
    }

    companion object {
        private val logger = LogManager.getLogger(RestRequest::class.java)
    }
}
