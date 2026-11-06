import PyPDF2
import json

reader = PyPDF2.PdfReader('fx-570ESPLUS_991ESPLUS_AR.pdf')
num_pages = len(reader.pages)
print(f"Total pages: {num_pages}")

# Get outline / bookmarks
try:
    outline = reader.outline
    def parse_outline(outl, depth=0):
        items = []
        for item in outl:
            if isinstance(item, list):
                items.extend(parse_outline(item, depth+1))
            else:
                title = getattr(item, 'title', str(item))
                page = reader.get_destination_page_number(item) if hasattr(reader, 'get_destination_page_number') else '?'
                items.append({'depth': depth, 'title': title, 'page': page})
        return items
    bookmarks = parse_outline(outline)
    print(f"Found {len(bookmarks)} bookmarks:")
    for b in bookmarks[:50]:
        print("  " * b['depth'] + f"- {b['title']} (p. {b['page']})")
except Exception as e:
    print(f"Outline parsing failed: {e}")

# Also extract text from first few pages (table of contents / index)
print("\n--- Page 1 to 5 Sample Text ---")
for p in range(min(5, num_pages)):
    text = reader.pages[p].extract_text()
    print(f"=== PAGE {p+1} ===")
    print(text[:500] if text else "[No text]")
