# ECSE Project 100

## Introduction

This physiological monitoring app is part of the physiological monitoring system developed as part of ECSE Project 100.

ECSE Project 100 is one of many research projects undertaken as part of SOFTENG 700 in the academic year 2023 at the Faculty of Engineering, University of Auckland. It is overseen by the Department of Electrical, Computer and Software Engineering (ECSE). The project is supported by the Faculty of Science, University of Auckland.

## Setup

- Clone / download this repository.
- Open the sample project in Android Studio.
- Make sure you have a valid API key. You can request one for your Empatica Connect account from the [Empatica Developer Area][1].
- Create `apikey.properties` in the root folder of the repository.
- Inside `apikey.properties`, add the line `EMPATICA_API_KEY="<your API key>"`.
- Download the Android E4 link 1.0 or newer SDK from the [Empatica Developer Area][1].
- Unzip the archive you've downloaded and copy the `.aar` file you'll find inside into the `libs` folder contained in the sample project.
- Build and run the project.
- If a device is in range and its light is blinking green, but the app doesn't connect, please check that the discovered device can be used with your API key. If the `allowed` parameter is always false, the device is not linked to your API key. Please check your [Empatica Developer Area][1].

If you need any additional information about the Empatica API for Android, please check the [official documentation][2].

[1]: https://www.empatica.com/connect/developer.php
[2]: http://developer.empatica.com
