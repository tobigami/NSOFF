#!/usr/bin/env python3
"""Dựng dữ liệu cho cơ chế phá trần kỹ năng: nối cấp cho một chiêu và tạo quyển sách đi kèm.

CẨN THẬN: `skill_template` có cột JSON `skillTemplates` trông y như nơi chứa dữ liệu từng cấp,
nhưng nó ĐÃ CHẾT -- đoạn nạp nó trong GameData bị chú thích hết, và số liệu bên trong đã lệch thực
tế (chiêu 86: cột JSON ghi max_fight 6, bảng thật ghi 7). Nguồn thật là BẢNG `skill`, nạp bằng
LOAD_SKILL = "SELECT * FROM skill WHERE template_id = ? ORDER BY point ASC". Ghi nhầm vào cột JSON
thì máy chủ vẫn nhận max_point mới nên client hiện "1/6", nhưng bấm nâng cấp sẽ báo "Không có bản
cấp 2 của kỹ năng này".

Ràng buộc của giao thức phải giữ (xem Server.setSkill):

    writeByte(maxPoint)        trần <= 127
    writeByte(skills.size())   số cấp mỗi chiêu <= 127
    writeByte(skill.level)     cấp nhân vật yêu cầu <= 127  <-- dễ tràn nhất
    writeShort(skill.id)       id <= 32767
    writeShort(option.param)   giá trị mỗi chỉ số <= 32767  <-- dễ tràn thứ nhì

`max_point` đặt bằng TRẦN CAO NHẤT chứ không phải trần gốc. Nó là một con số dùng chung cho cả
máy chủ, không thể khác nhau theo từng người; để bằng trần gốc thì lúc ai đó phá trần lên cấp 7,
cấp hiện tại vượt cấp tối đa và KHUNG THÔNG TIN TRONG GAME VỠ (cụt, mất nút Cộng/Gắn/Đóng). Trần
thật của từng người do máy chủ giữ, xem PhaTran.java.

Mô tả chiêu phải NGẮN: khung thông tin trong game chỉ vừa 9 dòng, mô tả 97 ký tự chiếm 3 dòng là
đủ đẩy phần dưới ra ngoài khung.

Thời gian chờ:
    cấp 1..TRAN_GOC   bớt đều CHO_BUOC ms mỗi cấp -- nhịp tuyến tính của chiêu gốc
                      (chiêu 83: 500,490,480,...; chiêu 18: 850,800,750,...)
    cấp trên TRAN_GOC GIỮ NGUYÊN mức của cấp TRAN_GOC

Phá trần chỉ tăng sát thương, KHÔNG rút ngắn thời gian chờ. Lý do không phải cân bằng suông mà là
đo được: hạ thời gian chờ xuống 0,1 giây thì client không theo kịp, nó vẫn chỉ ra đòn mỗi ~0,33
giây, nên phần chờ cắt đi phần lớn rơi vào hư không -- người chơi trả một quyển sách để lấy một
con số không hiện ra trong trận. Sát thương thì cộng bao nhiêu hiện đủ bấy nhiêu.

MP nhích nhẹ mỗi cấp, theo đúng quy ước của game (chiêu 83: MP 177->210 qua 12 cấp).

Dùng: python3 tools/pha-tran-ky-nang.py [--ghi]
"""
import json
import subprocess
import sys

def doc_db():
    """Tên CSDL lấy từ cấu hình của CHÍNH cây nguồn này.

    Ghi cứng ở đây là cái bẫy: hai cây prod/dev dùng chung mã, khác mỗi cấu hình -- gõ lệnh ở cây
    dev mà công cụ lại sửa CSDL của người đang chơi thì không có gì trên màn hình báo cho biết.
    """
    import os
    goc = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
    with open(os.path.join(goc, 'work/server/NSO_KEM/mysql.properties'), encoding='utf-8') as f:
        for d in f:
            if d.startswith('nsoz.database.name='):
                return d.split('=', 1)[1].strip()
    raise SystemExit('không đọc được tên CSDL trong mysql.properties')


DB = doc_db()

MA_CHIEU = 86          # Tiêu Hoả Phi Long, phái phi tiêu
MO_TA = 'Hỏa thuật Lam Hỏa Phi Long'
TRAN_GOC = 6           # lên được bằng điểm kỹ năng
PHA_TRAN = 5           # số lần phá trần -> trần cao nhất 11

BUOC = 0.18            # mỗi cấp mạnh thêm 18% so với cấp 1
MP_BUOC = 20           # mỗi cấp tốn thêm bấy nhiêu MP
CHO_BUOC = 10          # cấp thường: bớt bấy nhiêu ms mỗi cấp (cấp phá trần không bớt nữa)
TRAN_SHORT = 32767

ITEM_MOI = 1265
ITEM_MAU = 1126        # chép icon và dáng từ quyển sách gốc


