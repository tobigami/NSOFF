#!/usr/bin/env python3
"""Nạp chỉ số cho những món trang phục trang bị 2 đã nằm sẵn trong tay người chơi.

`Item.initOption()` chỉ chạy lúc món được tạo ra. Món phát trước khi có nhánh chỉ số thì trong
kho vẫn là `"options": []`, và nó nằm im như thế mãi vì lúc đăng nhập máy chủ đọc thẳng từ JSON.
Nên sau khi sửa `initOption()` phải quét lại kho một lượt.

Bảng chỉ số ở đây **chép đúng** nhánh `initOptionTrangPhucMoi()` bên máy chủ. Sửa một bên thì
phải sửa bên kia, không thì món cũ và món mới lệch chỉ số nhau.

Thứ tự bắt buộc: sửa mã nguồn -> khởi động lại máy chủ -> mới chạy cái này. Chạy trước lúc khởi
động lại thì máy chủ còn giữ bản cũ trong bộ nhớ và ghi đè sạch khi người chơi thoát.

Dùng:
  python3 tools/nap-chi-so-trang-phuc.py          xem thử, không ghi
  python3 tools/nap-chi-so-trang-phuc.py --ghi    ghi thật
"""
import json
import os
import subprocess
import sys

DB = 'nso_test'
COT = ('bag', 'box', 'fashion', 'equiped')
MATNA = [[82, 2000], [87, 2000], [69, 12], [58, 25]]
# Chỉ số riêng theo từng món -- chép đúng bên máy chủ (initOptionCauLucDao và nhánh TYPE_BIKIP
# trong initOption). Món nào có tên ở đây thì dùng bảng này, không rơi vào bảng chung bên dưới.
RIENG = {
    # Bốn danh hiệu Hokage: Item.java, nhánh TYPE_BIKIP
    1150: [[82, 5000], [128, 12], [99, 200], [124, 800], [118, 100], [87, 1000]],
    1151: [[87, 2000], [116, 600], [126, 500], [113, 300], [117, 5000], [119, 400]],
    1152: [[82, 2500], [117, 2500], [87, 2500], [92, 50], [115, 250], [116, 250],
           [118, 150], [124, 400]],
    1153: [[82, 1000], [87, 1000], [93, 2], [115, 600], [92, 200], [67, 30], [105, 800]],
    1263: [[125, 5000], [117, 5000], [58, 15], [87, 6000], [94, 25], [113, 800], [114, 120],
           [67, 50], [105, 3000]],
    1264: [[125, 9000], [117, 5000], [58, 10], [87, 4500], [94, 18], [113, 500], [114, 80],
           [116, 300], [124, 600], [121, 20]],
}
AO_QUAN = [[125, 5000], [117, 5000], [94, 12], [127, 12], [130, 12], [131, 12]]


def q(sql):
    return subprocess.run(['mysql', '-uroot', DB, '-N', '--raw', '-e', sql],
                          capture_output=True, text=True).stdout


def chay(sql):
    r = subprocess.run(['mysql', '-uroot', DB, '-e', sql], capture_output=True, text=True)
    if r.returncode:
        raise SystemExit('sql hỏng: ' + r.stderr.strip())


def main():
    ghi = '--ghi' in sys.argv
    os.chdir(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

    loai = {}
    for l in q("SELECT id,type,name FROM item WHERE id IN (1150,1151,1152,1153,1236) OR id BETWEEN 1241 AND 1264;").strip().split('\n'):
        p = l.split('\t')
        loai[int(p[0])] = (int(p[1]), '\t'.join(p[2:]))

    ai = q("SELECT id,name FROM players WHERE online=1;").strip()
    if ai:
        print('  !! đang có người trong game -- ghi lúc này sẽ bị đè khi họ thoát:', ai.replace('\n', ', '))

    sua, nguoi = 0, 0
    for l in q("SELECT id," + ','.join(COT) + " FROM players;").split('\n'):
        if not l.strip():
            continue
        p = l.split('\t')
        pid, doi = p[0], {}
        for ten, raw in zip(COT, p[1:]):
            if not raw or raw == 'NULL':
                continue
            try:
                arr = json.loads(raw)
            except ValueError:
                continue
            if not isinstance(arr, list):
                continue
            co = False
            for it in arr:
                if not isinstance(it, dict) or it.get('id') not in loai:
                    continue
                can = RIENG.get(it['id']) or (MATNA if loai[it['id']][0] == 11 else AO_QUAN)
                if it.get('options') != can:
                    it['options'] = [list(x) for x in can]
                    co = True
                    sua += 1
            if co:
                doi[ten] = arr
        if doi:
            nguoi += 1
            print(f"  người {pid}: " + ', '.join(f'{k} ({len(v)} ô)' for k, v in doi.items()))
            if ghi:
                for ten, arr in doi.items():
                    tep = f'/tmp/nap{pid}{ten}.json'
                    open(tep, 'w').write(json.dumps(arr, ensure_ascii=False, separators=(',', ':')))
                    chay(f"UPDATE players SET {ten}=LOAD_FILE('{tep}') WHERE id={pid};")
                    os.remove(tep)

    print(f"\n  {sua} món ở {nguoi} người " + ("đã nạp chỉ số" if ghi else "cần nạp (chạy lại với --ghi)"))


if __name__ == '__main__':
    main()
