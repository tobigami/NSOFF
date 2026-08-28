import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Liệt kê mọi lệnh Thread.sleep(<hằng số>) trong client, kèm nơi chứa nó.
 *
 * Đây là thứ đáng ngờ nhất khi "game cảm giác chậm mà máy chủ không chậm": một lệnh ngủ ghi cứng
 * trong đường đánh thì không log nào bên máy chủ nhìn thấy được, chỉ hiện ra ở khoảng cách giữa
 * hai gói tin.
 *
 * Dùng: TimNgu <jar>
 */
public final class TimNgu {

    public static void main(String[] args) throws Exception {
        ZipFile z = new ZipFile(args[0]);
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
                for (AbstractInsnNode n = mn.instructions.getFirst(); n != null; n = n.getNext()) {
                    if (!(n instanceof MethodInsnNode)) {
                        continue;
                    }
                    MethodInsnNode m = (MethodInsnNode) n;
                    if (!m.owner.equals("java/lang/Thread") || !m.name.equals("sleep")) {
                        continue;
                    }
                    AbstractInsnNode t = truoc(n);
                    String bao = (t instanceof LdcInsnNode && ((LdcInsnNode) t).cst instanceof Long)
                            ? ((LdcInsnNode) t).cst + " ms" : "(không phải hằng số)";
                    System.out.println(String.format("%-14s %-14s ngủ %-22s [%d lệnh]",
                            gon(cn.name), gon(mn.name), bao, mn.instructions.size()));
                }
            }
        }
        z.close();
    }

    private static AbstractInsnNode truoc(AbstractInsnNode n) {
        for (AbstractInsnNode k = n.getPrevious(); k != null; k = k.getPrevious()) {
            if (k.getOpcode() >= 0) {
                return k;
            }
        }
        return null;
    }

    private static String gon(String t) {
        return t.length() <= 14 ? t : t.substring(0, 6) + "~" + t.length();
    }
}
