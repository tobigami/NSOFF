import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Repoints a client JAR at a different game server.
 *
 * The address lives in the constant pool as plain UTF8 ("socket://127.0.0.1:14444" and a bare
 * host). Blanking bytes in place -- the trick StripUrls uses -- only works when the replacement is
 * no longer than the original, and a real hostname usually is. So the pool is parsed and written
 * out again with the new entry: class files address the pool by index, never by byte offset, so an
 * entry may change length as long as the whole file is rewritten in order.
 *
 * Usage: SetServer <in.jar> <out.jar> <host> <port>
 */
public final class SetServer {

    private static String host;
    private static int port;
    private static int rewritten;

    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.out.println("usage: SetServer <in.jar> <out.jar> <host> <port>");
            return;
        }
        File in = new File(args[0]);
        File out = new File(args[1]);
        host = args[2];
        port = Integer.parseInt(args[3]);

        ZipFile zip = new ZipFile(in);
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(out));
        Enumeration<? extends ZipEntry> it = zip.entries();
        while (it.hasMoreElements()) {
            ZipEntry e = it.nextElement();
            byte[] data = readAll(zip.getInputStream(e));
            if (e.getName().endsWith(".class")) {
                byte[] patched = patchClass(data, e.getName());
                if (patched != null) {
                    data = patched;
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
        System.out.println("server address -> " + host + ":" + port
                + "   (" + rewritten + " constant(s) rewritten)");
        if (rewritten == 0) {
            System.out.println("!! nothing was changed -- the address is not where it was expected");
        }
    }

    /** Returns the rewritten class, or null when it holds no address. */
    private static byte[] patchClass(byte[] d, String name) throws Exception {
        DataInputStream in = new DataInputStream(new java.io.ByteArrayInputStream(d));
        if (in.readInt() != 0xCAFEBABE) {
            return null;
        }
        int minor = in.readUnsignedShort();
        int major = in.readUnsignedShort();
        int count = in.readUnsignedShort();

        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        DataOutputStream o = new DataOutputStream(bo);
        o.writeInt(0xCAFEBABE);
        o.writeShort(minor);
        o.writeShort(major);
        o.writeShort(count);

        boolean touched = false;
        for (int i = 1; i < count; i++) {
            int tag = in.readUnsignedByte();
            o.writeByte(tag);
            switch (tag) {
                case 1: {                       // UTF8
                    byte[] s = new byte[in.readUnsignedShort()];
                    in.readFully(s);
                    String text = new String(s, "UTF-8");
                    String next = replacement(text);
                    if (next != null) {
                        System.out.println("  " + name + ": \"" + text + "\" -> \"" + next + "\"");
                        s = next.getBytes("UTF-8");
                        touched = true;
                        rewritten++;
                    }
                    o.writeShort(s.length);
                    o.write(s);
                    break;
                }
                case 5:                          // Long
                case 6:                          // Double
                    copy(in, o, 8);
                    i++;                         // these take two pool slots
                    break;
                case 3:                          // Integer
                case 4:                          // Float
                case 9:                          // Fieldref
                case 10:                         // Methodref
                case 11:                         // InterfaceMethodref
                case 12:                         // NameAndType
                case 17:                         // Dynamic
                case 18:                         // InvokeDynamic
                    copy(in, o, 4);
                    break;
                case 15:                         // MethodHandle
                    copy(in, o, 3);
                    break;
                case 7:                          // Class
                case 8:                          // String
                case 16:                         // MethodType
                case 19:                         // Module
                case 20:                         // Package
                    copy(in, o, 2);
                    break;
                default:
                    throw new IllegalStateException(
                            name + ": unknown constant pool tag " + tag + " at " + i);
            }
        }
        if (!touched) {
            return null;
        }
        // Everything after the pool is index-based, so it can be copied through untouched.
        byte[] rest = new byte[in.available()];
        in.readFully(rest);
        o.write(rest);
        o.flush();
        return bo.toByteArray();
    }

    /** The new text for a constant, or null to leave it alone. */
    private static String replacement(String text) {
        if (text.startsWith("socket://") && text.length() > "socket://".length()) {
            // A complete address. The bare "socket://" prefix elsewhere is joined to a host at
            // run time, so replacing that one would give socket://host:port + host + port again.
            return "socket://" + host + ":" + port;
        }
        if (text.equals("127.0.0.1") || text.equals("localhost")) {
            return host;
        }
        return null;
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
