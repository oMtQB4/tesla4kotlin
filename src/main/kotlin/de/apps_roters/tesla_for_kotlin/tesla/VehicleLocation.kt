package de.apps_roters.tesla_for_kotlin.tesla

import java.time.Instant
import kotlin.math.*

class VehicleLocation {
    var latitude: Double
        private set
    var longitude: Double
        private set
    var speed = 0.0
        private set
    var heading = 0.0
        private set
    var minutesToArrival = 0.0
        private set
    var timestamp: Int
        private set
    var isGoingHome = false
        private set
    var isGoingAwayFromHome = false
        private set

    constructor(lat: Double, lon: Double) {
        latitude = lat
        longitude = lon
        timestamp = floor((Instant.now().toEpochMilli() / 1000).toDouble()).toInt()
    }

    // Assuming received timestamp is in milliseconds. We convert it to seconds.
    internal constructor(lat: Double, lon: Double, spd: Double, head: Double, ts: Double) {
        latitude = lat
        longitude = lon
        speed = spd
        heading = head
        timestamp = floor(ts / 1000).toInt()
    }

    // Convenience method to determine distance between two locations without getting lat/long first
    fun distanceFrom(location2: VehicleLocation): Double {
        return distanceFrom(location2.latitude, location2.longitude)
    }

    // Determine the approximate distance (in miles) between two points on a sphere (the Earth)	
    fun distanceFrom(lat2: Double, lon2: Double): Double {
        var lat2 = lat2
        var lat1 = latitude
        val lon1 = longitude

        // Allow from some variance in GPS readings here, so that we can move a certain distance but
        // still be 0 miles apart. If my math is right this means a single coordinate covers an area
        // about 291' long x 231' feet wide.
        if (abs(lat1 - lat2) <= 0.0008 && abs(lon1 - lon2) <= 0.0008) {
            return 0.0
        }

        // Distance between latitudes and longitudes
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        // Convert to radians
        lat1 = Math.toRadians(lat1)
        lat2 = Math.toRadians(lat2)

        // Apply formulae
        val a: Double = sin(dLat / 2).pow(2.0) + sin(dLon / 2).pow(2.0) * cos(lat1) * cos(lat2)
        val rad = 6371.0
        val c = 2 * asin(sqrt(a))
        return rad * c * 0.621371 // Convert to miles
    }

    val arrivalTimeSeconds: Int
        get() = floor(timestamp.toDouble() + floor(minutesToArrival * 60)).toInt()
    val timestampMillis: Double
        get() = timestamp.toDouble() * 1000

    fun setGoingAwayFromHome(f: Boolean): VehicleLocation {
        isGoingAwayFromHome = f
        return this
    }

    fun setGoingHome(f: Boolean): VehicleLocation {
        isGoingHome = f
        return this
    }

    fun setHeading(head: Double): VehicleLocation {
        heading = head
        return this
    }

    fun setLatitude(lat: Double): VehicleLocation {
        latitude = lat
        return this
    }

    fun setLongitude(lon: Double): VehicleLocation {
        longitude = lon
        return this
    }

    fun setMinutesToArrival(m: Double): VehicleLocation {
        minutesToArrival = m
        return this
    }

    fun setSpeed(spd: Double): VehicleLocation {
        speed = spd
        return this
    }

    fun setTimestamp(ts: Double): VehicleLocation {
        timestamp = floor(ts / 1000).toInt()
        return this
    }

    override fun toString(): String {
        return "Lat: $latitude, Long: $longitude, Heading: $heading, Speed: $speed, Timestamp: $timestamp"
    }
}
