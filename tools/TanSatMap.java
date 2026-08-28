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
 * Bắt menu "Tàn sát" chỉ hiện quái của map hiện tại.
 *
 * Vòng lặp dựng menu trong client đọc một Vector tĩnh tên pF -- danh sách tích luỹ mọi mẫu quái
 * từng gặp, không bao giờ dọn khi đổi map. Chỗ này đổi hai lệnh getstatic pF trong đúng vòng lặp
 * đó thành lời gọi TanSat.cur(), hàm dựng danh sách từ quái đang có trong map.
 *
 * getstatic và invokestatic đều là 1 byte mã lệnh cộng 2 byte chỉ số, nên không byte nào xê dịch
 * và mọi lệnh nhảy trong phương thức vẫn trỏ đúng chỗ.
 *
 * Định vị bằng chuỗi "Tàn sát all" -- mục đầu tiên của chính menu đó -- rồi bắt mẫu vòng lặp ngay
 * sau nó. Không dò theo tên trường vì tên đã bị làm rối và trùng nhau khắp nơi.
 *
 * Usage: TanSatMap <in.jar> <out.jar>
 */
public final class TanSatMap {

    private static final int LDC_W = 0x13, GETSTATIC = 0xB2, INVOKESTATIC = 0xB8;
    private static final String ANCHOR = "Tàn sát all";
    private static final String HOOK_CLASS = "TanSat", HOOK_METHOD = "cur";
    /** Vòng lặp bắt đầu trong khoảng này tính từ chỗ đẩy chuỗi mốc. */
    private static final int LOOKAHEAD = 40;

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("usage: TanSatMap <in.jar> <out.jar>");
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
        System.out.println(done
                ? "menu Tàn sát -> " + HOOK_CLASS + "." + HOOK_METHOD + "() (chỉ quái map hiện tại)"
                : "!! không tìm thấy vòng lặp dựng menu Tàn sát -- chưa vá");
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
        int anchorUtf8 = -1, vecUtf8 = -1;
        int[] stringTarget = new int[count];
        int[] refNameAndType = new int[count];
        int[] natDescriptor = new int[count];
        String[] utf8 = new String[count];

        for (int i = 1; i < count; i++) {
            int tag = in.readUnsignedByte();
            pool.writeByte(tag);
            switch (tag) {
                case 1: {
                    byte[] s = new byte[in.readUnsignedShort()];
                    in.readFully(s);
                    utf8[i] = new String(s, "UTF-8");
                    if (utf8[i].equals(ANCHOR)) {
                        anchorUtf8 = i;
                    }
                    pool.writeShort(s.length);
                    pool.write(s);
                    break;
                }
                case 8: {
                    int t = in.readUnsignedShort();
                    stringTarget[i] = t;
                    pool.writeShort(t);
                    break;
                }
                case 9: {                                  // Fieldref: nhớ NameAndType của nó
                    int owner = in.readUnsignedShort(), nat = in.readUnsignedShort();
                    refNameAndType[i] = nat;
                    pool.writeShort(owner);
                    pool.writeShort(nat);
                    break;
                }
                case 12: {                                 // NameAndType: nhớ chỉ số mô tả kiểu
                    int nameIdx = in.readUnsignedShort(), descIdx = in.readUnsignedShort();
                    natDescriptor[i] = descIdx;
                    pool.writeShort(nameIdx);
                    pool.writeShort(descIdx);
                    break;
                }
                case 5: case 6: copy(in, pool, 8); i++; break;
                case 10: case 11: case 3: case 4: case 17: case 18:
                    copy(in, pool, 4); break;
                case 15: copy(in, pool, 3); break;
                case 7: case 16: case 19: case 20: copy(in, pool, 2); break;
                default: throw new IllegalStateException("tag lạ " + tag);
            }
        }
        if (anchorUtf8 == -1) {
            return null;
        }
        int anchorString = -1;
        for (int i = 1; i < count; i++) {
            if (stringTarget[i] == anchorUtf8) {
                anchorString = i;
                break;
            }
        }
        if (anchorString == -1) {
            return null;
        }

