import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Nails a client to one game server by rewriting the code that builds the connection URL.
 *
 * SetServer is enough for clients that keep the address as a plain constant. This one is for the
 * clients that do not: the "148 TB2" build downloads its server list at run time and keeps it in
 * RMS, so no constant in the jar holds a host, and its two built-in "localhost" entries are dead
 * code -- the menu commands that reach them (200041/200042) are never created anywhere.
 *
 * What all of them still share is the moment of truth:
 *
 *     new StringBuffer("socket://").append(host).append(":").append(port).toString()
 *
 * so the host and port are replaced right there, after the menu has had its say. The six
 * instructions that append host, colon and port are swapped for a single push of "host:port" plus
 * padding, which keeps the method exactly as long as it was -- every branch target inside it stays
 * valid, so no other part of the class has to be touched.
 *
 * Usage: ForceServer <in.jar> <out.jar> <host> <port>
 */
public final class ForceServer {

    /** Opcodes of the sequence being replaced, in order, with method indexes left open. */
    private static final int ALOAD_1 = 0x2B;
    private static final int ILOAD_2 = 0x1C;
    private static final int LDC = 0x12;
    private static final int LDC_W = 0x13;
    private static final int INVOKEVIRTUAL = 0xB6;
    private static final int NOP = 0x00;
    private static final int PATTERN_LENGTH = 13;

