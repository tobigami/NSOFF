import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Dò những chỗ quyết định NHỊP trong client: hàm nào vừa hỏi giờ vừa so với một hằng số.
 *
 * Tên lớp trong client bị rối thành hàng trăm ký tự I/l nên đọc bằng mắt là vô vọng. Nhưng nhịp
 * đánh thì luôn có hình dạng giống nhau: lấy System.currentTimeMillis() rồi so hiệu với một con
 * số. Lọc theo hình dạng ấy là ra một danh sách ngắn, đủ ngắn để soi từng cái.
 *
 * Dùng: DoNhip <jar> [ngưỡng dưới] [ngưỡng trên]
 */
public final class DoNhip {

    public static void main(String[] args) throws Exception {
        String jar = args[0];
        int thap = args.length > 1 ? Integer.parseInt(args[1]) : 30;
        int cao = args.length > 2 ? Integer.parseInt(args[2]) : 3000;
        ZipFile z = new ZipFile(jar);
        int soLop = 0, soHam = 0;
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
            soLop++;
            for (Object mo : cn.methods) {
                MethodNode mn = (MethodNode) mo;
                if (mn.instructions == null) {
                    continue;
                }
                boolean hoiGio = false, nguGia = false;
                List<Integer> so = new ArrayList<>();
                for (AbstractInsnNode n = mn.instructions.getFirst(); n != null; n = n.getNext()) {
                    if (n instanceof MethodInsnNode) {
                        MethodInsnNode m = (MethodInsnNode) n;
                        if (m.name.equals("currentTimeMillis")) {
                            hoiGio = true;
                        }
                        if (m.name.equals("sleep")) {
                            nguGia = true;
                        }
                    } else if (n instanceof IntInsnNode && n.getOpcode() == Opcodes.SIPUSH) {
                        so.add(((IntInsnNode) n).operand);
                    } else if (n instanceof IntInsnNode && n.getOpcode() == Opcodes.BIPUSH) {
                        so.add(((IntInsnNode) n).operand);
                    } else if (n instanceof LdcInsnNode && ((LdcInsnNode) n).cst instanceof Number) {
                        so.add(((Number) ((LdcInsnNode) n).cst).intValue());
                    }
                }
                if (!hoiGio && !nguGia) {
                    continue;
                }
                List<Integer> loc = new ArrayList<>();
                for (int v : so) {
                    if (v >= thap && v <= cao && !loc.contains(v)) {
                        loc.add(v);
                    }
                }
                if (loc.isEmpty()) {
                    continue;
                }
                soHam++;
                System.out.println((nguGia ? "[ngu] " : "[gio] ") + rutGon(cn.name) + "." + mn.name
                        + mn.desc + "  hằng số: " + loc);
            }
        }
        z.close();
        System.out.println("-- quét " + soLop + " lớp, " + soHam + " hàm khớp");
    }

    /** Tên lớp rối dài hàng trăm ký tự; giữ 6 ký tự đầu + độ dài là đủ để nhận ra và tra lại. */
    private static String rutGon(String ten) {
        return ten.length() <= 14 ? ten : ten.substring(0, 6) + "~" + ten.length();
    }
}
