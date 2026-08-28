#!/usr/bin/env python3
"""Thêm một bộ trang phục ba mảnh (nón, áo, quần) từ ba tấm hiệu ứng có sẵn.

Bộ thứ ba làm bằng tay là đủ để thấy quy trình lặp lại y hệt, nên gói lại thành một lệnh. Mọi con
số đều lấy từ bản thiết kế gốc nằm trong `effect_data`, không nắn bằng mắt:

  frames  bảng 30 hoặc 60 khung, khớp một-một với 30 khung `CharInfo` của nhân vật.
          Mỗi khung là danh sách lớp; đầu và thân lấy lớp [0] (mấy lớp sau là hiệu ứng đòn đánh),
          chân lấy lớp có dy gần 0 nhất (tấm chân trộn thêm một lớp vẽ ngang tầm đầu, và thứ tự
          hai lớp đảo qua đảo lại giữa các khung).
  sprites toạ độ từng ô trên tấm.

Chiếu qua `CharInfo` sẽ ra: mỗi ô mảnh dùng ô ảnh nào, lệch bao nhiêu. Một ô mảnh có thể gánh nhiều
khung nhân vật, khi đó lấy ô ảnh hay gặp nhất và lệch trung bình.

Ba bảng id đều phải liền mạch nên mọi thứ đều nối vào cuối. Riêng biểu tượng thì để ở vùng 30000,
tách hẳn khỏi `nj_image` -- không thì mỗi lần thêm bộ mới, ảnh mới lại nuốt mất id biểu tượng cũ.

Dùng:
  python3 tools/them-trang-phuc.py <tênBộ> <tấmĐầu> <tấmThân> <tấmChân> <giới> <mãĐầu>
ví dụ:
  python3 tools/them-trang-phuc.py Deidara 225 226 227 1 266
"""
import json
import os
import shutil
import subprocess
import struct
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from idanh import don_duong, id_moi

DB = 'nso_test'
GOC = 'work/server/NSO_KEM'
ANH = f'{GOC}/Data/Img'
SMALL = f'{ANH}/Small'
# 0 đầu, 1 chân, 2 thân -- thứ tự ô trong CharInfo, xem tools/charinfo.json
O_CHARINFO = {'dau': 0, 'chan': 1, 'than': 2}
SO_KHUNG = {'dau': 8, 'than': 18, 'chan': 10}
LOAI_MANH = {'dau': 0, 'than': 1, 'chan': 2}
LOAI_MON = {'dau': 11, 'than': 2, 'chan': 6}
TEN_MON = {'dau': 'Nón', 'than': 'Áo', 'chan': 'Quần'}


def q(sql):
    return subprocess.run(['mysql', '-uroot', DB, '-N', '--raw', '-e', sql],
                          capture_output=True, text=True).stdout


def chay(sql):
    r = subprocess.run(['mysql', '-uroot', DB, '-e', sql], capture_output=True, text=True)
    if r.returncode:
        raise SystemExit('sql hỏng: ' + r.stderr.strip())


def kichThuoc(idAnh):
    b = open(f'{SMALL}/1/Small{idAnh}.png', 'rb').read()
    return struct.unpack('>II', b[16:24])


def doBang(tam, phan, charInfo):
    """Ra bảng {ô mảnh: {ô ảnh, dx, dy}} cho một tấm."""
    frames = json.loads(q(f"SELECT frames FROM effect_data WHERE id={tam};").strip())
    gom = {}
    for cf in range(30):
        o = charInfo[cf][O_CHARINFO[phan]]
        if o == [0, 0, 0] or cf >= len(frames) or not frames[cf]:
            continue
        lop = max(frames[cf], key=lambda x: x['dy']) if phan == 'chan' else frames[cf][0]
        # khoá để dạng chuỗi cho khớp lúc tra ở dưới -- để số nguyên là tra hụt, mọi ô mảnh rơi
        # vào nhánh dự phòng và cả bộ dùng chung một ảnh
        gom.setdefault(str(o[0]), []).append((lop['id'], lop['dx'] - o[1], o[2] + lop['dy']))
    bang = {}
    for k, v in gom.items():
        anh = [x[0] for x in v]
        bang[k] = {'o': max(set(anh), key=anh.count),
                   'dx': round(sum(x[1] for x in v) / len(v)),
                   'dy': round(sum(x[2] for x in v) / len(v))}
    return bang


