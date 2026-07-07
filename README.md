# Agricultural Logistics Hub

This is a high-fidelity foundational scaffolding for a modern agricultural logistics platform using a hybrid mobile/web SOAP service architecture. It integrates a **PHP SOAP Backend** with a WSDL interface and a **Kotlin Jetpack Compose Android Client** powered by `ksoap2-android`.

---

## Project Architecture

```
IT140P/
├── php-soap-backend/          # SOAP Web Service Server & Diagnostics
│   ├── agriflow.wsdl          # Strict-typed SOAP WSDL service contract
│   ├── server.php             # Native PHP SoapServer implementation
│   └── client.php             # Browser-based test client & XML inspector
├── android-app/               # Jetpack Compose Android Application
│   ├── app/src/main/
│   │   ├── AndroidManifest.xml # Network configurations & internet permissions
│   │   └── java/com/example/agriflow/
│   │       ├── MainActivity.kt # Entry point activity
│   │       ├── Navigation.kt   # App navigation mapping
│   │       ├── network/
│   │       │   └── SoapRepository.kt # SOAP envelopes constructor & parser
│   │       ├── viewmodel/
│   │       │   └── AgriFlowViewModel.kt # StateFlow inputs/outputs manager
│   │       └── ui/
│   │           ├── theme/      # Agricultural green color palettes
│   │           └── screens/    # Jetpack Compose UI (MainScreen.kt)
│   └── build.gradle.kts       # Android module build definitions
└── README.md                  # System setup & configuration guide
```

---

## Backend Setup (`/php-soap-backend`)

The PHP SOAP server uses the native `SoapServer` class. Follow these instructions to launch it locally.

### Prerequisites
* **PHP 8.0+** installed. (Found at `C:\xampp\php\php.exe` in XAMPP environments).
* **SOAP Extension Enabled**:
  * Open your `php.ini` (e.g. `C:\xampp\php\php.ini`).
  * Find the line `;extension=soap` and remove the leading semicolon (change to `extension=soap`).
  * *Note: The running script is configured to load the extension dynamically at runtime, but system-wide activation is recommended.*

### Starting the Server
Run PHP's built-in web server pointing to the backend directory:
```powershell
# Open terminal in the workspace root
cd php-soap-backend

# Start the PHP server on port 8000 explicitly listening on IPv4
C:\xampp\php\php.exe -d extension=soap -S 127.0.0.1:8000
```

### Testing the SOAP Service in the Browser
Once the server is running, open your web browser and navigate to:
* **WSDL Definition**: `http://localhost:8000/server.php?wsdl`
* **SOAP Web Diagnostic Client**: `http://localhost:8000/client.php`

The browser diagnostic client (`client.php`) lets you test all four calculations directly in the browser and inspect the exact **SOAP Request** and **SOAP Response XML envelopes** sent over the wire.

---

## Android App Setup (`/android-app`)

The Android application is built with **Kotlin**, **Jetpack Compose (Material 3)**, and uses the **ksoap2-android** library to consume the PHP SOAP service.

### Configuring the Connection Endpoint
By default, the Android app targets `http://10.0.2.2:8000/server.php` (which represents the local computer host machine from inside the Android Emulator).

* **If running on an Emulator**: Leave the default configuration.
* **If running on a Physical Android Device**:
  1. Make sure your computer and Android device are on the *same Wi-Fi network*.
  2. Find your computer's local IP address (e.g., `192.168.1.150`).
  3. Inside the AgriFlow Android app, tap the **Settings (Gear Icon)** in the top right header.
  4. Change the server URL to point to your computer's IP (e.g., `http://192.168.1.150:8000/server.php`).

### Dependencies & Setup
All dependencies resolve automatically. Key imports in the Gradle build:
* `com.google.code.ksoap2-android:ksoap2-android:3.6.4`
* Sonatype Repository resolution for `ksoap2` is configured in `settings.gradle.kts`.

---

## Computational Transactions

AgriFlow implements 4 complex calculations:

1. **Yield Forecast (`calculateYieldForecast`)**
   * *Arguments*: `area` (float), `temp` (float), `ph` (float)
   * *Model*: Predicts crop production using double bell-curve distributions that penalize extreme temperature or acidic/alkaline soils (Optimal values: 25°C, 6.5 pH).

2. **Freight Pricing (`calculateFreightPrice`)**
   * *Arguments*: `distance` (float), `fuelPrice` (float), `weight` (float)
   * *Model*: Computes shipping rates with a $250.00 base dispatcher fare, a fuel usage rate of 3.5 km/liter, and cargo load weight fee ($0.05 per km per ton).

3. **Hub Location Clustering (`calculateHubCluster`)**
   * *Arguments*: `latitudes` (array of floats), `longitudes` (array of floats), `k` (int)
   * *Model*: Implements a **K-Means clustering algorithm** in PHP. It groups multiple farm coordinates into `k` optimal logistics hub locations (centroids). Contains a "Randomize Sample Data" helper in the app to instantly fill coordinates.

4. **Carbon Footprint (`calculateCarbonFootprint`)**
   * *Arguments*: `efficiency` (float), `distance` (float), `weight` (float)
   * *Model*: Calculates CO₂ emission metrics in kilograms. The Android UI features convenient presets for Heavy Trucks (62.0 g/t-km), Trains (22.0 g/t-km), Air freight (560.0 g/t-km), and Ocean ships (8.0 g/t-km).
