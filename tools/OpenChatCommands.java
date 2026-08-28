import java.io.DataInputStream;
import java.io.File;
import java.nio.file.Files;

/**
 * Mở lệnh chat của máy chủ cho mọi người chơi, không chỉ riêng tài khoản chủ server.
 *
 * Char.chat gác đường vào AdminService.process bằng hai điều kiện ghép cứng trong bytecode:
 *
 *     if (user != null && user.isTien() && user.username.equals("avada") && process(this, text))
 *
 * Char.java dùng Lombok nên không biên dịch lại được, phải vá thẳng. Mỗi lời gọi được thay bằng
 * "bỏ tham số rồi đẩy true", đúng 3 byte như cũ nên không lệnh nhảy nào xê dịch:
 *
 *     invokevirtual isTien()Z            -> pop  ; iconst_1 ; nop
 *     invokevirtual String.equals(...)Z  -> pop2 ; iconst_1 ; nop
 *
 * Chỉ vá đúng chỗ dẫn tới AdminService.process. Ba chỗ còn lại trong lớp -- lệnh "ban", lệnh
 * "menu", và mục "Quản lý" ở NPC Vua Hùng -- giữ nguyên khoá theo tên tài khoản. Việc lệnh nào
 * cho ai nằm ở AdminService.isAdmin(), sửa được mà không phải đụng bytecode nữa: lệnh cấp đồ và
 * cấp cấp độ vẫn đòi cột isTien trong bảng users.
 *
 * Usage: OpenChatCommands <Char.class>
 */
public final class OpenChatCommands {

    private static final int INVOKEVIRTUAL = 0xB6, INVOKESTATIC = 0xB8;
    private static final int POP = 0x57, POP2 = 0x58, ICONST_1 = 0x04, NOP = 0x00;
    /** Khoảng lùi tối đa từ lời gọi getInstance() về tới hai điều kiện phía trước. */
    private static final int BACK = 48;

    public static void main(String[] args) throws Exception {
        File f = new File(args.length > 0 ? args[0] : "build/srvpatch/com/nsoz/model/Char.class");
        byte[] d = Files.readAllBytes(f.toPath());

        int isTien = ref(d, "com/nsoz/model/User", "isTien", "()Z");
        int equals = ref(d, "java/lang/String", "equals", "(Ljava/lang/Object;)Z");
        int getInstance = ref(d, "com/nsoz/admin/AdminService", "getInstance", null);
        // Chỉ mở đúng cửa dẫn tới process(). AdminService.getInstance() còn được gọi ở chỗ khác
        // (adminActionPlayer -- thao tác quản trị lên người chơi khác), chỗ đó phải khoá nguyên.
        int process = ref(d, "com/nsoz/admin/AdminService", "process",
                "(Lcom/nsoz/model/Char;Ljava/lang/String;)Z");
        if (isTien < 0 || equals < 0 || getInstance < 0 || process < 0) {
            System.out.println("!! không tìm thấy đủ hằng cần thiết"
                    + " (isTien=" + isTien + " equals=" + equals
                    + " getInstance=" + getInstance + " process=" + process + ")");
            return;
        }

        int patched = 0;
        for (int i = 0; i + 2 < d.length; i++) {
            if ((d[i] & 0xFF) != INVOKESTATIC || index(d, i) != getInstance) {
                continue;
            }
            if (!callsProcess(d, i, process)) {
                continue;
            }
            int equalsAt = -1, isTienAt = -1;
            for (int k = i - 1; k >= 0 && k >= i - BACK; k--) {
                if ((d[k] & 0xFF) != INVOKEVIRTUAL) {
                    continue;
                }
                int target = index(d, k);
                if (target == equals && equalsAt == -1) {
                    equalsAt = k;
                } else if (target == isTien && equalsAt != -1) {
                    isTienAt = k;
                    break;
                }
            }
            if (equalsAt < 0 || isTienAt < 0) {
                continue;
            }
            write(d, equalsAt, POP2);
            write(d, isTienAt, POP);
            patched++;
            System.out.println("  đã mở cửa tại byte " + isTienAt + " và " + equalsAt);
        }

        if (patched != 1) {
            System.out.println("!! chờ đúng một chỗ, tìm thấy " + patched + " -- không ghi gì");
            return;
        }
        Files.write(f.toPath(), d);
        System.out.println("xong: mọi người chơi gõ được lệnh chat của máy chủ;"
                + " lệnh cấp phát vẫn do AdminService.isAdmin() quyết định");
    }

    /** Ngay sau getInstance() có phải là lời gọi process() không -- vài byte đẩy tham số ở giữa. */
    private static boolean callsProcess(byte[] d, int at, int process) {
        for (int k = at + 3; k < at + 12 && k + 2 < d.length; k++) {
            if ((d[k] & 0xFF) == INVOKEVIRTUAL && index(d, k) == process) {
                return true;
            }
        }
        return false;
    }

    private static void write(byte[] d, int at, int drop) {
        d[at] = (byte) drop;
        d[at + 1] = (byte) ICONST_1;
        d[at + 2] = (byte) NOP;
    }

    private static int index(byte[] d, int at) {
        return ((d[at + 1] & 0xFF) << 8) | (d[at + 2] & 0xFF);
    }

    /** Tìm chỉ số Methodref theo tên lớp, tên hàm và mô tả (mô tả null là bỏ qua). */
    private static int ref(byte[] d, String owner, String name, String desc) throws Exception {
        DataInputStream in = new DataInputStream(new java.io.ByteArrayInputStream(d));
        in.readInt();
        in.readUnsignedShort();
        in.readUnsignedShort();
        int count = in.readUnsignedShort();

        String[] utf8 = new String[count];
        int[] classNameIndex = new int[count];
        int[] refClass = new int[count], refNat = new int[count];
        int[] natName = new int[count], natDesc = new int[count];

        for (int i = 1; i < count; i++) {
            int tag = in.readUnsignedByte();
            switch (tag) {
                case 1: {
                    byte[] s = new byte[in.readUnsignedShort()];
                    in.readFully(s);
                    utf8[i] = new String(s, "UTF-8");
                    break;
                }
                case 7: classNameIndex[i] = in.readUnsignedShort(); break;
                case 8: case 16: case 19: case 20: in.readUnsignedShort(); break;
                case 15: in.readUnsignedByte(); in.readUnsignedShort(); break;
                case 5: case 6: in.readLong(); i++; break;
                case 10: case 11: refClass[i] = in.readUnsignedShort();
                                  refNat[i] = in.readUnsignedShort(); break;
                case 12: natName[i] = in.readUnsignedShort();
                         natDesc[i] = in.readUnsignedShort(); break;
                default: in.readInt(); break;
            }
        }
        for (int i = 1; i < count; i++) {
            if (refClass[i] == 0) {
                continue;
            }
            String cls = utf8[classNameIndex[refClass[i]]];
            String nm = utf8[natName[refNat[i]]];
            String ds = utf8[natDesc[refNat[i]]];
            if (owner.equals(cls) && name.equals(nm) && (desc == null || desc.equals(ds))) {
                return i;
            }
        }
        return -1;
    }
}
