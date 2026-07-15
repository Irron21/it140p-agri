<?php
header("Content-Type: application/json; charset=utf-8");
header("Access-Control-Allow-Origin: *");

$host = '127.0.0.1';
$db   = 'agriflow_db';
$user = 'root';
$pass = '';
$charset = 'utf8mb4';

$dsn = "mysql:host=$host;dbname=$db;charset=$charset";
$options = [PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION, PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC];

try {
    $pdo = new PDO($dsn, $user, $pass, $options);
} catch (\PDOException $e) {
    echo json_encode(["success" => false, "error" => "Connection failed"]);
    exit;
}

$query = "SELECT * FROM yield_history";
$where = [];
$params = [];

if (!empty($_GET['days'])) {
    $where[] = "timestamp >= DATE_SUB(NOW(), INTERVAL ? DAY)";
    $params[] = (int)$_GET['days'];
}
if (isset($_GET['min_yield'])) {
    $where[] = "forecasted_yield >= ?";
    $params[] = (float)$_GET['min_yield'];
}
if (isset($_GET['min_temp'])) {
    $where[] = "temperature >= ?";
    $params[] = (float)$_GET['min_temp'];
}
if (isset($_GET['max_temp'])) {
    $where[] = "temperature <= ?";
    $params[] = (float)$_GET['max_temp'];
}
if (isset($_GET['min_ph'])) {
    $where[] = "ph_level >= ?";
    $params[] = (float)$_GET['min_ph'];
}
if (isset($_GET['max_ph'])) {
    $where[] = "ph_level <= ?";
    $params[] = (float)$_GET['max_ph'];
}

if (!empty($where)) { $query .= " WHERE " . implode(" AND ", $where); }

$sort = $_GET['sort'] ?? 'date_desc';
if ($sort === 'yield_desc') { $query .= " ORDER BY forecasted_yield DESC"; }
else { $query .= " ORDER BY timestamp DESC"; }

$stmt = $pdo->prepare($query);
$stmt->execute($params);
echo json_encode(["success" => true, "data" => $stmt->fetchAll()]);
?>
