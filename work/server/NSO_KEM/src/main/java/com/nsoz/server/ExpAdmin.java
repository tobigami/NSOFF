package com.nsoz.server;

import com.nsoz.model.Char;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Đổi tỉ lệ kinh nghiệm của cả máy chủ khi đang chạy.
 *
 * Char.addExp nhân thẳng số nhận được với Config.getRateEXP() trước khi cộng, nên đổi giá trị đó
 * là mọi nguồn kinh nghiệm đều nhân theo: đánh quái, nhiệm vụ, hang động. Không phải sửa từng chỗ.
 *
 * Máy chủ vốn đã có hai đường đặt tỉ lệ, cả hai đều không dùng được lúc đang chơi:
 *
 *   - game.server.exp trong config.properties, chỉ đọc một lần lúc khởi động;
 *   - lệnh "setExp" của RequestHandler, nhưng phải gọi qua RMI từ trang quản trị ngoài.
 *
 * (Còn một đường thứ ba đọc bảng options key = 'expserver', nhưng lời gọi loadExp() trong
 * Zone.java đã bị chú thích nên đường đó chết -- đừng sửa bảng đó rồi ngồi chờ, không ăn thua.)
 *
 * Ở đây gọi thẳng setRateEXP nên có hiệu lực ngay lập tức, đồng thời ghi lại xuống
 * config.properties để lần khởi động sau vẫn giữ. Ghi bằng cách thay đúng một dòng chứ không
 * dùng Properties.store(), vì hàm đó nuốt sạch chú thích trong tệp.
 */
public class ExpAdmin extends JFrame {

    private static final String CONFIG = "config.properties";
    private static final String KEY = "game.server.exp";
    /** Trên mức này gần như chắc là gõ nhầm, hỏi lại cho chắc. */
    private static final int WARN_ABOVE = 10;

    private final JTextArea info = new JTextArea();
    private final JTextField custom = new JTextField("2", 4);
    private final JLabel status = new JLabel(" ");

    public ExpAdmin() {
        super("Kinh nghiệm");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(520, 300);
        setLocationRelativeTo(null);

        info.setEditable(false);
        add(new JScrollPane(info), BorderLayout.CENTER);

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        int[] quick = { 1, 2, 3, 5 };
        for (int i = 0; i < quick.length; i++) {
            final int rate = quick[i];
            bar.add(button(rate == 1 ? "Về x1" : "x" + rate, new ActionListener() {
                public void actionPerformed(ActionEvent e) { apply(rate); }
            }));
        }
        bar.add(new JLabel("  Tự nhập:"));
        bar.add(custom);
        bar.add(button("Đặt", new ActionListener() {
            public void actionPerformed(ActionEvent e) { applyCustom(); }
        }));
        bar.add(button("Làm mới", new ActionListener() {
            public void actionPerformed(ActionEvent e) { refresh(); }
        }));
        add(bar, BorderLayout.NORTH);
        add(status, BorderLayout.SOUTH);

        refresh();
    }

    public static void run() {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new ExpAdmin().setVisible(true);
            }
        });
    }

    private JButton button(String text, ActionListener listener) {
        JButton b = new JButton(text);
        b.addActionListener(listener);
        return b;
    }

    private void applyCustom() {
        int rate;
        try {
            rate = Integer.parseInt(custom.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Nhập một số nguyên, ví dụ 2.");
            return;
        }
        apply(rate);
    }

    private void apply(int rate) {
        if (rate < 1) {
            JOptionPane.showMessageDialog(this, "Tỉ lệ nhỏ nhất là 1 (x1 là bình thường).");
            return;
        }
        if (rate > WARN_ABOVE) {
            int ok = JOptionPane.showConfirmDialog(this,
                    "Đặt x" + rate + " kinh nghiệm cho cả máy chủ?\n"
                    + "Mức này lên cấp rất nhanh, mà cấp đã lên thì không hạ lại được.",
                    "Xác nhận", JOptionPane.YES_NO_OPTION);
            if (ok != JOptionPane.YES_OPTION) {
                return;
            }
        }
        try {
            int told = set(rate);
            refresh();
            status.setText(" đã đặt x" + rate + ", báo cho " + told + " người đang chơi");
        } catch (Throwable ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Không đặt được:\n" + ex);
        }
    }

    /**
     * Đặt tỉ lệ và ghi lại. Trả về số người đang chơi đã được báo.
     *
     * Cửa sổ này và lệnh chat "exp" dùng chung đúng hàm này, để hai đường không bao giờ lệch nhau.
     */
    public static int set(int rate) throws Exception {
        Config.getInstance().setRateEXP(rate);
        writeConfig(String.valueOf(rate));
        return announce(rate);
    }

    /** Báo cho người đang chơi, vì kinh nghiệm nhận được đổi ngay mà màn hình không hiện gì. */
    private static int announce(int rate) {
        String text = rate > 1
                ? "Máy chủ đang x" + rate + " kinh nghiệm."
                : "Kinh nghiệm đã về mức bình thường.";
        int told = 0;
        for (Char c : ServerManager.getChars()) {
            // isCleaned/bag == null là nhân vật đã thoát nhưng vỏ chưa dọn xong.
            if (c == null || !c.isHuman || c.isCleaned || c.bag == null) {
                continue;
            }
            try {
                c.serverMessage(text);
                told++;
            } catch (Exception ignored) {
            }
        }
        return told;
    }

    private void refresh() {
        StringBuilder b = new StringBuilder();
        b.append("Đang chạy: x").append(Config.getInstance().getRateEXP()).append(" kinh nghiệm\n");
        b.append("Trong ").append(CONFIG).append(": ").append(KEY).append(" = ")
         .append(readConfig()).append("\n\n");
        b.append("Đổi ở đây có hiệu lực ngay với mọi người đang chơi, không cần\n");
        b.append("khởi động lại, và được ghi lại nên lần bật máy chủ sau vẫn giữ.\n\n");
        b.append("Nhân vào mọi nguồn kinh nghiệm: đánh quái, nhiệm vụ, hang động.\n");
        b.append("Không đụng tới tiền, vật phẩm rơi hay điểm tiềm năng.\n\n");
        b.append("Người mới đăng nhập cũng được báo sẵn trong tin nhắn chào,\n");
        b.append("phần đó máy chủ tự lo khi tỉ lệ lớn hơn 1.");
        info.setText(b.toString());
        info.setCaretPosition(0);
        custom.setText(String.valueOf(Config.getInstance().getRateEXP()));
    }

    private String readConfig() {
        try {
            for (String line : Files.readAllLines(new File(CONFIG).toPath(), StandardCharsets.UTF_8)) {
                if (line.trim().startsWith(KEY)) {
                    int at = line.indexOf('=');
                    return at < 0 ? line.trim() : line.substring(at + 1).trim();
                }
            }
        } catch (Exception ignored) {
        }
        return "(không đọc được)";
    }

    private static void writeConfig(String value) throws Exception {
        File file = new File(CONFIG);
        List<String> lines = new ArrayList<>(Files.readAllLines(file.toPath(), StandardCharsets.UTF_8));
        boolean done = false;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).trim().startsWith(KEY)) {
                lines.set(i, KEY + "=" + value);
                done = true;
                break;
            }
        }
        if (!done) {
            lines.add(KEY + "=" + value);
        }
        StringBuilder out = new StringBuilder();
        for (String line : lines) {
            out.append(line).append('\n');
        }
        Files.write(file.toPath(), out.toString().getBytes(StandardCharsets.UTF_8));
    }
}
