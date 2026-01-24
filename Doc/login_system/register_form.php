<form method="POST" action="register_process.php" class="auth-form">
    <h2>Register</h2>
    <div class="form-group">
        <label for="register_username">Username:</label>
        <input type="text" id="register_username" name="username" required>
    </div>
    <div class="form-group">
        <label for="register_email">Email:</label>
        <input type="email" id="register_email" name="email" required>
    </div>
    <div class="form-group">
        <label for="register_password">Password:</label>
        <input type="password" id="register_password" name="password" required>
    </div>
    <div class="form-group">
        <label for="confirm_password">Confirm Password:</label>
        <input type="password" id="confirm_password" name="confirm_password" required>
    </div>
    <button type="submit">Register</button>
</form>