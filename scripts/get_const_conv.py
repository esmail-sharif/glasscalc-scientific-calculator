import PyPDF2

reader = PyPDF2.PdfReader('fx-570ESPLUS_991ESPLUS_AR.pdf')
# pages 38-42
text = ""
for p in range(38, 43):
    text += f"\n=== PAGE {p} ===\n" + reader.pages[p-1].extract_text()

with open('scripts/constants_and_conversions.txt', 'w', encoding='utf-8') as f:
    f.write(text)

print("Extracted constants and conversions text (length:", len(text), ")")
