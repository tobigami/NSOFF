package com.nsoz.server;

import com.nsoz.event.Event;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
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
 * Bật hoặc tắt sự kiện mà không phải sửa tệp cấu hình rồi khởi động lại.
 *
 * Máy chủ chọn sự kiện bằng đúng một dòng trong config.properties: game.event là TÊN LỚP, và
 * Event.init() nạp nó bằng Class.forName. Ở đây làm lại đúng ba bước mà Server.init() làm --
 * init, loadEventPoint, initStore -- nên đổi được ngay khi đang chạy.
 *
 * Ghi luôn xuống config.properties để lần khởi động sau vẫn giữ lựa chọn đó. Ghi bằng cách thay
 * đúng một dòng, không dùng Properties.store(), vì hàm đó nuốt sạch chú thích trong tệp.
 *
 * Ngày kết thúc của mỗi sự kiện nằm cứng trong mã (Halloween tới 2027, Noel tới 2088...), chính
 * tác giả cũng ghi chú là cứ để dài rồi tắt bằng cấu hình -- nên tắt ở đây là cách đúng.
 */
public class EventAdmin extends JFrame {

    private static final String CONFIG = "config.properties";
    private static final String KEY = "game.event";
    private static final String PACKAGE = "com.nsoz.event.";

    /** Tên lớp và tên hiển thị. Noevent là lớp rỗng sẵn có trong máy chủ, dùng để tắt. */
    private static final String[][] EVENTS = {
        { "Noevent", "Tắt sự kiện" },
        { "Halloween", "Halloween" },
        { "TrungThuNew", "Trung thu" },
        { "Noel", "Giáng sinh" },
        { "LunarNewYear", "Tết âm lịch" },
        { "CoHon", "Cô hồn" },
        { "He", "Hè" },
        { "KoroKing", "Koro King" },
        { "VietnameseWomensDay", "20/10" },
        { "InternationalWomensDay", "8/3" },
    };

    private final DefaultListModel<String> names = new DefaultListModel<>();
    private final JList<String> list = new JList<>(names);
    private final JTextArea info = new JTextArea();
    private final JLabel status = new JLabel(" ");

    public EventAdmin() {
        super("Sự kiện");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(560, 330);
        setLocationRelativeTo(null);

        for (String[] e : EVENTS) {
            names.addElement(e[1]);
        }
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        info.setEditable(false);

        add(new JScrollPane(list), BorderLayout.WEST);
        add(new JScrollPane(info), BorderLayout.CENTER);

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bar.add(button("Bật sự kiện đang chọn", new ActionListener() {
            public void actionPerformed(ActionEvent e) { apply(); }
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
                new EventAdmin().setVisible(true);
            }
        });
    }

    private JButton button(String text, ActionListener listener) {
        JButton b = new JButton(text);
        b.addActionListener(listener);
        return b;
    }

    private void refresh() {
        Event now = Event.getEvent();
        StringBuilder b = new StringBuilder();
        b.append("Đang chạy: ");
        if (now == null) {
            b.append("(không có sự kiện nào)\n");
        } else {
            b.append(now.getClass().getSimpleName()).append('\n');
            b.append("Còn hiệu lực: ").append(Event.isEvent() ? "có" : "đã hết hạn").append('\n');
        }
        b.append("\nCấu hình ").append(CONFIG).append(":\n  ").append(KEY).append(" = ")
         .append(readConfig()).append("\n\n");
        b.append("Đổi ở đây có hiệu lực ngay, không cần khởi động lại.\n");
        b.append("Người đang chơi có thể phải đổi map hoặc đăng nhập lại\n");
        b.append("mới thấy phần trang trí và NPC của sự kiện.");
        info.setText(b.toString());
        info.setCaretPosition(0);

        String current = readConfig();
        for (int i = 0; i < EVENTS.length; i++) {
            if (current.endsWith(EVENTS[i][0])) {
                list.setSelectedIndex(i);
                break;
            }
        }
        status.setText(" ");
    }

    private void apply() {
        int i = list.getSelectedIndex();
        if (i < 0) {
            JOptionPane.showMessageDialog(this, "Chọn một sự kiện trong danh sách.");
            return;
        }
        String cls = PACKAGE + EVENTS[i][0];
        try {
            // Đúng ba bước của Server.init(): nạp lớp, nạp mốc điểm, dựng cửa hàng sự kiện.
            Config.getInstance().setEvent(cls);
            Event.init();
            Event now = Event.getEvent();
            if (now == null) {
                status.setText(" không nạp được lớp " + cls);
                return;
            }
            now.loadEventPoint();
            now.initStore();
            writeConfig(cls);
            refresh();
            status.setText(" đã chuyển sang " + EVENTS[i][1] + " (" + EVENTS[i][0] + ")");
        } catch (Throwable ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Không đổi được sự kiện:\n" + ex);
        }
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

    private void writeConfig(String value) throws Exception {
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
