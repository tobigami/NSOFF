import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.io.*;
import java.util.Enumeration;
import java.util.zip.*;

/**
 * Cho client NST_X1 áp mảng thời trang lên hình nhân vật.
 *
 * X1 khác NSO ở chỗ nó KHÔNG vứt mảng mười số: nó đọc đủ và cất vào mười trường của lớp nhân vật
 * `aL`, và đã dùng ô số 6 để vẽ thú cưỡi. Nên việc còn lại chỉ là chép ba ô đầu sang ba trường mà
 * hàm vẽ thật sự đọc:
 *
 *      ce (ô 0, tóc)  -> bH:I        cf (ô 1, thân) -> bM:I        ch (ô 2, chân) -> bI:I
 *
 * Ba trường đích lấy từ `aL.l(LaK;)V` -- hàm duy nhất đọc bảng CharInfo `[[[I`, đọc 90 lần, và
 * quanh mỗi lần chỉ có bốn trường số của aL: bH, bI, bM, bO. Client vẽ bốn ô đầu/thân/chân/vũ khí
 * nên bO là vũ khí, ba cái kia là ba mảnh cơ thể.
 *
 * VÌ SAO PHẢI SINH BYTECODE TAY thay vì viết một lớp Java rồi dịch: lớp `aL` có 46 tên trường bị
 * dùng lại cho nhiều kiểu, trong đó có đúng bH, bI, bM, bO -- mỗi cái tồn tại cả bản `Z` lẫn bản
 * `I`. Nguồn Java không chỉ đích được kiểu nào, javac luôn chọn `Z` và báo lỗi. Bytecode thì ghi
 * rõ mô tả kiểu nên chỉ đúng bản `I`.
 *
 * Lớp phụ sinh ra ở phiên bản Java 5 (major 49) -- dưới Java 6 nên KHÔNG cần bảng khung ngăn xếp,
 * nhờ vậy viết nhánh rẽ thoải mái. Máy ảo Java 8 vẫn nạp bình thường.
 *
 * Chỗ chèn chỉ có hai lệnh và không có nhánh rẽ nào, nên bảng khung của hàm bị chèn vẫn đúng
 * nguyên -- đây là lý do phần điều kiện phải nằm trong lớp phụ chứ không chèn thẳng.
 *
 * Usage: ThoiTrangX1Hook <jar vào> <jar ra>
 */
public final class ThoiTrangX1Hook {

    private static final String NHAN_VAT = "aL";
    private static final String LOP_PHU = "ThoiTrangX";
    /**
     * {ô thời trang (int), trường mảnh (short)}.
     *
     * Ba trường đích tìm bằng cách quét hàm vẽ xem trường nào được dùng làm CHỈ SỐ TRA MẢNG --
     * ra đúng bốn trường short s, t, u, v, mỗi cái một lần, tức bốn ô đầu/thân/chân/vũ khí.
     * Trùng khít tên với client NSO (xem mod-src/ThoiTrang.java), hai bản cùng gốc.
     *
     * Lần trước tôi đoán là bH/bM/bI vì chúng xuất hiện quanh chỗ vẽ -- sai, và ghi mã trang phục
     * vào đó làm nhân vật rung lắc loạn xạ. "Xuất hiện gần" khác hẳn "được dùng làm chỉ số".
     */
    private static final String[][] CHEP = {{"ce", "s"}, {"cf", "u"}, {"ch", "t"}};

    /** Bật để lớp phụ in vài dòng đầu, phục vụ dò lỗi. */
    private static final boolean IN_THU = true;

    /** Mọi trường số (không tĩnh) của lớp nhân vật, gom lúc đọc jar để đổ ra log khi dò lỗi. */
    private static final java.util.List<String[]> TRUONG_SO = new java.util.ArrayList<String[]>();

