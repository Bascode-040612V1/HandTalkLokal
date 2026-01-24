    @echo off
    chcp 65001 >nul
    cd /d "%~dp0"
    setlocal enabledelayedexpansion
    
    :: Check if Python is available
    python --version >nul 2>&1
    if errorlevel 1 (
      echo Python not found in PATH
      echo Please install Python and add it to your PATH environment variable
      timeout /t 3 /nobreak >nul
      exit /b
    )

    :: Minimal HandTalk Batch File
    :: Contains only essential functionality

    title HandTalk Minimal

    :menu
    cls
    echo ╔══════════════════════════════════════════════════════════════╗
    echo ║                    HANDTALK MINIMAL                         ║
    echo ╚══════════════════════════════════════════════════════════════╝
    echo.
    echo Select an option:
    echo.
    echo 1. Collect Hand Gesture Data (Auto-Save)
    echo 2. Real-time Gesture Recognition
    echo 3. Real-time Gesture Recognition with Dialect Selection
    echo 4. See All Gestures
    echo 5. View Logs
    echo 6. Retrain Model with Current Data
    echo 7. Exit
    echo.

    :: Allow user to select an option without auto-selection
    echo.
    echo Please select an option (1-7):
    choice /c 1234567 /n /m "Select an option: "
    set "menuChoice=!errorlevel!"

    echo You selected option !menuChoice!
    if "!menuChoice!"=="1" goto collect_data
    if "!menuChoice!"=="2" goto recognize
    if "!menuChoice!"=="3" goto recognize_dialect
    if "!menuChoice!"=="4" goto see_gestures
    if "!menuChoice!"=="5" goto view_logs
    if "!menuChoice!"=="6" goto retrain_model
    if "!menuChoice!"=="7" goto end

    goto menu

    :collect_data
    cls
    echo Starting hand gesture data collection...
    echo.
    echo Instructions:
    echo 1. Show both hands to the camera
    echo 2. Press 's' to start recording
    echo 3. Perform your gesture
    echo 4. Press 'q' to quit
    echo.
    echo Running data collection... Close the camera window when finished.
    echo.
    python scripts\collect_data_bimanual.py
    echo.
    echo Data collection completed and automatically saved.
    echo.
    goto menu

    :recognize
    cls
    echo Starting real-time gesture recognition...
    echo.
    echo Instructions:
    echo 1. Show both hands to the camera
    echo 2. Perform gestures naturally
    echo 3. Press 'q' to quit
    echo.
    echo Running gesture recognition... Close the camera window when finished.
    echo.
    python scripts\recognize_gestures_bimanual.py
    echo.
    echo Recognition session ended.
    echo.
    goto menu

    :recognize_dialect
    cls
    echo Starting real-time gesture recognition with dialect selection...
    echo.
    echo Available dialects:
    echo 1. English
    echo 2. Filipino
    echo 3. Cebuano
    echo 4. Hiligaynon
    echo 5. Maranao
    echo.
    echo Instructions:
    echo 1. Show both hands to the camera
    echo 2. Perform gestures naturally
    echo 3. Press 'q' to quit
    echo.
    echo Running gesture recognition with dialect selection... Close the camera window when finished.
    echo.
    python scripts\recognize_gestures_bimanual.py
    echo.
    echo Recognition session ended.
    echo.
    goto menu

    :see_gestures
    cls
    echo Displaying all saved gestures...
    echo.
    python scripts\view_gestures.py
    echo.
    goto menu

    :view_logs
    cls
    echo Viewing system logs...
    echo.
    python scripts\view_logs.py
    goto menu

    :retrain_model
    cls
    echo Retraining model with current data...
    echo.
    echo This will retrain the gesture recognition model using all data in your dataset.
    echo.
    python scripts\retrain_model.py
    echo.
    goto menu

    :end
    exit