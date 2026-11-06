import PyPDF2

reader = PyPDF2.PdfReader('fx-570ESPLUS_991ESPLUS_AR.pdf')

def extract_section(start_page, end_page):
    out = []
    for p in range(start_page - 1, end_page):
        t = reader.pages[p].extract_text()
        if t:
            out.append(f"--- PAGE {p+1} ---\n" + t)
    return "\n".join(out)

# Read MODE summary (pages 9-11)
print("=== MODE SUMMARY (P. 9-11) ===")
print(extract_section(9, 11)[:2000])

# Read CONST & CONV (pages 38-41)
print("\n=== CONST & CONV (P. 38-41) ===")
print(extract_section(38, 41)[:2000])
