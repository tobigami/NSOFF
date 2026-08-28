#!/usr/bin/env python3
"""Hạ lệnh ngủ ghi cứng trong hàm đánh của client.

Vì sao phải sửa client: máy chủ gửi thời gian chờ 100ms, client tuân thủ đúng -- nhưng ngay trong
hàm đánh nó còn `Thread.sleep(500)`. Không log nào bên máy chủ thấy được lệnh ngủ ấy; nó chỉ lộ ra
ở khoảng cách giữa hai gói tin: đo được xen kẽ ~100ms rồi ~550ms, khớp đúng hai lệnh sleep(100) và
sleep(500) nằm cạnh nhau trong cùng một hàm.

Vì sao vá thẳng byte chứ không đọc-ghi lại lớp bằng ASM: MicroEmulator tự chạy ASM lên từng lớp lúc
nạp jar, và một lớp do ASM 3.1 ghi lại thì nó bỏ nguyên cả jar (đã thử: bản sao chép thuần nạp 59
lớp, bản ghi lại nạp 0). Đổi số hiệu hằng số trong lệnh ldc2_w chỉ động vào 2 byte, kích thước lớp
không đổi, không lệnh nhảy nào xê dịch, nên MicroEmulator không có gì để phàn nàn.

Dùng: ha-ngu-danh.py <jar vào> <jar ra> [ms mới]
"""
import re
import struct
import sys
import zipfile

NGU_CU = 500           # lệnh ngủ cần hạ

# Hàm gửi gói đánh, nhận diện bằng độ dài tên + hình dạng mô tả. Mọi hàm GỌI nó đều nằm trên
# đường ra đòn -- đó là tiêu chí lọc, thay vì đoán xem lớp nào là lớp nào giữa một rừng tên rối.
GUI_DON_LOP = 228
GUI_DON_HAM = 229
GUI_DON_MOTA = re.compile(r'^\(L[^;]+;L[^;]+;I\)V$')


def doc_pool(b):
    """Trả về (danh sách mục, vị trí kết thúc pool). Mục là (tag, giá trị)."""
    n = struct.unpack_from('>H', b, 8)[0]
    muc = [None] * n
    i, p = 1, 10
    while i < n:
        tag = b[p]
        if tag == 1:                                  # Utf8
            ln = struct.unpack_from('>H', b, p + 1)[0]
            muc[i] = (tag, b[p + 3:p + 3 + ln].decode('utf-8', 'replace'))
            p += 3 + ln
        elif tag in (7, 8, 16, 19, 20):
            muc[i] = (tag, struct.unpack_from('>H', b, p + 1)[0]); p += 3
        elif tag == 15:
            muc[i] = (tag, None); p += 4
        elif tag in (3, 4):
            muc[i] = (tag, struct.unpack_from('>i', b, p + 1)[0]); p += 5
        elif tag == 5:                                # Long
            muc[i] = (tag, struct.unpack_from('>q', b, p + 1)[0]); p += 9; i += 1
        elif tag == 6:                                # Double
            muc[i] = (tag, None); p += 9; i += 1
        elif tag in (9, 10, 11, 12, 17, 18):
            muc[i] = (tag, struct.unpack_from('>HH', b, p + 1)); p += 5
        else:
            raise ValueError('tag lạ %d ở %d' % (tag, p))
        i += 1
    return muc, p


def bo_qua_attr(b, p):
    n = struct.unpack_from('>H', b, p)[0]
    p += 2
    for _ in range(n):
        ln = struct.unpack_from('>I', b, p + 2)[0]
        p += 6 + ln
    return p


def vung_ma(b, p_sau_pool, muc):
    """Danh sách (tên hàm, đầu mã, dài mã) theo đúng thứ tự trong tệp."""
    p = p_sau_pool + 6                                 # access, this, super
    ni = struct.unpack_from('>H', b, p)[0]; p += 2 + 2 * ni
    for _ in range(2):                                 # fields rồi methods
        n = struct.unpack_from('>H', b, p)[0]; p += 2
        ket = []
        for _ in range(n):
            ten = muc[struct.unpack_from('>H', b, p + 2)[0]][1]
            na = struct.unpack_from('>H', b, p + 6)[0]
            p += 8
            for _ in range(na):
                ta = muc[struct.unpack_from('>H', b, p)[0]][1]
                ln = struct.unpack_from('>I', b, p + 2)[0]
                if ta == 'Code':
                    dai = struct.unpack_from('>I', b, p + 10)[0]
                    ket.append((ten, p + 14, dai))
                p += 6 + ln
        last = ket
    return last


def ref_gui_don(muc):
    """Số hiệu mọi mục Methodref trỏ tới hàm gửi gói đánh."""
    ra = []
    for i, m in enumerate(muc):
        if not m or m[0] != 10:
            continue
        lop, nt = m[1]
        if muc[lop][0] != 7:
            continue
        ten_lop = muc[muc[lop][1]][1]
        ten = muc[muc[nt][1][0]][1]
        mota = muc[muc[nt][1][1]][1]
        if (len(ten_lop) == GUI_DON_LOP and len(ten) == GUI_DON_HAM
                and GUI_DON_MOTA.match(mota)):
            ra.append(i)
    return ra


