import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.io.*;
import java.util.Enumeration;
import java.util.zip.*;

/**
 * Cho client biết vẽ gì khi mặc một áo choàng mới.
 *
 * Client giữ sẵn một bảng cứng: một hàm không tham số trả về {@code int[]}, đọc trường {@code w}
 * (mã món áo choàng máy chủ gửi xuống ở gói lệnh con -56) rồi trả về bốn mã ảnh. Mã nào không có
 * trong bảng thì không vẽ gì -- nên món áo choàng mới thêm sẽ vô hình.
 *
 * Bộ vá chèn thêm một nhánh vào **đầu** hàm:
 *
 *     if (this.w == <mã món>) return new int[]{a, b, c, b};
 *
 * Chèn ở đầu nên không cần hiểu phần còn lại của hàm, và không đụng vào nhánh nào sẵn có.
 *
 * Nhận diện hàm bằng chữ ký chứ không bằng tên: tên đã bị làm rối và đổi theo mỗi bản build, còn
 * "hàm trả về int[] có đọc trường w kiểu short" thì chỉ có đúng một.
 *
 * Chèn nhánh rẽ làm bảng khung ngăn xếp cũ sai, nên hạ phiên bản lớp xuống 49: từ 50 trở lên máy
 * ảo đòi bảng khung, còn 49 thì nó dùng bộ kiểm kiểu suy diễn đời cũ, không cần bảng. Rẻ hơn nhiều
 * so với bắt ASM 3.1 tự dựng lại khung cho một lớp đã bị làm rối tên.
 *
 * Usage: AoChoangHook <jar vào> <jar ra> <mã món> <ảnh1> <ảnh2> <ảnh3>
 */
public final class AoChoangHook {

    private static int daVa = 0;
    private static int moChot = 0;

