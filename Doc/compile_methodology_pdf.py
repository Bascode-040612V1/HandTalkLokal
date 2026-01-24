import os
import sys
import subprocess
from pathlib import Path

def check_dependencies():
    """Check if required dependencies are available"""
    # Check for Python
    print(f"Python version: {sys.version}")
    
    # Check for LaTeX
    try:
        result = subprocess.run(['pdflatex', '--version'], 
                              capture_output=True, text=True, check=False)
        latex_available = result.returncode == 0
    except FileNotFoundError:
        latex_available = False
    
    # Check for pandoc
    try:
        result = subprocess.run(['pandoc', '--version'], 
                              capture_output=True, text=True, check=False)
        pandoc_available = result.returncode == 0
    except FileNotFoundError:
        pandoc_available = False
    
    return latex_available, pandoc_available

def install_dependencies():
    """Install required packages via pip"""
    print("Installing required Python packages...")
    subprocess.check_call([sys.executable, '-m', 'pip', 'install', 'weasyprint', 'markdown'])

def convert_via_pypdf(tex_file_path):
    """Convert LaTeX content to PDF using reportlab as an alternative method"""
    print("Attempting conversion using ReportLab...")
    
    try:
        from reportlab.lib.pagesizes import letter
        from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Preformatted
        from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
        from reportlab.lib.units import inch
        import re
        
        # Read the LaTeX file
        with open(tex_file_path, 'r', encoding='utf-8') as f:
            tex_content = f.read()
        
        # Create PDF document
        pdf_path = tex_file_path.replace('.tex', '.pdf')
        doc = SimpleDocTemplate(pdf_path, pagesize=letter)
        styles = getSampleStyleSheet()
        story = []
        
        # Custom styles
        section_style = ParagraphStyle(
            'CustomHeading1',
            parent=styles['Heading1'],
            fontSize=16,
            spaceAfter=12,
        )
        
        subsection_style = ParagraphStyle(
            'CustomHeading2',
            parent=styles['Heading2'],
            fontSize=14,
            spaceAfter=10,
        )
        
        # Process LaTeX content line by line
        lines = tex_content.split('\n')
        i = 0
        while i < len(lines):
            line = lines[i].strip()
            
            if line.startswith('\\section{'):
                # Extract section title
                title = re.sub(r'^\\section{(.*)}', r'\1', line)
                p = Paragraph(title, section_style)
                story.append(p)
                story.append(Spacer(1, 12))
            elif line.startswith('\\subsection{'):
                # Extract subsection title
                title = re.sub(r'^\\subsection{(.*)}', r'\1', line)
                p = Paragraph(title, subsection_style)
                story.append(p)
                story.append(Spacer(1, 10))
            elif line.startswith('\\subsubsection{'):
                # Extract sub-subsection title
                title = re.sub(r'^\\subsubsection{(.*)}', r'\1', line)
                p = Paragraph(title, styles['Heading3'])
                story.append(p)
                story.append(Spacer(1, 8))
            elif line and not line.startswith('\\') and not line.startswith('%') and not any(x in line.lower() for x in ['\\documentclass', '\\usepackage', '\\title', '\\author', '\\date', '\\begin{document}', '\\maketitle', '\\doublespacing', '\\end{document}', '\\begin{', '\\end{']):
                # Add regular text paragraphs
                clean_line = re.sub(r'[\\][\\]', '', line)  # Remove escaped characters
                clean_line = re.sub(r'[\\][wW] ', '', clean_line)  # Remove formatting commands
                clean_line = re.sub(r'[{}]', '', clean_line)  # Remove braces
                clean_line = re.sub(r'[\\][a-zA-Z]+', '', clean_line)  # Remove LaTeX commands
                if clean_line.strip():
                    p = Paragraph(clean_line, styles['Normal'])
                    story.append(p)
                    story.append(Spacer(1, 6))
            
            i += 1
        
        # Build PDF
        doc.build(story)
        print(f"PDF created successfully: {pdf_path}")
        return True
        
    except ImportError:
        print("ReportLab not available. Install with: pip install reportlab")
        return False
    except Exception as e:
        print(f"Error during PDF conversion: {str(e)}")
        return False

