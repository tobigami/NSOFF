#!/usr/bin/env python3
"""Máy chủ tệp cho thư mục chia sẻ.

Khác bản có sẵn của Python ở hai chỗ:

1. Khai báo bảng mã UTF-8. python3 -m http.server gửi tệp .txt với kiểu "text/plain" trần, không
   nói bảng mã; trình duyệt gặp vậy thì đoán, thường đoán ra latin-1, nên tiếng Việt trong
   PHIEN-BAN.txt hiện thành "phÃ¡t hÃ nh".

2. Bảng liệt kê sắp theo thời gian sửa, mới nhất lên đầu. Bản gốc sắp theo tên, mà tên các bản
   client là NSO-1.0, NSO-1.4, NSO-2.1... nên bản mới nhất nằm lẫn giữa danh sách và người tải
   rất dễ bấm nhầm bản cũ. Sắp theo thời gian thì không phải bảo trì gì thêm khi phát hành bản
   sau, và bảng còn hiện luôn ngày giờ với dung lượng để đối chiếu.
"""
import html
import http.server
import io
import json
import os
import secrets
import socketserver
import sys
import threading
import time
import urllib.parse

TEXT_UTF8 = {
    ".txt": "text/plain; charset=utf-8",
    ".md": "text/plain; charset=utf-8",
    ".html": "text/html; charset=utf-8",
}

STYLE = """body{font-family:system-ui,-apple-system,sans-serif;margin:2rem auto;max-width:46rem;
padding:0 1rem;color:#222;background:#fafafa}
h1{font-size:1.15rem;font-weight:600;margin:0 0 1rem}
table{border-collapse:collapse;width:100%}
td,th{padding:.45rem .6rem;text-align:left;border-bottom:1px solid #e2e2e2;font-size:.92rem}
th{font-weight:600;color:#666;font-size:.8rem;text-transform:uppercase;letter-spacing:.03em}
td.n{text-align:right;color:#666;white-space:nowrap}
a{color:#0b57d0;text-decoration:none}a:hover{text-decoration:underline}
tr:first-child td{font-weight:600}
@media(prefers-color-scheme:dark){body{background:#16181c;color:#e6e6e6}
td,th{border-color:#2c2f36}th,td.n{color:#9aa0a6}a{color:#8ab4f8}}"""


def kich_co(n):
    for don_vi in ("B", "KB", "MB", "GB"):
        if n < 1024 or don_vi == "GB":
            return f"{n:.0f} {don_vi}" if don_vi == "B" else f"{n:.1f} {don_vi}"
        n /= 1024.0


# ---------------------------------------------------------------- bảng góp ý
#
# Lưu ra TỆP chứ không vào MySQL: máy chủ này vốn chỉ phát tệp tĩnh, chưa hề có thông tin đăng
# nhập cơ sở dữ liệu. Nhét MySQL vào đây nghĩa là rải mật khẩu thêm một chỗ nữa, cho một bảng
# vài chục dòng. Tệp JSON thì mở ra đọc được bằng mắt, chép đi chép lại cũng dễ.
#
# Để NGOÀI thư mục phát (luutru/) để không ai tải thẳng tệp thô về được -- nội dung thì công khai,
# nhưng tên người góp ý và mã máy dùng để chống bỏ phiếu trùng thì không cần phơi ra.
GOC = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TEP_GOP_Y = os.path.join(GOC, "luutru", "gop-y.json")
TEP_KHOA = os.path.join(GOC, "luutru", "gop-y-khoa.txt")

# ThreadingTCPServer nên hai người bấm cùng lúc là hai luồng cùng ghi. Không khoá thì người sau
# đọc bản cũ, ghi đè mất góp ý của người trước.
KHOA_GHI = threading.Lock()

TRANG_THAI = ("moi", "dang-xem", "dang-lam", "xong", "khong")

DAI_TOI_DA = {"tieu_de": 120, "noi_dung": 2000, "nguoi": 40, "ghi_chu": 1000}


def khoa_quan_tri():
    """Mã của chủ server để đổi trạng thái. Sinh một lần rồi giữ nguyên trong tệp."""
    try:
        with open(TEP_KHOA, encoding="utf-8") as f:
            k = f.read().strip()
            if k:
                return k
    except OSError:
        pass
    k = secrets.token_urlsafe(12)
    os.makedirs(os.path.dirname(TEP_KHOA), exist_ok=True)
    with open(TEP_KHOA, "w", encoding="utf-8") as f:
        f.write(k)
    return k


