import java.awt.image.BufferedImage;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

/**
 * Soi một hàng effect_data xem client có đọc lại đúng như server ghi không.
 *
 * Lý do phải có: EffectData.setData() ghi x, y, w, h của mỗi khung bằng **một byte**, và số khung
 * cũng một byte. Vượt trần thì không ai báo lỗi cả -- server vẫn chạy, client vẫn nhận, chỉ có
 * hình vẽ ra là rác hoặc bị cắt. Ràng buộc này nằm im trong một dòng writeByte, không có chỗ nào
 * ghi thành tài liệu, nên mỗi lần thay ảnh hiệu ứng là một lần dễ giẫm phải.
 *
 * Và ba con số trần KHÔNG bằng nhau, đây là chỗ đã trả giá một lần: x/y/w/h tới 255 (client đọc
 * không dấu), số khung ảnh chỉ tới 127 (đọc CÓ dấu), còn chuỗi phát chỉ tới 126 -- trần này thậm
 * chí không đến từ khâu truyền mà từ việc con trỏ chạy chuỗi bên client là một byte. Suy một con
 * số ra cho cả ba là sai.
 *
 * Kiểm bốn thứ, mỗi thứ một dòng KHỚP / SAI:
 *   1. mọi x, y, w, h nằm trong 0..255; số khung ảnh <= 127; chuỗi phát <= 126
 *   2. mỗi khung nằm gọn trong tấm ảnh mức 1 -- kiểm cả hình chữ nhật, không kiểm từng số rời
 *   3. tấm ảnh mức 2, 3, 4 rộng và cao đúng gấp 2, 3, 4 lần tấm mức 1
 *   4. mọi chỉ số khung trong frames và running đều trỏ tới một sprite có thật
 *
 * Usage: EffectCheck [id] [thư mục Data/Img/Effect]
 *        mặc định: 201 work/server/NSO_KEM/Data/Img/Effect
 */
public final class EffectCheck {

    private static final String URL = "jdbc:mysql://127.0.0.1:3306/nso_test?user=root";
    private static final int BYTE_MAX = 255;

    /**
     * Trần độ dài chuỗi phát, và nó KHÔNG bằng trần của x/y/w/h.
     *
     * Độ dài đi qua writeByte rồi readUnsignedByte nên nhìn qua tưởng được 255. Cái chặn nằm ở
     * con trỏ chạy chuỗi bên client: nó là một byte, quay vòng bằng `if (dD > dB.length) dD = 0`.
     * Dài hơn 126 thì con trỏ đếm tới 127 rồi tràn xuống -128, không bao giờ quay vòng được nữa,
     * và client bọc cả khối trong catch rỗng nên hiệu ứng chỉ lặng lẽ đứng hình.
     */
    private static final int SEQ_MAX = 126;

    /** Số khung ảnh đọc bằng readByte CÓ DẤU bên client, khác hẳn x/y/w/h đọc không dấu. */
    private static final int SPRITE_MAX = 127;

    private static int fail = 0;

    private EffectCheck() {
    }

