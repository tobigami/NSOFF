
// khi chạy windows

package com.nsoz.server;
import com.nsoz.util.Log;
import com.nsoz.util.NinjaUtils;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;


public class AutoMaintenance {

    public static void maintenance(int hours, int minutes, int seconds) {
        LocalDateTime localNow = LocalDateTime.now();
        ZoneId currentZone = ZoneId.of("Asia/Ho_Chi_Minh");
        ZonedDateTime zonedNow = ZonedDateTime.of(localNow, currentZone);
        ZonedDateTime zonedNext5;
        zonedNext5 = zonedNow.withHour(hours).withMinute(minutes).withSecond(seconds);
        if (zonedNow.compareTo(zonedNext5) > 0) {
            zonedNext5 = zonedNext5.plusDays(1);
        }

        Duration duration = Duration.between(zonedNow, zonedNext5);
        long initalDelay = duration.getSeconds();


Runnable runnable = new Runnable() { // bật tự đóng cmd
    public void run() {
        try {
            Server.maintance();
        } finally {
            NinjaUtils.setTimeout(() -> {
                try {
                    Runtime.getRuntime().exec("taskkill /F /IM cmd.exe");
                    openCmd(new String(NinjaUtils.getFile("run.bat")));
                    System.exit(1);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }, 30000);
        }
    }
};


        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(runnable, initalDelay, 24 * 60 * 60, TimeUnit.SECONDS);
        System.out.println("Tự động bảo trì " + hours + "h" + minutes);
    }

    public static void openCmd(String containerName) {
        try {
            Runtime rt = Runtime.getRuntime();
            rt.exec("cmd /c start cmd.exe /K \"dir && " + containerName);

        } catch (IOException ex) {
            Log.logException("Error handling server shutdown", AutoMaintenance.class, ex);
        }
    }
}





// khi chạy linux

//package com.nsoz.server;
//import com.nsoz.util.Log;
//import java.io.IOException;
//import java.time.Duration;
//import java.time.LocalDateTime;
//import java.time.ZoneId;
//import java.time.ZonedDateTime;
//import java.util.concurrent.Executors;
//import java.util.concurrent.ScheduledExecutorService;
//import java.util.concurrent.TimeUnit;
//import java.util.logging.Level;
//import java.util.logging.Logger;
//
//
//public class AutoMaintenance {
//
//
//    public static void maintenance(int hours, int minutes, int seconds) {
//        LocalDateTime localNow = LocalDateTime.now();
//        ZoneId currentZone = ZoneId.of("Asia/Ho_Chi_Minh");
//        ZonedDateTime zonedNow = ZonedDateTime.of(localNow, currentZone);
//        ZonedDateTime zonedNext5;
//        zonedNext5 = zonedNow.withHour(hours).withMinute(minutes).withSecond(seconds);
//        if (zonedNow.compareTo(zonedNext5) > 0) {
//            zonedNext5 = zonedNext5.plusDays(1);
//        }
//
//        Duration duration = Duration.between(zonedNow, zonedNext5);
//        long initalDelay = duration.getSeconds();
//        Runnable runnable = new Runnable() {
//            public void run() {
//                try {
//                    Server.maintance();
//                } finally {
//                    openCmd("nso_sony_server");
//                    System.exit(1);
//                }
//            }
//        };
//        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
//        scheduler.scheduleAtFixedRate(runnable, initalDelay, 24 * 60 * 60, TimeUnit.SECONDS);
//        System.out.println("Tự động bảo trì " + hours + "h" + minutes);
//    }
//
//    public static void openCmd(String containerName) {
//        try {
//            Runtime rt = Runtime.getRuntime();
//
//            // Dừng container hiện tại
//            rt.exec(new String[] {"/bin/bash", "-c", "docker stop " + containerName});
//            rt.exec(new String[] {"/bin/bash", "-c", "docker rm " + containerName});
//            System.out.println("Container " + containerName + " đã được khởi động lại.");
//
//        } catch (IOException ex) {
//            Log.logException("Error handling server shutdown", AutoMaintenance.class, ex);
//        }
//    }
//
//
//}
