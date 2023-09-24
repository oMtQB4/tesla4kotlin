# Tesla for Kotlin

This library is based on the implementation of [teslacharging](https://github.com/mikes222/teslacharging)
from [mikes222](https://github.com/mikes222).

## Features

- Start and stop charging
- Set the charging amps
- Set the charging limit in percent
- Retrieve charging infos from the car
- Retrieve state of the car
- Open/close the chargeport
- Set the charging limit in percent
- Wake the car up

## Using the library

### Download
Download the tesla4kotlin-VERSION.jar of the current version from the resources directory and add it to your project.

### Include
Include the library in your project, e.g. in gradle (kotlin). 
Copy the file to the `libs` folder and add the dependency to your `build.gradle.kts` file:

```kotlin
dependencies {
    implementation(files("libs/tesla4kotlin-VERSION.jar"))
}
```

### Authentication

To use the library, you need to authenticate with your Tesla account.
Currently, an access token and a refresh token are used to authenticate.


## Build the library

This library is using `semantic-release` to manage the versioning.

Call `./gradlew lib` to build the library.
The library will be created in the `build/libs` directory.


## Setup

TODO

## Contribution

Feel free to enhance the program as needed and create PullRequests so that others can benefit from your enhancements.
Please also follow [mikes222](https://github.com/mikes222).

## Credits

**Source inspired by**

[https://github.com/mikes222/teslacharging](https://github.com/mikes222/teslacharging)

[https://github.com/rrarey02/TeslaRTPCharging](https://github.com/rrarey02/TeslaRTPCharging)

**Tesla API documentation (unofficial):**

[https://tesla-api.timdorr.com](https://tesla-api.timdorr.com)

[https://www.teslaapi.io](https://www.teslaapi.io)

## Disclaimer

We are not liable for any damage or any functionality of this app. The library is provided as-is. Use at your own risk.

## License

Apache-2.0 license
