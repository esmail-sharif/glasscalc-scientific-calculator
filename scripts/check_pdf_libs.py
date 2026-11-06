import sys

modules = ["pypdf", "PyPDF2", "fitz", "pdfminer", "pdfplumber"]
for m in modules:
    try:
        mod = __import__(m)
        print(f"{m} is available")
    except ImportError:
        pass
