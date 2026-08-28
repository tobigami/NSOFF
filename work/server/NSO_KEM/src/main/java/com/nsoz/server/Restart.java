package com.nsoz.server;

import com.nsoz.util.Log;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

/**
 * Tắt máy chủ rồi bật lại bằng một tiến trình mới.
 *
 * Không bật lại trong cùng JVM được: Server.stop() đóng Netty và các manager, còn Server.init()
 * thì nạp dữ liệu bằng cách add() vào các danh sách singleton -- gọi lần hai là mọi vật phẩm, bản
 * đồ, cửa hàng đều có hai bản. Sinh JVM mới thì mọi thứ về đúng trạng thái sạch.
 *
 * Tiến trình mới phải đợi tiến trình cũ nhả cổng 14444, vì main() có kiểm tra cổng trống và sẽ bỏ
 * cuộc nếu thấy bận. Nên nó được bọc trong một lệnh sh có nghỉ vài giây trước khi chạy java.
 *
 * Khi chạy dưới pm2 thì ngược lại: tự sinh tiến trình là hỏng, vì pm2 thấy tiến trình cũ thoát
 * cũng bật một cái mới -- hai máy chủ cùng giành cổng 14444. Lúc đó chỉ cần thoát cho gọn và để
 * pm2 lo phần bật lại; biến môi trường NSO_UNDER_PM2 trong ecosystem.config.js là dấu hiệu.
 */
public final class Restart {

    /** Báo trước cho người đang chơi, tính bằng giây. */
    private static final int WARN_SECONDS = 15;
    /** Tiến trình mới đợi bấy nhiêu giây cho cổng được nhả. */
    private static final int PORT_WAIT = 8;

    private Restart() {
    }

    /** Có người quản tiến trình lo việc bật lại hay không. */
    static boolean underPm2() {
        return "1".equals(System.getenv("NSO_UNDER_PM2"));
    }

    public static void now() {
        try {
            List<String> cmd = underPm2() ? new ArrayList<String>() : command();
            if (cmd == null) {
                System.out.println("!! không xác định được lệnh khởi động -- huỷ, máy chủ vẫn chạy");
                return;
            }

            System.out.println("Khởi động lại sau " + WARN_SECONDS + " giây...");
            String text = "Máy chủ khởi động lại sau " + WARN_SECONDS
                    + " giây, vui lòng đứng yên trong giây lát.";
            try {
                GlobalService.getInstance().chat("server", text);
                GlobalService.getInstance().showAlert("server", text);
            } catch (Exception ignored) {
                // chưa có ai online thì thôi, không phải lý do để dừng việc khởi động lại
            }
            Thread.sleep(WARN_SECONDS * 1000L);

            Server.saveAllPlayer();
            Server.saveAll();
            System.out.println("Đã lưu dữ liệu, đang đóng máy chủ...");
            Server.stop();

            if (underPm2()) {
                System.out.println("Đang chạy dưới pm2, thoát để pm2 bật lại.");
                System.exit(0);
            }

            new ProcessBuilder(cmd)
                    .directory(new File(System.getProperty("user.dir")))
                    .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                    .redirectErrorStream(true)
                    .start();
            System.out.println("Đã bật tiến trình mới, tiến trình này thoát.");
            System.exit(0);
        } catch (Exception e) {
            Log.logException("Lỗi khởi động lại: ", Restart.class, e);
        }
    }

    /** Dựng lại đúng lệnh đã khởi động tiến trình này, bọc trong sh để chờ cổng được nhả. */
    private static List<String> command() {
        try {
            File jar = new File(NinjaSchool.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            if (!jar.isFile()) {
                return null;   // chạy từ thư mục class, không phải jar -- không đoán bừa
            }
            StringBuilder line = new StringBuilder();
            line.append("sleep ").append(PORT_WAIT).append("; exec ");
            line.append(quote(System.getProperty("java.home") + File.separator
                    + "bin" + File.separator + "java"));
            for (String a : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
                // -agentlib/-javaagent là của trình gỡ lỗi đang gắn vào, mang sang chỉ gây lỗi cổng
                if (a.startsWith("-agentlib") || a.startsWith("-javaagent")
                        || a.startsWith("-Xdebug") || a.startsWith("-Xrunjdwp")) {
                    continue;
                }
                line.append(' ').append(quote(a));
            }
            line.append(" -jar ").append(quote(jar.getAbsolutePath()));

            List<String> cmd = new ArrayList<>();
            cmd.add("/bin/sh");
            cmd.add("-c");
            cmd.add(line.toString());
            return cmd;
        } catch (Exception e) {
            return null;
        }
    }

    private static String quote(String s) {
        return "'" + s.replace("'", "'\\''") + "'";
    }
}
