import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Tìm hàm client ĐỌC một gói, bằng cách so thứ tự các lời gọi read* với thứ tự máy chủ ghi.
 *
 * Đây là cầu nối duy nhất đi được giữa hai bên: tên lớp/hàm/trường trong client đã bị rối hết,
 * nhưng THỨ TỰ đọc thì buộc phải khớp từng nhịp với thứ tự ghi bên máy chủ, nếu không gói sẽ lệch.
 * Nên chuỗi "readShort, readByte, readByte, readShort, readInt, ..." chính là vân tay không thể
 * giả của hàm đọc bảng kỹ năng.
 *
 * Dùng: TimDocGoi <jar> <chuỗi read cách nhau bởi dấu phẩy>
 */
public final class TimDocGoi {

    public static void main(String[] args) throws Exception {
        ZipFile z = new ZipFile(args[0]);
        String[] can = args[1].split(",");
        for (ZipEntry e : java.util.Collections.list(z.entries())) {
            if (!e.getName().endsWith(".class")) {
                continue;
            }
            ClassNode cn = new ClassNode();
            try (InputStream in = z.getInputStream(e)) {
                new ClassReader(in).accept(cn, ClassReader.SKIP_DEBUG);
            } catch (Throwable ex) {
                continue;
            }
            for (Object mo : cn.methods) {
                MethodNode mn = (MethodNode) mo;
                if (mn.instructions == null) {
                    continue;
                }
                List<String> doc = new ArrayList<>();
                for (AbstractInsnNode n = mn.instructions.getFirst(); n != null; n = n.getNext()) {
                    if (n instanceof MethodInsnNode) {
                        String ten = ((MethodInsnNode) n).name;
                        if (ten.startsWith("read")) {
                            doc.add(ten);
                        }
                    }
                }
                int o = viTri(doc, can);
                if (o >= 0) {
                    System.out.println(gon(cn.name) + "." + gon(mn.name) + "  khớp tại lời gọi read thứ "
                            + o + " / " + doc.size() + "  (" + mn.instructions.size() + " lệnh)");
                }
            }
        }
        z.close();
    }

    private static int viTri(List<String> co, String[] can) {
        for (int i = 0; i + can.length <= co.size(); i++) {
            boolean ok = true;
            for (int j = 0; j < can.length; j++) {
                if (!co.get(i + j).equals(can[j].trim())) {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                return i;
            }
        }
        return -1;
    }

    private static String gon(String t) {
        return t.length() <= 14 ? t : t.substring(0, 6) + "~" + t.length();
    }
}
