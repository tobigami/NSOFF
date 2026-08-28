package com.nsoz.server;

import com.nsoz.db.jdbc.DbManager;
import com.nsoz.model.User;
import com.nsoz.util.StringUtils;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Tạo và sửa tài khoản mà không phải gõ SQL.
 *
 * Mật khẩu phải băm bằng đúng hàm máy chủ dùng lúc đăng nhập (BCrypt qua StringUtils.genPass),
 * chèn chuỗi thô vào cột password thì đăng nhập luôn sai. Tên tài khoản đi theo đúng luật của
 * User.login(): chỉ chữ và số, và cột username là char(15) nên dài hơn là bị cắt âm thầm.
 *
 * Chỉ đụng vào bảng users. Nhân vật do người chơi tự tạo lúc vào game.
 *
 * Mọi câu truy vấn ở đây tự mở và tự đóng connection, không dùng DbManager.executeQuery():
 * hàm đó đóng cả statement lẫn connection ngay trước khi trả ResultSet về, nên đọc quá dòng
 * đầu là dính "Operation not allowed after ResultSet closed".
 */
public class AccountAdmin extends JFrame {

    private static final Pattern NAME = Pattern.compile("^[a-zA-Z0-9]+$");
    private static final int NAME_MAX = 15;
    /** Bằng mức máy chủ tự phát cho tài khoản mới trong User.newPlay(). */
    private static final int LUONG_MOI = 999;

