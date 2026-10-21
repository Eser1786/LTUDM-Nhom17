import pdfplumber
import os
import re

pdf_dir = "pdf/2026-2027-1"
all_pdfs = [os.path.join(pdf_dir, f) for f in os.listdir(pdf_dir) if f.endswith('.pdf') and f != 'TKB_monchung.pdf']

class_names = set()

for pdf_file in all_pdfs:
    with pdfplumber.open(pdf_file) as pdf:
        for page in pdf.pages:
            table = page.extract_table()
            if table:
                header = None
                for i, row in enumerate(table):
                    if row and 'Tên lớp' in str(row):
                        header = row
                        break
                if header:
                    class_idx = header.index('Tên lớp')
                    for row in table:
                        if row and len(row) > class_idx and row[class_idx]:
                            class_name = str(row[class_idx]).strip()
                            if class_name and class_name != 'Tên lớp':
                                for cn in class_name.split(','):
                                    cn = cn.strip().replace('\n', '')
                                    if cn and re.match(r'^[A-Z]{2,3}\d{4}$', cn):
                                        class_names.add(cn)

valid_classes = [c for c in class_names if re.match(r'^[A-Z]{2,3}\d{4}$', c)]
no_letter = [c for c in valid_classes if not re.search(r'[A-Za-z]$', c)]
has_letter = [c for c in valid_classes if re.search(r'[A-Za-z]$', c)]

with open('classes_no_letter.txt', 'w', encoding='utf-8') as f:
    f.write('CLASSES WITHOUT LETTER SUFFIX:\n')
    f.write('=' * 50 + '\n')
    for c in sorted(no_letter):
        f.write(f'{c}\n')
    f.write(f'\nTotal: {len(no_letter)}\n')
    
    f.write('\n\nCLASSES WITH LETTER SUFFIX:\n')
    f.write('=' * 50 + '\n')
    for c in sorted(has_letter):
        f.write(f'{c}\n')
    f.write(f'\nTotal: {len(has_letter)}\n')

print(f'Done. No letter: {len(no_letter)}, With letter: {len(has_letter)}')