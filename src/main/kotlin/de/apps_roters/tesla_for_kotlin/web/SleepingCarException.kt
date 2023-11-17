package de.apps_roters.tesla_for_kotlin.web

class SleepingCarException internal constructor(cause: Exception? = null) : RuntimeException(cause) {
    companion object {
        /**
         *
         */
        private const val serialVersionUID = 3455802235331219949L
    }
}
