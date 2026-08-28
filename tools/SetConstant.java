import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;

/**
 * Đổi một hằng chuỗi trong một file .class.
 *
 * Dùng để mở cổng vào menu quản lý trong game: Char.npcVuaHung() so tên tài khoản với chuỗi
 * "nsokeny" viết cứng trong mã, mà Char.java không dịch lại được (cần Lombok, và ở đây không có
 * mạng để tải). Sửa thẳng hằng số thì không phải dịch gì cả.
 *
 * Chuỗi mới dài ngắn thế nào cũng được: bảng hằng đánh địa chỉ bằng chỉ mục chứ không bằng vị trí
 * byte, nên chỉ cần ghi lại cả file theo đúng thứ tự. Phần sau bảng hằng chép nguyên si.
 *
 * Usage: SetConstant <file.class> <chuỗi cũ> <chuỗi mới>
 */
public final class SetConstant {

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("usage: SetConstant <file.class> <old> <new>");
            return;
        }
        File f = new File(args[0]);
        String from = args[1];
        String to = args[2];
        byte[] d = Files.readAllBytes(f.toPath());

        DataInputStream in = new DataInputStream(new java.io.ByteArrayInputStream(d));
        if (in.readInt() != 0xCAFEBABE) {
            System.out.println("!! " + f.getName() + " không phải file .class");
            return;
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

        int hits = 0;
        for (int i = 1; i < count; i++) {
            int tag = in.readUnsignedByte();
            o.writeByte(tag);
            switch (tag) {
                case 1: {                        // UTF8
                    byte[] s = new byte[in.readUnsignedShort()];
                    in.readFully(s);
                    if (new String(s, "UTF-8").equals(from)) {
                        s = to.getBytes("UTF-8");
                        hits++;
                    }
                    o.writeShort(s.length);
                    o.write(s);
                    break;
                }
                case 5:                          // Long
                case 6:                          // Double
                    copy(in, o, 8);
                    i++;                         // chiếm hai ô
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
                    throw new IllegalStateException("thẻ hằng lạ: " + tag + " ở " + i);
            }
        }
        if (hits != 1) {
            System.out.println("!! tìm thấy " + hits + " hằng \"" + from
                    + "\" (mong đợi đúng 1) -- không đổi gì");
            return;
        }
        byte[] rest = new byte[in.available()];
        in.readFully(rest);
        o.write(rest);
        o.flush();

        FileOutputStream out = new FileOutputStream(f);
        out.write(bo.toByteArray());
        out.close();
        System.out.println("  " + f.getName() + ": \"" + from + "\" -> \"" + to + "\"");
    }

    private static void copy(DataInputStream in, DataOutputStream o, int n) throws Exception {
        byte[] b = new byte[n];
        in.readFully(b);
        o.write(b);
    }
}
