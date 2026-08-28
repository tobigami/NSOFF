import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Bỏ tiền tố "[null]" ở mọi dòng thông báo do client tự sinh.
 *
 * Hàm hiện thông báo nội bộ của client ghép chuỗi kiểu:
 *
 *     hienThongBao("[" + Ae + "]" + text);
 *
 * Ae là một trường String tĩnh mà trong toàn bộ jar chỉ có đúng một lệnh ĐỌC, không có lệnh ghi
 * nào -- lập trình viên gốc để dành chỗ cho một cái nhãn rồi không bao giờ gán. Nó vĩnh viễn là
 * null, nên mọi thông báo đều mở đầu bằng "[null]".
 *
 * Cách vá: 29 byte đầu của hàm chỉ làm mỗi việc dựng chuỗi có tiền tố. Thay chúng bằng aload_0
 * (đẩy thẳng tham số text) rồi 28 lệnh nop. Lệnh ngay sau đó vẫn nhận đúng một String trên ngăn
 * xếp như cũ. Hàm này không có lệnh nhảy nào nên không có đích nhảy nào rơi vào vùng bị đè, và
 * bảng khung ngăn xếp cũng không cần sửa.
 *
 * Usage: NoNullTag <in.jar> <out.jar>
 */
public final class NoNullTag {

    private static final int ALOAD_0 = 0x2A, NOP = 0x00;
    /** Số byte của đoạn dựng chuỗi bị bỏ đi. */
    private static final int SPAN = 29;

    /**
     * new StringBuffer, dup, ldc_w "[", invokespecial, getstatic Ae, invokevirtual append,
     * ldc_w "]", invokevirtual append, aload_0, invokevirtual append, invokevirtual toString,
     * invokestatic, invokestatic, return. Dấu ? là toán hạng bất kỳ.
     */
    private static final int[] SHAPE = {
        0xBB, -1, -1, 0x59, 0x13, -1, -1, 0xB7, -1, -1, 0xB2, -1, -1, 0xB6, -1, -1,
        0x13, -1, -1, 0xB6, -1, -1, 0x2A, 0xB6, -1, -1, 0xB6, -1, -1,
        0xB8, -1, -1, 0xB8, -1, -1, 0xB1
    };

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("usage: NoNullTag <in.jar> <out.jar>");
            return;
        }
        ZipFile zip = new ZipFile(new File(args[0]));
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(new File(args[1])));
        int hit = 0;
        Enumeration<? extends ZipEntry> it = zip.entries();
        while (it.hasMoreElements()) {
            ZipEntry e = it.nextElement();
            byte[] data = readAll(zip.getInputStream(e));
            if (hit == 0 && e.getName().endsWith(".class")) {
                hit += strip(data);
            }
            ZipEntry copy = new ZipEntry(e.getName());
            copy.setTime(e.getTime());
            zos.putNextEntry(copy);
            zos.write(data);
            zos.closeEntry();
        }
        zip.close();
        zos.close();
        System.out.println(hit > 0
                ? "bỏ tiền tố [null] ở thông báo client (" + hit + " chỗ)"
                : "!! không tìm thấy hàm ghép tiền tố -- chưa vá");
    }

    /** Đè tại chỗ, trả về số chỗ đã sửa. */
    private static int strip(byte[] d) {
        int n = 0;
        for (int i = 0; i + SHAPE.length <= d.length; i++) {
            if (!matches(d, i)) {
                continue;
            }
            d[i] = (byte) ALOAD_0;
            for (int k = 1; k < SPAN; k++) {
                d[i + k] = (byte) NOP;
            }
            n++;
            i += SHAPE.length;
        }
        return n;
    }

    private static boolean matches(byte[] d, int at) {
        for (int k = 0; k < SHAPE.length; k++) {
            if (SHAPE[k] >= 0 && (d[at + k] & 0xFF) != SHAPE[k]) {
                return false;
            }
        }
        return true;
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
