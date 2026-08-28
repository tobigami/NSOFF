import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Window;

/**
 * Launches the offline build and parks the emulator window on a chosen display.
 *
 * macOS refuses osascript window control without accessibility permission, but a window this JVM
 * owns can be moved directly, so MicroEmulator is started in-process and repositioned.
 *
 * Usage: Play <midlet-or-jar> [screenIndex]  -- screenIndex defaults to the last (secondary) display.
 */
public final class Play {

    public static void main(String[] args) throws Exception {
        // launching the MIDlet class directly skips MicroEmulator's own launcher list
        final String jar = args.length > 0 ? args[0] : "main.GameMidlet";
        GraphicsDevice[] screens =
                GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
        int want = args.length > 1 ? Integer.parseInt(args[1]) : screens.length - 1;
        if (want < 0 || want >= screens.length) {
            want = screens.length - 1;
        }
        final GraphicsDevice screen = screens[want];
        System.out.println("displays: " + screens.length + ", using #" + want + " "
                + screen.getIDstring() + " " + screen.getDefaultConfiguration().getBounds());

        Thread emu = new Thread(new Runnable() {
            public void run() {
                try {
                    org.microemu.app.Main.main(new String[] { jar });
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }, "microemu");
        emu.setDaemon(false);
        emu.start();

        // The frame appears a moment after Main starts; keep nudging it for a while so it lands on
        // the requested display even if the emulator resizes itself during startup.
        GraphicsConfiguration gc = screen.getDefaultConfiguration();
        Rectangle b = gc.getBounds();
        for (int i = 0; i < 40; i++) {
            Thread.sleep(250);
            Window[] ws = Window.getWindows();
            for (int k = 0; k < ws.length; k++) {
                Window w = ws[k];
                if (!w.isShowing() || w.getWidth() < 100) {
                    continue;
                }
                int x = b.x + (b.width - w.getWidth()) / 2;
                int y = b.y + (b.height - w.getHeight()) / 2;
                if (w.getX() != x || w.getY() != y) {
                    w.setLocation(x, y);
                    if (i == 0) {
                        System.out.println("moved " + w.getClass().getSimpleName()
                                + " to " + x + "," + y);
                    }
                }
                w.toFront();
            }
        }
    }

}
