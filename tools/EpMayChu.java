import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.io.*;
import java.util.Enumeration;
import java.util.zip.*;

/**
 * Ép một client nối vào đúng một máy chủ, bằng cách thay thẳng URL ngay trước lúc mở kết nối.
 *
 * Khác `ForceServer`: bản kia tìm đoạn ghép chuỗi `new StringBuffer("socket://").append(host)...`
 * rồi thay tại chỗ. Client NST_X1 không ghép kiểu đó -- host và cổng nằm trong những trường tĩnh
 * được giải mã lúc chạy, nên trong jar không có hằng nào để đổi.
 *
 * Nên ở đây chặn muộn hơn hẳn, tại `Connector.open(String)`: bỏ chuỗi client vừa tính ra khỏi ngăn
 * xếp rồi đẩy vào chuỗi của mình. Client tính gì cũng mặc, cái đi vào Connector luôn là địa chỉ ta
 * chỉ định. Chỉ chèn hai lệnh (POP, LDC), không thêm nhánh rẽ nào, nên bảng khung ngăn xếp cũ vẫn
 * đúng -- chỉ cần ASM tính lại độ sâu.
 *
 * Chỉ đụng vào lời gọi có đúng một tham số String; các dạng open khác (có chế độ đọc/ghi) không
 * dùng ở đây và cũng không nên đụng vào.
 *
 * Usage: EpMayChu <jar vào> <jar ra> <host> <port>
 */
public final class EpMayChu {

    private static int daVa = 0;

    private EpMayChu() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.out.println("dùng: EpMayChu <jar vào> <jar ra> <host> <port>");
            return;
        }
        String url = "socket://" + args[2] + ":" + args[3];
        ZipFile vao = new ZipFile(args[0]);
        ZipOutputStream ra = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(args[1])));
        for (Enumeration<? extends ZipEntry> e = vao.entries(); e.hasMoreElements(); ) {
            ZipEntry z = e.nextElement();
            byte[] than = doc(vao.getInputStream(z));
            if (z.getName().endsWith(".class")) {
                byte[] moi = va(than, url);
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
        System.out.println("ép máy chủ: " + url + "  (vá " + daVa + " chỗ mở kết nối)");
        if (daVa == 0) {
            System.out.println("!! không vá được chỗ nào -- client này mở kết nối kiểu khác");
        }
    }

    private static byte[] va(byte[] than, String url) {
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
                if (!(n instanceof MethodInsnNode)) {
                    continue;
                }
                MethodInsnNode mi = (MethodInsnNode) n;
                if (!mi.owner.equals("javax/microedition/io/Connector")
                        || !mi.name.equals("open")
                        || !mi.desc.equals("(Ljava/lang/String;)Ljavax/microedition/io/Connection;")) {
                    continue;
                }
                InsnList them = new InsnList();
                them.add(new InsnNode(Opcodes.POP));
                them.add(new LdcInsnNode(url));
                mn.instructions.insertBefore(mi, them);
                daVa++;
                doi = true;
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
