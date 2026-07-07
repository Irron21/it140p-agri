<?php
/**
 * AgriFlow SOAP Browser Test Client
 * 
 * An interactive, modern interface to run test SOAP queries against server.php
 * and view SOAP request/response XML payloads.
 */

// Disable caching of WSDL during development
ini_set("soap.wsdl_cache_enabled", "0");

$error = null;
$result = null;
$requestXml = null;
$responseXml = null;
$action = $_POST['action'] ?? null;

// Determine endpoint location (dynamically use current server hostname/port)
$protocol = (!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off') ? "https" : "http";
$host = $_SERVER['HTTP_HOST'] ?? "localhost:8000";
$wsdlUrl = "{$protocol}://{$host}/server.php?wsdl";

if ($action) {
    try {
        $client = new SoapClient('agriflow.wsdl');

        switch ($action) {
            case 'calculateYieldForecast':
                $area = (float)($_POST['yield_area'] ?? 0);
                $temp = (float)($_POST['yield_temp'] ?? 0);
                $ph = (float)($_POST['yield_ph'] ?? 0);
                
                $res = $client->calculateYieldForecast($area, $temp, $ph);
                $result = "Result: " . $res . " metric tons of crop yield forecasted.";
                break;

            case 'calculateFreightPrice':
                $distance = (float)($_POST['freight_distance'] ?? 0);
                $fuelPrice = (float)($_POST['freight_fuel'] ?? 0);
                $weight = (float)($_POST['freight_weight'] ?? 0);
                
                $res = $client->calculateFreightPrice($distance, $fuelPrice, $weight);
                $result = "Result: Quote price is ₱" . number_format($res, 2) . " PHP.";
                break;

            case 'calculateHubCluster':
                $latsStr = $_POST['cluster_lats'] ?? '';
                $lngsStr = $_POST['cluster_lngs'] ?? '';
                $k = (int)($_POST['cluster_k'] ?? 1);

                // Clean commas/whitespace
                $latitudes = array_filter(array_map('trim', explode(',', $latsStr)), 'is_numeric');
                $longitudes = array_filter(array_map('trim', explode(',', $lngsStr)), 'is_numeric');
                
                // Pack into SOAP input structure (matching WSDL types DoubleArray)
                $res = $client->calculateHubCluster(
                    ['value' => array_map('floatval', $latitudes)],
                    ['value' => array_map('floatval', $longitudes)],
                    $k
                );
                
                // Unpack output CoordinateArray
                $coords = [];
                if (isset($res->coordinate)) {
                    if (is_array($res->coordinate)) {
                        foreach ($res->coordinate as $index => $c) {
                            $coords[] = "Hub " . ($index + 1) . " -> Latitude: " . $c->latitude . ", Longitude: " . $c->longitude;
                        }
                    } else {
                        $coords[] = "Hub 1 -> Latitude: " . $res->coordinate->latitude . ", Longitude: " . $res->coordinate->longitude;
                    }
                }
                $result = "Result: Clustered Centroids Calculated:\n" . (empty($coords) ? "No clusters generated." : implode("\n", $coords));
                break;

            case 'calculateCarbonFootprint':
                $efficiency = (float)($_POST['carbon_efficiency'] ?? 0);
                $distance = (float)($_POST['carbon_distance'] ?? 0);
                $weight = (float)($_POST['carbon_weight'] ?? 0);
                
                $res = $client->calculateCarbonFootprint($efficiency, $distance, $weight);
                $result = "Result: Carbon footprint is " . number_format($res, 4) . " kg of CO2.";
                break;
        }

        // Retrieve raw XML for debugging presentation
        $requestXml = formatXmlString($client->__getLastRequest());
        $responseXml = formatXmlString($client->__getLastResponse());
    } catch (Exception $e) {
        $error = "SOAP Fault Exception: " . $e->getMessage();
        if (isset($client)) {
            $requestXml = formatXmlString($client->__getLastRequest() ?? '');
            $responseXml = formatXmlString($client->__getLastResponse() ?? '');
        }
    }
}

/**
 * Format raw XML string for readable display.
 */
function formatXmlString($xmlString) {
    if (empty($xmlString)) return "";
    $dom = new DOMDocument('1.0');
    $dom->preserveWhiteSpace = false;
    $dom->formatOutput = true;
    @$dom->loadXML($xmlString);
    return $dom->saveXML();
}
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>AgriFlow - SOAP Backend Test Client</title>
    <!-- Modern typography -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@300;400;500;600;700&family=JetBrains+Mono:wght@400;500&display=swap" rel="stylesheet">
    <style>
        :root {
            --bg-color: #0b1311;
            --card-bg: #121f1b;
            --primary: #10b981;
            --primary-hover: #059669;
            --accent: #34d399;
            --text-main: #f3f4f6;
            --text-muted: #9ca3af;
            --border: #223730;
            --xml-bg: #090e0c;
        }

        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
        }

        body {
            font-family: 'Plus Jakarta Sans', sans-serif;
            background-color: var(--bg-color);
            color: var(--text-main);
            padding: 40px 20px;
            line-height: 1.6;
        }

        .container {
            max-width: 1200px;
            margin: 0 auto;
        }

        header {
            text-align: center;
            margin-bottom: 40px;
        }

        header h1 {
            font-size: 2.5rem;
            font-weight: 700;
            color: var(--primary);
            margin-bottom: 10px;
            letter-spacing: -0.025em;
        }

        header p {
            color: var(--text-muted);
            font-size: 1.1rem;
        }

        .server-status {
            display: inline-block;
            background-color: var(--card-bg);
            border: 1px solid var(--border);
            padding: 8px 16px;
            border-radius: 30px;
            font-size: 0.9rem;
            color: var(--accent);
            margin-top: 15px;
            font-family: 'JetBrains Mono', monospace;
        }

        .grid {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 30px;
        }

        @media (max-width: 900px) {
            .grid {
                grid-template-columns: 1fr;
            }
        }

        .panel {
            background-color: var(--card-bg);
            border: 1px solid var(--border);
            border-radius: 16px;
            padding: 25px;
            box-shadow: 0 10px 30px rgba(0,0,0,0.3);
        }

        .panel h2 {
            font-size: 1.4rem;
            margin-bottom: 20px;
            color: var(--text-main);
            border-bottom: 1px solid var(--border);
            padding-bottom: 10px;
        }

        .form-section {
            margin-bottom: 30px;
            background: rgba(255, 255, 255, 0.02);
            padding: 20px;
            border-radius: 12px;
            border: 1px solid rgba(16, 185, 129, 0.05);
        }

        .form-section h3 {
            font-size: 1.1rem;
            color: var(--accent);
            margin-bottom: 15px;
        }

        .form-group {
            margin-bottom: 15px;
        }

        label {
            display: block;
            font-size: 0.85rem;
            color: var(--text-muted);
            margin-bottom: 5px;
            font-weight: 500;
        }

        input[type="text"], input[type="number"], textarea {
            width: 100%;
            background-color: rgba(0, 0, 0, 0.2);
            border: 1px solid var(--border);
            border-radius: 8px;
            padding: 10px 12px;
            color: var(--text-main);
            font-family: inherit;
            font-size: 0.95rem;
            transition: all 0.3s ease;
        }

        input[type="text"]:focus, input[type="number"]:focus, textarea:focus {
            outline: none;
            border-color: var(--primary);
            box-shadow: 0 0 0 2px rgba(16, 185, 129, 0.2);
        }

        textarea {
            resize: vertical;
            min-height: 80px;
        }

        .row {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 15px;
        }

        button {
            display: inline-block;
            width: 100%;
            background-color: var(--primary);
            color: #ffffff;
            border: none;
            border-radius: 8px;
            padding: 12px;
            font-size: 0.95rem;
            font-weight: 600;
            cursor: pointer;
            transition: background-color 0.2s ease;
            margin-top: 10px;
        }

        button:hover {
            background-color: var(--primary-hover);
        }

        .results-panel {
            display: flex;
            flex-direction: column;
            gap: 20px;
        }

        .result-box {
            background: rgba(16, 185, 129, 0.08);
            border: 1px solid var(--primary);
            padding: 15px;
            border-radius: 8px;
            white-space: pre-wrap;
            font-family: 'JetBrains Mono', monospace;
            font-size: 0.95rem;
        }

        .error-box {
            background: rgba(239, 68, 68, 0.1);
            border: 1px solid #ef4444;
            padding: 15px;
            border-radius: 8px;
            color: #fca5a5;
            font-family: 'JetBrains Mono', monospace;
            font-size: 0.9rem;
        }

        .xml-viewer {
            display: flex;
            flex-direction: column;
            gap: 15px;
        }

        .xml-box {
            background-color: var(--xml-bg);
            border: 1px solid var(--border);
            border-radius: 8px;
            padding: 15px;
            overflow-x: auto;
        }

        .xml-box h4 {
            font-size: 0.85rem;
            color: var(--text-muted);
            margin-bottom: 8px;
            border-bottom: 1px solid var(--border);
            padding-bottom: 4px;
            text-transform: uppercase;
            letter-spacing: 0.05em;
        }

        pre {
            font-family: 'JetBrains Mono', monospace;
            font-size: 0.8rem;
            color: #a7f3d0;
            line-height: 1.4;
        }

        .helper-btn {
            background: #1d332d;
            border: 1px solid var(--border);
            color: var(--accent);
            padding: 4px 8px;
            border-radius: 4px;
            font-size: 0.75rem;
            cursor: pointer;
            margin-top: 5px;
            display: inline-block;
            text-decoration: none;
            text-align: center;
        }

        .helper-btn:hover {
            background: #25443c;
        }
    </style>
    <script>
        function fillSampleCoordinates() {
            // Fill coordinates for sample farms around clean regions
            document.getElementById('cluster_lats').value = "14.5995, 14.6042, 14.5826, 14.6188, 14.5901";
            document.getElementById('cluster_lngs').value = "120.9842, 120.9733, 121.0125, 121.0344, 120.9991";
            document.getElementById('cluster_k').value = "2";
        }
    </script>
