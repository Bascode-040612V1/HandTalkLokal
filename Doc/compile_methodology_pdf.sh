#!/bin/bash
# Script to compile the methodology LaTeX document to PDF

cd "$(dirname "$0")"

echo "Compiling Methodology LaTeX document to PDF..."

# Check if pdflatex is installed
if ! command -v pdflatex &> /dev/null; then
    echo "pdflatex is not installed. Please install a LaTeX distribution such as TeX Live."
    echo "On Ubuntu/Debian: sudo apt-get install texlive-full"
    echo "On Windows: Install MiKTeX or TeX Live"
    echo "On macOS: Install MacTeX"
    exit 1
fi

# Compile the LaTeX file to PDF
pdflatex -include-directory=.. -output-directory=. Methodology.tex

echo "Compilation complete. Check the Doc folder for Methodology.pdf"