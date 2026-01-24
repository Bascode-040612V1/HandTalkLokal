@echo off
chcp 65001 >nul 2>&1
setlocal EnableDelayedExpansion

echo.
echo =============================================
echo  Hand Gesture Data Collection for HandTalk  
echo =============================================
echo.

REM Change to the script directory
cd /d "%~dp0"

echo Current directory: %cd%
echo.

REM Check if Python is installed
python --version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Python is not installed or not in PATH.
    echo Please install Python 3.7 or higher and add it to PATH.
    pause
    exit /b 1
)

REM Check if required Python packages are installed
echo Checking for required Python packages...
python -c "import cv2, mediapipe, numpy, pandas" >nul 2>&1
if errorlevel 1 (
    echo Installing required Python packages...
    pip install opencv-python mediapipe numpy pandas
    if errorlevel 1 (
        echo ERROR: Failed to install required packages.
        pause
        exit /b 1
    )
)

echo.
echo =============================================
echo  Available Options:
echo  1. Collect Hand Gesture Data (Open Camera)
echo  2. View Current Gestures in Dataset
echo  3. View Training Logs
echo  4. Retrain Model with New Data
echo  0. Exit
echo =============================================
echo.

set /p choice="Enter your choice (0-4): "

if "!choice!"=="0" goto :exit
if "!choice!"=="1" goto :collect_data
if "!choice!"=="2" goto :view_gestures
if "!choice!"=="3" goto :view_logs
if "!choice!"=="4" goto :retrain_model

echo Invalid choice. Please enter 0-4.
pause
goto :eof

:collect_data
echo.
echo =============================================
echo  Starting Hand Gesture Data Collection
echo  Getting ready to open camera...
echo =============================================
echo.
echo Please prepare your gesture.
echo The script will ask for a gesture name and then start a 3-second countdown.
echo.
python collect_data_bimanual.py
if errorlevel 1 (
    echo ERROR: Data collection failed.
) else (
    echo Data collection completed successfully.
    echo New data has been added to gestures_bimanual.csv
)
pause
goto :eof

:view_gestures
echo.
echo =============================================
echo  Viewing Current Gesture Dataset
echo =============================================
echo.
if exist "data\gestures_bimanual.csv" (
    echo Current gesture data file exists.
    echo First few rows:
    python -c "import pandas as pd; df = pd.read_csv('data/gestures_bimanual.csv'); print('Dataset shape:', df.shape); print('\nFirst 5 rows:'); print(df.head()); print('\nUnique labels:', df['label'].unique())"
) else (
    echo No gesture data file found. Please collect some data first.
)
pause
goto :eof

:view_logs
echo.
echo =============================================
echo  Viewing Training Logs
echo =============================================
echo.
if exist "logs" (
    dir logs /b
    echo.
    if exist "logs\handtalk.log" (
        echo Last 20 lines of log:
        powershell -Command "(Get-Content logs\handtalk.log)[-20..-1]"
    )
) else (
    echo No logs directory found.
)
pause
goto :eof

:retrain_model
echo.
echo =============================================
echo  Retraining Model with New Data
echo =============================================
echo.
python retrain_model.py
if errorlevel 1 (
    echo ERROR: Model retraining failed.
) else (
    echo Model retraining completed successfully.
)
pause
goto :eof

:exit
echo Goodbye!
endlocal