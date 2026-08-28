package com.nsoz.server;

import com.nsoz.item.Item;
import com.nsoz.model.AutoUseLogin;
import com.nsoz.model.Char;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Xem và dọn hành trang của người chơi đang online.
 *
 * Chỉ làm việc với người đang online: dữ liệu của họ nằm trong bộ nhớ máy chủ và bị ghi đè cả
 * hàng lúc đăng xuất, nên sửa thẳng vào cơ sở dữ liệu khi họ đang chơi là mất công.
 *
 * Danh sách id rác dùng chung một nguồn với phần tự dọn lúc đăng nhập -- cùng tệp autouse.properties,
 * cùng hàm đọc. Sửa ở đây là lần đăng nhập sau cũng theo, khỏi phải nhớ hai chỗ.
 */
public class BagAdmin extends JFrame {

    private static final String FILE = "autouse.properties";

    private final DefaultListModel<String> names = new DefaultListModel<>();
    private final JList<String> chars = new JList<>(names);
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[] { "Ô", "ID", "Tên", "Số lượng", "Nâng cấp", "Khoá" }, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private final JTextField junkIds = new JTextField(24);
    private final JLabel status = new JLabel(" ");
    private List<Char> online = new ArrayList<>();

    public BagAdmin() {
        super("Hành trang");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(760, 420);
        setLocationRelativeTo(null);

        chars.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        chars.addListSelectionListener(e -> showBag());
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setPreferredWidth(35);
        table.getColumnModel().getColumn(1).setPreferredWidth(45);
        table.getColumnModel().getColumn(2).setPreferredWidth(230);
        table.getColumnModel().getColumn(3).setPreferredWidth(60);
        table.getColumnModel().getColumn(4).setPreferredWidth(60);
        table.getColumnModel().getColumn(5).setPreferredWidth(45);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(chars), new JScrollPane(table));
        split.setDividerLocation(170);
        add(split, BorderLayout.CENTER);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(button("Làm mới", e -> refresh()));
        top.add(button("Xoá ô đang chọn", e -> removeSelected()));
        add(top, BorderLayout.NORTH);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottom.add(new JLabel("ID rác:"));
        bottom.add(junkIds);
        bottom.add(button("Dọn ngay", e -> dropNow()));
        bottom.add(button("Lưu danh sách", e -> saveJunk()));
        bottom.add(button("Dọn cho tất cả", e -> dropForEveryone()));

        JPanel south = new JPanel(new BorderLayout());
        south.add(bottom, BorderLayout.CENTER);
        south.add(status, BorderLayout.SOUTH);
        add(south, BorderLayout.SOUTH);

        junkIds.setText(join(AutoUseLogin.ids("drop")));
        refresh();
    }

    public static void run() {
        java.awt.EventQueue.invokeLater(() -> new BagAdmin().setVisible(true));
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
            names.addElement(c.name + " (cấp " + c.level + ")");
        }
        status.setText(" " + online.size() + " nhân vật đang online");
        for (int i = 0; i < names.size(); i++) {
            if (names.get(i).equals(keep)) {
                chars.setSelectedIndex(i);
                break;
            }
        }
        if (chars.getSelectedIndex() < 0 && !online.isEmpty()) {
            chars.setSelectedIndex(0);
        }
        showBag();
    }

    private Char selected() {
        int i = chars.getSelectedIndex();
        return i < 0 || i >= online.size() ? null : online.get(i);
    }

    private void showBag() {
        model.setRowCount(0);
        Char c = selected();
        if (!TaskAdmin.alive(c)) {
            return;
        }
        for (int i = 0; i < c.bag.length; i++) {
            Item item = c.bag[i];
            if (item == null) {
                continue;
            }
            model.addRow(new Object[] { i, item.id,
                    item.template != null ? item.template.name : "?",
                    item.getQuantity(),
                    item.upgrade > 0 ? "+" + item.upgrade : "",
                    item.isLock ? "khoá" : "" });
        }
        status.setText(" " + c.name + ": " + model.getRowCount() + "/" + c.bag.length + " ô đang dùng");
    }

    private void removeSelected() {
        Char c = selected();
        int row = table.getSelectedRow();
        if (!TaskAdmin.alive(c) || row < 0) {
            JOptionPane.showMessageDialog(this, "Chọn một nhân vật và một ô trong bảng.");
            return;
        }
        int slot = (Integer) model.getValueAt(row, 0);
        Item item = c.bag[slot];
        if (item == null) {
            refresh();
            return;
        }
        c.removeItem(slot, item.getQuantity(), true);
        showBag();
        status.setText(" đã xoá ô " + slot + " của " + c.name);
    }

    private void dropNow() {
        Char c = selected();
        if (!TaskAdmin.alive(c)) {
            JOptionPane.showMessageDialog(this, "Chọn một nhân vật đang online.");
            return;
        }
        int n = AutoUseLogin.dropAll(c, parse(junkIds.getText()));
        showBag();
        status.setText(" " + c.name + ": đã dọn " + n + " ô");
    }

    private void dropForEveryone() {
        List<Integer> junk = parse(junkIds.getText());
        int total = 0, people = 0;
        for (Char c : new ArrayList<>(online)) {
            if (!TaskAdmin.alive(c)) {
                continue;
            }
            int n = AutoUseLogin.dropAll(c, junk);
            if (n > 0) {
                people++;
                total += n;
            }
        }
        showBag();
        status.setText(" đã dọn " + total + " ô của " + people + " nhân vật");
    }

    /**
     * Ghi danh sách vào tệp cấu hình mà không đụng tới những dòng khác -- đọc bằng Properties rồi
     * ghi lại sẽ nuốt sạch chú thích, mà chú thích ở đó mới là thứ giải thích từng khoá làm gì.
     */
    private void saveJunk() {
        List<Integer> junk = parse(junkIds.getText());
        File file = new File(FILE);
        try {
            List<String> lines = file.exists()
                    ? new ArrayList<>(java.nio.file.Files.readAllLines(file.toPath()))
                    : new ArrayList<>();
            boolean replaced = false;
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).trim().startsWith("drop")) {
                    lines.set(i, "drop = " + join(junk));
                    replaced = true;
                    break;
                }
            }
            if (!replaced) {
                lines.add("");
                lines.add("# id vật phẩm tự xoá khỏi hành trang khi đăng nhập");
                lines.add("drop = " + join(junk));
            }
            StringBuilder out = new StringBuilder();
            for (String line : lines) {
                out.append(line).append('\n');
            }
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(out.toString().getBytes("UTF-8"));
            fos.close();
            status.setText(" đã lưu vào " + FILE + ": " + join(junk) + " -- áp dụng từ lần đăng nhập sau");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Không ghi được " + FILE + ":\n" + ex);
        }
    }

    private List<Integer> parse(String text) {
        List<Integer> ids = new ArrayList<>();
        for (String part : text.split("[,\\s]+")) {
            try {
                // id 0 là Đá cấp 1 nên phải nhận cả số 0, chỉ loại số âm.
                int id = Integer.parseInt(part.trim());
                if (id >= 0) {
                    ids.add(Integer.valueOf(id));
                }
            } catch (Exception ignored) {
            }
        }
        return ids;
    }

    private String join(List<Integer> ids) {
        StringBuilder sb = new StringBuilder();
        for (Integer id : ids) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(id);
        }
        return sb.toString();
    }
}