    private static String address;
    private static int patched;

    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.out.println("usage: ForceServer <in.jar> <out.jar> <host> <port>");
            return;
        }
        address = args[2] + ":" + Integer.parseInt(args[3]);

        ZipFile zip = new ZipFile(new File(args[0]));
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(new File(args[1])));
        Enumeration<? extends ZipEntry> it = zip.entries();
        while (it.hasMoreElements()) {
            ZipEntry e = it.nextElement();
            byte[] data = readAll(zip.getInputStream(e));
            if (e.getName().endsWith(".class")) {
                byte[] next = patchClass(data, e.getName());
                if (next != null) {
                    data = next;
                }
            }
            ZipEntry copy = new ZipEntry(e.getName());
            copy.setTime(e.getTime());
            zos.putNextEntry(copy);
            zos.write(data);
            zos.closeEntry();
        }
        zos.close();
        zip.close();

        System.out.println("connection forced to " + address + "   (" + patched + " site(s) patched)");
        if (patched == 0) {
            System.out.println("!! nothing was patched -- the URL is not built the expected way");
        }
    }

    /** Returns the rewritten class, or null when it does not build a socket URL. */
    private static byte[] patchClass(byte[] d, String name) throws Exception {
        DataInputStream in = new DataInputStream(new java.io.ByteArrayInputStream(d));
        if (in.readInt() != 0xCAFEBABE) {
            return null;
        }
        int minor = in.readUnsignedShort();
        int major = in.readUnsignedShort();
        int count = in.readUnsignedShort();

        ByteArrayOutputStream poolOut = new ByteArrayOutputStream();
        DataOutputStream pool = new DataOutputStream(poolOut);

        // The colon is what anchors the search: the sequence being replaced pushes it between host
        // and port, and its pool index is the only part of the pattern known ahead of time. What
        // ldc names is the String entry, not the UTF8 it points at, so both are tracked.
        int colonUtf8 = -1;
        int colon = -1;
        int[] stringTarget = new int[count];
        boolean hasSocket = false;
        for (int i = 1; i < count; i++) {
            int tag = in.readUnsignedByte();
            pool.writeByte(tag);
            switch (tag) {
                case 1: {
                    byte[] s = new byte[in.readUnsignedShort()];
                    in.readFully(s);
                    String text = new String(s, "UTF-8");
                    if (text.equals(":")) {
                        colonUtf8 = i;
                    }
                    if (text.startsWith("socket://")) {
                        hasSocket = true;
                    }
                    pool.writeShort(s.length);
                    pool.write(s);
                    break;
                }
                case 8: {
                    int target = in.readUnsignedShort();
                    stringTarget[i] = target;
                    pool.writeShort(target);
                    break;
                }
                case 5:
                case 6:
                    copy(in, pool, 8);
                    i++;
                    break;
                case 3: case 4: case 9: case 10: case 11: case 12: case 17: case 18:
                    copy(in, pool, 4);
                    break;
                case 15:
                    copy(in, pool, 3);
                    break;
                case 7: case 16: case 19: case 20:
                    copy(in, pool, 2);
                    break;
                default:
                    throw new IllegalStateException(name + ": unknown constant tag " + tag);
            }
        }
        for (int i = 1; i < count; i++) {
            if (stringTarget[i] == colonUtf8 && colonUtf8 != -1) {
                colon = i;
                break;
            }
        }
        if (!hasSocket || colon == -1) {
            return null;
        }

        byte[] rest = readAll(in);

        // "..." lives in the pool as a UTF8 entry plus a String entry pointing at it. Both go on
        // the end so that every index already written into the code keeps meaning what it did.
        int utf8Index = count;
        int stringIndex = count + 1;
        byte[] text = address.getBytes("UTF-8");
        pool.writeByte(1);
        pool.writeShort(text.length);
        pool.write(text);
        pool.writeByte(8);
        pool.writeShort(utf8Index);

        int sites = rewriteCalls(rest, colon, stringIndex, name);
        if (sites == 0) {
            return null;
        }
        patched += sites;

        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        DataOutputStream o = new DataOutputStream(bo);
        o.writeInt(0xCAFEBABE);
        o.writeShort(minor);
        o.writeShort(major);
        o.writeShort(count + 2);
        o.write(poolOut.toByteArray());
        o.write(rest);
        return bo.toByteArray();
    }

    /**
     * Replaces every
     *     aload_1; invokevirtual append; ldc ":"; invokevirtual append; iload_2; invokevirtual append
     * with
     *     ldc_w "host:port"; invokevirtual append; nop x7
     * in place. Same length, so nothing downstream shifts.
     */
    private static int rewriteCalls(byte[] code, int colon, int stringIndex, String name) {
        int found = 0;
        for (int i = 0; i + PATTERN_LENGTH <= code.length; i++) {
            if ((code[i] & 0xFF) != ALOAD_1
                    || (code[i + 1] & 0xFF) != INVOKEVIRTUAL
                    || (code[i + 4] & 0xFF) != LDC
                    || (code[i + 5] & 0xFF) != colon
                    || (code[i + 6] & 0xFF) != INVOKEVIRTUAL
                    || (code[i + 9] & 0xFF) != ILOAD_2
                    || (code[i + 10] & 0xFF) != INVOKEVIRTUAL) {
                continue;
            }
            // Both appends must be the same method -- StringBuffer.append(String) -- or this is
            // some other code that merely looks alike.
            if (code[i + 2] != code[i + 7] || code[i + 3] != code[i + 8]) {
                continue;
            }
            byte hi = code[i + 2];
            byte lo = code[i + 3];
            code[i] = (byte) LDC_W;
            code[i + 1] = (byte) (stringIndex >> 8);
            code[i + 2] = (byte) stringIndex;
            code[i + 3] = (byte) INVOKEVIRTUAL;
            code[i + 4] = hi;
            code[i + 5] = lo;
            for (int k = i + 6; k < i + PATTERN_LENGTH; k++) {
                code[k] = (byte) NOP;
            }
            System.out.println("  " + shortName(name) + ": connection URL fixed at byte " + i);
            found++;
            i += PATTERN_LENGTH - 1;
        }
        return found;
    }

    /** Obfuscated class names run to hundreds of characters; print just enough to identify one. */
    private static String shortName(String name) {
        return name.length() > 44 ? name.substring(0, 40) + "..." : name;
    }

    private static void copy(DataInputStream in, DataOutputStream o, int n) throws Exception {
        byte[] b = new byte[n];
        in.readFully(b);
        o.write(b);
    }

    private static byte[] readAll(InputStream in) throws Exception {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) {
            bo.write(buf, 0, n);
        }
        in.close();
        return bo.toByteArray();
    }
}
