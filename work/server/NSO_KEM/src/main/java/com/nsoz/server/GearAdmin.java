package com.nsoz.server;

import com.nsoz.convert.Converter;
import com.nsoz.item.Item;
import com.nsoz.item.ItemTemplate;
import com.nsoz.model.Char;
import com.nsoz.store.ItemStore;
import com.nsoz.store.Store;
import com.nsoz.store.StoreManager;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * Màn hình chọn nhân vật rồi mặc bộ trang bị tốt nhất theo cấp.
 *
 * Phần chọn món và mặc nằm ở BestGear, dùng chung với lệnh chat "gear" -- ở đây chỉ lo giao diện.
 */
public class GearAdmin extends JFrame {


    private final DefaultListModel<String> names = new DefaultListModel<>();
    private final JList<String> chars = new JList<>(names);
    private final JTextField level = new JTextField(4);
    private final JComboBox<String> sys = new JComboBox<>(
            new String[] { "hệ 0", "hệ 1", "hệ 2", "hệ 3" });
    private final JCheckBox replaceWeaker = new JCheckBox("thay cả món đang mặc nếu yếu hơn", true);
    /** Bộ chỉ số của menu NPC Vua Hùng: +24, tinh luyện cộng dồn 9 bậc, ghép thêm chỉ số. */
    private final JCheckBox boost = new JCheckBox("chỉ số kiểu Vua Hùng (mạnh bất thường)", false);
    private final JTextArea log = new JTextArea();
    private List<Char> online = new ArrayList<>();

    public GearAdmin() {
        super("Mặc đồ tốt nhất");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(720, 430);
        setLocationRelativeTo(null);

        chars.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        chars.addListSelectionListener(e -> showChar());
        log.setEditable(false);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(chars), new JScrollPane(log));
        split.setDividerLocation(190);
        add(split, BorderLayout.CENTER);

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bar.add(button("Làm mới", e -> refresh()));
        bar.add(new JLabel("   Cấp đồ:"));
        bar.add(level);
        bar.add(sys);
        bar.add(replaceWeaker);
        bar.add(boost);
        bar.add(button("Mặc đồ tốt nhất", e -> dress()));
        add(bar, BorderLayout.NORTH);

        refresh();
    }

    public static void run() {
        java.awt.EventQueue.invokeLater(() -> new GearAdmin().setVisible(true));
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
        for (int i = 0; i < names.size(); i++) {
            if (names.get(i).equals(keep)) {
                chars.setSelectedIndex(i);
                break;
            }
        }
        if (chars.getSelectedIndex() < 0 && !online.isEmpty()) {
            chars.setSelectedIndex(0);
        }
        showChar();
    }

    private Char selected() {
        int i = chars.getSelectedIndex();
        return i < 0 || i >= online.size() ? null : online.get(i);
    }

    /** Liệt kê đồ đang mặc để so trước sau. */
    private void showChar() {
        Char c = selected();
        if (!TaskAdmin.alive(c)) {
            return;
        }
        level.setText(String.valueOf(c.level));
        sys.setSelectedIndex(Math.max(0, Math.min(3, c.getSys())));
        StringBuilder sb = new StringBuilder(c.name + " -- cấp " + c.level
                + ", hệ " + c.getSys() + ", phái " + c.classId + "\n\nĐang mặc:\n");
        for (int i = 0; i < BestGear.SLOT.length; i++) {
            Item worn = c.equipment[BestGear.SLOT[i]] == null ? null
                    : Converter.getInstance().toItem(c.equipment[BestGear.SLOT[i]]);
            sb.append(String.format("  %-11s %s%n", BestGear.SLOT_NAME[i], BestGear.describe(worn)));
        }
        log.setText(sb.toString());
        log.setCaretPosition(0);
    }


    private void dress() {
        Char c = selected();
        if (!TaskAdmin.alive(c)) {
            JOptionPane.showMessageDialog(this, "Chọn một nhân vật đang online.");
            return;
        }
        int want;
        try {
            want = Integer.parseInt(level.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Cấp đồ phải là số.");
            return;
        }
        byte wantSys = (byte) sys.getSelectedIndex();

        String report = BestGear.dress(c, want, wantSys, replaceWeaker.isSelected(),
                boost.isSelected());
        log.setText("Mặc đồ cho " + c.name + " theo cấp " + want + ":\n\n" + report);
        log.setCaretPosition(0);
    }

}