    private final DefaultTableModel model = new DefaultTableModel(
            new Object[] { "ID", "Tài khoản", "Lượng", "Xu", "Trạng thái", "Nhân vật" }, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private final JTextField name = new JTextField(10);
    private final JPasswordField pass = new JPasswordField(10);
    private final JLabel status = new JLabel(" ");
    private List<Integer> ids = new ArrayList<>();

    public AccountAdmin() {
        super("Tài khoản");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(640, 380);
        setLocationRelativeTo(null);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setPreferredWidth(40);
        table.getColumnModel().getColumn(1).setPreferredWidth(130);
        table.getColumnModel().getColumn(2).setPreferredWidth(70);
        table.getColumnModel().getColumn(3).setPreferredWidth(90);
        table.getColumnModel().getColumn(4).setPreferredWidth(90);
        table.getColumnModel().getColumn(5).setPreferredWidth(160);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bar.add(new JLabel("Tài khoản:"));
        bar.add(name);
        bar.add(new JLabel("Mật khẩu:"));
        bar.add(pass);
        bar.add(button("Tạo mới", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                create();
            }
        }));
        bar.add(button("Đổi mật khẩu", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                changePassword();
            }
        }));
        bar.add(button("Khoá / Mở", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                toggleLock();
            }
        }));
        bar.add(button("Đá phiên kẹt", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                kick();
            }
        }));
        bar.add(button("Làm mới", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                refresh();
            }
        }));
        add(bar, BorderLayout.NORTH);
        add(status, BorderLayout.SOUTH);

        refresh();
    }

    public static void run() {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new AccountAdmin().setVisible(true);
            }
        });
    }

    private JButton button(String text, ActionListener listener) {
        JButton b = new JButton(text);
        b.addActionListener(listener);
        return b;
    }

    private void refresh() {
        ids = new ArrayList<>();
        model.setRowCount(0);
        Connection conn = null;
        try {
            conn = DbManager.getConnection();
            // GROUP_CONCAT để thấy ngay tài khoản nào đã có nhân vật, khỏi phải tra bảng khác.
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT u.id, u.username, u.luong, u.status, u.`lock`, u.ban_until,"
                    + " (SELECT GROUP_CONCAT(p.name) FROM players p WHERE p.user_id = u.id) AS chars,"
                    + " (SELECT SUM(p.xu) FROM players p WHERE p.user_id = u.id) AS xu"
                    + " FROM users u ORDER BY u.id DESC LIMIT 200;");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ids.add(rs.getInt("id"));
                String chars = rs.getString("chars");
                model.addRow(new Object[] {
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getLong("luong"),
                        rs.getObject("xu") == null ? 0L : rs.getLong("xu"),
                        state(rs),
                        chars == null ? "(chưa có)" : chars });
            }
            rs.close();
            ps.close();
            status.setText(" " + ids.size() + " tài khoản");
        } catch (Exception ex) {
            fail("Không đọc được danh sách tài khoản", ex);
        } finally {
            DbManager.closeConnection(conn);
        }
    }

    private String state(ResultSet rs) throws Exception {
        if (rs.getInt("lock") == 1) {
            return "khoá";
        }
        if (rs.getInt("status") == 1) {
            return "bị cấm";
        }
        if (rs.getTimestamp("ban_until") != null
                && rs.getTimestamp("ban_until").getTime() > System.currentTimeMillis()) {
            return "đang bị treo";
        }
        return "bình thường";
    }

    private void create() {
        String user = name.getText().trim();
        String password = new String(pass.getPassword());
        if (user.isEmpty() || password.isEmpty()) {
            warn("Nhập cả tên tài khoản lẫn mật khẩu.");
            return;
        }
        if (!NAME.matcher(user).matches()) {
            warn("Tên tài khoản chỉ được gồm chữ và số -- đây là luật của chính màn hình đăng nhập.");
            return;
        }
        if (user.length() > NAME_MAX) {
            warn("Tên tài khoản dài quá " + NAME_MAX + " ký tự, cơ sở dữ liệu sẽ cắt bớt.");
            return;
        }
        try {
            if (exists(user)) {
                warn("Tài khoản \"" + user + "\" đã tồn tại.");
                return;
            }
            DbManager.executeUpdate(
                    "INSERT INTO `users`(`username`, `password`, `activated`, `luong`) VALUES (?, ?, ?, ?);",
                    user, StringUtils.genPass(password), 1, LUONG_MOI);
            pass.setText("");
            refresh();
            status.setText(" đã tạo \"" + user + "\", lượng " + LUONG_MOI
                    + " -- vào game bấm \"Chơi mới\" để tạo nhân vật");
        } catch (Exception ex) {
            fail("Không tạo được tài khoản", ex);
        }
    }

    private void changePassword() {
        String user = selectedName();
        if (user == null) {
            return;
        }
        String password = new String(pass.getPassword());
        if (password.isEmpty()) {
            warn("Nhập mật khẩu mới vào ô Mật khẩu rồi bấm lại.");
            return;
        }
        try {
            DbManager.executeUpdate("UPDATE `users` SET `password` = ? WHERE `username` = ? LIMIT 1;",
                    StringUtils.genPass(password), user);
            pass.setText("");
            status.setText(" đã đổi mật khẩu cho \"" + user + "\"");
        } catch (Exception ex) {
            fail("Không đổi được mật khẩu", ex);
        }
    }

    private void toggleLock() {
        String user = selectedName();
        if (user == null) {
            return;
        }
        try {
            // Đảo tại chỗ trong SQL để khỏi phải tin vào giá trị đang hiện trên bảng, vốn là ảnh
            // chụp một thời điểm và có thể đã cũ.
            DbManager.executeUpdate(
                    "UPDATE `users` SET `lock` = IF(`lock` = 1, 0, 1) WHERE `username` = ? LIMIT 1;",
                    user);
            refresh();
            status.setText(" đã đổi trạng thái khoá của \"" + user + "\"");
        } catch (Exception ex) {
            fail("Không đổi được trạng thái khoá", ex);
        }
    }

    /**
     * Gỡ phiên còn kẹt trong bộ nhớ của một tài khoản.
     *
     * Máy chủ chỉ dọn phiên khi vòng đọc gói tin kết thúc, mà vòng đó chỉ kết thúc khi socket
     * thật sự đứt. Client sập hoặc mất mạng đột ngột thì socket vẫn mở phía máy chủ -- không có
     * TCP keepalive lẫn timeout đọc, cả hai đều bị chú thích trong Session (dòng 83-85). Phiên ma
     * đó khiến User.login() từ chối: "Tài khoản đã có người đăng nhập".
     *
     * Nút này làm đúng những việc lẽ ra closeMessage() làm -- lưu dữ liệu, gỡ khỏi danh sách,
     * đóng socket -- nên không phải khởi động lại máy chủ nữa.
     */
    private void kick() {
        String user = selectedName();
        if (user == null) {
            return;
        }
        int killed = 0;
        for (User u : ServerManager.getUsers()) {
            if (u == null || u.username == null || !u.username.equalsIgnoreCase(user)) {
                continue;
            }
            try {
                if (u.sltChar != null) {
                    // Lưu trước khi gỡ, nếu không thì tiến độ từ lần lưu cuối tới lúc sập bị mất.
                    try {
                        u.sltChar.saveData();
                    } catch (Exception ignored) {
                    }
                    ServerManager.removeChar(u.sltChar);
                }
                try {
                    u.saveData();
                } catch (Exception ignored) {
                }
                if (u.session != null) {
                    u.session.disconnect();
                }
            } catch (Exception ignored) {
            } finally {
                ServerManager.removeUser(u);
                killed++;
            }
        }
        // Bỏ mốc chờ đăng nhập lại, nếu không lần vào ngay sau đó bị chính máy chủ đóng phiên
        // mới (Session.java:437 đóng phiên khi mốc này còn hơn 4 giây).
        User.timeWaitLogin.remove(user);
        try {
            DbManager.executeUpdate("UPDATE `users` SET `online` = 0 WHERE `username` = ? LIMIT 1;", user);
            DbManager.executeUpdate("UPDATE `players` p JOIN `users` u ON u.id = p.user_id"
                    + " SET p.online = 0 WHERE u.username = ?;", user);
        } catch (Exception ignored) {
        }
        refresh();
        status.setText(killed > 0
                ? " đã gỡ " + killed + " phiên kẹt của \"" + user + "\", đăng nhập lại được ngay"
                : " \"" + user + "\" không có phiên nào trong bộ nhớ, đã dọn cờ online trong CSDL");
    }

    private boolean exists(String user) throws Exception {
        Connection conn = null;
        try {
            conn = DbManager.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT `id` FROM `users` WHERE `username` = ? LIMIT 1;");
            ps.setString(1, user);
            ResultSet rs = ps.executeQuery();
            boolean found = rs.next();
            rs.close();
            ps.close();
            return found;
        } finally {
            DbManager.closeConnection(conn);
        }
    }

    private String selectedName() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= ids.size()) {
            warn("Chọn một tài khoản trong bảng trước đã.");
            return null;
        }
        return String.valueOf(model.getValueAt(row, 1));
    }

    private void warn(String text) {
        JOptionPane.showMessageDialog(this, text);
    }

    private void fail(String text, Exception ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(this, text + ":\n" + ex);
    }
}
