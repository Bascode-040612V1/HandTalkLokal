@echo off
chcp 65001 >nul
setlocal EnableDelayedExpansion

REM Clear screen and show title
cls
echo ================================================================================
echo  HAND GESTURE RECOGNITION TRAINING PIPELINE
echo ================================================================================
echo.
echo This script automates the entire hand gesture recognition training pipeline.
echo It will collect data, train a model, and convert it to TensorFlow Lite format.
echo.
echo Organized Output Structure:
echo   - output/tflite_model/: Contains the final TFLite model for Android
echo   - output/labels/: Contains label files for Android
echo   - output/model_assets/: Contains label encoders and other model assets
echo   - output/trained_models/: Contains trained Keras models
echo.
echo ================================================================================
echo  SELECT AN OPTION:
echo ================================================================================
echo.
echo [1] Train Model (from existing dataset)
echo [2] Convert to TFLite (from trained model)
echo [3] Validate TFLite Model
echo [4] Run Complete Pipeline (Train + Convert + Validate)
echo [5] Collect New Gesture Data
echo [6] Organize Existing Datasets
echo [7] View Current Dataset Info
echo [8] Clean Output Directories
echo [9] Deploy to Android Assets
echo [0] Exit
echo.
set /p choice="Enter your choice (0-9): "

if "!choice!"=="1" goto :train_model
if "!choice!"=="2" goto :convert_tflite
if "!choice!"=="3" goto :validate_tflite
if "!choice!"=="4" goto :complete_pipeline
if "!choice!"=="5" goto :collect_data
if "!choice!"=="6" goto :organize_datasets
if "!choice!"=="7" goto :view_dataset
if "!choice!"=="8" goto :clean_outputs
if "!choice!"=="9" goto :deploy_android
if "!choice!"=="0" goto :exit

echo.
echo Invalid choice. Please select 0-9.
pause
goto :eof

:train_model
echo.
echo ================================================================================
echo  TRAINING MODEL
echo ================================================================================
echo.
if not exist "data\gestures_bimanual.csv" (
    echo ERROR: Dataset not found at data/gestures_bimanual.csv
    echo Please collect some data first using option 5, or place the dataset in the data folder.
    pause
    goto :eof
)
echo Training model from dataset...
python train_model.py
if errorlevel 1 (
    echo ERROR: Model training failed.
) else (
    echo ✅ Model training completed successfully.
    echo Generated files:
    echo - output/trained_models/handtalk_model.keras (trained model)
    echo - output/trained_models/best_model.keras (best performing checkpoint)
    echo - output/model_assets/label_encoder.pkl (label encoder)
    echo - output/model_assets/labels.txt (text labels)
)
pause
goto :eof

:convert_tflite
echo.
echo ================================================================================
echo  CONVERTING MODEL TO TFLITE
echo ================================================================================
echo.
if not exist "handtalk_model.keras" (
    echo ERROR: Keras model not found at handtalk_model.keras
    echo Please train the model first using option 1.
    pause
    goto :eof
)
echo Converting Keras model to TFLite...
python convert_to_tflite.py
if errorlevel 1 (
    echo ERROR: Model conversion failed.
) else (
    echo ✅ Model conversion completed successfully.
    echo Generated files:
    echo - output/tflite_model/handtalk_model.tflite (TFLite model for Android)
    echo - output/labels/labels.txt (text labels for Android)
    echo - output/model_assets/label_encoder.pkl (label encoder)
)
pause
goto :eof

:validate_tflite
echo.
echo ================================================================================
echo  VALIDATING TFLITE MODEL
echo ================================================================================
echo.
if not exist "output/tflite_model/handtalk_model.tflite" (
    echo ERROR: TFLite model not found at output/tflite_model/handtalk_model.tflite
    echo Please convert the model first using option 2.
    pause
    goto :eof
)
echo Validating TFLite model...
python -c "from convert_to_tflite import validate_tflite_model; validate_tflite_model()"
if errorlevel 1 (
    echo ERROR: Model validation failed.
) else (
    echo ✅ Model validation completed successfully.
    echo Model is ready for deployment!
)
pause
goto :eof

:complete_pipeline
echo.
echo ================================================================================
echo  RUNNING COMPLETE PIPELINE
echo ================================================================================
echo.
echo This will execute the full pipeline:
echo   1. Train the model
echo   2. Convert to TFLite
echo   3. Validate the TFLite model
echo.

set /p confirm="Are you sure you want to proceed? (y/n): "
if /i not "!confirm!"=="y" (
    echo Operation cancelled.
    pause
    goto :eof
)

echo.
echo Step 1: Model Training
python train_model.py
if errorlevel 1 (
    echo ERROR: Model training failed.
    pause
    exit /b 1
)

echo.
echo Step 2: Converting to TFLite...
python convert_to_tflite.py
if errorlevel 1 (
    echo ERROR: Model conversion failed.
    pause
    exit /b 1
)

echo.
echo Step 3: Validating TFLite model...
python -c "from convert_to_tflite import validate_tflite_model; validate_tflite_model()"
if errorlevel 1 (
    echo ERROR: Model validation failed.
    pause
    exit /b 1
)

