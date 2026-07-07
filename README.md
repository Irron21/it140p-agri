# Agricultural Logistics Hub

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
│   │   └── java/com/example/agrihub/
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

### Prerequisites
* **PHP 8.0+** installed. (Found at `C:\xampp\php\php.exe` in XAMPP environments).
* **SOAP Extension Enabled**:
  * Open your `php.ini` (e.g. `C:\xampp\php\php.ini`).
  * Find the line `;extension=soap` and remove the leading semicolon (change to `extension=soap`).

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

---

## Android App Setup (`/android-app`)

### Configuring the Connection Endpoint
By default, the Android app (emulator) targets `http://10.0.2.2:8000/server.php`.

* **If youre running on Android Studio**: Leave the default configuration.
1. Open Folder (IT140P > android-app)
2. Open Device Manager and create an emulator with its lowest API.

### Dependencies & Setup
All dependencies resolve automatically. Key imports in the Gradle build:
* `com.google.code.ksoap2-android:ksoap2-android:3.6.4`
* Sonatype Repository resolution for `ksoap2` is configured in `settings.gradle.kts`.

---

## Recommendations
* Data Persistence (Database Integration): Maybe some sort of history list of recent computations?
* REST Integration: Maybe an API to fetch live fuel prices? Hence we need REST instead of SOAP to avoid heavy XML envelopes!
