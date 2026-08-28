import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.io.*;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.zip.*;

/**
 * Nối mảng thời trang của máy chủ vào phần vẽ nhân vật của client.
 *
 * Client gốc, ở mỗi gói tin mô tả nhân vật, làm đúng thế này:
 *
 *     bipush 10 · newarray short · astore n      dựng mảng mười số
 *     vòng lặp readShort() mười lần               đọc cho đủ, khỏi lệch luồng
 *     goto ...                                    rồi vứt luôn, không ai đọc lại
 *
 * Bản v4 trong thư mục gpt cũng y hệt, nên không có gì để chép sang. Ở đây chèn thêm một lời gọi
 * ThoiTrang.ap(mảng) ngay sau vòng lặp, để ba số đầu được áp lên tóc/thân/chân.
 *
 * Chèn đúng hai lệnh và không thêm nhánh rẽ nào, nên bảng khung ngăn xếp sẵn có vẫn đúng, chỉ cần
 * ASM tính lại độ sâu -- không phải dựng lại frame, thứ hay làm hỏng lớp đã bị làm rối tên.
 *
 * Vị trí chèn là nhãn thoát vòng lặp, tức đích của lệnh if_icmpge canh vòng. Chèn trước lệnh goto
 * ở đó nên chỉ chạy trên đường bình thường; nhánh bắt lỗi vẫn nhảy thẳng qua như cũ.
 *
 * Nhân vật nhận đồ không phải lúc nào cũng là nhân vật của mình: hai chỗ lấy qua getMyChar, chỗ
 * thứ ba nằm trong hàm tĩnh và nhận nhân vật khác qua tham số. Nên thay vì đoán, bộ vá nhìn lại
 * lệnh putfield gán tóc/thân/chân ngay phía trên rồi nhân bản đúng lệnh đã đẩy nhân vật đó lên
 * ngăn xếp -- lệnh ấy luôn đứng cách putfield đúng bốn nhịp.
 *
 * Usage: ThoiTrangHook <jar vào> <jar ra>
 */
public final class ThoiTrangHook {

    private static final String LOP_PHU = "ThoiTrang";
    private static int daVa = 0;

