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
 * Cho client vẽ số lớn thành dạng K / M / B.
 *
 * Lớp font của client vẽ chữ bằng cách tra từng ký tự trong một chuỗi bộ ký tự:
 *
 *     int idx = boKyTu.indexOf(chuoi.charAt(i));   // " 0123456789+-"
 *
 * Chỗ này làm hai việc trong đúng lớp đó:
 *
 *   1. Nối ".KMB" vào chuỗi bộ ký tự, khớp với bốn ô ảnh mà MakeNumberFont đã vẽ thêm. Cả năm
 *      font màu dùng chung một hằng chuỗi nên sửa một chỗ là đủ.
 *
 *   2. Trỏ String.length() và String.charAt(int) sang SoNgan.len / SoNgan.charAt. Không đi tìm
 *      từng nơi gọi vẽ, mà chặn ngay tại chỗ font đọc nội dung chuỗi -- mọi con số vẽ bằng font
 *      nào cũng đi qua đây. invokevirtual và invokestatic đều 3 byte nên không byte nào xê dịch.
 *
 * Việc lọc chuỗi nào được rút gọn nằm trong SoNgan, không nằm ở đây: chỉ chuỗi toàn chữ số và từ
 * 100.000 trở lên mới đổi, nên dòng chỉ số món đồ giữ nguyên.
 *
 * Usage: ShortNumbers <in.jar> <out.jar>
 */
public final class ShortNumbers {

    private static final int INVOKEVIRTUAL = 0xB6, INVOKESTATIC = 0xB8;
    private static final String BO_CU = " 0123456789+-";
    private static final String BO_MOI = " 0123456789+-.KMB";
    private static final String HOOK = "SoNgan";

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("usage: ShortNumbers <in.jar> <out.jar>");
            return;
        }
        ZipFile zip = new ZipFile(new File(args[0]));
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(new File(args[1])));
        boolean done = false;
        Enumeration<? extends ZipEntry> it = zip.entries();
        while (it.hasMoreElements()) {
            ZipEntry e = it.nextElement();
            byte[] data = readAll(zip.getInputStream(e));
            if (!done && e.getName().endsWith(".class")) {
                byte[] next = patch(data);
                if (next != null) {
                    data = next;
                    done = true;
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
        if (!done) {
            System.out.println("!! không tìm thấy lớp font -- chưa vá");
        }
    }

    private static byte[] patch(byte[] d) throws Exception {
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
        int[] natName = new int[count], natDesc = new int[count];
        int[] classNameIdx = new int[count];
        boolean coBoKyTu = false;

        for (int i = 1; i < count; i++) {
            int tag = in.readUnsignedByte();
            pool.writeByte(tag);
            switch (tag) {
                case 1: {
                    byte[] s = new byte[in.readUnsignedShort()];
                    in.readFully(s);
                    utf8[i] = new String(s, "UTF-8");
                    if (utf8[i].equals(BO_CU)) {
                        s = BO_MOI.getBytes("UTF-8");   // dài ra, bể hằng được dựng lại nên không sao
                        coBoKyTu = true;
                    }
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
                case 10: case 11: {
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
                case 9: case 3: case 4: case 17: case 18: copy(in, pool, 4); break;
                case 15: copy(in, pool, 3); break;
                case 8: case 16: case 19: case 20: copy(in, pool, 2); break;
                default: throw new IllegalStateException("tag lạ " + tag);
            }
        }
        if (!coBoKyTu) {
            return null;                       // không phải lớp font
        }
        int cuLen = timRef(count, utf8, refClass, refNat, natName, natDesc, classNameIdx,
                "java/lang/String", "length", "()I");
        int cuChar = timRef(count, utf8, refClass, refNat, natName, natDesc, classNameIdx,
                "java/lang/String", "charAt", "(I)C");
        if (cuLen < 0 || cuChar < 0) {
            System.out.println("!! lớp font không gọi length/charAt như mong đợi -- chưa vá");
            return null;
        }

        // Mười hằng mới nối vào cuối, mọi chỉ số cũ giữ nguyên ý nghĩa.
        int base = count;
        byte[] cls = HOOK.getBytes("UTF-8");
        byte[] nLen = "len".getBytes("UTF-8"), dLen = "(Ljava/lang/String;)I".getBytes("UTF-8");
        byte[] nCh = "charAt".getBytes("UTF-8"), dCh = "(Ljava/lang/String;I)C".getBytes("UTF-8");
        pool.writeByte(1); pool.writeShort(cls.length);  pool.write(cls);            // base
        pool.writeByte(7); pool.writeShort(base);                                    // base+1
        pool.writeByte(1); pool.writeShort(nLen.length); pool.write(nLen);           // base+2
        pool.writeByte(1); pool.writeShort(dLen.length); pool.write(dLen);           // base+3
        pool.writeByte(12); pool.writeShort(base + 2); pool.writeShort(base + 3);    // base+4
        pool.writeByte(10); pool.writeShort(base + 1); pool.writeShort(base + 4);    // base+5
        pool.writeByte(1); pool.writeShort(nCh.length);  pool.write(nCh);            // base+6
        pool.writeByte(1); pool.writeShort(dCh.length);  pool.write(dCh);            // base+7
        pool.writeByte(12); pool.writeShort(base + 6); pool.writeShort(base + 7);    // base+8
        pool.writeByte(10); pool.writeShort(base + 1); pool.writeShort(base + 8);    // base+9
        int moiLen = base + 5, moiChar = base + 9;

        byte[] rest = readAll(in);
        int a = doi(rest, cuLen, moiLen), b = doi(rest, cuChar, moiChar);
        System.out.println("  bộ ký tự font -> \"" + BO_MOI + "\"");
        System.out.println("  chuyển hướng length: " + a + " chỗ, charAt: " + b + " chỗ");

        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        DataOutputStream o = new DataOutputStream(bo);
        o.writeInt(0xCAFEBABE);
        o.writeShort(minor);
        o.writeShort(major);
        o.writeShort(count + 10);
        o.write(poolOut.toByteArray());
        o.write(rest);
        return bo.toByteArray();
    }

    /** Đổi mọi "invokevirtual cu" thành "invokestatic moi". Trả về số chỗ đã đổi. */
    private static int doi(byte[] code, int cu, int moi) {
        int n = 0;
        for (int i = 0; i + 2 < code.length; i++) {
            if ((code[i] & 0xFF) != INVOKEVIRTUAL) {
                continue;
            }
            if ((((code[i + 1] & 0xFF) << 8) | (code[i + 2] & 0xFF)) != cu) {
                continue;
            }
            code[i] = (byte) INVOKESTATIC;
            code[i + 1] = (byte) (moi >> 8);
            code[i + 2] = (byte) moi;
            n++;
        }
        return n;
    }

    private static int timRef(int count, String[] utf8, int[] refClass, int[] refNat,
            int[] natName, int[] natDesc, int[] classNameIdx, String cls, String name, String desc) {
        for (int i = 1; i < count; i++) {
            int c = refClass[i], nat = refNat[i];
            if (c <= 0 || nat <= 0 || c >= count || nat >= count) {
                continue;
            }
            if (!cls.equals(utf8[classNameIdx[c]])) {
                continue;
            }
            if (name.equals(utf8[natName[nat]]) && desc.equals(utf8[natDesc[nat]])) {
                return i;
            }
        }
        return -1;
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
