@echo off
chcp 65001 >nul 2>&1
setlocal EnableDelayedExpansion

echo.
echo =============================================
echo  Hand Gesture Recognition Training Pipeline  
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
python -c "import cv2, mediapipe, numpy, pandas, tensorflow, sklearn, matplotlib, seaborn" >nul 2>&1
if errorlevel 1 (
    echo Installing required Python packages...
    pip install opencv-python mediapipe numpy pandas tensorflow scikit-learn matplotlib seaborn
    if errorlevel 1 (
        echo ERROR: Failed to install required packages.
        pause
        exit /b 1
    )
)

echo.
echo =============================================
echo  Available Options:
echo  1. Collect Data
echo  2. Train Model
echo  3. Validate Dataset
echo  4. Convert to TFLite
echo  5. Run Full Pipeline (Data -> Train -> Convert)
echo  6. Run Full Pipeline with Validation
echo  0. Exit
echo =============================================
echo.

set /p choice="Enter your choice (0-6): "

if "!choice!"=="0" goto :exit
if "!choice!"=="1" goto :collect_data
if "!choice!"=="2" goto :train_model
if "!choice!"=="3" goto :validate_dataset
if "!choice!"=="4" goto :convert_tflite
if "!choice!"=="5" goto :full_pipeline
if "!choice!"=="6" goto :full_pipeline_with_validation

echo Invalid choice. Please enter 0-6.
pause
goto :eof

:collect_data
echo.
echo =============================================
echo  Starting Data Collection
echo =============================================
echo.
echo Please enter the gesture name when prompted.
echo The script will collect 500 frames for the gesture.
echo.
python collect_data_bimanual.py
if errorlevel 1 (
    echo ERROR: Data collection failed.
) else (
    echo Data collection completed successfully.
)
pause
goto :eof

:train_model
echo.
echo =============================================
echo  Starting Model Training
echo =============================================
echo.
python improved_training_script.py
if errorlevel 1 (
    echo ERROR: Model training failed.
) else (
    echo Model training completed successfully.
    echo Check the generated plots and model files.
)
pause
goto :eof

:convert_tflite
echo.
echo =============================================
echo  Converting Model to TensorFlow Lite
echo =============================================
echo.
python convert_to_tflite.py
if errorlevel 1 (
    echo ERROR: Model conversion failed.
) else (
    echo Model conversion completed successfully.
    echo TFLite model and labels files are ready for Android.
)
pause
goto :eof

:validate_dataset
echo.
echo =============================================
echo  Validating Dataset
echo =============================================
echo.
python validate_dataset.py
if errorlevel 1 (
    echo ERROR: Dataset validation failed.
) else (
    echo Dataset validation completed successfully.
    echo Check the generated reports and plots.
)
pause
goto :eof

:full_pipeline
echo.
echo =============================================
echo  Running Full Pipeline (Data -> Train -> Convert)
echo =============================================
echo.

echo Step 1: Data Collection
python collect_data_bimanual.py
if errorlevel 1 (
    echo ERROR: Data collection failed. Stopping pipeline.
    pause
    exit /b 1
)

echo.
echo Step 2: Model Training
python improved_training_script.py
if errorlevel 1 (
    echo ERROR: Model training failed. Stopping pipeline.
    pause
    exit /b 1
)

echo.
echo Step 3: Model Conversion to TFLite
python convert_to_tflite.py
if errorlevel 1 (
    echo ERROR: Model conversion failed.
    pause
    exit /b 1
)

echo.
echo =============================================
echo  Full Pipeline Completed Successfully!
echo  Files generated:
echo  - sign_language_data.csv (updated with new data)
echo  - improved_gesture_model.h5 (trained model)
echo  - gesture_model.tflite (Android-ready model)
echo  - labels.txt (corresponding labels)
echo =============================================
pause
goto :eof

:full_pipeline_with_validation
echo.
echo =============================================
echo  Running Full Pipeline with Validation
echo =============================================
echo.

echo Step 1: Dataset Validation
python validate_dataset.py
if errorlevel 1 (
    echo ERROR: Dataset validation failed.
    pause
    exit /b 1
)

echo.
echo Step 2: Data Collection
python collect_data_bimanual.py
if errorlevel 1 (
    echo ERROR: Data collection failed. Stopping pipeline.
    pause
    exit /b 1
)

echo.
echo Step 3: Model Training
python improved_training_script.py
if errorlevel 1 (
    echo ERROR: Model training failed. Stopping pipeline.
    pause
    exit /b 1
)

echo.
echo Step 4: Model Conversion to TFLite
python convert_to_tflite.py
if errorlevel 1 (
    echo ERROR: Model conversion failed.
    pause
    exit /b 1
)

echo.
echo =============================================
echo  Full Pipeline with Validation Completed Successfully!
echo  Files generated:
echo  - sign_language_data.csv (updated with new data)
echo  - improved_gesture_model.h5 (trained model)
echo  - gesture_model.tflite (Android-ready model)
echo  - labels.txt (corresponding labels)
echo  - Various validation reports and plots
echo =============================================
pause
goto :eof

:exit
echo Goodbye!
endlocal