    private ThoiTrangHook() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("dùng: ThoiTrangHook <jar vào> <jar ra>");
            return;
        }
        ZipFile vao = new ZipFile(args[0]);
        ZipOutputStream ra = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(args[1])));
        for (Enumeration<? extends ZipEntry> e = vao.entries(); e.hasMoreElements(); ) {
            ZipEntry z = e.nextElement();
            byte[] than = doc(vao.getInputStream(z));
            if (z.getName().endsWith(".class")) {
                byte[] moi = va(than);
                if (moi != null) {
                    than = moi;
                }
            }
            ZipEntry moi = new ZipEntry(z.getName());
            moi.setTime(z.getTime());
            ra.putNextEntry(moi);
            ra.write(than);
            ra.closeEntry();
        }
        vao.close();
        ra.close();
        System.out.println("thời trang: đã nối " + daVa + " chỗ");
        if (daVa == 0) {
            System.out.println("  !! không tìm thấy chỗ nào -- client đã đổi, cần dò lại");
        }
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

    private static byte[] va(byte[] than) {
        ClassNode cn = new ClassNode();
        try {
            new ClassReader(than).accept(cn, 0);
        } catch (Throwable ex) {
            return null;                       // lớp lạ thì để nguyên, đừng làm hỏng cả mẻ
        }
        boolean doi = false;
        for (Iterator<?> it = cn.methods.iterator(); it.hasNext(); ) {
            MethodNode mn = (MethodNode) it.next();
            if (mn.instructions == null) {
                continue;
            }
            doi |= vaMotHam(mn);
        }
        if (!doi) {
            return null;
        }
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        return cw.toByteArray();
    }

    private static boolean vaMotHam(MethodNode mn) {
        boolean doi = false;
        AbstractInsnNode n = mn.instructions.getFirst();
        while (n != null) {
            AbstractInsnNode sau = n.getNext();
            int oCuc = nhanDangMang(n);
            if (oCuc >= 0) {
                LabelNode thoat = nhanThoatVong(n);
                if (thoat != null) {
                    AbstractInsnNode dat = sauNhan(thoat);
                    FieldInsnNode pf = putfieldGanNhat(n);
                    AbstractInsnNode nguoi = pf == null ? null : lenhDayNguoiLen(pf);
                    if (dat != null && nguoi != null) {
                        InsnList them = new InsnList();
                        them.add(nhanBan(nguoi));
                        them.add(new VarInsnNode(Opcodes.ALOAD, oCuc));
                        them.add(new MethodInsnNode(Opcodes.INVOKESTATIC, LOP_PHU, "ap",
                                "(L" + pf.owner + ";[S)V"));
                        mn.instructions.insertBefore(dat, them);
                        daVa++;
                        doi = true;
                        sau = dat;
                    }
                }
            }
            n = sau;
        }
        return doi;
    }

    /** Trả về số hiệu biến cục bộ nếu đây đúng là chỗ dựng mảng mười số, không thì -1. */
    private static int nhanDangMang(AbstractInsnNode n) {
        if (!(n instanceof IntInsnNode) || n.getOpcode() != Opcodes.BIPUSH
                || ((IntInsnNode) n).operand != 10) {
            return -1;
        }
        AbstractInsnNode a = ke(n);
        if (!(a instanceof IntInsnNode) || a.getOpcode() != Opcodes.NEWARRAY
                || ((IntInsnNode) a).operand != Opcodes.T_SHORT) {
            return -1;
        }
        AbstractInsnNode b = ke(a);
        if (!(b instanceof VarInsnNode) || b.getOpcode() != Opcodes.ASTORE) {
            return -1;
        }
        return ((VarInsnNode) b).var;
    }

    /**
     * Đích của lệnh if_icmpge canh vòng lặp, tức nơi chạy tiếp sau khi đọc đủ mười số. Chỉ nhận khi
     * trong vòng có gọi readShort, để khỏi vá nhầm một mảng mười phần tử nào khác.
     */
    private static LabelNode nhanThoatVong(AbstractInsnNode tu) {
        boolean coDocShort = false;
        AbstractInsnNode n = tu;
        for (int i = 0; i < 40 && n != null; i++, n = n.getNext()) {
            if (n instanceof MethodInsnNode && "readShort".equals(((MethodInsnNode) n).name)) {
                coDocShort = true;
            }
            if (n.getOpcode() == Opcodes.IF_ICMPGE && n instanceof JumpInsnNode) {
                return coDocShort || coDocShortSau(n) ? ((JumpInsnNode) n).label : null;
            }
        }
        return null;
    }

    /** Lệnh canh vòng đứng trước phần thân, nên phải nhìn thêm vài lệnh phía sau nó. */
    private static boolean coDocShortSau(AbstractInsnNode tu) {
        AbstractInsnNode n = tu;
        for (int i = 0; i < 12 && n != null; i++, n = n.getNext()) {
            if (n instanceof MethodInsnNode && "readShort".equals(((MethodInsnNode) n).name)) {
                return true;
            }
        }
        return false;
    }

    /** Lệnh thật đầu tiên sau một nhãn: bỏ qua nhãn, khung ngăn xếp và số dòng. */
    private static AbstractInsnNode sauNhan(LabelNode nhan) {
        AbstractInsnNode n = nhan.getNext();
        while (n instanceof LabelNode || n instanceof FrameNode || n instanceof LineNumberNode) {
            n = n.getNext();
        }
        return n;
    }

    /** putfield gán tóc/thân/chân gần nhất phía trên, để biết đây là gói mô tả nhân vật nào. */
    private static FieldInsnNode putfieldGanNhat(AbstractInsnNode tu) {
        AbstractInsnNode n = tu;
        for (int i = 0; i < 60 && n != null; i++, n = n.getPrevious()) {
            if (n.getOpcode() == Opcodes.PUTFIELD && n instanceof FieldInsnNode) {
                FieldInsnNode f = (FieldInsnNode) n;
                if ("S".equals(f.desc) && f.name.length() == 1
                        && "stuv".indexOf(f.name.charAt(0)) >= 0) {
                    return f;
                }
            }
        }
        return null;
    }

    /** Lệnh đẩy nhân vật lên ngăn xếp: đứng đúng bốn nhịp trước putfield. */
    private static AbstractInsnNode lenhDayNguoiLen(FieldInsnNode pf) {
        AbstractInsnNode n = pf;
        for (int i = 0; i < 4; i++) {
            n = truoc(n);
            if (n == null) {
                return null;
            }
        }
        boolean hopLe = n.getOpcode() == Opcodes.ALOAD
                || (n.getOpcode() == Opcodes.INVOKESTATIC && n instanceof MethodInsnNode);
        return hopLe ? n : null;
    }

    private static AbstractInsnNode nhanBan(AbstractInsnNode n) {
        if (n instanceof VarInsnNode) {
            return new VarInsnNode(n.getOpcode(), ((VarInsnNode) n).var);
        }
        MethodInsnNode m = (MethodInsnNode) n;
        return new MethodInsnNode(m.getOpcode(), m.owner, m.name, m.desc);
    }

    /** Lệnh thật liền trước, bỏ qua nhãn và khung. */
    private static AbstractInsnNode truoc(AbstractInsnNode n) {
        AbstractInsnNode k = n.getPrevious();
        while (k instanceof LabelNode || k instanceof FrameNode || k instanceof LineNumberNode) {
            k = k.getPrevious();
        }
        return k;
    }

    /** Lệnh thật kế tiếp, bỏ qua nhãn và khung. */
    private static AbstractInsnNode ke(AbstractInsnNode n) {
        AbstractInsnNode k = n.getNext();
        while (k instanceof LabelNode || k instanceof FrameNode || k instanceof LineNumberNode) {
            k = k.getNext();
        }
        return k;
    }
}