    private static void noiChu(MethodVisitor mv, String chu) {
        mv.visitLdcInsn(chu);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
                "(Ljava/lang/String;)Ljava/lang/StringBuilder;");
    }

    private static void noiSo(MethodVisitor mv, String truong, String kieu) {
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(Opcodes.GETFIELD, NHAN_VAT, truong, kieu);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
                "(I)Ljava/lang/StringBuilder;");
    }

    private static int daVa = 0;

    private ThoiTrangX1Hook() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("dùng: ThoiTrangX1Hook <jar vào> <jar ra>");
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
            ZipEntry m = new ZipEntry(z.getName());
            m.setTime(z.getTime());
            ra.putNextEntry(m);
            ra.write(than);
            ra.closeEntry();
        }
        ZipEntry phu = new ZipEntry(LOP_PHU + ".class");
        phu.setTime(System.currentTimeMillis());
        ra.putNextEntry(phu);
        ra.write(lopPhu());
        ra.closeEntry();
        vao.close();
        ra.close();
        System.out.println("thời trang X1: đã nối " + daVa + " chỗ, kèm lớp phụ " + LOP_PHU);
        if (daVa == 0) {
            System.out.println("!! không tìm thấy chỗ đọc mảng thời trang -- client này khác cấu trúc");
        }
    }

    /**
     * Sinh lớp phụ: ba hàm tra "mảnh hiệu lực" -- có cải trang thì trả cải trang, không thì trả
     * mảnh gốc. Ký hiệu hàm nhận (LaL;) trả về S, ĐÚNG BẰNG hiệu ứng ngăn xếp của lệnh
     * `getfield aL.s:S` mà nó thay thế, nên chỉ cần đổi một lệnh, không xê dịch gì khác.
     */
    private static byte[] lopPhu() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL,
                LOP_PHU, null, "java/lang/Object", null);
        if (IN_THU) {
            cw.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "daIn", "I", null, Integer.valueOf(0)).visitEnd();
            cw.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "dem", "I", null, Integer.valueOf(0)).visitEnd();
        }
        for (String[] c : CHEP) {
            MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                    "h" + c[1], "(L" + NHAN_VAT + ";)S", null, null);
            mv.visitCode();
            Label goc = new Label();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitJumpInsn(Opcodes.IFNULL, goc);
            // In vài lần đầu để biết giá trị có tới nơi không. Đếm để khỏi ngập log: mỗi khung
            // hình gọi một lần, không chặn thì vài giây là hàng nghìn dòng.
            // Dò lỗi: cứ vài trăm khung hình lại đổ cả mười ô thời trang ra log, kèm mã bộ phận
            // thật đang vẽ. Nhìn số 309/310/308 rơi vào ô nào là biết ngay ô nào là tóc/thân/chân,
            // khỏi phải đoán. Chỉ gắn vào một hàm để khỏi in ba lần một khung.
            if (IN_THU && c[1].equals("s")) {
                Label thoiIn = new Label();
                mv.visitFieldInsn(Opcodes.GETSTATIC, LOP_PHU, "dem", "I");
                mv.visitInsn(Opcodes.ICONST_1);
                mv.visitInsn(Opcodes.IADD);
                mv.visitInsn(Opcodes.DUP);
                mv.visitFieldInsn(Opcodes.PUTSTATIC, LOP_PHU, "dem", "I");
                mv.visitIntInsn(Opcodes.SIPUSH, 240);
                mv.visitInsn(Opcodes.IREM);
                mv.visitJumpInsn(Opcodes.IFNE, thoiIn);
                mv.visitFieldInsn(Opcodes.GETSTATIC, LOP_PHU, "daIn", "I");
                mv.visitIntInsn(Opcodes.BIPUSH, 60);
                mv.visitJumpInsn(Opcodes.IF_ICMPGE, thoiIn);
                mv.visitFieldInsn(Opcodes.GETSTATIC, LOP_PHU, "daIn", "I");
                mv.visitInsn(Opcodes.ICONST_1);
                mv.visitInsn(Opcodes.IADD);
                mv.visitFieldInsn(Opcodes.PUTSTATIC, LOP_PHU, "daIn", "I");
                mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
                mv.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
                mv.visitInsn(Opcodes.DUP);
                mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V");
                noiChu(mv, "[TT]");
                for (String[] o : TRUONG_SO) {
                    noiChu(mv, " " + o[0] + o[1] + "=");
                    noiSo(mv, o[0], o[1]);
                }
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString",
                        "()Ljava/lang/String;");
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println",
                        "(Ljava/lang/String;)V");
                mv.visitLabel(thoiIn);
            }
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitFieldInsn(Opcodes.GETFIELD, NHAN_VAT, c[0], "I");
            mv.visitJumpInsn(Opcodes.IFLT, goc);          // âm = không cải trang
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitFieldInsn(Opcodes.GETFIELD, NHAN_VAT, c[0], "I");
            mv.visitInsn(Opcodes.I2S);
            mv.visitInsn(Opcodes.IRETURN);
            mv.visitLabel(goc);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitFieldInsn(Opcodes.GETFIELD, NHAN_VAT, c[1], "S");
            mv.visitInsn(Opcodes.IRETURN);
            mv.visitMaxs(4, 1);
            mv.visitEnd();
        }
        cw.visitEnd();
        return cw.toByteArray();
    }

    private static byte[] va(byte[] than) {
        ClassNode cn = new ClassNode();
        try {
            new ClassReader(than).accept(cn, 0);
        } catch (Throwable ex) {
            return null;
        }
        if (!cn.name.equals(NHAN_VAT)) {
            return null;
        }
        boolean doi = false;
        for (Object mo : cn.methods) {
            MethodNode mn = (MethodNode) mo;
            if (mn.instructions == null) {
                continue;
            }
            boolean laHamVe = false;
            for (AbstractInsnNode n = mn.instructions.getFirst(); n != null; n = n.getNext()) {
                if (n instanceof FieldInsnNode && ((FieldInsnNode) n).desc.equals("[[[I")) {
                    laHamVe = true;
                    break;
                }
            }
            if (!laHamVe) {
                continue;
            }
            // Thay chỗ ĐỌC, không phải chỗ ghi.
            //
            // Chèn ở đầu hàm thì vô ích: chính hàm vẽ này tự ghi lại `aL.s` trong thân nó, nên
            // giá trị đặt lúc vào hàm bị xoá trước khi được dùng. Thay thẳng lệnh đọc thì cải
            // trang thắng đúng vào lúc lấy mảnh ra vẽ, bất kể ai ghi gì trước đó.
            for (AbstractInsnNode n = mn.instructions.getFirst(); n != null; ) {
                AbstractInsnNode ke = n.getNext();
                if (n.getOpcode() == Opcodes.GETFIELD) {
                    FieldInsnNode f = (FieldInsnNode) n;
                    if (f.owner.equals(NHAN_VAT) && f.desc.equals("S")) {
                        for (String[] c : CHEP) {
                            if (f.name.equals(c[1])) {
                                mn.instructions.set(n, new MethodInsnNode(Opcodes.INVOKESTATIC,
                                        LOP_PHU, "h" + c[1], "(L" + NHAN_VAT + ";)S"));
                                daVa++;
                                doi = true;
                                break;
                            }
                        }
                    }
                }
                n = ke;
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
