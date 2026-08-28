import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.nio.file.Files;

/**
 * Hạ ba id hiệu ứng của danh hiệu Akatsuki xuống dưới 256 để client nhận được.
 *
 * VÌ SAO PHẢI LÀM
 *
 * Char.updateEveryHalfSecond() gọi addEffect(this, 301|302|303, ...) cho người mặc món 1133.
 * Đường truyền cái id ấy có HAI chặng, và hai chặng dùng hai bề rộng khác nhau:
 *
 *   MapService.addEffect   ds.writeShort(id)                 -> 301 đi qua bình thường
 *   Service.sendImgEffect  isVersionAbove(239) ? writeShort  -> client 1.4.8 (=148) KHÔNG đạt
 *                                              : writeByte   -> 301 & 0xFF = 45
 *
 * Nên client được báo "hãy vẽ hiệu ứng 301" nhưng lại nhận ảnh dán nhãn 45. Nó đi tìm ảnh của
 * 301, không thấy, và không vẽ gì -- lặng lẽ, không lỗi. Mọi hiệu ứng đang chạy đều <= 255 nên
 * chưa bao giờ chạm phải trần này; 301/302/303 là ba cái đầu tiên vượt. Đây cũng là lý do danh
 * hiệu V_VIP của server gốc chưa bao giờ hiện gì.
 *
 * VÌ SAO VÁ BYTECODE CHỨ KHÔNG SỬA NGUỒN
 *
 * Char.java dùng Lombok nên không biên dịch lại được. Nhưng ở đây chỉ cần đổi GIÁ TRỊ của ba
 * lệnh sipush -- 2 byte mỗi lệnh, dài y hệt lệnh cũ. Không dời offset, không dựng lại
 * StackMapTable, không đụng bể hằng. Đây là loại vá rẻ nhất có thể.
 *
 * Mẫu tìm rất kín, và mỗi hằng chỉ xuất hiện đúng một lần trong cả lớp:
 *
 *     11 <id cũ>     sipush 301
 *     11 27 10       sipush 10000        <- thời gian sống, luôn đi liền sau
 *     03 03          iconst_0 iconst_0
 *     B6 <addEffect> invokevirtual MapService.addEffect(Object,IIII)V
 *
 * Usage: FixAkatsukiEffectId [Char.class] [idMới1 idMới2 idMới3]
 *        mặc định: build/srvpatch/com/nsoz/model/Char.class 211 212 213
 */
public final class FixAkatsukiEffectId {

    private static final int SIPUSH = 0x11, ICONST_0 = 0x03, INVOKEVIRTUAL = 0xB6;
    private static final int DURATION = 10000;

    /** Trần thật của id hiệu ứng, do chặng gửi ảnh ghi bằng một byte với client cũ. */
    private static final int ID_MAX = 255;

    private static final int[] OLD = {301, 302, 303};

    private FixAkatsukiEffectId() {
    }

    public static void main(String[] args) throws Exception {
        File f = new File(args.length > 0 ? args[0] : "build/srvpatch/com/nsoz/model/Char.class");
        int[] neu = {211, 212, 213};
        if (args.length >= 4) {
            for (int i = 0; i < 3; i++) {
                neu[i] = Integer.parseInt(args[i + 1]);
            }
        }
        for (int id : neu) {
            if (id < 0 || id > ID_MAX) {
                System.out.println("!! id mới " + id + " phải nằm trong 0.." + ID_MAX
                        + ": client cũ nhận id ảnh bằng một byte.");
                System.exit(1);
            }
        }

        byte[] d = Files.readAllBytes(f.toPath());
        int addEffect = methodRef(d, "addEffect", "(Ljava/lang/Object;IIII)V");
        if (addEffect < 0) {
            System.out.println("!! không thấy MapService.addEffect trong bể hằng -- chưa vá");
            return;
        }

        int done = 0;
        for (int i = 0; i < OLD.length; i++) {
            int at = find(d, OLD[i], addEffect);
            if (at < 0) {
                System.out.println("   bỏ qua " + OLD[i] + " -- không thấy (có thể đã vá rồi)");
                continue;
            }
            d[at + 1] = (byte) (neu[i] >> 8);
            d[at + 2] = (byte) neu[i];
            System.out.println("   " + OLD[i] + " -> " + neu[i] + " tại byte " + at);
            done++;
        }

        if (done == 0) {
            System.out.println("!! không chỗ nào khớp -- chưa ghi gì");
            return;
        }
        Files.write(f.toPath(), d);
        System.out.println("  đã vá " + done + "/3 hằng");
    }

    /** Vị trí lệnh sipush mang id cần đổi, hoặc -1. */
    private static int find(byte[] d, int id, int addEffect) {
        for (int i = 0; i + 11 <= d.length; i++) {
            if (u1(d, i) != SIPUSH || u2(d, i + 1) != id) {
                continue;
            }
            if (u1(d, i + 3) != SIPUSH || u2(d, i + 4) != DURATION) {
                continue;
            }
            if (u1(d, i + 6) != ICONST_0 || u1(d, i + 7) != ICONST_0) {
                continue;
            }
            if (u1(d, i + 8) != INVOKEVIRTUAL || u2(d, i + 9) != addEffect) {
                continue;
            }
            return i;
        }
        return -1;
    }

    private static int u1(byte[] d, int at) {
        return d[at] & 0xFF;
    }

    private static int u2(byte[] d, int at) {
        return ((d[at] & 0xFF) << 8) | (d[at + 1] & 0xFF);
    }

    /** Chỉ số Methodref theo tên và mô tả, hoặc -1. */
    private static int methodRef(byte[] d, String name, String desc) throws Exception {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(d));
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
}
