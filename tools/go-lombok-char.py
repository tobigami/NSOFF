#!/usr/bin/env python3
"""Gỡ Lombok khỏi một lớp bằng cách viết tay đúng những accessor mà Lombok vốn sinh ra.

Máy này không có lombok.jar và cũng không tải được (dự án dựng offline), nên `javac` gặp
@Getter/@Setter là hỏng -- đó là lý do Char.java lâu nay chỉ vá được ở mức bytecode. Ở đây thay
annotation bằng phương thức thật, để tệp biên dịch lại được như mọi tệp khác.

Chuẩn đối chiếu là chính lớp Char.class trong jar: script chỉ sinh những phương thức mà `javap`
xác nhận là có thật, đúng tên đúng kiểu. Không đoán theo quy tắc đặt tên của Lombok -- quy tắc ấy
có mấy chỗ bẫy (trường `boolean isLoadFinish` cho ra `isLoadFinish()` và `setLoadFinish()`, chứ
không phải `isIsLoadFinish()`), và đoán sai một cái là hỏng chỗ gọi ở tệp khác mà javac không hề
báo, vì mấy tệp ấy không được dịch lại.

Dùng: python3 tools/go-lombok-char.py [com/nsoz/map/zones/Zone.java] [--ghi]
"""
import re
import subprocess
import sys

JAR = 'work/server/NSO_KEM/target/Nso-jar-with-dependencies.jar'
GOC = 'work/server/NSO_KEM/src/main/java/'

# Lớp cần gỡ, truyền bằng đường dẫn tương đối từ com/nsoz. Mặc định là Char cho giữ nguyên cách gọi cũ.
NGUON = GOC + 'com/nsoz/model/Char.java'


def chu_ky_goc():
    """Bộ chữ ký phương thức của lớp gốc, lấy thẳng từ jar."""
    trong = NGUON[len(GOC):].replace('.java', '.class')
    subprocess.run(['unzip', '-o', '-q', JAR, trong, '-d', '/tmp/goccls'], check=True)
    ra = subprocess.run(['javap', '-p', '/tmp/goccls/' + trong],
                        capture_output=True, text=True).stdout
    return set(l.strip() for l in ra.split('\n') if l.strip().endswith(';'))


