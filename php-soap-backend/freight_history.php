<?php
header("Content-Type: application/json; charset=utf-8");
header("Access-Control-Allow-Origin: *");

$host = '127.0.0.1'; $db = 'agriflow_db'; $user = 'root'; $pass = '';
$dsn = "mysql:host=$host;dbname=$db;charset=utf8mb4";
$pdo = new PDO($dsn, $user, $pass, [PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION, PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC]);

$query = "SELECT * FROM freight_history";
$where = []; $params = [];

if (!empty($_GET['days'])) {
    $where[] = "timestamp >= DATE_SUB(NOW(), INTERVAL ? DAY)";
    $params[] = (int)$_GET['days'];
}
if (isset($_GET['min_cost'])) {
    $where[] = "result_cost >= ?";
    $params[] = (float)$_GET['min_cost'];
}
if (isset($_GET['min_dist'])) {
    $where[] = "distance >= ?";
    $params[] = (float)$_GET['min_dist'];
}
if (isset($_GET['max_dist'])) {
    $where[] = "distance <= ?";
    $params[] = (float)$_GET['max_dist'];
}

if (!empty($where)) { $query .= " WHERE " . implode(" AND ", $where); }

$sort = $_GET['sort'] ?? 'date_desc';
if ($sort === 'cost_desc') { $query .= " ORDER BY result_cost DESC"; }
else { $query .= " ORDER BY timestamp DESC"; }

$stmt = $pdo->prepare($query);
$stmt->execute($params);
echo json_encode(["success" => true, "data" => $stmt->fetchAll()]);
?>
