package com.nsoz.server;

import com.nsoz.map.Map;
import com.nsoz.map.MapManager;
import com.nsoz.model.Char;
import com.nsoz.util.NinjaUtils;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * Đưa nhân vật đang online về một toạ độ cố định.
 *
 * Đi đúng đường mà mục "Đi tới" trong menu quản trị đi: rời khu hiện tại, đặt toạ độ, rồi vào lại
 * khu của bản đồ đích. Tự dựng lấy thì máy khách không nhận được gói tin đổi bản đồ và người chơi
 * đứng lơ lửng ở khu cũ.
 *
 * Dùng khi có người kẹt trong hang, trong đấu trường, hoặc rơi vào bản đồ không có khu nào --
 * những chỗ mà tự thoát ra không được.
 */
public class TeleportAdmin extends JFrame {

    private final DefaultListModel<String> names = new DefaultListModel<>();
    private final JList<String> chars = new JList<>(names);
    private final JTextField mapId = new JTextField("22", 5);
    private final JTextField x = new JTextField("1741", 5);
    private final JTextField y = new JTextField("264", 5);
    private final JCheckBox useFirst = new JCheckBox("dùng điểm vào mặc định của map");
    private final JLabel status = new JLabel(" ");
    private List<Char> online = new ArrayList<>();

    public TeleportAdmin() {
        super("Đưa về toạ độ");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(520, 320);
        setLocationRelativeTo(null);

        chars.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        chars.addListSelectionListener(e -> showWhere());
        add(new JScrollPane(chars), BorderLayout.CENTER);

        JPanel form = new JPanel(new GridLayout(0, 1, 0, 4));
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row.add(new JLabel("Map:"));
        row.add(mapId);
        row.add(new JLabel("x:"));
        row.add(x);
        row.add(new JLabel("y:"));
        row.add(y);
        form.add(row);
        form.add(useFirst);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(button("Làm mới", e -> refresh()));
        buttons.add(button("Về Làng Tone", e -> preset(22, 1741, 264)));
        buttons.add(button("Đưa về toạ độ trên", e -> move()));
        buttons.add(button("Đưa tất cả về", e -> moveAll()));
        buttons.add(button("Đặt điểm lưu = map trên", e -> setSavePoint()));
        form.add(buttons);

        JPanel south = new JPanel(new BorderLayout());
        south.add(form, BorderLayout.CENTER);
        south.add(status, BorderLayout.SOUTH);
        add(south, BorderLayout.SOUTH);

        refresh();
    }

    public static void run() {
        java.awt.EventQueue.invokeLater(() -> new TeleportAdmin().setVisible(true));
    }

    private JButton button(String text, ActionListener listener) {
        JButton b = new JButton(text);
        b.addActionListener(listener);
        return b;
    }

    private void refresh() {
        String keep = chars.getSelectedValue();
        online = new ArrayList<>();
        names.clear();
        for (Char c : ServerManager.getChars()) {
            if (!TaskAdmin.alive(c) || !c.isHuman) {
                continue;
            }
            online.add(c);
            names.addElement(c.name + " -- map " + c.mapId + " (" + c.x + "," + c.y + ")");
        }
        for (int i = 0; i < names.size(); i++) {
            if (names.get(i).equals(keep)) {
                chars.setSelectedIndex(i);
                break;
            }
        }
        if (chars.getSelectedIndex() < 0 && !online.isEmpty()) {
            chars.setSelectedIndex(0);
        }
        status.setText(" " + online.size() + " nhân vật đang online");
    }

    private void showWhere() {
        Char c = selected();
        if (TaskAdmin.alive(c)) {
            status.setText(" " + c.name + " đang ở map " + c.mapId + " (" + c.x + "," + c.y + ")");
        }
    }

    private Char selected() {
        int i = chars.getSelectedIndex();
        return i < 0 || i >= online.size() ? null : online.get(i);
    }

    private void preset(int map, int px, int py) {
        mapId.setText(String.valueOf(map));
        x.setText(String.valueOf(px));
        y.setText(String.valueOf(py));
        useFirst.setSelected(false);
        move();
    }

    private void move() {
        Char c = selected();
        if (!TaskAdmin.alive(c)) {
            JOptionPane.showMessageDialog(this, "Chọn một nhân vật đang online.");
            return;
        }
        String why = send(c);
        refresh();
        status.setText(why != null ? " " + c.name + ": " + why
                : " đã đưa " + c.name + " về map " + c.mapId + " (" + c.x + "," + c.y + ")");
    }

    private void moveAll() {
        int done = 0;
        for (Char c : new ArrayList<>(online)) {
            if (TaskAdmin.alive(c) && send(c) == null) {
                done++;
            }
        }
        refresh();
        status.setText(" đã đưa " + done + " nhân vật về map " + mapId.getText().trim());
    }

    /**
     * Đặt điểm hồi sinh của nhân vật về map đang nhập.
     *
     * Đáng có vì lệnh auto nhiệm vụ hằng ngày của máy khách đòi đứng ở trường, mà xong việc nó
     * lại "về" -- tức về điểm lưu. Điểm lưu mặc định là Làng Tone nên vòng auto đứt ngay sau
     * nhiệm vụ đầu tiên. Trong game tự làm được bằng lệnh ltd, nhưng phải đi tới nơi mới lưu
     * được; ở đây đặt hộ được cho bất kỳ ai đang online.
     */
    private void setSavePoint() {
        Char c = selected();
        if (!TaskAdmin.alive(c)) {
            JOptionPane.showMessageDialog(this, "Chọn một nhân vật đang online.");
            return;
        }
        int map;
        try {
            map = Integer.parseInt(mapId.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "ID map phải là số.");
            return;
        }
        if (MapManager.getInstance().find(map) == null) {
            JOptionPane.showMessageDialog(this, "Không có map " + map + ".");
            return;
        }
        c.saveCoordinate = (short) map;
        status.setText(" điểm lưu của " + c.name + " giờ là map " + map
                + " -- ba trường là 1 Hirosaki, 27 Haruna, 72 Ookaza");
    }

    /** Trả về lý do nếu không đi được, null nếu xong. */
    private String send(Char c) {
        int map;
        try {
            map = Integer.parseInt(mapId.getText().trim());
        } catch (NumberFormatException ex) {
            return "ID map phải là số";
        }
        Map target = MapManager.getInstance().find(map);
        if (target == null) {
            return "không có map " + map;
        }
        int zone = NinjaUtils.randomZoneId(map);
        if (zone == -1) {
            // 62 trong 179 map không có khu tĩnh nào (đấu trường, hang động, địa đạo): khu của
            // chúng do hệ thống tạo lúc chạy, ném người vào đó là họ đứng ngoài mọi khu.
            return "map " + map + " không có khu nào để vào";
        }
        try {
            short[] xy;
            if (useFirst.isSelected()) {
                xy = NinjaUtils.getFirstPosition((short) map);
            } else {
                xy = new short[] { Short.parseShort(x.getText().trim()),
                                   Short.parseShort(y.getText().trim()) };
            }
            c.outZone();
            c.setXY(xy);
            target.joinZone(c, zone);
            return null;
        } catch (NumberFormatException ex) {
            return "x và y phải là số";
        } catch (Exception ex) {
            ex.printStackTrace();
            return "lỗi: " + ex;
        }
    }
}
