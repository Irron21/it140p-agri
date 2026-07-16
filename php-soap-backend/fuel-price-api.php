<?php
/**
 * AgriFlow REST Fuel Price API (Live Data)
 *
 * Lightweight REST (JSON over HTTP) endpoint that provides live diesel
 * prices by scraping gaswatchph.com in real-time.
 *
 * GET /fuel-price-api.php
 */

header("Content-Type: application/json; charset=utf-8");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, OPTIONS");

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(204);
    exit;
}

const SOURCE_URL = "https://gaswatchph.com/";
const FALLBACK_PRICE = 60.00;

/**
 * Fetches raw HTML from a URL with a short timeout.
 */
function fetchHtml(string $url): ?string {
    if (function_exists('curl_init')) {
        $ch = curl_init($url);
        curl_setopt_array($ch, [
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_FOLLOWLOCATION => true,
            CURLOPT_CONNECTTIMEOUT => 6,
            CURLOPT_TIMEOUT => 8,
            CURLOPT_USERAGENT => "AgriFlow-FuelPriceBot/1.0",
            CURLOPT_SSL_VERIFYPEER => true,
        ]);
        $body = curl_exec($ch);
        $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        curl_close($ch);

        if ($body === false || $httpCode >= 400) return null;
        return $body;
    }

    $context = stream_context_create(['http' => ['method' => 'GET', 'timeout' => 8]]);
    $body = @file_get_contents($url, false, $context);
    return $body !== false ? $body : null;
}

/**
 * Extracts the Metro Manila average diesel price from the source HTML.
 */
function extractDieselPrice(string $html): ?array {
    $text = strip_tags($html);
    $text = html_entity_decode($text, ENT_QUOTES | ENT_HTML5, 'UTF-8');
    $text = preg_replace('/\s+/u', ' ', $text);

    if (preg_match('/Metro Manila average diesel price is\s*₱\s*([0-9]+(?:\.[0-9]+)?)\s*\/\s*L/iu', $text, $m)) {
        $asOf = null;
        if (preg_match('/As of\s+([A-Z][a-z]+\s+\d{1,2},\s*\d{4}),\s*the Metro Manila average diesel/iu', $text, $d)) {
            $asOf = $d[1];
        }
        return ['price' => (float)$m[1], 'asOf' => $asOf, 'note' => 'Metro Manila station-weighted average'];
    }

    if (preg_match('/Metro Manila averages are\s*₱\s*([0-9]+(?:\.[0-9]+)?)\s*\/\s*L\s*for diesel/iu', $text, $m)) {
        return ['price' => (float)$m[1], 'asOf' => null, 'note' => 'Metro Manila station-weighted average'];
    }

    return null;
}

// --- Main request handling -------------------------------------------------

$html = fetchHtml(SOURCE_URL);
$extracted = $html !== null ? extractDieselPrice($html) : null;

if ($extracted !== null) {
    $payload = [
        'success' => true,
        'fuelType' => 'diesel',
        'pricePerLiter' => $extracted['price'],
        'currency' => 'PHP',
        'asOfText' => $extracted['asOf'],
        'note' => $extracted['note'],
        'sourceUrl' => SOURCE_URL,
        'fetchedAt' => date(DATE_ATOM),
        'cacheStatus' => 'live'
    ];
} else {
    // Fallback if scraping fails
    $payload = [
        'success' => true,
        'fuelType' => 'diesel',
        'pricePerLiter' => FALLBACK_PRICE,
        'currency' => 'PHP',
        'asOfText' => null,
        'note' => 'Live scrape unavailable; using built-in default.',
        'sourceUrl' => SOURCE_URL,
        'fetchedAt' => date(DATE_ATOM),
        'cacheStatus' => 'fallback'
    ];
}

echo json_encode($payload, JSON_PRETTY_PRINT | JSON_UNESCAPED_SLASHES);
