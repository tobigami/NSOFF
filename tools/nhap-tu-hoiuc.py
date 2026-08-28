#!/usr/bin/env python3
"""Nhập một bộ trang phục ba mảnh từ bản hồi ức (`nso_hoiuc`) sang bản của mình.

Khác với `them-trang-phuc.py` (cắt từ tấm hiệu ứng), ở đây mảnh đã dựng sẵn bên kia -- chỉ việc
chép ảnh và đánh số lại. Đỡ được cả khâu dò `effect_data` lẫn khâu nắn lệch.

Hai bản đánh số ảnh **khác nhau**, nên không bê id nguyên xi được. Cách xử lý:

  - Ảnh nào ở hai bên giống hệt từng byte (mấy khung mặc định 1, 2, 3, 4, 35) thì giữ nguyên id.
  - Ảnh nào chỉ có bên kia thì cấp id mới nối vào cuối `nj_image`, chép đủ bốn cỡ.

Kho ảnh của hồi ức nằm ở `res/assets/icon/<cỡ>/Small<id>.png` -- tên thư mục là "icon" nhưng nó
chứa tất cả ảnh nhỏ, cả biểu tượng lẫn mảnh ghép.

Biểu tượng vật phẩm để ở vùng 30000, và trước khi chèn ảnh mới thì dời biểu tượng nào đụng dải id
sắp dùng -- cùng lý do đã ghi trong `them-trang-phuc.py`.

Dùng:
  python3 tools/nhap-tu-hoiuc.py <tênBộ> <mảnhĐầu> <mảnhThân> <mảnhChân> <giới> <mãĐầu>
ví dụ:
  python3 tools/nhap-tu-hoiuc.py "Naruto Hiền Nhân" 233 234 235 1 278
"""
import json
import os
import shutil
import struct
import subprocess
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from idanh import don_duong, id_moi

NGUON = 'nso_hoiuc'
DICH = 'nso_test'
GOC = 'work/server/NSO_KEM'
SMALL = f'{GOC}/Data/Img/Small'
KHO_V4 = 'thamkhao/ZnsoC 2/ZnsoC/NsoC/res/assets/icon'
LOAI_MANH = {'dau': 0, 'than': 1, 'chan': 2}
LOAI_MON = {'dau': 11, 'than': 2, 'chan': 6}
TEN_MON = {'dau': 'Nón', 'than': 'Áo', 'chan': 'Quần'}


def q(db, sql):
    return subprocess.run(['mysql', '-uroot', db, '-N', '--raw', '-e', sql],
                          capture_output=True, text=True).stdout


def q_dich(sql):
    return q(DICH, sql)


def chay(sql):
    r = subprocess.run(['mysql', '-uroot', DICH, '-e', sql], capture_output=True, text=True)
    if r.returncode:
        raise SystemExit('sql hỏng: ' + r.stderr.strip())


def giongHet(idAnh):
    """Ảnh cùng id ở hai bên có trùng từng byte không, ở cả bốn cỡ."""
    for z in (1, 2, 3, 4):
        a, b = f'{SMALL}/{z}/Small{idAnh}.png', f'{KHO_V4}/{z}/Small{idAnh}.png'
        if not (os.path.exists(a) and os.path.exists(b)):
            return False
        if open(a, 'rb').read() != open(b, 'rb').read():
            return False
    return True


