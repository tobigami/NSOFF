import java.io.DataInputStream;
import java.io.File;
import java.nio.file.Files;

/**
 * Trỏ lời gọi Mob.Fight(Char, int) sang BuNhin.danh(Mob, Char, int).
 *
 * Chỗ cần vá nằm trong Mob, ngay sau khi trừ máu quái:
 *
 *     addHp(-damage);
 *     this.Fight(p, damage);        <- đúng lời gọi này
 *     zone.getService().attackMonster(damage, false, this);
 *
 * invokevirtual Fight(Char,int) và invokestatic danh(Mob,Char,int) đều là 3 byte, và ngăn xếp
 * giống hệt (this, p, dame) nên thay tại chỗ được, không byte nào xê dịch.
 *
 * Mob dùng Lombok nên không biên dịch lại được -- lombok không có trong kho cục bộ và máy đang
 * làm việc ngoại tuyến, nên đây là đường duy nhất.
 *
 * Chỉ vá lời gọi nào có "attackMonster" theo ngay sau, để những chỗ gọi Fight khác giữ nguyên.
 *
 * Usage: BuNhinHook [Mob.class]
 */
public final class BuNhinHook {

    private static final int INVOKEVIRTUAL = 0xB6, INVOKESTATIC = 0xB8;
    private static final int TAM_NHIN = 20;

    public static void main(String[] args) throws Exception {
        File f = new File(args.length > 0 ? args[0] : "build/srvpatch/com/nsoz/mob/Mob.class");
        byte[] d = Files.readAllBytes(f.toPath());

        int fight = ref(d, "com/nsoz/mob/Mob", "Fight", "(Lcom/nsoz/model/Char;I)V");
        int attack = ref(d, null, "attackMonster", "(IZLcom/nsoz/mob/Mob;)V");
        if (fight < 0) {
            System.out.println("!! không thấy Mob.Fight -- chưa vá");
            return;
        }
        int moi = them(d, "com/nsoz/server/BuNhin", "danh",
                "(Lcom/nsoz/mob/Mob;Lcom/nsoz/model/Char;I)V");
        byte[] out = moi >= 0 ? POOL : null;
        if (out == null) {
            System.out.println("!! không nối được hằng mới -- chưa vá");
            return;
        }
        int demFight = 0;
        for (int i = 0; i + 2 < out.length; i++) {
            if ((out[i] & 0xFF) == INVOKEVIRTUAL
                    && (((out[i + 1] & 0xFF) << 8) | (out[i + 2] & 0xFF)) == fight) {
                demFight++;
            }
        }
        int n = 0;
        for (int i = 0; i + 2 < out.length; i++) {
            if ((out[i] & 0xFF) != INVOKEVIRTUAL) {
                continue;
            }
            if ((((out[i + 1] & 0xFF) << 8) | (out[i + 2] & 0xFF)) != fight) {
                continue;
            }
            // Trong Mob có nhiều lời gọi Fight nên phải lọc bằng attackMonster đứng ngay sau.
            // Trong Char chỉ có đúng một lời gọi -- chính là đường đánh của người chơi -- nên
            // không cần mốc, và cũng không được đòi mốc vì ngữ cảnh hai bên khác nhau.
            if (attack >= 0 && demFight > 1 && !theoSau(out, i + 3, attack)) {
                continue;
            }
            out[i] = (byte) INVOKESTATIC;
            out[i + 1] = (byte) (moi >> 8);
            out[i + 2] = (byte) moi;
            n++;
        }
        if (n == 0) {
            System.out.println("!! thấy Mob.Fight nhưng không chỗ nào hợp mẫu -- chưa vá");
            return;
        }
        Files.write(f.toPath(), out);
        System.out.println("  Mob.Fight -> BuNhin.danh: " + n + " chỗ");
    }

    private static boolean theoSau(byte[] code, int from, int ref) {
        for (int k = from; k < from + TAM_NHIN && k + 2 < code.length; k++) {
            if ((code[k] & 0xFF) == INVOKEVIRTUAL
                    && (((code[k + 1] & 0xFF) << 8) | (code[k + 2] & 0xFF)) == ref) {
                return true;
            }
        }
        return false;
    }

    /** Lớp đã dựng lại kèm hằng mới; đặt ở đây cho gọn vì chỉ dùng một lần. */
    private static byte[] POOL;

