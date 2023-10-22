package de.apps_roters.tesla_for_kotlin.tesla

import de.apps_roters.tesla_for_kotlin.web.AuthRestRequest
import de.apps_roters.tesla_for_kotlin.web.SleepingCarException
import org.apache.commons.collections4.queue.CircularFifoQueue
import org.apache.logging.log4j.LogManager


class TeslaVehicle(authRestRequest: AuthRestRequest, teslaConfiguration: TeslaConfiguration) {
    private val authRestRequest: AuthRestRequest
    var id: String?
    private var vin: String?
    private var displayName: String?
    private var homeLatitude: Double? = null
    private var homeLongitude: Double? = null
    private var lastHome: VehicleLocation? = null
    private var lastCache: VehicleData? = null
    private var timestampCache: Long = 0

    // Vehicle location history
    private val vehicleLocationHistory: CircularFifoQueue<VehicleLocation> = CircularFifoQueue<VehicleLocation>(250)

    init {
        this.authRestRequest = authRestRequest
        vin = teslaConfiguration.readVin()
        id = teslaConfiguration.readID_S()
        displayName = teslaConfiguration.readDisplayName()
        try {
            homeLatitude = teslaConfiguration.readHomeLatitude()
            homeLongitude = teslaConfiguration.readHomeLongitude()
        } catch (ex: Exception) {
            // logger.fatal("Unable to read Home latitude/longitude, Exiting.");
            // System.exit(1);
        }
        if (vin == null || id == null || displayName == null) getVehicleMatchForVIN(teslaConfiguration)
        if (homeLatitude == null || homeLongitude == null) {
            val driveState = vehicleDriveState
            if (driveState != null && driveState.latitude != null && driveState.longitude != null) {
                homeLatitude = driveState.latitude
                homeLongitude = driveState.longitude
                teslaConfiguration.updateHomeLocation(homeLatitude!!, homeLongitude!!)
            }
        }
    }

    /**
     * Get a JSON object with the vehicle details matching the configured VIN. If no
     * VIN is configured and the account only has one Tesla associated with it,
     * return that vehicle.
     *
     * @return JSON object with vehicle details
     * @throws Exception
     */
    @Throws(Exception::class)
    fun getVehicleMatchForVIN(teslaConfiguration: TeslaConfiguration): Vehicle? {
        var vehicleMatch: Vehicle? = null
        val responseJSON = authRestRequest.getJSON<VehicleList>(
            TeslaConfiguration.Companion.apiBase + "/api/1/vehicles",
            VehicleList::class.java
        )
        if (responseJSON != null) {
            if (responseJSON.response!!.size > 0) {

                // When we only have one vehicle and no VIN, we use the identifier of the
                // vehicle we've found.
                if (responseJSON.response!!.size == 1 && (vin == null || vin!!.length == 0)) {
                    val vehicle = responseJSON.response!![0]
                    if (vehicle.id_s != null) {
                        vehicleMatch = vehicle
                    }

                    // With multiple vehicles or a defined VIN, make sure we find the correct
                    // vehicle.
                } else {
                    for (vehicle in responseJSON.response!!) {
                        if (vehicle.id_s != null && vehicle.vin != null) {
                            if (vehicle.vin == vin) {
                                vehicleMatch = vehicle
                                break
                            }
                        }
                    }
                }
            }
        }
        vin = vehicleMatch!!.vin
        id = vehicleMatch.id_s
        if (vehicleMatch.display_name != null) {
            displayName = vehicleMatch.display_name
        }
        teslaConfiguration.updateVin(vin, id, displayName)
        return vehicleMatch
    }

    val homePosition: VehicleLocation
        get() = VehicleLocation(homeLatitude!!, homeLongitude!!)

    @get:Throws(Exception::class)
    val vehicleData: Vehicle?
        /**
         * Get the full vehicle data set
         *
         * @param id ID of the vehicle to use when requesting vehicle data
         * @return Vehicle data response JSON object
         * @throws Exception
         */
        get() {
            if (timestampCache != 0L && System.currentTimeMillis() - timestampCache < 10000) {
                return lastCache!!.response
            }
//            try {
                val vehicleDataResponse = authRestRequest
                    .getJSON<VehicleData>(
                        TeslaConfiguration.Companion.apiBase + "/api/1/vehicles/" + id + "/vehicle_data",
                        VehicleData::class.java
                    )
                if (vehicleDataResponse != null && vehicleDataResponse.response != null) {
                    lastCache = vehicleDataResponse
                    timestampCache = System.currentTimeMillis()
                    return vehicleDataResponse.response
                }
//            } catch (e: SleepingCarException) {
//                val ok = wakeUpVehicle()
//                if (!ok) {
//                    logger.warn("Waking up the car failed")
//                }
//                // repeat once
//                val vehicleDataResponse = authRestRequest
//                    .getJSON<VehicleData>(
//                        TeslaConfiguration.Companion.apiBase + "/api/1/vehicles/" + id + "/vehicle_data",
//                        VehicleData::class.java
//                    )
//                if (vehicleDataResponse != null && vehicleDataResponse.response != null) {
//                    lastCache = vehicleDataResponse
//                    timestampCache = System.currentTimeMillis()
//                    return vehicleDataResponse.response
//                }
//            }
            return null
        }

