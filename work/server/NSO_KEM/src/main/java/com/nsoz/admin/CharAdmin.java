package com.nsoz.admin;

import com.nsoz.db.jdbc.DbManager;
import com.nsoz.item.Equip;
import com.nsoz.item.Item;
import com.nsoz.item.ItemManager;
import com.nsoz.item.ItemFactory;
import com.nsoz.item.ItemTemplate;
import com.nsoz.item.TinhLuyen;
import com.nsoz.map.Map;
import com.nsoz.map.MapManager;
import com.nsoz.model.Char;
import com.nsoz.task.Task;
import com.nsoz.task.TaskTemplate;
import com.nsoz.option.ItemOption;
import com.nsoz.server.GameData;
import com.nsoz.server.GiveItem;
import com.nsoz.server.ServerManager;
import com.nsoz.option.SkillOption;
import com.nsoz.skill.PhaTran;
import com.nsoz.skill.Skill;
import com.nsoz.skill.SkillTemplate;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;
import com.nsoz.util.NinjaUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * Xem và sửa nhân vật, kể cả khi người đó không online.
 *
 * Cái bẫy của việc sửa thẳng cơ sở dữ liệu: nếu nhân vật đang chơi thì máy chủ giữ một bản trong
 * bộ nhớ, và {@link Char#saveData()} sẽ ghi đè cả hàng lúc người đó thoát game hoặc lúc tắt máy
 * chủ -- mọi thay đổi ghi thẳng vào bảng trong lúc đó đều mất trắng. Nên lớp này luôn rẽ hai
 * nhánh:
 *
 * <ul>
 *   <li>Nhân vật <b>offline</b>: đọc và ghi thẳng bảng {@code players}, mỗi thao tác chỉ đụng
 *       đúng những cột nó cần đổi.</li>
 *   <li>Nhân vật <b>online</b>: sửa đối tượng {@link Char} trong bộ nhớ rồi gọi
 *       {@code saveData()}, hệt như chính người chơi vừa cộng điểm trong game.</li>
 * </ul>
 *
 * Nhánh "online" chỉ chạy được khi lớp này sống trong chính tiến trình máy chủ. Chạy độc lập
 * (xem {@link CharAdminHttp#main}) thì máy chủ nằm ở tiến trình khác, không với tới đối tượng
 * trong bộ nhớ của nó được -- lúc đó mọi thao tác ghi lên người đang online đều bị từ chối chứ
 * không âm thầm ghi vào bảng rồi mất.
 *
 * <p>Cách nhận biết online: cột {@code players.online}. Cột đó được đặt 1 lúc đăng nhập
 * ({@code User.java}), đặt 0 lúc đăng xuất, và bị dọn về 0 cho cả máy chủ lúc khởi động
 * ({@code Server.java}) lẫn trong {@code tools/run-server.sh}. Khi lớp này chạy trong máy chủ thì
 * còn đối chiếu thêm {@code ServerManager.getChars()} -- đó mới là nguồn thật, cột kia chỉ là
 * bản sao có thể lệch nếu máy chủ chết bất thường.
 */
public final class CharAdmin {

    /** Tên bốn ô tiềm năng, kèm chỗ nó thật sự tác động (xem AbilityFromEquip). */
    public static final String[] POTENTIAL_NAME = {
            "Sức mạnh", "Chính xác", "Thể lực", "Tinh thần"
    };
    public static final String[] POTENTIAL_NOTE = {
            "sát thương hệ ngoại", "chính xác và né đòn", "HP tối đa (x10)", "MP tối đa (x10) và sát thương hệ nội"
    };

    /** Mười sáu ô trang bị, đánh số đúng theo ItemTemplate.TYPE_*. */
    public static final String[] SLOT_NAME = {
            "Nón", "Vũ khí", "Áo", "Dây chuyền", "Găng tay", "Nhẫn", "Quần", "Ngọc bội",
            "Giày", "Bùa", "Thú nuôi", "Mặt nạ", "Áo choàng", "Bao tay", "Mắt thần", "Bí kíp"
    };

    /** Kỹ năng phân thân: máy chủ cấm nâng và cấm tẩy, chép y nguyên giới hạn đó. */
    private static final int CLONE_SKILL_MIN = 67;
    private static final int CLONE_SKILL_MAX = 72;

    /** true khi lớp này đang sống trong tiến trình máy chủ game, tức là với tới được bộ nhớ. */
    private static volatile boolean inServer = false;

    private CharAdmin() {
    }

    /** Máy chủ gọi lúc khởi động để bật nhánh sửa trong bộ nhớ. */
    public static void markInServer() {
        inServer = true;
    }

    public static boolean isInServer() {
        return inServer;
    }

    // ------------------------------------------------------------------ đọc

    /** Danh sách nhân vật để chọn. Cờ online lấy từ cả hai nguồn, nguồn nào nói có là có. */
    public static JSONArray list() throws Exception {
        JSONArray out = new JSONArray();
        try (Connection conn = DbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT `id`, `name`, `class`, `gender`, `online`, `point`, `spoint`,"
                             + " CAST(JSON_EXTRACT(`data`, '$.level') AS UNSIGNED) AS `level`"
                             + " FROM `players` WHERE `deleted_at` IS NULL ORDER BY `id`;")) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    JSONObject o = new JSONObject();
                    o.put("id", id);
                    o.put("name", rs.getString("name"));
                    o.put("classId", rs.getInt("class"));
                    o.put("className", className(rs.getInt("class")));
                    o.put("gender", rs.getInt("gender"));
                    o.put("level", rs.getInt("level"));
                    o.put("point", rs.getInt("point"));
                    o.put("spoint", rs.getInt("spoint"));
                    o.put("online", isOnline(id, rs.getInt("online")));
                    out.add(o);
                }
            }
        }
        return out;
    }

    /**
     * Toàn bộ trạng thái của một nhân vật.
     *
     * Nhân vật online thì đọc từ bộ nhớ, vì bảng lúc đó là ảnh cũ từ lần lưu gần nhất; offline thì
     * đọc bảng, vì bảng lúc đó chính là sự thật duy nhất.
     */
    public static JSONObject view(int id) throws Exception {
        // Đọc thì không chặn: nhân vật đang online mà bảng điều khiển chạy ngoài máy chủ thì vẫn
        // xem được ảnh từ lần lưu gần nhất, chỉ ghi mới nguy hiểm.
        Char c = boNho(id);
        return c != null ? viewLive(c) : viewDb(id);
    }

    private static JSONObject viewDb(int id) throws Exception {
        try (Connection conn = DbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT * FROM `players` WHERE `id` = ? LIMIT 1;")) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("Không có nhân vật id " + id);
                }
                JSONObject data = parseObject(rs.getString("data"));
                int classId = rs.getInt("class");
                int level = num(data.get("level"));

                JSONObject o = new JSONObject();
                o.put("id", id);
                o.put("name", rs.getString("name"));
                o.put("nguon", "csdl");
                o.put("task", moTaNhiemVuDb(rs.getInt("taskId"), rs.getString("task")));
                o.put("phanThan", xemPhanThan(id, null));
                o.put("online", isOnline(id, rs.getInt("online")));
                o.put("classId", classId);
                o.put("className", className(classId));
                o.put("gender", rs.getInt("gender"));
                o.put("level", level);
                o.put("exp", data.get("exp") == null ? 0L : longOf(data.get("exp")));
                o.put("xu", rs.getLong("xu"));
                o.put("xuInBox", rs.getLong("xuInBox"));
                o.put("yen", rs.getLong("yen"));
                o.put("luong", rs.getInt("luong"));
                o.put("point", rs.getInt("point"));
                o.put("spoint", rs.getInt("spoint"));
                o.put("tayTiemNang", num(data.get("tayTiemNang")));
                o.put("tayKyNang", num(data.get("tayKyNang")));

                int[] pot = readPotential(rs.getString("potential"));
                o.put("potential", intArray(pot));
                o.put("potentialTong", sum(pot) + rs.getInt("point"));
                o.put("potentialTran", potentialPool(classId, level,
                        num(data.get("limitTiemNangSo")), num(data.get("limitBangHoa"))));
                o.put("spointTran", skillPool(classId, level,
                        num(data.get("limitKyNangSo")), num(data.get("limitPhongLoi"))));

                o.put("equip", gearJson(parseArray(rs.getString("equiped"))));
                o.put("fashion", gearJson(parseArray(rs.getString("fashion"))));
                o.put("bag", itemsJson(parseArray(rs.getString("bag"))));
                o.put("box", itemsJson(parseArray(rs.getString("box"))));
                o.put("numberCellBag", rs.getInt("numberCellBag"));
                o.put("numberCellBox", rs.getInt("numberCellBox"));
                o.put("skill", skillJsonDb(classId, parseArray(rs.getString("skill")),
                        data.get("phaTran") == null ? "" : String.valueOf(data.get("phaTran"))));
                return o;
            }
        }
    }

    private static JSONObject viewLive(Char c) {
        JSONObject o = new JSONObject();
        o.put("id", c.id);
        o.put("name", c.name);
        o.put("nguon", "bộ nhớ máy chủ");
        o.put("task", moTaNhiemVu(c));
        o.put("chiSo", chiSo(c));
        try {
            o.put("phanThan", xemPhanThan(c.id, c));
        o.put("tinhLuyen", tinhLuyenJson(c));
        JSONObject vt = new JSONObject();
        vt.put("map", (int) c.mapId);
        vt.put("x", (int) c.x);
        vt.put("y", (int) c.y);
        vt.put("diemLuu", (int) c.saveCoordinate);
        o.put("viTri", vt);
        } catch (Exception ignore) {
        }
        o.put("online", true);
        o.put("classId", (int) c.classId);
        o.put("className", className(c.classId));
        o.put("gender", (int) c.gender);
        o.put("level", c.level);
        o.put("exp", c.exp);
        o.put("xu", c.coin);
        o.put("xuInBox", c.coinInBox);
        o.put("yen", c.yen);
        o.put("luong", c.user != null ? c.user.gold : 0);
        o.put("point", (int) c.potentialPoint);
        o.put("spoint", (int) c.skillPoint);
        o.put("tayTiemNang", c.tayTiemNang);
        o.put("tayKyNang", c.tayKyNang);
        o.put("potential", intArray(c.potential));
        o.put("potentialTong", sum(c.potential) + c.potentialPoint);
        o.put("potentialTran", potentialPool(c.classId, c.level, c.limitTiemNangSo, c.limitBangHoa));
        o.put("spointTran", skillPool(c.classId, c.level, c.limitKyNangSo, c.limitPhongLoi));

        JSONArray equip = new JSONArray();
        for (int i = 0; i < SLOT_NAME.length; i++) {
            Item it = c.equipment == null ? null : c.equipment[i];
            if (it != null) {
                equip.add(itemJson(it, i));
            }
        }
        o.put("equip", equip);
        JSONArray fashion = new JSONArray();
        for (int i = 0; i < SLOT_NAME.length; i++) {
            Item it = c.fashion == null ? null : c.fashion[i];
            if (it != null) {
                fashion.add(itemJson(it, i));
            }
        }
        o.put("fashion", fashion);
        o.put("bag", liveItems(c.bag, c.numberCellBag));
        o.put("box", liveItems(c.box, c.numberCellBox));
        o.put("numberCellBag", (int) c.numberCellBag);
        o.put("numberCellBox", (int) c.numberCellBox);

        JSONArray skills = new JSONArray();
        if (c.vSkill != null) {
            for (Skill s : c.vSkill) {
                if (s == null || s.template == null) {
                    continue;
                }
                skills.add(skillJson(s.template.id, s.template.name, s.point, s.template.maxPoint,
                        s.level, s.template.description, s.template.iconId, s, c.phaTran));
            }
        }
        o.put("skill", skills);
        return o;
    }

    // ------------------------------------------------------------- thao tác

    /**
     * Cộng (hoặc trừ, khi delta âm) điểm tiềm năng chưa dùng.
     */
    public static JSONObject addPotentialPoint(int id, int delta) throws Exception {
        Char c = live(id);
        if (c != null) {
            c.potentialPoint = (short) Math.max(0, c.potentialPoint + delta);
            luuOnline(c, true, false);
            return ok(id, "Điểm tiềm năng chưa dùng: " + c.potentialPoint);
        }
        int now = Math.max(0, intCol(id, "point") + delta);
        set(id, "`point` = ?", now);
        return ok(id, "Điểm tiềm năng chưa dùng: " + now);
    }

    /** Cộng (hoặc trừ) điểm kỹ năng chưa dùng. */
    public static JSONObject addSkillPoint(int id, int delta) throws Exception {
        Char c = live(id);
        if (c != null) {
            c.skillPoint = (short) Math.max(0, c.skillPoint + delta);
            luuOnline(c, false, true);
            return ok(id, "Điểm kỹ năng chưa dùng: " + c.skillPoint);
        }
        int now = Math.max(0, intCol(id, "spoint") + delta);
        set(id, "`spoint` = ?", now);
        return ok(id, "Điểm kỹ năng chưa dùng: " + now);
    }

    /**
     * Dồn điểm tiềm năng vào một ô, trừ đúng số điểm đó khỏi kho chưa dùng.
     *
     * Đây là bản không kiểm tra của {@code Char.upPotential}: cho phép cả điểm âm để rút bớt ra
     * (trả lại vào kho), thứ mà trong game không làm được.
     */
    public static JSONObject spendPotential(int id, int index, int point) throws Exception {
        if (index < 0 || index >= 4) {
            throw new IllegalArgumentException("Ô tiềm năng phải từ 0 đến 3.");
        }
        Char c = live(id);
        if (c != null) {
            if (point > c.potentialPoint) {
                throw new IllegalArgumentException("Chỉ còn " + c.potentialPoint + " điểm chưa dùng.");
            }
            if (c.potential[index] + point < 0) {
                throw new IllegalArgumentException("Ô " + POTENTIAL_NAME[index] + " không đủ để rút ra.");
            }
            c.potential[index] += point;
            c.potentialPoint -= point;
            capNhatChiSo(c);
            luuOnline(c, true, false);
            return ok(id, POTENTIAL_NAME[index] + " = " + c.potential[index]
                    + ", còn " + c.potentialPoint + " điểm chưa dùng");
        }
        try (Connection conn = DbManager.getConnection()) {
            int[] pot;
            int free;
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT `potential`, `point` FROM `players` WHERE `id` = ? LIMIT 1;")) {
                stmt.setInt(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalArgumentException("Không có nhân vật id " + id);
                    }
                    pot = readPotential(rs.getString("potential"));
                    free = rs.getInt("point");
                }
            }
            if (point > free) {
                throw new IllegalArgumentException("Chỉ còn " + free + " điểm chưa dùng.");
            }
            if (pot[index] + point < 0) {
                throw new IllegalArgumentException("Ô " + POTENTIAL_NAME[index] + " không đủ để rút ra.");
            }
            pot[index] += point;
            free -= point;
            try (PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE `players` SET `potential` = ?, `point` = ? WHERE `id` = ? LIMIT 1;")) {
                stmt.setString(1, intArray(pot).toJSONString());
                stmt.setInt(2, free);
                stmt.setInt(3, id);
                stmt.executeUpdate();
            }
            return ok(id, POTENTIAL_NAME[index] + " = " + pot[index] + ", còn " + free + " điểm chưa dùng");
        }
    }

    /** Đặt thẳng cả bốn ô tiềm năng, kho điểm chưa dùng được tính lại cho khớp trần. */
    public static JSONObject setPotential(int id, int[] value) throws Exception {
        if (value == null || value.length != 4) {
            throw new IllegalArgumentException("Phải đủ bốn ô tiềm năng.");
        }
        for (int v : value) {
            if (v < 0) {
                throw new IllegalArgumentException("Điểm tiềm năng không được âm.");
            }
        }
        Char c = live(id);
        if (c != null) {
            int pool = potentialPool(c.classId, c.level, c.limitTiemNangSo, c.limitBangHoa);
            System.arraycopy(value, 0, c.potential, 0, 4);
            c.potentialPoint = (short) Math.max(0, pool - sum(value));
            capNhatChiSo(c);
            luuOnline(c, true, false);
            return ok(id, "Đã đặt tiềm năng, còn " + c.potentialPoint + " điểm chưa dùng");
        }
        JSONObject data = dataOf(id);
        int classId = intCol(id, "class");
        int pool = potentialPool(classId, num(data.get("level")),
                num(data.get("limitTiemNangSo")), num(data.get("limitBangHoa")));
        int free = Math.max(0, pool - sum(value));
        try (Connection conn = DbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE `players` SET `potential` = ?, `point` = ? WHERE `id` = ? LIMIT 1;")) {
            stmt.setString(1, intArray(value).toJSONString());
            stmt.setInt(2, free);
            stmt.setInt(3, id);
            stmt.executeUpdate();
        }
        return ok(id, "Đã đặt tiềm năng, còn " + free + " điểm chưa dùng");
    }

    /**
     * Tẩy điểm tiềm năng: đưa bốn ô về mức khởi đầu của phái và trả lại toàn bộ kho điểm.
     *
     * Chép đúng phép tính của {@code Char.tayTiemNang} nhưng không trừ lượt tẩy -- đây là thao tác
     * của quản trị, không phải người chơi mua lượt ở NPC.
     */
    public static JSONObject resetPotential(int id) throws Exception {
        Char c = live(id);
        if (c != null) {
            int pool = potentialPool(c.classId, c.level, c.limitTiemNangSo, c.limitBangHoa);
            int[] base = basePotential(c.classId);
            System.arraycopy(base, 0, c.potential, 0, 4);
            c.potentialPoint = (short) pool;
            capNhatChiSo(c);
            luuOnline(c, true, false);
            return ok(id, "Đã tẩy tiềm năng, có " + pool + " điểm để chia lại");
        }
        JSONObject data = dataOf(id);
        int classId = intCol(id, "class");
        int pool = potentialPool(classId, num(data.get("level")),
                num(data.get("limitTiemNangSo")), num(data.get("limitBangHoa")));
        int[] base = basePotential(classId);
        try (Connection conn = DbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE `players` SET `potential` = ?, `point` = ? WHERE `id` = ? LIMIT 1;")) {
            stmt.setString(1, intArray(base).toJSONString());
            stmt.setInt(2, pool);
            stmt.setInt(3, id);
            stmt.executeUpdate();
        }
        return ok(id, "Đã tẩy tiềm năng, có " + pool + " điểm để chia lại");
    }

    /**
     * Nâng một kỹ năng thêm {@code delta} cấp, trừ đúng số đó khỏi kho điểm kỹ năng.
     *
     * delta âm là hạ cấp và hoàn điểm -- trong game không có, nhưng quản trị cần để sửa nhầm.
     * Cấp thấp nhất là 1: máy chủ dựng kỹ năng theo cặp (mã, cấp) và không có bản cấp 0 cho kỹ
     * năng đã học, nên tụt xuống 0 là nạp lại lỗi.
     */
    public static JSONObject upSkill(int id, int templateId, int delta) throws Exception {
        // Kỹ năng phân thân trước đây bị chặn ở đây. Kiểm lại thì bảng `skill` có đủ 11 bản
        // (cấp 0 tới 10) cho cả sáu mã 67-72, y hệt kỹ năng thường, nên nâng được bình thường.
        // Mỗi phái một mã riêng, và phép tra theo (phái, mã, cấp) bên dưới tự loại mã sai phái.
        Char c = live(id);
        if (c != null) {
            if (c.vSkill == null) {
                throw new IllegalStateException("Nhân vật này chưa nạp xong kỹ năng.");
            }
            Skill cu = null;
            int viTri = -1;
            for (int i = 0; i < c.vSkill.size(); i++) {
                Skill s = c.vSkill.get(i);
                if (s != null && s.template != null && s.template.id == templateId) {
                    cu = s;
                    viTri = i;
                    break;
                }
            }
            if (cu == null) {
                throw new IllegalArgumentException("Nhân vật chưa có kỹ năng mã " + templateId + ".");
            }
            int moi = cu.point + delta;
            kiemTraCapKyNang(c.classId, templateId,
                    PhaTran.tranThat(templateId, cu.template.maxPoint, c.phaTran), moi);
            if (delta > c.skillPoint) {
                throw new IllegalArgumentException("Chỉ còn " + c.skillPoint + " điểm kỹ năng.");
            }
            Skill thay = GameData.getInstance().getSkill(c.classId, templateId, moi);
            if (thay == null) {
                throw new IllegalArgumentException("Không có bản cấp " + moi + " của kỹ năng này.");
            }
            doiKyNang(c, viTri, cu, thay);
            c.skillPoint = (short) Math.max(0, c.skillPoint - delta);
            capNhatChiSo(c);
            try {
                c.getService().loadSkill();
            } catch (Throwable ignore) {
                // Không có phiên hiển thị thì thôi, dữ liệu đã đúng rồi.
            }
            luuOnline(c, false, true);
            return ok(id, cu.template.name + " cấp " + moi + ", còn " + c.skillPoint + " điểm kỹ năng");
        }

        int classId = intCol(id, "class");
        JSONArray skills = parseArray(strCol(id, "skill"));
        JSONObject muc = null;
        for (Object o : skills) {
            JSONObject s = (JSONObject) o;
            if (num(s.get("id")) == templateId) {
                muc = s;
                break;
            }
        }
        if (muc == null) {
            throw new IllegalArgumentException("Nhân vật chưa có kỹ năng mã " + templateId + ".");
        }
        int moi = num(muc.get("point")) + delta;
        SkillTemplate t = template(classId, templateId);
        kiemTraCapKyNang(classId, templateId,
                PhaTran.tranThat(templateId, t == null ? 0 : t.maxPoint, phaTranCsdl(id)), moi);
        if (GameData.getInstance().getSkill(classId, templateId, moi) == null) {
            throw new IllegalArgumentException("Không có bản cấp " + moi + " của kỹ năng này.");
        }
        int free = intCol(id, "spoint");
        if (delta > free) {
            throw new IllegalArgumentException("Chỉ còn " + free + " điểm kỹ năng.");
        }
        muc.put("point", moi);
        free = Math.max(0, free - delta);
        try (Connection conn = DbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE `players` SET `skill` = ?, `spoint` = ? WHERE `id` = ? LIMIT 1;")) {
            stmt.setString(1, skills.toJSONString());
            stmt.setInt(2, free);
            stmt.setInt(3, id);
            stmt.executeUpdate();
        }
        return ok(id, (t == null ? "Kỹ năng " + templateId : t.name)
                + " cấp " + moi + ", còn " + free + " điểm kỹ năng");
    }

    /**
     * Tẩy điểm kỹ năng: mọi kỹ năng về cấp 1, kho điểm về đầy.
     *
     * Chép đúng {@code Char.tayKyNang}: kỹ năng phân thân (mã 67-72) không đụng tới, và kho điểm
     * tính theo cấp độ chứ không cộng dồn số điểm vừa hoàn -- mỗi kỹ năng giữ lại cấp 1 miễn phí,
     * y như trong game.
     */
    public static JSONObject resetSkill(int id) throws Exception {
        Char c = live(id);
        if (c != null) {
            if (c.vSkill == null) {
                throw new IllegalStateException("Nhân vật này chưa nạp xong kỹ năng.");
            }
            int pool = skillPool(c.classId, c.level, c.limitKyNangSo, c.limitPhongLoi);
            for (int i = 0; i < c.vSkill.size(); i++) {
                Skill cu = c.vSkill.get(i);
                if (cu == null || cu.template == null || cu.isCloneSkill()) {
                    continue;
                }
                Skill thay = GameData.getInstance().getSkill(c.classId, cu.template.id, 1);
                if (thay == null) {
                    continue;
                }
                doiKyNang(c, i, cu, thay);
            }
            c.skillPoint = (short) pool;
            capNhatChiSo(c);
            try {
                c.getService().loadSkill();
            } catch (Throwable ignore) {
            }
            luuOnline(c, false, true);
            return ok(id, "Đã tẩy kỹ năng, có " + pool + " điểm để chia lại");
        }

        int classId = intCol(id, "class");
        JSONObject data = dataOf(id);
        int pool = skillPool(classId, num(data.get("level")),
                num(data.get("limitKyNangSo")), num(data.get("limitPhongLoi")));
        JSONArray skills = parseArray(strCol(id, "skill"));
        for (Object o : skills) {
            JSONObject s = (JSONObject) o;
            int sid = num(s.get("id"));
            if (GameData.getInstance().getSkill(classId, sid, 1) == null) {
                continue;
            }
            s.put("point", 1);
        }
        try (Connection conn = DbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE `players` SET `skill` = ?, `spoint` = ? WHERE `id` = ? LIMIT 1;")) {
            stmt.setString(1, skills.toJSONString());
            stmt.setInt(2, pool);
            stmt.setInt(3, id);
            stmt.executeUpdate();
        }
        return ok(id, "Đã tẩy kỹ năng, có " + pool + " điểm để chia lại");
    }

    /** Đưa mọi kỹ năng đã học lên cấp cao nhất, kho điểm kỹ năng về 0. */
    public static JSONObject maxSkill(int id) throws Exception {
        Char c = live(id);
        if (c != null) {
            if (c.vSkill == null) {
                throw new IllegalStateException("Nhân vật này chưa nạp xong kỹ năng.");
            }
            for (int i = 0; i < c.vSkill.size(); i++) {
                Skill cu = c.vSkill.get(i);
                if (cu == null || cu.template == null || cu.isCloneSkill()) {
                    continue;
                }
                Skill thay = capCaoNhat(c.classId, cu.template.id, cu.template.maxPoint);
                if (thay != null && thay.point != cu.point) {
                    doiKyNang(c, i, cu, thay);
                }
            }
            c.skillPoint = 0;
            capNhatChiSo(c);
            try {
                c.getService().loadSkill();
            } catch (Throwable ignore) {
            }
            luuOnline(c, false, true);
            return ok(id, "Đã nâng hết kỹ năng lên cấp tối đa");
        }

        int classId = intCol(id, "class");
        JSONArray skills = parseArray(strCol(id, "skill"));
        for (Object o : skills) {
            JSONObject s = (JSONObject) o;
            int sid = num(s.get("id"));
            SkillTemplate t = template(classId, sid);
            Skill thay = capCaoNhat(classId, sid, t == null ? 0 : t.maxPoint);
            if (thay != null) {
                s.put("point", (int) thay.point);
            }
        }
        try (Connection conn = DbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE `players` SET `skill` = ?, `spoint` = 0 WHERE `id` = ? LIMIT 1;")) {
            stmt.setString(1, skills.toJSONString());
            stmt.setInt(2, id);
            stmt.executeUpdate();
        }
        return ok(id, "Đã nâng hết kỹ năng lên cấp tối đa");
    }

    /** Cấp thêm lượt tẩy ở NPC (tayTiemNang / tayKyNang trong cột data). */
    public static JSONObject addResetTicket(int id, int tiemNang, int kyNang) throws Exception {
        Char c = live(id);
        if (c != null) {
            c.tayTiemNang = Math.max(0, c.tayTiemNang + tiemNang);
            c.tayKyNang = Math.max(0, c.tayKyNang + kyNang);
            c.saveData();
            return ok(id, "Lượt tẩy: tiềm năng " + c.tayTiemNang + ", kỹ năng " + c.tayKyNang);
        }
        JSONObject data = dataOf(id);
        int a = Math.max(0, num(data.get("tayTiemNang")) + tiemNang);
        int b = Math.max(0, num(data.get("tayKyNang")) + kyNang);
        data.put("tayTiemNang", a);
        data.put("tayKyNang", b);
        set(id, "`data` = ?", data.toJSONString());
        return ok(id, "Lượt tẩy: tiềm năng " + a + ", kỹ năng " + b);
    }

    // ------------------------------------------------------------- phép tính

    /**
     * Trần điểm tiềm năng của một cấp độ, chép từ {@code Char.tayTiemNang}.
     *
     * Không phải một phép nhân đơn giản: từ cấp 70 trở lên mỗi mốc lại cộng thêm một lần
     * {@code (level - mốc) * 10} nữa, nên các mốc chồng lên nhau.
     */
    public static int potentialPool(int classId, int level, int limitTiemNangSo, int limitBangHoa) {
        int pool = level * 10;
        int[] moc = { 70, 80, 90, 100, 130, 145 };
        for (int m : moc) {
            if (level >= m) {
                pool += (level - m) * 10;
            }
        }
        pool += 10 * (limitTiemNangSo + limitBangHoa);
        return pool;
    }

    /** Trần điểm kỹ năng, chép từ {@code Char.tayKyNang}. Chưa vào trường thì không có điểm nào. */
    public static int skillPool(int classId, int level, int limitKyNangSo, int limitPhongLoi) {
        if (classId == 0) {
            return 0;
        }
        return Math.max(0, level - 9) + limitKyNangSo + limitPhongLoi;
    }

    /** Bốn ô tiềm năng lúc mới tẩy, chép từ {@code Char.tayTiemNang}. */
    public static int[] basePotential(int classId) {
        boolean ngoai = classId == 1 || classId == 3 || classId == 5;
        return ngoai ? new int[] { 10, 5, 5, 5 } : new int[] { 5, 5, 5, 10 };
    }

    // ---------------------------------------------------------------- riêng

    // ------------------------------------------------------------- phân thân

    /**
     * Phân thân là **một nhân vật riêng**, không phải một kỹ năng.
     *
     * Nó có hàng riêng trong bảng {@code clone_char} với phái, giới tính, tiềm năng, điểm kỹ năng
     * và bộ kỹ năng của chính nó. Đừng lẫn với sáu mã 67-72 trong bộ kỹ năng của người chơi -- đó
     * là kỹ năng *triệu hồi* phân thân, nằm bên chủ.
     *
     * Mã hàng suy ra từ mã người chơi, xem {@code CloneChar} dòng 48.
     */
    private static int maPhanThan(int idNguoiChoi) {
        return -(10000000 + idNguoiChoi);
    }

    /** Mô tả phân thân để hiện lên bảng. Trả về cờ {@code co} = false nếu người này chưa có. */
    private static JSONObject xemPhanThan(int idNguoiChoi, Char chu) throws Exception {
        JSONObject o = new JSONObject();
        if (chu != null && chu.clone != null) {
            Char pt = chu.clone;
            o.put("co", true);
            o.put("nguon", "bộ nhớ máy chủ");
            o.put("classId", (int) pt.classId);
            o.put("className", className(pt.classId));
            o.put("gender", (int) pt.gender);
            o.put("spoint", (int) pt.skillPoint);
            o.put("point", (int) pt.potentialPoint);
            o.put("potential", intArray(pt.potential));
            JSONArray ds = new JSONArray();
            if (pt.vSkill != null) {
                for (Skill sk : pt.vSkill) {
                    if (sk == null || sk.template == null) {
                        continue;
                    }
                    ds.add(skillJson(sk.template.id, sk.template.name, sk.point, sk.template.maxPoint,
                            sk.level, sk.template.description, sk.template.iconId, sk, ""));
                }
            }
            o.put("skill", ds);
            return o;
        }

        try (Connection k = DbManager.getConnection();
             PreparedStatement st = k.prepareStatement(
                     "SELECT `class`,`gender`,`spoint`,`point`,`potential`,`skill`"
                             + " FROM `clone_char` WHERE `id` = ? LIMIT 1;")) {
            st.setInt(1, maPhanThan(idNguoiChoi));
            try (ResultSet rs = st.executeQuery()) {
                if (!rs.next()) {
                    o.put("co", false);
                    return o;
                }
                int lop = rs.getInt("class");
                o.put("co", true);
                o.put("nguon", "csdl");
                o.put("classId", lop);
                o.put("className", className((byte) lop));
                o.put("gender", rs.getInt("gender"));
                o.put("spoint", rs.getInt("spoint"));
                o.put("point", rs.getInt("point"));
                o.put("potential", parseArray(rs.getString("potential")));
                o.put("skill", skillJsonDb(lop, parseArray(rs.getString("skill")),
                        chu != null ? chu.phaTran : phaTranCsdl(idNguoiChoi)));
            }
        }
        return o;
    }

    /**
     * Nâng hoặc hạ một kỹ năng của phân thân.
     *
     * Chủ online thì sửa thẳng đối tượng trong bộ nhớ rồi gọi {@code saveData()} của **chủ** --
     * hàm ấy lưu luôn phân thân, xem cuối {@code Char.saveData}. Chủ offline thì sửa hàng
     * {@code clone_char}, lúc đó không có bản nào trong bộ nhớ để bị ghi đè.
     */
    public static JSONObject napKyNangPhanThan(int id, int templateId, int delta) throws Exception {
        Char chu = live(id);
        if (chu != null) {
            if (chu.clone == null) {
                throw new IllegalStateException("Nhân vật này chưa có phân thân.");
            }
            Char pt = chu.clone;
            if (pt.vSkill == null) {
                throw new IllegalStateException("Phân thân chưa nạp xong kỹ năng.");
            }
            Skill cu = null;
            int viTri = -1;
            for (int i = 0; i < pt.vSkill.size(); i++) {
                Skill sk = pt.vSkill.get(i);
                if (sk != null && sk.template != null && sk.template.id == templateId) {
                    cu = sk;
                    viTri = i;
                    break;
                }
            }
            if (cu == null) {
                throw new IllegalArgumentException("Phân thân chưa có kỹ năng mã " + templateId + ".");
            }
            int moi = cu.point + delta;
            kiemTraCapKyNang(pt.classId, templateId,
                    PhaTran.tranThat(templateId, cu.template.maxPoint, pt.phaTran), moi);
            if (delta > pt.skillPoint) {
                throw new IllegalArgumentException("Phân thân chỉ còn " + pt.skillPoint + " điểm kỹ năng.");
            }
            Skill thay = GameData.getInstance().getSkill(pt.classId, templateId, moi);
            if (thay == null) {
                throw new IllegalArgumentException("Không có bản cấp " + moi + " của kỹ năng này.");
            }
            doiKyNang(pt, viTri, cu, thay);
            pt.skillPoint = (short) Math.max(0, pt.skillPoint - delta);
            capNhatChiSo(pt);
            luuOnline(chu, false, false);
            return ok(id, "Phân thân: " + cu.template.name + " cấp " + moi
                    + ", còn " + pt.skillPoint + " điểm kỹ năng");
        }

        int ma = maPhanThan(id);
        int lop;
        int free;
        JSONArray skills;
        try (Connection k = DbManager.getConnection();
             PreparedStatement st = k.prepareStatement(
                     "SELECT `class`,`spoint`,`skill` FROM `clone_char` WHERE `id` = ? LIMIT 1;")) {
            st.setInt(1, ma);
            try (ResultSet rs = st.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalStateException("Nhân vật này chưa có phân thân.");
                }
                lop = rs.getInt("class");
                free = rs.getInt("spoint");
                skills = parseArray(rs.getString("skill"));
            }
        }
        JSONObject muc = null;
        for (Object o : skills) {
            JSONObject sk = (JSONObject) o;
            if (num(sk.get("id")) == templateId) {
                muc = sk;
                break;
            }
        }
        if (muc == null) {
            throw new IllegalArgumentException("Phân thân chưa có kỹ năng mã " + templateId + ".");
        }
        int moi = num(muc.get("point")) + delta;
        SkillTemplate t = template(lop, templateId);
        // Phân thân dùng chung chuỗi phá trần với bản chính -- chỉ có một cột `data` cho cả hai.
        kiemTraCapKyNang(lop, templateId,
                PhaTran.tranThat(templateId, t == null ? 0 : t.maxPoint, phaTranCsdl(id)), moi);
        if (GameData.getInstance().getSkill(lop, templateId, moi) == null) {
            throw new IllegalArgumentException("Không có bản cấp " + moi + " của kỹ năng này.");
        }
        if (delta > free) {
            throw new IllegalArgumentException("Phân thân chỉ còn " + free + " điểm kỹ năng.");
        }
        muc.put("point", moi);
        int conLai = Math.max(0, free - delta);
        try (Connection k = DbManager.getConnection();
             PreparedStatement st = k.prepareStatement(
                     "UPDATE `clone_char` SET `skill` = ?, `spoint` = ? WHERE `id` = ? LIMIT 1;")) {
            st.setString(1, skills.toJSONString());
            st.setInt(2, conLai);
            st.setInt(3, ma);
            st.executeUpdate();
        }
        return ok(id, "Phân thân: " + (t == null ? "kỹ năng " + templateId : t.name)
                + " cấp " + moi + ", còn " + conLai + " điểm kỹ năng");
    }

    /**
     * Dồn điểm tiềm năng của phân thân vào một ô, trừ đúng số điểm đó khỏi kho chưa dùng.
     *
     * Cho phép điểm âm để rút bớt ra, y như bản dành cho nhân vật chính -- trong game không làm
     * được, nhưng quản trị cần để sửa nhầm.
     */
    public static JSONObject chiaTiemNangPhanThan(int id, int index, int point) throws Exception {
        if (index < 0 || index >= 4) {
            throw new IllegalArgumentException("Ô tiềm năng phải từ 0 đến 3.");
        }
        Char chu = live(id);
        if (chu != null) {
            if (chu.clone == null) {
                throw new IllegalStateException("Nhân vật này chưa có phân thân.");
            }
            Char pt = chu.clone;
            if (point > pt.potentialPoint) {
                throw new IllegalArgumentException("Phân thân chỉ còn " + pt.potentialPoint + " điểm.");
            }
            if (pt.potential[index] + point < 0) {
                throw new IllegalArgumentException("Ô này chỉ có " + pt.potential[index] + " điểm.");
            }
            pt.potential[index] += point;
            pt.potentialPoint = (short) Math.max(0, pt.potentialPoint - point);
            capNhatChiSo(pt);
            luuOnline(chu, false, false);
            return ok(id, "Phân thân: " + POTENTIAL_NAME[index] + " = " + pt.potential[index]
                    + ", còn " + pt.potentialPoint + " điểm");
        }

        int ma = maPhanThan(id);
        int free;
        JSONArray pot;
        try (Connection k = DbManager.getConnection();
             PreparedStatement st = k.prepareStatement(
                     "SELECT `point`,`potential` FROM `clone_char` WHERE `id` = ? LIMIT 1;")) {
            st.setInt(1, ma);
            try (ResultSet rs = st.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalStateException("Nhân vật này chưa có phân thân.");
                }
                free = rs.getInt("point");
                pot = parseArray(rs.getString("potential"));
            }
        }
        if (pot.size() < 4) {
            throw new IllegalStateException("Dữ liệu tiềm năng của phân thân không đủ bốn ô.");
        }
        int dang = num(pot.get(index));
        if (point > free) {
            throw new IllegalArgumentException("Phân thân chỉ còn " + free + " điểm.");
        }
        if (dang + point < 0) {
            throw new IllegalArgumentException("Ô này chỉ có " + dang + " điểm.");
        }
        pot.set(index, dang + point);
        int conLai = Math.max(0, free - point);
        try (Connection k = DbManager.getConnection();
             PreparedStatement st = k.prepareStatement(
                     "UPDATE `clone_char` SET `potential` = ?, `point` = ? WHERE `id` = ? LIMIT 1;")) {
            st.setString(1, pot.toJSONString());
            st.setInt(2, conLai);
            st.setInt(3, ma);
            st.executeUpdate();
        }
        return ok(id, "Phân thân: " + POTENTIAL_NAME[index] + " = " + (dang + point)
                + ", còn " + conLai + " điểm");
    }

    // ------------------------------------------------------------- chỉ số nhân vật

    /**
     * Bộ số của bảng thông tin trong game: máu, chakra, sát thương, né, chính xác, kháng...
     *
     * Đọc thẳng các trường đã tính sẵn trên {@link Char} chứ **không tự cộng lại từ đồ**. Công thức
     * thật nằm trong {@code AbilityFromEquip}: máu tối đa còn nhân theo tiềm năng, cộng phần trăm,
     * cộng chỉ số hỗ trợ của kỹ năng. Chép lại công thức ấy ở đây là chép sai sớm muộn, mà sai thì
     * bảng điều khiển nói một đằng game một nẻo -- tệ hơn hẳn việc không hiện gì.
     *
     * Vì thế chỉ có với nhân vật **đang online**: các trường này chỉ tồn tại trong bộ nhớ.
     */
    private static JSONObject chiSo(Char c) {
        JSONObject o = new JSONObject();
        o.put("hp", c.hp);
        o.put("maxHP", c.maxHP);
        o.put("mp", c.mp);
        o.put("maxMP", c.maxMP);
        o.put("damage", c.damage);
        o.put("dameDown", c.dameDown);
        o.put("miss", c.miss);
        o.put("exactly", c.exactly);
        o.put("fatal", c.fatal);
        o.put("resFire", c.resFire);
        o.put("resIce", c.resIce);
        o.put("resWind", c.resWind);
        return o;
    }

    // ------------------------------------------------------------- thêm món

    /** Danh mục vật phẩm để chọn khi thêm: chỉ những cột cần cho việc tìm và nhận mặt. */
    /**
     * Vũ khí 100x -- chép từ ItemTemplate.isVk100x() vì ở đây chỉ có hàng cơ sở dữ liệu, chưa
     * dựng thành ItemTemplate.
     */
    private static final int[] VK_100X = {1111, 1112, 1113, 1114, 1115, 1116};

    /** Cầu lục đạo tím và đen -- hai món cộng chỉ số nặng nhất trong nhóm áo choàng. */
    private static final int[] CAU_LUC_DAO = {1263, 1264};

    /** Mã món trở lên được coi là đồ tự thêm về sau, không thuộc nội dung gốc của game. */
    private static final int MOC_CUSTOM = 1080;

    /**
     * Món này có bị cấm phát qua bảng web không.
     *
     * VÌ SAO CẦN: phát đồ mạnh qua web làm người chơi lấy được thứ mình muốn quá dễ, chơi vài hôm
     * là chán. Chặn bốn nhóm, giữ lại trang bị phổ thông và toàn bộ nguyên liệu:
     *
     *   - fashion > -1  : đồ vào ô trang bị 2. Vừa đẹp vừa cộng chỉ số, và cộng CHỒNG lên bộ đồ
     *                     thường nên là nguồn sức mạnh lớn nhất game.
     *   - id >= 1080    : đồ tự thêm về sau. Chỉ số đặt tay, thường mạnh hơn hẳn đồ gốc.
     *   - type 14       : mắt thần. Cả chuỗi 10 bậc vốn phải cày 100-1000 điểm danh vọng mỗi bậc.
     *   - vũ khí 100x, cầu lục đạo: hai nhóm mạnh nhất còn lại không lọt ba luật trên.
     *
     * Kiểm ở CẢ hai nơi -- lọc danh mục và chặn lúc phát. Chỉ lọc danh mục là vô nghĩa: themMon
     * nhận thẳng idMon từ yêu cầu HTTP, ai biết mã món vẫn gọi tay được.
     */
    public static boolean monBiKhoa(int id, int type, int fashion) {
        if (fashion > -1 || id >= MOC_CUSTOM || type == 14) {
            return true;
        }
        for (int v : VK_100X) {
            if (id == v) {
                return true;
            }
        }
        for (int v : CAU_LUC_DAO) {
            if (id == v) {
                return true;
            }
        }
        return false;
    }

    /** Tra bảng item rồi hỏi monBiKhoa. Trả false nếu không có món nào mang mã đó. */
    private static boolean monBiKhoa(int id) throws Exception {
        try (Connection k = DbManager.getConnection();
             PreparedStatement st = k.prepareStatement(
                     "SELECT `type`,`fashion` FROM `item` WHERE `id`=? LIMIT 1;")) {
            st.setInt(1, id);
            try (ResultSet rs = st.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }
                return monBiKhoa(id, rs.getInt("type"), rs.getInt("fashion"));
            }
        }
    }

    public static JSONArray danhMucMon() throws Exception {
        JSONArray out = new JSONArray();
        try (Connection k = DbManager.getConnection();
             PreparedStatement st = k.prepareStatement(
                     "SELECT `id`,`name`,`type`,`level`,`icon`,`description`,`fashion` FROM `item` ORDER BY `id`;");
             ResultSet rs = st.executeQuery()) {
            while (rs.next()) {
                if (monBiKhoa(rs.getInt("id"), rs.getInt("type"), rs.getInt("fashion"))) {
                    continue;
                }
                JSONObject j = new JSONObject();
                j.put("id", rs.getInt("id"));
                j.put("name", rs.getString("name"));
                j.put("type", rs.getInt("type"));
                j.put("level", rs.getInt("level"));
                j.put("icon", rs.getInt("icon"));
                j.put("mota", rs.getString("description") == null ? "" : rs.getString("description"));
                out.add(j);
            }
        }
        return out;
    }

    /**
     * Thêm món vào hành trang.
     *
     * Đi qua {@code ItemFactory.newItem} nên chỉ số của món do chính máy chủ sinh ra, giống hệt
     * lúc rơi trong game -- không tự bịa danh sách chỉ số, thứ chắc chắn sẽ lệch với đồ thật.
     *
     * Chỉ làm được khi người chơi **đang online**: chỗ trống trong túi, việc gộp món chồng và gói
     * tin báo cho máy khách đều nằm trong {@code Char.addItemToBag}. Nhét thẳng vào cột JSON thì
     * phải tự đoán ô trống và tự dựng đủ mười sáu khoá của một ô đồ -- đúng loại lỗi từng làm nhân
     * vật không đăng nhập được.
     */
    public static JSONObject themMon(int id, int idMon, int soLuong) throws Exception {
        // Chốt thứ hai của luật khoá. Lọc danh mục chỉ giấu món khỏi giao diện; đường này nhận
        // thẳng idMon từ yêu cầu HTTP nên phải tự kiểm lấy.
        if (monBiKhoa(idMon)) {
            throw new IllegalArgumentException(
                    "Món này đã bị khoá không phát qua bảng web (đồ trang bị 2, đồ tự thêm,"
                    + " mắt thần, vũ khí 100x, cầu lục đạo).");
        }
        if (soLuong <= 0) {
            soLuong = 1;
        }
        if (soLuong > 9999) {
            throw new IllegalArgumentException("Nhiều nhất 9999 cái một lần.");
        }
        // getItemTemplate tra thẳng theo chỉ số mảng nên mã ngoài dải ném IndexOutOfBounds với
        // câu tiếng Anh khó hiểu. Chặn trước để lời nhắn còn đọc được.
        ItemTemplate t;
        try {
            t = ItemManager.getInstance().getItemTemplate(idMon);
        } catch (RuntimeException ex) {
            t = null;
        }
        if (t == null) {
            throw new IllegalArgumentException("Không có vật phẩm mã " + idMon + ".");
        }
        Char c = live(id);
        if (c == null) {
            throw new IllegalStateException(
                    "Chỉ thêm món được cho người đang online. Việc này phải đi qua đúng đường của"
                            + " game để món có chỉ số thật và túi được xếp đúng chỗ trống.");
        }
        if (c.bag == null) {
            throw new IllegalStateException("Nhân vật vừa thoát game, hãy tải lại danh sách.");
        }
        // Dùng GiveItem.give chứ KHÔNG phải ItemFactory.newItem.
        //
        // newItem chỉ chạy initOption(), mà chỉ số của vũ khí và giáp KHÔNG nằm ở đó -- chúng nằm
        // trong bảng store_data, lấy ra qua Converter. Dựng bằng newItem thì món ra đời rỗng
        // không một chỉ số nào, đúng triệu chứng "thêm giáp xong mở ra chẳng thấy gì".
        //
        // GiveItem.give là đúng đường máy chủ vẫn dùng: tra dòng cửa hàng, dựng bằng Converter ở
        // mức kịch trần, rẽ riêng cho đồ cấp 90/100 (hai loại này cũng không có dòng cửa hàng, chỉ
        // số sinh bằng mã trong Item.randomOptionItem9x/10x), cộng mức nâng cấp tối đa, và xử lý
        // đúng việc xếp chồng -- đồ chồng đi một món mang cả số lượng, đồ không chồng thì mỗi ô
        // một đối tượng riêng.
        //
        // up = -1 lấy mức nâng cấp tối đa của món; sys = -1 là không kén hệ.
        String ketQua = GiveItem.give(c, idMon, soLuong, -1, -1);
        luuOnline(c, false, false);
        return ok(id, ketQua);
    }

    // ------------------------------------------------------------- xoá món

    /**
     * Xoá món khỏi túi hoặc rương, **không hỏi món có khoá hay đã nâng cấp hay không**.
     *
     * Trong game, khoá và độ nâng cấp là để chặn người chơi lỡ tay bán mất đồ quý. Bảng điều khiển
     * này là chỗ của người quản trị, và lý do thường gặp nhất để mở nó ra chính là gỡ một món đang
     * làm kẹt nhân vật -- món khoá lại càng hay là thủ phạm. Chặn ở đây thì công cụ vô dụng đúng
     * lúc cần nhất.
     *
     * @param soLuong bao nhiêu cái; số không hoặc âm nghĩa là xoá cả ô, kể cả ô đang chồng nhiều cái
     */
    public static JSONObject xoaMon(int id, String kho, int slot, int soLuong) throws Exception {
        kiemKho(kho);
        Char c = live(id);
        if (c != null) {
            return xoaMonOnline(c, kho, slot, soLuong);
        }
        return xoaMonCsdl(id, kho, new int[] { slot }, soLuong);
    }

    /** Xoá nhiều ô một lượt. Mỗi ô xoá trọn, không chia số lượng. */
    public static JSONObject xoaNhieuMon(int id, String kho, int[] slots) throws Exception {
        kiemKho(kho);
        if (slots == null || slots.length == 0) {
            throw new IllegalArgumentException("Chưa chọn ô nào.");
        }
        Char c = live(id);
        if (c != null) {
            int n = 0;
            for (int slot : slots) {
                if (xoaOTrongBoNho(c, kho, slot)) {
                    n++;
                }
            }
            luuOnline(c, false, false);
            return ok(id, "Đã xoá " + n + " ô khỏi " + tenKho(kho) + ".");
        }
        return xoaMonCsdl(id, kho, slots, 0);
    }

    private static void kiemKho(String kho) {
        if (!"bag".equals(kho) && !"box".equals(kho)) {
            throw new IllegalArgumentException("Chỉ xoá được trong túi hoặc rương."
                    + " Trang bị đang mặc thì phải cởi ra trước.");
        }
    }

    private static String tenKho(String kho) {
        return "bag".equals(kho) ? "hành trang" : "rương";
    }

    private static JSONObject xoaMonOnline(Char c, String kho, int slot, int soLuong) throws Exception {
        Item[] arr = "bag".equals(kho) ? c.bag : c.box;
        Item it = layO(arr, slot);
        String ten = it.template == null ? ("#" + it.id) : it.template.name;
        int dangCo = it.getQuantity();

        if (soLuong > 0 && soLuong < dangCo && "bag".equals(kho)) {
            // Bớt số lượng thì đi bằng đường của game để máy khách thấy ngay, khỏi đăng nhập lại.
            c.removeItem(slot, soLuong, true);
            luuOnline(c, false, false);
            return ok(c.id, "Đã bớt " + soLuong + " " + ten + ", còn "
                    + (arr[slot] == null ? 0 : arr[slot].getQuantity()) + ".");
        }
        if ("bag".equals(kho)) {
            c.removeItem(slot, dangCo, true);
        } else {
            arr[slot] = null;
        }
        luuOnline(c, false, false);
        return ok(c.id, "Đã xoá " + ten + (dangCo > 1 ? " x" + dangCo : "")
                + " khỏi " + tenKho(kho) + ".");
    }

    private static boolean xoaOTrongBoNho(Char c, String kho, int slot) {
        Item[] arr = "bag".equals(kho) ? c.bag : c.box;
        if (arr == null || slot < 0 || slot >= arr.length || arr[slot] == null) {
            return false;
        }
        if ("bag".equals(kho)) {
            c.removeItem(slot, arr[slot].getQuantity(), true);
        } else {
            arr[slot] = null;
        }
        return true;
    }

    private static Item layO(Item[] arr, int slot) {
        if (arr == null || slot < 0 || slot >= arr.length || arr[slot] == null) {
            throw new IllegalArgumentException("Ô " + slot + " đang trống.");
        }
        return arr[slot];
    }

    /**
     * Xoá khi nhân vật offline: sửa thẳng cột JSON.
     *
     * Ô nhận dạng bằng khoá {@code index} trong chính hàng đó chứ không phải vị trí trong mảng --
     * mảng lưu xuống không có ô trống, ô số 5 có thể nằm ở phần tử thứ ba.
     */
    private static JSONObject xoaMonCsdl(int id, String kho, int[] slots, int soLuong) throws Exception {
        Set<Integer> can = new HashSet<>();
        for (int s : slots) {
            can.add(s);
        }
        JSONArray arr = parseArray(strCol(id, kho));
        JSONArray giu = new JSONArray();
        int xoa = 0, bot = 0;
        for (Object o : arr) {
            JSONObject obj = (JSONObject) o;
            if (!can.contains(num(obj.get("index")))) {
                giu.add(obj);
                continue;
            }
            int dangCo = obj.get("quantity") == null ? 1 : num(obj.get("quantity"));
            if (soLuong > 0 && soLuong < dangCo) {
                obj.put("quantity", dangCo - soLuong);
                giu.add(obj);
                bot++;
            } else {
                xoa++;
            }
        }
        if (xoa == 0 && bot == 0) {
            throw new IllegalArgumentException("Không tìm thấy ô nào trong số đã chọn.");
        }
        set(id, "`" + kho + "` = ?", giu.toJSONString());
        return ok(id, "Đã xoá " + xoa + " ô" + (bot > 0 ? ", bớt số lượng " + bot + " ô" : "")
                + " khỏi " + tenKho(kho) + ".");
    }

    // ------------------------------------------------------------- nhiệm vụ

    /**
     * Nhiệm vụ đọc từ cơ sở dữ liệu, cho người đang offline.
     *
     * Chỉ có số và tên: bước với tiến độ nằm trong cột {@code task}, mà đọc được cũng chẳng làm
     * gì, vì bỏ qua nhiệm vụ đòi nhân vật phải online.
     */
    private static JSONObject moTaNhiemVuDb(int taskId, String cotTask) {
        JSONObject o = new JSONObject();
        o.put("id", taskId);
        TaskTemplate t = Task.getTaskTemplate(taskId);
        o.put("ten", t == null ? "(không có mẫu nhiệm vụ)" : t.getName());
        if (t != null) {
            o.put("soBuoc", t.getSubNames().length);
        }
        Object d = JSONValue.parse(cotTask == null ? "" : cotTask);
        if (d instanceof JSONObject) {
            Object i = ((JSONObject) d).get("index");
            Object n = ((JSONObject) d).get("count");
            if (i != null) {
                o.put("buoc", (int) Double.parseDouble(i.toString()) + 1);
            }
            if (n != null) {
                o.put("tienDo", String.valueOf((int) Double.parseDouble(n.toString())));
            }
        }
        return o;
    }

    /** Chặn vòng lặp nhảy tới chạy mãi nếu gặp nhiệm vụ không chịu tiến. */
    private static final int TOI_DA_NHAY = 200;

    /**
     * Nhiệm vụ chính đang làm: đang ở nhiệm vụ nào, bước mấy, còn thiếu bao nhiêu.
     *
     * {@code taskMain} rỗng nghĩa là vừa xong nhiệm vụ trước mà chưa gặp NPC để nhận nhiệm vụ kế --
     * vẫn tra được tên qua {@code taskId} để biết đang chờ cái gì.
     */
    private static JSONObject moTaNhiemVu(Char c) {
        JSONObject o = new JSONObject();
        Task task = c.taskMain;
        TaskTemplate t = task != null ? task.template : Task.getTaskTemplate(c.taskId);
        o.put("id", c.taskId);
        if (t == null) {
            o.put("ten", "(không có mẫu nhiệm vụ)");
            return o;
        }
        o.put("ten", t.getName());
        String[] subs = t.getSubNames();
        o.put("soBuoc", subs.length);
        if (task == null) {
            o.put("trangThai", "chưa nhận -- cần gặp NPC");
            return o;
        }
        short[] counts = t.getCounts();
        int i = task.index;
        o.put("buoc", i + 1);
        o.put("viec", i >= 0 && i < subs.length ? subs[i] : "?");
        o.put("tienDo", i >= 0 && i < counts.length && counts[i] > 0
                ? task.count + "/" + counts[i]
                : String.valueOf((int) task.count));
        o.put("buocCuoi", task.isComplete());
        return o;
    }

    /**
     * Nhân vật để thao tác nhiệm vụ. Bắt buộc online, và nói rõ vì sao chứ không im lặng.
     *
     * Bỏ qua nhiệm vụ phải chạy qua đúng đường của game (phát thưởng, phát vật phẩm mở đầu, gửi
     * gói tin xuống máy khách), mà đường ấy chỉ có trong bộ nhớ máy chủ. Sửa thẳng cột
     * {@code taskId} trong cơ sở dữ liệu thì nhiệm vụ nhảy số nhưng người chơi mất phần thưởng và
     * mất luôn vật phẩm mở đầu -- kẹt nặng hơn lúc đầu.
     */
    private static Char nhanVatLamNhiemVu(int id) throws Exception {
        Char c = live(id);
        if (c == null) {
            throw new IllegalStateException(
                    "Chỉ bỏ qua nhiệm vụ được cho người đang online. Việc này phải đi qua đúng"
                            + " đường của game để phát thưởng và vật phẩm mở đầu; sửa thẳng cơ sở"
                            + " dữ liệu thì số nhiệm vụ nhảy nhưng người chơi mất phần thưởng.");
        }
        if (c.isCleaned || c.bag == null) {
            throw new IllegalStateException("Nhân vật vừa thoát game, hãy tải lại danh sách.");
        }
        return c;
    }

    /** Bỏ qua một bước trong nhiệm vụ hiện tại. */
    public static JSONObject boQuaBuoc(int id) throws Exception {
        Char c = nhanVatLamNhiemVu(id);
        if (c.taskMain == null) {
            throw new IllegalStateException(c.name + " chưa nhận nhiệm vụ, không có bước để bỏ qua.");
        }
        if (c.taskMain.isComplete()) {
            throw new IllegalStateException("Đây là bước cuối rồi, hãy dùng \"Bỏ qua nhiệm vụ\".");
        }
        c.taskNext();
        return ok(id, c.name + ": đã bỏ qua một bước");
    }

    /** Kết thúc nhiệm vụ hiện tại và nhận nhiệm vụ kế. */
    public static JSONObject boQuaNhiemVu(int id) throws Exception {
        Char c = nhanVatLamNhiemVu(id);
        int truoc = c.taskId;
        String vuong = tienMotNhiemVu(c);
        if (vuong != null) {
            throw new IllegalStateException(c.name + ": " + vuong);
        }
        return ok(id, c.name + ": nhiệm vụ " + truoc + " -> " + c.taskId);
    }

    /** Nhảy tới một nhiệm vụ xa hơn, đi lần lượt từng nhiệm vụ một. */
    public static JSONObject nhayToiNhiemVu(int id, int muon) throws Exception {
        Char c = nhanVatLamNhiemVu(id);
        if (muon <= c.taskId) {
            throw new IllegalStateException(c.name + " đang ở nhiệm vụ " + c.taskId
                    + ", chỉ nhảy tới được nhiệm vụ lớn hơn.");
        }
        int truoc = c.taskId;
        int buoc = 0;
        String vuong = null;
        while (c.taskId < muon && buoc < TOI_DA_NHAY) {
            int dang = c.taskId;
            vuong = tienMotNhiemVu(c);
            if (vuong != null) {
                break;
            }
            if (c.taskId == dang) {
                vuong = "nhiệm vụ " + dang + " không tiến thêm được";
                break;
            }
            buoc++;
        }
        return ok(id, c.name + ": " + truoc + " -> " + c.taskId + " (" + buoc + " nhiệm vụ)"
                + (vuong != null ? " -- dừng vì " + vuong : ""));
    }

    /**
     * Kết thúc nhiệm vụ hiện tại rồi nhận luôn nhiệm vụ kế. Trả về lý do nếu không đi được, null
     * nếu xong.
     *
     * Đi bằng đúng đường mà vật phẩm "Lệnh bài hoàn thành" đi ({@code Char.finishTask(true)}), nên
     * phần thưởng và các gói tin gửi xuống máy khách giống hệt lúc chơi thật -- người chơi thấy
     * nhiệm vụ đổi ngay, không phải đăng nhập lại. Hàm đó là private nên phải gọi qua reflection;
     * sửa {@code Char.java} chỉ để đổi một từ khoá thì tệ hơn, file ấy hai mươi lăm nghìn dòng.
     */
    private static String tienMotNhiemVu(Char c) {
        if (c.isCleaned || c.bag == null) {
            return "nhân vật đã thoát game";
        }
        if (c.taskMain == null) {
            // Nhận trước đã: finishTask() chỉ tăng taskId khi đang có nhiệm vụ trong tay, gọi lúc
            // rỗng thì phần thưởng vẫn phát mà nhiệm vụ đứng yên.
            c.takingTask();
            if (c.taskMain == null) {
                return "không nhận được nhiệm vụ " + c.taskId + " (hết bảng nhiệm vụ?)";
            }
        }
        int truoc = c.taskId;
        try {
            java.lang.reflect.Method finish = Char.class.getDeclaredMethod("finishTask", boolean.class);
            finish.setAccessible(true);
            finish.invoke(c, Boolean.TRUE);
        } catch (Exception ex) {
            return "lỗi khi kết thúc nhiệm vụ: " + ex;
        }
        if (c.taskId == truoc) {
            // finishTask() quay ra sớm khi hành trang đầy -- phần thưởng nào cũng cần chỗ chứa.
            return "hành trang đầy, hãy dọn bớt rồi thử lại";
        }
        c.takingTask();
        return null;
    }

    /**
     * Cửa vào nhánh ghi: trả về đối tượng trong bộ nhớ, null nếu nhân vật offline.
     *
     * Chạy ngoài tiến trình máy chủ thì không với tới bộ nhớ được, nên phải chặn thêm bằng cột
     * {@code online}: ghi vào bảng của người đang chơi là thay đổi sẽ bị lần lưu sau xoá sạch.
     * Chỉ phần ghi mới đi qua đây; phần đọc dùng thẳng {@link #boNho}.
     */
    private static Char live(int id) throws Exception {
        Char c = boNho(id);
        if (c == null && !inServer && isOnlineTheoBang(id)) {
            throw new IllegalStateException(
                    "Nhân vật đang online. Bảng điều khiển chạy ngoài máy chủ nên không sửa được"
                            + " bản trong bộ nhớ, mà ghi thẳng vào CSDL thì lần máy chủ lưu tiếp"
                            + " theo sẽ xoá mất. Hãy đợi người này thoát game, hoặc chạy bảng điều"
                            + " khiển ngay trong máy chủ.");
        }
        return c;
    }

    /** Đối tượng trong bộ nhớ nếu với tới được, không phán xét gì thêm. */
    private static Char boNho(int id) {
        return inServer ? ServerManager.findCharById(id) : null;
    }

    private static boolean isOnline(int id, int cot) {
        if (inServer) {
            try {
                return ServerManager.findCharById(id) != null;
            } catch (Throwable ignore) {
                return cot != 0;
            }
        }
        return cot != 0;
    }

    private static boolean isOnlineTheoBang(int id) throws Exception {
        return intCol(id, "online") != 0;
    }

    /**
     * Lưu nhân vật online.
     *
     * {@code saveData()} ghi cả hàng nên một lần gọi là đủ cho mọi thay đổi; hai cờ chỉ để quyết
     * định có đẩy số mới xuống màn hình người chơi hay không.
     */
    private static void luuOnline(Char c, boolean doiTiemNang, boolean doiKyNang) {
        try {
            if (doiTiemNang) {
                c.getService().updatePotential();
            }
            if (doiKyNang) {
                c.getService().loadSkill();
            }
        } catch (Throwable ignore) {
            // Người chơi không có phiên hiển thị (bot, phân thân) thì bỏ qua, dữ liệu vẫn đúng.
        }
        c.saveData();
    }

    /** Tính lại chỉ số dẫn xuất; hỏng thì cũng không được để mất phần dữ liệu đã sửa. */
    /**
     * Bí kíp đang mặc và chỉ số của nó, cho phần tinh luyện trên bảng web.
     *
     * Trả về null khi chưa mặc bí kíp -- bảng web dựa vào đó để ẩn hẳn khối tinh luyện đi, thay
     * vì hiện một khối rỗng không bấm được.
     */
    private static JSONObject biKipJson(Char c) {
        if (c == null || c.equipment == null || c.equipment[ItemTemplate.TYPE_BIKIP] == null) {
            return null;
        }
        Equip bk = c.equipment[ItemTemplate.TYPE_BIKIP];
        JSONObject o = new JSONObject();
        o.put("id", bk.id);
        o.put("name", bk.template == null ? ("Bí kíp " + bk.id) : bk.template.name);
        o.put("icon", bk.template == null ? -1 : (int) bk.template.icon);
        JSONArray cs = new JSONArray();
        int cap = 0;
        if (bk.getOptions() != null) {
            for (ItemOption op : bk.getOptions()) {
                if (op == null || op.optionTemplate == null) {
                    continue;
                }
                if (op.optionTemplate.id == 85) {
                    cap = op.param;
                }
                JSONObject j = new JSONObject();
                j.put("ma", op.optionTemplate.id);
                j.put("text", op.getOptionString());
                j.put("param", op.param);
                cs.add(j);
            }
        }
        o.put("cap", cap);
        o.put("tran", TRAN_TINH_LUYEN);
        o.put("chiSo", cs);
        return o;
    }

    /**
     * Đưa một người đang online về đúng một toạ độ -- bản web của cửa sổ "Đi tới" trong công cụ.
     *
     * Đi đúng đường mà mục "Đi tới" ở menu quản trị đi: rời khu hiện tại, đặt toạ độ, rồi vào lại
     * khu của bản đồ đích. Tự gán mapId/x/y thì máy khách không nhận được gói đổi bản đồ và người
     * chơi đứng lơ lửng ở khu cũ -- nhìn thì như đã đi, mà đánh quái, nhặt đồ đều không được.
     *
     * Dùng khi có người kẹt trong hang, trong đấu trường, hoặc rơi vào bản đồ không còn khu nào.
     *
     * @param diemMacDinh true thì bỏ qua x/y, dùng điểm vào mặc định của bản đồ
     */
    public static JSONObject doiToaDo(int id, int map, int x, int y, boolean diemMacDinh)
            throws Exception {
        Char c = live(id);
        if (c == null) {
            throw new IllegalStateException("Chỉ đưa được người đang online: việc này phải đi qua"
                    + " đúng đường đổi khu của máy chủ, người offline thì chưa có khu nào để rời.");
        }
        Map dich = MapManager.getInstance().find(map);
        if (dich == null) {
            throw new IllegalArgumentException("Không có bản đồ mã " + map + ".");
        }
        int khu = NinjaUtils.randomZoneId(map);
        if (khu == -1) {
            // 62 trong 179 bản đồ không có khu tĩnh nào (đấu trường, hang động, địa đạo): khu của
            // chúng do hệ thống dựng lúc chạy, ném người vào đó là họ đứng ngoài mọi khu.
            throw new IllegalArgumentException("Bản đồ " + map + " không có khu nào để vào."
                    + " Đây là loại bản đồ chỉ sinh khu lúc chạy (phó bản, đấu trường, hang động).");
        }
        short[] xy = diemMacDinh
                ? NinjaUtils.getFirstPosition((short) map)
                : new short[] { (short) x, (short) y };
        c.outZone();
        c.setXY(xy);
        dich.joinZone(c, khu);
        luuOnline(c, false, false);
        return ok(id, "Đã đưa " + c.name + " về bản đồ " + c.mapId + " (" + c.x + "," + c.y + ")");
    }

    /**
     * Đặt điểm lưu -- nơi nhân vật hiện ra sau khi chết hoặc thoát ra vào lại.
     *
     * Ba trường là 1 Hirosaki, 27 Haruna, 72 Ookaza. Đặt vào một bản đồ nguy hiểm thì người chơi
     * chết xong hiện lại đúng chỗ vừa chết, nên chỉ nên đặt về làng hoặc trường.
     */
    public static JSONObject datDiemLuu(int id, int map) throws Exception {
        Char c = live(id);
        if (c == null) {
            throw new IllegalStateException("Chỉ đặt được cho người đang online.");
        }
        if (MapManager.getInstance().find(map) == null) {
            throw new IllegalArgumentException("Không có bản đồ mã " + map + ".");
        }
        c.saveCoordinate = (short) map;
        luuOnline(c, false, false);
        return ok(id, "Điểm lưu của " + c.name + " giờ là bản đồ " + map);
    }

    /**
     * Chọn thân để thao tác: "pt" là phân thân, còn lại là chủ thân.
     *
     * Chỉ chạy cho người đang online. Chỉ số và trang bị của cả hai thân nằm trong bộ nhớ máy chủ;
     * ghi thẳng vào CSDL thì lần họ thoát game sẽ bị đè mất.
     */
    private static Char nguoi(int id, String ai) throws Exception {
        Char c = live(id);
        if (c == null) {
            throw new IllegalStateException("Chỉ làm được cho người đang online: chỉ số nằm trong"
                    + " bản nhớ của máy chủ, ghi thẳng vào CSDL thì lần họ thoát game sẽ bị đè mất.");
        }
        if (!"pt".equals(ai)) {
            return c;
        }
        if (c.clone == null) {
            throw new IllegalStateException("Nhân vật này chưa triệu hồi phân thân.");
        }
        return c.clone;
    }

    /** Chủ thân của một thân bất kỳ -- nơi duy nhất gọi saveData được. */
    private static Char chuCua(Char c) {
        if (c instanceof com.nsoz.model.CloneChar) {
            return ((com.nsoz.model.CloneChar) c).human;
        }
        return c;
    }

    /** Bí kíp đang mặc, ném lỗi nếu chưa mặc. */
    private static Equip biKipDangMac(Char c) {
        if (c.equipment == null || c.equipment[ItemTemplate.TYPE_BIKIP] == null) {
            throw new IllegalArgumentException("Thân này chưa mặc bí kíp nào.");
        }
        return c.equipment[ItemTemplate.TYPE_BIKIP];
    }

    /**
     * Tinh luyện một món trang bị, đúng phép tính của NPC trong game.
     *
     * Mức cộng từng chỉ số lấy ở {@link TinhLuyen} -- cùng bảng mà đường trong game dùng, nên món
     * luyện ở đây mạnh yếu giống hệt món luyện ngoài kia. Trần cũng giữ nguyên {@value
     * com.nsoz.item.TinhLuyen#TRAN}.
     *
     * Khác trong game hai chỗ, đều cố ý: không trừ yên và tử tinh thạch, và luôn thành công thay
     * vì tỉ lệ tụt dần 60% xuống 6%.
     *
     * @param ai   "pt" là phân thân, còn lại là chủ thân
     * @param kho  "equip" là ô trang bị, "bag" là hành trang
     */
    public static JSONObject tinhLuyenMon(int id, String ai, String kho, int slot, int soLan)
            throws Exception {
        if (soLan <= 0) {
            soLan = 1;
        }
        if (soLan > TinhLuyen.TRAN) {
            soLan = TinhLuyen.TRAN;
        }
        Char c = nguoi(id, ai);
        Item it = layMon(c, kho, slot);
        if (it == null) {
            throw new IllegalArgumentException("Ô này không có món nào.");
        }
        if (!TinhLuyen.nhomTinhLuyen(it.template)) {
            throw new IllegalArgumentException(ten(it) + " không thuộc nhóm tinh luyện được.");
        }
        ItemOption cap = TinhLuyen.doTinhLuyen(it);
        if (cap == null) {
            throw new IllegalArgumentException(ten(it) + " chưa có dòng \"Độ tinh luyện\" --"
                    + " trong game phải dịch chuyển trang bị trước mới luyện được.");
        }
        if (cap.param >= TinhLuyen.TRAN) {
            throw new IllegalArgumentException(ten(it) + " đã đạt độ tinh luyện tối đa "
                    + TinhLuyen.TRAN + ".");
        }
        int daLam = 0;
        while (daLam < soLan && TinhLuyen.motLan(it)) {
            daLam++;
        }
        it.isLock = true;
        capNhatChiSo(c);
        try {
            c.getService().updateHp();
            c.getService().updateMp();
        } catch (Throwable ignore) {
        }
        luuOnline(chuCua(c), false, false);
        return ok(id, "Đã tinh luyện " + ten(it) + " " + daLam + " lần, độ tinh luyện "
                + cap.param + "/" + TinhLuyen.TRAN
                + (cap.param >= TinhLuyen.TRAN ? " -- kịch trần." : ""));
    }

    private static Item layMon(Char c, String kho, int slot) {
        if ("equip".equals(kho)) {
            return c.equipment == null || slot < 0 || slot >= c.equipment.length
                    ? null : c.equipment[slot];
        }
        return c.bag == null || slot < 0 || slot >= c.bag.length ? null : c.bag[slot];
    }

    private static String ten(Item it) {
        return it.template == null ? ("món " + it.id) : it.template.name;
    }

    /**
     * Đặt lại số lần phá trần của một chiêu, và kéo cấp chiêu xuống cho vừa trần mới.
     *
     * Dùng để đưa một nhân vật về đúng trạng thái TRƯỚC khi phá trần: gọi với soLan = 0 thì trần
     * trở lại trần gốc và cấp chiêu bị hạ xuống đúng mức đó. Không hoàn sách, vì sách phá trần
     * không tốn điểm kỹ năng nên hạ cấp cũng chẳng nợ ai điểm nào.
     *
     * Bắt buộc phải hạ cấp cùng lúc: cấp chiêu nằm ở bảng `skill`, mà số lần phá nằm ở cột `data`.
     * Sửa mỗi một bên thì hoặc chiêu vượt trần (khung thông tin trong game vỡ), hoặc bản cấp cao
     * không còn trong bảng và nhân vật hỏng lúc đăng nhập.
     */
    public static JSONObject datPhaTran(int id, int maChieu, int soLan) throws Exception {
        if (PhaTran.tranGoc(maChieu) < 0) {
            throw new IllegalArgumentException("Chiêu " + maChieu + " không nằm trong cơ chế phá trần.");
        }
        if (soLan < 0) {
            soLan = 0;
        }
        if (soLan > PhaTran.TOI_DA) {
            soLan = PhaTran.TOI_DA;
        }
        Char c = live(id);
        if (c != null) {
            c.phaTran = PhaTran.ghi(c.phaTran, maChieu, soLan);
            int tran = PhaTran.tranThat(maChieu, 0, c.phaTran);
            String ha = "";
            for (int i = 0; i < c.vSkill.size(); i++) {
                Skill sk = c.vSkill.get(i);
                if (sk == null || sk.template == null || sk.template.id != maChieu) {
                    continue;
                }
                if (sk.point > tran) {
                    Skill thay = GameData.getInstance().getSkill(c.classId, maChieu, tran);
                    if (thay == null) {
                        throw new IllegalStateException("Không có bản cấp " + tran + " của chiêu này.");
                    }
                    ha = " (hạ " + sk.point + " -> " + tran + ")";
                    doiKyNang(c, i, sk, thay);
                }
                break;
            }
            capNhatChiSo(c);
            luuOnline(c, false, true);
            return ok(id, "Phá trần chiêu " + maChieu + " đặt về " + soLan + " lần" + ha);
        }

        // Ngoại tuyến: sửa thẳng hai cột, `data` giữ số lần phá còn `skill` giữ cấp.
        JSONObject data = parseObject(strCol(id, "data"));
        data.put("phaTran", PhaTran.ghi(phaTranCsdl(id), maChieu, soLan));
        int tran = PhaTran.tranThat(maChieu, 0, String.valueOf(data.get("phaTran")));
        JSONArray skills = parseArray(strCol(id, "skill"));
        String ha = "";
        for (Object o : skills) {
            JSONObject sk = (JSONObject) o;
            if (num(sk.get("id")) != maChieu) {
                continue;
            }
            if (num(sk.get("point")) > tran) {
                ha = " (hạ " + num(sk.get("point")) + " -> " + tran + ")";
                sk.put("point", tran);
            }
            break;
        }
        try (Connection conn = DbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE `players` SET `data` = ?, `skill` = ? WHERE `id` = ? LIMIT 1;")) {
            stmt.setString(1, data.toJSONString());
            stmt.setString(2, skills.toJSONString());
            stmt.setInt(3, id);
            stmt.executeUpdate();
        }
        return ok(id, "Phá trần chiêu " + maChieu + " đặt về " + soLan + " lần" + ha);
    }

    /**
     * Mọi thứ thẻ "Tinh luyện" cần: bí kíp của hai thân, và mọi món đang mang tinh luyện được.
     *
     * Gom về một khoá để trang web không phải tự đoán món nào luyện được -- luật "luyện được" nằm
     * ở {@link TinhLuyen}, cùng luật mà NPC trong game dùng, nên danh sách này đúng bằng danh sách
     * người chơi thấy khi mở khung tinh luyện.
     */
    private static JSONObject tinhLuyenJson(Char c) {
        JSONObject o = new JSONObject();
        o.put("tran", TinhLuyen.TRAN);
        o.put("tranBiKip", TRAN_TINH_LUYEN);
        o.put("biKip", biKipJson(c));
        Char pt = c == null ? null : c.clone;
        boolean coPt = pt != null && !pt.isDead;
        o.put("coPhanThan", coPt);
        o.put("biKipPt", coPt ? biKipJson(pt) : null);
        JSONArray mon = new JSONArray();
        quetMonTinhLuyen(c, "chu", mon);
        if (coPt) {
            // Phân thân dùng chung hành trang với chủ thân, nên chỉ quét ô trang bị của nó --
            // quét cả túi là mỗi món hiện hai lần.
            quetTrangBi(pt, "pt", mon);
        }
        o.put("mon", mon);
        return o;
    }

    private static void quetMonTinhLuyen(Char c, String ai, JSONArray ra) {
        quetTrangBi(c, ai, ra);
        if (c != null && c.bag != null) {
            for (int i = 0; i < c.bag.length; i++) {
                JSONObject j = monTinhLuyenJson(c.bag[i], ai, "bag", i);
                if (j != null) {
                    ra.add(j);
                }
            }
        }
    }

    private static void quetTrangBi(Char c, String ai, JSONArray ra) {
        if (c == null || c.equipment == null) {
            return;
        }
        for (int i = 0; i < c.equipment.length; i++) {
            JSONObject j = monTinhLuyenJson(c.equipment[i], ai, "equip", i);
            if (j != null) {
                ra.add(j);
            }
        }
    }

    /** Một món trong danh sách tinh luyện; null nếu món này không luyện được. */
    private static JSONObject monTinhLuyenJson(Item it, String ai, String kho, int slot) {
        if (it == null || !TinhLuyen.nhomTinhLuyen(it.template)) {
            return null;
        }
        ItemOption cap = TinhLuyen.doTinhLuyen(it);
        if (cap == null) {
            return null;
        }
        JSONObject o = new JSONObject();
        o.put("ai", ai);
        o.put("kho", kho);
        o.put("slot", slot);
        o.put("id", it.id);
        o.put("name", ten(it));
        o.put("icon", it.template == null ? -1 : (int) it.template.icon);
        o.put("o", (int) it.template.type);
        o.put("cap", cap.param);
        JSONArray cs = new JSONArray();
        for (ItemOption op : it.options) {
            if (op == null || op.optionTemplate == null || op.optionTemplate.id == 85) {
                continue;
            }
            JSONObject j = new JSONObject();
            j.put("text", op.getOptionString());
            // Cho thấy trước lần luyện tới cộng bao nhiêu -- đúng thứ người ta muốn biết để quyết
            // định luyện tiếp hay dừng.
            j.put("them", op.optionTemplate.type == 8
                    ? TinhLuyen.mucCong(op.optionTemplate.id, cap.param) : 0);
            cs.add(j);
        }
        o.put("chiSo", cs);
        return o;
    }

    /**
     * Bộ chỉ số bí kíp có thể quay ra, chép nguyên khoảng ngẫu nhiên của Tashino trong game.
     *
     * Mỗi dòng là {mã chỉ số, cận dưới, cận trên, tỉ lệ góp mặt}. Hai dòng 92 và 99 trong game chỉ
     * được thả vào rổ khi tung đồng xu trúng, nên ở đây ghi 50 -- giữ nguyên độ hiếm của chúng.
     */
    /**
     * Trần độ tinh luyện, lấy đúng con số Tashino chặn trong game.
     *
     * {@code Char.java}, nhánh {@code CMDConfirmPopup.NANG_BI_KIP}: đọc dòng chỉ số 85 ra {@code cap},
     * {@code cap >= 9} là "Bí kíp của ngươi đã quá mạnh, ta không thể giúp được ngươi". Mỗi lần
     * thành công dòng 85 cộng đúng 1 (9/20 = 0 nên rơi vào mức tối thiểu +1), nên 9 vừa là số lần
     * nâng được vừa là độ tinh luyện cao nhất.
     */
    private static final int TRAN_TINH_LUYEN = 9;

    private static final int[][] RO_BIKIP = {
        {100, 10, 20, 100}, {82, 500, 1500, 100}, {83, 500, 1500, 100}, {84, 10, 20, 100},
        {86, 10, 20, 100}, {87, 100, 800, 100}, {88, 100, 1000, 100}, {89, 100, 1000, 100},
        {90, 100, 1000, 100}, {91, 10, 20, 100}, {92, 10, 20, 50}, {95, 10, 20, 100},
        {96, 10, 20, 100}, {97, 10, 20, 100}, {98, 5, 10, 100}, {99, 20, 80, 50},
    };

    /**
     * Quay lại toàn bộ chỉ số của bí kíp đang mặc -- đúng việc Tashino làm ở mục "Luyện bí kíp",
     * nhưng bấm được liên tục cho tới khi ưng.
     *
     * Khác bản trong game bốn chỗ, đều là cố ý:
     * <ul>
     *   <li>không tốn 500 lượng và không cần chừa ô trống trong hành trang;</li>
     *   <li>bí kíp ở nguyên trong ô trang bị, không rơi xuống túi bắt mặc lại -- có thế mới quay
     *       liên tiếp được;</li>
     *   <li>quay được cả bí kíp đã tinh luyện (trong game bị chặn), nhưng độ tinh luyện trở về 0
     *       vì dàn chỉ số là dàn mới hoàn toàn;</li>
     *   <li>rổ chỉ số lấy đủ mọi dòng. Vòng quay trong game gọi {@code nextInt(size - 1)} nên dòng
     *       cuối rổ không bao giờ trúng; ở đây không bỏ sót dòng nào.</li>
     * </ul>
     *
     * @param soDong số dòng chỉ số muốn có, 0 nghĩa là để ngẫu nhiên 1-5 như trong game
     */
    public static JSONObject doiChiSoBiKip(int id, int soDong, String ai) throws Exception {
        Char c = nguoi(id, ai);
        Equip bk = biKipDangMac(c);
        int muon = soDong <= 0 ? NinjaUtils.nextInt(1, 5) : Math.min(5, soDong);

        List<ItemOption> ro = new ArrayList<>();
        for (int[] d : RO_BIKIP) {
            if (d[3] >= 100 || NinjaUtils.nextBoolean()) {
                ro.add(new ItemOption(d[0], NinjaUtils.nextInt(d[1], d[2])));
            }
        }
        // Nền chỉ số cố định phải dựng lại chứ không được xoá trắng.
        //
        // Trong game vòng quay tạo một món MỚI bằng ItemFactory.newItem, mà hàm dựng của Item chạy
        // initOption -- nên bí kíp danh hiệu (Đệ Nhất/Nhị/Tam/Tứ) lấy lại nguyên bộ chỉ số riêng
        // của nó rồi mới cộng thêm mấy dòng quay được. Bí kíp thường thì initOption không cho gì,
        // nên ra đúng "85 + mấy dòng quay". Xoá trắng danh sách là mất luôn phần làm nên danh hiệu.
        List<ItemOption> moi = new ArrayList<>();
        try {
            Item nen = ItemFactory.getInstance().newItem(bk.id);
            if (nen.options != null) {
                for (ItemOption o : nen.options) {
                    if (o != null && o.optionTemplate != null && o.optionTemplate.id != 85) {
                        moi.add(o);
                    }
                }
            }
        } catch (Throwable bo) {
            // Không dựng được món mẫu thì vẫn quay được, chỉ là không có nền -- vẫn hơn là hỏng.
        }
        int nenCo = moi.size();
        moi.add(new ItemOption(85, 0));
        for (int i = 0; i < muon && !ro.isEmpty(); i++) {
            moi.add(ro.remove(NinjaUtils.nextInt(ro.size())));
        }
        bk.options.clear();
        bk.options.addAll(moi);
        bk.isLock = true;

        capNhatChiSo(c);
        try {
            c.getService().updateHp();
            c.getService().updateMp();
        } catch (Throwable ignore) {
        }
        luuOnline(chuCua(c), false, false);
        return ok(id, "Đã đổi sang dàn " + muon + " dòng quay"
                + (nenCo > 0 ? " (giữ nguyên " + nenCo + " dòng chỉ số riêng của bí kíp)" : "")
                + ", độ tinh luyện về 0. Trong game phải thoát ra vào lại mới thấy chữ mới, còn"
                + " tác dụng thì đã ăn ngay.");
    }

    /**
     * Tinh luyện bí kíp đang mặc, không tốn thạch, không tốn lượng, không hỏng.
     *
     * Phép tính chép ĐÚNG nhánh tinh luyện ở NPC Tashino (Char.java, CMDConfirmPopup.NANG_BI_KIP):
     * mỗi chỉ số cộng thêm 1/20 giá trị hiện có, riêng bốn mã 92/86/84/91 thì 1/15, và không bao
     * giờ dưới 1. Chỉ số 85 ("Độ tinh luyện") cũng nằm trong danh sách nên tự tăng theo, đúng như
     * trong game.
     *
     * Trần thì KHÔNG nới: dừng đúng ở độ tinh luyện {@value #TRAN_TINH_LUYEN} như Tashino. Nới
     * trần ở đây là mở một đường sức mạnh mà đường chơi thật không có, mà mỗi bậc lại cộng theo
     * phần trăm nên bấm thêm vài chục lần là chỉ số phình ra vô hạn.
     *
     * Khác trong game ba chỗ, đều là cố ý:
     *   - không trừ thạch và lượng, vì đây là công cụ quản trị
     *   - luôn thành công, thay vì tỉ lệ 100 - 11*cấp phần trăm (cấp 0 chắc chắn được, cấp 8 chỉ
     *     còn 12%) -- người chơi thật vẫn phải cày đủ thạch cho từng lần trượt
     *   - GIỮ NGUYÊN món ở ô bí kíp; trong game mỗi lần thành công món rơi về hành trang và phải
     *     mặc lại, làm thế thì không luyện liên tục được
     */
    public static JSONObject tinhLuyenBiKip(int id, int soLan, String ai) throws Exception {
        if (soLan <= 0) {
            soLan = 1;
        }
        if (soLan > TRAN_TINH_LUYEN) {
            soLan = TRAN_TINH_LUYEN;
        }
        Char c = nguoi(id, ai);
        Equip bk = biKipDangMac(c);
        if (bk.getOptions() == null || bk.getOptions().isEmpty()) {
            throw new IllegalArgumentException("Bí kíp này không có chỉ số để tinh luyện.");
        }
        ItemOption doTinhLuyen = null;
        for (ItemOption o : bk.getOptions()) {
            if (o != null && o.optionTemplate != null && o.optionTemplate.id == 85) {
                doTinhLuyen = o;
                break;
            }
        }
        if (doTinhLuyen == null) {
            throw new IllegalArgumentException("Bí kíp này chưa có dòng \"Độ tinh luyện\" nên chưa"
                    + " tinh luyện được -- hãy bấm \"Đổi chỉ số\" một lần trước, đúng như trong"
                    + " game phải luyện bí kíp ở Tashino rồi mới nâng được.");
        }
        if (doTinhLuyen.param >= TRAN_TINH_LUYEN) {
            throw new IllegalArgumentException("Bí kíp đã đạt độ tinh luyện tối đa "
                    + TRAN_TINH_LUYEN + ", trong game Tashino cũng từ chối nâng tiếp.");
        }
        int daLam = 0;
        while (daLam < soLan && doTinhLuyen.param < TRAN_TINH_LUYEN) {
            for (ItemOption o : bk.getOptions()) {
                if (o == null || o.optionTemplate == null) {
                    continue;
                }
                int themVao = o.param / 20;
                int ma = o.optionTemplate.id;
                if (ma == 92 || ma == 86 || ma == 84 || ma == 91) {
                    themVao = o.param / 15;
                }
                o.param += themVao > 0 ? themVao : 1;
            }
            daLam++;
        }
        capNhatChiSo(c);
        try {
            // Gửi lại HP/MP để bảng thông tin trong game hiện chỉ số mới ngay, khỏi thoát vào lại.
            c.getService().updateHp();
            c.getService().updateMp();
        } catch (Throwable ignore) {
            // Không có phiên hiển thị thì thôi, chỉ số trong bộ nhớ đã đúng.
        }
        luuOnline(chuCua(c), false, false);
        int cap = 0;
        for (ItemOption o : bk.getOptions()) {
            if (o != null && o.optionTemplate != null && o.optionTemplate.id == 85) {
                cap = o.param;
            }
        }
        return ok(id, "Đã tinh luyện " + daLam + " lần, độ tinh luyện hiện là " + cap + "/"
                + TRAN_TINH_LUYEN
                + (cap >= TRAN_TINH_LUYEN ? " -- đã kịch trần, không nâng thêm được nữa." : ""));
    }

    private static void capNhatChiSo(Char c) {
        try {
            c.setAbility();
        } catch (Throwable ignore) {
        }
    }

    /**
     * Thay một kỹ năng trong cả bốn chỗ máy chủ giữ nó.
     *
     * {@code vSkill} là danh sách đầy đủ, còn {@code vSupportSkill} và {@code vSkillFight} là hai
     * bản lọc trỏ tới cùng đối tượng. Đổi mỗi vSkill thì hai bản kia còn giữ đối tượng cũ và
     * nhân vật vẫn đánh bằng cấp cũ -- đúng cách {@code Char.upSkill} làm.
     */
    /**
     * Quên hẳn một kỹ năng: xoá khỏi danh sách, coi như chưa từng học.
     *
     * Khác "tẩy điểm kỹ năng" -- cái đó đưa mọi kỹ năng về cấp 1 nhưng vẫn giữ. Ở đây món sách võ
     * công coi như chưa dùng, kỹ năng biến mất khỏi bảng.
     *
     * Hoàn lại {@code cấp - 1} điểm: học kỹ năng bằng sách không tốn điểm nào (sách thêm thẳng
     * cấp 1), nên chỉ những cấp nâng lên sau đó mới là điểm người chơi đã bỏ ra.
     */
    public static JSONObject quenKyNang(int id, int templateId) throws Exception {
        Char c = live(id);
        if (c != null) {
            if (c.vSkill == null) {
                throw new IllegalStateException("Nhân vật này chưa nạp xong kỹ năng.");
            }
            Skill cu = null;
            int viTri = -1;
            for (int i = 0; i < c.vSkill.size(); i++) {
                Skill sk = c.vSkill.get(i);
                if (sk != null && sk.template != null && sk.template.id == templateId) {
                    cu = sk;
                    viTri = i;
                    break;
                }
            }
            if (cu == null) {
                throw new IllegalArgumentException("Nhân vật chưa có kỹ năng mã " + templateId + ".");
            }
            String ten = cu.template.name;
            int hoan = Math.max(0, cu.point - 1);
            c.vSkill.remove(viTri);
            // Gỡ khỏi mọi danh sách phụ, nếu không thì đánh nhau vẫn dùng được kỹ năng vừa xoá.
            if (c.vSupportSkill != null) {
                c.vSupportSkill.removeIf(x -> x != null && x.template != null && x.template.id == templateId);
            }
            if (c.vSkillFight != null) {
                c.vSkillFight.removeIf(x -> x != null && x.template != null && x.template.id == templateId);
            }
            // Và khỏi ba mảng phím tắt, không thì phím tắt trỏ vào chỗ trống.
            for (byte[] tat : new byte[][]{c.onOSkill, c.onKSkill, c.onCSkill}) {
                if (tat == null) {
                    continue;
                }
                for (int j = 0; j < tat.length; j++) {
                    if (tat[j] == templateId) {
                        tat[j] = -1;
                    }
                }
            }
            if (c.selectedSkill != null && c.selectedSkill.template != null
                    && c.selectedSkill.template.id == templateId) {
                c.selectedSkill = c.vSkill.isEmpty() ? null : c.vSkill.get(0);
            }
            c.skillPoint = (short) (c.skillPoint + hoan);
            capNhatChiSo(c);
            try {
                c.getService().loadSkill();
            } catch (Throwable ignore) {
                // Không có phiên hiển thị thì thôi, dữ liệu đã đúng rồi.
            }
            luuOnline(c, false, true);
            return ok(id, "Đã quên " + ten + ", hoàn " + hoan + " điểm kỹ năng");
        }

        JSONArray skills = parseArray(strCol(id, "skill"));
        JSONArray giu = new JSONArray();
        int hoan = -1;
        for (Object o : skills) {
            JSONObject sk = (JSONObject) o;
            if (num(sk.get("id")) == templateId) {
                hoan = Math.max(0, num(sk.get("point")) - 1);
            } else {
                giu.add(sk);
            }
        }
        if (hoan < 0) {
            throw new IllegalArgumentException("Nhân vật chưa có kỹ năng mã " + templateId + ".");
        }
        int free = intCol(id, "spoint") + hoan;
        try (Connection conn = DbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE `players` SET `skill` = ?, `spoint` = ? WHERE `id` = ? LIMIT 1;")) {
            stmt.setString(1, giu.toJSONString());
            stmt.setInt(2, free);
            stmt.setInt(3, id);
            stmt.executeUpdate();
        }
        SkillTemplate t = template(intCol(id, "class"), templateId);
        return ok(id, "Đã quên " + (t == null ? "kỹ năng " + templateId : t.name)
                + ", hoàn " + hoan + " điểm kỹ năng");
    }

    private static void doiKyNang(Char c, int viTri, Skill cu, Skill thay) {
        c.vSkill.set(viTri, thay);
        if (thay.template.type == Skill.SKILL_AUTO_USE) {
            if (c.vSupportSkill == null) {
                return;
            }
            for (int j = 0; j < c.vSupportSkill.size(); j++) {
                if (c.vSupportSkill.get(j).template.id == cu.template.id) {
                    c.vSupportSkill.set(j, thay);
                }
            }
        } else if ((thay.template.type == Skill.SKILL_CLICK_USE_ATTACK
                || thay.template.type == Skill.SKILL_CLICK_LIVE
                || thay.template.type == Skill.SKILL_CLICK_USE_BUFF
                || thay.template.type == Skill.SKILL_CLICK_NPC)
                && (thay.template.maxPoint == 0 || thay.point > 0)) {
            if (c.vSkillFight == null) {
                return;
            }
            for (int j = 0; j < c.vSkillFight.size(); j++) {
                if (c.vSkillFight.get(j).template.id == cu.template.id) {
                    c.vSkillFight.set(j, thay);
                }
            }
        }
    }

    private static void kiemTraCapKyNang(int classId, int templateId, int maxPoint, int moi) {
        if (moi < 1) {
            throw new IllegalArgumentException("Cấp kỹ năng thấp nhất là 1.");
        }
        if (maxPoint > 0 && moi > maxPoint) {
            throw new IllegalArgumentException("Kỹ năng này cao nhất là cấp " + maxPoint + ".");
        }
    }

    private static Skill capCaoNhat(int classId, int templateId, int maxPoint) {
        for (int p = Math.max(maxPoint, 1); p >= 1; p--) {
            Skill s = GameData.getInstance().getSkill(classId, templateId, p);
            if (s != null) {
                return s;
            }
        }
        return null;
    }

    /** Chuỗi phá trần đọc thẳng từ cột `data` -- dùng cho nhánh nhân vật đang offline. */
    private static String phaTranCsdl(int id) {
        try {
            Object o = parseObject(strCol(id, "data")).get("phaTran");
            return o == null ? "" : String.valueOf(o);
        } catch (Throwable ignore) {
            return "";
        }
    }

    private static SkillTemplate template(int classId, int templateId) {
        try {
            return GameData.getInstance().getTemplate(classId, templateId);
        } catch (Throwable ignore) {
            return null;
        }
    }

    // ------------------------------------------------------- dựng JSON món đồ

    private static JSONArray gearJson(JSONArray raw) {
        JSONArray out = new JSONArray();
        for (Object o : raw) {
            JSONObject obj = (JSONObject) o;
            Item it = item(obj);
            if (it == null) {
                continue;
            }
            out.add(itemJson(it, it.template.type));
        }
        return out;
    }

    private static JSONArray itemsJson(JSONArray raw) {
        JSONArray out = new JSONArray();
        for (Object o : raw) {
            JSONObject obj = (JSONObject) o;
            Item it = item(obj);
            if (it == null) {
                continue;
            }
            out.add(itemJson(it, num(obj.get("index"))));
        }
        return out;
    }

    private static JSONArray liveItems(Item[] arr, int len) {
        JSONArray out = new JSONArray();
        if (arr == null) {
            return out;
        }
        for (int i = 0; i < Math.min(len, arr.length); i++) {
            if (arr[i] != null) {
                out.add(itemJson(arr[i], i));
            }
        }
        return out;
    }

    /**
     * Dựng lại món từ JSON đã lưu.
     *
     * Cố tình đi qua {@code new Item(JSONObject)} chứ không tự đọc lấy: chỉ số của món không nằm
     * trong bảng {@code item} mà trong chính chuỗi đã lưu, và đúng hàm này mới ghép được ngọc
     * khảm vào danh sách chỉ số. Món có mã không còn trong dữ liệu thì bỏ qua thay vì làm hỏng
     * cả trang.
     */
    private static Item item(JSONObject obj) {
        try {
            return new Item(obj);
        } catch (Throwable ex) {
            return null;
        }
    }

    private static JSONObject itemJson(Item it, int slot) {
        JSONObject o = new JSONObject();
        ItemTemplate t = it.template;
        o.put("slot", slot);
        o.put("slotName", slot >= 0 && slot < SLOT_NAME.length ? SLOT_NAME[slot] : String.valueOf(slot));
        o.put("id", it.id);
        o.put("name", t == null ? "?" : t.name);
        o.put("type", t == null ? -1 : t.type);
        o.put("level", t == null ? 0 : t.level);
        o.put("icon", t == null ? -1 : (int) t.icon);
        o.put("moTa", t == null || t.description == null ? "" : t.description);
        o.put("upgrade", (int) it.upgrade);
        o.put("sys", (int) it.sys);
        o.put("quantity", it.getQuantity());
        o.put("lock", it.isLock);
        o.put("expire", it.expire);

        JSONArray opts = new JSONArray();
        try {
            // getOptions() chứ không phải options: bản này gộp cả chỉ số của ngọc khảm vào, đúng
            // như bảng thông tin món trong game.
            for (ItemOption op : it.getOptions()) {
                if (op == null || op.optionTemplate == null) {
                    continue;
                }
                JSONObject j = new JSONObject();
                j.put("id", op.optionTemplate.id);
                j.put("param", op.param);
                j.put("text", op.getOptionString());
                opts.add(j);
            }
        } catch (Throwable ignore) {
        }
        o.put("options", opts);

        JSONArray gems = new JSONArray();
        if (it.gems != null) {
            for (Item g : it.gems) {
                JSONObject j = new JSONObject();
                j.put("id", g.id);
                j.put("name", g.template == null ? "?" : g.template.name);
                j.put("upgrade", (int) g.upgrade);
                gems.add(j);
            }
        }
        o.put("gems", gems);
        return o;
    }

    private static JSONArray skillJsonDb(int classId, JSONArray raw, String phaTran) {
        JSONArray out = new JSONArray();
        for (Object o : raw) {
            JSONObject s = (JSONObject) o;
            int sid = num(s.get("id"));
            int point = num(s.get("point"));
            SkillTemplate t = template(classId, sid);
            Skill ban = GameData.getInstance().getSkill(classId, sid, point);
            out.add(skillJson(sid, t == null ? "Kỹ năng " + sid : t.name, point,
                    t == null ? 0 : t.maxPoint, ban == null ? 0 : ban.level,
                    t == null ? "" : t.description, t == null ? -1 : (int) t.iconId, ban, phaTran));
        }
        return out;
    }

    /**
     * @param ban bản kỹ năng ở cấp hiện tại, để lấy chỉ số thật. Mô tả trong bảng chỉ nói chung
     *            chung ("nâng cao sức tấn công") nên nhìn vào không biết cấp này mạnh cỡ nào --
     *            phải kèm đúng con số của cấp đang có thì mới quyết định được có nên nâng không.
     */
    private static JSONObject skillJson(int id, String name, int point, int maxPoint,
            int levelRequire, String description, int icon, Skill ban, String phaTran) {
        JSONObject o = new JSONObject();
        int goc = PhaTran.tranGoc(id);
        int daPha = goc > -1 ? PhaTran.doc(phaTran, id) : 0;
        o.put("id", id);
        o.put("name", name);
        o.put("point", point);
        // maxPoint gửi đi là trần HIỆN TẠI để nút +1 tự tắt đúng lúc; mức cao nhất gửi riêng.
        o.put("maxPoint", goc > -1 ? goc + daPha : maxPoint);
        o.put("tranCao", goc > -1 ? goc + PhaTran.TOI_DA : 0);
        o.put("daPha", daPha);
        o.put("toiDaPha", goc > -1 ? PhaTran.TOI_DA : 0);
        o.put("levelRequire", levelRequire);
        o.put("description", description == null ? "" : description);
        o.put("clone", id >= CLONE_SKILL_MIN && id <= CLONE_SKILL_MAX);
        o.put("icon", icon);
        JSONArray cs = new JSONArray();
        if (ban != null) {
            o.put("mp", (int) ban.manaUse);
            o.put("cho", ban.coolDown);
            o.put("tamDanh", (int) ban.maxFight);
            if (ban.options != null) {
                for (SkillOption op : ban.options) {
                    if (op == null || op.optionTemplate == null || op.optionTemplate.name == null) {
                        continue;
                    }
                    cs.add(op.optionTemplate.name.replace("#", String.valueOf(op.param)));
                }
            }
        }
        o.put("chiSo", cs);
        return o;
    }

    // ------------------------------------------------------------ tiện ích

    private static JSONObject ok(int id, String message) throws Exception {
        JSONObject o = new JSONObject();
        o.put("ok", true);
        o.put("message", message);
        o.put("char", view(id));
        return o;
    }

    private static void set(int id, String setClause, Object value) throws Exception {
        try (Connection conn = DbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE `players` SET " + setClause + " WHERE `id` = ? LIMIT 1;")) {
            stmt.setObject(1, value);
            stmt.setInt(2, id);
            stmt.executeUpdate();
        }
    }

    private static int intCol(int id, String col) throws Exception {
        try (Connection conn = DbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT `" + col + "` FROM `players` WHERE `id` = ? LIMIT 1;")) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("Không có nhân vật id " + id);
                }
                return rs.getInt(1);
            }
        }
    }

    private static String strCol(int id, String col) throws Exception {
        try (Connection conn = DbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT `" + col + "` FROM `players` WHERE `id` = ? LIMIT 1;")) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("Không có nhân vật id " + id);
                }
                return rs.getString(1);
            }
        }
    }

    private static JSONObject dataOf(int id) throws Exception {
        return parseObject(strCol(id, "data"));
    }

    private static int[] readPotential(String json) {
        int[] out = { 5, 5, 5, 5 };
        JSONArray arr = parseArray(json);
        for (int i = 0; i < 4 && i < arr.size(); i++) {
            out[i] = num(arr.get(i));
        }
        return out;
    }

    private static JSONArray parseArray(String json) {
        Object o = json == null ? null : JSONValue.parse(json);
        return o instanceof JSONArray ? (JSONArray) o : new JSONArray();
    }

    private static JSONObject parseObject(String json) {
        Object o = json == null ? null : JSONValue.parse(json);
        return o instanceof JSONObject ? (JSONObject) o : new JSONObject();
    }

    @SuppressWarnings("unchecked")
    private static JSONArray intArray(int[] value) {
        JSONArray arr = new JSONArray();
        for (int v : value) {
            arr.add(v);
        }
        return arr;
    }

    private static int sum(int[] value) {
        int s = 0;
        for (int v : value) {
            s += v;
        }
        return s;
    }

    private static int num(Object o) {
        if (o == null) {
            return 0;
        }
        try {
            return (int) Double.parseDouble(o.toString());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private static long longOf(Object o) {
        if (o == null) {
            return 0L;
        }
        try {
            return (long) Double.parseDouble(o.toString());
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    public static String className(int classId) {
        try {
            return GameData.getInstance().findClass((byte) classId).getName();
        } catch (Throwable ignore) {
            return "Phái " + classId;
        }
    }

    /** Danh sách tên chỉ số, để trang web hiện chú thích mà không cần nhúng cứng. */
    public static JSONObject meta() {
        JSONObject o = new JSONObject();
        JSONArray pot = new JSONArray();
        for (int i = 0; i < POTENTIAL_NAME.length; i++) {
            JSONObject j = new JSONObject();
            j.put("index", i);
            j.put("name", POTENTIAL_NAME[i]);
            j.put("note", POTENTIAL_NOTE[i]);
            pot.add(j);
        }
        o.put("potential", pot);
        JSONArray slot = new JSONArray();
        for (String s : SLOT_NAME) {
            slot.add(s);
        }
        o.put("slot", slot);
        o.put("inServer", inServer);
        return o;
    }

}
