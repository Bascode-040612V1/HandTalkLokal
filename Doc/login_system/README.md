# Login/Register System

A simple login and registration system built with PHP and MySQL using XAMPP.

## Requirements

- XAMPP (Apache, MySQL, PHP)
- Web browser

## Installation

1. Place all files in your XAMPP `htdocs` folder (e.g., `C:\xampp\htdocs\login_system\`)
2. Start Apache and MySQL from XAMPP Control Panel
3. Access the application via `http://localhost/login_system/`

## Database Configuration

### Option 1: Automatic Setup (Recommended)

The application will automatically create the database and table when accessed for the first time.

### Option 2: Manual Database Import

1. Open phpMyAdmin (http://localhost/phpmyadmin)
2. Click "Import" tab
3. Choose the `database_schema.sql` file from this folder
4. Click "Go" to import
5. Optionally import `sample_data.sql` for test accounts

Default database configuration:
- Server: localhost
- Username: root
- Password: (empty by default in XAMPP)
- Database: login_system

## Making the Application Accessible from Mobile Devices

To access your application from mobile devices on the same network:

### Step 1: Find Your Computer's IP Address

Windows:
1. Open Command Prompt
2. Run: `ipconfig`
3. Look for "IPv4 Address" under your active network adapter (usually looks like 192.168.x.x or 10.x.x.x)

### Step 2: Configure Firewall

Allow Apache through Windows Firewall:
1. Go to Windows Defender Firewall
2. Click "Allow an app or feature through Windows Defender Firewall"
3. Click "Change settings" and scroll to find Apache HTTP Server
4. Make sure both Private and Public checkboxes are checked

### Step 3: Update Apache Configuration (Optional)

If needed, you can allow external connections by editing Apache configuration:
1. In XAMPP Control Panel, click "Config" next to Apache
2. Select "httpd-xampp.conf"
3. Look for the `<Directory "C:/xampp/htdocs">` section
4. Change `Require local` to `Require all granted` in the appropriate sections
5. Restart Apache

### Step 4: Access from Mobile Device

On your mobile device's browser, enter:
```
http://YOUR_COMPUTER_IP_ADDRESS/xampp_folder_name/
```

For example:
```
http://192.168.1.100/login_system/
```

## Security Notes

- This is a basic implementation for learning purposes
- For production, implement additional security measures like CSRF protection
- Change default credentials when deploying to production
- Consider using HTTPS in production environments

## SQL Files Included

- `database_schema.sql` - Contains the database structure and table creation
- `sample_data.sql` - Contains sample user accounts for testing

## Features

- User registration with validation
- Secure password hashing
- Session-based authentication
- Responsive design
- Clean, modern UI

## Troubleshooting

- If you get a "Connection Refused" error on mobile, check firewall settings
- Ensure both devices are on the same network
- Verify Apache and MySQL are running in XAMPP
- Check that ports 80 and 443 are not blocked