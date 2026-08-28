package com.nsoz.server;

import com.nsoz.model.Char;
import com.nsoz.task.Task;
import com.nsoz.task.TaskTemplate;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Nhiệm vụ chính của các nhân vật đang online: xem đang ở đâu và bỏ qua khi bị kẹt.
 *
 * Việc bỏ qua đi bằng đúng đường mà vật phẩm "Lệnh bài hoàn thành" đi (Char.useItem ->
 * finishTask(true)), nên phần thưởng, vật phẩm mở đầu và các gói tin gửi xuống máy khách đều giống
 * hệt lúc chơi thật -- người chơi thấy nhiệm vụ đổi ngay, không phải đăng nhập lại. Char.finishTask
 * là private nên phải gọi qua reflection; viết lại Char.java chỉ để đổi một từ khoá thì tệ hơn
 * nhiều, vì file đó 25 nghìn dòng và không nằm trong phần đang được biên dịch lại.
 *
 * Chỉ làm việc với nhân vật đang online: dữ liệu của người đang chơi nằm trong bộ nhớ máy chủ, và
 * máy chủ ghi đè cả hàng trong cơ sở dữ liệu lúc thoát -- sửa thẳng vào cơ sở dữ liệu khi họ đang
 * online thì thay đổi sẽ bị mất.
 */
public class TaskAdmin extends JFrame {

    /** Chặn vòng lặp "nhảy tới" chạy mãi nếu có nhiệm vụ không chịu tiến. */
    private static final int MAX_SKIP = 200;