echo.
echo ================================================================================
echo  COMPLETE PIPELINE FINISHED SUCCESSFULLY!
echo ================================================================================
echo.
echo Generated files:
echo - output/trained_models/handtalk_model.keras (trained Keras model)
echo - output/tflite_model/handtalk_model.tflite (TFLite model for Android)
echo - output/model_assets/label_encoder.pkl (label encoder)
echo - output/labels/labels.txt (text labels for Android)
echo - output/trained_models/best_model.keras (best performing checkpoint)
echo.
echo The model is ready for deployment on Android!
echo.
pause
goto :eof

:collect_data
echo.
echo ================================================================================
echo  COLLECTING NEW GESTURE DATA (OPENS CAMERA)
echo ================================================================================
echo.
echo Preparing to open camera for data collection...
echo The script will ask for a gesture name and then start a 3-second countdown.
echo.
python collect_data_bimanual.py
if errorlevel 1 (
    echo ERROR: Data collection failed.
) else (
    echo ✅ Data collection completed successfully.
    echo New data has been added to data/gestures_bimanual.csv
)
pause
goto :eof

:organize_datasets
echo.
echo ================================================================================
echo  ORGANIZING DATASETS
echo ================================================================================
echo.
echo Creating organized directory structure...
if not exist "datasets" mkdir datasets
if not exist "datasets/raw" mkdir datasets/raw
if not exist "datasets/processed" mkdir datasets/processed
if not exist "datasets/backup" mkdir datasets/backup

echo Moving existing datasets to organized structure...
if exist "data/gestures_bimanual.csv" (
    copy "data/gestures_bimanual.csv" "datasets/processed/"
    echo Moved processed dataset to datasets/processed/
)

if exist "data/arm_hand_sequences" (
    xcopy "data/arm_hand_sequences" "datasets/raw/arm_hand_sequences" /E /I
    echo Moved raw sequence data to datasets/raw/
)

echo ✅ Dataset organization completed!
echo.
echo New structure:
echo - datasets/raw/: Raw captured data
echo - datasets/processed/: Processed training data
echo - datasets/backup/: Backup copies
echo.
pause
goto :eof

:view_dataset
echo.
echo ================================================================================
echo  VIEWING DATASET INFORMATION
echo ================================================================================
echo.
if not exist "data/gestures_bimanual.csv" (
    echo ERROR: Dataset not found at data/gestures_bimanual.csv
    echo Please collect some data first using option 5.
    pause
    goto :eof
)

echo Getting dataset information...
python -c "
import pandas as pd
import os
df = pd.read_csv('data/gestures_bimanual.csv')
print('DATASET INFORMATION:')
print('===================')
print(f'Total samples: {len(df)}')
print(f'Features per sample: {len(df.columns)-1}')  # -1 for label column
print(f'Total gestures: {df[''label''].nunique()}')
print('')
print('Gesture distribution:')
print('-------------------')
print(df[''label''].value_counts())
print('')
print('File size: {:.2f} MB'.format(os.path.getsize('data/gestures_bimanual.csv') / (1024*1024)))
"
pause
goto :eof

:clean_outputs
echo.
echo ================================================================================
echo  CLEANING OUTPUT DIRECTORIES
echo ================================================================================
echo.
set /p confirm="This will delete all output files. Are you sure? (y/n): "
if /i not "!confirm!"=="y" (
    echo Operation cancelled.
    pause
    goto :eof
)

if exist "output" (
    rmdir /s /q "output"
    echo ✅ Output directories cleaned.
) else (
    echo No output directories found.
)

echo.
echo Creating fresh output directories...
mkdir "output"
mkdir "output\tflite_model"
mkdir "output\labels"
mkdir "output\model_assets"
mkdir "output\trained_models"
echo ✅ Fresh output directories created.
pause
goto :eof

:deploy_android
echo.
echo ================================================================================
echo  DEPLOYING TO ANDROID ASSETS
echo ================================================================================
echo.
if not exist "output\tflite_model\handtalk_model.tflite" (
    echo ERROR: TFLite model not found at output\tflite_model\handtalk_model.tflite
    echo Please run the complete pipeline first.
    pause
    goto :eof
)

if not exist "..\..\app\src\main\assets" (
    echo ERROR: Android assets directory not found.
    echo Expected location: ..\..\app\src\main\assets
    pause
    goto :eof
)

echo Deploying model and labels to Android assets...
copy "output\tflite_model\handtalk_model.tflite" "..\..\app\src\main\assets\"
copy "output\labels\labels.txt" "..\..\app\src\main\assets\"

if errorlevel 1 (
    echo ERROR: Deployment failed.
    echo Make sure the Android project exists and you have write permissions.
) else (
    echo ✅ Model deployed to Android assets successfully!
    echo Files copied:
    echo - handtalk_model.tflite
    echo - labels.txt
)
pause
goto :eof

:exit
echo.
echo Thank you for using the Hand Gesture Recognition Training Pipeline!
echo.
endlocal