import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.*;

import java.io.*;
import java.util.Enumeration;
import java.util.zip.*;

/**
 * Đổi chữ hiển thị trong client, bằng cách thay thẳng hằng chuỗi trong bytecode.
 *
 * Dùng cho mấy nhãn ở bảng Thông tin. Chỉ đổi hằng chuỗi, không thêm bớt lệnh nào, nên bảng khung
 * ngăn xếp vẫn đúng nguyên -- rẻ và an toàn hơn hẳn việc chèn mã để tự dựng câu chữ.
 *
 * Cặp chữ truyền theo dạng "cũ=>mới", cách nhau bằng dấu chấm phẩy.
 *
 * Usage: SuaNhan <jar vào> <jar ra> "cũ=>mới;cũ=>mới"
 */
public final class SuaNhan {

    private static int daDoi = 0;

    private SuaNhan() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("dùng: SuaNhan <jar vào> <jar ra> \"cũ=>mới;...\"");
            return;
        }
        String[] cap = args[2].split(";");
        ZipFile vao = new ZipFile(args[0]);
        ZipOutputStream ra = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(args[1])));
        for (Enumeration<? extends ZipEntry> e = vao.entries(); e.hasMoreElements(); ) {
            ZipEntry z = e.nextElement();
            byte[] than = doc(vao.getInputStream(z));
            if (z.getName().endsWith(".class")) {
                byte[] moi = va(than, cap);
                if (moi != null) {
                    than = moi;
                }
            }
            ZipEntry m = new ZipEntry(z.getName());
            m.setTime(z.getTime());
            ra.putNextEntry(m);
            ra.write(than);
            ra.closeEntry();
        }
        vao.close();
        ra.close();
        System.out.println("sửa nhãn: đã đổi " + daDoi + " chỗ");
    }

    private static byte[] va(byte[] than, String[] cap) {
        ClassNode cn = new ClassNode();
        try {
            new ClassReader(than).accept(cn, 0);
        } catch (Throwable ex) {
            return null;
        }
        boolean doi = false;
        for (Object mo : cn.methods) {
            MethodNode mn = (MethodNode) mo;
            if (mn.instructions == null) {
                continue;
            }
            for (AbstractInsnNode n = mn.instructions.getFirst(); n != null; n = n.getNext()) {
                if (!(n instanceof LdcInsnNode)) {
                    continue;
                }
                LdcInsnNode l = (LdcInsnNode) n;
                if (!(l.cst instanceof String)) {
                    continue;
                }
                for (String c : cap) {
                    int i = c.indexOf("=>");
                    if (i < 0) {
                        continue;
                    }
                    if (l.cst.equals(c.substring(0, i))) {
                        l.cst = c.substring(i + 2);
                        daDoi++;
                        doi = true;
                    }
                }
            }
        }
        if (!doi) {
            return null;
        }
        ClassWriter cw = new ClassWriter(0);
        cn.accept(cw);
        return cw.toByteArray();
    }

    private static byte[] doc(InputStream in) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        byte[] d = new byte[8192];
        for (int n; (n = in.read(d)) > 0; ) {
            b.write(d, 0, n);
        }
        in.close();
        return b.toByteArray();
    }
}