    private final DefaultTableModel model = new DefaultTableModel(
            new Object[] { "Tên", "Cấp", "ID", "Nhiệm vụ", "Bước", "Việc phải làm", "Tiến độ" }, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private final JTextField target = new JTextField(4);
    private final JLabel status = new JLabel(" ");
    private List<Char> rows = new ArrayList<>();

    public TaskAdmin() {
        super("Quản lý nhiệm vụ");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(900, 320);
        setLocationRelativeTo(null);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setPreferredWidth(110);
        table.getColumnModel().getColumn(1).setPreferredWidth(40);
        table.getColumnModel().getColumn(2).setPreferredWidth(40);
        table.getColumnModel().getColumn(3).setPreferredWidth(200);
        table.getColumnModel().getColumn(4).setPreferredWidth(50);
        table.getColumnModel().getColumn(5).setPreferredWidth(320);
        table.getColumnModel().getColumn(6).setPreferredWidth(70);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bar.add(button("Làm mới", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                refresh();
            }
        }));
        bar.add(button("Bỏ qua bước", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                skipStep();
            }
        }));
        bar.add(button("Bỏ qua nhiệm vụ", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                skipTask();
            }
        }));
        bar.add(new JLabel("   Nhảy tới nhiệm vụ ID:"));
        bar.add(target);
        bar.add(button("Nhảy", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jumpTo();
            }
        }));
        add(bar, BorderLayout.NORTH);
        add(status, BorderLayout.SOUTH);

        refresh();
    }

    public static void run() {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new TaskAdmin().setVisible(true);
            }
        });
    }

    private JButton button(String text, ActionListener listener) {
        JButton b = new JButton(text);
        b.addActionListener(listener);
        return b;
    }

    private void refresh() {
        String keep = selectedName();
        rows = new ArrayList<>();
        model.setRowCount(0);
        for (Char c : ServerManager.getChars()) {
            if (!alive(c) || !c.isHuman) {
                continue;
            }
            rows.add(c);
            model.addRow(describe(c));
        }
        status.setText(" " + rows.size() + " nhân vật đang online");
        if (keep != null) {
            for (int i = 0; i < rows.size(); i++) {
                if (keep.equals(rows.get(i).name)) {
                    table.setRowSelectionInterval(i, i);
                    break;
                }
            }
        }
    }

    private Object[] describe(Char c) {
        Task task = c.taskMain;
        // taskMain rỗng nghĩa là vừa xong nhiệm vụ trước và chưa gặp NPC để nhận nhiệm vụ kế; vẫn
        // tra được tên qua taskId để biết đang chờ cái gì.
        TaskTemplate t = task != null ? task.template : Task.getTaskTemplate(c.taskId);
        if (t == null) {
            return new Object[] { c.name, c.level, c.taskId, "(không có mẫu nhiệm vụ)", "-", "-", "-" };
        }
        String[] subs = t.getSubNames();
        short[] counts = t.getCounts();
        if (task == null) {
            return new Object[] { c.name, c.level, c.taskId, t.getName(), "-",
                    "chưa nhận -- cần gặp NPC", "-" };
        }
        int i = task.index;
        String sub = i >= 0 && i < subs.length ? subs[i] : "?";
        String need = i >= 0 && i < counts.length && counts[i] > 0
                ? task.count + "/" + counts[i]
                : String.valueOf(task.count);
        return new Object[] { c.name, c.level, c.taskId, t.getName(),
                (i + 1) + "/" + subs.length, sub, need };
    }

    private String selectedName() {
        int row = table.getSelectedRow();
        return row < 0 || row >= rows.size() ? null : rows.get(row).name;
    }

    private Char selected() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= rows.size()) {
            JOptionPane.showMessageDialog(this, "Hãy chọn một nhân vật trong bảng.");
            return null;
        }
        Char c = rows.get(row);
        // Bảng là ảnh chụp một thời điểm. Người chơi thoát ra trong lúc cửa sổ còn mở thì máy chủ
        // dọn nhân vật và đặt bag = null; đụng vào là đổ NullPointerException tận trong Char.
        if (!alive(c)) {
            JOptionPane.showMessageDialog(this,
                    c.name + " đã thoát game. Bấm \"Làm mới\" rồi thử lại.");
            refresh();
            return null;
        }
        return c;
    }

    /** Nhân vật còn thực sự trong game hay chỉ là vỏ đã bị dọn. */
    static boolean alive(Char c) {
        return c != null && !c.isCleaned && c.bag != null;
    }

    private void skipStep() {
        Char c = selected();
        if (c == null) {
            return;
        }
        if (c.taskMain == null) {
            JOptionPane.showMessageDialog(this, c.name + " chưa nhận nhiệm vụ, không có bước để bỏ qua.");
            return;
        }
        if (c.taskMain.isComplete()) {
            JOptionPane.showMessageDialog(this,
                    "Đây là bước cuối rồi, dùng \"Bỏ qua nhiệm vụ\" để kết thúc.");
            return;
        }
        c.taskNext();
        refresh();
        status.setText(" " + c.name + ": đã bỏ qua một bước");
    }

    private void skipTask() {
        Char c = selected();
        if (c == null) {
            return;
        }
        int before = c.taskId;
        String why = advance(c);
        refresh();
        status.setText(why != null
                ? " " + c.name + ": " + why
                : " " + c.name + ": nhiệm vụ " + before + " -> " + c.taskId);
    }

    private void jumpTo() {
        Char c = selected();
        if (c == null) {
            return;
        }
        int want;
        try {
            want = Integer.parseInt(target.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "ID nhiệm vụ phải là số.");
            return;
        }
        if (want <= c.taskId) {
            JOptionPane.showMessageDialog(this,
                    c.name + " đang ở nhiệm vụ " + c.taskId + ", chỉ nhảy tới được nhiệm vụ lớn hơn.");
            return;
        }
        int before = c.taskId;
        int steps = 0;
        String why = null;
        while (c.taskId < want && steps < MAX_SKIP) {
            int at = c.taskId;
            why = advance(c);
            if (why != null) {
                break;
            }
            if (c.taskId == at) {
                why = "nhiệm vụ " + at + " không tiến thêm được";
                break;
            }
            steps++;
        }
        refresh();
        status.setText(" " + c.name + ": " + before + " -> " + c.taskId
                + " (" + steps + " nhiệm vụ)" + (why != null ? " -- dừng vì " + why : ""));
    }

    /**
     * Kết thúc nhiệm vụ hiện tại rồi nhận luôn nhiệm vụ kế. Trả về lý do nếu không đi được, null
     * nếu xong.
     */
    private String advance(Char c) {
        if (!alive(c)) {
            return "nhân vật đã thoát game";
        }
        if (c.taskMain == null) {
            // Nhận trước đã, vì finishTask() chỉ tăng taskId khi đang có nhiệm vụ trong tay --
            // gọi lúc rỗng thì phần thưởng vẫn phát mà nhiệm vụ đứng yên.
            c.takingTask();
            if (c.taskMain == null) {
                return "không nhận được nhiệm vụ " + c.taskId + " (hết bảng nhiệm vụ?)";
            }
        }
        int before = c.taskId;
        try {
            Method finish = Char.class.getDeclaredMethod("finishTask", boolean.class);
            finish.setAccessible(true);
            finish.invoke(c, Boolean.TRUE);
        } catch (Exception ex) {
            ex.printStackTrace();
            return "lỗi khi kết thúc nhiệm vụ: " + ex;
        }
        if (c.taskId == before) {
            // finishTask() quay ra sớm khi hành trang đầy, phần thưởng nào cũng cần chỗ chứa.
            return "hành trang đầy, hãy dọn bớt rồi thử lại";
        }
        c.takingTask();
        return null;
    }
}
