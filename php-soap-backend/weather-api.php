<?php
/**
 * AgriFlow REST Weather API (Live Real-Time Data)
 *
 * Fetches real environmental data from Open-Meteo (public API) based on
 * coordinates. If no coordinates are provided, it defaults to Laguna, PH.
 *
 * GET /weather-api.php?lat=14.1&lon=121.2
 */

header("Content-Type: application/json; charset=utf-8");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, OPTIONS");

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(204);
    exit;
}

// Laguna, Philippines defaults
$lat = isset($_GET['lat']) ? (float)$_GET['lat'] : 14.27;
$lon = isset($_GET['lon']) ? (float)$_GET['lon'] : 121.12;

$apiUrl = "https://api.open-meteo.com/v1/forecast?latitude={$lat}&longitude={$lon}&current=temperature_2m,relative_humidity_2m,weather_code&timezone=auto";

/**
 * Weather Code to Condition mapping (WMO codes)
 */
function getWeatherCondition($code) {
    if ($code == 0) return "Clear Sky";
    if ($code >= 1 && $code <= 3) return "Partly Cloudy";
    if ($code >= 45 && $code <= 48) return "Foggy";
    if ($code >= 51 && $code <= 67) return "Rainy";
    if ($code >= 71 && $code <= 77) return "Snowy";
    if ($code >= 80 && $code <= 82) return "Rain Showers";
    if ($code >= 95) return "Thunderstorm";
    return "Unknown";
}

$ch = curl_init($apiUrl);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_TIMEOUT, 10);
$response = curl_exec($ch);
$httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
curl_close($ch);

if ($httpCode !== 200 || !$response) {
    echo json_encode([
        "success" => false,
        "error" => "Failed to fetch real-time weather data"
    ]);
    exit;
}

$data = json_decode($response, true);
$current = $data['current'];

$payload = [
    "success" => true,
    "temperature" => round($current['temperature_2m'], 1),
    "unit" => "Celsius",
    "humidity" => $current['relative_humidity_2m'],
    "condition" => getWeatherCondition($current['weather_code']),
    "location" => "Lat: $lat, Lon: $lon",
    "lat" => $lat,
    "lon" => $lon,
    "fetchedAt" => date(DATE_ATOM)
];

echo json_encode($payload, JSON_PRETTY_PRINT);