def doc_gop_y():
    try:
        with open(TEP_GOP_Y, encoding="utf-8") as f:
            d = json.load(f)
    except (OSError, ValueError):
        d = {}
    d.setdefault("tiep", 1)
    d.setdefault("y_tuong", [])
    return d


def ghi_gop_y(d):
    """Ghi qua tệp tạm rồi đổi tên -- mất điện giữa chừng thì bản cũ vẫn nguyên vẹn."""
    os.makedirs(os.path.dirname(TEP_GOP_Y), exist_ok=True)
    tam = TEP_GOP_Y + ".tam"
    with open(tam, "w", encoding="utf-8") as f:
        json.dump(d, f, ensure_ascii=False, indent=1)
    os.replace(tam, TEP_GOP_Y)


def cat(s, khoa):
    return str(s or "").strip()[:DAI_TOI_DA[khoa]]


class Handler(http.server.SimpleHTTPRequestHandler):
    extensions_map = {**http.server.SimpleHTTPRequestHandler.extensions_map, **TEXT_UTF8}

    def tra_json(self, ma, obj):
        b = json.dumps(obj, ensure_ascii=False).encode("utf-8")
        self.send_response(ma)
        self.send_header("Content-type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(b)))
        self.end_headers()
        self.wfile.write(b)

    def than_json(self):
        n = int(self.headers.get("Content-Length") or 0)
        if n <= 0 or n > 64 * 1024:
            return {}
        try:
            return json.loads(self.rfile.read(n).decode("utf-8"))
        except ValueError:
            return {}

    def do_GET(self):
        if self.path.split("?")[0] == "/api/gop-y":
            d = doc_gop_y()
            # Không trả mã máy của người bỏ phiếu ra ngoài -- chỉ cần biết SỐ phiếu và mình đã
            # bỏ chưa, không cần biết ai bỏ.
            t = urllib.parse.parse_qs(urllib.parse.urlparse(self.path).query)
            toi = (t.get("ai") or [""])[0]
            ra = []
            for y in d["y_tuong"]:
                ra.append({**{k: v for k, v in y.items() if k != "phieu"},
                           "so_phieu": len(y.get("phieu", [])),
                           "da_bo_phieu": toi in y.get("phieu", [])})
            self.tra_json(200, {"ok": True, "y_tuong": ra})
            return
        super().do_GET()

    def do_POST(self):
        duong = self.path.split("?")[0]
        if not duong.startswith("/api/gop-y"):
            self.send_error(404, "Không có đường này")
            return
        req = self.than_json()
        try:
            with KHOA_GHI:
                d = doc_gop_y()
                if duong == "/api/gop-y":
                    tieu_de = cat(req.get("tieu_de"), "tieu_de")
                    if not tieu_de:
                        self.tra_json(400, {"ok": False, "loi": "Thiếu tiêu đề."})
                        return
                    y = {"id": d["tiep"], "tieu_de": tieu_de,
                         "noi_dung": cat(req.get("noi_dung"), "noi_dung"),
                         "nguoi": cat(req.get("nguoi"), "nguoi") or "ẩn danh",
                         "luc": time.strftime("%Y-%m-%d %H:%M"),
                         "trang_thai": "moi", "phieu": [], "ghi_chu": []}
                    d["tiep"] += 1
                    d["y_tuong"].insert(0, y)
                elif duong == "/api/gop-y/phieu":
                    y = self.tim(d, req.get("id"))
                    if y is None:
                        self.tra_json(404, {"ok": False, "loi": "Không có ý tưởng này."})
                        return
                    ai = cat(req.get("ai"), "nguoi")
                    ps = y.setdefault("phieu", [])
                    if ai in ps:
                        ps.remove(ai)
                    elif ai:
                        ps.append(ai)
                elif duong == "/api/gop-y/ghi-chu":
                    y = self.tim(d, req.get("id"))
                    if y is None:
                        self.tra_json(404, {"ok": False, "loi": "Không có ý tưởng này."})
                        return
                    nd = cat(req.get("noi_dung"), "ghi_chu")
                    if not nd:
                        self.tra_json(400, {"ok": False, "loi": "Ghi chú rỗng."})
                        return
                    y.setdefault("ghi_chu", []).append(
                        {"nguoi": cat(req.get("nguoi"), "nguoi") or "ẩn danh",
                         "noi_dung": nd, "luc": time.strftime("%Y-%m-%d %H:%M")})
                elif duong == "/api/gop-y/trang-thai":
                    if cat(req.get("khoa"), "nguoi") != khoa_quan_tri():
                        self.tra_json(403, {"ok": False, "loi": "Sai mã quản trị."})
                        return
                    y = self.tim(d, req.get("id"))
                    tt = str(req.get("trang_thai") or "")
                    if y is None or tt not in TRANG_THAI:
                        self.tra_json(400, {"ok": False, "loi": "Dữ liệu không hợp lệ."})
                        return
                    y["trang_thai"] = tt
                else:
                    self.send_error(404, "Không có đường này")
                    return
                ghi_gop_y(d)
            self.tra_json(200, {"ok": True})
        except Exception as loi:
            self.tra_json(500, {"ok": False, "loi": str(loi)})

    @staticmethod
    def tim(d, ma):
        try:
            ma = int(ma)
        except (TypeError, ValueError):
            return None
        for y in d["y_tuong"]:
            if y.get("id") == ma:
                return y
        return None

    def list_directory(self, path):
        try:
            ten = os.listdir(path)
        except OSError:
            self.send_error(404, "Không đọc được thư mục")
            return None

        muc = []
        for t in ten:
            if t.startswith("."):
                continue
            day_du = os.path.join(path, t)
            try:
                st = os.stat(day_du)
            except OSError:
                continue
            muc.append((st.st_mtime, t, os.path.isdir(day_du), st.st_size))
        muc.sort(reverse=True)          # mới nhất lên đầu

        hien = urllib.parse.unquote(self.path, errors="surrogatepass")
        ra = io.StringIO()
        ra.write("<!DOCTYPE html><html lang='vi'><head><meta charset='utf-8'>")
        ra.write("<meta name='viewport' content='width=device-width,initial-scale=1'>")
        ra.write(f"<title>{html.escape(hien)}</title><style>{STYLE}</style></head><body>")
        ra.write(f"<h1>{html.escape(hien)}</h1>")
        ra.write("<table><tr><th>Tên</th><th class='n'>Sửa lúc</th><th class='n'>Dung lượng</th></tr>")
        if hien.rstrip("/"):
            ra.write("<tr><td><a href='..'>.. (lên một cấp)</a></td><td class='n'></td>"
                     "<td class='n'></td></tr>")
        for mtime, t, la_thu_muc, co in muc:
            lien_ket = urllib.parse.quote(t, errors="surrogatepass") + ("/" if la_thu_muc else "")
            nhan = html.escape(t) + ("/" if la_thu_muc else "")
            luc = time.strftime("%d/%m/%Y %H:%M", time.localtime(mtime))
            ra.write(f"<tr><td><a href='{lien_ket}'>{nhan}</a></td>"
                     f"<td class='n'>{luc}</td>"
                     f"<td class='n'>{'' if la_thu_muc else kich_co(co)}</td></tr>")
        ra.write("</table></body></html>")

        du_lieu = ra.getvalue().encode("utf-8", "surrogateescape")
        self.send_response(200)
        self.send_header("Content-type", "text/html; charset=utf-8")
        self.send_header("Content-Length", str(len(du_lieu)))
        self.end_headers()
        return io.BytesIO(du_lieu)


def main():
    ip, port, directory = sys.argv[1], int(sys.argv[2]), sys.argv[3]
    handler = lambda *a, **kw: Handler(*a, directory=directory, **kw)
    # allow_reuse_address để bật lại ngay sau khi tắt, khỏi vướng cổng còn ở trạng thái TIME_WAIT.
    socketserver.TCPServer.allow_reuse_address = True
    with socketserver.ThreadingTCPServer((ip, port), handler) as httpd:
        print(f"chia tài nguyên tại http://{ip}:{port}/", flush=True)
        print(f"bảng góp ý: http://{ip}:{port}/gop-y/  (mã quản trị: {khoa_quan_tri()})", flush=True)
        httpd.serve_forever()


if __name__ == "__main__":
    main()
