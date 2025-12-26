@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

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

:: Use timeout to automatically select option 1 if no input is given within 5 seconds
echo Waiting 5 seconds for input, or press a number key (1-7)...
choice /c 1234567 /t 5 /d 1 /n >nul
set choice=%errorlevel%

if "%choice%"=="1" set choice=1
if "%choice%"=="2" set choice=2
if "%choice%"=="3" set choice=3
if "%choice%"=="4" set choice=4
if "%choice%"=="5" set choice=5
if "%choice%"=="6" set choice=6
if "%choice%"=="7" goto end

echo You selected option %choice%
timeout /t 1 /nobreak >nul

if "%choice%"=="1" goto collect_data
if "%choice%"=="2" goto recognize
if "%choice%"=="3" goto recognize_dialect
if "%choice%"=="4" goto see_gestures
if "%choice%"=="5" goto view_logs
if "%choice%"=="6" goto retrain_model
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
python collect_data_bimanual.py
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
python recognize_gestures_bimanual.py
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
python recognize_gestures_bimanual.py
echo.
echo Recognition session ended.
echo.
goto menu

:see_gestures
cls
echo Displaying all saved gestures...
echo.
python view_gestures.py
echo.
goto menu

:view_logs
cls
echo Viewing system logs...
echo.
python view_logs.py
goto menu

:retrain_model
cls
echo Retraining model with current data...
echo.
echo This will retrain the gesture recognition model using all data in your dataset.
echo.
python retrain_model.py
echo.
pause
goto menu

:end
exit