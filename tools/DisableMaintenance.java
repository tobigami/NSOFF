import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;

/**
 * Stops the server scheduling its own daily shutdown.
 *
 * Server.init() calls AutoMaintenance.maintenance(hour, minute, 0), which sleeps until the
 * configured time and then closes the server. There is no setting to turn it off, and an
 * out-of-range hour is no good either: init() catches the exception and returns false, so the
 * server would refuse to boot.
 *
 * The call pushes three ints and returns void, so replacing the 3-byte invokestatic with three
 * 1-byte pops leaves the stack balanced and every following offset exactly where it was -- no
 * jump target, exception table entry or line number has to move.
 */
public final class DisableMaintenance {

    public static void main(String[] args) throws Exception {
        File f = new File(args.length > 0 ? args[0]
                : "build/srvpatch/com/nsoz/server/Server.class");
        byte[] d = Files.readAllBytes(f.toPath());

        // invokestatic AutoMaintenance.maintenance -> pop; pop; pop
        int[] call = { 0xB8, 0x04, 0x60 };
        int hits = 0, at = -1;
        for (int i = 0; i + call.length <= d.length; i++) {
            boolean ok = true;
            for (int k = 0; k < call.length && ok; k++) {
                ok = (d[i + k] & 0xFF) == call[k];
            }
            if (ok) {
                hits++;
                at = i;
            }
        }
        if (hits != 1) {
            System.out.println("!! expected one call site, found " + hits + " -- nothing changed");
            return;
        }
        d[at] = 0x57;
        d[at + 1] = 0x57;
        d[at + 2] = 0x57;

        FileOutputStream o = new FileOutputStream(f);
        o.write(d);
        o.close();
        System.out.println("  automatic maintenance disabled (call at byte " + at + " replaced)");
    }
}
