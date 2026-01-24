<?php
session_start();

if (!isset($_SESSION['user_id'])) {
    header("Location: index.php");
    exit();
}
?>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Dashboard</title>
    <link rel="stylesheet" href="styles.css">
</head>
<body>
    <div class="container">
        <h1>Dashboard</h1>
        <p>Welcome, <?php echo htmlspecialchars($_SESSION['username']); ?>!</p>
        <p>You are successfully logged in.</p>
        <p><a href="logout.php">Logout</a></p>
        
        <div class="dashboard-content">
            <h2>Your Account Information</h2>
            <p>Username: <?php echo htmlspecialchars($_SESSION['username']); ?></p>
            <p>User ID: <?php echo $_SESSION['user_id']; ?></p>
            <p>Login Time: <?php echo date('Y-m-d H:i:s'); ?></p>
        </div>
    </div>
</body>
</html>