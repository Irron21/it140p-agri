<?php
/**
 * AgriFlow SOAP Web Service Server
 * 
 * Implements agricultural logistics calculations including K-Means Clustering,
 * Yield Forecasting, Freight Pricing, and Carbon Footprint estimations.
 */

// Disable caching of WSDL during development
ini_set("soap.wsdl_cache_enabled", "0");

// Serve the WSDL content directly if requested via GET '?wsdl'
if (isset($_GET['wsdl'])) {
    header("Content-Type: text/xml; charset=utf-8");
    readfile("agriflow.wsdl");
    exit;
}

/**
 * Service class containing business logic for AgriFlow computations.
 */
class AgriFlowService {
    
    /**
     * Calculates the estimated crop yield forecast based on environmental criteria.
     * Uses a double-bell-curve model to penalize deviation from optimal temperature and pH.
     * 
     * @param float $area Crop cultivation area in hectares.
     * @param float $temp Average temperature in Celsius.
     * @param float $ph Soil pH level.
     * @return float Forecasted yield in metric tons.
     */
    public function calculateYieldForecast($area, $temp, $ph) {
        $area = (float)$area;
        $temp = (float)$temp;
        $ph = (float)$ph;
        
        // Base productivity coefficient (tons per hectare under optimal conditions)
        $baseProductivity = 4.2;
        
        // Optimal temperature bell curve (Optimal: 25C, standard deviation approximation)
        $tempOptimal = 25.0;
        $tempFactor = exp(-pow($temp - $tempOptimal, 2) / 100.0); // drops off outside [15, 35]
        
        // Optimal soil pH bell curve (Optimal: 6.5, standard deviation approximation)
        $phOptimal = 6.5;
        $phFactor = exp(-pow($ph - $phOptimal, 2) / 2.0); // drops off outside [5.0, 8.0]
        
        $forecastedYield = $area * $baseProductivity * $tempFactor * $phFactor;
        
        return round(max(0.0, $forecastedYield), 4);
    }

    /**
     * Calculates the shipping rate quote based on distance, fuel costs, and freight weight.
     * 
     * @param float $distance Shipping distance in kilometers.
     * @param float $fuelPrice Local fuel price per liter.
     * @param float $weight Cargo weight in metric tons.
     * @return float Estimated freight price in USD.
     */
    public function calculateFreightPrice($distance, $fuelPrice, $weight) {
        $distance = (float)$distance;
        $fuelPrice = (float)$fuelPrice; // Local diesel price per liter (e.g. 60 PHP)
        $weight = (float)$weight; // Weight in tons
        
        $baseRate = 2500.00; // Base flat fare dispatcher fee in PHP
        
        // Exact formula: Base (2500) + ((Distance / 3.5) * FuelPrice) + (1.50 * Weight * Distance)
        $totalPrice = $baseRate + (($distance / 3.5) * $fuelPrice) + (1.50 * $weight * $distance);
        
        return round($totalPrice, 2);
    }