def main():
    global NGUON
    ghi = '--ghi' in sys.argv
    for a in sys.argv[1:]:
        if not a.startswith('--'):
            NGUON = GOC + a if not a.startswith(GOC) else a
    goc = chu_ky_goc()
    src = open(NGUON, encoding='utf-8').read().split('\n')

    # @Getter đặt ngay trên `class` thì Lombok sinh getter cho MỌI trường, không riêng trường nào
    # có annotation. Bắt trường hợp đó rồi coi như mọi trường đều được đánh dấu.
    caLop = set()
    for i, l in enumerate(src):
        if l.strip() in ('@Getter', '@Setter'):
            j = i + 1
            while j < len(src) and src[j].strip().startswith('@'):
                j += 1
            if j < len(src) and re.match(r'\s*(public|final|abstract|\s)*class\s', src[j]):
                caLop.add(l.strip()[1:])

    # gom các khai báo có annotation
    truong = []          # (dòng, {Getter,Setter}, kiểu, [tên...])
    i = 0
    while i < len(src):
        if src[i].strip() in ('@Getter', '@Setter'):
            anns, dau = set(), i
            while src[i].strip() in ('@Getter', '@Setter'):
                anns.add(src[i].strip()[1:])
                i += 1
            m = re.match(r'\s*(?:public|private|protected)?\s*(?:static\s+)?(?:final\s+)?'
                         r'([A-Za-z_$][\w$]*(?:\s*<[^;]*?>)?(?:\[\])?)\s+(.+?);', src[i])
            if not m:
                # @Getter đặt trên `class` -- đã xử lý riêng ở trên, ở đây bỏ qua.
                if re.match(r'\s*(public|final|abstract|\s)*class\s', src[i]):
                    continue
                raise SystemExit('không đọc được khai báo ở dòng %d: %s' % (i + 1, src[i]))
            kieu = m.group(1).strip()
            tens = [t.strip().split('=')[0].strip() for t in m.group(2).split(',')]
            if 'static' in src[i].split(kieu)[0]:
                anns = set(anns) | {'static'}
            truong.append((dau, i, anns, kieu, tens))
        elif caLop:
            m = re.match(r'\s*(?:public|private|protected)\s+(?!static\s+final)(?:static\s+)?(?:final\s+)?'
                         r'([A-Za-z_$][\w$]*(?:\s*<[^;]*?>)?(?:\[\])?)\s+([a-zA-Z_$][\w$, =]*);', src[i])
            if m and 'class ' not in src[i] and '(' not in src[i]:
                tens = [t.strip().split('=')[0].strip() for t in m.group(2).split(',')]
                anns = set(caLop)
                if 'static' in src[i].split(m.group(1))[0]:
                    anns.add('static')
                truong.append((i, i, anns, m.group(1).strip(), tens))
        i += 1

    # sinh phương thức, chỉ giữ cái nào javap xác nhận
    ra, bo = [], []
    for _, _, anns, kieu, tens in truong:
        tinh = 'static ' if 'static' in anns else ''
        for ten in tens:
            hoa = ten[0].upper() + ten[1:]
            lay = [f'is{hoa}', f'get{hoa}'] if kieu == 'boolean' else [f'get{hoa}']
            if ten.startswith('is') and len(ten) > 2 and ten[2].isupper():
                lay.insert(0, ten)                       # boolean isLockFire -> isLockFire()
            dat = [f'set{hoa}']
            if ten.startswith('is') and len(ten) > 2 and ten[2].isupper():
                dat.insert(0, 'set' + ten[2:])           # isLoadFinish -> setLoadFinish

            if 'Getter' in anns:
                for ten_lay in lay:
                    if any(re.search(r'\b%s\(\);$' % re.escape(ten_lay), c) for c in goc):
                        ra.append(f'    public {tinh}{kieu} {ten_lay}() {{\n'
                                  f'        return {"" if tinh else "this."}{ten};\n    }}')
                        break
                else:
                    bo.append(f'getter {ten} ({kieu})')
            if 'Setter' in anns:
                for ten_dat in dat:
                    if any(re.search(r'\bvoid %s\(' % re.escape(ten_dat), c) for c in goc):
                        ra.append(f'    public {tinh}void {ten_dat}({kieu} {ten}) {{\n'
                                  f'        {"" if tinh else "this."}{ten} = {ten};\n    }}')
                        break
                else:
                    bo.append(f'setter {ten} ({kieu})')

    print(f'  sinh {len(ra)} phương thức từ {len(truong)} khai báo')
    if bo:
        print('  !! javap không xác nhận, bỏ qua: ' + ', '.join(bo))

    if not ghi:
        print('\n  (xem thử -- chạy lại với --ghi để sửa tệp)')
        return

    # bỏ annotation và import lombok
    xoa = set()
    for dau, cuoi, _, _, _ in truong:
        if dau != cuoi:
            xoa.update(range(dau, cuoi))
    # annotation ở cấp lớp: xoá dòng @Getter/@Setter đứng ngay trên `class`
    for i, l in enumerate(src):
        if l.strip() in ('@Getter', '@Setter'):
            j = i + 1
            while j < len(src) and src[j].strip().startswith('@'):
                j += 1
            if j < len(src) and re.match(r'\s*(public|final|abstract|\s)*class\s', src[j]):
                xoa.add(i)
    moi = [l for n, l in enumerate(src)
           if n not in xoa and not l.startswith('import lombok.')]

    # chèn trước dấu } đóng lớp
    for n in range(len(moi) - 1, -1, -1):
        if moi[n].strip() == '}':
            moi[n:n] = ['', '    // ==== Accessor thay cho Lombok, viết tay cho khớp Char.class gốc ====',
                        ''] + [m + '\n' for m in ra]
            break
    open(NGUON, 'w', encoding='utf-8').write('\n'.join(moi))
    print('  đã ghi ' + NGUON)


if __name__ == '__main__':
    main()
