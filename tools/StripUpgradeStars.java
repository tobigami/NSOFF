import java.io.DataInputStream;
import java.io.File;
import java.nio.file.Files;

/**
 * Bỏ dãy hình thoi vẽ theo mức nâng cấp trong khung thông tin vật phẩm.
 *
 * Khung đó tính số hình thoi bằng "nâng_cấp / 2 + 1" rồi vẽ lặp, mỗi cái cách nhau 10 pixel. Với
 * đồ +24 là 13 cái, rộng 130 pixel -- vượt bề ngang khung, nên khung co giãn liên tục mỗi khung
 * hình và chữ nhấp nháy không đọc được. Đồ trong game gốc tối đa +16 nên tác giả không gặp.
 *
 * Cách vá: đặt thẳng biến đếm đó về 0. Sáu vòng vẽ trong hàm đều lấy chung biến này làm cận trên,
 * nên chỉ cần một chỗ là tắt hết, không phải đụng vào từng vòng.
 *
 *     aload_2; getfield qq; iconst_2; idiv; iconst_1; iadd; istore 4     (10 byte)
 *  -> iconst_0; istore 4; nop x7                                        (10 byte)
 *
 * Đúng 10 byte như cũ nên không lệnh nhảy nào xê dịch, và ngăn xếp vẫn cân bằng.
 *
 * Mấy chỗ vẽ một hình thoi lẻ (nhánh "nâng cấp = 3" chẳng hạn) giữ nguyên: chúng không lặp nên
 * không làm tràn khung, và bỏ luôn thì mất hẳn dấu hiệu món đã nâng cấp.
 *
 * Usage: StripUpgradeStars <jar vào> <jar ra>
 */
public final class StripUpgradeStars {

    private static final int ALOAD_2 = 0x2C, GETFIELD = 0xB4, ICONST_2 = 0x05, IDIV = 0x6C;
    private static final int ICONST_1 = 0x04, IADD = 0x60, ISTORE = 0x36;
    private static final int ICONST_0 = 0x03, NOP = 0x00;

    public static void main(String[] args) throws Exception {
        File in = new File(args[0]);
        File out = new File(args[1]);
        java.util.zip.ZipFile zip = new java.util.zip.ZipFile(in);
        java.io.OutputStream os = Files.newOutputStream(out.toPath());
        java.util.zip.ZipOutputStream zo = new java.util.zip.ZipOutputStream(os);

        int patched = 0;
        java.util.Enumeration<? extends java.util.zip.ZipEntry> es = zip.entries();
        while (es.hasMoreElements()) {
            java.util.zip.ZipEntry e = es.nextElement();
            byte[] data = read(zip.getInputStream(e));
            if (e.getName().endsWith(".class")) {
                patched += patch(data);
            }
            zo.putNextEntry(new java.util.zip.ZipEntry(e.getName()));
            zo.write(data);
            zo.closeEntry();
        }
        zo.close();
        zip.close();

        if (patched == 1) {
            System.out.println("  đã tắt dãy hình thoi theo mức nâng cấp");
        } else {
            System.out.println("!! chờ đúng một chỗ, tìm thấy " + patched
                    + " -- kiểm lại trước khi phát hành");
        }
    }

    /** Trả về số chỗ đã vá trong một lớp. */
    private static int patch(byte[] d) throws Exception {
        int qq = fieldRef(d, "qq", "I");
        if (qq < 0) {
            return 0;
        }
        int n = 0;
        for (int i = 0; i + 9 < d.length; i++) {
            if ((d[i] & 0xFF) != ALOAD_2 || (d[i + 1] & 0xFF) != GETFIELD) {
                continue;
            }
            int ref = ((d[i + 2] & 0xFF) << 8) | (d[i + 3] & 0xFF);
            if (ref != qq) {
                continue;
            }
            if ((d[i + 4] & 0xFF) != ICONST_2 || (d[i + 5] & 0xFF) != IDIV
                    || (d[i + 6] & 0xFF) != ICONST_1 || (d[i + 7] & 0xFF) != IADD
                    || (d[i + 8] & 0xFF) != ISTORE) {
                continue;
            }
            int slot = d[i + 9] & 0xFF;
            d[i] = (byte) ICONST_0;
            d[i + 1] = (byte) ISTORE;
            d[i + 2] = (byte) slot;
            for (int k = i + 3; k <= i + 9; k++) {
                d[k] = (byte) NOP;
            }
            n++;
        }
        return n;
    }

    /** Chỉ số Fieldref theo tên và mô tả, bất kể lớp chủ. */
    private static int fieldRef(byte[] d, String name, String desc) throws Exception {
        DataInputStream in = new DataInputStream(new java.io.ByteArrayInputStream(d));
        if (in.readInt() != 0xCAFEBABE) {
            return -1;
        }
        in.readUnsignedShort();
        in.readUnsignedShort();
        int count = in.readUnsignedShort();
        String[] utf8 = new String[count];
        int[] refNat = new int[count], natName = new int[count], natDesc = new int[count];
        boolean[] isField = new boolean[count];

        for (int i = 1; i < count; i++) {
            int tag = in.readUnsignedByte();
            switch (tag) {
                case 1: {
                    byte[] s = new byte[in.readUnsignedShort()];
                    in.readFully(s);
                    utf8[i] = new String(s, "UTF-8");
                    break;
                }
                case 7: case 8: case 16: case 19: case 20: in.readUnsignedShort(); break;
                case 15: in.readUnsignedByte(); in.readUnsignedShort(); break;
                case 5: case 6: in.readLong(); i++; break;
                case 9: isField[i] = true; in.readUnsignedShort();
                        refNat[i] = in.readUnsignedShort(); break;
                case 10: case 11: in.readUnsignedShort();
                         refNat[i] = in.readUnsignedShort(); break;
                case 12: natName[i] = in.readUnsignedShort();
                         natDesc[i] = in.readUnsignedShort(); break;
                default: in.readInt(); break;
            }
        }
        for (int i = 1; i < count; i++) {
            if (!isField[i] || refNat[i] == 0) {
                continue;
            }
            if (name.equals(utf8[natName[refNat[i]]]) && desc.equals(utf8[natDesc[refNat[i]]])) {
                return i;
            }
        }
        return -1;
    }

    private static byte[] read(java.io.InputStream in) throws Exception {
        java.io.ByteArrayOutputStream b = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        for (int n; (n = in.read(buf)) > 0; ) {
            b.write(buf, 0, n);
        }
        in.close();
        return b.toByteArray();
    }
}
