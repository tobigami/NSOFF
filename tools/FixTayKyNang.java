import java.io.DataInputStream;
import java.io.File;
import java.nio.file.Files;

/**
 * Sửa hai lỗi ở nhánh dùng giấy phép tẩy điểm và sách kỹ năng sơ cấp trong Char.
 *
 * Lỗi 1 -- báo sai số. Nhánh vật phẩm 241 (giấy phép tẩy kỹ năng) tăng đúng biến tayKyNang, nhưng
 * câu thông báo lại in biến tayTiemNang:
 *
 *     this.tayKyNang++;
 *     serverMessage(String.format("Số lần tẩy điểm kỹ năng của bạn là %d", tayTiemNang));
 *
 * Người chơi dùng sách xong thấy con số không nhúc nhích nên tưởng vật phẩm hỏng, trong khi số
 * lần tẩy thật sự đã tăng. Lỗi chép nhầm biến từ nhánh 240 ngay phía trên.
 *
 * Lỗi 2 -- ăn sạch chồng đồ. Cả ba nhánh 240, 241 và 252 đều gọi:
 *
 *     removeItem(item.index, item.getQuantity(), true);
 *
 * getQuantity() là số lượng của cả chồng, nên dùng một quyển trong chồng mười quyển thì mất cả
 * mười mà chỉ được cộng một lần. Đổi thành hằng 1.
 *
 * Char.java dùng Lombok nên không biên dịch lại được, phải vá thẳng bytecode. Cả hai chỗ đều thay
 * bằng chuỗi lệnh dài đúng bằng chuỗi cũ, không byte nào xê dịch:
 *
 *     getfield tayTiemNang            -> getfield tayKyNang        (3 byte đổi chỉ số)
 *     aload_1; invokevirtual getQty   -> iconst_1; nop; nop; nop   (4 byte)
 *
 * Định vị bằng chính câu thông báo của từng nhánh, không dò theo mẫu chung: dạng
 * removeItem(index, getQuantity(), true) còn xuất hiện ở nhiều chỗ khác trong lớp, nơi việc dùng
 * hết cả chồng là cố ý.
 *
 * Usage: FixTayKyNang [Char.class]
 */
public final class FixTayKyNang {

    private static final int GETFIELD = 0xB4, PUTFIELD = 0xB5;
    private static final int ALOAD_1 = 0x2B, INVOKEVIRTUAL = 0xB6, ICONST_1 = 0x04, NOP = 0x00;
    private static final int LDC_W = 0x13;

    private static final String MSG_KY_NANG = "Số lần tẩy điểm kỹ năng của bạn là %d";
    private static final String MSG_TIEM_NANG = "Số lần tẩy điểm tiềm năng của bạn là %d";
    private static final String MSG_SACH = "Bạn nhận được 1 điểm kỹ năng.";

    public static void main(String[] args) throws Exception {
        File f = new File(args.length > 0 ? args[0] : "build/srvpatch/com/nsoz/model/Char.class");
        byte[] d = Files.readAllBytes(f.toPath());

        int kyNang = stringConst(d, MSG_KY_NANG);
        int tiemNang = stringConst(d, MSG_TIEM_NANG);
        int sach = stringConst(d, MSG_SACH);
        if (kyNang < 0 || tiemNang < 0 || sach < 0) {
            System.out.println("!! không thấy đủ ba câu thông báo mốc -- chưa vá");
            return;
        }

        int atKyNang = findLdc(d, kyNang);
        if (atKyNang < 0) {
            System.out.println("!! không thấy chỗ đẩy câu thông báo tẩy kỹ năng -- chưa vá");
            return;
        }
        boolean ok = fixField(d, atKyNang);
        int n = 0;
        n += fixQuantity(d, findLdc(d, tiemNang)) ? 1 : 0;
        n += fixQuantity(d, atKyNang) ? 1 : 0;
        n += fixQuantity(d, findLdc(d, sach)) ? 1 : 0;

        // Quét nốt mọi chỗ còn lại trong hàm dùng vật phẩm. Mẫu 13 byte dưới đây rất kín: phải
        // đúng aload_0 aload_1 getfield <index> aload_1 invokevirtual <getQuantity>
        // iconst_1 invokevirtual <removeItem>, với hai chỉ số hằng tra thẳng theo tên và mô tả.
        // Các lời gọi truyền false (iconst_0) không khớp nên không bị đụng tới.
        int qty = methodRef(d, "getQuantity", "()I");
        int rem = methodRef(d, "removeItem", "(IIZ)V");
        int quet = qty > 0 && rem > 0 ? sweep(d, qty, rem) : 0;
        n += quet;

        if (!ok && n == 0) {
            System.out.println("!! không chỗ nào khớp -- có thể đã vá rồi");
            return;
        }
        Files.write(f.toPath(), d);
        System.out.println((ok ? "  sửa biến in ra ở thông báo tẩy kỹ năng\n" : "")
                + "  chỉ trừ 1 cái thay vì cả chồng: " + n + " chỗ");
    }

