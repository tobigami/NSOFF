package com.nsoz.server;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import com.nsoz.clan.Clan;
import com.nsoz.db.jdbc.DbManager;
import com.nsoz.event.Event;
import com.nsoz.model.Char;
import com.nsoz.stall.StallManager;
import com.nsoz.util.Log;
import com.nsoz.util.NinjaUtils;

/**
 * @author ASD
 */
public class NinjaSchool extends WindowAdapter implements ActionListener {

    public static boolean isStop = false;
    private Frame frame;

    public NinjaSchool() { ///
        // Cửa sổ này dựng lúc máy chủ khởi động, nên là chỗ tiện nhất để bật luồng nới giới hạn
        // ngày -- Server.init() dùng Lombok nên không biên dịch lại được.
        DailyLimit.start();
        try {
            // Ghi rõ đang là bản nào ngay trên thanh tiêu đề. Hai cửa sổ trông hệt nhau mà một
            // cái bấm "Bảo trì (tắt hẳn)" là đá hết người đang chơi -- không được để phải đoán.
            String mt = System.getenv("NSO_MOI_TRUONG");
            String tieuDe = mt == null || mt.trim().isEmpty() ? "Quản lý"
                    : "prod".equalsIgnoreCase(mt.trim())
                            ? "Quản lý  ***  BẢN CHẠY THẬT (prod)  ***"
                            : "Quản lý  —  bản thử nghiệm (" + mt.trim() + ")";
            frame = new Frame(tieuDe);
            InputStream is = getClass().getClassLoader().getResourceAsStream("icon.png");
            byte[] data = new byte[is.available()];
            is.read(data);
            ImageIcon img = new ImageIcon(data);
            frame.setIconImage(img.getImage());
            frame.setBackground(Color.BLACK);
            frame.addWindowListener(this);

            // Trước đây các nút đặt bằng setBounds(30, y, 140, 30) trong layout null, nên bề rộng
            // đứng yên ở 140 điểm ảnh dù chữ dài bao nhiêu -- trên macOS phông rộng hơn Windows nên
            // nhãn bị cắt thành "Lưu dữ liệu gia t...". GridLayout để nút tự nở theo cửa sổ, cộng
            // với setResizable(true), nên nhãn dài đến mấy cũng đọc được.
            Panel buttons = new Panel(new GridLayout(0, 1, 0, 8));
            buttons.add(makeButton("Bảo trì (tắt hẳn)", "baotri9912"));
            buttons.add(makeButton("Khởi động lại", "restart99"));
            buttons.add(makeButton("Lưu Shinwa", "shinwa99"));
            buttons.add(makeButton("Lưu dữ liệu gia tộc", "clan99"));
            buttons.add(makeButton("Lưu dữ liệu người chơi", "player99"));
            buttons.add(makeButton("Làm mới TOP", "bxh99"));
            buttons.add(makeButton("Gửi Đồ", "buff99"));
            buttons.add(makeButton("Gửi ngọc", "addGems"));
            buttons.add(makeButton("Quản lý nhiệm vụ", "task99"));
            buttons.add(makeButton("Nâng cấp tự động", "upgrade99"));
            buttons.add(makeButton("Tài khoản", "account99"));
            buttons.add(makeButton("Hành trang", "bag99"));
            buttons.add(makeButton("Đưa về toạ độ", "tele99"));
            buttons.add(makeButton("Mặc đồ tốt nhất", "gear99"));
            buttons.add(makeButton("Sự kiện", "event99"));
            buttons.add(makeButton("Kinh nghiệm", "exp99"));
            buttons.add(makeButton("Boss & sự kiện", "boss99"));

            Panel pad = new Panel(new BorderLayout(0, 0));
            pad.add(buttons, BorderLayout.CENTER);
            frame.setLayout(new BorderLayout(0, 0));
            frame.add(pad, BorderLayout.CENTER);
            frame.add(margin(20, 1), BorderLayout.WEST);
            frame.add(margin(20, 1), BorderLayout.EAST);
            frame.add(margin(1, 12), BorderLayout.NORTH);
            frame.add(margin(1, 12), BorderLayout.SOUTH);

            frame.setResizable(true);
            frame.pack();
            frame.setSize(Math.max(frame.getWidth(), 260), frame.getHeight());
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        } catch (IOException ex) {
            Logger.getLogger(NinjaSchool.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private Button makeButton(String label, String command) {
        Button b = new Button(label);
        b.setActionCommand(command);
        b.addActionListener(this);
        return b;
    }

    /** Khoảng trống quanh hàng nút -- AWT không có đường viền rỗng như Swing. */
    private Panel margin(int width, int height) {
        Panel p = new Panel();
        p.setPreferredSize(new Dimension(width, height));
        return p;
    }

    public static void main(String args[]) throws Exception {
        if (Config.getInstance().load()) {
            if (!DbManager.start()) {
                return;
            }
            if (NinjaUtils.availablePort(Config.getInstance().getPort())) {
                new NinjaSchool(); ///  tắt giao diện khi chạy linux
                if (!Server.init()) {
                    System.out.println("Khoi tao that bai!");
                    return;
                }
                Server.start();
            } else {
                System.out.println("Port " + Config.getInstance().getPort() + " da duoc su dung!");
            }
        } else {
            System.out.println("Vui long kiem tra lai cau hinh 1!");
        }
    }



    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("shinwa99")) {
            if (Server.start) {
                System.out.println("Lưu Shinwa");
                StallManager.getInstance().save();
                System.out.println("Lưu shinwa xong");
            } else {
                System.out.println("Mãy chủ chưa bật");
            }
        }
        if (e.getActionCommand().equals("baotri9912")) {
            if (Server.start) {
                if (!isStop) {
                    (new Thread(new Runnable() {
                        public void run() {
                            try {
                                Server.maintance();
                                System.exit(0);
                            } catch (Exception e) {
                                Log.logException("Lỗi commanf bảo trì: ", NinjaSchool.class, e);

                            }

                        }
                    })).start();
                }

            } else {
                System.out.println("Máy chủ chưa bật.");
            }
        }
        if (e.getActionCommand().equals("clan99")) {
            System.out.println("Lưu dữ liệu gia tộc.");
            List<Clan> clans = Clan.getClanDAO().getAll();
            synchronized (clans) {
                for (Clan clan : clans) {
                    Clan.getClanDAO().update(clan);
                }
            }
            System.out.println("Lưu dữ liệu gia tộc xong");
        }
        if (e.getActionCommand().equals("bxh99")) {
            List<Char> chars = ServerManager.getChars();
            for (Char _char : chars) {
                _char.saveData();
            }
            System.out.println("Làm mới bảng xếp hạng");
            Ranked.refresh();
            System.out.println("Làm mới bảng xếp hạng xong");
        }
        if (e.getActionCommand().equals("player99")) {
            System.out.println("Lưu dữ liệu người chơi");
            List<Char> chars = ServerManager.getChars();
            for (Char _char : chars) {
                try {
                    if (_char != null && !_char.isCleaned) {
                        _char.saveData();
                        if (Event.getEvent() != null) {
                            _char.updateEventPoint();
                        }
                        if (_char.clone != null && !_char.clone.isCleaned) {
                            _char.clone.saveData();
                        }
                        if (_char.user != null && !_char.user.isCleaned) {
                            if (_char.user != null) {
                                _char.user.saveData();
                            }

                        }

                    }
                } catch (Exception ex) {
                    Log.logException("Lỗi Lưu data player action: ", NinjaSchool.class, ex);

                }
            }
            System.out.println("Lưu dữ liệu người chơi xong");
        }
        if (e.getActionCommand().equals("restartSQL99")) {
            System.out.println("Bắt đầu khởi động lại!");

            System.out.println("Khởi động xong!");
        }
        if (e.getActionCommand().equals("buff99")) {
            SendItemAdmin.run();
        }
        if (e.getActionCommand().equals("addGems")) {
            SendGems.run();
        }
        if (e.getActionCommand().equals("task99")) {
            TaskAdmin.run();
        }
        if (e.getActionCommand().equals("upgrade99")) {
            UpgradeAdmin.run();
        }
        if (e.getActionCommand().equals("account99")) {
            AccountAdmin.run();
        }
        if (e.getActionCommand().equals("bag99")) {
            BagAdmin.run();
        }
        if (e.getActionCommand().equals("tele99")) {
            TeleportAdmin.run();
        }
        if (e.getActionCommand().equals("gear99")) {
            GearAdmin.run();
        }
        if (e.getActionCommand().equals("event99")) {
            EventAdmin.run();
        }
        if (e.getActionCommand().equals("exp99")) {
            ExpAdmin.run();
        }
        if (e.getActionCommand().equals("boss99")) {
            BossAdmin.run();
        }
        if (e.getActionCommand().equals("restart99")) {
            if (!Server.start) {
                System.out.println("Máy chủ chưa bật.");
                return;
            }
            if (isStop) {
                System.out.println("Máy chủ đang trong quá trình tắt.");
                return;
            }
            (new Thread(Restart::now, "restart")).start();
        }
    }

    public void windowClosing(WindowEvent e) {
        frame.dispose();
        if (Server.start) {
            System.out.println("Đóng máy chủ.");
            Server.stop();
            System.exit(0);
        }
    }


}
