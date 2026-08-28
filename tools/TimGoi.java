import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

/**
 * Liệt kê mọi chỗ client DỰNG một gói tin, kèm mã lệnh của gói.
 *
 * Mẫu luôn giống nhau: new <Gói> ; dup ; <hằng số> ; invokespecial <init>. Mã lệnh gói là thứ duy
 * nhất không bị rối tên, nên đây là cách rẻ nhất để đi từ "máy chủ nhận lệnh 60" ngược về đúng hàm
 * trong client đã gửi nó.
 *
 * Dùng: TimGoi <jar> [mã cần tìm]
 */
public final class TimGoi {

    public static void main(String[] args) throws Exception {
        ZipFile z = new ZipFile(args[0]);
        Integer can = args.length > 1 ? Integer.parseInt(args[1]) : null;
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
                    if (!(n instanceof TypeInsnNode) || n.getOpcode() != Opcodes.NEW) {
                        continue;
                    }
                    String lop = ((TypeInsnNode) n).desc;
                    AbstractInsnNode a = sau(n), b = sau(a), c = sau(b);
                    if (a == null || b == null || c == null || a.getOpcode() != Opcodes.DUP) {
                        continue;
                    }
                    Integer ma = so(b);
                    if (ma == null || !(c instanceof MethodInsnNode)) {
                        continue;
                    }
                    MethodInsnNode kt = (MethodInsnNode) c;
                    if (!kt.name.equals("<init>") || !kt.owner.equals(lop)) {
                        continue;
                    }
                    if (can != null && ma.intValue() != can.intValue()) {
                        continue;
                    }
                    System.out.println("mã " + ma + "  <- " + gon(cn.name) + "." + gon(mn.name)
                            + "  (gói: " + gon(lop) + kt.desc + ")");
                }
            }
        }
        z.close();
    }

    private static AbstractInsnNode sau(AbstractInsnNode n) {
        for (AbstractInsnNode k = n == null ? null : n.getNext(); k != null; k = k.getNext()) {
            if (k.getOpcode() >= 0) {
                return k;   // bỏ nhãn, dòng, khung
            }
        }
        return null;
    }

    private static Integer so(AbstractInsnNode n) {
        int op = n.getOpcode();
        if (op >= Opcodes.ICONST_M1 && op <= Opcodes.ICONST_5) {
            return op - Opcodes.ICONST_0;
        }
        if (n instanceof IntInsnNode && (op == Opcodes.BIPUSH || op == Opcodes.SIPUSH)) {
            return ((IntInsnNode) n).operand;
        }
        return null;
    }

    private static String gon(String t) {
        return t.length() <= 14 ? t : t.substring(0, 6) + "~" + t.length();
    }
}
