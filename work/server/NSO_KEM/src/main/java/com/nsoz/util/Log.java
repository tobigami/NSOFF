/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nsoz.util;

import com.nsoz.server.Config;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;

/**
 * @author Admin
 */
public class Log {

    private static final Logger LOG = LogManager.getLogger(Log.class);

    // Info Level Logs
    public static void info(String message) {
        System.out.println(message);
    }

    public static void info(Object object) {
        System.out.println(object);
    }

    // Warn Level Logs
    public static void warn(String message) {
        LOG.warn(message);
    }

    public static void warn(Object object) {
        LOG.warn(object);
    }

    // Error Level Logs
    public static void error(String message) {
        LOG.error(message);
    }

    public static void error(Object object) {
        LOG.error(object);
    }

    // Fatal Level Logs
    public static void fatal(String message) {
        LOG.fatal(message);
    }


    // Debug Level Logs
    public static void debug(String message) {
        if (Config.getInstance().isShowLog()) {
            LOG.debug(message);
        }
    }

    public static void debug(Object object) {
        if (Config.getInstance().isShowLog()) {
            LOG.debug(object);
        }
    }

    public static void error(String message, Throwable throwable) {
        LOG.error(message, throwable);
    }

    /**
     * Ai đang gây ra dòng log này, theo từng luồng.
     *
     * Máy chủ xử lý mỗi phiên trên một luồng riêng, nên chỉ cần ghi tên vào đầu mỗi gói tin là mọi
     * dòng log sinh ra sau đó đều biết của ai -- khỏi phải truyền tên qua hàng chục lời gọi hàm.
     * Không có tên thì cũng không sao, log vẫn ghi bình thường.
     */
    private static final ThreadLocal<String> AI = new ThreadLocal<>();

    private static final java.time.format.DateTimeFormatter GIO =
            java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss");

    /** Gọi ở đầu mỗi gói tin. Truyền null khi chưa biết là ai. */
    public static void datAi(String ten) {
        if (ten == null || ten.isEmpty()) {
            AI.remove();
        } else {
            AI.set(ten);
        }
    }

    public static String ai() {
        String t = AI.get();
        return t == null ? "?" : t;
    }

    public static void logException(String message, Class<?> clazz, Throwable throwable) {
        StackTraceElement[] stackTraceElements = throwable.getStackTrace();
   //  throwable.printStackTrace();   /// không cho log lỗi ra cmd
        String method = "";
        int lineNumber = 0;
        if (stackTraceElements.length > 1) {
            method = stackTraceElements[1].getMethodName();
            lineNumber = stackTraceElements[1].getLineNumber();
        }else if (stackTraceElements.length == 1) {
            method = stackTraceElements[0].getMethodName();
            lineNumber = stackTraceElements[0].getLineNumber();
        } else {
            method = "Unknown method";
            lineNumber = -1;  // Giá trị mặc định nếu không có thông tin stack trace
        }
        // Một dòng gọn, có giờ và tên người chơi, để `grep` ra là đọc được ngay.
        //
        // Khung kẻ `<----->` cũ trải lỗi ra bảy dòng và không có tên ai, nên muốn biết lỗi của
        // người nào thì phải dò ngược lên các dòng trước đó -- mà log nhiều phiên trộn lẫn nhau
        // thì dò cũng không chắc đúng.
        String tom = clazz.getSimpleName() + "." + method + ":" + lineNumber;
        Log.error(String.format("[LOI] %s  nv=%s  %s  %s -- %s",
                java.time.LocalTime.now().format(GIO), ai(), tom, message, throwable), throwable);
    }

    public static void log(Priority priority, Object message) {
        LOG.log(priority, message);
    }

}
