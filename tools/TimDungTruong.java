import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Liệt kê mọi chỗ ĐỌC / GHI một trường, tìm theo tên trường (tên trường trong client ngắn, chỉ
 * lớp mới bị rối dài) -- để đi từ "trường này chứa thời gian chờ" tới "hàm nào dùng nó".
 *
 * Dùng: TimDungTruong <jar> <tên trường>[,<tên>...]
 */
public final class TimDungTruong {

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
                for (AbstractInsnNode n = mn.instructions.getFirst(); n != null; n = n.getNext()) {
                    if (!(n instanceof FieldInsnNode)) {
                        continue;
                    }
                    FieldInsnNode f = (FieldInsnNode) n;
                    for (String c : can) {
                        if (!f.name.equals(c.trim())) {
                            continue;
                        }
                        boolean doc = n.getOpcode() == Opcodes.GETFIELD || n.getOpcode() == Opcodes.GETSTATIC;
                        System.out.println((doc ? "đọc  " : "GHI  ") + gon(f.owner) + "." + f.name + ":" + f.desc
                                + "   trong " + gon(cn.name) + "." + gon(mn.name) + gonD(mn.desc)
                                + "  [" + mn.instructions.size() + " lệnh]");
                    }
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
