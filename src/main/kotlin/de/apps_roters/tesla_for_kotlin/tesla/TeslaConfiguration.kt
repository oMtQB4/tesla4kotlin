package de.apps_roters.tesla_for_kotlin.tesla

import org.apache.logging.log4j.LogManager
import org.json.JSONObject
import java.io.*
import java.util.*

class TeslaConfiguration() {
    private var propertiesFile = "app.properties"
    private var prop: Properties? = null

    fun load(propertiesFile: String) {
        this.propertiesFile = propertiesFile
    }

    /**
     * Store any updated configuration values in the properties file
     *
     * @param configChanges HashMap of configuration keys and values to update
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun updateConfiguration(configChanges: HashMap<String, Any?>) {
        logger.debug("Update configuration with {}", JSONObject(configChanges).toString())
        val newLines = ArrayList<String>()
        val scanner: Scanner
        scanner = Scanner(File(propertiesFile))
        while (scanner.hasNextLine()) {
            var line = scanner.nextLine().trim { it <= ' ' }
            if (line.indexOf("=") > 0) {
                val parts = line.split("=".toRegex(), limit = 2).toTypedArray()
                val key = parts[0]
                var value = parts[1]
                if (configChanges.containsKey(key)) {
                    value = configChanges[key].toString()
                }
                line = "$key=$value"
            }
            newLines.add(line)
        }
        scanner.close()
        if (newLines.size > 0) {
            val writer = BufferedWriter(FileWriter(propertiesFile))
            for (line in newLines) {
                writer.write(line + "\n")
            }
            writer.close()
            val config = File(propertiesFile)
            lastConfigurationModification = config.lastModified()
        }
    }

    @Throws(IOException::class)
    fun updateTokens(accessToken: String?, refreshToken: String?) {
        val newTokens = HashMap<String, Any?>()
        newTokens[ACCESS_TOKEN] = accessToken
        newTokens[REFRESH_TOKEN] = refreshToken
        updateConfiguration(newTokens)
    }

    @Throws(IOException::class)
    fun updateVin(vin: String?, id_s: String?, displayName: String?) {
        val newTokens = HashMap<String, Any?>()
        newTokens[VIN] = vin
        newTokens[ID_S] = id_s
        newTokens[DISPLAY_NAME] = displayName
        updateConfiguration(newTokens)
    }

    @Throws(IOException::class)
    fun updateHomeLocation(latitude: Double, longitude: Double) {
        val newTokens = HashMap<String, Any?>()
        newTokens[HOME_LATITUDE] = latitude
        newTokens[HOME_LONGITUDE] = longitude
        updateConfiguration(newTokens)
    }

    private fun openPropertiesFile() {
        if (prop != null) return
        val config = File(propertiesFile)
        var configurationUpdated = false
        if (lastConfigurationModification > 0 && config.lastModified() > lastConfigurationModification) {
            configurationUpdated = true
            logger.info("Configuration has changed")
        } else if (lastConfigurationModification > 0) {
            return
        }
        lastConfigurationModification = config.lastModified()
        prop = Properties()
        try {
            FileInputStream(propertiesFile).use { fis -> prop!!.load(fis) }
        } catch (ex: FileNotFoundException) {
            logger.fatal("$propertiesFile not found in current directory, Exiting.")
            System.exit(1)
        } catch (ex: IOException) {
            logger.fatal("$propertiesFile could not be read, Exiting.")
            System.exit(1)
        }
    }

    fun readAccessToken(): String {
        openPropertiesFile()
        return prop!!.getProperty(ACCESS_TOKEN)
    }

    fun readRefreshToken(): String {
        openPropertiesFile()
        return prop!!.getProperty(REFRESH_TOKEN)
    }

    fun readVin(): String? {
        openPropertiesFile()
        return if (prop!!.getProperty(VIN).isEmpty()) null else prop!!.getProperty(VIN)
    }

    fun readID_S(): String? {
        openPropertiesFile()
        return if (prop!!.getProperty(ID_S).isEmpty()) null else prop!!.getProperty(ID_S)
    }

    fun readDisplayName(): String? {
        openPropertiesFile()
        return if (prop!!.getProperty(DISPLAY_NAME).isEmpty()) null else prop!!.getProperty(DISPLAY_NAME)
    }

    fun readHomeLatitude(): Double {
        openPropertiesFile()
        return prop!!.getProperty(HOME_LATITUDE).toDouble()
    }

    fun readHomeLongitude(): Double {
        openPropertiesFile()
        return prop!!.getProperty(HOME_LONGITUDE).toDouble()
    }

    companion object {
        private val logger = LogManager.getLogger(TeslaConfiguration::class.java)

        // Tesla API base URL
        const val apiBase = "https://owner-api.teslamotors.com"

        // API call retry settings
        const val MAX_RETRIES = 5
        const val RETRY_INTERVAL_SECONDS = 15

        // Property file keys
        const val ACCESS_TOKEN = "ACCESS_TOKEN"
        const val HOME_LATITUDE = "HOME_LATITUDE"
        const val HOME_LONGITUDE = "HOME_LONGITUDE"
        const val MAX_ELECTRICITY_PRICE = "MAX_ELECTRICITY_PRICE"
        const val MINIMUM_DEPARTURE_SOC = "MINIMUM_DEPARTURE_SOC"
        const val POLL_INTERVAL_SECONDS = "POLL_INTERVAL_SECONDS"
        const val REFRESH_TOKEN = "REFRESH_TOKEN"
        const val RESTART_ON_CURRENT_DROP = "RESTART_ON_CURRENT_DROP"
        const val SOC_GAIN_PER_HOUR = "SOC_GAIN_PER_HOUR"
        const val VIN = "VIN"
        const val ID_S = "ID_S"
        const val DISPLAY_NAME = "DISPLAY_NAME"
        var lastConfigurationModification: Long = 0

        /**
         * Load configuration into the variables we use. Also handles reloading
         * configuration when the properties file is changed.
         */
        fun loadConfiguration() {

            // Departure charge settings
            val departureChargeUpdated = false
            //		try {
//			int newMinimumDepartureSoC = Integer.parseInt(prop.getProperty(MINIMUM_DEPARTURE_SOC));
//			if (configurationUpdated && newMinimumDepartureSoC != minimumDepartureSoC) {
//				log("New minimum departure SoC: " + newMinimumDepartureSoC + "%");
//				departureChargeUpdated = true;
//			}
//			minimumDepartureSoC = newMinimumDepartureSoC;
//		} catch (Exception ex) {
//			minimumDepartureSoC = 0;
//		}
//
//		try {
//			double newSoCGainPerHour = Double.parseDouble(prop.getProperty(SOC_GAIN_PER_HOUR));
//			if (configurationUpdated && newSoCGainPerHour != soCGainPerHour) {
//				log("New SoC gain per hour: " + newSoCGainPerHour + "%");
//				departureChargeUpdated = true;
//			}
//			soCGainPerHour = newSoCGainPerHour;
//		} catch (Exception ex) {
//			soCGainPerHour = 0;
//			minimumDepartureSoC = 0;
//		}
//
//		shouldChargeForDepature = (minimumDepartureSoC > 0 && soCGainPerHour > 0);
//		if (configurationUpdated && departureChargeUpdated) {
//			log("Vehicle will" + (!shouldChargeForDepature ? " not" : "") + " be charged to reach minimum departure SoC"
//					+ (minimumDepartureSoC > 0 ? " of " + minimumDepartureSoC + "%" : "") + ".");
//		}
//
//		// Restart on current drop setting
//		boolean newRestartOnCurrentDrop = false;
//		String restartOnCurrentDropSetting = prop.getProperty(RESTART_ON_CURRENT_DROP);
//		if (restartOnCurrentDropSetting != null && restartOnCurrentDropSetting.toLowerCase().equals("y")) {
//			newRestartOnCurrentDrop = true;
//		}
//
//		if (configurationUpdated && newRestartOnCurrentDrop != restartOnCurrentDrop) {
//			log("New restart on current drop flag: " + (newRestartOnCurrentDrop ? "Y" : "N"));
//		}
//		restartOnCurrentDrop = newRestartOnCurrentDrop;
        }
    }
}