</head>
<body>
    <div class="container">
        <header>
            <h1>AgriFlow Logistical Service Suite</h1>
            <p>SOAP Backend Interface Diagnostic Console</p>
            <div class="server-status">
                WSDL Target: <a href="<?php echo htmlspecialchars($wsdlUrl); ?>" target="_blank" style="color: var(--accent);"><?php echo htmlspecialchars($wsdlUrl); ?></a>
            </div>
        </header>

        <div class="grid">
            <!-- Left Panel: SOAP Action Inputs -->
            <div class="panel">
                <h2>SOAP Web Service Transactions</h2>

                <!-- 1. Yield Forecast -->
                <div class="form-section">
                    <h3>Calculate Yield Forecast</h3>
                    <form method="POST" action="">
                        <input type="hidden" name="action" value="calculateYieldForecast">
                        <div class="row">
                            <div class="form-group">
                                <label for="yield_area">Cultivation Area (Hectares)</label>
                                <input type="number" step="0.01" name="yield_area" id="yield_area" value="120.5" required>
                            </div>
                            <div class="form-group">
                                <label for="yield_temp">Mean Temperature (°C)</label>
                                <input type="number" step="0.1" name="yield_temp" id="yield_temp" value="26.8" required>
                            </div>
                        </div>
                        <div class="form-group">
                            <label for="yield_ph">Soil pH Level</label>
                            <input type="number" step="0.1" name="yield_ph" id="yield_ph" value="6.2" required>
                        </div>
                        <button type="submit">Submit calculateYieldForecast</button>
                    </form>
                </div>

                <!-- 2. Freight Price -->
                <div class="form-section">
                    <h3>Calculate Freight Price</h3>
                    <form method="POST" action="">
                        <input type="hidden" name="action" value="calculateFreightPrice">
                        <div class="row">
                            <div class="form-group">
                                <label for="freight_distance">Logistics Distance (km)</label>
                                <input type="number" step="0.1" name="freight_distance" id="freight_distance" value="380.0" required>
                            </div>
                            <div class="form-group">
                                <label for="freight_fuel">Fuel Price (₱/Liter)</label>
                                <input type="number" step="0.01" name="freight_fuel" id="freight_fuel" value="60.00" required>
                            </div>
                        </div>
                        <div class="form-group">
                            <label for="freight_weight">Freight Load Weight (Metric Tons)</label>
                            <input type="number" step="0.1" name="freight_weight" id="freight_weight" value="18.5" required>
                        </div>
                        <button type="submit">Submit calculateFreightPrice</button>
                    </form>
                </div>

                <!-- 3. Hub Cluster -->
                <div class="form-section">
                    <h3>Calculate Hub Cluster (K-Means)</h3>
                    <form method="POST" action="">
                        <input type="hidden" name="action" value="calculateHubCluster">
                        <div class="form-group">
                            <label for="cluster_lats">Latitudes (Comma separated)</label>
                            <input type="text" name="cluster_lats" id="cluster_lats" value="14.5995, 14.6042, 14.5826" required>
                        </div>
                        <div class="form-group">
                            <label for="cluster_lngs">Longitudes (Comma separated)</label>
                            <input type="text" name="cluster_lngs" id="cluster_lngs" value="120.9842, 120.9733, 121.0125" required>
                        </div>
                        <div class="form-group">
                            <label for="cluster_k">Centroids Target Count (K)</label>
                            <input type="number" name="cluster_k" id="cluster_k" value="2" min="1" required>
                        </div>
                        <button type="button" class="helper-btn" onclick="fillSampleCoordinates()">Load 5 Farm Coordinates</button>
                        <button type="submit">Submit calculateHubCluster</button>
                    </form>
                </div>

                <!-- 4. Carbon Footprint -->
                <div class="form-section">
                    <h3>Calculate Carbon Footprint</h3>
                    <form method="POST" action="">
                        <input type="hidden" name="action" value="calculateCarbonFootprint">
                        <div class="row">
                            <div class="form-group">
                                <label for="carbon_efficiency">Efficiency Coefficient (gCO₂ / Ton-km)</label>
                                <input type="number" step="0.1" name="carbon_efficiency" id="carbon_efficiency" value="62.0" required>
                            </div>
                            <div class="form-group">
                                <label for="carbon_distance">Delivery Distance (km)</label>
                                <input type="number" step="0.1" name="carbon_distance" id="carbon_distance" value="150.0" required>
                            </div>
                        </div>
                        <div class="form-group">
                            <label for="carbon_weight">Cargo Mass (Metric Tons)</label>
                            <input type="number" step="0.1" name="carbon_weight" id="carbon_weight" value="8.4" required>
                        </div>
                        <button type="submit">Submit calculateCarbonFootprint</button>
                    </form>
                </div>
            </div>

            <!-- Right Panel: SOAP Envelopes & Diagnostics -->
            <div class="panel results-panel">
                <h2>Execution &amp; SOAP Debug Diagnostics</h2>

                <?php if ($action): ?>
                    <div>
                        <h4 style="margin-bottom: 8px; color: var(--accent);">Action: <?php echo htmlspecialchars($action); ?></h4>
                        <?php if ($error): ?>
                            <div class="error-box"><?php echo htmlspecialchars($error); ?></div>
                        <?php else: ?>
                            <div class="result-box"><?php echo htmlspecialchars($result); ?></div>
                        <?php endif; ?>
                    </div>

                    <div class="xml-viewer">
                        <div class="xml-box">
                            <h4>SOAP XML Request Envelope</h4>
                            <pre><code><?php echo htmlspecialchars($requestXml); ?></code></pre>
                        </div>
                        <div class="xml-box">
                            <h4>SOAP XML Response Envelope</h4>
                            <pre><code><?php echo htmlspecialchars($responseXml); ?></code></pre>
                        </div>
                    </div>
                <?php else: ?>
                    <p style="color: var(--text-muted); text-align: center; margin-top: 50px;">
                        Submit any form on the left to trigger a SOAP request.<br>
                        The calculated outputs and raw SOAP XML headers/envelopes will populate here in real-time.
                    </p>
                <?php endif; ?>
            </div>
        </div>
    </div>
</body>
</html>