def main():
    if len(sys.argv) < 7:
        raise SystemExit(__doc__)
    tenBo, tamDau, tamThan, tamChan, gioi, maDau = (sys.argv[1], int(sys.argv[2]), int(sys.argv[3]),
                                                    int(sys.argv[4]), int(sys.argv[5]), int(sys.argv[6]))
    os.chdir(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
    charInfo = json.load(open('tools/charinfo.json'))
    tam = {'dau': tamDau, 'than': tamThan, 'chan': tamChan}
    bang = {p: doBang(tam[p], p, charInfo) for p in ('dau', 'than', 'chan')}

    anhDau = id_moi(q, SMALL)
    manhDau = int(q("SELECT MAX(id) FROM nj_part;").strip()) + 1
    monDau = int(q("SELECT MAX(id) FROM item;").strip()) + 1
    iconDau = max(30000, int(q("SELECT MAX(icon) FROM item WHERE icon >= 30000;").strip() or 30000) + 1)

    # 1. Tính trước cả ba dải rồi mới cắt. Phải theo thứ tự này: `CatO` ghi thẳng lên tệp, nên
    # dọn đường xong mới được cắt -- cắt trước thì tệp gốc đã mất, dọn cái gì nữa.
    thu = {}
    for p in ('dau', 'than', 'chan'):
        b = bang[p]
        thu[p] = [b[k]['o'] for k in sorted(b, key=int)]
    don_duong(q, chay, SMALL, anhDau, anhDau + sum(len(v) for v in thu.values()))

    viTri = {}
    cur = anhDau
    for p in ('dau', 'than', 'chan'):
        sp = json.loads(q(f"SELECT sprites FROM effect_data WHERE id={tam[p]};").strip())
        rects = ';'.join(f"{sp[i]['x']},{sp[i]['y']},{sp[i]['w']},{sp[i]['h']}" for i in thu[p])
        subprocess.run(['java', '-cp', 'build/selftest', 'CatO', ANH, str(tam[p]), str(cur), rects],
                       check=True)
        viTri[p] = cur
        cur += len(thu[p])

    # 1b. Dời biểu tượng nào sắp bị ảnh hưởng. Hơn bốn trăm vật phẩm dùng biểu tượng theo tệp,
    # id của chúng nằm rải từ 3187 trở lên. Khi `nj_image` mọc tới đó thì id ấy có hàng, và client
    # vẽ mảnh trang phục thay cho biểu tượng. Bản gốc của mấy tệp đó lấy lại từ bản v4 trong gpt.
    V4 = 'thamkhao/ZnsoC 2/ZnsoC/NsoC/res/assets/icon'
    dinh = [l.split('\t') for l in
            q(f"SELECT id,icon FROM item WHERE icon BETWEEN {anhDau} AND {cur - 1};").strip().split('\n') if l]
    if dinh:
        iconAn = max(30100, int(q("SELECT MAX(icon) FROM item WHERE icon >= 30100;").strip() or 30099) + 1)
        banDo = {}
        for _, ic in dinh:
            if ic not in banDo:
                banDo[ic] = iconAn
                iconAn += 1
        for cu, new in banDo.items():
            for z in (1, 2, 3, 4):
                # Lấy từ kho của mình trước: lúc này tệp cũ còn nguyên vì chưa cắt đè lên.
                # Kho hồi ức chỉ là chỗ dự phòng, và nó không có đủ mọi biểu tượng của mình.
                for nguon in (f'{SMALL}/{z}/Small{cu}.png', f'{V4}/{z}/Small{cu}.png'):
                    if os.path.exists(nguon):
                        shutil.copy(nguon, f'{SMALL}/{z}/Small{new}.png')
                        break
                else:
                    print(f'  !! không tìm ra tệp gốc cho biểu tượng {cu} cỡ {z}')
        for it, ic in dinh:
            chay(f"UPDATE item SET icon={banDo[ic]} WHERE id={it};")
        print(f"  đã dời {len(dinh)} biểu tượng ra khỏi vùng ảnh mới")

    # 2. hàng nj_image, toạ độ x >= 256 để buộc client xin tệp thay vì cắt từ khung có sẵn
    hang, x, y = [], 300, 520
    for i in range(anhDau, cur):
        w, h = kichThuoc(i)
        hang.append(f"({i},'[4,{x},{y},{w},{h}]')")
        x += 40
        if x > 560:
            x, y = 300, y + 40
    chay("INSERT INTO nj_image (id, smallImage) VALUES " + ','.join(hang) + ";")

    # 3. ba mảnh và ba vật phẩm
    ma = {'dau': maDau, 'than': maDau + 1, 'chan': maDau + 2}
    dong = []
    for n, p in enumerate(('dau', 'than', 'chan')):
        b, dauAnh, soK = bang[p], viTri[p], SO_KHUNG[p]
        ks = sorted(b, key=int)
        khung = []
        for k in range(soK):
            s = str(k)
            if s in b:
                khung.append({"dx": b[s]['dx'], "dy": b[s]['dy'], "id": dauAnh + ks.index(s)})
            else:
                khung.append({"dx": 0, "dy": 0, "id": 0 if k == 0 else dauAnh})
        tep = f'/tmp/manh{manhDau + n}.json'
        open(tep, 'w').write(json.dumps(khung))
        chay(f"INSERT INTO nj_part (id,type,part) VALUES "
             f"({manhDau + n},{LOAI_MANH[p]},LOAD_FILE('{tep}'));")
        os.remove(tep)
        ten = f"{TEN_MON[p]} {tenBo}"
        icon = iconDau + n
        for z in (1, 2, 3, 4):
            shutil.copy(f'{SMALL}/{z}/Small{dauAnh}.png', f'{SMALL}/{z}/Small{icon}.png')
        chay(f"INSERT INTO item (id,name,type,gender,description,level,icon,part,fashion,isUpToUp) "
             f"VALUES ({monDau + n},'{ten}',{LOAI_MON[p]},{gioi},'Trang phục {tenBo}.',20,"
             f"{icon},0,{ma[p]},0);")
        dong.append(f"            case {ma[p]}:\n"
                    f"                return new int[]{{{manhDau + n if p == 'dau' else -1}, "
                    f"{manhDau + n if p == 'than' else -1}, "
                    f"{manhDau + n if p == 'chan' else -1}}};        // {ten}")
        print(f"  {ten}: vật phẩm {monDau + n}, mảnh {manhDau + n}, ảnh {dauAnh}.., icon {icon}, mã {ma[p]}")

    print("\nthêm vào caiTrang() trong FashionFromEquip.java:")
    print('\n'.join(dong))
    print()
    for t in ('item', 'nj_part', 'nj_image'):
        lo = q(f"SELECT MAX(id)-MIN(id)+1-COUNT(*) FROM {t};").strip()
        cao = q(f"SELECT MAX(id) FROM {t};").strip()
        print(f"  {t}: cao nhất {cao}, lỗ {lo}")


if __name__ == '__main__':
    main()
