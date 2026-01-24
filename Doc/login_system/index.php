<?php
session_start();
$message = "";

if (isset($_GET['registered']) && $_GET['registered'] == 1) {
    $message = "Registration successful! Please login.";  
}

if (isset($_SESSION['error'])) {
    $message = $_SESSION['error'];
    unset($_SESSION['error']);
}

if (isset($_SESSION['errors'])) {
    $errors = $_SESSION['errors'];
    unset($_SESSION['errors']);
}
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Login/Register System</title>
    <link rel="stylesheet" href="styles.css">
</head>
<body>
    <div class="container">
        <h1>Welcome to Login/Register System</h1>
        
        <?php if (!empty($message)): ?>
            <div class="success"><?php echo htmlspecialchars($message); ?></div>
        <?php endif; ?>
        
        <?php if (isset($errors) && !empty($errors)): ?>
            <div class="error">
                <?php foreach ($errors as $error): ?>
                    <p><?php echo htmlspecialchars($error); ?></p>
                <?php endforeach; ?>
            </div>
        <?php endif; ?>
        
        <?php
        if (isset($_SESSION['user_id'])) {
            echo "<p>Hello, " . htmlspecialchars($_SESSION['username']) . "! <a href='logout.php'>Logout</a></p>";
            echo "<p>You are logged in. <a href='dashboard.php'>Go to Dashboard</a></p>";
        } else {
            echo "<div class='auth-container'>";
            include 'login_form.php';
            echo "<div class='divider'>OR</div>";
            include 'register_form.php';
            echo "</div>";
        }
        ?>
    </div>
</body>
</html>