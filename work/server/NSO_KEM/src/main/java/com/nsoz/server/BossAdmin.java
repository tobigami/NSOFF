package com.nsoz.server;

import com.nsoz.map.MapManager;
import com.nsoz.map.Vithu;
import com.nsoz.map.War;
import com.nsoz.util.Log;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Gọi boss và mở sự kiện NGAY, không phải chờ tới khung giờ.
 *
 * VÌ SAO CẦN: mọi thứ trong game đều hẹn theo giờ thật -- boss ra lúc 6h và 21h, chiến trường
 * 17h30 tới 23h33, vĩ thú 20h55. Muốn thử một thay đổi liên quan tới chúng thì phải đợi đúng
 * khung giờ đó, hoặc sửa giờ trong mã rồi khởi động lại máy chủ (đá hết người đang chơi ra).
 * Bảng này gọi thẳng vào cùng những hàm mà bộ hẹn giờ gọi, nên thứ hiện ra giống hệt hàng thật.
 *
 * KHÔNG đụng tới lịch: bấm ở đây là mở thêm một lượt, lịch cũ vẫn chạy đúng giờ như thường.
 *
 * Chiến trường và vĩ thú không phải "bật một phát là xong" mà là một CHUỖI có giai đoạn --
 * dựng map, mở đăng ký, vào trận, kết thúc -- ngăn cách bằng những quãng chờ dài (chiến trường
 * 30 phút đăng ký + 60 phút đánh; vĩ thú 5 + 5 + 60 phút). Nên ở đây cho chọn:
 *   - "Đúng nhịp thật": giữ nguyên các quãng chờ, dùng khi muốn cho người chơi vào thật.
 *   - "Rút gọn": mỗi quãng còn 20 giây, dùng khi chỉ muốn xem thử map và phần thưởng có chạy
 *     không. Rút gọn thì người chơi gần như không kịp đăng ký -- đừng dùng lúc đông người.
 */
public class BossAdmin extends JFrame {

    private static BossAdmin dangMo;

    private final JComboBox<String> topBoss = new JComboBox<>(SpawnBossManager.cacTop());
    private final JComboBox<String> kieuBoss = new JComboBox<>(new String[] {"Tất cả điểm", "Một điểm ngẫu nhiên"});
    private final JComboBox<String> loaiWar = new JComboBox<>(new String[] {
        "Cấp 50-70", "Cấp 71-89", "Cấp 90-110", "Cấp 111-130", "Cấp tuỳ chọn"});
    private final JComboBox<String> nhip = new JComboBox<>(new String[] {"Đúng nhịp thật", "Rút gọn (20 giây/giai đoạn)"});
    private final JTextArea nhatKy = new JTextArea(10, 52);

    public static void run() {
        SwingUtilities.invokeLater(() -> {
            if (dangMo != null && dangMo.isDisplayable()) {
                dangMo.toFront();
                return;
            }
            dangMo = new BossAdmin();
            dangMo.setVisible(true);
        });
    }

    private BossAdmin() {
        super("Gọi boss & mở sự kiện ngay");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        JPanel tren = new JPanel(new GridLayout(0, 1, 4, 4));

        JPanel hangBoss = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        hangBoss.setBorder(BorderFactory.createTitledBorder("Boss thế giới"));
        hangBoss.add(new JLabel("Tốp:"));
        hangBoss.add(topBoss);
        hangBoss.add(kieuBoss);
        JButton nutBoss = new JButton("Gọi ra ngay");
        nutBoss.addActionListener(e -> goiBoss());
        hangBoss.add(nutBoss);
        tren.add(hangBoss);

        JPanel hangWar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        hangWar.setBorder(BorderFactory.createTitledBorder("Chiến trường"));
        hangWar.add(loaiWar);
        JButton nutWar = new JButton("Mở ngay");
        nutWar.addActionListener(e -> moWar());
        hangWar.add(nutWar);
        tren.add(hangWar);

        JPanel hangVithu = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        hangVithu.setBorder(BorderFactory.createTitledBorder("Vĩ thú"));
        JButton nutVithu = new JButton("Mở ngay");
        nutVithu.addActionListener(e -> moVithu());
        hangVithu.add(nutVithu);
        hangVithu.add(Box.createHorizontalStrut(12));
        hangVithu.add(new JLabel("Nhịp:"));
        hangVithu.add(nhip);
        tren.add(hangVithu);

        add(tren, BorderLayout.NORTH);

        nhatKy.setEditable(false);
        add(new JScrollPane(nhatKy), BorderLayout.CENTER);

        JLabel chan = new JLabel("  Lịch thật: boss 6h/21h, boss sự kiện 12h/20h,"
                + " chiến trường 17h30-23h33, vĩ thú 20h55. Bấm ở đây không đổi lịch.");
        add(chan, BorderLayout.SOUTH);

        ghi("Sẵn sàng. Máy chủ " + (Server.start ? "đang chạy." : "CHƯA bật -- gọi boss sẽ không có tác dụng."));
        pack();
        setLocationRelativeTo(null);
    }

