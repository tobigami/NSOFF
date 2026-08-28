"""Cấp id ảnh mới cho `nj_image` mà không giẫm lên ảnh đang có chủ.

`MAX(nj_image.id) + 1` **không** phải id trống. `nj_part` được phép trỏ tới id ảnh không có hàng
nào trong `nj_image`: kho gốc của game có hơn hai nghìn tấm nằm dạng tệp rời, client cứ theo id mà
xin tệp. Nên id cao nhất đang dùng nằm ở tệp trên đĩa và ở trong `nj_part`, không phải ở bảng.

Đã trả giá một lần: bộ thêm trang phục lấy id từ 3129 trở đi, ghi đè lên ảnh của NPC Tashino và
vài món đồ cũ. Xem `tools/go-dung-anh.py`.
"""
import json
import os
import re


def id_moi(q, small_dir):
    """Id của hàng `nj_image` kế tiếp, kèm cảnh báo nếu dải đó đang có tệp chiếm chỗ.

    Vẫn là `MAX(id) + 1` chứ không phải id trống đầu tiên: bảng bắt buộc liền mạch, hàng mới chỉ
    có thể nối ngay sau hàng cuối, nhảy cóc là hỏng cả luồng. Việc dọn chỗ là của `don_duong`.
    """
    cao = int(q("SELECT MAX(id) FROM nj_image;").strip() or 0)
    ban = sum(1 for t in os.listdir(f'{small_dir}/1')
              if (m := re.fullmatch(r'Small(\d+)\.png', t)) and int(m.group(1)) > cao)
    if ban:
        print(f'  chú ý: có {ban} tệp ảnh mang id lớn hơn {cao} -- nhớ gọi don_duong '
              f'trước khi ghi đè, không thì giẫm lên chúng')
    return cao + 1


# Vùng dời tạm: khối id liền, không tệp nào chiếm, không hàng nj_image nào chiếm, và **dưới
# 32767** -- `Server.loadParts` đọc id ảnh bằng `shortValue()` nên mảnh không trỏ cao hơn được.
KHO_DOI = 14113


def don_duong(q, chay, small, dau, cuoi, cos=(1, 2, 3, 4)):
    """Dọn dải id [dau, cuoi) trước khi ghi ảnh mới vào đó.

    `nj_image` phải liền mạch nên **không được nhảy cóc** qua id đang bận -- hàng mới bắt buộc nối
    ngay sau hàng cuối. Vậy nên dọn chỗ chứ không né chỗ: ảnh nào đang nằm trong dải thì chép sang
    id trống ở vùng cao, rồi trỏ lại những ai đang dùng nó.

    Trỏ lại được hai nơi: `nj_part` và cột `icon` của `item`. Ảnh nằm trong dải mà **không ai trong
    hai nơi đó dùng** thì vẫn chép đi và báo ra -- có thể hiệu ứng hay bảng khác đang dùng, tự động
    sửa mò thì nguy hơn là nói cho người biết.
    """
    import json as _json
    import shutil as _shutil

    ban = {}
    kho = KHO_DOI
    for a in range(dau, cuoi):
        if not os.path.exists(f'{small}/1/Small{a}.png'):
            continue
        while os.path.exists(f'{small}/1/Small{kho}.png'):
            kho += 1
        if kho > 32767:
            raise SystemExit('hết chỗ dời dưới ngưỡng short 32767 -- phải dọn tay')
        for z in cos:
            g = f'{small}/{z}/Small{a}.png'
            if os.path.exists(g):
                _shutil.copy(g, f'{small}/{z}/Small{kho}.png')
        ban[a] = kho
        kho += 1
    if not ban:
        return {}

    dung = set()
    for l in q("SELECT id,part FROM nj_part;").strip().split('\n'):
        if not l.strip():
            continue
        i, p = l.split('\t', 1)
        try:
            k = _json.loads(p)
        except ValueError:
            continue
        if not any(f.get('id') in ban for f in k):
            continue
        for f in k:
            if f.get('id') in ban:
                f['id'] = ban[f['id']]
                dung.add(f['id'])
        tep = f'/tmp/don{i}.json'
        open(tep, 'w').write(_json.dumps(k))
        chay(f"UPDATE nj_part SET part=LOAD_FILE('{tep}') WHERE id={i};")
        os.remove(tep)

    for cu, moi in ban.items():
        r = q(f"SELECT COUNT(*) FROM item WHERE icon={cu};").strip()
        if r and int(r) > 0:
            chay(f"UPDATE item SET icon={moi} WHERE icon={cu};")
            dung.add(moi)

    mo_coi = [a for a, n in ban.items() if n not in dung]
    print(f'  dọn đường: dời {len(ban)} ảnh khỏi dải {dau}..{cuoi - 1} sang vùng {KHO_DOI}+')
    if mo_coi:
        print(f'  !! {len(mo_coi)} ảnh không tìm ra ai dùng (nj_part/icon đều không) -- '
              f'đã chép sang id mới nhưng chưa trỏ lại được: {sorted(mo_coi)[:10]}')
    return ban
