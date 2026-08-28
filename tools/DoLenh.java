import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Dò xem client xử lý một lệnh máy chủ ở đâu, và ghi kết quả vào trường nào.
 *
 * Lệnh nằm trong client dưới dạng hằng số (bipush/sipush) hoặc khoá của lookupswitch/tableswitch.
 * Tìm được chỗ đó rồi in mấy chục lệnh kế tiếp là thấy ngay nó đọc gì và gán vào đâu -- đủ để biết
 * tên (đã bị làm rối) của trường cần vá.
 *
 * Usage: DoLenh <jar> <số lệnh> [số lệnh in ra]
 */
public final class DoLenh {

    public static void main(String[] args) throws Exception {
        ZipFile z = new ZipFile(args[0]);
        int can = Integer.parseInt(args[1]);
        int sau = args.length > 2 ? Integer.parseInt(args[2]) : 40;
        for (Enumeration<? extends ZipEntry> e = z.entries(); e.hasMoreElements(); ) {
            ZipEntry en = e.nextElement();
            if (!en.getName().endsWith(".class")) {
                continue;
            }
            ClassNode cn = new ClassNode();
            try {
                new ClassReader(doc(z.getInputStream(en))).accept(cn, 0);
            } catch (Throwable t) {
                continue;
            }
            for (Object mo : cn.methods) {
                MethodNode mn = (MethodNode) mo;
                if (mn.instructions == null) {
                    continue;
                }
                AbstractInsnNode n = mn.instructions.getFirst();
                int i = 0;
                while (n != null) {
                    if (n instanceof TableSwitchInsnNode) {
                        TableSwitchInsnNode t = (TableSwitchInsnNode) n;
                        if (can >= t.min && can <= t.max) {
                            // Bảng nhảy rộng: đừng in từ chỗ bảng, mà nhảy thẳng tới nhánh của
                            // đúng khoá cần tìm. In từ bảng thì ra toàn mã của khoá nhỏ nhất.
                            System.out.println("=== bảng nhảy " + t.min + ".." + t.max
                                    + " trong " + gon(cn.name) + "." + gon(mn.name));
                            LabelNode nhan = (LabelNode) t.labels.get(can - t.min);
                            // Nhánh thường mở đầu bằng một lệnh nhảy sang chỗ khác; đi theo nó,
                            // không thì in ra toàn mã của nhánh nằm kế bên.
                            AbstractInsnNode dau = sauNhan(nhan);
                            if (dau != null && dau.getOpcode() == Opcodes.GOTO) {
                                dau = ((JumpInsnNode) dau).label;
                            }
                            System.out.println("--- nhánh của khoá " + can + ":");
                            in(dau, sau);
                        }
                    } else if (khop(n, can)) {
                        System.out.println("=== " + gon(cn.name) + "." + gon(mn.name) + "  (lệnh #" + i + ")");
                        in(n, sau);
                    }
                    n = n.getNext();
                    i++;
                }
            }
        }
        z.close();
    }

    /** Tên bị làm rối dài hàng trăm ký tự, in đủ thì không đọc nổi. */
    private static String gon(String t) {
        return t.length() <= 24 ? t : t.substring(0, 10) + "..." + t.substring(t.length() - 8);
    }

    /** Lệnh thật đầu tiên sau một nhãn: bỏ qua nhãn, khung ngăn xếp và số dòng. */
    private static AbstractInsnNode sauNhan(AbstractInsnNode n) {
        AbstractInsnNode k = n;
        while (k instanceof LabelNode || k instanceof FrameNode || k instanceof LineNumberNode) {
            k = k.getNext();
        }
        return k;
    }

    private static boolean khop(AbstractInsnNode n, int can) {
        if (n instanceof IntInsnNode && (n.getOpcode() == Opcodes.BIPUSH || n.getOpcode() == Opcodes.SIPUSH)) {
            return ((IntInsnNode) n).operand == can;
        }
        if (n instanceof LookupSwitchInsnNode) {
            List<?> keys = ((LookupSwitchInsnNode) n).keys;
            for (Object k : keys) {
                if (((Integer) k) == can) {
                    return true;
                }
            }
        }
        if (n instanceof TableSwitchInsnNode) {
            TableSwitchInsnNode t = (TableSwitchInsnNode) n;
            // Chỉ nhận bảng nhảy hẹp: bảng rộng khớp bừa vào mọi chỗ có chứa số ấy.
            return can >= t.min && can <= t.max && (t.max - t.min) < 40;
        }
        // Cố tình KHÔNG nhận tableswitch rộng: dải của nó thường rộng, khớp bừa vào mọi bảng nhảy
        // có chứa số ấy nên in ra toàn chỗ không liên quan. Lệnh con so bằng bipush hoặc nằm trong
        // lookupswitch, hai dạng trên là đủ.
        return false;
    }

    private static void in(AbstractInsnNode tu, int sau) {
        AbstractInsnNode n = tu;
        for (int k = 0; k < sau && n != null; n = n.getNext()) {
            String s = ta(n);
            if (s != null) {
                System.out.println("    " + s);
                k++;
            }
        }
    }

    private static String ta(AbstractInsnNode n) {
        int op = n.getOpcode();
        if (op < 0) {
            return null;
        }
        String ten = op < TEN.length ? TEN[op] : ("op" + op);
        if (n instanceof FieldInsnNode) {
            FieldInsnNode f = (FieldInsnNode) n;
            return ten + " " + f.owner + "." + f.name + " : " + f.desc;
        }
        if (n instanceof MethodInsnNode) {
            MethodInsnNode m = (MethodInsnNode) n;
            return ten + " " + m.owner + "." + m.name + m.desc;
        }
        if (n instanceof VarInsnNode) {
            return ten + " " + ((VarInsnNode) n).var;
        }
        if (n instanceof IntInsnNode) {
            return ten + " " + ((IntInsnNode) n).operand;
        }
        if (n instanceof LdcInsnNode) {
            return ten + " " + ((LdcInsnNode) n).cst;
        }
        return ten;
    }

    private static final String[] TEN = new String[256];

    static {
        for (int i = 0; i < 256; i++) {
            TEN[i] = "op" + i;
        }
        TEN[0x2a] = "aload_0";
        TEN[0x10] = "bipush";
        TEN[0x11] = "sipush";
        TEN[0x12] = "ldc";
        TEN[0x19] = "aload";
        TEN[0x15] = "iload";
        TEN[0x36] = "istore";
        TEN[0x3a] = "astore";
        TEN[0xb2] = "getstatic";
        TEN[0xb3] = "putstatic";
        TEN[0xb4] = "getfield";
        TEN[0xb5] = "putfield";
        TEN[0xb6] = "invokevirtual";
        TEN[0xb7] = "invokespecial";
        TEN[0xb8] = "invokestatic";
        TEN[0xb9] = "invokeinterface";
        TEN[0xa7] = "goto";
        TEN[0xac] = "ireturn";
        TEN[0xb1] = "return";
        TEN[0x57] = "pop";
        TEN[0x59] = "dup";
        TEN[0x93] = "i2s";
        TEN[0x91] = "i2b";
    }

    private static byte[] doc(InputStream in) throws Exception {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        byte[] d = new byte[8192];
        for (int n; (n = in.read(d)) > 0; ) {
            b.write(d, 0, n);
        }
        in.close();
        return b.toByteArray();
    }
}
