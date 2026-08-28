"""Rút ảnh gốc từ tệp dự án Pixelorama (.pxo).

Lớp ảnh nằm ở image_data/frames/<n>/layer_<n> dạng RGBA8 thô, kích thước lấy từ data.json.
Ghi ra PNG bằng zlib, không cần thư viện ngoài.
"""
import json, struct, sys, zipfile, zlib

def ghi_png(duong_dan, rong, cao, rgba):
    def khoi(ten, du_lieu):
        c = ten + du_lieu
        return struct.pack('>I', len(du_lieu)) + c + struct.pack('>I', zlib.crc32(c) & 0xffffffff)
    hang = b''.join(b'\x00' + rgba[y*rong*4:(y+1)*rong*4] for y in range(cao))
    png = (b'\x89PNG\r\n\x1a\n'
           + khoi(b'IHDR', struct.pack('>IIBBBBB', rong, cao, 8, 6, 0, 0, 0))
           + khoi(b'IDAT', zlib.compress(hang, 9))
           + khoi(b'IEND', b''))
    open(duong_dan, 'wb').write(png)

def rut(pxo, ra):
    with zipfile.ZipFile(pxo) as z:
        d = json.loads(z.read('data.json'))
        w, h = d['size_x'], d['size_y']
        raw = z.read('image_data/frames/1/layer_1')
    assert len(raw) == w*h*4, f'{len(raw)} != {w*h*4}'
    ghi_png(ra, w, h, raw)
    mau = {raw[i:i+4] for i in range(0, len(raw), 4)}
    print(f'{ra}  {w}x{h}  {len(mau)} màu')

for pxo, ra in zip(sys.argv[1::2], sys.argv[2::2]):
    rut(pxo, ra)
