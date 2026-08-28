#!/usr/bin/env python3
"""Gỡ chỗ đè id ảnh giữa mảnh trang phục mới thêm và mảnh vốn có.

## Chuyện gì đã xảy ra

`them-trang-phuc.py` cấp id ảnh mới bằng `MAX(nj_image.id) + 1`. Nghe thì đúng, mà sai: **`nj_part`
có quyền trỏ tới id ảnh không hề có hàng trong `nj_image`**. Kho ảnh gốc của game có hơn hai nghìn
tấm nằm dạng tệp rời, client cứ theo id mà xin tệp, không cần hàng nào cả. Nên "id cao nhất trong
bảng" thấp hơn hẳn "id cao nhất đang được dùng", và bộ cấp phát thản nhiên lấy những id đã có chủ.

Hậu quả: ảnh cắt từ tấm hiệu ứng ghi đè thẳng lên tệp của NPC và của mấy món đồ cũ. NPC Tashino
hiện ra thành đống hình chồng chéo là vì thế.

## Cách gỡ

Không xoá gì, không đụng tới thứ tự liền mạch của ba bảng. Với mỗi ảnh bị đè:

  1. chép nội dung hiện tại (tức ảnh trang phục) sang một id mới nối vào cuối `nj_image`
  2. trỏ mảnh trang phục sang id mới
  3. trả tệp gốc về đúng id cũ, lấy từ kho client v4
  4. sửa lại kích thước trong hàng `nj_image` của id cũ cho khớp tệp vừa trả

Chọn dời phía trang phục chứ không dời phía cũ, vì id cũ là thứ client gốc và mọi bảng khác
(hiệu ứng, quái, NPC) vẫn đang trỏ tới -- dời chúng thì phải dò lại toàn bộ, còn mảnh trang phục
thì ta nắm trọn.

Dùng:
  python3 tools/go-dung-anh.py          xem thử
  python3 tools/go-dung-anh.py --ghi    làm thật
"""
import json
import os
import shutil
import struct
import subprocess
import sys

DB = 'nso_test'
SMALL = 'work/server/NSO_KEM/Data/Img/Small'
V4 = 'thamkhao/ZnsoC 2/ZnsoC/NsoC/res/assets/icon'
MANH_MOI = set(range(308, 331))          # mảnh trang phục tự thêm
COS = (1, 2, 3, 4)


def q(sql):
    return subprocess.run(['mysql', '-uroot', DB, '-N', '--raw', '-e', sql],
                          capture_output=True, text=True).stdout


def chay(sql):
    r = subprocess.run(['mysql', '-uroot', DB, '-e', sql], capture_output=True, text=True)
    if r.returncode:
        raise SystemExit('sql hỏng: ' + r.stderr.strip())


def co(tep):
    b = open(tep, 'rb').read()
    return struct.unpack('>II', b[16:24])


def main():
    ghi = '--ghi' in sys.argv
    os.chdir(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

    manh = {}
    for l in q("SELECT id,part FROM nj_part;").strip().split('\n'):
        i, p = l.split('\t', 1)
        try:
            manh[int(i)] = json.loads(p)
        except ValueError:
            pass

    cu, moi = {}, {}
    for m, k in manh.items():
        for f in k:
            a = f.get('id', 0)
            if a <= 4 or a == 35:            # khung mặc định, ai cũng dùng chung
                continue
            (moi if m in MANH_MOI else cu).setdefault(a, set()).add(m)

    de = []
    for a in sorted(set(cu) & set(moi)):
        x, y = f'{SMALL}/1/Small{a}.png', f'{V4}/1/Small{a}.png'
        if not os.path.exists(y):
            print(f'  !! ảnh {a} bị đè nhưng kho v4 không có bản gốc -- bỏ qua, phải tìm nguồn khác')
            continue
        if open(x, 'rb').read() != open(y, 'rb').read():
            de.append(a)

    if not de:
        print('  không còn ảnh nào bị đè')
        return
    print(f'  {len(de)} ảnh bị đè, thuộc {len(set().union(*[cu[a] for a in de]))} mảnh cũ')

    dau = int(q("SELECT MAX(id) FROM nj_image;").strip()) + 1
    ban = {a: dau + n for n, a in enumerate(de)}
    print(f'  ảnh trang phục sẽ dời sang id {dau}..{dau + len(de) - 1}')
    if not ghi:
        print('\n  (xem thử -- chạy lại với --ghi để làm thật)')
        return

    hang, x, y = [], 300, 700
    for a in de:
        n = ban[a]
        for z in COS:
            shutil.copy(f'{SMALL}/{z}/Small{a}.png', f'{SMALL}/{z}/Small{n}.png')
        w, h = co(f'{SMALL}/1/Small{n}.png')
        hang.append(f"({n},'[4,{x},{y},{w},{h}]')")
        x += 40
        if x > 560:
            x, y = 300, y + 40
    chay("INSERT INTO nj_image (id, smallImage) VALUES " + ','.join(hang) + ";")

    for a in de:
        for z in COS:
            g = f'{V4}/{z}/Small{a}.png'
            if os.path.exists(g):
                shutil.copy(g, f'{SMALL}/{z}/Small{a}.png')
        w, h = co(f'{SMALL}/1/Small{a}.png')
        cur = q(f"SELECT smallImage FROM nj_image WHERE id={a};").strip()
        try:
            r = json.loads(cur)
        except ValueError:
            r = [4, 300, 900, w, h]
        # ép về đường xin tệp: x >= 256 thì client không cắt từ khung ghép sẵn nữa
        r = [r[0], max(r[1], 300), r[2], w, h]
        chay(f"UPDATE nj_image SET smallImage='{json.dumps(r)}' WHERE id={a};")

    doi = 0
    for m in sorted(MANH_MOI):
        if m not in manh:
            continue
        k = manh[m]
        if not any(f.get('id') in ban for f in k):
            continue
        for f in k:
            if f.get('id') in ban:
                f['id'] = ban[f['id']]
        tep = f'/tmp/go{m}.json'
        open(tep, 'w').write(json.dumps(k))
        chay(f"UPDATE nj_part SET part=LOAD_FILE('{tep}') WHERE id={m};")
        os.remove(tep)
        doi += 1

    print(f'  đã dời {len(de)} ảnh, trả {len(de)} tệp gốc, trỏ lại {doi} mảnh trang phục')
    for t in ('nj_image', 'nj_part', 'item'):
        print(f"    {t}: cao nhất {q(f'SELECT MAX(id) FROM {t};').strip()}, "
              f"lỗ {q(f'SELECT MAX(id)-MIN(id)+1-COUNT(*) FROM {t};').strip()}")


if __name__ == '__main__':
    main()
