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
 * Rút gọn con số trong câu "Bạn nhận được ... Kinh nghiệm".
 *
 * Câu này do client tự ghép chứ không phải máy chủ gửi xuống:
 *
 *     new StringBuffer().append(Fx).append(" ").append(exp).append(" ").append(DN).toString()
 *
 * Vì có cả chữ lẫn số nên nó không lọt bộ lọc "chuỗi số trần" của SoNgan -- phải chặn ngay lúc
 * ghép. Chỗ này đổi lời gọi append(long) thành SoNgan.them(StringBuffer, long): cùng nhận
 * (StringBuffer, long), cùng trả StringBuffer, và invokevirtual với invokestatic đều 3 byte nên
 * ngăn xếp lẫn độ dài đều khớp.
 *
 * Chỉ đổi lời gọi nào có nhãn "Kinh nghiệm" (trường DN) xuất hiện ngay sau đó, để những chỗ ghép
 * số khác trong cùng lớp không bị đụng tới.
 *
 * Usage: ShortExpMsg <in.jar> <out.jar>
 */
public final class ShortExpMsg {

    private static final int INVOKEVIRTUAL = 0xB6, INVOKESTATIC = 0xB8, GETSTATIC = 0xB2;
    /** Bao nhiêu byte sau lời gọi append thì còn coi là cùng một câu. */
    private static final int TAM_NHIN = 24;
    private static final String HOOK = "SoNgan", HOOK_METHOD = "them";
    private static final String DESC = "(Ljava/lang/StringBuffer;J)Ljava/lang/StringBuffer;";

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("usage: ShortExpMsg <in.jar> <out.jar>");
            return;
        }
        ZipFile zip = new ZipFile(new File(args[0]));
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(new File(args[1])));
        int tong = 0;
        Enumeration<? extends ZipEntry> it = zip.entries();
        while (it.hasMoreElements()) {
            ZipEntry e = it.nextElement();
            byte[] data = readAll(zip.getInputStream(e));
            if (e.getName().endsWith(".class")) {
                byte[][] r = patch(data);
                if (r != null) {
                    data = r[0];
                    tong += r[1][0];
                }
            }
            ZipEntry copy = new ZipEntry(e.getName());
            copy.setTime(e.getTime());
            zos.putNextEntry(copy);
            zos.write(data);
            zos.closeEntry();
        }
        zip.close();
        zos.close();
        System.out.println(tong > 0
                ? "  rút gọn số trong câu nhận kinh nghiệm: " + tong + " chỗ"
                : "!! không thấy câu nhận kinh nghiệm -- chưa vá");
    }

    private static byte[][] patch(byte[] d) throws Exception {
        DataInputStream in = new DataInputStream(new java.io.ByteArrayInputStream(d));
        if (in.readInt() != 0xCAFEBABE) {
            return null;
        }
        int minor = in.readUnsignedShort(), major = in.readUnsignedShort();
        int count = in.readUnsignedShort();

        ByteArrayOutputStream poolOut = new ByteArrayOutputStream();
        DataOutputStream pool = new DataOutputStream(poolOut);
        String[] utf8 = new String[count];
        int[] refClass = new int[count], refNat = new int[count];
        int[] natName = new int[count], natDesc = new int[count], classNameIdx = new int[count];

        for (int i = 1; i < count; i++) {
            int tag = in.readUnsignedByte();
            pool.writeByte(tag);
            switch (tag) {
                case 1: {
                    byte[] s = new byte[in.readUnsignedShort()];
                    in.readFully(s);
                    utf8[i] = new String(s, "UTF-8");
                    pool.writeShort(s.length);
                    pool.write(s);
                    break;
                }
                case 7: {
                    int t = in.readUnsignedShort();
                    classNameIdx[i] = t;
                    pool.writeShort(t);
                    break;
                }
                case 9: case 10: case 11: {
                    int c = in.readUnsignedShort(), nat = in.readUnsignedShort();
                    refClass[i] = c;
                    refNat[i] = nat;
                    pool.writeShort(c);
                    pool.writeShort(nat);
                    break;
                }
                case 12: {
                    int nm = in.readUnsignedShort(), de = in.readUnsignedShort();
                    natName[i] = nm;
                    natDesc[i] = de;
                    pool.writeShort(nm);
                    pool.writeShort(de);
                    break;
                }
                case 5: case 6: copy(in, pool, 8); i++; break;
                case 3: case 4: case 17: case 18: copy(in, pool, 4); break;
                case 15: copy(in, pool, 3); break;
                case 8: case 16: case 19: case 20: copy(in, pool, 2); break;
                default: throw new IllegalStateException("tag lạ " + tag);
            }
        }

        int append = -1, dn = -1;
        for (int i = 1; i < count; i++) {
            int c = refClass[i], nat = refNat[i];
            if (c <= 0 || nat <= 0 || c >= count || nat >= count) {
                continue;
            }
            String cls = utf8[classNameIdx[c]], nm = utf8[natName[nat]], de = utf8[natDesc[nat]];
            if ("java/lang/StringBuffer".equals(cls) && "append".equals(nm)
                    && "(J)Ljava/lang/StringBuffer;".equals(de)) {
                append = i;
            }
            if ("DN".equals(nm) && "Ljava/lang/String;".equals(de)) {
                dn = i;
            }
        }
        if (append < 0 || dn < 0) {
            return null;
        }

        int base = count;
        byte[] cls = HOOK.getBytes("UTF-8"), meth = HOOK_METHOD.getBytes("UTF-8");
        byte[] sig = DESC.getBytes("UTF-8");
        pool.writeByte(1); pool.writeShort(cls.length);  pool.write(cls);
        pool.writeByte(7); pool.writeShort(base);
        pool.writeByte(1); pool.writeShort(meth.length); pool.write(meth);
        pool.writeByte(1); pool.writeShort(sig.length);  pool.write(sig);
        pool.writeByte(12); pool.writeShort(base + 2); pool.writeShort(base + 3);
        pool.writeByte(10); pool.writeShort(base + 1); pool.writeShort(base + 4);
        int moi = base + 5;

        byte[] rest = readAll(in);
        int n = 0;
        for (int i = 0; i + 2 < rest.length; i++) {
            if ((rest[i] & 0xFF) != INVOKEVIRTUAL) {
                continue;
            }
            if ((((rest[i + 1] & 0xFF) << 8) | (rest[i + 2] & 0xFF)) != append) {
                continue;
            }
            if (!coNhan(rest, i + 3, dn)) {
                continue;
            }
            rest[i] = (byte) INVOKESTATIC;
            rest[i + 1] = (byte) (moi >> 8);
            rest[i + 2] = (byte) moi;
            n++;
        }
        if (n == 0) {
            return null;
        }

        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        DataOutputStream o = new DataOutputStream(bo);
        o.writeInt(0xCAFEBABE);
        o.writeShort(minor);
        o.writeShort(major);
        o.writeShort(count + 6);
        o.write(poolOut.toByteArray());
        o.write(rest);
        return new byte[][] { bo.toByteArray(), { (byte) n } };
    }

    /** Nhãn "Kinh nghiệm" có được đẩy lên trong tầm nhìn ngay sau lời gọi không. */
    private static boolean coNhan(byte[] code, int from, int dn) {
        for (int k = from; k < from + TAM_NHIN && k + 2 < code.length; k++) {
            if ((code[k] & 0xFF) == GETSTATIC
                    && (((code[k + 1] & 0xFF) << 8) | (code[k + 2] & 0xFF)) == dn) {
                return true;
            }
        }
        return false;
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