    /** Nối sáu hằng mới vào cuối bể, trả về chỉ số Methodref. */
    private static int them(byte[] d, String cls, String name, String desc) throws Exception {
        java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
        java.io.DataOutputStream o = new java.io.DataOutputStream(bo);
        DataInputStream in = new DataInputStream(new java.io.ByteArrayInputStream(d));
        o.writeInt(in.readInt());
        o.writeShort(in.readUnsignedShort());
        o.writeShort(in.readUnsignedShort());
        int count = in.readUnsignedShort();
        o.writeShort(count + 6);
        java.io.ByteArrayOutputStream pb = new java.io.ByteArrayOutputStream();
        java.io.DataOutputStream pool = new java.io.DataOutputStream(pb);
        for (int i = 1; i < count; i++) {
            int tag = in.readUnsignedByte();
            pool.writeByte(tag);
            switch (tag) {
                case 1: {
                    byte[] s = new byte[in.readUnsignedShort()];
                    in.readFully(s);
                    pool.writeShort(s.length);
                    pool.write(s);
                    break;
                }
                case 5: case 6: sao(in, pool, 8); i++; break;
                case 9: case 10: case 11: case 12: case 3: case 4: case 17: case 18:
                    sao(in, pool, 4); break;
                case 15: sao(in, pool, 3); break;
                case 7: case 8: case 16: case 19: case 20: sao(in, pool, 2); break;
                default: throw new IllegalStateException("tag lạ " + tag);
            }
        }
        int base = count;
        byte[] c = cls.getBytes("UTF-8"), m = name.getBytes("UTF-8"), s = desc.getBytes("UTF-8");
        pool.writeByte(1); pool.writeShort(c.length); pool.write(c);
        pool.writeByte(7); pool.writeShort(base);
        pool.writeByte(1); pool.writeShort(m.length); pool.write(m);
        pool.writeByte(1); pool.writeShort(s.length); pool.write(s);
        pool.writeByte(12); pool.writeShort(base + 2); pool.writeShort(base + 3);
        pool.writeByte(10); pool.writeShort(base + 1); pool.writeShort(base + 4);
        o.write(pb.toByteArray());
        byte[] rest = new byte[in.available()];
        in.readFully(rest);
        o.write(rest);
        POOL = bo.toByteArray();
        return base + 5;
    }

    private static void sao(DataInputStream in, java.io.DataOutputStream o, int n) throws Exception {
        byte[] b = new byte[n];
        in.readFully(b);
        o.write(b);
    }

    /** Chỉ số Methodref theo lớp, tên và mô tả. cls null là bỏ qua phần lớp. */
    private static int ref(byte[] d, String cls, String name, String desc) throws Exception {
        DataInputStream in = new DataInputStream(new java.io.ByteArrayInputStream(d));
        in.readInt(); in.readUnsignedShort(); in.readUnsignedShort();
        int count = in.readUnsignedShort();
        String[] utf8 = new String[count];
        int[] rc = new int[count], rn = new int[count], nn = new int[count], nd = new int[count];
        int[] ci = new int[count];
        for (int i = 1; i < count; i++) {
            int tag = in.readUnsignedByte();
            switch (tag) {
                case 1: {
                    byte[] s = new byte[in.readUnsignedShort()];
                    in.readFully(s);
                    utf8[i] = new String(s, "UTF-8");
                    break;
                }
                case 7: ci[i] = in.readUnsignedShort(); break;
                case 9: case 10: case 11: rc[i] = in.readUnsignedShort(); rn[i] = in.readUnsignedShort(); break;
                case 12: nn[i] = in.readUnsignedShort(); nd[i] = in.readUnsignedShort(); break;
                case 5: case 6: in.skipBytes(8); i++; break;
                case 3: case 4: case 17: case 18: in.skipBytes(4); break;
                case 15: in.skipBytes(3); break;
                case 8: case 16: case 19: case 20: in.skipBytes(2); break;
                default: throw new IllegalStateException("tag lạ " + tag);
            }
        }
        for (int i = 1; i < count; i++) {
            int nat = rn[i];
            if (nat <= 0 || nat >= count) {
                continue;
            }
            if (!name.equals(utf8[nn[nat]]) || !desc.equals(utf8[nd[nat]])) {
                continue;
            }
            if (cls != null && !cls.equals(utf8[ci[rc[i]]])) {
                continue;
            }
            return i;
        }
        return -1;
    }
}