def q(sql):
    return subprocess.run(['mysql', '-uroot', DB, '-N', '--raw', '-e', sql],
                          capture_output=True, text=True).stdout


def chay(sql):
    r = subprocess.run(['mysql', '-uroot', DB, '-e', sql], capture_output=True, text=True)
    if r.returncode:
        raise SystemExit('sql hỏng: ' + r.stderr.strip())


def main():
    ghi = '--ghi' in sys.argv
    cuoi = TRAN_GOC + PHA_TRAN

    ten = q(f"SELECT name FROM skill_template WHERE id={MA_CHIEU};").strip()
    dong = [l.split('\t') for l in q(
        "SELECT id,max_fight,level,mana_use,cooldown,point,dx,dy,options FROM `skill` "
        f"WHERE template_id={MA_CHIEU} ORDER BY point;").strip().split('\n')]
    co = {int(d[5]): d for d in dong}
    if 1 not in co:
        raise SystemExit('không thấy cấp 1 trong bảng skill')
    if max(co) >= 2:
        raise SystemExit(f'{ten} đã có tới cấp {max(co)} -- dọn trước rồi chạy lại')

    g = co[1]
    ops = json.loads(g[8])
    ke = int(q("SELECT MAX(id) FROM `skill`;").strip()) + 1
    choGoc = int(g[4])

    def gioCho(n):
        # Đoạn cấp thường giữ nhịp tuyến tính của chiêu gốc; đoạn phá trần GIỮ NGUYÊN thời gian
        # chờ ở mức của trần gốc. Phá trần chỉ để mạnh thêm, không để bắn dày thêm -- xem chú
        # thích đầu tệp.
        return choGoc - CHO_BUOC * (min(n, TRAN_GOC) - 1)

    them = []
    for n in range(2, cuoi + 1):
        he = 1 + BUOC * (n - 1)
        o = [{'param': int(round(x['param'] * he)), 'id': x['id']} for x in ops]
        for x in o:
            if x['param'] > TRAN_SHORT:
                raise SystemExit(f"cấp {n} chỉ số {x['id']} vượt {TRAN_SHORT}, sẽ tràn short")
        if int(g[2]) > 127:
            raise SystemExit('cấp nhân vật yêu cầu vượt 127, sẽ tràn byte')
        them.append({'id': ke + len(them), 'point': n, 'max_fight': int(g[1]), 'level': int(g[2]),
                     'mana_use': int(g[3]) + MP_BUOC * (n - 1), 'cooldown': gioCho(n),
                     'dx': int(g[6]), 'dy': int(g[7]), 'options': o})

    print(f"  {ten}: nối cấp 2..{cuoi} vào BẢNG skill, id {ke}..{ke + len(them) - 1}")
    print(f"     cấp  1  chờ {choGoc:3d} ms  MP {g[3]}")
    for m in them:
        dau = ' <- phá trần' if m['point'] > TRAN_GOC else ''
        print(f"     cấp {m['point']:2d}  chờ {m['cooldown']:3d} ms  MP {m['mana_use']}"
              f"  chỉ số {[x['param'] for x in m['options']]}{dau}")
    print(f"  max_point -> {cuoi} (trần cao nhất; trần gốc {TRAN_GOC} do PhaTran.java giữ)")

    if not ghi:
        print('\n  (xem thử -- chạy lại với --ghi để ghi)')
        return

    for m in them:
        o = json.dumps(m['options'], separators=(',', ':')).replace("'", "''")
        chay("INSERT INTO `skill` (id,template_id,max_fight,level,mana_use,cooldown,point,dx,dy,options) "
             f"VALUES ({m['id']},{MA_CHIEU},{m['max_fight']},{m['level']},{m['mana_use']},"
             f"{m['cooldown']},{m['point']},{m['dx']},{m['dy']},'{o}');")
    chay(f"UPDATE skill_template SET max_point={cuoi}, "
         f"description='{MO_TA}' WHERE id={MA_CHIEU};")

    if q(f"SELECT COUNT(*) FROM item WHERE id={ITEM_MOI};").strip() == '0':
        chay("INSERT INTO item (id,name,type,gender,description,level,icon,part,fashion,isUpToUp) "
             f"SELECT {ITEM_MOI}, CONCAT(name,' limit-break'), type, gender, "
             f"'Phá trần chiêu vượt cấp {TRAN_GOC}, mỗi quyển thêm 1 cấp. Tối đa {PHA_TRAN} lần.', "
             f"level, icon, part, fashion, isUpToUp FROM item WHERE id={ITEM_MAU};")
        print(f'  đã tạo vật phẩm {ITEM_MOI}')
    print('  ĐÃ GHI -- nhớ bump game.skill.version và game.item.version rồi khởi động lại')


if __name__ == '__main__':
    main()
