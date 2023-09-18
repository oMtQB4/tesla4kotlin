import de.apps_roters.tesla_for_kotlin.tesla.*
import de.apps_roters.tesla_for_kotlin.web.*

fun main(args: Array<String>) {
    val restRequest = RestRequest()
    val teslaConfiguration = TeslaConfiguration()
    val teslaAuth = TeslaAuth(restRequest, teslaConfiguration)

    val authRestRequest = AuthRestRequest(restRequest, teslaAuth)

    val teslaVehicle = TeslaVehicle(authRestRequest, teslaConfiguration)
    val teslaCharge = TeslaCharge(authRestRequest, teslaVehicle, teslaConfiguration)

    println("Charging State:\n${teslaCharge.chargeState.toString()}")
    println("Drive State:\n${teslaVehicle.vehicleData?.drive_state.toString()}")

}