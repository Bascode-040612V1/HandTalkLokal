#!/usr/bin/env python3
"""
Simple script to view the latest log file
"""

import os
import glob
import sys
from datetime import datetime

# Add parent directory to path to find logging_config
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

def view_latest_log():
    """View the main log file"""
    # Check for the main handtalk.log file
    main_log = os.path.join("logs", "handtalk.log")
    if not os.path.exists(main_log):
        print("No main log file found!")
        return
    
    print(f"Log file: {main_log}")
    print("=" * 50)
    
    # Display the contents
    try:
        with open(main_log, 'r', encoding='utf-8') as f:
            print(f.read())
    except Exception as e:
        print(f"Error reading log file: {e}")

def list_all_logs():
    """List the main log file with its timestamp"""
    log_dir = "logs"
    if not os.path.exists(log_dir):
        print("No logs directory found!")
        return
    
    main_log = os.path.join(log_dir, "handtalk.log")
    if not os.path.exists(main_log):
        print("No main log file found!")
        return
    
    print("Available log files:")
    print("=" * 50)
    
    # Get modification time of the main log file
    mod_time = datetime.fromtimestamp(os.path.getmtime(main_log))
    formatted_time = mod_time.strftime("%Y-%m-%d %H:%M:%S")
    
    file_size = os.path.getsize(main_log)
    basename = os.path.basename(main_log)
    print(f"{formatted_time} - {basename} ({file_size} bytes)")

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