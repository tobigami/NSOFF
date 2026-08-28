import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;

/**
 * Moves the status/quest lines the client draws over the play area (ap.q, the block that prints
 * "HS luong", "Loc Do" and the current quest).
 *
 * ap.class is far too tangled to recompile, but the position comes from two places that are a
 * single byte each in the bytecode:
 *
 *   getstatic mq; sipush 136; iaload; getstatic ci.a; iadd; istore_2   -> y = mq[136] + ci.a
 *   ifeq +6; iinc 3, 30                                                -> x += 30
 *
 * Rewriting the array index and the iinc operand keeps every instruction the same length, so no
 * offset in the method moves and nothing else has to be touched.
 */
public final class MoveHudText {

    /** Index into mq[] for the vertical anchor. 0 is the array's zero entry, i.e. hard to the top. */
    private static final int Y_INDEX = 0;
    /** Horizontal offset. The stock client indents by 30; 0 pins the lines to the left edge. */
    private static final int X_OFFSET = 0;

    public static void main(String[] args) throws Exception {
        File f = new File(args.length > 0 ? args[0] : "build/lcpatch/ap.class");
        byte[] d = Files.readAllBytes(f.toPath());

        int done = 0;
        done += patchY(d);
        done += patchX(d);

        if (done == 2) {
            FileOutputStream o = new FileOutputStream(f);
            o.write(d);
            o.close();
            System.out.println("  status lines moved: mq index -> " + Y_INDEX
                    + ", x offset -> " + X_OFFSET);
        } else {
            System.out.println("!! layout not found (" + done + "/2) -- " + f.getName()
                    + " left untouched");
        }
    }

    /** getstatic mq; sipush <idx>; iaload; getstatic ci.a; iadd; istore_2 */
    private static int patchY(byte[] d) {
        int[] head = { 0xB2, 0x05, 0x60, 0x11 };          // getstatic mq, sipush
        int[] tail = { 0x2E, 0xB2, 0x06, 0x27, 0x60, 0x3D }; // iaload, getstatic ci.a, iadd, istore_2
        int hits = 0, at = -1;
        for (int i = 0; i + head.length + 2 + tail.length <= d.length; i++) {
            if (!matches(d, i, head)) {
                continue;
            }
            if (!matches(d, i + head.length + 2, tail)) {
                continue;
            }
            hits++;
            at = i + head.length;
        }
        if (hits != 1) {
            System.out.println("    y: expected one match, found " + hits);
            return 0;
        }
        d[at] = (byte) ((Y_INDEX >> 8) & 0xFF);
        d[at + 1] = (byte) (Y_INDEX & 0xFF);
        return 1;
    }

    /** ifeq +6; iinc 3, <n> */
    private static int patchX(byte[] d) {
        int[] p = { 0x99, 0x00, 0x06, 0x84, 0x03, 0x1E };
        int hits = 0, at = -1;
        for (int i = 0; i + p.length <= d.length; i++) {
            if (matches(d, i, p)) {
                hits++;
                at = i + 5;
            }
        }
        if (hits != 1) {
            System.out.println("    x: expected one match, found " + hits);
            return 0;
        }
        d[at] = (byte) X_OFFSET;
        return 1;
    }

    private static boolean matches(byte[] d, int at, int[] pattern) {
        for (int k = 0; k < pattern.length; k++) {
            if ((d[at + k] & 0xFF) != pattern[k]) {
                return false;
            }
        }
        return true;
    }
}
