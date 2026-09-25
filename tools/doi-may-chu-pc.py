#!/usr/bin/env python3
# Trỏ client PC (bản Unity "pb189") về một máy chủ khác, bằng cách sửa thẳng chuỗi danh sách
# máy chủ nằm trong NinjaSchool_189_Data/Managed/Assembly-CSharp.dll.
#
#   python3 tools/doi-may-chu-pc.py <Assembly-CSharp.dll> <ip> [--cong 14444] [--ten TEN] [--ra TEP]
#
# Vì sao phải sửa trong DLL: client không có tệp cấu hình nào chứa địa chỉ, cũng không có ô cho
# người chơi tự gõ. `PlayerPrefs` chỉ nhớ *chỉ số* máy chủ đã chọn (`lastServer`, `indServer`),
# còn danh sách thì nằm cứng trong ba chuỗi của Assembly-CSharp.dll. Các tệp asset của Unity
# (level0, globalgamemanagers, sharedassets0) không có chuỗi nào chứa địa chỉ -- đã dò.
#
# CÁI BẪY CHÍNH: không được đổi ĐỘ DÀI chuỗi.
#
# Chuỗi literal của .NET nằm trong heap #US, mỗi chuỗi là [độ dài nén][UTF-16LE][1 byte cuối], và
# lệnh `ldstr` trong IL trỏ tới chuỗi bằng **offset tuyệt đối** vào heap đó. Viết chuỗi dài hơn là
# đè lên chuỗi kế tiếp; viết ngắn hơn mà dịch phần sau lên là mọi offset phía sau lệch hết. Nên
# script này giữ **y nguyên số ký tự**, chỉ đệm/cắt phần TÊN máy chủ cho vừa. Tên chỉ để hiện ở
# màn chọn máy chủ, thừa vài dấu cách không ảnh hưởng gì.
#
# Bản trong nso.zip đã được người khác vá đúng theo cách này: `Assembly-CSharp.dll.bak` ghi
# `vps.nso.id.vn` còn bản đang dùng ghi `103.28.32.204` -- hai chuỗi **cùng 13 ký tự**, hai tệp
# **cùng 1.025.024 byte**, chỉ khác đúng 36 byte. Cách này chạy được, không phải suy đoán.
#
# Cổng mặc định của client vốn đã là 14444, trùng với `server.port` trong config.properties của
# máy chủ, nên thường chỉ cần đổi mỗi địa chỉ.

import argparse
import re
import sys

# NAME:HOST:PORT:cờ:cờ  nối nhau bằng dấu phẩy. Bắt cả dạng tên miền lẫn dạng IP vì client gốc
# dùng nj*.teamobi.com, còn các bản vá lại thì dùng IP.
MAU = re.compile(r'^[^:,]+:[A-Za-z0-9.\-]+:\d+:\d+:\d+(?:,[^:,]+:[A-Za-z0-9.\-]+:\d+:\d+:\d+)*$')


def tim_danh_sach(data):
    """Trả về [(offset, chuỗi)] cho mọi chuỗi UTF-16 trông như danh sách máy chủ."""
    ra = []
    for m in re.finditer(rb'(?:[\x20-\x7e]\x00){20,}', data):
        chuoi = m.group().decode('utf-16-le')
        if MAU.match(chuoi) and ':' in chuoi:
            ra.append((m.start(), chuoi))
    return ra


def dung_lai(chuoi, ip, cong, ten):
    """Dựng danh sách mới trỏ về ip:cong, dài ĐÚNG bằng chuỗi cũ.

    Giữ nguyên số mục và hai cờ cuối của từng mục -- không biết chúng để làm gì (mục cuối của
    mọi danh sách đều là `:0:1`), nên chép lại thay vì tự đặt.
    """
    muc_cu = chuoi.split(',')
    duoi = []
    for m in muc_cu:
        phan = m.split(':')
        duoi.append(f':{ip}:{cong}:{phan[3]}:{phan[4]}')

    con_lai = len(chuoi) - sum(len(d) for d in duoi) - (len(muc_cu) - 1)
    if con_lai < len(muc_cu):
        raise SystemExit(
            f'địa chỉ quá dài: {ip}:{cong} không nhét vừa {len(chuoi)} ký tự với {len(muc_cu)} mục'
        )

    # Chia đều số ký tự còn lại cho các tên; phần dư dồn vào những tên đầu.
    day = con_lai // len(muc_cu)
    du = con_lai % len(muc_cu)
    ra = []
    for i, d in enumerate(duoi):
        n = day + (1 if i < du else 0)
        ra.append(ten[:n].ljust(n) + d)
    moi = ','.join(ra)
    assert len(moi) == len(chuoi), (len(moi), len(chuoi))
    return moi


def main():
    p = argparse.ArgumentParser()
    p.add_argument('dll')
    p.add_argument('ip')
    p.add_argument('--cong', default='14444')
    p.add_argument('--ten', default='May nha')
    p.add_argument('--ra', help='tệp ghi ra (mặc định: <dll>.moi)')
    a = p.parse_args()

    data = bytearray(open(a.dll, 'rb').read())
    ds = tim_danh_sach(data)
    if not ds:
        raise SystemExit('không tìm thấy chuỗi danh sách máy chủ nào trong ' + a.dll)

    # Vá HẾT, không riêng chuỗi đang dùng. Client còn hai danh sách gốc của teamobi làm đường lui
    # (và một đường `NJlink` tải danh sách qua HTTP); để nguyên là có lúc nó lại nối ra ngoài.
    for off, cu in ds:
        moi = dung_lai(cu, a.ip, a.cong, a.ten)
        data[off:off + len(cu) * 2] = moi.encode('utf-16-le')
        print(f'  {off}: {cu[:46]}…')
        print(f'  {" " * len(str(off))}  -> {moi[:46]}…')

    ra = a.ra or a.dll + '.moi'
    open(ra, 'wb').write(bytes(data))
    goc = len(open(a.dll, 'rb').read())
    assert len(data) == goc, 'kích thước tệp đã đổi -- sai rồi'
    print(f'xong: {ra}  ({len(ds)} danh sách, {goc} byte, không đổi kích thước)')


if __name__ == '__main__':
    main()
