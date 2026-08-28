import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Tìm mọi chỗ gọi tới một hàm, chỉ định hàm bằng ĐỘ DÀI tên lớp + độ dài tên hàm + mô tả.
 *
 * Tên trong client dài hàng trăm ký tự I/l nên không gõ tay được; nhưng bộ ba (dài lớp, dài hàm,
 * mô tả) thì gần như luôn là duy nhất, mà lại gõ được.
 *
 * Dùng: AiGoi <jar> <dàiLớp> <dàiHàm> [mô tả]
 */
public final class AiGoi {

    public static void main(String[] args) throws Exception {
        ZipFile z = new ZipFile(args[0]);
        int dLop = Integer.parseInt(args[1]);
        int dHam = Integer.parseInt(args[2]);
        String moTa = args.length > 3 ? args[3] : null;
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
                    if (m.owner.length() != dLop || m.name.length() != dHam) {
                        continue;
                    }
                    if (moTa != null && !m.desc.equals(moTa)) {
                        continue;
                    }
                    System.out.println(gon(cn.name) + "." + gon(mn.name) + gonD(mn.desc)
                            + "   -> gọi " + gon(m.owner) + "." + gon(m.name) + gonD(m.desc)
                            + "   [hàm gọi dài " + mn.instructions.size() + " lệnh]");
                }
            }
        }
        z.close();
    }

    private static String gon(String t) {
        return t.length() <= 14 ? t : t.substring(0, 6) + "~" + t.length();
    }

    private static String gonD(String d) {
        StringBuilder sb = new StringBuilder();
        for (String p : d.split("(?<=[;)])")) {
            sb.append(p.length() > 20 ? p.substring(0, 2) + "~" + (p.length() - 2) : p);
        }
        return sb.toString();
    }
}