def convert_via_txt(tex_file_path):
    """Simple fallback: convert LaTeX to TXT then to PDF"""
    print("Attempting conversion via text format...")
    
    try:
        from reportlab.lib.pagesizes import letter
        from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer
        from reportlab.lib.styles import getSampleStyleSheet
        import re
        
        # Read the LaTeX file
        with open(tex_file_path, 'r', encoding='utf-8') as f:
            tex_content = f.read()
        
        # Create PDF document
        pdf_path = tex_file_path.replace('.tex', '.pdf')
        doc = SimpleDocTemplate(pdf_path, pagesize=letter)
        styles = getSampleStyleSheet()
        story = []
        
        # Process content to extract text
        lines = tex_content.split('\n')
        current_text = ""
        
        for line in lines:
            line = line.strip()
            
            # Skip LaTeX commands and document structure
            if (line.startswith('\\') and not line.startswith('\\section') and 
                not line.startswith('\\subsection') and not line.startswith('\\subsubsection')) or \
               line.startswith('%') or \
               any(x in line.lower() for x in ['\\documentclass', '\\usepackage', '\\title', '\\author', '\\date', \
                                            '\\begin{document}', '\\maketitle', '\\doublespacing', '\\end{document}']):
                continue
            
            # Handle section titles
            if line.startswith('\\section{'):
                title = re.sub(r'^\\section{(.*)}', r'\1', line)
                p = Paragraph(f"<b>{title}</b>", styles['Heading1'])
                story.append(p)
                story.append(Spacer(1, 12))
            elif line.startswith('\\subsection{'):
                title = re.sub(r'^\\subsection{(.*)}', r'\1', line)
                p = Paragraph(f"<b>{title}</b>", styles['Heading2'])
                story.append(p)
                story.append(Spacer(1, 10))
            elif line.startswith('\\subsubsection{'):
                title = re.sub(r'^\\subsubsection{(.*)}', r'\1', line)
                p = Paragraph(f"<b>{title}</b>", styles['Heading3'])
                story.append(p)
                story.append(Spacer(1, 8))
            elif line and '$' not in line:  # Skip lines with math for simplicity
                # Clean the line from LaTeX commands
                clean_line = re.sub(r'[\\][a-zA-Z]+', '', line)  # Remove commands
                clean_line = re.sub(r'[{}]', '', clean_line)      # Remove braces
                clean_line = re.sub(r'[~^]', '', clean_line)      # Remove special chars
                clean_line = clean_line.strip()
                
                if clean_line:
                    p = Paragraph(clean_line, styles['Normal'])
                    story.append(p)
                    story.append(Spacer(1, 6))
        
        # Build PDF
        doc.build(story)
        print(f"PDF created successfully: {pdf_path}")
        return True
        
    except ImportError:
        print("ReportLab not available. Install with: pip install reportlab")
        return False
    except Exception as e:
        print(f"Error during PDF conversion: {str(e)}")
        return False

def main():
    # Get the directory where this script is located
    script_dir = Path(__file__).parent.absolute()
    tex_file_path = script_dir / "Methodology.tex"
    
    if not tex_file_path.exists():
        print(f"Error: Could not find {tex_file_path}")
        return
    
    print("Checking dependencies...")
    latex_available, pandoc_available = check_dependencies()
    
    print(f"LaTeX available: {latex_available}")
    print(f"Pandoc available: {pandoc_available}")
    
    if latex_available:
        print("Compiling LaTeX document to PDF using pdflatex...")
        try:
            result = subprocess.run([
                'pdflatex', 
                '-include-directory=..', 
                '-output-directory=.', 
                str(tex_file_path)
            ], cwd=script_dir, check=True, capture_output=True, text=True)
            
            print("PDF created successfully using pdflatex!")
            print("Check the Doc folder for Methodology.pdf")
        except subprocess.CalledProcessError as e:
            print(f"Error during pdflatex compilation: {e}")
            print("Trying alternative conversion methods...")
            
            # Try alternative methods
            install_dependencies()
            success = convert_via_pypdf(str(tex_file_path))
            if not success:
                success = convert_via_txt(str(tex_file_path))
                if not success:
                    print("All conversion methods failed.")
    else:
        print("LaTeX (pdflatex) not found. Installing required Python packages and trying alternative methods...")
        install_dependencies()
        success = convert_via_pypdf(str(tex_file_path))
        if not success:
            success = convert_via_txt(str(tex_file_path))
            if not success:
                print("Alternative conversion methods failed.")
            print("\nTo compile properly with LaTeX:")
            print("- Install MiKTeX (Windows): https://miktex.org/")
            print("- Or install TeX Live (Linux/Mac): https://www.tug.org/texlive/")
            print("- Or install MacTeX (Mac): https://www.tug.org/mactex/")

if __name__ == "__main__":
    main()