    private AoChoangHook() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 6) {
            System.out.println("dùng: AoChoangHook <jar vào> <jar ra> <mã món> <ảnh1> <ảnh2> <ảnh3>");
            return;
        }
        int maMon = Integer.parseInt(args[2]);
        int a1 = Integer.parseInt(args[3]);
        int a2 = Integer.parseInt(args[4]);
        int a3 = Integer.parseInt(args[5]);

        ZipFile vao = new ZipFile(args[0]);
        ZipOutputStream ra = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(args[1])));
        for (Enumeration<? extends ZipEntry> e = vao.entries(); e.hasMoreElements(); ) {
            ZipEntry z = e.nextElement();
            byte[] than = doc(vao.getInputStream(z));
            if (z.getName().endsWith(".class")) {
                byte[] moi = va(than, maMon, a1, a2, a3);
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
        vao.close();
        ra.close();
        System.out.println("áo choàng: đã nối " + daVa + " chỗ, mở " + moChot + " chốt chặn");
        if (daVa == 0) {
            System.out.println("  !! không tìm thấy hàm tra bảng áo choàng -- client đã đổi, cần dò lại");
        }
    }

    private static byte[] va(byte[] than, int maMon, int a1, int a2, int a3) {
        ClassNode cn = new ClassNode();
        try {
            // Bỏ bảng khung lúc đọc: ta sắp làm nó sai, và sẽ hạ phiên bản lớp để khỏi cần nó.
            new ClassReader(than).accept(cn, ClassReader.SKIP_FRAMES);
        } catch (Throwable ex) {
            return null;
        }
        boolean doi = false;
        doi |= moChot(cn);
        for (Object mo : cn.methods) {
            MethodNode mn = (MethodNode) mo;
            if (mn.instructions == null || !mn.desc.endsWith(")[I") || !docTruongW(mn)) {
                continue;
            }
            AbstractInsnNode dau = mn.instructions.getFirst();
            LabelNode cu = new LabelNode();
            InsnList them = new InsnList();
            if (System.getenv("AO_CHOANG_LOG") != null) {
                // In thẳng giá trị w mỗi lần client hỏi bảng áo choàng. Không thêm nhánh rẽ nào
                // nên bảng khung ngăn xếp vẫn đúng. MicroEmulator gom System.out vào build/play-*.log.
                them.add(new FieldInsnNode(Opcodes.GETSTATIC, "java/lang/System", "out",
                        "Ljava/io/PrintStream;"));
                them.add(new VarInsnNode(Opcodes.ALOAD, 0));
                them.add(new FieldInsnNode(Opcodes.GETFIELD, cn.name, "w", "S"));
                them.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream",
                        "println", "(I)V"));
            }
            them.add(new VarInsnNode(Opcodes.ALOAD, 0));
            them.add(new FieldInsnNode(Opcodes.GETFIELD, cn.name, "w", "S"));
            them.add(soNguyen(maMon));
            them.add(new JumpInsnNode(Opcodes.IF_ICMPNE, cu));
            them.add(new InsnNode(Opcodes.ICONST_4));
            them.add(new IntInsnNode(Opcodes.NEWARRAY, Opcodes.T_INT));
            dat(them, 0, a1);
            dat(them, 1, a2);
            dat(them, 2, a3);
            dat(them, 3, a2);       // bốn nhịp đi-về, giống bảng sẵn có của áo choàng cũ
            them.add(new InsnNode(Opcodes.ARETURN));
            them.add(cu);
            mn.instructions.insertBefore(dau, them);
            daVa++;
            doi = true;
        }
        if (!doi) {
            return null;
        }
        if (cn.version > Opcodes.V1_5) {
            cn.version = Opcodes.V1_5;
        }
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        return cw.toByteArray();
    }

    /**
     * Mở chốt chặn đứng ngay trước lời gọi bảng áo choàng.
     *
     * Client chỉ hỏi tới bảng áo choàng khi {@code mảnh.pi[khung].id > 4}, tức khi khung đang vẽ
     * không phải khung mặc định. Nhận ra lớp ấy nhờ nó có đúng ba trường {@code eo:S, uG:B, uH:B}
     * -- đúng bộ (id, dx, dy) của một khung mảnh.
     *
     * Với nhân vật mặc trang phục trang bị 2, điều kiện ấy không thoả, nên áo choàng không bao giờ
     * được hỏi tới -- kể cả áo choàng có sẵn của game. Đổi lệnh rẽ thành "luôn đi tiếp": bỏ hai
     * giá trị đang so rồi nhảy thẳng tới nhánh gọi.
     */
    private static boolean moChot(ClassNode cn) {
        boolean doi = false;
        for (Object mo : cn.methods) {
            MethodNode mn = (MethodNode) mo;
            if (mn.instructions == null) {
                continue;
            }
            for (AbstractInsnNode n = mn.instructions.getFirst(); n != null; n = n.getNext()) {
                if (!(n instanceof FieldInsnNode) || n.getOpcode() != Opcodes.GETFIELD) {
                    continue;
                }
                FieldInsnNode f = (FieldInsnNode) n;
                if (!"eo".equals(f.name) || !"S".equals(f.desc)) {
                    continue;
                }
                AbstractInsnNode b = ke(n);
                AbstractInsnNode c = b == null ? null : ke(b);
                if (b == null || b.getOpcode() != Opcodes.ICONST_4
                        || c == null || c.getOpcode() != Opcodes.IF_ICMPGT) {
                    continue;
                }
                LabelNode nhan = ((JumpInsnNode) c).label;
                InsnList thay = new InsnList();
                thay.add(new InsnNode(Opcodes.POP2));
                thay.add(new JumpInsnNode(Opcodes.GOTO, nhan));
                mn.instructions.insertBefore(c, thay);
                mn.instructions.remove(c);
                moChot++;
                doi = true;
                break;
            }
        }
        return doi;
    }

    private static AbstractInsnNode ke(AbstractInsnNode n) {
        AbstractInsnNode k = n.getNext();
        while (k instanceof LabelNode || k instanceof FrameNode || k instanceof LineNumberNode) {
            k = k.getNext();
        }
        return k;
    }

    /** Hàm cần tìm là hàm duy nhất trả về int[] mà có đọc trường w kiểu short. */
    private static boolean docTruongW(MethodNode mn) {
        for (AbstractInsnNode n = mn.instructions.getFirst(); n != null; n = n.getNext()) {
            if (n instanceof FieldInsnNode) {
                FieldInsnNode f = (FieldInsnNode) n;
                if ("w".equals(f.name) && "S".equals(f.desc)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void dat(InsnList l, int viTri, int giaTri) {
        l.add(new InsnNode(Opcodes.DUP));
        l.add(soNguyen(viTri));
        l.add(soNguyen(giaTri));
        l.add(new InsnNode(Opcodes.IASTORE));
    }

    private static AbstractInsnNode soNguyen(int v) {
        if (v >= -1 && v <= 5) {
            return new InsnNode(Opcodes.ICONST_0 + v);
        }
        if (v >= Byte.MIN_VALUE && v <= Byte.MAX_VALUE) {
            return new IntInsnNode(Opcodes.BIPUSH, v);
        }
        if (v >= Short.MIN_VALUE && v <= Short.MAX_VALUE) {
            return new IntInsnNode(Opcodes.SIPUSH, v);
        }
        return new LdcInsnNode(v);
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