    /**
     * Performs a K-Means Clustering algorithm on arrays of coordinates (lat, lng)
     * to determine the optimal centers (centroids) for 'k' logistic hubs.
     * 
     * @param stdClass $latitudesInput Object containing sequence array of latitudes.
     * @param stdClass $longitudesInput Object containing sequence array of longitudes.
     * @param int $k Number of clusters / hubs to identify.
     * @return array Array structure matching CoordinateArray.
     */
    public function calculateHubCluster($latitudesInput, $longitudesInput, $k) {
        $latitudes = $this->normalizeArray($latitudesInput);
        $longitudes = $this->normalizeArray($longitudesInput);
        $k = (int)$k;

        $n = count($latitudes);
        if ($n === 0 || count($longitudes) !== $n || $k <= 0) {
            return ['coordinate' => []];
        }

        // Pair points
        $points = [];
        for ($i = 0; $i < $n; $i++) {
            $points[] = ['lat' => $latitudes[$i], 'lng' => $longitudes[$i]];
        }

        // Keep K bounded to number of coordinates
        $k = min($k, $n);

        // Initialize centroids by spacing them evenly across data indices
        $centroids = [];
        for ($i = 0; $i < $k; $i++) {
            $index = (int)floor($i * ($n / $k));
            $centroids[$i] = $points[$index];
        }

        // Perform K-Means iterations (heavy computation)
        $maxIterations = 20;
        for ($iter = 0; $iter < $maxIterations; $iter++) {
            $groups = array_fill(0, $k, []);
            
            // Step 1: Assign each point to the closest centroid
            foreach ($points as $p) {
                $minDistance = INF;
                $closestCentroid = 0;
                for ($c = 0; $c < $k; $c++) {
                    // Calculate squared Euclidean distance
                    $dist = pow($p['lat'] - $centroids[$c]['lat'], 2) + pow($p['lng'] - $centroids[$c]['lng'], 2);
                    if ($dist < $minDistance) {
                        $minDistance = $dist;
                        $closestCentroid = $c;
                    }
                }
                $groups[$closestCentroid][] = $p;
            }

            // Step 2: Recalculate centroids
            $centroidsChanged = false;
            for ($c = 0; $c < $k; $c++) {
                if (count($groups[$c]) > 0) {
                    $sumLat = 0;
                    $sumLng = 0;
                    foreach ($groups[$c] as $p) {
                        $sumLat += $p['lat'];
                        $sumLng += $p['lng'];
                    }
                    $newCentroid = [
                        'lat' => $sumLat / count($groups[$c]),
                        'lng' => $sumLng / count($groups[$c])
                    ];
                    
                    if (abs($centroids[$c]['lat'] - $newCentroid['lat']) > 0.00001 ||
                        abs($centroids[$c]['lng'] - $newCentroid['lng']) > 0.00001) {
                        $centroids[$c] = $newCentroid;
                        $centroidsChanged = true;
                    }
                }
            }
            
            // Early exit if centroids have converged
            if (!$centroidsChanged) {
                break;
            }
        }

        // Format return payload matching CoordinateArray structure
        $result = [];
        foreach ($centroids as $c) {
            $result[] = [
                'latitude' => round($c['lat'], 6),
                'longitude' => round($c['lng'], 6)
            ];
        }

        return ['coordinate' => $result];
    }

    /**
     * Calculates the estimated CO2 emissions of cargo transport in kg.
     * 
     * @param float $efficiency CO2 emission efficiency (grams CO2 per ton-km).
     * @param float $distance Transport distance in kilometers.
     * @param float $weight Cargo weight in metric tons.
     * @return float CO2 footprint in kilograms.
     */
    public function calculateCarbonFootprint($efficiency, $distance, $weight) {
        $efficiency = (float)$efficiency;
        $distance = (float)$distance;
        $weight = (float)$weight;
        
        // Total emissions in grams = efficiency * distance * weight
        // Convert to kg by dividing by 1000
        $emissionsKg = ($efficiency * $distance * $weight) / 1000.0;
        
        return round(max(0.0, $emissionsKg), 4);
    }

    /**
     * Normalizes the SOAP array wrapper into a standard PHP flat array.
     * 
     * @param mixed $soapInput Input parsed from SOAP XML.
     * @return array Native numeric array of floats.
     */
    private function normalizeArray($soapInput) {
        if (!isset($soapInput->value)) {
            return [];
        }
        if (is_array($soapInput->value)) {
            return array_map('floatval', $soapInput->value);
        }
        return [(float)$soapInput->value];
    }
}

// Initialize SoapServer with WSDL path and settings
$server = new SoapServer("agriflow.wsdl", [
    'uri' => 'http://localhost:8000/server.php',
    'soap_version' => SOAP_1_1,
    'cache_wsdl' => WSDL_CACHE_NONE
]);
$server->setClass('AgriFlowService');

// Handle request
$server->handle();
?>
