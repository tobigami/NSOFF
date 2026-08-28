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
import org.objectweb.asm.tree.MethodNode;

/**
 * Tìm hàm nào trong client có chứa ĐỦ một tập hằng số cho trước.
 *
 * Tên lớp bị rối nên không tra theo tên được; nhưng mã lệnh gói tin thì hiện nguyên hình là hằng
 * số. Hỏi "hàm nào vừa có 60 vừa có 61" là ra ngay chỗ gửi gói đánh quái / đánh người.
 *
 * Dùng: TimHang <jar> <số>[,<số>...]
 */
public final class TimHang {

    public static void main(String[] args) throws Exception {
        ZipFile z = new ZipFile(args[0]);
        List<Integer> can = new ArrayList<>();
        for (String s : args[1].split(",")) {
            can.add(Integer.parseInt(s.trim()));
        }
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
                List<Integer> co = new ArrayList<>();
                for (AbstractInsnNode n = mn.instructions.getFirst(); n != null; n = n.getNext()) {
                    Integer v = null;
                    int op = n.getOpcode();
                    if (n instanceof IntInsnNode && (op == Opcodes.BIPUSH || op == Opcodes.SIPUSH)) {
                        v = ((IntInsnNode) n).operand;
                    } else if (n instanceof LdcInsnNode && ((LdcInsnNode) n).cst instanceof Number) {
                        v = ((Number) ((LdcInsnNode) n).cst).intValue();
                    } else if (op >= Opcodes.ICONST_M1 && op <= Opcodes.ICONST_5) {
                        v = op - Opcodes.ICONST_0;
                    }
                    if (v != null && can.contains(v) && !co.contains(v)) {
                        co.add(v);
                    }
                }
                if (co.size() == can.size()) {
                    System.out.println(gon(cn.name) + "." + gon(mn.name) + gonDesc(mn.desc)
                            + "   (" + mn.instructions.size() + " lệnh)");
                }
            }
        }
        z.close();
    }

    private static String gon(String t) {
        return t.length() <= 14 ? t : t.substring(0, 6) + "~" + t.length();
    }

    private static String gonDesc(String d) {
        StringBuilder sb = new StringBuilder();
        for (String p : d.split("(?<=[;)])")) {
            sb.append(p.length() > 20 ? p.substring(0, 4) + "~" : p);
        }
        return sb.toString();
    }
}
