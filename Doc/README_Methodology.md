# Hand Talk Lokal - Methodology Documentation

This directory contains the academic methodology documentation for the Hand Talk Lokal project, a sign language recognition mobile application.

## Files Included

1. `Methodology.tex` - The main LaTeX source file containing the detailed methodology section for the thesis
2. `compile_methodology_pdf.sh` - A bash script to compile the LaTeX file to PDF
3. `compile_methodology_pdf.bat` - A Windows batch script to compile the LaTeX file to PDF
4. `compile_methodology_pdf.py` - A Python script providing alternative PDF generation without LaTeX dependency
5. `thesis-journal-template.pdf` - Template for thesis formatting
6. `generalDocumentationFormat.pdf` - General documentation format guide
7. `README_Methodology.md` - This documentation file explaining how to use the methodology document

## Compiling the Methodology Document to PDF

To convert the LaTeX methodology document to PDF:

### On Windows:
1. Option A: Install MiKTeX or TeX Live, then double-click the `compile_methodology_pdf.bat` file
2. Option B: If LaTeX is not available, run `python compile_methodology_pdf.py` (requires Python 3.x)

### On Linux/macOS:
1. Option A: Ensure you have a LaTeX distribution installed (TeX Live, MacTeX), then run: `./compile_methodology_pdf.sh`
2. Option B: If LaTeX is not available, run: `python3 compile_methodology_pdf.py`

### Manual Compilation:
If you prefer to compile manually:
```bash
pdflatex -output-directory=./Doc ./Doc/Methodology.tex
```

### Python-Based Alternative:
The Python script (`compile_methodology_pdf.py`) provides an alternative method that works without LaTeX:
1. It first checks if LaTeX is available and uses it if present
2. If LaTeX is not available, it installs required packages and converts via HTML/PDF
3. This makes the document generation more accessible without requiring full LaTeX installation

## Content Overview

The methodology document covers:

- Research Design
- System Development Methodology
- System Architecture
- Mathematical Models and Algorithms Used
- Data Flow and Processing
- Development Tools and Technologies
- Testing and Evaluation
- Data Analysis Methods
- Ethical Considerations

This methodology section is written in formal academic tone and follows thesis-level standards for computer science and application development research.

## Academic Use

This document is designed to be directly incorporated into a thesis manuscript as the Methodology chapter. The content is comprehensive and follows academic writing standards for developmental research in computer science.