def main():
    if len(sys.argv) < 7:
        raise SystemExit(__doc__)
    tenBo = sys.argv[1]
    manhNguon = {'dau': int(sys.argv[2]), 'than': int(sys.argv[3]), 'chan': int(sys.argv[4])}
    gioi, maDau = int(sys.argv[5]), int(sys.argv[6])
    os.chdir(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

    khung = {p: json.loads(q(NGUON, f"SELECT part FROM nj_part WHERE id={manhNguon[p]};").strip())
             for p in ('dau', 'than', 'chan')}
    canAnh = sorted({f['id'] for k in khung.values() for f in k if f.get('id')})

    giuNguyen = [i for i in canAnh if giongHet(i)]
    chepMoi = [i for i in canAnh if i not in giuNguyen]
    thieu = [i for i in chepMoi
             if not all(os.path.exists(f'{KHO_V4}/{z}/Small{i}.png') for z in (1, 2, 3, 4))]
    if thieu:
        raise SystemExit(f'kho hồi ức thiếu ảnh (không đủ bốn cỡ): {thieu}')
    print(f'{len(canAnh)} ảnh: {len(giuNguyen)} cái đã có sẵn giống hệt, {len(chepMoi)} cái phải chép')

    anhDau = id_moi(lambda sql: q(DICH, sql), SMALL)
    banDo = {i: i for i in giuNguyen}
    for n, i in enumerate(chepMoi):
        banDo[i] = anhDau + n
    cuoi = anhDau + len(chepMoi)

    # dời biểu tượng nào rơi vào dải id sắp dùng, lấy bản gốc từ kho hồi ức
    dinh = [l.split('\t') for l in
            q(DICH, f"SELECT id,icon FROM item WHERE icon BETWEEN {anhDau} AND {cuoi - 1};").strip().split('\n') if l]
    if dinh:
        an = max(30100, int(q(DICH, "SELECT MAX(icon) FROM item WHERE icon >= 30100;").strip() or 30099) + 1)
        doi = {}
        for _, ic in dinh:
            if ic not in doi:
                doi[ic] = an
                an += 1
        for cu, moi in doi.items():
            for z in (1, 2, 3, 4):
                # kho của mình trước, kho hồi ức chỉ dự phòng -- xem ghi chú ở them-trang-phuc.py
                for ng in (f'{SMALL}/{z}/Small{cu}.png', f'{KHO_V4}/{z}/Small{cu}.png'):
                    if os.path.exists(ng):
                        shutil.copy(ng, f'{SMALL}/{z}/Small{moi}.png')
                        break
                else:
                    print(f'  !! không tìm ra tệp gốc cho biểu tượng {cu} cỡ {z}')
        for it, ic in dinh:
            chay(f"UPDATE item SET icon={doi[ic]} WHERE id={it};")
        print(f'  đã dời {len(dinh)} biểu tượng ra khỏi dải ảnh mới')

    don_duong(q_dich, chay, SMALL, anhDau, cuoi)
    for cu in chepMoi:
        for z in (1, 2, 3, 4):
            shutil.copy(f'{KHO_V4}/{z}/Small{cu}.png', f'{SMALL}/{z}/Small{banDo[cu]}.png')

    hang, x, y = [], 300, 560
    for cu in chepMoi:
        moi = banDo[cu]
        b = open(f'{SMALL}/1/Small{moi}.png', 'rb').read()
        w, h = struct.unpack('>II', b[16:24])
        hang.append(f"({moi},'[4,{x},{y},{w},{h}]')")
        x += 40
        if x > 560:
            x, y = 300, y + 40
    if hang:
        chay("INSERT INTO nj_image (id, smallImage) VALUES " + ','.join(hang) + ";")

    manhDau = int(q(DICH, "SELECT MAX(id) FROM nj_part;").strip()) + 1
    monDau = int(q(DICH, "SELECT MAX(id) FROM item;").strip()) + 1
    iconDau = max(30000, int(q(DICH, "SELECT MAX(icon) FROM item WHERE icon BETWEEN 30000 AND 30099;").strip() or 30000) + 1)
    ma = {'dau': maDau, 'than': maDau + 1, 'chan': maDau + 2}
    dong = []
    for n, p in enumerate(('dau', 'than', 'chan')):
        moi = [{'dx': f['dx'], 'dy': f['dy'], 'id': banDo.get(f['id'], 0)} for f in khung[p]]
        tep = f'/tmp/nhap{manhDau + n}.json'
        open(tep, 'w').write(json.dumps(moi))
        chay(f"INSERT INTO nj_part (id,type,part) VALUES ({manhDau + n},{LOAI_MANH[p]},LOAD_FILE('{tep}'));")
        os.remove(tep)
        anhIcon = next((f['id'] for f in moi if f['id'] > 4), moi[1]['id'])
        icon = iconDau + n
        for z in (1, 2, 3, 4):
            shutil.copy(f'{SMALL}/{z}/Small{anhIcon}.png', f'{SMALL}/{z}/Small{icon}.png')
        ten = f"{TEN_MON[p]} {tenBo}"
        chay(f"INSERT INTO item (id,name,type,gender,description,level,icon,part,fashion,isUpToUp) "
             f"VALUES ({monDau + n},'{ten}',{LOAI_MON[p]},{gioi},'Trang phục {tenBo}.',20,{icon},0,{ma[p]},0);")
        dong.append(f"            case {ma[p]}:\n"
                    f"                return new int[]{{{manhDau + n if p == 'dau' else -1}, "
                    f"{manhDau + n if p == 'than' else -1}, "
                    f"{manhDau + n if p == 'chan' else -1}}};        // {ten}")
        print(f"  {ten}: vật phẩm {monDau + n}, mảnh {manhDau + n} (hồi ức {manhNguon[p]}), icon {icon}, mã {ma[p]}")

    print("\nthêm vào caiTrang() trong FashionFromEquip.java:")
    print('\n'.join(dong))
    print()
    for t in ('item', 'nj_part', 'nj_image'):
        print(f"  {t}: cao nhất {q(DICH, f'SELECT MAX(id) FROM {t};').strip()},"
              f" lỗ {q(DICH, f'SELECT MAX(id)-MIN(id)+1-COUNT(*) FROM {t};').strip()}")


if __name__ == '__main__':
    main()
