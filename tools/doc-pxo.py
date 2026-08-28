#!/usr/bin/env python3
"""Lấy ảnh ra từ tệp dự án .pxo của Pixelorama.

Bấm Save trong Pixelorama cho ra .pxo chứ không phải PNG, nhưng bên trong nó là gói zip, và lớp
ảnh nằm ở dạng RGBA thô -- mỗi điểm 4 byte, xếp theo hàng. Kích thước lấy từ data.json. Nhờ vậy
không cần mở lại Pixelorama để xuất, tránh cảnh sửa xong quên bước Export.

Usage: doc-pxo.py <tệp .pxo> <ảnh ra .png>
"""
import json
import struct
import sys
import zipfile
import zlib


def viet_png(duong_dan, rong, cao, rgba):
    def chunk(loai, du_lieu):
        return (struct.pack(">I", len(du_lieu)) + loai + du_lieu
                + struct.pack(">I", zlib.crc32(loai + du_lieu) & 0xFFFFFFFF))

    # Mỗi hàng trong PNG bắt đầu bằng một byte kiểu lọc; 0 là không lọc.
    tho = b"".join(b"\x00" + rgba[y * rong * 4:(y + 1) * rong * 4] for y in range(cao))
    ihdr = struct.pack(">IIBBBBB", rong, cao, 8, 6, 0, 0, 0)
    with open(duong_dan, "wb") as f:
        f.write(b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", ihdr)
                + chunk(b"IDAT", zlib.compress(tho, 9)) + chunk(b"IEND", b""))


def main():
    vao, ra = sys.argv[1], sys.argv[2]
    with zipfile.ZipFile(vao) as z:
        meta = json.loads(z.read("data.json"))
        rong, cao = meta["size_x"], meta["size_y"]
        ten_lop = [n for n in z.namelist() if "/layer_" in n and "indices" not in n]
        du_lieu = z.read(sorted(ten_lop)[0])
    can = rong * cao * 4
    if len(du_lieu) != can:
        print(f"!! lớp ảnh {len(du_lieu)} byte, cần {can} -- định dạng khác, chưa đọc được")
        return
    viet_png(ra, rong, cao, du_lieu)
    print(f"  {ra}: {rong}x{cao} lấy từ {vao}")


if __name__ == "__main__":
    main()
