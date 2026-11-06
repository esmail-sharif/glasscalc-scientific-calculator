import PyPDF2

reader = PyPDF2.PdfReader('fx-570ESPLUS_991ESPLUS_AR.pdf')

def print_page_snippets(pages):
    for p in pages:
        t = reader.pages[p-1].extract_text()
        first_few = t[:300].replace('\n', ' ') if t else ''
        print(f"Page {p}: {first_few}\n")

print("=== MODES SAMPLING ===")
print_page_snippets([42, 43, 51, 55, 57, 60, 62])
