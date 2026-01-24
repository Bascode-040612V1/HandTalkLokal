@echo off
REM Batch script to compile the methodology LaTeX document to PDF

cd /d "%~dp0"

python --version >nul 2>&1 || (echo Python is required but not found in PATH. & exit /b 1)

REM Check if pdflatex is available
pdflatex --version >nul 2>&1
if %errorlevel% neq 0 (
    echo pdflatex is not found. Please install a LaTeX distribution such as MiKTeX.
    echo Visit https://miktex.org/ to download MiKTeX for Windows.
    echo This script will exit now.
    exit /b 1
)

echo Compiling Methodology LaTeX document to PDF...
pdflatex -include-directory=.. -output-directory=. Methodology.tex

echo.
echo Compilation complete. Check the Doc folder for Methodology.pdf
pause