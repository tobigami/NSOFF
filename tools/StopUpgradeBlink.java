import java.io.File;
import java.nio.file.Files;

/**
 * Tắt hiệu ứng nhấp nháy tên món trong khung thông tin vật phẩm.
 *
 * Với đồ nâng cấp từ +15 trở lên, client vẽ tên món LẦN THỨ HAI bằng màu khác ở những khung hình
 * xen kẽ -- ý đồ ban đầu chắc là cho đồ xịn lấp lánh. Bộ +24 của menu NPC Vua Hùng luôn vượt mốc
 * đó nên chữ chớp đỏ trắng không ngừng, đọc không nổi.
 *
 * Nhận diện theo đúng bốn lệnh liền nhau, chứ không tìm mỗi số 15 (số đó còn dùng ở chỗ chọn màu
 * hình thoi):
 *
 *     bipush 15 ; if_icmplt X ; getstatic <cờ đảo khung hình> ; ifne X
 *
 * Vá bằng cách nâng mốc 15 lên 127 -- không món nào đạt tới, nên nhánh vẽ lần hai không bao giờ
 * chạy. Đổi đúng một byte toán hạng, không lệnh nào xê dịch.
 *
 * Usage: StopUpgradeBlink <jar vào> <jar ra>
 */
public final class StopUpgradeBlink {

    private static final int BIPUSH = 0x10, IF_ICMPLT = 0xA1, GETSTATIC = 0xB2, IFNE = 0x9A;
    private static final int MOC_CU = 15, MOC_MOI = 127;

    public static void main(String[] args) throws Exception {
        java.util.zip.ZipFile zip = new java.util.zip.ZipFile(new File(args[0]));
        java.util.zip.ZipOutputStream zo = new java.util.zip.ZipOutputStream(
                Files.newOutputStream(new File(args[1]).toPath()));

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
            System.out.println("  đã tắt nhấp nháy tên món đồ nâng cấp cao");
        } else {
            System.out.println("!! chờ đúng một chỗ, tìm thấy " + patched
                    + " -- kiểm lại trước khi phát hành");
        }
    }

    private static int patch(byte[] d) {
        int n = 0;
        for (int i = 0; i + 10 < d.length; i++) {
            if ((d[i] & 0xFF) != BIPUSH || (d[i + 1] & 0xFF) != MOC_CU) {
                continue;
            }
            if ((d[i + 2] & 0xFF) != IF_ICMPLT || (d[i + 5] & 0xFF) != GETSTATIC
                    || (d[i + 8] & 0xFF) != IFNE) {
                continue;
            }
            int dichIcmplt = ((d[i + 3] & 0xFF) << 8) | (d[i + 4] & 0xFF);
            int dichIfne = ((d[i + 9] & 0xFF) << 8) | (d[i + 10] & 0xFF);
            // Hai lệnh nhảy cách nhau 6 byte nên cùng trỏ tới một đích khi hiệu đúng bằng 6.
            if (dichIcmplt - 6 != dichIfne) {
                continue;
            }
            d[i + 1] = (byte) MOC_MOI;
            n++;
        }
        return n;
    }

    private static byte[] read(java.io.InputStream in) throws Exception {
        java.io.ByteArrayOutputStream b = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        for (int k; (k = in.read(buf)) > 0; ) {
            b.write(buf, 0, k);
        }
        in.close();
        return b.toByteArray();
    }
}
