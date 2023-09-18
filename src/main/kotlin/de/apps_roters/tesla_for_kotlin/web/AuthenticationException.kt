package de.apps_roters.tesla_for_kotlin.web

class AuthenticationException internal constructor(cause: Exception?) : RuntimeException(cause) {
    companion object {
        /**
         *
         */
        private const val serialVersionUID = -7376240560681935086L
    }
}
