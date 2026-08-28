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
 * Đổi một lệnh chat sẵn có của client thành lệnh gọi lớp của mình.
 *
 * Client giữ danh sách lệnh dưới dạng một chuỗi các so sánh text.equals("<lệnh>"), nhánh nào khớp
 * thì gọi hàm rồi return. Chỗ này lợi dụng đúng cấu trúc đó: giữ nguyên lệnh invokestatic, chỉ
 * đổi chỉ số hằng mà nó trỏ tới. Không byte nào xê dịch nên mọi lệnh nhảy trong phương thức vẫn
 * đúng chỗ -- thứ luôn hỏng khi chèn thêm mã vào bytecode.
 *
 * Tên lệnh mới phải dài đúng bằng tên cũ, vì nó được ghi đè ngay tại chỗ trong hằng UTF8.
 *
 * Usage: HookCommand <in.jar> <out.jar> <lệnh cũ> <lệnh mới> <Lớp.hàm> <thư mục .class cần nhét>
 *   ví dụ: HookCommand in.jar out.jar hds dan Dan.fill build/modcls
 */
public final class HookCommand {

    private static final int LDC = 0x12, LDC_W = 0x13;
    private static final int IFEQ = 0x99, INVOKESTATIC = 0xB8;
    /** Bao nhiêu byte sau khi đẩy tên lệnh thì còn coi là cùng một nhánh so sánh. */
    private static final int LOOKAHEAD = 16;

