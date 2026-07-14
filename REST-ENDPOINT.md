# Live Fuel Price REST Endpoint

## Summary
Adds a lightweight REST/JSON integration that fetches the current diesel
price (₱/liter) from a backend endpoint, so the Freight Cost calculator can
use a live market rate instead of a manually-typed value. This sits
alongside the existing SOAP integrations as an intentionally simpler
alternative — a plain HTTP GET with a JSON response, no WSDL contract or XML
envelope.

## Why REST here and SOAP elsewhere
The rest of the app (`SoapRepository`) talks to a `server.php` WSDL endpoint
because those transactions (yield forecast, freight cost, hub clustering,
carbon footprint) are structured request/response calculations. Fuel price
is different: it's a single, frequently-changing value with no complex
input shape, so a plain REST call is the more appropriate — and lighter —
tool for the job. Keeping both in the app also demonstrates the two
integration styles side by side.

## Files touched
| File | Role |
|---|---|
| `network/FuelPriceRepository.kt` | New. Owns the HTTP call and JSON parsing. |
| `viewmodel/AgriFlowViewModel.kt` | Holds the endpoint URL, exposes `fuelPriceState`, exposes `fetchLiveFuelPrice()` / `updateFuelApiUrl()`. |
| `ui/main/MainScreen.kt` | "Fetch Live Diesel Price (REST)" button on the Freight tab; settings dialog field for the endpoint URL. |

## How it works

### 1. `FuelPriceRepository.fetchDieselPrice(endpointUrl)`
- Opens a plain `HttpURLConnection` (`GET`, `Accept: application/json`),
  8-second connect/read timeouts.
- Reads the response body, checks the HTTP status code is `2xx`.
- Parses the JSON body into a `FuelPriceInfo` domain model:

  ```kotlin
  data class FuelPriceInfo(
      val pricePerLiter: Double,
      val fuelType: String,
      val currency: String,
      val sourceUrl: String,
      val asOfText: String?,
      val note: String?,
      val cacheStatus: String,   // "live" | "cached" | "stale-cache" | "fallback"
      val fetchedAt: String?
  )
  ```
- Returns `Result<FuelPriceInfo>` — `Result.success` on a well-formed
  `{"success": true, ...}` body, `Result.failure` on a bad HTTP status, a
  `{"success": false}` body, or any exception (network error, malformed
  JSON, etc.).
- `cacheStatus` tells the UI whether the value is fresh off the source,
  served from cache, stale cache, or a hardcoded fallback — this is shown
  to the user rather than hidden, so they know how trustworthy the number
  is.

### 2. ViewModel wiring
- `_fuelApiUrl` (`MutableStateFlow`) holds the endpoint URL, defaulting to
  `http://10.0.2.2:8000/fuel-price-api.php` (the special IP Android
  emulators use to reach the host machine's `localhost`).
- `updateFuelApiUrl(newUrl)` lets the user override it via Settings — useful
  for testing against a different host or a deployed server.
- `fetchLiveFuelPrice()` launches a coroutine, sets `_fuelPriceState` to
  `Loading`, calls the repository, and maps the `Result` to
  `SoapUiState.Success` / `SoapUiState.Error` (the same sealed state type
  used by the SOAP calls, so the UI layer handles both uniformly).

### 3. UI (Freight tab)
- A slider still lets the user set the fuel price manually.
- Below it, an **"Fetch Live Diesel Price (REST)"** button triggers the
  fetch; while loading it shows a spinner + "Fetching from GasWatch PH…".
- On success, a caption shows the cache status and `asOfText` (e.g. *"Live ·
  GasWatch PH · as of ..."*). On failure, an inline error caption is shown
  instead — the fetch failing doesn't block the freight calculation, since
  the manual slider value is still usable.
- The endpoint URL itself is editable from the settings dialog (gear icon
  in the top bar), alongside the SOAP `server.php` URL.

## Not in scope
- No local caching of the fetched value on the Android side — `cacheStatus`
  values like `"cached"`/`"stale-cache"` reflect caching done on the
  **server**, not the app.
- No retry/backoff logic; a failed fetch just surfaces an error and the
  user can tap the button again or keep using the manual slider.