    /**
     * Wake up the vehicle so we know future commands will work.
     *
     * @param id ID of the vehicle to use in the wake_up request
     * @throws Exception
     */
    @Throws(Exception::class)
    fun wakeUpVehicle(): Boolean {
        logger.debug("Wake up, Tesla {}!", id)
        val wakeResponse = authRestRequest
            .postJson<VehicleData>(
                TeslaConfiguration.Companion.apiBase + "/api/1/vehicles/" + id + "/wake_up",
                null,
                VehicleData::class.java
            )
        if (wakeResponse != null) {
            if (wakeResponse.response != null) {
                val response = wakeResponse.response
                if (response!!.state != null && response.state == "online") {
                    return true
                }
            }
        }
        return false
    }

    @get:Throws(Exception::class)
    private val vehicleState: String?
        /**
         * Get the current state of the vehicle with the provided ID. NOTE! This call
         * won't wake a sleeping Tesla!
         *
         * @param id ID of the vehicle to use when requesting the current state
         * @return String representing vehicle state (online, asleep, offline, waking,
         * unknown)
         * @throws Exception
         */
        private get() {
            val vehicleResponse =
                authRestRequest.getJSON<VehicleData>("api/1/vehicles/$id", VehicleData::class.java)
            if (vehicleResponse != null) {
                val data = vehicleResponse.response
                if (data!!.state != null) {
                    return data.state
                }
            }
            return "unknown"
        }

    @get:Throws(Exception::class)
    val vehicleDriveState: DriveState?
        /**
         * Get the drive state of the vehicle
         *
         * @param id ID of the vehicle to use when requesting drive state
         * @return Drive state response JSON object
         * @throws Exception
         */
        get() {
            val vehicleDataResponse = vehicleData
            return if (vehicleDataResponse != null && vehicleDataResponse.drive_state != null) {
                vehicleDataResponse.drive_state
            } else null
        }

    @get:Throws(Exception::class)
    val vehicleChargeState: ChargeState?
        /**
         * Get the charge state of the vehicle
         *
         * @param id ID of the vehicle to use when requesting charge state
         * @return Charge state response JSON object
         * @throws Exception
         */
        get() {
            val vehicleDataResponse = vehicleData
            return if (vehicleDataResponse != null && vehicleDataResponse.charge_state != null) {
                vehicleDataResponse.charge_state
            } else null
        }

    /**
     * Request current location details from vehicle and store them in our queue,
     * returning the current location details.
     *
     * Make sure to wake up the vehicle
     *
     * @param id ID of the vehicle to use in the drive state request
     * @return VehicleLocation object with location details returned from the Tesla
     * API
     * @throws Exception
     */
    @Throws(Exception::class)
    private fun updateVehicleLocationDetails(): VehicleLocation? {
        val driveStateResponse = vehicleDriveState
        var v: VehicleLocation? = null
        if (driveStateResponse != null) {
            if (driveStateResponse.latitude != null && driveStateResponse.longitude != null) {
                val currentLatitude = driveStateResponse.latitude!!
                val currentLongitude = driveStateResponse.longitude!!
                val speed: Double = if (driveStateResponse.speed == null) 0.0 else driveStateResponse.speed!!
                val heading = driveStateResponse.heading.toDouble()
                val timestamp = driveStateResponse.timestamp.toDouble()
                v = VehicleLocation(currentLatitude, currentLongitude, speed, heading, timestamp)

                // Vehicle is currently navigating. We can use that to schedule the next
                // location poll!
//				if (driveStateResponse.getAhas("active_route_latitude")
//						&& driveStateResponse.has("active_route_longitude")) {
//					double destinationLatitude = driveStateResponse.getDouble("active_route_latitude");
//					double destinationLongitude = driveStateResponse.getDouble("active_route_longitude");
//
//					VehicleLocation destination = new VehicleLocation(destinationLatitude, destinationLongitude);
//					if (driveStateResponse.has("active_route_minutes_to_arrival")) {
//						double destinationMinutesToArrival = driveStateResponse
//								.getDouble("active_route_minutes_to_arrival");
//						if (destination.distanceFrom(homeLatitude, homeLongitude) == 0) {
//							v.setGoingHome(true);
//						} else {
//							v.setGoingAwayFromHome(true);
//						}
//						v.setMinutesToArrival(destinationMinutesToArrival);
//					}
//				}
                logger.debug("Most recent vehicle location: {}", v.toString())

                // When we see vehicle is at home, clear location history so when we start
                // looking at history to
                // determine next polling times we don't have to worry about home -> destination
                // -> home.
                val distanceFromHome = v.distanceFrom(homeLatitude!!, homeLongitude!!)
                if (distanceFromHome == 0.0) {
                    vehicleLocationHistory.clear()
                    lastHome = v
                }

                // Only add this new location to the queue if it significantly different from
                // the two entries that came
                // before it. We only need two entries in a row to decide the vehicle has
                // stopped, no point in continuing
                // to fill the queue with stopped entries.
                val locationHistorySize: Int = vehicleLocationHistory.size
                if (locationHistorySize >= 2) {
                    val mostRecentLocation: VehicleLocation = vehicleLocationHistory.get(locationHistorySize - 1)
                    val previousLocation: VehicleLocation = vehicleLocationHistory.get(locationHistorySize - 2)
                    if (!(mostRecentLocation.distanceFrom(previousLocation) == 0.0
                                && mostRecentLocation.distanceFrom(v) == 0.0)
                    ) {
                        vehicleLocationHistory.add(v)
                    }
                } else {
                    vehicleLocationHistory.add(v)
                }
            }
        }
        return v
    }

