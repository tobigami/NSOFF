package com.nsoz.server;

import com.nsoz.convert.Converter;
import com.nsoz.item.Item;
import com.nsoz.item.ItemFactory;
import com.nsoz.item.ItemManager;
import com.nsoz.item.ItemTemplate;
import com.nsoz.model.Char;
import com.nsoz.option.ItemOption;
import com.nsoz.store.ItemStore;
import com.nsoz.store.Store;
import com.nsoz.store.StoreManager;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gửi vật phẩm cho nhân vật: tìm theo tên, xem trước ảnh và chỉ số, rồi gửi.
 *
 * Cửa sổ "Gửi Đồ" cũ bắt gõ đúng id -- mà id thì nằm trong cơ sở dữ liệu, không tra được lúc đang
 * dùng. Ở đây danh sách vật phẩm lấy thẳng từ ItemManager (chính bộ máy chủ đang chạy), lọc theo
 * tên không dấu, kèm ảnh đọc từ Data/Img/Small.
 *
 * Đồ mặc được thì chỉ số không nằm trong vật phẩm mà nằm ở bảng cửa hàng: mỗi dòng store_data ghi
 * mức TỐI ĐA, còn mức sàn do ItemStore tự trừ ra. Nên vật phẩm dựng qua đúng đường máy chủ dùng --
 * Converter.toItem(itemStore, MAX/MIN/RANDOM) rồi Item.next(nâng cấp) -- chứ không tự chế chỉ số,
 * để món gửi đi không khác gì món nhặt được trong game.
 */
public class SendItemAdmin extends JFrame {

    /** Ảnh nằm ở Data/Img/Small/<mức phóng>/Small<icon>.png; mức 4 to nhất, thiếu thì lùi dần. */
    private static final int[] ZOOMS = { 4, 3, 2, 1 };
    private static final int CELL = 28;
    private static final int PREVIEW = 96;
    /** Nâng cấp cao nhất trong game: dòng chỉ số "(+12)" chỉ mở ở mức này. */
    private static final int MAX_UPGRADE = 12;

    private static final String[] TYPE_NAME = new String[40];
    static {
        for (int i = 0; i < TYPE_NAME.length; i++) {
            TYPE_NAME[i] = "loại " + i;
        }
        put(ItemTemplate.TYPE_NON, "Nón");
        put(ItemTemplate.TYPE_VUKHI, "Vũ khí");
        put(ItemTemplate.TYPE_AO, "Áo");
        put(ItemTemplate.TYPE_LIEN, "Liên");
        put(ItemTemplate.TYPE_GANGTAY, "Găng tay");
        put(ItemTemplate.TYPE_NHAN, "Nhẫn");
        put(ItemTemplate.TYPE_QUAN, "Quần");
        put(ItemTemplate.TYPE_NGOCBOI, "Ngọc bội");
        put(ItemTemplate.TYPE_GIAY, "Giày");
        put(ItemTemplate.TYPE_PHU, "Phù");
        put(ItemTemplate.TYPE_THUNUOI, "Thú nuôi");
        put(ItemTemplate.TYPE_MATNA, "Mặt nạ");
        put(ItemTemplate.TYPE_AOCHOANG, "Áo choàng");
        put(ItemTemplate.TYPE_BAOTAY, "Bao tay");
        put(ItemTemplate.TYPE_MATTHAN, "Mắt thần");
        put(ItemTemplate.TYPE_BIKIP, "Bí kíp");
        put(ItemTemplate.TYPE_HP, "Bình HP");
        put(ItemTemplate.TYPE_MP, "Bình MP");
        put(ItemTemplate.TYPE_EAT, "Thức ăn");
        put(ItemTemplate.TYPE_MONEY, "Tiền");
        put(ItemTemplate.TYPE_TUI_TIEN, "Túi tiền");
        put(ItemTemplate.TYPE_MEAT, "Thịt");
        put(ItemTemplate.TYPE_DRAGONBALL, "Ngọc rồng");
        put(ItemTemplate.TYPE_TASK_SAVE, "Vật phẩm nhiệm vụ");
        put(ItemTemplate.TYPE_TASK_WAIT, "Vật phẩm nhiệm vụ");
        put(ItemTemplate.TYPE_TASK, "Vật phẩm nhiệm vụ");
        put(ItemTemplate.TYPE_CRYSTAL, "Huyền tinh");
        put(ItemTemplate.TYPE_ORDER, "Khác");
        put(ItemTemplate.TYPE_PROTECT, "Bảo hiểm");
        put(ItemTemplate.TYPE_NGOC_KHAM, "Ngọc khảm");
    }