    public static void main(String[] args) throws Exception {
        if (args.length < 6) {
            System.out.println("usage: HookCommand <in.jar> <out.jar> <old> <new> <Class.method> <classesDir>");
            return;
        }
        String oldCmd = args[2], newCmd = args[3];
        if (oldCmd.length() != newCmd.length()) {
            System.out.println("!! tên lệnh mới phải dài đúng bằng tên cũ (" + oldCmd.length() + " ký tự)");
            return;
        }
        int dot = args[4].lastIndexOf('.');
        String hookClass = args[4].substring(0, dot), hookMethod = args[4].substring(dot + 1);

        ZipFile zip = new ZipFile(new File(args[0]));
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(new File(args[1])));
        boolean done = false;
        Enumeration<? extends ZipEntry> it = zip.entries();
        while (it.hasMoreElements()) {
            ZipEntry e = it.nextElement();
            byte[] data = readAll(zip.getInputStream(e));
            if (!done && e.getName().endsWith(".class")) {
                byte[] next = patch(data, oldCmd, newCmd, hookClass, hookMethod, e.getName());
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

        File dir = new File(args[5]);
        File[] extra = dir.listFiles();
        for (int i = 0; extra != null && i < extra.length; i++) {
            if (!extra[i].getName().endsWith(".class")) {
                continue;
            }
            zos.putNextEntry(new ZipEntry(extra[i].getName()));
            zos.write(java.nio.file.Files.readAllBytes(extra[i].toPath()));
            zos.closeEntry();
            System.out.println("  nhét vào jar: " + extra[i].getName());
        }
        zos.close();

        System.out.println(done
                ? "lệnh \"" + oldCmd + "\" -> \"" + newCmd + "\" gọi " + hookClass + "." + hookMethod + "()"
                : "!! không tìm thấy nhánh của lệnh \"" + oldCmd + "\"");
    }

    /** Trả về lớp đã sửa, hoặc null nếu lớp này không chứa lệnh cần tìm. */
    private static byte[] patch(byte[] d, String oldCmd, String newCmd,
            String hookClass, String hookMethod, String name) throws Exception {
        DataInputStream in = new DataInputStream(new java.io.ByteArrayInputStream(d));
        if (in.readInt() != 0xCAFEBABE) {
            return null;
        }
        int minor = in.readUnsignedShort(), major = in.readUnsignedShort();
        int count = in.readUnsignedShort();

        ByteArrayOutputStream poolOut = new ByteArrayOutputStream();
        DataOutputStream pool = new DataOutputStream(poolOut);
        int cmdUtf8 = -1;
        int[] stringTarget = new int[count];
        // Ghi lại đủ thứ để sau đó tra được: Methodref -> NameAndType -> chuỗi mô tả.
        String[] utf8 = new String[count];
        int[] refNameAndType = new int[count];
        int[] natDescriptor = new int[count];

        for (int i = 1; i < count; i++) {
            int tag = in.readUnsignedByte();
            pool.writeByte(tag);
            switch (tag) {
                case 1: {
                    byte[] s = new byte[in.readUnsignedShort()];
                    in.readFully(s);
                    utf8[i] = new String(s, "UTF-8");
                    if (utf8[i].equals(oldCmd)) {
                        cmdUtf8 = i;
                        s = newCmd.getBytes("UTF-8");   // ghi đè tại chỗ, cùng độ dài
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
                case 5: case 6: copy(in, pool, 8); i++; break;
                case 10: case 11: {                       // Methodref / InterfaceMethodref
                    int owner = in.readUnsignedShort();
                    int nat = in.readUnsignedShort();
                    refNameAndType[i] = nat;
                    pool.writeShort(owner);
                    pool.writeShort(nat);
                    break;
                }
                case 12: {                                 // NameAndType
                    int nameIdx = in.readUnsignedShort();
                    int descIdx = in.readUnsignedShort();
                    natDescriptor[i] = descIdx;
                    pool.writeShort(nameIdx);
                    pool.writeShort(descIdx);
                    break;
                }
                case 3: case 4: case 9: case 17: case 18:
                    copy(in, pool, 4); break;
                case 15: copy(in, pool, 3); break;
                case 7: case 16: case 19: case 20: copy(in, pool, 2); break;
                default: throw new IllegalStateException(name + ": tag lạ " + tag);
            }
        }
        if (cmdUtf8 == -1) {
            return null;
        }
        int cmdString = -1;
        for (int i = 1; i < count; i++) {
            if (stringTarget[i] == cmdUtf8) {
                cmdString = i;
                break;
            }
        }
        if (cmdString == -1) {
            return null;
        }

        byte[] rest = readAll(in);

        // Sáu hằng mới, nối vào cuối để mọi chỉ số cũ giữ nguyên ý nghĩa:
        // Utf8 tên lớp, Class, Utf8 tên hàm, Utf8 "()V", NameAndType, Methodref.
        int base = count;
        byte[] cls = hookClass.getBytes("UTF-8"), meth = hookMethod.getBytes("UTF-8"), sig = "()V".getBytes("UTF-8");
        pool.writeByte(1); pool.writeShort(cls.length);  pool.write(cls);      // base
        pool.writeByte(7); pool.writeShort(base);                              // base+1
        pool.writeByte(1); pool.writeShort(meth.length); pool.write(meth);     // base+2
        pool.writeByte(1); pool.writeShort(sig.length);  pool.write(sig);      // base+3
        pool.writeByte(12); pool.writeShort(base + 2); pool.writeShort(base + 3); // base+4
        pool.writeByte(10); pool.writeShort(base + 1); pool.writeShort(base + 4); // base+5
        int methodRef = base + 5;

        String[] descriptors = new String[count];
        for (int i = 1; i < count; i++) {
            int nat = refNameAndType[i];
            if (nat > 0 && nat < count) {
                int descIndex = natDescriptor[nat];
                descriptors[i] = descIndex > 0 && descIndex < count ? utf8[descIndex] : null;
            }
        }

        if (!repoint(rest, cmdString, methodRef, descriptors)) {
            System.out.println("!! thấy tên lệnh nhưng nhánh của nó không mở đầu bằng "
                    + "một lệnh gọi hàm ()V -- chưa vá");
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
        return bo.toByteArray();
    }

    /**
     * Tìm chỗ đẩy tên lệnh, dò tới lệnh ifeq đầu tiên của nhánh đó, rồi đổi toán hạng của lệnh
     * invokestatic ngay sau nó. Chỉ nhận khi hàm bị thay có mô tả ()V.
     */
    private static boolean repoint(byte[] code, int cmdString, int methodRef, String[] descriptors) {
        for (int i = 0; i + LOOKAHEAD < code.length; i++) {
            int push = code[i] & 0xFF;
            int at;
            if (push == LDC && (code[i + 1] & 0xFF) == cmdString) {
                at = i + 2;
            } else if (push == LDC_W && (((code[i + 1] & 0xFF) << 8) | (code[i + 2] & 0xFF)) == cmdString) {
                at = i + 3;
            } else {
                continue;
            }
            for (int k = at; k < at + LOOKAHEAD && k + 4 < code.length; k++) {
                if ((code[k] & 0xFF) != IFEQ) {
                    continue;
                }
                int body = k + 3;
                if ((code[body] & 0xFF) != INVOKESTATIC) {
                    break;
                }
                int target = ((code[body + 1] & 0xFF) << 8) | (code[body + 2] & 0xFF);
                String desc = target < descriptors.length ? descriptors[target] : null;
                if (!"()V".equals(desc)) {
                    System.out.println("!! hàm bị thay có mô tả " + desc + ", không phải ()V -- bỏ qua");
                    break;
                }
                code[body + 1] = (byte) (methodRef >> 8);
                code[body + 2] = (byte) methodRef;
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