    /////////////////////////////////////////////////////////////////////////
    inner class Vehicle {
        var id: String? = null

        // from vehicleData
        var user_id: String? = null
        var vehicle_id: String? = null
        var vin: String? = null
        var display_name: String? = null
        var color: String? = null

        // from vehicleData, e.g. "OWNER"
        var access_type: String? = null
        var tokens: List<String>? = null

        // e.g. "online"
        var state: String? = null
        var in_service = false
        var id_s: String? = null
        var calendar_enabled = false
        var api_version = 0
        var backseat_token: String? = null
        var backset_token_updated_at: String? = null

        // from vehicleData
        var drive_state: DriveState? = null

        // from vehicleData
        var charge_state: ChargeState? = null
        var option_codes: String? = null
        var ble_autopair_enrolled = false
    }

    /////////////////////////////////////////////////////////////////////////
    inner class ChargeState {
        var battery_heater_on = false

        // battery level in percent (e.g. 46)
        var battery_level = 0

        // in miles (150.2 = 242km)
        var battery_range = 0.0

        // charge amps requested, e.g. 32
        var charge_amps = 0

        // currently requested charging amps
        var charge_current_request = 0

        // maximum possible charging amps
        var charge_current_request_max = 0
        var charge_enable_request = false
        var charge_energy_added = 0.0

        // e.g. 50
        var charge_limit_soc = 0

        // e.g. 100
        var charge_limit_soc_max = 0

        // e.g. 50
        var charge_limit_soc_min = 0

        // e.g. 90
        var charge_limit_soc_std = 0
        var charge_miles_added_ideal = 0.0
        var charge_miles_added_rated = 0.0
        var charge_port_cold_weather_mode = false

        // e.g. "<invalid>"
        var charge_port_color: String? = null

        // e.g. "Engaged"
        var charge_port_latch: String? = null

        // e.g. 7.4
        var charge_rate = 0.0
        var charge_to_max_range = false

        // e.g. 4
        var charger_actual_current = 0

        // e.g. 2
        var charger_phases: Int? = null
        var charger_pilot_current = 0

        // e.g. 2
        var charger_power = 0

        // not always zero if not charging. I see "2" now without connection
        var charger_voltage = 0

        // e.g. "Disconnected", "Stopped", "Charging"
        var charging_state: String? = null

        // e.g. "<invalid>", IEC
        var conn_charge_cable: String? = null

        // e.g. 108.11
        var est_battery_range = 0.0

        // e.g. "<invalid>"
        var fast_charger_brand: String? = null
        var fast_charger_present = false

        // e.g. "<invalid>", "ACSingleWireCAN"
        var fast_charger_type: String? = null
        var ideal_battery_range = 0.0
        var managed_charging_active = false
        var managed_charging_start_time: Long? = null
        var managed_charging_user_canceled = false
        var max_range_charge_counter = 0

        // e.g. 370
        var minutes_to_full_charge = 0
        var not_enough_power_to_heat: Boolean? = null
        var scheduled_charging_pending = false
        var scheduled_charging_start_time: Long? = null
        var time_to_full_charge = 0.0
        var timestamp: Long = 0
        var trip_charging = false

        // e.g. 35
        var usable_battery_level = 0
        var user_charge_enable_request: String? = null
        var charge_port_door_open = false
        var off_peak_charging_enabled = false

        // e.g. "all_week"
        var off_peak_charging_times: String? = null

        // e.g. 360
        var off_peak_hours_end_time = 0
        var preconditioning_enabled = false

        // e.g. "all_week"
        var preconditioning_times: String? = null

        // e.g. "Off"
        var scheduling_charging_mode: String? = null
        var scheduling_charging_pending = false
        var scheduling_charging_start_time: String? = null
        var scheduling_charging_start_time_app = 0
        var scheduled_departure_time: Long = 0
        var scheduled_departure_time_minutes = 0
        var supercharger_session_trip_planner = false
        override fun toString(): String {
            return """
                battery_heater_on=$battery_heater_on
                battery_level=$battery_level
                battery_range=$battery_range
                charge_amps=$charge_amps
                charge_current_request=$charge_current_request
                charge_current_request_max=$charge_current_request_max
                charge_enable_request=$charge_enable_request
                charge_energy_added=$charge_energy_added
                charge_limit_soc=$charge_limit_soc
                charge_limit_soc_max=$charge_limit_soc_max
                charge_limit_soc_min=$charge_limit_soc_min
                charge_limit_soc_std=$charge_limit_soc_std
                charge_miles_added_ideal=$charge_miles_added_ideal
                charge_miles_added_rated=$charge_miles_added_rated
                charge_port_cold_weather_mode=$charge_port_cold_weather_mode
                charge_port_color=$charge_port_color
                charge_port_latch=$charge_port_latch
                charge_rate=$charge_rate
                charge_to_max_range=$charge_to_max_range
                charger_actual_current=$charger_actual_current
                charger_phases=$charger_phases
                charger_pilot_current=$charger_pilot_current
                charger_power=$charger_power
                charger_voltage=$charger_voltage
                charging_state=$charging_state
                conn_charge_cable=$conn_charge_cable
                est_battery_range=$est_battery_range
                fast_charger_brand=$fast_charger_brand
                fast_charger_present=$fast_charger_present
                fast_charger_type=$fast_charger_type
                ideal_battery_range=$ideal_battery_range
                managed_charging_active=$managed_charging_active
                managed_charging_start_time=$managed_charging_start_time
                managed_charging_user_canceled=$managed_charging_user_canceled
                max_range_charge_counter=$max_range_charge_counter
                minutes_to_full_charge=$minutes_to_full_charge
                not_enough_power_to_heat=$not_enough_power_to_heat
                scheduled_charging_pending=$scheduled_charging_pending
                scheduled_charging_start_time=$scheduled_charging_start_time
                time_to_full_charge=$time_to_full_charge
                timestamp=$timestamp
                trip_charging=$trip_charging
                usable_battery_level=$usable_battery_level
                user_charge_enable_request=$user_charge_enable_request
                charge_port_door_open=$charge_port_door_open
                off_peak_charging_enabled=$off_peak_charging_enabled
                off_peak_charging_times=$off_peak_charging_times
                off_peak_hours_end_time=$off_peak_hours_end_time
                preconditioning_enabled=$preconditioning_enabled
                preconditioning_times=$preconditioning_times
                scheduling_charging_mode=$scheduling_charging_mode
                scheduling_charging_pending=$scheduling_charging_pending
                scheduling_charging_start_time=$scheduling_charging_start_time
                scheduling_charging_start_time_app=$scheduling_charging_start_time_app
                scheduled_departure_time=$scheduled_departure_time
                scheduled_departure_time_minutes=$scheduled_departure_time_minutes
                supercharger_session_trip_planner=$supercharger_session_trip_planner
                """.trimIndent()
        }
    }

    /////////////////////////////////////////////////////////////////////////
    inner class DriveState {
        var gps_as_of = 0
        var heading = 0
        var latitude: Double? = null
        var longitude: Double? = null
        var native_latitude = 0.0
        var native_longitude = 0.0
        var native_location_supported = 0
        var native_type: String? = null
        var power = 0
        var shift_state: String? = null
        var speed: Double? = null
        var timestamp: Long = 0

        override fun toString(): String {
            return """
                gps_as_of=$gps_as_of
                heading=$heading
                latitude=$latitude
                longitude=$longitude
                native_latitude=$native_latitude
                native_longitude=$native_longitude
                native_location_supported=$native_location_supported
                native_type=$native_type
                power=$power
                shift_state=$shift_state
                speed=$speed
                timestamp=$timestamp
            """.trimIndent()
        }
    }

    /////////////////////////////////////////////////////////////////////////
    internal inner class VehicleList {
        var response: List<Vehicle>? = null
        var count = 0
    }

    /////////////////////////////////////////////////////////////////////////
    inner class VehicleData {
        var response: Vehicle? = null
    }

    companion object {
        val logger = LogManager.getLogger(TeslaVehicle::class.java)
    }
}