    /**
     * Đổi getfield của câu thông báo sang đúng trường mà nhánh vừa tăng.
     *
     * Chỉ số trường lấy ngay từ lệnh putfield của phép tăng phía trên -- khỏi phải tra tên trường
     * trong bể hằng, và chắc chắn khớp đúng cái nhánh này thao tác.
     */
    private static boolean fixField(byte[] d, int atLdc) {
        int inc = -1;
        for (int i = atLdc; i > atLdc - 64 && i > 0; i--) {
            if ((d[i] & 0xFF) == PUTFIELD) {
                inc = ((d[i + 1] & 0xFF) << 8) | (d[i + 2] & 0xFF);
                break;
            }
        }
        if (inc < 0) {
            return false;
        }
        for (int i = atLdc + 3; i < atLdc + 24 && i + 2 < d.length; i++) {
            if ((d[i] & 0xFF) != GETFIELD) {
                continue;
            }
            int cur = ((d[i + 1] & 0xFF) << 8) | (d[i + 2] & 0xFF);
            if (cur == inc) {
                return false;                      // đã đúng rồi, hoặc đã vá trước đó
            }
            d[i + 1] = (byte) (inc >> 8);
            d[i + 2] = (byte) inc;
            return true;
        }
        return false;
    }

    /**
     * Tìm ngược từ câu thông báo về lệnh gọi removeItem của cùng nhánh rồi ghim số lượng thành 1.
     *
     * Mẫu: aload_0 aload_1 getfield index aload_1 invokevirtual getQuantity iconst_1
     *      invokevirtual removeItem
     */
    private static boolean fixQuantity(byte[] d, int atLdc) {
        if (atLdc < 0) {
            return false;
        }
        for (int i = atLdc; i > atLdc - 80 && i > 6; i--) {
            if ((d[i] & 0xFF) != ALOAD_1 || (d[i + 1] & 0xFF) != INVOKEVIRTUAL) {
                continue;
            }
            if ((d[i + 4] & 0xFF) != ICONST_1 || (d[i + 5] & 0xFF) != INVOKEVIRTUAL) {
                continue;
            }
            d[i] = (byte) ICONST_1;
            d[i + 1] = (byte) NOP;
            d[i + 2] = (byte) NOP;
            d[i + 3] = (byte) NOP;
            return true;
        }
        return false;
    }

    /** Ghim số lượng thành 1 ở mọi chỗ còn khớp mẫu. Trả về số chỗ đã sửa. */
    private static int sweep(byte[] d, int qty, int rem) {
        int n = 0;
        for (int i = 0; i + 13 <= d.length; i++) {
            if ((d[i] & 0xFF) != 0x2A || (d[i + 1] & 0xFF) != ALOAD_1
                    || (d[i + 2] & 0xFF) != GETFIELD) {
                continue;
            }
            if ((d[i + 5] & 0xFF) != ALOAD_1 || (d[i + 6] & 0xFF) != INVOKEVIRTUAL
                    || ref(d, i + 6) != qty) {
                continue;
            }
            if ((d[i + 9] & 0xFF) != ICONST_1 || (d[i + 10] & 0xFF) != INVOKEVIRTUAL
                    || ref(d, i + 10) != rem) {
                continue;
            }
            d[i + 5] = (byte) ICONST_1;
            d[i + 6] = (byte) NOP;
            d[i + 7] = (byte) NOP;
            d[i + 8] = (byte) NOP;
            n++;
        }
        return n;
    }