    private static void put(int type, String name) {
        if (type >= 0 && type < TYPE_NAME.length) {
            TYPE_NAME[type] = name;
        }
    }

    private final Map<Integer, ImageIcon> iconCache = new HashMap<>();
    private final List<ItemTemplate> all = new ArrayList<>();
    private List<ItemTemplate> shown = new ArrayList<>();

    private final JTextField search = new JTextField(18);
    private final JComboBox<String> typeFilter = new JComboBox<>();
    /**
     * Lọc đồ thời trang -- ô "Tbi 2" trong game.
     *
     * Không phân biệt được bằng cột type: Áo Kurama và Áo Thố đều là type 2. Thứ quyết định là
     * cột fashion, mang id ảnh thời trang khi món đó là đồ thời trang và -1 khi không phải.
     */
    private final JCheckBox onlyFashion = new JCheckBox("Chỉ đồ thời trang (Tbi 2)");
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[] { "", "ID", "Tên", "Loại", "Cấp", "Ô" }, 0) {
        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }
        @Override
        public Class<?> getColumnClass(int c) {
            return c == 0 ? ImageIcon.class : Object.class;
        }
    };
    private final JTable table = new JTable(model);

    private final JLabel bigIcon = new JLabel("", SwingConstants.CENTER);
    private final JLabel title = new JLabel(" ");
    private final JTextArea detail = new JTextArea(12, 28);

    private final JComboBox<String> who = new JComboBox<>();
    private final JTextField quantity = new JTextField("1", 5);
    private final JComboBox<String> upgrade = new JComboBox<>();
    private final JComboBox<String> sys = new JComboBox<>();
    private final JComboBox<String> mode = new JComboBox<>(
            new String[] { "Tối đa", "Ngẫu nhiên", "Tối thiểu" });
    private final JCheckBox lock = new JCheckBox("Khoá", true);
    private final JTextField xu = new JTextField("0", 8);
    private final JTextField yen = new JTextField("0", 8);
    private final JTextField luong = new JTextField("0", 7);
    private final JLabel status = new JLabel(" ");

    public SendItemAdmin() {
        super("Gửi đồ cho nhân vật");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        loadItems();

        for (int i = 0; i <= MAX_UPGRADE; i++) {
            upgrade.addItem("+" + i);
        }

        typeFilter.addItem("Tất cả");
        boolean[] seen = new boolean[TYPE_NAME.length];
        for (ItemTemplate t : all) {
            if (t.type >= 0 && t.type < seen.length && !seen[t.type]) {
                seen[t.type] = true;
                typeFilter.addItem(TYPE_NAME[t.type]);
            }
        }

        table.setRowHeight(CELL + 4);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setMaxWidth(CELL + 10);
        table.getColumnModel().getColumn(1).setMaxWidth(60);
        table.getColumnModel().getColumn(2).setPreferredWidth(240);
        table.getColumnModel().getColumn(3).setPreferredWidth(110);
        table.getColumnModel().getColumn(4).setMaxWidth(50);
        table.getColumnModel().getColumn(5).setMaxWidth(55);
        table.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                    boolean foc, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, null, sel, foc, row, col);
                l.setHorizontalAlignment(CENTER);
                l.setIcon(row < shown.size() ? icon(shown.get(row).icon, CELL) : null);
                return l;
            }
        });
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    showDetail();
                }
            }
        });

        DocumentListener refilter = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filter(); }
            public void removeUpdate(DocumentEvent e) { filter(); }
            public void changedUpdate(DocumentEvent e) { filter(); }
        };
        search.getDocument().addDocumentListener(refilter);
        typeFilter.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { filter(); }
        });
        onlyFashion.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { filter(); }
        });

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Tìm theo tên:"));
        top.add(search);
        top.add(new JLabel("   Loại:"));
        top.add(typeFilter);
        top.add(onlyFashion);
        JButton reload = new JButton("Nạp lại nhân vật");
        reload.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { reloadChars(); }
        });
        top.add(reload);

        bigIcon.setPreferredSize(new Dimension(PREVIEW + 16, PREVIEW + 16));
        bigIcon.setBorder(BorderFactory.createEtchedBorder());
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
        detail.setEditable(false);
        detail.setLineWrap(true);
        detail.setWrapStyleWord(true);
        detail.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));

        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        right.add(bigIcon);
        right.add(title);
        right.add(new JScrollPane(detail));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(table), right);
        split.setDividerLocation(520);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottom.add(new JLabel("Gửi cho:"));
        bottom.add(who);
        bottom.add(new JLabel(" Số lượng:"));
        bottom.add(quantity);
        bottom.add(new JLabel(" Nâng cấp:"));
        bottom.add(upgrade);
        bottom.add(new JLabel(" Hệ:"));
        bottom.add(sys);
        bottom.add(new JLabel(" Chỉ số:"));
        bottom.add(mode);
        bottom.add(lock);
        JButton send = new JButton("Gửi");
        send.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { send(); }
        });
        bottom.add(send);

        JPanel money = new JPanel(new FlowLayout(FlowLayout.LEFT));
        money.add(new JLabel("Kèm tiền —  Xu:"));
        money.add(xu);
        money.add(new JLabel(" Yên:"));
        money.add(yen);
        money.add(new JLabel(" Lượng:"));
        money.add(luong);
        JButton sendMoney = new JButton("Chỉ gửi tiền");
        sendMoney.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { sendMoney(); }
        });
        money.add(sendMoney);

        JPanel south = new JPanel(new BorderLayout());
        south.add(bottom, BorderLayout.NORTH);
        south.add(money, BorderLayout.CENTER);
        south.add(status, BorderLayout.SOUTH);

        add(top, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);

        mode.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { showDetail(); }
        });
        sys.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { showDetail(); }
        });
        upgrade.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { showDetail(); }
        });

        reloadChars();
        filter();
        setSize(1080, 620);
        setLocationRelativeTo(null);
    }

    public static void run() {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new SendItemAdmin().setVisible(true);
            }
        });
    }

    // ------------------------------------------------------------------ dữ liệu

    private void loadItems() {
        // ItemManager không có hàm đếm, nên duyệt tới khi hết chỉ mục.
        for (int i = 0; ; i++) {
            ItemTemplate t;
            try {
                t = ItemManager.getInstance().getItemTemplate(i);
            } catch (IndexOutOfBoundsException stop) {
                break;
            }
            if (t != null && t.name != null) {
                all.add(t);
            }
        }
    }

    private void reloadChars() {
        String keep = (String) who.getSelectedItem();
        DefaultComboBoxModel<String> m = new DefaultComboBoxModel<>();
        for (Char c : ServerManager.getChars()) {
            if (c != null && !c.isCleaned && c.isHuman) {
                m.addElement(c.name);
            }
        }
        who.setModel(m);
        if (keep != null) {
            who.setSelectedItem(keep);
        }
        status.setText(" " + m.getSize() + " nhân vật đang online");
    }

    /** Bỏ dấu để gõ "ma sau" cũng ra "Mạc Sầu Tiêu". */
    private static String plain(String s) {
        String n = Normalizer.normalize(s, Normalizer.Form.NFD);
        StringBuilder b = new StringBuilder(n.length());
        for (int i = 0; i < n.length(); i++) {
            char c = n.charAt(i);
            if (Character.getType(c) != Character.NON_SPACING_MARK) {
                b.append(c == 'đ' || c == 'Đ' ? 'd' : Character.toLowerCase(c));
            }
        }
        return b.toString();
    }

    private void filter() {
        String q = plain(search.getText().trim());
        String want = (String) typeFilter.getSelectedItem();
        shown = new ArrayList<>();
        model.setRowCount(0);
        for (ItemTemplate t : all) {
            String tn = t.type >= 0 && t.type < TYPE_NAME.length ? TYPE_NAME[t.type] : "?";
            if (want != null && !"Tất cả".equals(want) && !want.equals(tn)) {
                continue;
            }
            if (onlyFashion.isSelected() && t.fashion == -1) {
                continue;
            }
            if (!q.isEmpty() && !plain(t.name).contains(q) && !String.valueOf(t.id).equals(q)) {
                continue;
            }
            shown.add(t);
            model.addRow(new Object[] { null, t.id, t.name, tn, t.level,
                    t.fashion == -1 ? "Tbi 1" : "Tbi 2" });
        }
        if (!shown.isEmpty()) {
            table.setRowSelectionInterval(0, 0);
        } else {
            showDetail();
        }
    }

    private ItemTemplate selected() {
        int r = table.getSelectedRow();
        return r < 0 || r >= shown.size() ? null : shown.get(r);
    }

    /** Các dòng cửa hàng của vật phẩm này -- mỗi dòng là một hệ, và là nơi giữ chỉ số. */
    private List<ItemStore> storeRows(int itemId) {
        // Danh sách cửa hàng cần dò nằm ở GiveItem, dùng chung với lệnh chat "item" -- hai chỗ
        // dò khác nhau thì cùng một món ra chỉ số khác nhau, rất khó lần ra.
        return GiveItem.storeRows(itemId);
    }

    // ------------------------------------------------------------------ hiển thị

    private void showDetail() {
        ItemTemplate t = selected();
        if (t == null) {
            bigIcon.setIcon(null);
            title.setText(" ");
            detail.setText("");
            sys.setModel(new DefaultComboBoxModel<String>());
            return;
        }
        bigIcon.setIcon(icon(t.icon, PREVIEW));
        title.setText(t.name + "  (id " + t.id + ")");

        List<ItemStore> rows = storeRows(t.id);
        String keepSys = (String) sys.getSelectedItem();
        DefaultComboBoxModel<String> sm = new DefaultComboBoxModel<>();
        for (ItemStore s : rows) {
            sm.addElement(String.valueOf(s.getSys()));
        }
        if (sm.getSize() == 0) {
            sm.addElement("-");
        }
        sys.setModel(sm);
        if (keepSys != null) {
            sys.setSelectedItem(keepSys);
        }

        StringBuilder b = new StringBuilder();
        b.append("Loại  : ").append(t.type >= 0 && t.type < TYPE_NAME.length
                ? TYPE_NAME[t.type] : String.valueOf(t.type)).append(" (").append(t.type).append(")\n");
        b.append("Cấp   : ").append(t.level).append('\n');
        b.append("Xếp chồng: ").append(t.isUpToUp ? "có" : "không").append('\n');
        if (t.description != null && !t.description.isEmpty()) {
            b.append('\n').append(t.description).append('\n');
        }
        ItemStore chosen = chosenStore(rows);
        if (chosen == null) {
            // Không có dòng nào trong store_data. Với đồ thời trang thì đó là chuyện bình thường:
            // 93 trong 95 món không có dòng nào, và một phần trong số đó được máy chủ random chỉ
            // số ngay lúc mặc (Char.useEquipment -> equip.randomOption()).
            b.append("\nKhông có dòng chỉ số trong store_data.");
            if (t.fashion != -1) {
                b.append("\nĐồ thời trang phần lớn là vậy -- một số món được random chỉ số"
                        + "\nngay khi mặc, nên gửi đi vẫn dùng được bình thường.");
            } else {
                b.append("\nGửi đi sẽ là món trắng chỉ số.");
            }
        } else {
            b.append("\n--- chỉ số khi gửi (").append(mode.getSelectedItem())
             .append(", nâng cấp ").append(upgrade.getSelectedItem()).append(") ---\n");
            Item preview = build(chosen, t, upgradeValue());
            for (ItemOption o : preview.options) {
                b.append(String.format("%-36s %d%n",
                        o.optionTemplate.name != null ? o.optionTemplate.name : "?", o.param));
            }
            b.append("\nMức sàn / trần của hệ này:\n");
            List<ItemOption> mx = chosen.getMaxOptions();
            List<ItemOption> mn = chosen.getMinOptions();
            for (int i = 0; i < mx.size(); i++) {
                b.append(String.format("%-30s %5d .. %d%n",
                        mx.get(i).optionTemplate.name, mn.get(i).param, mx.get(i).param));
            }
        }
        detail.setText(b.toString());
        detail.setCaretPosition(0);
    }

    private ItemStore chosenStore(List<ItemStore> rows) {
        if (rows.isEmpty()) {
            return null;
        }
        Object s = sys.getSelectedItem();
        for (ItemStore r : rows) {
            if (String.valueOf(r.getSys()).equals(s)) {
                return r;
            }
        }
        return rows.get(0);
    }

    private int upgradeValue() {
        int i = upgrade.getSelectedIndex();
        return i < 0 ? 0 : i;
    }

    private byte modeValue() {
        switch (mode.getSelectedIndex()) {
            case 1: return Converter.RANDOM_OPTION;
            case 2: return Converter.MIN_OPTION;
            default: return Converter.MAX_OPTION;
        }
    }

    /** Dựng một món đúng cách máy chủ dựng: từ dòng cửa hàng, rồi cộng nâng cấp. */
    private Item build(ItemStore store, ItemTemplate t, int up) {
        Item item;
        if (store != null) {
            item = Converter.getInstance().toItem(store, modeValue());
            item.sys = store.getSys();
        } else {
            item = ItemFactory.getInstance().newItem(t.id);
        }
        if (up > 0) {
            item.next(up);
        }
        return item;
    }

    private ImageIcon icon(int id, int size) {
        Integer key = Integer.valueOf(id * 1000 + size);
        ImageIcon cached = iconCache.get(key);
        if (cached != null) {
            return cached;
        }
        for (int z = 0; z < ZOOMS.length; z++) {
            File f = new File("Data/Img/Small/" + ZOOMS[z] + "/Small" + id + ".png");
            if (!f.isFile()) {
                continue;
            }
            ImageIcon raw = new ImageIcon(f.getPath());
            int w = raw.getIconWidth();
            int h = raw.getIconHeight();
            if (w <= 0 || h <= 0) {
                continue;
            }
            // Giữ nguyên tỉ lệ, không phóng to quá kích thước gốc cho khỏi vỡ hạt.
            double k = Math.min((double) size / w, (double) size / h);
            if (k > 1) {
                k = 1;
            }
            ImageIcon out = new ImageIcon(raw.getImage().getScaledInstance(
                    Math.max(1, (int) (w * k)), Math.max(1, (int) (h * k)), Image.SCALE_SMOOTH));
            iconCache.put(key, out);
            return out;
        }
        iconCache.put(key, null);
        return null;
    }

    // ------------------------------------------------------------------ gửi

    private void send() {
        ItemTemplate t = selected();
        if (t == null) {
            JOptionPane.showMessageDialog(this, "Hãy chọn một vật phẩm.");
            return;
        }
        String name = (String) who.getSelectedItem();
        Char c = name == null ? null : Char.findCharByName(name);
        if (!TaskAdmin.alive(c)) {
            JOptionPane.showMessageDialog(this, "Nhân vật không online. Bấm \"Nạp lại nhân vật\".");
            return;
        }
        int n;
        try {
            n = Integer.parseInt(quantity.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Số lượng phải là số.");
            return;
        }
        if (n <= 0) {
            JOptionPane.showMessageDialog(this, "Số lượng phải lớn hơn 0.");
            return;
        }

        ItemStore store = chosenStore(storeRows(t.id));
        int up = upgradeValue();
        int sent = 0;

        if (t.isUpToUp) {
            // Đồ xếp chồng đi một món mang cả số lượng; tách ra nhiều món sẽ bị gộp lại sai.
            Item item = build(store, t, up);
            item.setQuantity(n);
            item.isLock = lock.isSelected();
            sent = c.addItemToBag(item) ? n : 0;
        } else {
            for (int i = 0; i < n; i++) {
                // Mỗi ô phải là một đối tượng riêng, dùng lại một Item thì các ô cùng trỏ một chỗ.
                Item item = build(store, t, up);
                item.setQuantity(1);
                item.isLock = lock.isSelected();
                if (!c.addItemToBag(item)) {
                    break;
                }
                sent++;
            }
        }

        if (sent == 0) {
            status.setText(" Không gửi được -- hành trang của " + c.name + " đã đầy.");
            JOptionPane.showMessageDialog(this, "Hành trang của " + c.name + " đã đầy.");
            return;
        }
        String msg = "Đã gửi " + sent + " " + t.name
                + (up > 0 ? " +" + up : "") + " cho " + c.name;
        if (sent < n) {
            msg += " (thiếu " + (n - sent) + ", hết chỗ)";
        }
        String extra = giveMoney(c);
        status.setText(" " + msg + extra);
        c.serverMessage("Bạn nhận được " + t.name + (up > 0 ? " +" + up : "")
                + (sent > 1 ? " x" + sent : ""));
    }

    private void sendMoney() {
        String name = (String) who.getSelectedItem();
        Char c = name == null ? null : Char.findCharByName(name);
        if (!TaskAdmin.alive(c)) {
            JOptionPane.showMessageDialog(this, "Nhân vật không online. Bấm \"Nạp lại nhân vật\".");
            return;
        }
        String extra = giveMoney(c);
        status.setText(extra.isEmpty() ? " Không có khoản tiền nào để gửi." : " " + c.name + ":" + extra);
    }

    /** Cộng xu/yên/lượng nếu có nhập; trả về phần mô tả để ghép vào dòng trạng thái. */
    private String giveMoney(Char c) {
        long vXu = number(xu);
        long vYen = number(yen);
        long vLuong = number(luong);
        StringBuilder b = new StringBuilder();
        if (vXu > 0) {
            c.addCoin(vXu);
            b.append(" +").append(vXu).append(" xu");
        }
        if (vYen > 0) {
            c.addYen(vYen);
            b.append(" +").append(vYen).append(" yên");
        }
        if (vLuong > 0) {
            c.addGold((int) vLuong);
            b.append(" +").append(vLuong).append(" lượng");
        }
        return b.toString();
    }

    private long number(JTextField f) {
        try {
            return Long.parseLong(f.getText().trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