    private void ghi(String s) {
        nhatKy.append(new SimpleDateFormat("HH:mm:ss").format(new Date()) + "  " + s + "\n");
        nhatKy.setCaretPosition(nhatKy.getDocument().getLength());
    }

    private void goiBoss() {
        String key = (String) topBoss.getSelectedItem();
        byte kieu = kieuBoss.getSelectedIndex() == 1 ? SpawnBossManager.RANDOM : SpawnBossManager.ALL;
        try {
            int n = SpawnBossManager.getInstance().goiNgay(key, kieu);
            if (n < 0) {
                ghi("Không có tốp boss tên '" + key + "'.");
            } else {
                ghi("Đã gọi tốp '" + key + "' -- " + n + " điểm spawn.");
            }
        } catch (Exception ex) {
            ghi("Lỗi gọi boss: " + ex);
            Log.logException("BossAdmin goi boss: ", BossAdmin.class, ex);
        }
    }

    /** Quãng chờ giữa các giai đoạn, theo lựa chọn nhịp. */
    private long cho(long that) {
        return nhip.getSelectedIndex() == 1 ? 20000L : that;
    }

    private void moWar() {
        final int loai = loaiWar.getSelectedIndex();
        ghi("Mở chiến trường " + loaiWar.getSelectedItem() + " ("
                + (nhip.getSelectedIndex() == 1 ? "rút gọn" : "đúng nhịp") + ")...");
        // Chạy ở luồng riêng: chuỗi này có Thread.sleep hàng chục phút, để trên luồng giao diện
        // là treo cứng cả bảng điều khiển lẫn cửa sổ máy chủ.
        new Thread(() -> {
            try {
                War war = new War(loai);
                MapManager.getInstance().normalWar = war;
                war.initMap();
                bao("chiến trường: đã dựng map, mở đăng ký");
                war.register();
                Thread.sleep(cho(1800000L));
                war.start();
                bao("chiến trường: VÀO TRẬN");
                Thread.sleep(cho(3600000L));
                war.end();
                bao("chiến trường: kết thúc");
            } catch (Exception ex) {
                bao("chiến trường lỗi: " + ex);
                Log.logException("BossAdmin mo war: ", BossAdmin.class, ex);
            }
        }, "admin-war").start();
    }

    private void moVithu() {
        ghi("Mở vĩ thú (" + (nhip.getSelectedIndex() == 1 ? "rút gọn" : "đúng nhịp") + ")...");
        new Thread(() -> {
            try {
                Vithu vt = MapManager.getInstance().vithu = new Vithu();
                vt.initMap();
                bao("vĩ thú: đã dựng map");
                Thread.sleep(cho(vt.TIME_REGISTER));
                vt.register();
                bao("vĩ thú: mở đăng ký");
                Thread.sleep(cho(vt.TIME_START));
                vt.start();
                bao("vĩ thú: VÀO TRẬN");
                Thread.sleep(cho(vt.TIME_END));
                vt.End();
                bao("vĩ thú: kết thúc");
            } catch (Exception ex) {
                bao("vĩ thú lỗi: " + ex);
                Log.logException("BossAdmin mo vithu: ", BossAdmin.class, ex);
            }
        }, "admin-vithu").start();
    }

    private void bao(String s) {
        SwingUtilities.invokeLater(() -> ghi(s));
    }
}
