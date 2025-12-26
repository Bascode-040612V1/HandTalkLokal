#!/usr/bin/env python3
"""
Simple script to view the latest log file
"""

import os
import glob
from datetime import datetime

def view_latest_log():
    """View the most recent log file"""
    # Find all log files
    log_dir = "logs"
    if not os.path.exists(log_dir):
        print("No logs directory found!")
        return
    
    log_files = glob.glob(os.path.join(log_dir, "handtalk_*.log"))
    
    if not log_files:
        print("No log files found!")
        return
    
    # Sort by modification time (newest first)
    log_files.sort(key=os.path.getmtime, reverse=True)
    
    # Get the most recent log file
    latest_log = log_files[0]
    
    print(f"Latest log file: {latest_log}")
    print("=" * 50)
    
    # Display the contents
    try:
        with open(latest_log, 'r') as f:
            print(f.read())
    except Exception as e:
        print(f"Error reading log file: {e}")

def list_all_logs():
    """List all log files with their timestamps"""
    log_dir = "logs"
    if not os.path.exists(log_dir):
        print("No logs directory found!")
        return
    
    log_files = glob.glob(os.path.join(log_dir, "handtalk_*.log"))
    
    if not log_files:
        print("No log files found!")
        return
    
    # Sort by modification time (newest first)
    log_files.sort(key=os.path.getmtime, reverse=True)
    
    print("Available log files:")
    print("=" * 50)
    
    for log_file in log_files:
        # Extract timestamp from filename
        try:
            timestamp_str = os.path.basename(log_file).replace("handtalk_", "").replace(".log", "")
            timestamp = datetime.strptime(timestamp_str, "%Y%m%d_%H%M%S")
            formatted_time = timestamp.strftime("%Y-%m-%d %H:%M:%S")
        except:
            formatted_time = "Unknown"
        
        file_size = os.path.getsize(log_file)
        print(f"{formatted_time} - {os.path.basename(log_file)} ({file_size} bytes)")

def main():
    """Main function"""
    print("HandTalk Log Viewer")
    print("=" * 20)
    print()
    
    while True:
        print("Options:")
        print("1. View latest log")
        print("2. List all logs")
        print("3. Exit")
        print()
        
        choice = input("Enter your choice (1-3): ").strip()
        
        if choice == '1':
            view_latest_log()
        elif choice == '2':
            list_all_logs()
        elif choice == '3':
            break
        else:
            print("Invalid choice!")
        
        print()
        input("Press Enter to continue...")
        print()

if __name__ == "__main__":
    main()