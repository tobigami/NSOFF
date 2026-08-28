#!/usr/bin/env python3
"""Nhận log chơi game do máy bạn bè gửi về, để đọc mà không phải nhờ họ đi tìm file.

Gắn vào **IP Tailscale**, không phải 0.0.0.0: chỉ máy trong tailnet mới gửi được, và đổi mạng là
cổng tự chết theo. Cùng lý do đã ghi trong `tools/run-share.sh`.

Log lưu vào `build/log-ban-be/<tên>-<thời điểm>.txt`. Chỉ nhận POST, chỉ ghi thêm file mới, không
đọc gì trên đĩa ra -- máy này còn giữ cơ sở dữ liệu và mã nguồn, đừng mở thêm đường nào để lấy.

Chặn hai thứ: tên do máy gửi đặt bị lọc còn chữ và số (khỏi ai đó đặt tên kiểu `../../`), và mỗi
lần nhận tối đa 2 MB.
"""
import datetime
import os
import re
import sys
from http.server import BaseHTTPRequestHandler, HTTPServer

TOI_DA = 2 * 1024 * 1024
THU_MUC = 'build/log-ban-be'


class Nhan(BaseHTTPRequestHandler):
    def do_POST(self):
        try:
            n = int(self.headers.get('Content-Length', 0))
        except ValueError:
            n = 0
        if n <= 0 or n > TOI_DA:
            self.send_error(413, 'log rong hoac qua lon')
            return
        than = self.rfile.read(n)

        ten = re.sub(r'[^A-Za-z0-9_-]', '', self.path.lstrip('/'))[:40] or 'khong-ten'
        gio = datetime.datetime.now().strftime('%Y%m%d-%H%M%S')
        os.makedirs(THU_MUC, exist_ok=True)
        tep = f'{THU_MUC}/{ten}-{gio}.txt'
        with open(tep, 'wb') as f:
            f.write(than)
        print(f'[log] nhan {len(than)} byte tu {self.client_address[0]} -> {tep}', flush=True)

        self.send_response(200)
        self.send_header('Content-Type', 'text/plain; charset=utf-8')
        self.end_headers()
        self.wfile.write('da nhan log, cam on\n'.encode())

    def do_GET(self):
        self.send_response(200)
        self.send_header('Content-Type', 'text/plain; charset=utf-8')
        self.end_headers()
        self.wfile.write('cho nhan log -- gui bang POST\n'.encode())

    def log_message(self, *a):
        pass                      # tự in dòng gọn ở trên rồi, khỏi in thêm dòng mặc định


def main():
    ip, cong = sys.argv[1], int(sys.argv[2])
    print(f'nhan log tai http://{ip}:{cong}/<ten>', flush=True)
    HTTPServer((ip, cong), Nhan).serve_forever()


if __name__ == '__main__':
    main()
