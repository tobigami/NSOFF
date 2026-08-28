import com.nsoz.db.jdbc.DbManager;
import com.nsoz.server.Config;
import com.nsoz.util.StringUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Kiem tra phan tao tai khoan truoc khi nap: cau lenh SQL cua AccountAdmin co chay that khong,
 * mat khau bam ra co qua duoc dung ham luc dang nhap khong, va tai khoan trung co bi chan khong.
 * Tao mot tai khoan tam roi xoa di, khong dung den tai khoan that.
 */
public class AccountCheck {

    /**
     * DbManager.executeQuery() dong ca statement lan connection truoc khi tra ResultSet ve, doc
     * qua dong dau la loi -- nen o day tu mo connection giong AccountAdmin.
     */
    static String[] one(String sql, Object... args) throws Exception {
        Connection conn = DbManager.getConnection();
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            for (int i = 0; i < args.length; i++) ps.setObject(i + 1, args[i]);
            ResultSet rs = ps.executeQuery();
            String[] row = null;
            if (rs.next()) {
                row = new String[rs.getMetaData().getColumnCount()];
                for (int i = 0; i < row.length; i++) row[i] = rs.getString(i + 1);
            }
            rs.close();
            ps.close();
            return row;
        } finally {
            DbManager.closeConnection(conn);
        }
    }

    static void listAll(String sql) throws Exception {
        Connection conn = DbManager.getConnection();
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                System.out.println("   " + rs.getInt("id") + " | " + rs.getString("username")
                        + " | luong=" + rs.getLong("luong") + " | nhan vat=" + rs.getString("chars"));
            }
            rs.close();
            ps.close();
        } finally {
            DbManager.closeConnection(conn);
        }
    }

    static final String USER = "zzcheck01";
    static final String PASS = "matkhau123";

    public static void main(String[] a) throws Exception {
        if (!Config.getInstance().load() || !DbManager.start()) {
            System.out.println("!! khong nap duoc cau hinh/DB");
            return;
        }
        DbManager.executeUpdate("DELETE FROM `users` WHERE `username` = ?;", USER);

        System.out.println("1) tao tai khoan");
        DbManager.executeUpdate(
                "INSERT INTO `users`(`username`, `password`, `activated`, `luong`) VALUES (?, ?, ?, ?);",
                USER, StringUtils.genPass(PASS), 1, 999);
        String[] r = one("SELECT `id`,`password`,`activated`,`luong`,`lock` FROM `users` WHERE `username` = ?;", USER);
        System.out.println("   id=" + r[0] + " activated=" + r[2] + " luong=" + r[3] + " lock=" + r[4]);
        System.out.println("   mat khau dung -> " + StringUtils.checkPassword(r[1], PASS));
        System.out.println("   mat khau sai  -> " + StringUtils.checkPassword(r[1], PASS + "x"));

        System.out.println("2) chan trung ten");
        System.out.println("   tim thay khi da ton tai -> "
                + (one("SELECT `id` FROM `users` WHERE `username` = ? LIMIT 1;", USER) != null));

        System.out.println("3) doi mat khau");
        String moi = "khac456";
        DbManager.executeUpdate("UPDATE `users` SET `password` = ? WHERE `username` = ? LIMIT 1;",
                StringUtils.genPass(moi), USER);
        r = one("SELECT `password` FROM `users` WHERE `username` = ?;", USER);
        System.out.println("   mat khau moi -> " + StringUtils.checkPassword(r[0], moi)
                + ", mat khau cu -> " + StringUtils.checkPassword(r[0], PASS));

        System.out.println("4) khoa / mo");
        DbManager.executeUpdate("UPDATE `users` SET `lock` = IF(`lock` = 1, 0, 1) WHERE `username` = ? LIMIT 1;", USER);
        String bat = one("SELECT `lock` FROM `users` WHERE `username` = ?;", USER)[0];
        DbManager.executeUpdate("UPDATE `users` SET `lock` = IF(`lock` = 1, 0, 1) WHERE `username` = ? LIMIT 1;", USER);
        String tat = one("SELECT `lock` FROM `users` WHERE `username` = ?;", USER)[0];
        System.out.println("   bat -> " + bat + ", tat -> " + tat);

        System.out.println("5) cau lenh danh sach cua man hinh");
        listAll("SELECT u.id, u.username, u.luong, u.status, u.`lock`, u.ban_until,"
                + " (SELECT GROUP_CONCAT(p.name) FROM players p WHERE p.user_id = u.id) AS chars,"
                + " (SELECT SUM(p.xu) FROM players p WHERE p.user_id = u.id) AS xu"
                + " FROM users u ORDER BY u.id DESC LIMIT 5;");

        DbManager.executeUpdate("DELETE FROM `users` WHERE `username` = ?;", USER);
        System.out.println("da xoa tai khoan tam, xong.");
        System.exit(0);
    }
}