def co_goi(ma, refs):
    for i in refs:
        b = struct.pack('>H', i)
        if bytes([0xB6]) + b in ma or bytes([0xB8]) + b in ma or bytes([0xB7]) + b in ma:
            return True
    return False


def main():
    vao, ra = sys.argv[1], sys.argv[2]
    moi = int(sys.argv[3]) if len(sys.argv) > 3 else 100
    zin = zipfile.ZipFile(vao)
    zout = zipfile.ZipFile(ra, 'w', zipfile.ZIP_DEFLATED)
    tong = 0
    solop = 0
    for info in zin.infolist():
        b = zin.read(info.filename)
        if info.filename.endswith('.class'):
            b2, so = va(b, moi)
            if so:
                tong += so
                solop += 1
                b = b2
        zout.writestr(info, b)
    zout.close()
    print('hạ lệnh ngủ trên đường ra đòn: %d chỗ trong %d lớp, %d ms -> %d ms'
          % (tong, solop, NGU_CU, moi))
    if tong == 0:
        print('!! không tìm thấy chỗ nào -- client này khác cấu trúc, ĐỪNG dùng jar vừa tạo')


def va(b, moi):
    try:
        muc, sau = doc_pool(b)
    except Exception:
        return b, 0

    def tim_long(v):
        for i, m in enumerate(muc):
            if m and m[0] == 5 and m[1] == v:
                return i
        return -1

    def tim_sleep():
        for i, m in enumerate(muc):
            if not m or m[0] != 10:
                continue
            lop, nt = m[1]
            if muc[lop][0] != 7:
                continue
            if (muc[muc[lop][1]][1] == 'java/lang/Thread'
                    and muc[muc[nt][1][0]][1] == 'sleep'
                    and muc[muc[nt][1][1]][1] == '(J)V'):
                return i
        return -1

    i_cu, i_sleep = tim_long(NGU_CU), tim_sleep()
    if i_cu < 0 or i_sleep < 0:
        return b, 0
    refs = ref_gui_don(muc)
    if not refs:
        return b, 0

    mau = bytes([0x14]) + struct.pack('>H', i_cu) + bytes([0xB8]) + struct.pack('>H', i_sleep)
    ham = vung_ma(b, sau, muc)
    can = [(t, d, l) for (t, d, l) in ham if mau in b[d:d + l] and co_goi(b[d:d + l], refs)]
    if not can:
        return b, 0

    i_moi = tim_long(moi)
    if i_moi >= 0:
        # Đường sạch nhất: chỉ trỏ lệnh ldc2_w sang một hằng số đã có sẵn. Không đụng gì ngoài
        # 2 byte trong đúng những hàm cần sửa.
        thay = bytes([0x14]) + struct.pack('>H', i_moi) + bytes([0xB8]) + struct.pack('>H', i_sleep)
        so = 0
        for (t, d, l) in can:
            ma = b[d:d + l]
            so += ma.count(mau)
            b = b[:d] + ma.replace(mau, thay) + b[d + l:]
        return b, so

    # Không có sẵn hằng số đích trong lớp này. Chỉ sửa thẳng GIÁ TRỊ của hằng số 500 khi mọi chỗ
    # dùng nó trong lớp đều nằm trong các hàm ra đòn -- nếu không thì sửa nó là đổi luôn những
    # giấc ngủ chẳng liên quan (màn hình chờ, vòng nền).
    dung_het = sum(b[d:d + l].count(bytes([0x14]) + struct.pack('>H', i_cu)) for (t, d, l) in ham)
    dung_can = sum(b[d:d + l].count(mau) for (t, d, l) in can)
    if dung_het != dung_can:
        print('   bỏ qua một lớp: hằng số %d ms còn được dùng ở %d chỗ ngoài đường ra đòn'
              % (NGU_CU, dung_het - dung_can))
        return b, 0
    # Mục Long dài đúng 8 byte, thay tại chỗ nên kích thước lớp không đổi.
    vt = vi_tri_pool(b, i_cu)
    b = b[:vt] + struct.pack('>q', moi) + b[vt + 8:]
    return b, dung_can


def vi_tri_pool(b, can):
    """Vị trí byte của phần dữ liệu trong mục hằng số thứ `can`."""
    n = struct.unpack_from('>H', b, 8)[0]
    i, p = 1, 10
    while i < n:
        tag = b[p]
        dau = p + 1
        if tag == 1:
            p += 3 + struct.unpack_from('>H', b, p + 1)[0]
        elif tag in (7, 8, 16, 19, 20):
            p += 3
        elif tag == 15:
            p += 4
        elif tag in (3, 4):
            p += 5
        elif tag in (5, 6):
            p += 9
        else:
            p += 5
        if i == can:
            return dau
        if tag in (5, 6):
            i += 1
        i += 1
    raise ValueError('không thấy mục %d' % can)


main()
