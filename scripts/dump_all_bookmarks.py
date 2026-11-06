import PyPDF2

reader = PyPDF2.PdfReader('fx-570ESPLUS_991ESPLUS_AR.pdf')

def print_bookmarks(outl, depth=0):
    for item in outl:
        if isinstance(item, list):
            print_bookmarks(item, depth+1)
        else:
            title = getattr(item, 'title', str(item))
            page = reader.get_destination_page_number(item) if hasattr(reader, 'get_destination_page_number') else '?'
            print("  " * depth + f"- {title} (ص {page})")

print_bookmarks(reader.outline)
