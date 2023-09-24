package de.apps_roters.tesla_for_kotlin.tesla

import de.apps_roters.tesla_for_kotlin.web.*
import org.apache.logging.log4j.LogManager

class TeslaCharge(
    authRestRequest: AuthRestRequest, teslaVehicle: TeslaVehicle,
    teslaConfiguration: TeslaConfiguration?
) {
    private val authRestRequest: AuthRestRequest
    private val teslaVehicle: TeslaVehicle

    init {
        this.authRestRequest = authRestRequest
        this.teslaVehicle = teslaVehicle
    }

    @get:Throws(Exception::class)
    val isVehicleCharging: Boolean
        /**
         * Determine whether vehicle is currently charging.
         *
         * @param id ID of the vehicle to use when requesting the charge state
         * @return Boolean for whether the vehicle is currently charging.
         * @throws Exception
         */
        get() {
            val chargeState: TeslaVehicle.ChargeState? = teslaVehicle.vehicleChargeState
            if (chargeState != null) {
                if (chargeState.charging_state != null && chargeState.charge_port_door_open) {
                    val chargingState = chargeState.charging_state
                    return chargingState.equals("charging", ignoreCase = true)
                }
            }
            return false
        }

    /**
     * Send a charge command to the vehicle
     *
     * @param id            ID of the vehicle to use in the charge command request
     * @param chargeCommand Command to send (start/stop)
     * @return `null` if successful or the reason why the command failed.
     * e.g. unknown (no result received), disconnected (cable not connected
     * to the car)
     * @throws Exception
     */
    @Throws(Exception::class)
    private fun sendChargeCommand(chargeCommand: String): String? {
        val apiEndpoint: String =
            (TeslaConfiguration.Companion.apiBase + "/api/1/vehicles/" + teslaVehicle.id + "/command/charge_"
                    + chargeCommand)
        val simpleResult = authRestRequest.postJson<SimpleResult>(apiEndpoint, null, SimpleResult::class.java)
        if (simpleResult?.response == null) return "unknown"
        return if (simpleResult.response!!.result) {
            null
        } else simpleResult.response!!.reason
    }

    /**
     *
     * @param chargePortCommand open/close
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    private fun sendChargePortCommand(chargePortCommand: String): String? {
        val apiEndpoint: String = (TeslaConfiguration.Companion.apiBase + "/api/1/vehicles/" + teslaVehicle.id
                + "/command/charge_port_door_" + chargePortCommand)
        val simpleResult = authRestRequest.postJson<SimpleResult>(apiEndpoint, null, SimpleResult::class.java)
        if (simpleResult == null || simpleResult.response == null) return "unknown"
        return if (simpleResult.response!!.result) {
            null
        } else simpleResult.response!!.reason
    }

    /**
     * Start charging the vehicle
     *
     * @param id ID of the vehicle to use in the request to start charging
     * @return `null` if successful or the reason why the command failed.
     * e.g. unknown (no result received), disconnected (cable not connected
     * to the car)
     * @throws Exception
     */
    @Throws(Exception::class)
    fun startCharging(): String? {
        return sendChargeCommand("start")
    }

    /**
     * Stop charging the vehicle
     *
     * @param id ID of the vehicle to use in the request to stop charging
     * @return `null` if successful or the reason why the command failed.
     * e.g. unknown (no result received), disconnected (cable not connected
     * to the car)
     * @throws Exception
     */
    @Throws(Exception::class)
    fun stopCharging(): String? {
        return sendChargeCommand("stop")
    }

    @Throws(Exception::class)
    fun chargePortOpen(): String? {
        return sendChargePortCommand("open")
    }

    @Throws(Exception::class)
    fun chargePortClose(): String? {
        return sendChargePortCommand("close")
    }

    @Throws(Exception::class)
    fun setChargeLimit(percent: Int): String? {
        val apiEndpoint: String = (TeslaConfiguration.Companion.apiBase + "/api/1/vehicles/" + teslaVehicle.id
                + "/command/set_charge_limit")
        val percentValue = PercentValue(percent)
        val simpleResult = authRestRequest.postJson<SimpleResult>(apiEndpoint, percentValue, SimpleResult::class.java)
        if (simpleResult == null || simpleResult.response == null) return "unknown"
        return if (simpleResult.response!!.result) {
            null
        } else simpleResult.response!!.reason
    }

    /**
     *
     * @param chargingAmps
     * @return `null` if the call was successful or the reason why the call failed
     * @throws Exception
     */
    @Throws(Exception::class)
    fun setChargingAmps(chargingAmps: Int): String? {
        val apiEndpoint: String = (TeslaConfiguration.Companion.apiBase + "/api/1/vehicles/" + teslaVehicle.id
                + "/command/set_charging_amps")
        val percentValue = ChargingAmpsValue(chargingAmps)
        val simpleResult = authRestRequest.postJson<SimpleResult>(apiEndpoint, percentValue, SimpleResult::class.java)
        if (simpleResult == null || simpleResult.response == null) return "unknown"
        return if (simpleResult.response!!.result) {
            null
        } else simpleResult.response!!.reason
    }

    @get:Throws(Exception::class)
    val chargeState: TeslaVehicle.ChargeState?
        get() {
            val apiEndpoint: String = (TeslaConfiguration.Companion.apiBase + "/api/1/vehicles/" + teslaVehicle.id
                    + "/data_request/charge_state")
            val simpleResult = authRestRequest.getJSON<ChargeStateData>(apiEndpoint, ChargeStateData::class.java)
            return if (simpleResult == null || simpleResult.response == null) null else simpleResult.response
        }

    /////////////////////////////////////////////////////////////////////////
    internal inner class SimpleResult {
        var response: SimpleResponse? = null
    }

    /////////////////////////////////////////////////////////////////////////
    internal inner class SimpleResponse {
        var reason: String? = null
        var result = false
    }

    /////////////////////////////////////////////////////////////////////////
    internal inner class PercentValue(var percent: Int)

    /////////////////////////////////////////////////////////////////////////
    internal inner class ChargingAmpsValue(var charging_amps: Int)

    /////////////////////////////////////////////////////////////////////////
    internal inner class ChargeStateData {
        var response: TeslaVehicle.ChargeState? = null
    }

    companion object {
        val logger = LogManager.getLogger(TeslaConfiguration::class.java)
    }
}
