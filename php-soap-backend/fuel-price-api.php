<?php
/**
 * AgriFlow REST Fuel Price API
 *
 * Lightweight REST (JSON over HTTP) endpoint that complements the SOAP
 * AgriFlowService. Demonstrates a second integration style within the same
 * backend: instead of a strict WSDL contract + XML envelopes, this is a
 * single GET endpoint that returns a small JSON payload -- useful for data
 * that changes often (like fuel prices) and doesn't need SOAP's overhead.
 *
 * GET /fuel-price-api.php
 *   -> { "success": true, "fuelType": "diesel", "pricePerLiter": 74.03, ... }
 *
 * Data source: gaswatchph.com (Metro Manila fuel price tracker). The page is
 * scraped server-side (no API key required) and the extracted average diesel
 * price is cached to disk so we don't hit the site on every request and so
 * the app still has *something* to show if the site is briefly unreachable.
 */

header("Content-Type: application/json; charset=utf-8");
// Allow the Android emulator / any local client to call this freely.
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, OPTIONS");

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(204);
    exit;
}

const SOURCE_URL = "https://gaswatchph.com/";
const CACHE_FILE = __DIR__ . "/fuel_price_cache.json";
const CACHE_TTL_SECONDS = 3 * 60 * 60; // 3 hours - fuel prices only change weekly anyway
const FALLBACK_PRICE = 60.00; // Last-resort default (matches original app default)

/**
 * Fetches raw HTML from a URL with a short timeout. Uses cURL when available,
 * falling back to a stream context so this still works on minimal PHP installs.
 *
 * @param string $url
 * @return string|null Raw HTML body, or null on failure.
 */
function fetchHtml(string $url): ?string {
    if (function_exists('curl_init')) {
        $ch = curl_init($url);
        curl_setopt_array($ch, [
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_FOLLOWLOCATION => true,
            CURLOPT_CONNECTTIMEOUT => 6,
            CURLOPT_TIMEOUT => 8,
            CURLOPT_USERAGENT => "AgriFlow-FuelPriceBot/1.0 (+school project; contact: agriflow@example.com)",
            CURLOPT_SSL_VERIFYPEER => true,
        ]);
        $body = curl_exec($ch);
        $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        $curlError = curl_error($ch);
        curl_close($ch);

        if ($body === false || $httpCode >= 400) {
            error_log("fuel-price-api: cURL fetch failed ({$httpCode}) {$curlError}");
            return null;
        }
        return $body;
    }

    // Fallback: file_get_contents with a stream context timeout.
    $context = stream_context_create([
        'http' => [
            'method' => 'GET',
            'timeout' => 8,
            'header' => "User-Agent: AgriFlow-FuelPriceBot/1.0 (+school project)\r\n",
        ]
    ]);
    $body = @file_get_contents($url, false, $context);
    return $body !== false ? $body : null;
}

/**
 * Extracts the Metro Manila average diesel price (and an "as of" label, if
 * present) from the gaswatchph.com homepage HTML. The page mixes
 * client-rendered tables with server-rendered SEO/summary text, so we scrape
 * the server-rendered summary sentences instead of trying to run its JS.
 *
 * Several patterns are tried in order of preference (average price first,
 * cheapest-station price as a fallback signal) to stay resilient to minor
 * copy changes on the source site.
 *
 * @param string $html
 * @return array{price: float, asOf: ?string, note: string}|null
 */