        byte[] rest = readAll(in);
        int at = findLoop(rest, anchorString);
        if (at < 0) {
            return null;
        }
        // Kiểu trả về phải khớp KIỂU của pF, không phải lớp sở hữu nó: đi Fieldref -> NameAndType
        // -> chuỗi mô tả, ra thẳng dạng "LTênLớpVector;".
        int fieldRef = ((rest[at + 1] & 0xFF) << 8) | (rest[at + 2] & 0xFF);
        int nat = fieldRef > 0 && fieldRef < count ? refNameAndType[fieldRef] : 0;
        int descIdx = nat > 0 && nat < count ? natDescriptor[nat] : 0;
        String fieldDesc = descIdx > 0 && descIdx < count ? utf8[descIdx] : null;
        if (fieldDesc == null || fieldDesc.charAt(0) != 'L') {
            System.out.println("!! không tra được kiểu của pF -- chưa vá");
            return null;
        }

        // Sáu hằng mới nối vào cuối, mọi chỉ số cũ giữ nguyên ý nghĩa.
        int base = count;
        byte[] cls = HOOK_CLASS.getBytes("UTF-8");
        byte[] meth = HOOK_METHOD.getBytes("UTF-8");
        byte[] sig = ("()" + fieldDesc).getBytes("UTF-8");
        pool.writeByte(1); pool.writeShort(cls.length);  pool.write(cls);          // base
        pool.writeByte(7); pool.writeShort(base);                                  // base+1
        pool.writeByte(1); pool.writeShort(meth.length); pool.write(meth);         // base+2
        pool.writeByte(1); pool.writeShort(sig.length);  pool.write(sig);          // base+3
        pool.writeByte(12); pool.writeShort(base + 2); pool.writeShort(base + 3);  // base+4
        pool.writeByte(10); pool.writeShort(base + 1); pool.writeShort(base + 4);  // base+5
        int methodRef = base + 5;

        int second = at + 9;                       // getstatic thứ hai của vòng lặp
        write(rest, at, methodRef);
        write(rest, second, methodRef);

        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        DataOutputStream o = new DataOutputStream(bo);
        o.writeInt(0xCAFEBABE);
        o.writeShort(minor);
        o.writeShort(major);
        o.writeShort(count + 6);
        o.write(poolOut.toByteArray());
        o.write(rest);
        return bo.toByteArray();
    }

    /**
     * Trả về vị trí lệnh getstatic đầu tiên của vòng lặp, hoặc -1.
     *
     * Mẫu cần khớp, đúng như javac sinh ra cho "for (i = 0; i < v.size(); i++)":
     *   iconst_0 istore_2 iload_2 getstatic#X invokevirtual if_icmpge getstatic#X iload_2 invokevirtual
     * Hai lệnh getstatic phải cùng toán hạng, đó là dấu chắc chắn rằng cả hai đọc cùng một Vector.
     */
    private static int findLoop(byte[] code, int anchorString) {
        for (int i = 0; i + LOOKAHEAD < code.length; i++) {
            if ((code[i] & 0xFF) != LDC_W) {
                continue;
            }
            if ((((code[i + 1] & 0xFF) << 8) | (code[i + 2] & 0xFF)) != anchorString) {
                continue;
            }
            for (int k = i + 3; k < i + LOOKAHEAD && k + 20 < code.length; k++) {
                if ((code[k] & 0xFF) != 0x03 || (code[k + 1] & 0xFF) != 0x3D
                        || (code[k + 2] & 0xFF) != 0x1C) {
                    continue;
                }
                int a = k + 3;
                if ((code[a] & 0xFF) != GETSTATIC || (code[a + 9] & 0xFF) != GETSTATIC) {
                    continue;
                }
                if (code[a + 1] != code[a + 10] || code[a + 2] != code[a + 11]) {
                    continue;
                }
                return a;
            }
        }
        return -1;
    }

    private static void write(byte[] code, int at, int methodRef) {
        code[at] = (byte) INVOKESTATIC;
        code[at + 1] = (byte) (methodRef >> 8);
        code[at + 2] = (byte) methodRef;
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
