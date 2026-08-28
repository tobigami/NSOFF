import java.awt.Frame;
import java.lang.reflect.Method;

/** Kiểm tra bảng nhiệm vụ dựng được và các hàm nó gọi qua reflection vẫn còn đúng tên. */
public class TaskCheck {
    public static void main(String[] a) throws Exception {
        Class<?> ch = Class.forName("com.nsoz.model.Char");
        Method finish = ch.getDeclaredMethod("finishTask", boolean.class);
        System.out.println("Char.finishTask(boolean): OK (" + finish + ")");
        System.out.println("Char.takingTask(): " + ch.getMethod("takingTask"));
        System.out.println("Char.taskNext():   " + ch.getMethod("taskNext"));
        System.out.println("Char.taskId field: " + ch.getField("taskId"));
        System.out.println("Char.taskMain:     " + ch.getField("taskMain"));

        Class<?> admin = Class.forName("com.nsoz.server.TaskAdmin");
        Object f = admin.getDeclaredConstructor().newInstance();
        System.out.println("TaskAdmin dựng được: " + ((Frame) f).getTitle()
                + " " + ((Frame) f).getWidth() + "x" + ((Frame) f).getHeight());
        ((Frame) f).dispose();
        System.exit(0);
    }
}
