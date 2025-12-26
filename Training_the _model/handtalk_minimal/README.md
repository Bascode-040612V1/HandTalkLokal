# HandTalk Minimal System

## Overview
This is the minimal version of the HandTalk system with only essential functionality:
- Hand tracking and gesture collection with auto-save
- Real-time gesture recognition
- View all saved gestures
- Single batch file interface
- Comprehensive logging for debugging and monitoring

## Usage
Run `handtalk.bat` and select from the menu:
1. Collect Hand Gesture Data (Auto-Save)
2. Real-time Gesture Recognition
3. See All Gestures
4. Exit

## Logging
The system now includes comprehensive logging to help diagnose issues and monitor performance:
- Logs are stored in the `logs` directory
- Each session creates a new log file with a timestamp
- Log levels include INFO, WARNING, and ERROR
- Logs are written to both file and console

## Requirements
- Python 3.7+
- OpenCV, MediaPipe, Scikit-learn, Pandas, Joblib, NumPy

Install with: `pip install -r requirements.txt`