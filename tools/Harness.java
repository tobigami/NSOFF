import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Headless-ish test harness for the offline build.
 *
 * Runs MicroEmulator in-process and grabs frames with Component.printAll(), which renders the
 * component tree into our own image. That works even when the emulator window lives on another
 * macOS Space or is not visible at all, so screenshots do not depend on the window manager.
 *
 * Usage: Harness <jar> <outDir> <script>
 *   script is a comma separated list of steps:
 *     wNNNN    wait NNNN ms
 *     sNAME    save screenshot NAME.png
 *     kNAME    press+release key NAME (UP DOWN LEFT RIGHT FIRE SOFT1 SOFT2 0-9 * #)
 */
public final class Harness {

    private static String outDir;

    public static void main(String[] args) throws Exception {
        final String jar = args[0];
        outDir = args[1];
        String script = args.length > 2 ? args[2] : "w15000,sboot";
        new File(outDir).mkdirs();

        Thread emu = new Thread(new Runnable() {
            public void run() {
                try {
                    org.microemu.app.Main.main(new String[] { jar });
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }, "microemu");
        emu.setDaemon(true);
        emu.start();

        String[] steps = script.split(",");
        for (int i = 0; i < steps.length; i++) {
            String s = steps[i].trim();
            if (s.length() == 0) continue;
            char op = s.charAt(0);
            String arg = s.substring(1);
            if (op == 'w') {
                Thread.sleep(Long.parseLong(arg));
            } else if (op == 's') {
                shot(arg);
            } else if (op == 'k') {
                key(arg);
            } else if (op == 'p') {
                pointer(arg);
            } else if (op == 'h') {
                hold(arg);
            }
        }
        System.out.println("HARNESS DONE");
        System.exit(0);
    }

    /** MicroEmulator's canvas component -- the thing that actually shows the game. */
    private static Component display() {
        Window[] ws = Window.getWindows();
        for (int i = 0; i < ws.length; i++) {
            Component c = find(ws[i], "DisplayComponent");
            if (c != null) return c;
        }
        return null;
    }

    private static Component find(Component root, String classNamePart) {
        if (root.getClass().getName().indexOf(classNamePart) >= 0) return root;
        if (root instanceof Container) {
            Component[] kids = ((Container) root).getComponents();
            for (int i = 0; i < kids.length; i++) {
                Component r = find(kids[i], classNamePart);
                if (r != null) return r;
            }
        }
        return null;
    }

    private static void shot(String name) throws Exception {
        List<Component> targets = new ArrayList<Component>();
        Component disp = display();
        if (disp != null && disp.getWidth() > 0) {
            targets.add(disp);
        } else {
            Window[] ws = Window.getWindows();
            for (int i = 0; i < ws.length; i++) {
                if (ws[i].getWidth() > 0 && ws[i].getHeight() > 0) targets.add(ws[i]);
            }
        }
        if (targets.isEmpty()) {
            System.out.println("SHOT " + name + " -> no component found");
            return;
        }
        Component c = targets.get(0);
        BufferedImage img = new BufferedImage(c.getWidth(), c.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        c.printAll(g);
        g.dispose();
        File f = new File(outDir, name + ".png");
        ImageIO.write(img, "png", f);
        System.out.println("SHOT " + name + " -> " + f + " (" + c.getWidth() + "x" + c.getHeight()
                + ") from " + c.getClass().getName());
    }

    /** The component MicroEmulator installs its KeyListener on. */
    private static Component deviceComp() {
        Window[] ws = Window.getWindows();
        for (int i = 0; i < ws.length; i++) {
            Component c = find(ws[i], "SwingDeviceComponent");
            if (c != null) return c;
        }
        return display();
    }

    private static void pointer(String arg) throws Exception {
        Component c = display();
        if (c == null) {
            System.out.println("PTR " + arg + " -> no display");
            return;
        }
        String[] xy = arg.split(":");
        int x = Integer.parseInt(xy[0]);
        int y = Integer.parseInt(xy[1]);
        long t = System.currentTimeMillis();
        c.dispatchEvent(new java.awt.event.MouseEvent(c, java.awt.event.MouseEvent.MOUSE_PRESSED,
                t, 0, x, y, 1, false, java.awt.event.MouseEvent.BUTTON1));
        Thread.sleep(90);
        c.dispatchEvent(new java.awt.event.MouseEvent(c, java.awt.event.MouseEvent.MOUSE_RELEASED,
                System.currentTimeMillis(), 0, x, y, 1, false, java.awt.event.MouseEvent.BUTTON1));
        System.out.println("PTR " + x + "," + y);
        Thread.sleep(400);
    }

    /** hNAME:MS -- hold a key down for MS, repeating the press so held-key logic keeps firing. */
    private static void hold(String arg) throws Exception {
        Component c = deviceComp();
        if (c == null) {
            System.out.println("HOLD " + arg + " -> no display");
            return;
        }
        String[] parts = arg.split(":");
        String name = parts[0];
        long ms = parts.length > 1 ? Long.parseLong(parts[1]) : 1000;
        int code = keyCode(name);
        char ch = keyChar(name);
        long end = System.currentTimeMillis() + ms;
        while (System.currentTimeMillis() < end) {
            c.dispatchEvent(new KeyEvent(c, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, code, ch));
            Thread.sleep(40);
        }
        c.dispatchEvent(new KeyEvent(c, KeyEvent.KEY_RELEASED, System.currentTimeMillis(), 0, code, ch));
        System.out.println("HOLD " + name + " " + ms + "ms");
        Thread.sleep(200);
    }

    private static void key(String name) throws Exception {
        Component c = deviceComp();
        if (c == null) {
            System.out.println("KEY " + name + " -> no display");
            return;
        }
        int code = keyCode(name);
        char ch = keyChar(name);
        long t = System.currentTimeMillis();
        c.dispatchEvent(new KeyEvent(c, KeyEvent.KEY_PRESSED, t, 0, code, ch));
        Thread.sleep(60);
        c.dispatchEvent(new KeyEvent(c, KeyEvent.KEY_RELEASED, System.currentTimeMillis(), 0, code, ch));
        System.out.println("KEY " + name);
        Thread.sleep(250);
    }

    private static int keyCode(String n) {
        if (n.equals("UP")) return KeyEvent.VK_UP;
        if (n.equals("DOWN")) return KeyEvent.VK_DOWN;
        if (n.equals("LEFT")) return KeyEvent.VK_LEFT;
        if (n.equals("RIGHT")) return KeyEvent.VK_RIGHT;
        if (n.equals("FIRE")) return KeyEvent.VK_ENTER;
        if (n.equals("SOFT1")) return KeyEvent.VK_F1;
        if (n.equals("SOFT2")) return KeyEvent.VK_F2;
        if (n.length() == 1) {
            char c = n.charAt(0);
            if (c >= '0' && c <= '9') return KeyEvent.VK_0 + (c - '0');
            // Clients that draw their own text fields read the typed character, so letters are
            // worth sending too -- that is the only way a script can fill in an account name.
            if (c >= 'a' && c <= 'z') return KeyEvent.VK_A + (c - 'a');
            if (c >= 'A' && c <= 'Z') return KeyEvent.VK_A + (c - 'A');
            if (c == '*') return KeyEvent.VK_MULTIPLY;
            if (c == '#') return KeyEvent.VK_NUMBER_SIGN;
        }
        return KeyEvent.VK_UNDEFINED;
    }

    private static char keyChar(String n) {
        if (n.length() == 1) return n.charAt(0);
        return KeyEvent.CHAR_UNDEFINED;
    }
}