    public static void main(String[] args) throws Exception {
        int id = args.length > 0 ? Integer.parseInt(args[0]) : 201;
        File imgDir = new File(args.length > 1 ? args[1] : "work/server/NSO_KEM/Data/Img/Effect");

        String sprites, frames, running;
        try (Connection c = DriverManager.getConnection(URL); Statement st = c.createStatement()) {
            ResultSet rs = st.executeQuery("SELECT sprites, frames, running FROM effect_data WHERE id = " + id);
            if (!rs.next()) {
                System.out.println("SAI   không có hàng effect_data id " + id);
                System.exit(1);
            }
            sprites = rs.getString("sprites");
            frames = rs.getString("frames");
            running = rs.getString("running");
        }

        int[][] sp = objects(sprites, "x", "y", "w", "h", "id");
        int[][] fr = objects(frames, "id", "dx", "dy");
        int[] run = ints(running);

        System.out.println("hiệu ứng " + id + ": " + sp.length + " khung ảnh, "
                + fr.length + " khung hình, chuỗi phát " + run.length + " nhịp");

        check("số khung ảnh trong 0.." + SPRITE_MAX, sp.length <= SPRITE_MAX, sp.length + "");
        check("chuỗi phát trong 0.." + SEQ_MAX + " (con trỏ chạy chuỗi của client là byte)",
                run.length <= SEQ_MAX, run.length + " nhịp -- dài hơn là hiệu ứng đứng hình");

        BufferedImage z1 = ImageIO.read(new File(imgDir, "1/" + id + ".png"));
        for (int[] s : sp) {
            String tag = "khung " + s[4];
            check(tag + " x,y,w,h lọt một byte",
                    inByte(s[0]) && inByte(s[1]) && inByte(s[2]) && inByte(s[3]),
                    s[0] + "," + s[1] + "," + s[2] + "," + s[3]);
            check(tag + " nằm gọn trong tấm mức 1",
                    s[0] + s[2] <= z1.getWidth() && s[1] + s[3] <= z1.getHeight(),
                    "cần " + (s[0] + s[2]) + "x" + (s[1] + s[3])
                            + ", có " + z1.getWidth() + "x" + z1.getHeight());
        }

        for (int z = 2; z <= 4; z++) {
            BufferedImage im = ImageIO.read(new File(imgDir, z + "/" + id + ".png"));
            check("tấm mức " + z + " gấp đúng " + z + " lần mức 1",
                    im.getWidth() == z1.getWidth() * z && im.getHeight() == z1.getHeight() * z,
                    im.getWidth() + "x" + im.getHeight()
                            + ", cần " + (z1.getWidth() * z) + "x" + (z1.getHeight() * z));
        }

        for (int[] f : fr) {
            check("khung hình trỏ tới sprite có thật", f[0] >= 0 && f[0] < sp.length, "id " + f[0]);
        }
        for (int r : run) {
            check("nhịp phát trỏ tới khung hình có thật", r >= 0 && r < fr.length, "id " + r);
        }

        System.out.println(fail == 0 ? "\nĐẠT" : "\nHỎNG: " + fail + " chỗ");
        System.exit(fail == 0 ? 0 : 1);
    }

    private static boolean inByte(int v) {
        return v >= 0 && v <= BYTE_MAX;
    }

    /** In một dòng cho mỗi phép kiểm; dòng SAI kèm giá trị thật để khỏi phải mở lại CSDL. */
    private static void check(String what, boolean ok, String detail) {
        if (!ok) {
            fail++;
            System.out.println("SAI   " + what + " -- " + detail);
        } else {
            System.out.println("KHỚP  " + what);
        }
    }

    /**
     * Bóc các trường số của từng đối tượng JSON theo thứ tự xuất hiện.
     *
     * Cột trong CSDL là JSON phẳng do chính server sinh, chưa bao giờ lồng sâu, nên đọc bằng
     * biểu thức chính quy trên từng cặp khoá-giá trị là đủ và khỏi kéo thêm thư viện. Cố tình
     * không dùng dạng lồng quantifier -- xem bài học về regex trong tasks/lessons.md.
     */
    private static int[][] objects(String json, String... keys) {
        Matcher obj = Pattern.compile("\\{[^{}]*}").matcher(json);
        java.util.List<int[]> out = new java.util.ArrayList<>();
        while (obj.find()) {
            String body = obj.group();
            int[] row = new int[keys.length];
            for (int i = 0; i < keys.length; i++) {
                Matcher m = Pattern.compile("\"" + keys[i] + "\"\\s*:\\s*(-?\\d+)").matcher(body);
                row[i] = m.find() ? Integer.parseInt(m.group(1)) : Integer.MIN_VALUE;
            }
            out.add(row);
        }
        return out.toArray(new int[0][]);
    }

    private static int[] ints(String json) {
        Matcher m = Pattern.compile("-?\\d+").matcher(json);
        java.util.List<Integer> out = new java.util.ArrayList<>();
        while (m.find()) {
            out.add(Integer.parseInt(m.group()));
        }
        int[] a = new int[out.size()];
        for (int i = 0; i < a.length; i++) {
            a[i] = out.get(i);
        }
        return a;
    }
}
