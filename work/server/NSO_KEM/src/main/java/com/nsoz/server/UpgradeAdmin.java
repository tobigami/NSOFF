package com.nsoz.server;

import com.nsoz.item.Item;
import com.nsoz.model.Char;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * Nâng cấp tự động: chọn nhân vật, chọn món trong hành trang, đặt mức muốn tới rồi để máy chủ tự
 * bỏ đá và bảo hiểm cho tới khi đạt.
 *
 * Việc nặng chạy ở luồng riêng và nghỉ một nhịp giữa các lượt: mỗi lượt gửi vài gói tin xuống máy
 * khách, dồn một mạch thì màn hình trong game không kịp bắt và người chơi thấy kết quả nhảy cóc.
 */
public class UpgradeAdmin extends JFrame {

    private final JComboBox<String> who = new JComboBox<>();
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[] { "Ô", "Tên", "Cấp", "Hiện tại", "Tối đa" }, 0) {
        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private final JComboBox<String> target = new JComboBox<>();
    private final JCheckBox insurance = new JCheckBox("Tự dùng bảo hiểm", true);
    private final JCheckBox gold = new JCheckBox("Dùng lượng (tỉ lệ x1,5)", false);
    private final JTextField tries = new JTextField("50", 4);
    private final JTextArea log = new JTextArea(14, 70);
    private final JButton start = new JButton("Bắt đầu");

    private List<Integer> slots = new ArrayList<>();

    public UpgradeAdmin() {
        super("Nâng cấp tự động");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        for (int i = 1; i <= 16; i++) {
            target.addItem("+" + i);
        }
        target.setSelectedItem("+8");

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setMaxWidth(50);
        table.getColumnModel().getColumn(1).setPreferredWidth(240);
        table.getColumnModel().getColumn(2).setMaxWidth(60);
        table.getColumnModel().getColumn(3).setMaxWidth(80);
        table.getColumnModel().getColumn(4).setMaxWidth(80);

        log.setEditable(false);
        log.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Nhân vật:"));
        top.add(who);
        JButton reload = new JButton("Nạp lại");
        reload.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { reloadChars(); }
        });
        top.add(reload);
        who.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { reloadItems(); }
        });

        JPanel opts = new JPanel(new FlowLayout(FlowLayout.LEFT));
        opts.add(new JLabel("Nâng tới:"));
        opts.add(target);
        opts.add(insurance);
        opts.add(gold);
        opts.add(new JLabel(" Tối đa số lượt:"));
        opts.add(tries);
        start.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { begin(); }
        });
        opts.add(start);

        JPanel south = new JPanel(new BorderLayout());
        south.add(opts, BorderLayout.NORTH);
        south.add(new JScrollPane(log), BorderLayout.CENTER);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);

        reloadChars();
        setSize(760, 620);
        setLocationRelativeTo(null);
    }

    public static void run() {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new UpgradeAdmin().setVisible(true);
            }
        });
    }

    private void reloadChars() {
        String keep = (String) who.getSelectedItem();
        DefaultComboBoxModel<String> m = new DefaultComboBoxModel<>();
        for (Char c : ServerManager.getChars()) {
            if (TaskAdmin.alive(c) && c.isHuman) {
                m.addElement(c.name);
            }
        }
        who.setModel(m);
        if (keep != null) {
            who.setSelectedItem(keep);
        }
        reloadItems();
    }

    private Char current() {
        String name = (String) who.getSelectedItem();
        return name == null ? null : Char.findCharByName(name);
    }

    private void reloadItems() {
        slots = new ArrayList<>();
        model.setRowCount(0);
        Char c = current();
        if (!TaskAdmin.alive(c)) {
            return;
        }
        for (int i = 0; i < c.numberCellBag; i++) {
            Item it = c.bag[i];
            if (AutoUpgrade.canUpgrade(it)) {
                slots.add(Integer.valueOf(i));
                model.addRow(new Object[] { Integer.valueOf(i), it.template.name,
                        Short.valueOf(it.template.level), "+" + it.upgrade,
                        "+" + it.template.getUpMax() });
            }
        }
        if (model.getRowCount() > 0) {
            table.setRowSelectionInterval(0, 0);
        } else {
            append("Không có trang bị nâng cấp được trong hành trang."
                    + " Món đang mặc phải cởi ra bỏ vào túi mới nâng được.");
        }
    }

    private void append(final String s) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                log.append(s + "\n");
                log.setCaretPosition(log.getDocument().getLength());
            }
        });
    }

    private void begin() {
        final Char c = current();
        if (!TaskAdmin.alive(c)) {
            JOptionPane.showMessageDialog(this, "Nhân vật không online. Bấm \"Nạp lại\".");
            return;
        }
        int row = table.getSelectedRow();
        if (row < 0 || row >= slots.size()) {
            JOptionPane.showMessageDialog(this, "Hãy chọn một trang bị.");
            return;
        }
        final int slot = slots.get(row).intValue();
        final int want = target.getSelectedIndex() + 1;
        int n;
        try {
            n = Integer.parseInt(tries.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Số lượt phải là số.");
            return;
        }
        if (n <= 0) {
            JOptionPane.showMessageDialog(this, "Số lượt phải lớn hơn 0.");
            return;
        }
        final int maxTries = n;
        final boolean useIns = insurance.isSelected();
        final boolean useGold = gold.isSelected();

        start.setEnabled(false);
        append("--- " + c.name + ", ô " + slot + " -> +" + want
                + (useIns ? ", có bảo hiểm" : ", KHÔNG bảo hiểm (hỏng là tụt cấp)")
                + (useGold ? ", dùng lượng" : "") + " ---");

        new Thread(new Runnable() {
            public void run() {
                try {
                    AutoUpgrade.Result r = AutoUpgrade.run(c, slot, want, useIns, useGold, maxTries);
                    for (String line : r.log) {
                        append("  " + line);
                    }
                    append(String.format("=> +%d -> +%d sau %d lượt, dùng %d đá%s. Dừng: %s",
                            r.from, r.to, r.tries, r.stones,
                            r.insuranceUsed ? " và bảo hiểm" : "", r.stop));
                } catch (Throwable t) {
                    append("!! lỗi: " + t);
                } finally {
                    java.awt.EventQueue.invokeLater(new Runnable() {
                        public void run() {
                            start.setEnabled(true);
                            reloadItems();
                        }
                    });
                }
            }
        }, "auto-upgrade").start();
    }
}