    private static int ref(byte[] d, int at) {
        return ((d[at + 1] & 0xFF) << 8) | (d[at + 2] & 0xFF);
    }

    /** Chỉ số Methodref theo tên và mô tả, hoặc -1. */
    private static int methodRef(byte[] d, String name, String desc) throws Exception {
        DataInputStream in = new DataInputStream(new java.io.ByteArrayInputStream(d));
        in.readInt();
        in.readUnsignedShort();
        in.readUnsignedShort();
        int count = in.readUnsignedShort();
        String[] utf8 = new String[count];
        int[] natName = new int[count], natDesc = new int[count], refNat = new int[count];
        for (int i = 1; i < count; i++) {
            int tag = in.readUnsignedByte();
            switch (tag) {
                case 1: {
                    byte[] b = new byte[in.readUnsignedShort()];
                    in.readFully(b);
                    utf8[i] = new String(b, "UTF-8");
                    break;
                }
                case 10: case 11: {
                    in.readUnsignedShort();
                    refNat[i] = in.readUnsignedShort();
                    break;
                }
                case 12: {
                    natName[i] = in.readUnsignedShort();
                    natDesc[i] = in.readUnsignedShort();
                    break;
                }
                case 5: case 6: in.skipBytes(8); i++; break;
                case 8: case 7: case 16: case 19: case 20: in.skipBytes(2); break;
                case 9: case 3: case 4: case 17: case 18: in.skipBytes(4); break;
                case 15: in.skipBytes(3); break;
                default: throw new IllegalStateException("tag lạ " + tag);
            }
        }
        for (int i = 1; i < count; i++) {
            int nat = refNat[i];
            if (nat <= 0 || nat >= count) {
                continue;
            }
            if (name.equals(utf8[natName[nat]]) && desc.equals(utf8[natDesc[nat]])) {
                return i;
            }
        }
        return -1;
    }

    /** Chỉ số hằng String trỏ tới chuỗi cho trước, hoặc -1. */
    private static int stringConst(byte[] d, String want) throws Exception {
        DataInputStream in = new DataInputStream(new java.io.ByteArrayInputStream(d));
        in.readInt();
        in.readUnsignedShort();
        in.readUnsignedShort();
        int count = in.readUnsignedShort();
        String[] utf8 = new String[count];
        int[] target = new int[count];
        for (int i = 1; i < count; i++) {
            int tag = in.readUnsignedByte();
            switch (tag) {
                case 1: {
                    byte[] s = new byte[in.readUnsignedShort()];
                    in.readFully(s);
                    utf8[i] = new String(s, "UTF-8");
                    break;
                }
                case 8: target[i] = in.readUnsignedShort(); break;
                case 5: case 6: in.skipBytes(8); i++; break;
                case 9: case 10: case 11: case 12: case 3: case 4: case 17: case 18:
                    in.skipBytes(4); break;
                case 15: in.skipBytes(3); break;
                case 7: case 16: case 19: case 20: in.skipBytes(2); break;
                default: throw new IllegalStateException("tag lạ " + tag);
            }
        }
        for (int i = 1; i < count; i++) {
            if (target[i] > 0 && want.equals(utf8[target[i]])) {
                return i;
            }
        }
        return -1;
    }

    private static int findLdc(byte[] d, int constIndex) {
        for (int i = 0; i + 2 < d.length; i++) {
            if ((d[i] & 0xFF) == LDC_W
                    && (((d[i + 1] & 0xFF) << 8) | (d[i + 2] & 0xFF)) == constIndex) {
                return i;
            }
        }
        return -1;
    }
}
