import java.awt.*;

/** Dựng đúng cửa sổ Quản lý rồi so bề rộng nút với bề rộng chữ, để biết nhãn có bị cắt không. */
public class GuiCheck {
    public static void main(String[] a) throws Exception {
        Class.forName("com.nsoz.server.NinjaSchool").getDeclaredConstructor().newInstance();
        Thread.sleep(1500);
        for (Frame f : Frame.getFrames()) {
            if (!"Quản lý".equals(f.getTitle())) continue;
            System.out.println("frame " + f.getWidth() + "x" + f.getHeight()
                    + " resizable=" + f.isResizable());
            walk(f);
            f.dispose();
        }
        System.exit(0);
    }
    static void walk(Container c) {
        for (Component comp : c.getComponents()) {
            if (comp instanceof Button) {
                Button b = (Button) comp;
                int need = b.getFontMetrics(b.getFont()).stringWidth(b.getLabel());
                System.out.printf("  %-26s width=%3d  chu=%3d  %s%n",
                        b.getLabel(), b.getWidth(), need, b.getWidth() >= need + 16 ? "OK" : "CAT!");
            } else if (comp instanceof Container) {
                walk((Container) comp);
            }
        }
    }
}
