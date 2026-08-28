#!/usr/bin/env python3
"""Dò xem nhân vật nào còn giữ vật phẩm đã bị xoá khỏi bảng item.

Máy chủ nạp nhân vật bằng cách tra mẫu vật phẩm theo id. Id không còn trong bảng thì tra ra ngoài
mảng, nạp hỏng, và client **rơi thẳng về màn hình chính mà không một dòng log nào**. Rất khó đoán
nếu không biết trước.

Nên sau mỗi lần xoá vật phẩm phải chạy cái này. Và phải chạy **sau khi khởi động lại máy chủ**:
máy chủ giữ nhân vật trong bộ nhớ, dọn trước lúc khởi động lại thì nó ghi đè lại y như cũ.

Dùng:
  python3 tools/kiem-mon-chet.py          chỉ báo
  python3 tools/kiem-mon-chet.py --don    báo rồi gỡ luôn
"""
import json
import subprocess
import sys
import os

DB = 'nso_test'
COT = ('bag', 'box', 'equiped', 'fashion', 'mask_box', 'collection_box')


def q(sql):
    return subprocess.run(['mysql', '-uroot', DB, '-N', '--raw', '-e', sql],
                          capture_output=True, text=True).stdout


def main():
    don = '--don' in sys.argv
    co = {int(x) for x in q("SELECT id FROM item;").split()}
    dangChoi = q("SELECT GROUP_CONCAT(name) FROM players WHERE online=1;").strip()
    if dangChoi:
        print(f'!! đang có người trong game ({dangChoi}) -- dọn lúc này sẽ bị ghi đè khi họ thoát')
    tong = 0
    for pid in [int(x) for x in q("SELECT id FROM players;").split()]:
        raw = q(f"SELECT {','.join(COT)} FROM players WHERE id={pid};").rstrip('\n').split('\t')
        ten = q(f"SELECT name FROM players WHERE id={pid};").strip()
        for cot, v in zip(COT, raw):
            if v in ('', 'NULL'):
                continue
            d = json.loads(v)
            chet = [(i.get('id'), i.get('name')) for i in d
                    if isinstance(i, dict) and i.get('id') not in co]
            if not chet:
                continue
            tong += len(chet)
            print(f'  {ten} · {cot}: {chet}')
            if don:
                d2 = [i for i in d if not (isinstance(i, dict) and i.get('id') not in co)]
                for n, i in enumerate(d2):
                    if 'index' in i:
                        i['index'] = n
                f = f'/tmp/kmc{pid}{cot}.json'
                open(f, 'w').write(json.dumps(d2, ensure_ascii=False, separators=(',', ':')))
                q(f"UPDATE players SET {cot}=LOAD_FILE('{f}') WHERE id={pid};")
                os.remove(f)
    print(f'tổng: {tong} chỗ giữ món đã xoá' + (' -- đã gỡ hết' if don and tong else ''))


if __name__ == '__main__':
    main()