function extractDieselPrice(string $html): ?array {
    // Strip tags and collapse whitespace so we can pattern-match on plain
    // sentences regardless of exactly which HTML elements wrap them.
    $text = strip_tags($html);
    $text = html_entity_decode($text, ENT_QUOTES | ENT_HTML5, 'UTF-8');
    $text = preg_replace('/\s+/u', ' ', $text);

    // Pattern 1: "the Metro Manila average diesel price is ₱74.03/L"
    if (preg_match('/Metro Manila average diesel price is\s*₱\s*([0-9]+(?:\.[0-9]+)?)\s*\/\s*L/iu', $text, $m)) {
        $asOf = null;
        if (preg_match('/As of\s+([A-Z][a-z]+\s+\d{1,2},\s*\d{4}),\s*the Metro Manila average diesel/iu', $text, $d)) {
            $asOf = $d[1];
        }
        return ['price' => (float)$m[1], 'asOf' => $asOf, 'note' => 'Metro Manila station-weighted average diesel price'];
    }

    // Pattern 2: "Metro Manila averages are ₱74.03/L for diesel"
    if (preg_match('/Metro Manila averages are\s*₱\s*([0-9]+(?:\.[0-9]+)?)\s*\/\s*L\s*for diesel/iu', $text, $m)) {
        return ['price' => (float)$m[1], 'asOf' => null, 'note' => 'Metro Manila station-weighted average diesel price'];
    }

    // Pattern 3 (fallback): cheapest tracked diesel price, e.g.
    // "the cheapest diesel in Metro Manila is at Flying V (₱70.67/L)"
    if (preg_match('/cheapest diesel in Metro Manila is at[^(]*\(\s*₱\s*([0-9]+(?:\.[0-9]+)?)\s*\/\s*L\s*\)/iu', $text, $m)) {
        return ['price' => (float)$m[1], 'asOf' => null, 'note' => 'Cheapest tracked diesel price in Metro Manila (average unavailable)'];
    }

    return null;
}

/**
 * Reads the on-disk cache, if present.
 */
function readCache(): ?array {
    if (!file_exists(CACHE_FILE)) {
        return null;
    }
    $raw = @file_get_contents(CACHE_FILE);
    if ($raw === false) {
        return null;
    }
    $data = json_decode($raw, true);
    return is_array($data) ? $data : null;
}

/**
 * Writes a fresh scrape result to the on-disk cache.
 */
function writeCache(array $payload): void {
    @file_put_contents(CACHE_FILE, json_encode($payload), LOCK_EX);
}

// --- Main request handling -------------------------------------------------

$cache = readCache();
$cacheIsFresh = $cache !== null && (time() - ($cache['fetchedAtUnix'] ?? 0)) < CACHE_TTL_SECONDS;

if ($cacheIsFresh) {
    $payload = $cache;
    $payload['cacheStatus'] = 'cached';
} else {
    $html = fetchHtml(SOURCE_URL);
    $extracted = $html !== null ? extractDieselPrice($html) : null;

    if ($extracted !== null) {
        $payload = [
            'success' => true,
            'fuelType' => 'diesel',
            'pricePerLiter' => $extracted['price'],
            'currency' => 'PHP',
            'unit' => 'liter',
            'asOfText' => $extracted['asOf'],
            'note' => $extracted['note'],
            'sourceUrl' => SOURCE_URL,
            'fetchedAtUnix' => time(),
            'fetchedAt' => date(DATE_ATOM),
        ];
        writeCache($payload);
        $payload['cacheStatus'] = 'live';
    } elseif ($cache !== null) {
        // Scrape failed but we have an older cached value - better than nothing.
        $payload = $cache;
        $payload['cacheStatus'] = 'stale-cache';
    } else {
        // No live data and no cache at all - last-resort hardcoded default.
        $payload = [
            'success' => true,
            'fuelType' => 'diesel',
            'pricePerLiter' => FALLBACK_PRICE,
            'currency' => 'PHP',
            'unit' => 'liter',
            'asOfText' => null,
            'note' => 'Live scrape unavailable; using built-in default price.',
            'sourceUrl' => SOURCE_URL,
            'fetchedAtUnix' => time(),
            'fetchedAt' => date(DATE_ATOM),
            'cacheStatus' => 'fallback',
        ];
    }
}

echo json_encode($payload, JSON_PRETTY_PRINT | JSON_UNESCAPED_SLASHES);
