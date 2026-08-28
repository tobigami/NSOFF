#!/usr/bin/env python3
"""Gom art của một bộ trang phục bên bản hồi ức về một chỗ, để xem và sửa trước khi nhập.

Không đụng gì tới cơ sở dữ liệu. Chỉ chép ảnh và ghi lại tình trạng, vì art bên đó không phải bộ
nào cũng đủ: có bộ chỉ có cỡ 1, có bộ thiếu vài ảnh giữa chừng, có bộ thiếu hẳn mảnh chân.

Chép ra `art/nhap/<tên bộ>/`:

    goc/<cỡ>/Small<id>.png   ảnh nguyên bản, chỉ những cỡ thật sự có
    xem.png                  bảng nhìn nhanh toàn bộ ảnh
    ghi-chu.md               mảnh nào, ảnh nào, thiếu gì, bên mình đã có chưa

Dùng:
  python3 tools/gom-art-hoiuc.py <tên bộ> <mảnh...>          gom theo mảnh của hồi ức
  python3 tools/gom-art-hoiuc.py <tên bộ> --anh <từ> <đến>   gom theo dải id ảnh
"""
import json
import os
import shutil
import subprocess
import sys

NGUON = 'nso_hoiuc'
DICH = 'nso_test'
KHO = 'thamkhao/ZnsoC 2/ZnsoC/NsoC/res/assets/icon'
TA = 'work/server/NSO_KEM/Data/Img/Small'
COS = (0, 1, 2, 3, 4)


def q(db, sql):
    return subprocess.run(['mysql', '-uroot', db, '-N', '--raw', '-e', sql],
                          capture_output=True, text=True).stdout


def main():
    if len(sys.argv) < 3:
        raise SystemExit(__doc__)
    os.chdir(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
    ten = sys.argv[1]
    manh, anh = [], []
    if sys.argv[2] == '--anh':
        anh = list(range(int(sys.argv[3]), int(sys.argv[4]) + 1))
    else:
        manh = [int(x) for x in sys.argv[2:]]
        for m in manh:
            d = q(NGUON, f"SELECT part FROM nj_part WHERE id={m};").strip()
            if d:
                anh += [f['id'] for f in json.loads(d) if f.get('id')]
    anh = sorted(set(anh))

    ra = f'art/nhap/{ten}'
    shutil.rmtree(ra, ignore_errors=True)
    os.makedirs(f'{ra}/goc', exist_ok=True)

    coCo, daCo, thieu = {}, [], []
    for i in anh:
        cs = [z for z in COS if os.path.exists(f'{KHO}/{z}/Small{i}.png')]
        if not cs:
            thieu.append(i)
            continue
        coCo[i] = cs
        for z in cs:
            os.makedirs(f'{ra}/goc/{z}', exist_ok=True)
            shutil.copy(f'{KHO}/{z}/Small{i}.png', f'{ra}/goc/{z}/Small{i}.png')
        if os.path.exists(f'{TA}/1/Small{i}.png') and \
                open(f'{TA}/1/Small{i}.png', 'rb').read() == open(f'{KHO}/1/Small{i}.png', 'rb').read():
            daCo.append(i)

    dayDu = [i for i, cs in coCo.items() if all(z in cs for z in (1, 2, 3, 4))]
    ghi = [f'# Art bộ {ten} lấy từ bản hồi ức', '',
           f'- mảnh nguồn: {manh or "(gom theo dải ảnh)"}',
           f'- cần {len(anh)} ảnh, chép được {len(coCo)}',
           f'- đủ bốn cỡ: {len(dayDu)}',
           f'- bên mình đã có sẵn giống hệt: {len(daCo)}',
           f'- thiếu hẳn bên hồi ức: {thieu or "không"}', '']
    con = {}
    for i, cs in coCo.items():
        if not all(z in cs for z in (1, 2, 3, 4)):
            con.setdefault(tuple(cs), []).append(i)
    if con:
        ghi.append('## Ảnh chưa đủ cỡ, phải phóng thêm trước khi nhập')
        for cs, ids in sorted(con.items()):
            ghi.append(f'- chỉ có cỡ {list(cs)}: {ids}')
        ghi.append('')
    ghi.append('## Danh sách ảnh')
    ghi.append(' '.join(str(i) for i in anh))
    open(f'{ra}/ghi-chu.md', 'w').write('\n'.join(ghi) + '\n')

    # bảng xem nhanh, lấy cỡ lớn nhất có được của từng ảnh
    xem = f'{ra}/.xem'
    os.makedirs(xem, exist_ok=True)
    nhan = []
    for i in sorted(coCo):
        z = max(coCo[i])
        shutil.copy(f'{KHO}/{z}/Small{i}.png', f'{xem}/Small{i}.png')
        nhan.append(f'{i}:{i}')
    if nhan:
        subprocess.run(['java', '-cp', 'build/selftest', 'Sheet', xem, f'{ra}/xem.png'] + nhan,
                       capture_output=True)
    shutil.rmtree(xem, ignore_errors=True)

    print(f'đã gom vào {ra}/')
    print(f'  {len(coCo)}/{len(anh)} ảnh, đủ bốn cỡ {len(dayDu)}, mình đã có {len(daCo)}, thiếu hẳn {len(thieu)}')
    for cs, ids in sorted(con.items()):
        print(f'  chỉ có cỡ {list(cs)}: {len(ids)} ảnh')


if __name__ == '__main__':
    main()
