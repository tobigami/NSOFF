import java.io.DataInputStream;
import java.io.File;
import java.nio.file.Files;

/**
 * Mở mục "Quản lý" ở NPC Vua Hùng cho mọi tài khoản có cột isTien = 1, thay vì đúng một tên.
 *
 * Char.npcVuaHung gác bằng hai điều kiện ghép cứng:
 *
 *     if (user.isTien() && user.username.equals("avada")) { menus.add(... "Quản lý" ...); }
 *
 * Chỉ vô hiệu vế SO TÊN, giữ nguyên vế isTien -- có vậy quyền mới bật tắt được từ cơ sở dữ liệu
 * mà không phải vá lại lần nữa. Lời gọi equals bị thay bằng "bỏ hai tham số rồi đẩy true", đúng
 * 3 byte như cũ nên không lệnh nhảy nào xê dịch.
 *
 * Nhận diện bằng chuỗi mốc đi kèm nhánh đó (mặc định "Quản lý"), nên không đụng nhầm ba chỗ khác
 * trong lớp cũng so tên theo cùng kiểu: lệnh chat "ban", lệnh "menu", và adminActionPlayer.
 *
 * Usage: OpenMenuGate <Char.class> [chuỗi mốc]
 */
public final class OpenMenuGate {

    private static final int LDC = 0x12, LDC_W = 0x13, INVOKEVIRTUAL = 0xB6;
    private static final int POP2 = 0x58, ICONST_1 = 0x04, NOP = 0x00;
    /** Khoảng lùi từ chuỗi mốc về tới lời gọi equals của nhánh đó. */
    private static final int BACK = 64;

    public static void main(String[] args) throws Exception {
        File f = new File(args.length > 0 ? args[0] : "build/srvpatch/com/nsoz/model/Char.class");
        String marker = args.length > 1 ? args[1] : "Quản lý";
        byte[] d = Files.readAllBytes(f.toPath());

        int equals = ref(d, "java/lang/String", "equals", "(Ljava/lang/Object;)Z");
        int markerString = stringOf(d, marker);
        if (equals < 0 || markerString < 0) {
            System.out.println("!! không tìm thấy hằng cần thiết (equals=" + equals
                    + " \"" + marker + "\"=" + markerString + ")");
            return;
        }

        int patched = 0;
        for (int i = 0; i + 2 < d.length; i++) {
            if (!pushes(d, i, markerString)) {
                continue;
            }
            for (int k = i - 1; k >= 0 && k >= i - BACK; k--) {
                if ((d[k] & 0xFF) != INVOKEVIRTUAL || index(d, k) != equals) {
                    continue;
                }
                d[k] = (byte) POP2;
                d[k + 1] = (byte) ICONST_1;
                d[k + 2] = (byte) NOP;
                patched++;
                System.out.println("  đã bỏ so tên tại byte " + k
                        + " (trước chuỗi \"" + marker + "\" ở byte " + i + ")");
                break;
            }
        }

        if (patched != 1) {
            System.out.println("!! chờ đúng một chỗ, tìm thấy " + patched + " -- không ghi gì");
            return;
        }
        Files.write(f.toPath(), d);
        System.out.println("xong: mục \"" + marker + "\" giờ mở cho mọi tài khoản có isTien = 1");
    }

    /** Lệnh tại vị trí này có đẩy đúng hằng chuỗi đó lên ngăn xếp không. */
    private static boolean pushes(byte[] d, int at, int stringIndex) {
        int op = d[at] & 0xFF;
        if (op == LDC) {
            return (d[at + 1] & 0xFF) == stringIndex;
        }
        if (op == LDC_W) {
            return (((d[at + 1] & 0xFF) << 8) | (d[at + 2] & 0xFF)) == stringIndex;
        }
        return false;
    }

    private static int index(byte[] d, int at) {
        return ((d[at + 1] & 0xFF) << 8) | (d[at + 2] & 0xFF);
    }

    /** Chỉ số hằng String trỏ tới đúng nội dung này. */
    private static int stringOf(byte[] d, String text) throws Exception {
        Pool p = new Pool(d);
        int utf8 = -1;
        for (int i = 1; i < p.count; i++) {
            if (text.equals(p.utf8[i])) {
                utf8 = i;
                break;
            }
        }
        if (utf8 < 0) {
            return -1;
        }
        for (int i = 1; i < p.count; i++) {
            if (p.stringTarget[i] == utf8) {
                return i;
            }
        }
        return -1;
    }

    /** Chỉ số Methodref theo lớp, tên hàm và mô tả. */
    private static int ref(byte[] d, String owner, String name, String desc) throws Exception {
        Pool p = new Pool(d);
        for (int i = 1; i < p.count; i++) {
            if (p.refClass[i] == 0) {
                continue;
            }
            String cls = p.utf8[p.classNameIndex[p.refClass[i]]];
            String nm = p.utf8[p.natName[p.refNat[i]]];
            String ds = p.utf8[p.natDesc[p.refNat[i]]];
            if (owner.equals(cls) && name.equals(nm) && desc.equals(ds)) {
                return i;
            }
        }
        return -1;
    }

    /** Constant pool đã tách sẵn những phần cần tra. */
    private static final class Pool {
        final int count;
        final String[] utf8;
        final int[] classNameIndex, stringTarget, refClass, refNat, natName, natDesc;

        Pool(byte[] d) throws Exception {
            DataInputStream in = new DataInputStream(new java.io.ByteArrayInputStream(d));
            in.readInt();
            in.readUnsignedShort();
            in.readUnsignedShort();
            count = in.readUnsignedShort();
            utf8 = new String[count];
            classNameIndex = new int[count];
            stringTarget = new int[count];
            refClass = new int[count];
            refNat = new int[count];
            natName = new int[count];
            natDesc = new int[count];

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
                    case 8: stringTarget[i] = in.readUnsignedShort(); break;
                    case 16: case 19: case 20: in.readUnsignedShort(); break;
                    case 15: in.readUnsignedByte(); in.readUnsignedShort(); break;
                    case 5: case 6: in.readLong(); i++; break;
                    case 10: case 11:
                        refClass[i] = in.readUnsignedShort();
                        refNat[i] = in.readUnsignedShort();
                        break;
                    case 12:
                        natName[i] = in.readUnsignedShort();
                        natDesc[i] = in.readUnsignedShort();
                        break;
                    default: in.readInt(); break;
                }
            }
        }
    }
}
