import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Hào quang Susanoo bao quanh nhân vật -- bộ giáp chakra dựng bằng nét sáng, trong suốt.
 *
 * Vì sao vẽ chứ không mượn: cả bản mình lẫn bản hồi ức đều KHÔNG có hình Susanoo nào. Bên hồi ức
 * món "Susano bọc cửu vĩ" chỉ là vũ khí vô hình (mảnh 217 trỏ vào một tấm 2x2 trong suốt), còn bên
 * mình món "Tà thuật Uchi" ghi "tăng cường sức mạnh triệu hồi Susano" mà trong mã không có lấy một
 * dòng nào về Susano -- mô tả suông.
 *
 * Hình dựng theo đúng thứ tự người ta nhớ về Susanoo: lồng ngực xương sườn, hai vai, mặt thiên cẩu mũi
 * dài, hai cánh tay ôm ra trước. Vẽ bằng nét sáng trên nền trong suốt để nhân vật vẫn nhìn thấy bên trong.
 *
 * Hai trần của máy khách phải nhớ: bề rộng ô sprite KHÔNG quá 127 (nó giữ trong byte có dấu),
 * chiều cao nên dưới 132 -- mức cao nhất trong 230 hiệu ứng sẵn có.
 */
public class VeSusano {

    private static final int SO_KHUNG = 8;
    private static final int RONG = 124;   // sát trần 127 của máy khách, đủ chỗ cho khiên và kiếm
    private static final int CAO = 96;     // dưới trần 132; vẫn chỉ nửa thân trên
    private static final int DY = -80;   // cao hơn bản -70 một chút, theo ý muốn

    /**
     * Xếp khung thành lưới, KHÔNG xếp dọc một hàng dài.
     *
     * Máy khách giữ toạ độ x, y của mỗi ô sprite trong MỘT BYTE -- soát 179 hiệu ứng gốc của game
     * thì x lớn nhất là 244, y lớn nhất 237, không cái nào chạm 256. Xếp dọc 8 khung cao 96 thì
     * ô cuối có y = 672, bên kia đọc ra 672 - 512 = 160 và cắt nhầm chỗ: khung đầu đúng, mấy khung
     * sau trôi dần. Đúng thứ đã làm biểu ngữ "nhảy lên xuống" và làm Susanoo vỡ vụn.
     *
     * 3 cột x 3 hàng: x cao nhất 248, y cao nhất 192 -- cả hai lọt byte.
     */
    private static final int COT = 3;
    private static final int HANG = 3;

    /**
     * Mỗi khung được giữ lại bao nhiêu nhịp máy khách -- đây là NÚM CHỈNH TỐC ĐỘ THỞ.
     *
     * Ảnh chỉ có 8 khung, nên tốc độ đậm/nhạt không nằm ở chỗ vẽ mà nằm ở mảng `running`: lặp mỗi
     * khung n lần thì một vòng thở dài 8*n nhịp. Để 3 thì vòng thở chỉ 24 nhịp, nhìn giật và gấp
     * như đang hụt hơi. Để 6 thì vòng thở 48 nhịp, chậm gấp đôi, ra dáng thứ đang trấn giữ chứ
     * không phải đang chạy.
     *
     * Đổi số này KHÔNG cần vẽ lại ảnh, chỉ cần nạp lại câu SQL và bật lại máy chủ.
     */
    private static final int LAP_KHUNG = 6;

    /**
     * Kiểu tả cái mũi thiên cẩu -- chọn bằng -Dmui=1..4.
     *
     * Vì sao phải có nhiều kiểu: góc CHÍNH DIỆN là góc tệ nhất để tả một cái mũi chìa ra trước.
     * Thứ hướng thẳng về phía người xem thì bị nén lại gần hết chiều dài, nên vẽ nó dài ra chỉ
     * làm nó THÕNG XUỐNG chứ không thành NHÔ RA -- ra cái vòi chứ không ra cái mũi. Muốn thấy
     * chiều dài thật thì phải quay đầu đi ít nhiều, mà quay đầu thì lại lệch với thân đang đứng
     * chính diện. Bốn kiểu dưới đây là bốn cách cân bằng khác nhau giữa hai cái đó.
     *
     *   0 = BẢN CÓ SỪNG, không mũi dài  <-- ĐANG DÙNG (chủ dự án chọn)
     *   1 = quay ba phần tư, mũi chìa qua mép má
     *   2 = quay nghiêng hẳn, mũi thành bóng cắt rõ nhất
     *   3 = chính diện, mũi ngắn và kín đáo
     *   4 = chính diện, không đắp khối mũi -- chỉ gợi bằng sáng tối
     *
     * VÌ SAO QUAY VỀ KIỂU 0 DÙ NÓ SAI NGUYÊN TÁC: Susanoo của Itachi đúng là mặt thiên cẩu mũi
     * dài, không sừng -- nhưng đó là chuyện ở cỡ tranh vẽ. Trong game cả hình cao 96 điểm ảnh,
     * cái đầu chỉ chiếm chừng 15, và ở cỡ đó cái mũi không đọc ra được bằng bất cứ kiểu nào (đã
     * so cả bốn, xem hai bản draft trong luutru/hieu-ung/255/). Cặp sừng thì ngược lại: nó nằm
     * ngoài đường bao nên còn sống sót khi thu nhỏ. Đúng nguyên tác mà không nhìn ra thì thua
     * sai nguyên tác mà nhận ra được -- nên giữ sừng.
     *
     * Bốn kiểu kia so ở cỡ thật cho kết luận: trong game cả hình cao 96 điểm ảnh,
     * cái đầu chỉ chiếm chừng 15 -- ở cỡ đó KHÔNG kiểu nào đọc ra mũi cả. Nên tiêu chí không còn
     * là "kiểu nào tả mũi đẹp nhất" mà là "kiểu nào không làm hỏng thứ khác". Kiểu 1 và 2 quay
     * đầu nên bóng đầu lệch hẳn, thu nhỏ lại nhìn như cái đầu bị méo; kiểu 4 thì trống trơn.
     * Kiểu 3 giữ đầu cân, mũi vừa đủ để ai phóng to sẽ thấy, ai chơi bình thường không thấy gợn.
     */
    /**
     * Susanoo của ai -- chọn bằng -Dnhanvat=itachi|sasuke.
     *
     * Hai thứ tách theo nhân vật, và chọn đúng hai thứ đó là có lý do: cả hai đều SỐNG SÓT khi
     * thu về cỡ thật (xem bài học trong tasks/lessons.md).
     *   - MÀU: đỏ cam cho Itachi, tím cho Sasuke. Màu không phụ thuộc kích thước, nhỏ mấy cũng rõ.
     *   - VŨ KHÍ: Itachi ôm khiên Bát Chỉ Kính + kiếm Totsuka; Sasuke giương CUNG. Cung nằm hẳn
     *     ngoài đường bao thân, in bóng lên nền, nên ở cỡ thật vẫn nhận ra ngay -- khác hẳn mấy
     *     chi tiết trên mặt.
     * Phần thân, sườn, vai dùng chung: đó là bộ giáp chakra, ai cũng vậy.
     */
    private static final String NHAN_VAT = System.getProperty("nhanvat", "itachi").toLowerCase();
    private static final boolean SASUKE = "sasuke".equals(NHAN_VAT);
    private static final boolean MADARA = "madara".equals(NHAN_VAT);

    /**
     * Ba Susanoo phải khác nhau ở DÁNG THÂN, không chỉ ở màu:
     *   Itachi -- lồng ngực hở, sườn cong, bo tròn.        Bộ xương chakra.
     *   Sasuke -- tấm giáp hình chữ V, cạnh gãy góc.       Bộ giáp mỏng.
     *   Madara -- giáp lá xếp hàng ngang, thân bè, đáy bằng. Giáp trận nặng.
     * Bài học đã trả giá: lần đầu Sasuke dùng chung khung của Itachi, kết quả chỉ như bản tô lại
     * màu tím. Ở cỡ thật thì đường bao là thứ duy nhất còn phân biệt được.
     */

    /**
     * Bố cục vũ khí -- chọn bằng -Dcanxung=0|1|2.
     *
     * VÌ SAO CÓ CÁI NÀY: máy khách KHÔNG lật hiệu ứng theo hướng nhân vật, và không thể bắt nó
     * làm vậy -- gói tin di chuyển chỉ mang x với y, máy chủ không bao giờ biết nhân vật quay bên
     * nào (xem Char.move). Nên vũ khí chìa hẳn sang một bên sẽ luôn chỉ về một phía dù người chơi
     * quay đi đâu. Cách chữa duy nhất không phải sửa máy khách là ĐỪNG VẼ THỨ CÓ HƯỚNG.
     *
     *   0 = cung chìa sang trái (đúng dáng bắn, nhưng lệch khi nhân vật quay phải)
     *   1 = cung giương chính diện -- ĐÃ THỬ VÀ BỎ: ở cỡ thật nó đọc ra cái vòng bầu dục
     *       quanh người chứ không ra cây cung, mất sạch dấu hiệu nhận dạng
     *   2 = bỏ hẳn vũ khí -- cân nhưng còn mỗi màu tím để phân biệt với Itachi
     *   4 = HÀO QUANG (mặc định): vòng chakra lớn sau lưng, ba tomoe xoay chậm. Không tay, không cánh.
     *   5 = ÁO CHOÀNG: ĐÃ THỬ VÀ BỎ -- định làm dải mềm buông xuống, ra một mớ gai nhọn,
     *       vẫn còn hơi hướng cánh mà lại rối hơn.
     *   6 = CỘT LỬA: ĐÃ THỬ VÀ BỎ -- lưỡi lửa xếp chồng thu nhỏ lại thành hai cái giàn
     *       giáo hình chữ nhật, chẳng ra lửa.
     *
     * Bốn, năm, sáu đều BỎ HẲN HAI TAY. Tay ở cỡ này luôn hỏng: vẽ mảnh thì thành que, vẽ dày
     * thì che mất thân, mà khớp nối thì nổi thành hòn bi. Thân giáp với cái đầu tự đứng được.
     *
     *   3 = ĐÔI CÁNH: bỏ vì vẽ nan + mép cắt thuỳ, đúng hai thứ tạo nên cánh dơi. Susanoo hoàn thiện của Sasuke vốn có cánh, nên đây không phải
     *       chế ra cho tiện mà là lấy đúng một đặc trưng khác của nhân vật. Cánh đối xứng nên
     *       quay hướng nào cũng đúng, lại xoè hẳn ra NGOÀI đường bao thân nên ở cỡ thật vẫn
     *       đọc được -- đúng tiêu chí đã rút ra ở tasks/lessons.md.
     */
    private static final int CAN_XUNG = Integer.parseInt(System.getProperty("canxung", "4"));

    private static final int KIEU_MUI = Integer.parseInt(System.getProperty("mui", "0"));

    public static void main(String[] args) throws Exception {
        String goc = args.length > 0 ? args[0] : "work/server/NSO_KEM/Data/Img/Effect";
        int ma = args.length > 1 ? Integer.parseInt(args[1]) : 255;
        String raSql = args.length > 2 ? args[2] : "/tmp/susano-" + NHAN_VAT + ".sql";
        int mauChinh = MADARA ? 0x3A6FE0 : (SASUKE ? 0x8A3FD0 : 0xD8322C);
        int mauSang  = MADARA ? 0xC2DBFF : (SASUKE ? 0xE2C4FF : 0xFFC08A);
        ve(goc, ma, mauChinh, mauSang);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(raSql), StandardCharsets.UTF_8)) {
            w.write("-- Hào quang Susanoo, sinh bằng tools/VeSusano.java\n" + cauSql(ma) + "\n");
        }
        System.out.println("xong hiệu ứng " + ma + " (" + RONG + "x" + CAO + ") -> " + raSql);
    }

    /**
     * Susanoo của Itachi: nửa thân trên, tay trái ôm Bát Chỉ Kính, tay phải cầm kiếm Totsuka.
     *
     * Hai món trong tay là thứ ai cũng nhận ra ở Susanoo của Itachi, nên chúng phải chiếm chỗ và
     * phải rõ ngay ở cỡ nhỏ: khiên vẽ thành tám cạnh với ba vòng đồng tâm, kiếm vẽ thành lưỡi
     * cong mảnh mọc ra từ chuôi hồ lô. Phần thân giữ vai trò nền -- giáp xếp lớp, sườn kép, ba
     * tomoe giữa ngực.
     *
     * Hoạt ảnh dồn vào ba chỗ: lửa quanh vai, tàn lửa bay lên, và vệt sáng chạy dọc lưỡi kiếm.
     */
    private static void ve(String goc, int ma, int mau, int sang) throws Exception {
        int z = 4, W = RONG * z, H = CAO * z;
        BufferedImage to = new BufferedImage(W, H * SO_KHUNG, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = to.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        Color cM = new Color(mau), cS = new Color(sang);

        for (int k = 0; k < SO_KHUNG; k++) {
            int y0 = k * H;
            float nhip = (float) (0.5 + 0.5 * Math.sin(k / (double) SO_KHUNG * Math.PI * 2));
            float gx = W / 2f;
            // Đậm nhạt theo nhịp, biên độ rộng hẳn để mắt thấy nó "thở".
            //
            // Trước đây nét chỉ dao động 165-220, chênh lệch nhỏ quá nên nhìn như đứng yên. Giờ
            // mở rộng gấp ba: lúc căng thì rõ từng đường gân, lúc lắng thì chỉ còn bóng mờ.
            int mo = (int) (115 + 115 * nhip);   // nét viền: 115 lúc lắng, 230 lúc căng
            int nen = (int) (24 + 34 * nhip);    // phần thịt
            Color net = new Color(cS.getRed(), cS.getGreen(), cS.getBlue(), mo);
            Color vien = new Color(cM.getRed(), cM.getGreen(), cM.getBlue(), mo);
            float ny = y0 + H * 0.66f;      // tâm ngực, hạ thấp hơn bản trước
            float dy = y0 + H * ((MADARA || KIEU_MUI == 0) ? 0.360f : 0.330f);  // bản có sừng giữ chỗ cũ; kiểu mũi dài phải nhích lên

            // 1. Vầng sáng nền -- HAI lớp thay vì một: lớp ngoài rộng mờ (glow thật sự lan ra
            // khỏi hình), lớp trong hẹp đậm hơn (áp sát quanh thân, cho cảm giác chakra bốc lên
            // từ chính khối chứ không phải một cái đèn nền dán phía sau).
            g.setPaint(new RadialGradientPaint(new Point2D.Float(gx, ny), W * 0.58f,
                    new float[] {0f, 0.7f, 1f},
                    new Color[] {new Color(cM.getRed(), cM.getGreen(), cM.getBlue(), ap(nen - 6)),
                        new Color(cM.getRed(), cM.getGreen(), cM.getBlue(), ap(nen / 4)),
                        new Color(mau & 0xFFFFFF, true)}));
            g.fill(new Ellipse2D.Float(gx - W * 0.58f, y0 + H * 0.02f, W * 1.16f, H * 1.0f));
            g.setPaint(new RadialGradientPaint(new Point2D.Float(gx, ny), W * 0.40f,
                    new float[] {0f, 0.65f, 1f},
                    new Color[] {new Color(cM.getRed(), cM.getGreen(), cM.getBlue(), ap(nen + 12)),
                        new Color(cM.getRed(), cM.getGreen(), cM.getBlue(), ap(nen / 2)),
                        new Color(mau & 0xFFFFFF, true)}));
            g.fill(new Ellipse2D.Float(gx - W * 0.40f, y0 + H * 0.26f, W * 0.80f, H * 0.68f));

            if (MADARA) {
                veAuraToa(g, gx, ny, z, cM, cS, mo, k, nhip);
            }

            // 2-3. THÂN. Hai dáng khác hẳn nhau, không phải đổi màu:
            //   Itachi -- LỒNG NGỰC hở, sườn cong, bo tròn. Susanoo của anh là bộ xương chakra.
            //   Sasuke -- GIÁP TRỤ kín, tấm ngực liền, cạnh gãy góc. Susanoo của cậu là bộ giáp.
            // Trước đây cả hai dùng chung khối này nên Sasuke chỉ như bản tô lại màu tím.
            // Madara KHÔNG có nhánh thân riêng nữa -- dùng chung lồng ngực của Itachi.
            //
            // Tôi đã thử hai lần: giáp lá xếp ngang (cứng, nặng nề) rồi áo vải vạt chéo (sai hẳn).
            // Cả hai đều tệ hơn cái khối vốn đã đẹp sẵn. Ở kích thước này, khối ngực có sườn cong
            // là thứ đã được kiểm chứng qua hai nhân vật; đẻ thêm biến thể chỉ để "cho khác" là
            // đổi cái chắc chắn lấy cái chưa biết. Madara khác ở MÀU, GAI và HAI KIẾM -- đủ rồi.
            if (SASUKE) {
                veNgucGiap(g, gx, ny, z, cM, cS, nen, mo, nhip);
            } else {
            Path2D nguc = new Path2D.Float();
            nguc.moveTo(gx - 15 * z, ny - 16 * z);
            nguc.curveTo(gx - 17 * z, ny - 4 * z, gx - 13 * z, ny + 10 * z, gx, ny + 18 * z);
            nguc.curveTo(gx + 13 * z, ny + 10 * z, gx + 17 * z, ny - 4 * z, gx + 15 * z, ny - 16 * z);
            nguc.curveTo(gx + 8 * z, ny - 20 * z, gx - 8 * z, ny - 20 * z, gx - 15 * z, ny - 16 * z);
            nguc.closePath();
            veKhoi(g, nguc, cM, cS, nen, mo);

            // 3. Xương sườn: cung mảnh, mờ dần xuống dưới -- gợi khối chứ không kẻ ô
            g.setStroke(new BasicStroke(1.1f * z, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < 4; i++) {
                float yy = ny - 9 * z + i * 6 * z;
                float r = (13 - i * 1.6f) * z;
                g.setColor(new Color(cS.getRed(), cS.getGreen(), cS.getBlue(), ap(mo - 30 - i * 18)));
                g.draw(new Arc2D.Float(gx - r, yy - 5 * z, r * 2, 11 * z, 200, 140, Arc2D.OPEN));
            }
            g.setColor(new Color(cS.getRed(), cS.getGreen(), cS.getBlue(), ap(mo - 45)));
            g.setStroke(new BasicStroke(1.6f * z, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new Line2D.Float(gx, ny - 14 * z, gx, ny + 14 * z));
            g.setColor(new Color(255, 240, 220, ap(mo - 20)));
            for (int i = 0; i < 3; i++) {
                double a = Math.toRadians(90 + i * 120 + nhip * 25);
                g.fill(new Ellipse2D.Double(gx + Math.cos(a) * 4 * z - 1.3 * z,
                        ny - 2 * z + Math.sin(a) * 4 * z - 1.3 * z, 2.6 * z, 2.6 * z));
            }

            }

            // 4. CỔ VÀ GÁY: nối đầu xuống thân, thứ bản trước thiếu hẳn
            Path2D co = new Path2D.Float();
            co.moveTo(gx - 5 * z, dy + 7 * z);
            co.curveTo(gx - 6 * z, ny - 20 * z, gx - 9 * z, ny - 18 * z, gx - 11 * z, ny - 15 * z);
            co.lineTo(gx + 11 * z, ny - 15 * z);
            co.curveTo(gx + 9 * z, ny - 18 * z, gx + 6 * z, ny - 20 * z, gx + 5 * z, dy + 7 * z);
            co.closePath();
            veKhoi(g, co, cM, cS, nen, mo - 30);

            // 5. ĐẦU: xem veDau -- có bốn kiểu tả mũi, chọn bằng -Dmui
            veDau(g, gx, dy, z, cM, cS, nen, mo);

            // 6. VAI
            if (SASUKE) {
                veVaiGiap(g, gx, ny, z, cM, cS, nen, mo);
            } else {
            for (int b = -1; b <= 1; b += 2) {
                float vx = gx + b * 17 * z, vy = ny - 12 * z;
                Path2D vai = new Path2D.Float();
                vai.moveTo(vx - b * 6 * z, vy - 3 * z);
                vai.curveTo(vx - b * 2 * z, vy - 9 * z, vx + b * 7 * z, vy - 7 * z, vx + b * 8 * z, vy + 1 * z);
                vai.curveTo(vx + b * 8 * z, vy + 7 * z, vx + b * 2 * z, vy + 9 * z, vx - b * 5 * z, vy + 6 * z);
                vai.closePath();
                veKhoi(g, vai, cM, cS, nen + 8, mo);
            }

            }

            // 7. TAY: khối thon dần, vẽ bằng đường cong thay vì nét kẻ dày
            float[] tayTrai, tayPhai;
            if (MADARA) {
                tayTrai = tayMadara(g, gx, ny, -1, z, cM, cS, nen, mo);
                tayPhai = tayMadara(g, gx, ny, 1, z, cM, cS, nen, mo);
            } else if (SASUKE && CAN_XUNG >= 4) {
                tayTrai = new float[] {gx - 24 * z, ny};   // không vẽ tay
                tayPhai = new float[] {gx + 24 * z, ny};
            } else if (SASUKE) {
                tayTrai = tayGiap(g, gx, ny, -1, z, cM, cS, nen, mo);
                tayPhai = tayGiap(g, gx, ny, 1, z, cM, cS, nen, mo);
            } else {
                tayTrai = tay(g, gx, ny, -1, z, cM, cS, nen, mo);
                tayPhai = tay(g, gx, ny, 1, z, cM, cS, nen, mo);
            }

            if (MADARA) {
                veKiemChakra(g, tayTrai, -1, z, cM, cS, nen, mo, k, nhip);
                veKiemChakra(g, tayPhai, 1, z, cM, cS, nen, mo, k, nhip);
            } else if (SASUKE && CAN_XUNG == 4) {
                veHaoQuang(g, gx, ny, z, cM, cS, nen, mo, k, nhip);
            } else if (SASUKE && CAN_XUNG == 5) {
                veAoChoang(g, gx, ny, z, cM, cS, nen, mo, k);
            } else if (SASUKE && CAN_XUNG == 6) {
                veCotLua(g, gx, ny, z, cM, cS, nen, mo, k, nhip);
            } else if (SASUKE && CAN_XUNG == 3) {
                veCanhSusano(g, gx, ny, z, cM, cS, nen, mo, k, nhip);
            } else if (SASUKE && CAN_XUNG == 2) {
                // không vẽ vũ khí
            } else if (SASUKE && CAN_XUNG == 1) {
                veCungChinhDien(g, gx, ny, z, cM, cS, nen, mo, k, nhip);
            } else if (SASUKE) {
                veCungTen(g, tayTrai, tayPhai, z, cM, cS, nen, mo, k, nhip);
            } else {
            // 8. KHIÊN Bát Chỉ Kính ở tay trái: bát giác bo tròn, tô dốc, một vệt sáng lướt
            float kx = tayTrai[0], ky = tayTrai[1] + 2 * z, bk = 13 * z;
            Path2D khien = new Path2D.Float();
            for (int i = 0; i < 8; i++) {
                double a = Math.toRadians(22.5 + i * 45), a2 = Math.toRadians(22.5 + (i + 1) * 45);
                double px = kx + Math.cos(a) * bk, py = ky + Math.sin(a) * bk;
                double qx = kx + Math.cos(a2) * bk, qy = ky + Math.sin(a2) * bk;
                if (i == 0) {
                    khien.moveTo(px, py);
                }
                khien.quadTo((px + qx) / 2 * 1.04 - kx * 0.04, (py + qy) / 2 * 1.04 - ky * 0.04, qx, qy);
            }
            khien.closePath();
            veKhoi(g, khien, cM, cS, nen + 12, mo);
            g.setStroke(new BasicStroke(0.9f * z, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 1; i <= 2; i++) {
                float r = bk * (1 - i * 0.29f);
                g.setColor(new Color(cS.getRed(), cS.getGreen(), cS.getBlue(), ap(mo - 40)));
                g.draw(new Ellipse2D.Float(kx - r, ky - r, r * 2, r * 2));
            }
            g.setColor(new Color(255, 245, 225, ap(mo - 10)));
            g.fill(new Ellipse2D.Float(kx - 2f * z, ky - 2f * z, 4 * z, 4 * z));
            g.setColor(new Color(255, 255, 255, 30 + (int) (55 * nhip)));
            g.setStroke(new BasicStroke(2.2f * z, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            float lx = kx - bk + (k / (float) SO_KHUNG) * bk * 2;
            g.draw(new Line2D.Float(lx, ky - bk * 0.65f, lx - 3 * z, ky + bk * 0.65f));
            // Vành lửa quanh mép khiên: mười hai lưỡi ngắn, dài ngắn lệch nhau theo khung, để
            // khiên cùng chất liệu với kiếm chứ không thành cái đĩa kim loại.
            g.setStroke(new BasicStroke(1.3f * z, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < 12; i++) {
                double a = Math.toRadians(i * 30 + k * 4);
                float cao2 = (2.5f + ((i + k) % 3) * 1.6f) * z;
                g.setColor(new Color(cS.getRed(), cS.getGreen(), cS.getBlue(), ap(mo - 90 - (i % 3) * 15)));
                Path2D lua2 = new Path2D.Float();
                lua2.moveTo(kx + Math.cos(a) * bk * 0.94, ky + Math.sin(a) * bk * 0.94);
                lua2.quadTo(kx + Math.cos(a + 0.25) * (bk + cao2 * 0.6f),
                        ky + Math.sin(a + 0.25) * (bk + cao2 * 0.6f),
                        kx + Math.cos(a) * (bk + cao2), ky + Math.sin(a) * (bk + cao2));
                g.draw(lua2);
            }

            // 9. KIẾM Totsuka ở tay phải: ngắn hơn hẳn, lưỡi cong thon, chuôi hồ lô nhỏ
            float tx = tayPhai[0], ty = tayPhai[1];
            Path2D holo = new Path2D.Float();
            holo.moveTo(tx, ty + 5 * z);
            holo.curveTo(tx + 3.4f * z, ty + 4 * z, tx + 3.4f * z, ty - 1 * z, tx + 1.4f * z, ty - 2.5f * z);
            holo.curveTo(tx + 3 * z, ty - 5 * z, tx - 3 * z, ty - 5 * z, tx - 1.4f * z, ty - 2.5f * z);
            holo.curveTo(tx - 3.4f * z, ty - 1 * z, tx - 3.4f * z, ty + 4 * z, tx, ty + 5 * z);
            holo.closePath();
            veKhoi(g, holo, cM, cS, nen + 20, mo);
            // Lưỡi kiếm dạng LỬA: mép lượn sóng, thon dần, lõi trắng bọc ngoài bởi vầng cam.
            //
            // Kiếm thẳng cạnh sắc trông như thanh sắt; Totsuka là kiếm chakra nên mép phải bò
            // như ngọn lửa. Biên độ sóng đổi theo khung để lưỡi kiếm liếm chứ không đứng im.
            float dai = 26 * z, song = 1.6f * z;
            Path2D luoi = new Path2D.Float();
            luoi.moveTo(tx - 2f * z, ty - 5 * z);
            for (int i = 1; i <= 5; i++) {
                float u = i / 5f;
                float lx2 = tx + (2 + 9 * u) * z + (float) Math.sin(u * 6 + k) * song;
                float ly2 = ty - 5 * z - dai * u;
                float rong = (2f - 1.7f * u) * z;
                luoi.lineTo(lx2 - rong, ly2);
            }
            for (int i = 5; i >= 1; i--) {
                float u = i / 5f;
                float lx2 = tx + (2 + 9 * u) * z + (float) Math.sin(u * 6 + k) * song;
                float ly2 = ty - 5 * z - dai * u;
                float rong = (2f - 1.7f * u) * z;
                luoi.lineTo(lx2 + rong, ly2);
            }
            luoi.closePath();
            // vầng lửa ngoài
            g.setColor(new Color(cM.getRed(), cM.getGreen(), cM.getBlue(), ap(mo - 70)));
            g.setStroke(new BasicStroke(3.4f * z, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(luoi);
            // lõi sáng
            g.setPaint(new GradientPaint(tx, ty, new Color(cS.getRed(), cS.getGreen(), cS.getBlue(), mo),
                    tx + 11 * z, ty - dai, new Color(255, 255, 255, ap(mo - 20))));
            g.fill(luoi);
            // vài lưỡi lửa nhỏ liếm ra khỏi thân kiếm
            g.setStroke(new BasicStroke(1.1f * z, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < 3; i++) {
                float u = 0.25f + i * 0.25f;
                float lx2 = tx + (2 + 9 * u) * z, ly2 = ty - 5 * z - dai * u;
                int b2 = (i + k) % 2 == 0 ? 1 : -1;
                g.setColor(new Color(cS.getRed(), cS.getGreen(), cS.getBlue(), ap(mo - 80)));
                Path2D liem = new Path2D.Float();
                liem.moveTo(lx2, ly2);
                liem.quadTo(lx2 + b2 * 4 * z, ly2 - 2 * z, lx2 + b2 * 2.5f * z, ly2 - 6 * z);
                g.draw(liem);
            }
            float t = k / (float) SO_KHUNG;
            g.setColor(new Color(255, 255, 255, 170));
            g.fill(new Ellipse2D.Float(tx + (2 + 8 * t) * z, ty - (6 + 24 * t) * z, 1.8f * z, 1.8f * z));
            }

            // 10b. Chớp chakra: vài nhánh sét mảnh bung ra khỏi vai và mũ, phân nhánh một lần.
            //
            // Susanoo là năng lượng nứt vỡ chứ không phải bề mặt liền mạch -- một vài tia chớp
            // ngoằn ngoèo phóng ra khỏi silhouette bán mỗi khung ở một vị trí khác, kiểu chakra
            // rò rỉ ra ngoài áp lực của chính nó.
            int soChop = 2 + (k % 2);
            for (int i = 0; i < soChop; i++) {
                float goc0 = ((i * 161 + k * 89) % 360);
                double rad = Math.toRadians(goc0);
                float sx = gx + (float) Math.cos(rad) * 20 * z;
                float sy = ny - 10 * z + (float) Math.sin(rad) * 16 * z;
                float dai2 = (4 + (i * 53 + k * 31) % 6) * z;
                float ex = sx + (float) Math.cos(rad) * dai2;
                float ey = sy + (float) Math.sin(rad) * dai2;
                // gãy khúc giữa đường -- sét không bao giờ thẳng
                float mx = (sx + ex) / 2 + (((i + k) % 5) - 2) * z * 0.8f;
                float my = (sy + ey) / 2 + (((i * 3 + k) % 5) - 2) * z * 0.8f;
                g.setStroke(new BasicStroke(0.6f * z, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER));
                g.setColor(new Color(255, 255, 255, ap(mo - 20)));
                Path2D chop = new Path2D.Float();
                chop.moveTo(sx, sy);
                chop.lineTo(mx, my);
                chop.lineTo(ex, ey);
                g.draw(chop);
                // nhánh phụ ngắn rẽ ra từ điểm gãy
                float bx2 = mx + (float) Math.cos(rad + 1.1) * 3 * z;
                float by2 = my + (float) Math.sin(rad + 1.1) * 3 * z;
                g.setStroke(new BasicStroke(0.4f * z, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(new Color(cS.getRed(), cS.getGreen(), cS.getBlue(), ap(mo - 60)));
                g.draw(new Line2D.Float(mx, my, bx2, by2));
            }

            // 10. Lửa quanh vai và tàn lửa
            g.setStroke(new BasicStroke(1.3f * z, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int b = -1; b <= 1; b += 2) {
                for (int i = 0; i < 3; i++) {
                    float bx = gx + b * (19 + i * 3f) * z;
                    float cao = (5 + ((k + i * 2) % 4) * 2.2f) * z;
                    float chan = ny - 14 * z + i * 3 * z;
                    g.setColor(new Color(cS.getRed(), cS.getGreen(), cS.getBlue(), ap(mo - 60 - i * 18)));
                    Path2D lua = new Path2D.Float();
                    lua.moveTo(bx, chan);
                    lua.curveTo(bx + b * 3 * z, chan - cao * 0.5f, bx + b * 1.5f * z, chan - cao * 0.8f,
                            bx - b * 1.2f * z, chan - cao);
                    g.draw(lua);
                }
            }
            // Tàn lửa: nhiều hạt hơn (11), kích thước lệch mạnh (0.4-1.7z) để có hạt to hạt
            // nhỏ như tro thật, và những hạt nhỏ nhất được vẽ với lõi trắng -- tia lửa nóng nhất
            // luôn trắng ở tâm trước khi ngả sang màu của lửa.
            for (int i = 0; i < 11; i++) {
                float px = gx + ((i * 47 + k * 17) % 72 - 36) * z;
                float py = ny + 14 * z - ((i * 13 + k * 11) % 46) * z;
                float ngau = (i * 31 + k * 7) % 10 / 10f;          // 0..1 giả-ngẫu-nhiên theo hạt
                float r = (0.4f + ngau * 1.3f) * z;
                int aa = Math.max(0, mo - 65 - (int) (ngau * 90));
                g.setColor(new Color(cS.getRed(), cS.getGreen(), cS.getBlue(), aa));
                g.fill(new Ellipse2D.Float(px - r, py - r, r * 2, r * 2));
                if (r < 1.1f * z) {
                    g.setColor(new Color(255, 255, 255, ap(aa - 40)));
                    g.fill(new Ellipse2D.Float(px - r * 0.4f, py - r * 0.4f, r * 0.8f, r * 0.8f));
                }
            }

            // Vài vệt loé bốn cánh (sparkle) rải quanh vai -- điểm nhấn nhỏ, chỉ hiện rõ ở khung
            // đỉnh nhịp để mắt bắt được cảm giác "lấp lánh" chứ không đứng yên.
            if (nhip > 0.72f) {
                for (int i = 0; i < 3; i++) {
                    float a2 = (i * 137 + k * 53) % 360;
                    double rad = Math.toRadians(a2);
                    float px = gx + (float) Math.cos(rad) * 26 * z;
                    float py = ny - 6 * z + (float) Math.sin(rad) * 20 * z;
                    int aa = ap((int) ((nhip - 0.72f) / 0.28f * 200));
                    g.setColor(new Color(255, 255, 255, aa));
                    float r2 = 2.2f * z;
                    g.setStroke(new BasicStroke(0.5f * z, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.draw(new Line2D.Float(px - r2, py, px + r2, py));
                    g.draw(new Line2D.Float(px, py - r2, px, py + r2));
                }
            }
        }
        g.dispose();

        // Ghép tám khung thành LƯỚI 3 cột chứ không xếp dọc một hàng -- xem chú thích ở COT.
        BufferedImage luoi = new BufferedImage(RONG * COT * z, CAO * HANG * z, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gl = luoi.createGraphics();
        for (int i = 0; i < SO_KHUNG; i++) {
            gl.drawImage(to.getSubimage(0, i * CAO * z, RONG * z, CAO * z),
                    (i % COT) * RONG * z, (i / COT) * CAO * z, null);
        }
        gl.dispose();
        to = luoi;

        for (int m = 1; m <= 4; m++) {
            BufferedImage ra = new BufferedImage(RONG * COT * m, CAO * HANG * m, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gg = ra.createGraphics();
            gg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            gg.drawImage(to, 0, 0, ra.getWidth(), ra.getHeight(), null);
            gg.dispose();
            File out = new File(goc + File.separator + m, ma + ".png");
            out.getParentFile().mkdirs();
            ImageIO.write(ra, "png", out);
            ghiBanXem(ra, out, m);
        }
    }

    /**
     * Tô một khối cho có độ nổi: dốc màu từ sáng trên xuống tối dưới, viền sáng, và một vệt
     * highlight mảnh ở mép trên.
     *
     * Nét kẻ đều một màu làm hình bẹt như bản vẽ kỹ thuật; ba lớp này là cách rẻ nhất để mắt đọc
     * ra chỗ lồi chỗ lõm.
     */
    /**
     * Ghi bản xem NGAY CẠNH ảnh thật, đổi đuôi .png thành .temp.png.
     *
     * Ảnh hiệu ứng là một TẤM GHÉP 3x3 khung trên nền trong suốt: mở thẳng ra chỉ thấy một mớ
     * vệt đỏ chồng nhau, không đếm được khung nào ra khung nào. Bản xem đặt nó lên nền tối, kẻ
     * lưới và đánh số từng khung -- nhìn phát là biết khung nào vẽ hụt, và quan trọng hơn là soát
     * được cách xếp lưới có còn lọt một byte không (xem chú thích ở COT).
     *
     * Máy chủ đọc ảnh theo đường dẫn chính xác nên file .temp.png nằm cạnh là vô hại.
     */
    private static void ghiBanXem(BufferedImage sheet, File that, int m) throws Exception {
        int W = sheet.getWidth(), H = sheet.getHeight();
        BufferedImage t = new BufferedImage(W, H + 26, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = t.createGraphics();
        g.setColor(new Color(0x14141A));
        g.fillRect(0, 0, W, H + 26);
        g.setColor(new Color(0x23232B));
        g.fillRect(0, 0, W, H);
        g.drawImage(sheet, 0, 0, null);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 11 + m * 2));
        for (int i = 0; i < SO_KHUNG; i++) {
            int x = (i % COT) * RONG * m, y = (i / COT) * CAO * m;
            g.setColor(new Color(0x50505E));
            g.drawRect(x, y, RONG * m, CAO * m);
            g.setColor(new Color(0xFFD9A0));
            g.drawString("khung " + i, x + 5, y + 15 + m * 2);
        }
        g.setColor(new Color(0xC8C8D2));
        g.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 13));
        g.drawString(that.getName() + "   -   tam ghep " + W + "x" + H + ",  moi khung "
                + (RONG * m) + "x" + (CAO * m) + ",  luoi " + COT + "x" + HANG
                + ",  nhip: moi khung lap " + LAP_KHUNG, 6, H + 18);
        g.dispose();
        ImageIO.write(t, "png", new File(that.getParentFile(),
                that.getName().replaceAll("\\.png$", ".temp.png")));
    }

    /** Kẹp về dải hợp lệ 0..255 -- biên độ nhịp rộng nên vài phép trừ có thể âm. */
    private static int ap(int v) {
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }

    /**
     * Tô một khối với BỐN lớp thay vì ba: nền dốc, viền, highlight mép trên, và giờ thêm RIM
     * LIGHT (viền sáng mảnh bám theo đúng đường bao) cùng bóng đổ dưới đáy.
     *
     * Rim light là thứ khiến vật thể trông có khối trên nền tối: ánh sáng từ hào quang phía sau
     * hắt lên cạnh của mọi khối, một đường viền mảnh sáng hơn hẳn phần thân của nó. Bóng đổ ở đáy
     * (một dải tối hẹp bên trong, sát mép dưới) mô phỏng chỗ khối này khuất bởi khối bên dưới nó.
     */
    /**
     * Đầu Susanoo: mũ giáp, vành trán, khe mắt, và cái mũi thiên cẩu theo kiểu đã chọn.
     *
     * Mũi KHÔNG dùng veKhoi, và đó là chủ ý. veKhoi viết cho khối bè (ngực, vai, mũ): nó tô dốc
     * từ trên xuống và kẻ một vòng cung highlight ngang theo hộp bao. Đưa một khối dài hẹp như
     * cái mũi vào thì vòng cung co lại thành cái móc câu chắn ngang sống mũi, còn dốc trên-xuống
     * làm chóp mũi tối thui trong khi gốc lại sáng -- ngược hẳn với khối tròn xoay.
     */


    /**
     * Hào quang toả: những tia sáng bắn thẳng ra khỏi thân, dài ngắn so le, phập phồng theo nhịp.
     *
     * Vẽ TRƯỚC mọi khối để nó nằm sau lưng -- tia sáng đè lên giáp thì thành mạng nhện. Tia không
     * chạm tới tâm mà bắt đầu từ vành ngoài thân, nếu không phần giữa ngực bị kẻ nát.
     */
    private static void veAuraToa(Graphics2D g, float gx, float ny, float z,
            Color cM, Color cS, int mo, int k, float nhip) {
        float cy = ny - 4 * z;
        for (int i = 0; i < 12; i++) {
            double a = Math.toRadians(i * 30 + k * 2.5);
            float r1 = 26 * z;
            float r2 = r1 + (16 + ((i * 37 + k * 13) % 5) * 6) * z * (0.6f + 0.4f * nhip);
            // Tia THON: bè ở gốc, vuốt nhọn ở ngọn. Kẻ bằng nét đều bề dày thì ra những thanh
            // chữ nhật xanh cắm quanh người -- thô, và không ai đọc ra là ánh sáng.
            float x1 = (float) (gx + Math.cos(a) * r1), y1 = (float) (cy + Math.sin(a) * r1);
            float x2 = (float) (gx + Math.cos(a) * r2), y2 = (float) (cy + Math.sin(a) * r2);
            float w = (1.9f - (i % 3) * 0.45f) * z;
            float nxx = (float) -Math.sin(a) * w, nyy = (float) Math.cos(a) * w;
            Path2D tia = new Path2D.Float();
            tia.moveTo(x1 + nxx, y1 + nyy);
            tia.lineTo(x2, y2);
            tia.lineTo(x1 - nxx, y1 - nyy);
            tia.closePath();
            g.setColor(new Color(cM.getRed(), cM.getGreen(), cM.getBlue(),
                    ap((int) (80 + 85 * nhip) - (i % 3) * 18)));
            g.fill(tia);
        }
    }

    /**
     * Tay Madara: hai đốt giáp dày, vươn chéo ra rồi hạ xuống, bàn tay nắm chuôi kiếm.
     *
     * Bài học từ tay của Sasuke: mảnh thì thành que, khớp tròn sáng thì thành hòn bi. Ở đây đốt
     * dày hẳn (6z), khớp là một TẤM CHẮN vuông chứ không phải hình tròn, và cả cánh tay nằm hẳn
     * ngoài mép thân nên không cắt vào giáp.
     */
    private static float[] tayMadara(Graphics2D g, float gx, float ny, int b, float z,
            Color cM, Color cS, int nen, int mo) {
        // Vung LÊN VÀ RA SAU: vai -> khuỷu hất lên ngoài -> bàn tay ở trên cao.
        // Tay buông xuống như bản trước làm dáng đứng ỉu; vung lên mới ra thế sắp chém.
        // Tay BUÔNG XUỐNG dọc thân, bàn tay ngang hông. Bản trước giơ lên trời -- sai ý và
        // cũng làm hình cao lêu nghêu.
        float vx = gx + b * 13 * z, vy = ny - 13 * z;
        float kx = gx + b * 18 * z, ky = ny + 1 * z;
        float bx = gx + b * 18 * z, by = ny + 15 * z;
        Path2D tren = new Path2D.Float();
        tren.moveTo(vx - b * 4 * z, vy - 4 * z);
        tren.lineTo(vx + b * 4.5f * z, vy - 2 * z);
        tren.lineTo(kx + b * 4 * z, ky + 1 * z);
        tren.lineTo(kx - b * 4 * z, ky - 1 * z);
        tren.closePath();
        veKhoi(g, tren, cM, cS, nen + 10, mo - 5);
        Path2D duoi = new Path2D.Float();
        duoi.moveTo(kx - b * 4 * z, ky - 1 * z);
        duoi.lineTo(kx + b * 4 * z, ky + 1 * z);
        duoi.lineTo(bx + b * 3.4f * z, by + 1 * z);
        duoi.lineTo(bx - b * 3.4f * z, by - 1 * z);
        duoi.closePath();
        veKhoi(g, duoi, cM, cS, nen + 7, mo - 12);
        // Tấm chắn khuỷu -- vuông, không tròn
        Path2D chan = new Path2D.Float();
        chan.moveTo(kx - b * 5 * z, ky - 4 * z);
        chan.lineTo(kx + b * 5.2f * z, ky - 2 * z);
        chan.lineTo(kx + b * 4.6f * z, ky + 4 * z);
        chan.lineTo(kx - b * 4.6f * z, ky + 2 * z);
        chan.closePath();
        veKhoi(g, chan, cM, cS, nen + 16, mo);
        return new float[] {bx, by};
    }

    /**
     * Kiếm CHAKRA, không phải kiếm thép.
     *
     * Khác nhau nằm ở cách tô: thép thì tô đặc rồi kẻ viền, chakra thì chồng nhiều lớp trong suốt
     * -- quầng rộng mờ ngoài cùng, thân, rồi lõi trắng mảnh ở giữa. Rìa không bao giờ là một
     * đường sắc nét mà nhoè dần ra. Thêm vài lưỡi lửa liếm dọc sống kiếm cho nó "sống".
     *
     * Chuôi nằm ĐÚNG chỗ bàn tay, lưỡi hất chéo lên ra ngoài thành hình chữ V với tay bên kia.
     */
    private static void veKiemChakra(Graphics2D g, float[] tay, int b, float z,
            Color cM, Color cS, int nen, int mo, int k, float nhip) {
        float cx = tay[0], cy = tay[1];
        float tx = cx + b * 22 * z, ty = cy - 26 * z;     // mũi vát chéo ra sau, góc thoải
        float ux = tx - cx, uy = ty - cy;
        float len = (float) Math.sqrt(ux * ux + uy * uy);
        float px = -uy / len, py = ux / len;

        // LƯỠI: hai sườn gần SONG SONG suốt thân, chỉ vát ở đoạn mũi.
        //
        // Bản trước vuốt nhọn ngay từ chuôi bằng quadTo -- ra hình nón, tức củ cà rốt phát sáng
        // chứ không ra lưỡi kiếm. Cái làm mắt đọc ra "kiếm" là hai cạnh song song chạy dài, còn
        // mũi nhọn chỉ là đoạn cuối.
        float w1 = 2.3f * z, wg = 1.95f * z;
        // Katana CONG, không thẳng đuỗn. Độ cong nhỏ thôi (4z trên cả chiều dài) nhưng đó chính
        // là thứ khiến lưỡi kiếm mềm mại thay vì như thanh thước kẻ.
        float cong = b * 3.4f * z;
        float mx1 = cx + ux * 0.5f + px * cong, my1 = cy + uy * 0.5f + py * cong;
        Path2D luoi = new Path2D.Float();
        luoi.moveTo(cx + px * w1, cy + py * w1);
        luoi.quadTo(mx1 + px * wg, my1 + py * wg, tx, ty);
        luoi.quadTo(mx1 - px * wg * 0.95f, my1 - py * wg * 0.95f, cx - px * w1, cy - py * w1);
        luoi.closePath();
        g.setStroke(new BasicStroke(3.8f * z, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(cM.getRed(), cM.getGreen(), cM.getBlue(), ap(mo - 150)));
        g.draw(luoi);
        g.setStroke(new BasicStroke(1.7f * z, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(cM.getRed(), cM.getGreen(), cM.getBlue(), ap(mo - 70)));
        g.draw(luoi);
        g.setPaint(new GradientPaint(cx, cy,
                new Color(cS.getRed(), cS.getGreen(), cS.getBlue(), ap(nen + 90)),
                tx, ty, new Color(255, 255, 255, ap(mo))));
        g.fill(luoi);
        g.setStroke(new BasicStroke(1.0f * z, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(255, 255, 255, ap(mo)));
        Path2D song = new Path2D.Float();
        song.moveTo(cx + ux * 0.08f, cy + uy * 0.08f);
        song.quadTo(mx1, my1, tx, ty);
        g.draw(song);

        // Lưỡi lửa chakra liếm dọc sống kiếm
        g.setStroke(new BasicStroke(1.1f * z, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 4; i++) {
            float u = 0.22f + i * 0.2f;
            float ex = cx + ux * u, ey = cy + uy * u;
            int d = ((i + k) % 2 == 0) ? 1 : -1;
            g.setColor(new Color(cS.getRed(), cS.getGreen(), cS.getBlue(), ap(mo - 85 - i * 10)));
            Path2D li = new Path2D.Float();
            li.moveTo(ex, ey);
            li.quadTo(ex + px * d * 5 * z, ey + py * d * 5 * z,
                    ex + ux * 0.14f + px * d * 3 * z, ey + uy * 0.14f + py * d * 3 * z);
            g.draw(li);
        }

        // Chắn tay + chuôi, nằm trong lòng bàn tay
        g.setStroke(new BasicStroke(1.8f * z, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(cS.getRed(), cS.getGreen(), cS.getBlue(), ap(mo - 20)));
        g.draw(new Line2D.Float(cx + px * 5 * z, cy + py * 5 * z, cx - px * 5 * z, cy - py * 5 * z));
        g.setStroke(new BasicStroke(3.0f * z, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(cM.getRed(), cM.getGreen(), cM.getBlue(), ap(mo - 35)));
        g.draw(new Line2D.Float(cx, cy, cx - ux / len * 7 * z, cy - uy / len * 7 * z));

        // Ánh chớp chạy dọc lưỡi
        float t2 = ((k + (b > 0 ? 0 : 4)) % SO_KHUNG) / (float) SO_KHUNG;
        g.setColor(new Color(255, 255, 255, ap((int) (110 + 90 * nhip))));
        g.fill(new Ellipse2D.Float(cx + ux * t2 - 1.6f * z, cy + uy * t2 - 1.6f * z, 3.2f * z, 3.2f * z));
    }


    /** Một tomoe (dấu phẩy): đầu tròn, đuôi vuốt cong. */
    private static Path2D tomoe(float x, float y, float r, double huong) {
        Path2D t = new Path2D.Float();
        float dx = (float) Math.cos(huong), dy = (float) Math.sin(huong);
        float px = -dy, py = dx;
        t.moveTo(x + px * r, y + py * r);
        t.quadTo(x + dx * r * 2.6f + px * r * 0.6f, y + dy * r * 2.6f + py * r * 0.6f,
                x + dx * r * 3.4f, y + dy * r * 3.4f);
        t.quadTo(x + dx * r * 1.4f - px * r * 1.1f, y + dy * r * 1.4f - py * r * 1.1f,
                x - px * r, y - py * r);
        t.quadTo(x - dx * r * 1.5f, y - dy * r * 1.5f, x + px * r, y + py * r);
        t.closePath();
        return t;
    }

    /**
     * HÀO QUANG: một vòng chakra lớn sau lưng, ba tomoe trôi chậm trên vòng.
     *
     * Vòng tròn là hình đối xứng hoàn hảo -- quay hướng nào cũng đúng, và ở cỡ thật nó là thứ
     * duy nhất trong mấy phương án còn giữ nguyên dáng khi thu nhỏ. Tomoe cho nó chất Naruto
     * thay vì thành cái đĩa bay.
     */
    private static void veHaoQuang(Graphics2D g, float gx, float ny, float z,
            Color cM, Color cS, int nen, int mo, int k, float nhip) {
        float cy = ny - 8 * z, r = (33 + 1.5f * nhip) * z;
        g.setStroke(new BasicStroke(4.6f * z, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(cM.getRed(), cM.getGreen(), cM.getBlue(), ap(mo - 120)));
        g.draw(new Ellipse2D.Float(gx - r, cy - r, r * 2, r * 2));
        g.setStroke(new BasicStroke(2.0f * z, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(cM.getRed(), cM.getGreen(), cM.getBlue(), ap(mo - 30)));
        g.draw(new Ellipse2D.Float(gx - r, cy - r, r * 2, r * 2));
        g.setStroke(new BasicStroke(0.8f * z, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(cS.getRed(), cS.getGreen(), cS.getBlue(), ap(mo)));
        g.draw(new Ellipse2D.Float(gx - r, cy - r, r * 2, r * 2));
        float r2 = r - 5 * z;
        g.setColor(new Color(cS.getRed(), cS.getGreen(), cS.getBlue(), ap(mo - 130)));
        g.draw(new Ellipse2D.Float(gx - r2, cy - r2, r2 * 2, r2 * 2));

        for (int i = 0; i < 3; i++) {
            double a = Math.toRadians(i * 120 + k * 45.0 / SO_KHUNG * 8);
            float tx = (float) (gx + Math.cos(a) * r), ty = (float) (cy + Math.sin(a) * r);
            Path2D tm = tomoe(tx, ty, 2.3f * z, a + Math.PI / 2);
            g.setColor(new Color(cS.getRed(), cS.getGreen(), cS.getBlue(), ap(mo)));
            g.fill(tm);
        }
    }

    /**
     * ÁO CHOÀNG: mảng chakra buông từ vai xuống hai bên.
     *
     * Khác cánh ở chỗ KHÔNG có nan và KHÔNG có mép cắt thuỳ -- đó đúng là hai thứ làm đôi cánh
     * kia thành cánh dơi. Ở đây chỉ có những dải mềm dài ngắn so le, rìa dưới bỏ ngỏ để nó tan
     * dần vào nền thay vì kết thúc bằng một đường viền.
     */
    private static void veAoChoang(Graphics2D g, float gx, float ny, float z,
            Color cM, Color cS, int nen, int mo, int k) {
        for (int b = -1; b <= 1; b += 2) {
            float rx = gx + b * 15 * z, ry = ny - 14 * z;
            for (int i = 0; i < 6; i++) {
                float goc = 18 + i * 13 + ((i + k) % 3) * 4;
                double a = Math.toRadians(goc);
                float d = (30 + ((i * 7 + k * 5) % 4) * 5) * z;
                float ex = (float) (rx + b * Math.cos(a) * d);
                float ey = (float) (ry + Math.sin(a) * d);
                float cong = ((i % 2 == 0) ? 5f : -3.5f) * z * b;
                Path2D l = luoiLua(rx, ry, ex, ey, (4.5f - i * 0.35f) * z, cong);
                g.setColor(new Color(cM.getRed(), cM.getGreen(), cM.getBlue(), ap(nen + 26 - i * 3)));
                g.fill(l);
                g.setStroke(new BasicStroke(0.9f * z, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(new Color(cS.getRed(), cS.getGreen(), cS.getBlue(), ap(mo - 90 - i * 8)));
                g.draw(l);
            }
        }
    }

    /** CỘT LỬA: hai cột chakra dựng đứng hai bên, lưỡi lửa chồng lên nhau bốc ngược. */
    private static void veCotLua(Graphics2D g, float gx, float ny, float z,
            Color cM, Color cS, int nen, int mo, int k, float nhip) {
        for (int b = -1; b <= 1; b += 2) {
            float cx = gx + b * 30 * z;
            g.setPaint(new RadialGradientPaint(new Point2D.Float(cx, ny), 15 * z,
                    new float[] {0f, 1f},
                    new Color[] {new Color(cM.getRed(), cM.getGreen(), cM.getBlue(), ap(nen + 30)),
                        new Color(cM.getRed(), cM.getGreen(), cM.getBlue(), 0)}));
            g.fill(new Ellipse2D.Float(cx - 15 * z, ny - 30 * z, 30 * z, 60 * z));
            for (int i = 0; i < 7; i++) {
                float by = ny + 20 * z - i * 7 * z;
                float lech = (((i * 13 + k * 9) % 5) - 2) * 1.6f * z;
                float cao = (10 + ((i * 5 + k) % 3) * 3) * z;
                Path2D l = luoiLua(cx + lech, by, cx + lech * 0.4f, by - cao,
                        (4.6f - i * 0.45f) * z, ((i + k) % 2 == 0 ? 2.6f : -2.6f) * z);
                g.setColor(new Color(cM.getRed(), cM.getGreen(), cM.getBlue(), ap(nen + 34 - i * 4)));
                g.fill(l);
                g.setStroke(new BasicStroke(0.9f * z, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(new Color(cS.getRed(), cS.getGreen(), cS.getBlue(), ap(mo - 85 - i * 9)));
                g.draw(l);
            }
        }
    }

    /**
     * Đôi cánh chakra, xoè đối xứng sau lưng.
     *
     * Ba lớp dựng nên một cánh, và thiếu lớp nào cũng hỏng:
     *   - MÀNG: khối trong mờ nối các nan, cho cánh có diện tích. Chỉ vẽ nan không thì ra cái cào.
     *   - NAN: bốn thanh thon toả từ vai, đây là thứ giữ được dáng ở cỡ nhỏ.
     *   - RÌA: mép ngoài cắt thành ba thuỳ cong. Mép thẳng đọc ra cánh dơi cao su; cắt thuỳ mới
     *     ra thứ đang cháy.
     *
     * Cánh KHÔNG vỗ theo khung -- chỉ xoè thêm rất nhẹ theo nhịp thở. Vỗ cánh trong 8 khung lặp
     * vô hạn nhìn như con bướm; Susanoo là thứ đứng trấn, cánh phải như đang chờ.
     */
    private static void veCanhSusano(Graphics2D g, float gx, float ny, float z,
            Color cM, Color cS, int nen, int mo, int k, float nhip) {
        float xoe = 1f + 0.06f * nhip;
        for (int b = -1; b <= 1; b += 2) {
            float rx = gx + b * 14 * z, ry = ny - 9 * z;      // gốc cánh, sau vai
            // Bốn nan toả lên và ra
            float[][] nan = {{34, -62}, {40, -40}, {39, -20}, {31, -2}};
            float[] tx = new float[4], tyy = new float[4];
            for (int i = 0; i < 4; i++) {
                double a = Math.toRadians(nan[i][1]);
                float d = nan[i][0] * z * xoe;
                tx[i] = (float) (rx + b * Math.cos(a) * d);
                tyy[i] = (float) (ry + Math.sin(a) * d);
            }
            // Màng: gốc -> đầu nan 0 -> lượn qua các đầu nan -> về gốc
            Path2D mang = new Path2D.Float();
            mang.moveTo(rx, ry);
            mang.lineTo(tx[0], tyy[0]);
            for (int i = 0; i < 3; i++) {
                float mx = (tx[i] + tx[i + 1]) / 2, my = (tyy[i] + tyy[i + 1]) / 2;
                // kéo điểm giữa vào trong -> mép lõm thành thuỳ
                mang.quadTo(rx + (mx - rx) * 0.74f, ry + (my - ry) * 0.74f, tx[i + 1], tyy[i + 1]);
            }
            mang.closePath();
            g.setColor(new Color(cM.getRed(), cM.getGreen(), cM.getBlue(), ap(nen + 10)));
            g.fill(mang);
            g.setStroke(new BasicStroke(1.1f * z, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(cM.getRed(), cM.getGreen(), cM.getBlue(), ap(mo - 55)));
            g.draw(mang);

            for (int i = 0; i < 4; i++) {
                Path2D n2 = new Path2D.Float();
                n2.moveTo(rx, ry);
                n2.quadTo(rx + (tx[i] - rx) * 0.55f, ry + (tyy[i] - ry) * 0.62f, tx[i], tyy[i]);
                netMem(g, n2, cM, cS, mo - 20, 3.2f * z, 1.5f * z, 0.7f * z);
            }

            // Tàn lửa bay lên men theo mép cánh
            g.setColor(new Color(cS.getRed(), cS.getGreen(), cS.getBlue(), ap(mo - 105)));
            for (int i = 0; i < 4; i++) {
                float u = (i + k * 0.5f) % 4 / 4f;
                float px = tx[0] + (tx[3] - tx[0]) * u, py = tyy[0] + (tyy[3] - tyy[0]) * u;
                float r2 = (0.9f + (i % 2) * 0.6f) * z;
                g.fill(new Ellipse2D.Float(px - r2, py - r2 - u * 4 * z, r2 * 2, r2 * 2));
            }
        }
    }

    /**
     * Cung giương CHÍNH DIỆN: nhìn thẳng vào mặt cung, hai cánh cong đều sang hai bên.
     *
     * Bố cục này đối xứng tuyệt đối qua trục dọc, nên nhân vật quay trái hay quay phải đều không
     * lộ ra là hiệu ứng đứng yên -- đó là toàn bộ lý do nó tồn tại. Mũi tên chĩa thẳng vào người
     * xem nên chỉ còn thấy đầu mũi: một điểm trắng bọc hắc hoả ngay giữa ngực, cũng đối xứng.
     */
    private static void veCungChinhDien(Graphics2D g, float gx, float ny, float z,
            Color cM, Color cS, int nen, int mo, int k, float nhip) {
        float r = 21 * z, be = 13 * z, cy = ny - 2 * z;
        for (int b = -1; b <= 1; b += 2) {
            Path2D canh = new Path2D.Float();
            canh.moveTo(gx + b * 3 * z, cy - r);
            canh.curveTo(gx + b * (be + 3) * z, cy - r * 0.55f, gx + b * be * z, cy + r * 0.12f,
                    gx + b * (be - 3) * z, cy + r * 0.62f);
            canh.curveTo(gx + b * (be - 6) * z, cy + r * 0.9f, gx + b * 6 * z, cy + r * 0.98f,
                    gx + b * 3 * z, cy + r);
            netMem(g, canh, cM, cS, mo, 4.6f * z, 2.1f * z, 0.9f * z);
        }
        // Dây cung: hai đoạn thẳng khép về tâm ngực
        g.setStroke(new BasicStroke(0.7f * z, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(cS.getRed(), cS.getGreen(), cS.getBlue(), ap(mo - 45)));
        g.draw(new Line2D.Float(gx - 3 * z, cy - r, gx, cy));
        g.draw(new Line2D.Float(gx + 3 * z, cy - r, gx, cy));
        g.draw(new Line2D.Float(gx - 3 * z, cy + r, gx, cy));
        g.draw(new Line2D.Float(gx + 3 * z, cy + r, gx, cy));

        // Hắc hoả bọc đầu mũi tên đang chĩa thẳng vào người xem
        g.setColor(new Color(10, 2, 14, ap((int) (150 + 60 * nhip))));
        g.fill(new Ellipse2D.Float(gx - 5 * z, cy - 5 * z, 10 * z, 10 * z));
        for (int i = 0; i < 10; i++) {
            double a = Math.toRadians(i * 36 + Math.sin(k * 0.8 + i * 1.7) * 14);
            float dai3 = (4.5f + ((i * 31 + k * 17) % 6) * 1.5f) * z;
            float ex = (float) (gx + Math.cos(a) * dai3), ey = (float) (cy + Math.sin(a) * dai3);
            float cong3 = (2.0f + ((i * 13 + k) % 3) * 1.4f) * ((i + k) % 2 == 0 ? 1 : -1) * z;
            Path2D l3 = luoiLua(gx, cy, ex, ey, 1.9f * z, cong3);
            g.setColor(new Color(8, 1, 12, ap(215)));
            g.fill(l3);
            g.setStroke(new BasicStroke(0.7f * z, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(186, 120, 255, ap((int) (105 + 75 * nhip) - (i % 3) * 22)));
            g.draw(l3);
        }
        float q = (2.4f + 1.6f * nhip) * z;
        g.setColor(new Color(255, 255, 255, ap(mo)));
        g.fill(new Ellipse2D.Float(gx - q, cy - q, q * 2, q * 2));
    }

    /**
     * Cung tên của Sasuke: tay trái giương cung, tay phải kéo dây, mũi tên chakra bọc hắc hoả.
     *
     * BA NGUYÊN TẮC ở đây, đều rút ra từ mấy lần vẽ hỏng trước:
     *
     * 1. Cánh cung dựng bằng NÉT, không bằng khối kín. Khối kín thì hai sườn cong khép lại thành
     *    một cái thấu kính, thu nhỏ ra hình con diều chứ không ra cung.
     *
     * 2. Không có nét nào vẽ một lần. Mỗi đường đều chồng BA LỚP -- quầng rộng mờ, thân màu chính,
     *    lõi sáng mảnh. Vẽ một lớp thì ra đường kẻ kỹ thuật, cứng đơ; ba lớp mới ra thứ phát sáng
     *    và mềm. Đây là chỗ khác nhau giữa "cây cung" và "chakra hình cây cung".
     *
     * 3. Cánh cung có ĐẦU HẤT NGƯỢC (recurve): hai đầu cong quặt về phía người bắn thay vì vuốt
     *    thẳng. Một cung cong đều là một cung tù; đầu hất ngược cho đường bao hai lần đổi chiều,
     *    nhìn vừa cong hơn vừa dứt khoát hơn dù cùng độ phình.
     */
    private static void veCungTen(Graphics2D g, float[] tayTrai, float[] tayPhai, float z,
            Color cM, Color cS, int nen, int mo, int k, float nhip) {
        float kx = tayTrai[0], ky = tayTrai[1];
        float tx = tayPhai[0], ty = tayPhai[1];
        float r = 20 * z;                        // nửa chiều dài cánh cung
        float dx = kx + 3 * z, dy2 = ky - 5 * z; // gốc dây; nhấc lên cho cung đứng giữa hình
        float nx = dx - 13 * z;                  // bụng cung phình sang trái
        // Toàn bộ cụm cung-tên-lửa phải nằm LỌT trong khung 124 điểm ảnh. Bản trước để bụng cung
        // 17z và tên dài 11z nữa, cộng thêm lưỡi hắc hoả toả ra là chóp trái âm -- lửa bị xén cụt
        // ngay mép ảnh. Có hàm kiểm ở cuối tệp soát chuyện này thay cho mắt.

        // Cánh cung recurve
        Path2D canh = new Path2D.Float();
        canh.moveTo(dx + 5.5f * z, dy2 - r);
        canh.curveTo(dx - 3 * z, dy2 - r * 0.88f, nx + 1.5f * z, dy2 - r * 0.44f, nx, dy2);
        canh.curveTo(nx + 1.5f * z, dy2 + r * 0.44f, dx - 3 * z, dy2 + r * 0.88f,
                dx + 5.5f * z, dy2 + r);
        netMem(g, canh, cM, cS, mo, 5.4f * z, 2.4f * z, 1.0f * z);

        // Lưỡi lửa liếm dọc sống cung -- ngắn, mảnh, lệch nhau theo khung
        g.setStroke(new BasicStroke(0.9f * z, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 9; i++) {
            float u = -0.88f + i * 0.22f;
            float px = nx + 1.2f * z + Math.abs(u) * 11 * z, py = dy2 + u * r;
            int b = (i + k) % 2 == 0 ? 1 : -1;
            g.setColor(new Color(cS.getRed(), cS.getGreen(), cS.getBlue(), ap(mo - 100 - (i % 3) * 14)));
            Path2D li = new Path2D.Float();
            li.moveTo(px, py);
            li.quadTo(px - 4.5f * z, py + b * 1.8f * z, px - 7.5f * z, py + b * 0.7f * z);
            g.draw(li);
        }

        // Dây cung: mảnh và mờ, chỉ là sợi sáng chứ không phải sợi cáp
        float vx = tx - 1.5f * z, vy = ty - 1 * z;
        g.setStroke(new BasicStroke(1.6f * z, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(cM.getRed(), cM.getGreen(), cM.getBlue(), ap(mo - 105)));
        g.draw(new Line2D.Float(dx + 5.5f * z, dy2 - r, vx, vy));
        g.draw(new Line2D.Float(dx + 5.5f * z, dy2 + r, vx, vy));
        g.setStroke(new BasicStroke(0.6f * z, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(cS.getRed(), cS.getGreen(), cS.getBlue(), ap(mo - 45)));
        g.draw(new Line2D.Float(dx + 5.5f * z, dy2 - r, vx, vy));
        g.draw(new Line2D.Float(dx + 5.5f * z, dy2 + r, vx, vy));

        // Thân mũi tên: KHỐI THON chứ không phải nét kẻ -- đuôi bè, mũi vuốt nhỏ. Nét kẻ đều một
        // bề dày đọc ra cái que; thon dần mới ra luồng khí đang dồn về phía trước.
        float mx = nx - 6 * z;
        Path2D than = new Path2D.Float();
        than.moveTo(vx, vy - 1.15f * z);
        than.quadTo((vx + mx) / 2, vy - 0.85f * z, mx, vy - 0.35f * z);
        than.quadTo((vx + mx) / 2, vy + 0.85f * z, vx, vy + 1.15f * z);
        than.closePath();
        g.setColor(new Color(cM.getRed(), cM.getGreen(), cM.getBlue(), ap(mo - 110)));
        g.setStroke(new BasicStroke(3.4f * z, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(than);
        g.setColor(new Color(cM.getRed(), cM.getGreen(), cM.getBlue(), ap(mo - 60)));
        g.setStroke(new BasicStroke(1.7f * z, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(than);
        g.setPaint(new GradientPaint(vx, vy, new Color(cS.getRed(), cS.getGreen(), cS.getBlue(), ap(mo - 20)),
                mx, vy, new Color(255, 255, 255, ap(mo))));
        g.fill(than);

        // ---- HẮC HOẢ Amaterasu ở đầu mũi tên ----
        //
        // Lửa đen trên nền tối là một bài toán ngược: tô đen lên nền đen thì mất hút. Cách duy
        // nhất đọc được là viền -- mỗi lưỡi lửa tô gần như đen tuyền rồi kẻ một sợi tím sáng bám
        // sát mép. Mắt đọc ra cái viền, và tự hiểu phần ruột tối bên trong là lửa.
        float goc = mx - 3.5f * z;
        g.setColor(new Color(10, 2, 14, ap((int) (150 + 60 * nhip))));
        g.fill(new Ellipse2D.Float(goc - 4.5f * z, vy - 4.5f * z, 9 * z, 9 * z));
        // Mười lưỡi trải trên 260 độ, dài ngắn so le và độ cong đảo chiều liên tục. Bản trước chỉ
        // 7 lưỡi cùng chiều cong, trải đều 120 độ -- đều tăm tắp nên xoè ra như cánh hoa. Lửa thật
        // thì không lưỡi nào giống lưỡi nào.
        for (int i = 0; i < 10; i++) {
            double a = Math.toRadians(105 + i * 26 + Math.sin(k * 0.8 + i * 1.7) * 16);
            float dai3 = (4.2f + ((i * 31 + k * 17) % 6) * 1.6f) * z;
            float ex = (float) (goc + Math.cos(a) * dai3);
            float ey = (float) (vy + Math.sin(a) * dai3);
            float cong3 = (2.0f + ((i * 13 + k) % 3) * 1.4f) * ((i + k) % 2 == 0 ? 1 : -1) * z;
            Path2D luoi3 = luoiLua(goc, vy, ex, ey, 1.9f * z, cong3);
            g.setColor(new Color(8, 1, 12, ap(215)));
            g.fill(luoi3);
            g.setStroke(new BasicStroke(0.7f * z, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(186, 120, 255, ap((int) (105 + 75 * nhip) - (i % 3) * 22)));
            g.draw(luoi3);
        }
        // Lõi trắng ngay chỗ tên tiếp giáp lửa -- điểm sáng nhất ảnh, để hắc hoả có chỗ mà tương phản
        g.setColor(new Color(255, 255, 255, ap(mo)));
        g.fill(new Ellipse2D.Float(goc + 2.2f * z, vy - 1.5f * z, 3 * z, 3 * z));
    }

    /**
     * Vẽ một đường thành ba lớp chồng lên nhau: quầng rộng mờ, thân, lõi sáng.
     *
     * Một nét đơn dù chọn màu khéo đến mấy vẫn ra đường kẻ kỹ thuật -- rìa cứng, bề dày đều. Ba
     * lớp cho rìa nhoè dần ra ngoài và sáng dồn vào giữa, đó là cách rẻ nhất để một đường trông
     * như đang phát sáng thay vì được tô màu.
     */
    private static void netMem(Graphics2D g, Shape hinh, Color cM, Color cS, int mo,
            float rongNgoai, float rongThan, float rongLoi) {
        g.setStroke(new BasicStroke(rongNgoai, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(cM.getRed(), cM.getGreen(), cM.getBlue(), ap(mo - 130)));
        g.draw(hinh);
        g.setStroke(new BasicStroke(rongThan, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(cM.getRed(), cM.getGreen(), cM.getBlue(), ap(mo - 25)));
        g.draw(hinh);
        g.setStroke(new BasicStroke(rongLoi, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(cS.getRed(), cS.getGreen(), cS.getBlue(), ap(mo)));
        g.draw(hinh);
    }

    /** Một lưỡi lửa: phình ở gốc, vuốt nhọn ở ngọn, cong theo tham số. */
    private static Path2D luoiLua(float bx, float by, float tx, float ty, float w, float cong) {
        float dx = tx - bx, dy = ty - by;
        float len = Math.max(0.001f, (float) Math.sqrt(dx * dx + dy * dy));
        float px = -dy / len * w, py = dx / len * w;
        float mx = (bx + tx) / 2 + (-dy / len) * cong, my = (by + ty) / 2 + (dx / len) * cong;
        Path2D f = new Path2D.Float();
        f.moveTo(bx + px, by + py);
        f.quadTo(mx + px * 0.6f, my + py * 0.6f, tx, ty);
        f.quadTo(mx - px * 0.6f, my - py * 0.6f, bx - px, by - py);
        f.closePath();
        return f;
    }

    private static void veDau(Graphics2D g, float gx, float dy, float z,
            Color cM, Color cS, int nen, int mo) {
        int k = KIEU_MUI;
        if (MADARA || k == 0) {
            veDauCoSung(g, gx, dy, z, cM, cS, nen, mo);
            return;
        }
        // Khối mặt dạt sang trái bao nhiêu so với tâm sọ -- đây chính là "độ quay đầu".
        float lech = k == 1 ? -3.6f * z : (k == 2 ? -5.4f * z : 0f);

        Path2D mu = new Path2D.Float();
        if (k == 2) {
            // Nhìn nghiêng: sọ phình hẳn về sau, mặt vát phía trước.
            mu.moveTo(gx + 1.5f * z, dy - 13 * z);
            mu.curveTo(gx + 10f * z, dy - 11.5f * z, gx + 12f * z, dy - 2f * z, gx + 9.5f * z, dy + 5.5f * z);
            mu.curveTo(gx + 7.5f * z, dy + 10.5f * z, gx + 2f * z, dy + 12f * z, gx - 3f * z, dy + 11f * z);
            mu.curveTo(gx - 7.5f * z, dy + 10f * z, gx - 9.5f * z, dy + 3.5f * z, gx - 9f * z, dy - 2.5f * z);
            mu.curveTo(gx - 8.5f * z, dy - 8.5f * z, gx - 3.5f * z, dy - 14f * z, gx + 1.5f * z, dy - 13 * z);
        } else {
            // Quay 3/4 (k=1): sọ dồn về bên PHẢI, má trái thu lại. Không làm bước này mà chỉ dạt
            // mấy chi tiết sang trái thì mũi trông như trượt khỏi mặt chứ đầu vẫn đứng chính diện.
            float p = k == 1 ? 1.15f : 1f, tr = k == 1 ? 0.82f : 1f;
            mu.moveTo(gx + (k == 1 ? 1.2f * z : 0), dy - 13 * z);
            mu.curveTo(gx + 8.5f * z * p, dy - 12 * z, gx + 11 * z * p, dy - 4 * z, gx + 10 * z * p, dy + 3 * z);
            mu.curveTo(gx + 9 * z * p, dy + 9 * z, gx + 5 * z * p, dy + 11 * z, gx, dy + 11 * z);
            mu.curveTo(gx - 5 * z * tr, dy + 11 * z, gx - 9 * z * tr, dy + 9 * z, gx - 10 * z * tr, dy + 3 * z);
            mu.curveTo(gx - 11 * z * tr, dy - 4 * z, gx - 8.5f * z * tr, dy - 12 * z,
                    gx + (k == 1 ? 1.2f * z : 0), dy - 13 * z);
        }
        mu.closePath();
        veKhoi(g, mu, cM, cS, nen + 6, mo);

        Path2D mao = new Path2D.Float();
        mao.moveTo(gx - 3.5f * z + lech * 0.3f, dy - 11 * z);
        mao.quadTo(gx + lech * 0.3f, dy - 20 * z, gx + 3.5f * z + lech * 0.3f, dy - 11 * z);
        mao.closePath();
        veKhoi(g, mao, cM, cS, nen + 10, mo);

        // Má loe hai bên -- nhìn nghiêng thì chỉ còn thấy một bên.
        for (int b = -1; b <= 1; b += 2) {
            if (k == 2 && b < 0) {
                continue;
            }
            Path2D goMa = new Path2D.Float();
            goMa.moveTo(gx + b * 9 * z, dy + 1 * z);
            goMa.quadTo(gx + b * 13.5f * z, dy + 7 * z, gx + b * 10 * z, dy + 13 * z);
            goMa.quadTo(gx + b * 6.5f * z, dy + 9 * z, gx + b * 9 * z, dy + 1 * z);
            goMa.closePath();
            veKhoi(g, goMa, cM, cS, nen + 4, mo - 20);
        }

        // Vành trán: góc 20..160 = NỬA TRÊN cung ellipse. Bản cũ để 200..140 tức nửa DƯỚI, nó
        // vắt ngang qua hai hốc mắt thành cái băng bịt mặt -- đầu nhỏ thì lẫn vào quai hàm, đầu
        // to lên là lộ ngay.
        g.setStroke(new BasicStroke(1.2f * z, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(cS.getRed(), cS.getGreen(), cS.getBlue(), ap(mo - 40)));
        g.draw(new Arc2D.Float(gx - 10 * z + lech * 0.5f, dy - 11 * z, 20 * z, 14 * z, 20, 140, Arc2D.OPEN));

        // Khe mắt. Quay đầu thì mắt phía xa hẹp lại, mắt phía gần giữ nguyên -- đó là toàn bộ
        // thủ thuật làm người xem tin là cái đầu đang quay.
        g.setColor(new Color(255, 246, 230, ap(mo + 30)));
        for (int b = -1; b <= 1; b += 2) {
            if (k == 2 && b > 0) {
                continue;
            }
            float hep = (k > 0 && k < 3 && b > 0) ? (k == 1 ? 0.55f : 0.3f) : 1f;
            float mx = gx + lech;
            Path2D mat = new Path2D.Float();
            mat.moveTo(mx + b * 3.4f * z * hep, dy + 0.4f * z);
            mat.quadTo(mx + b * 5.8f * z * hep, dy - 2.4f * z, mx + b * 7.6f * z * hep, dy - 1.8f * z);
            mat.quadTo(mx + b * 5.8f * z * hep, dy + 1.9f * z, mx + b * 3.4f * z * hep, dy + 0.4f * z);
            mat.closePath();
            g.fill(mat);
        }

        if (k == 4) {
            veMuiGoi(g, gx, dy, z, cS, mo);
        } else {
            veMuiKhoi(g, gx + lech, dy, z, cM, cS, nen, mo, k);
        }
    }

    /**
     * Đầu bản gốc: mũ có mào, hai sừng cong, má loe, tấm che cằm, khe mắt xếch. Không mũi dài.
     *
     * Giữ nguyên kích thước nhỏ của bản gốc (mũ cao 20z chứ không phải 24z như bản thiên cẩu):
     * đầu to lên là để lấy chỗ cho cái mũi, mà kiểu này không có mũi thì phóng to chỉ làm nhân
     * vật thành đầu to thân bé.
     */
    private static void veDauCoSung(Graphics2D g, float gx, float dy, float z,
            Color cM, Color cS, int nen, int mo) {
        Color net = new Color(cS.getRed(), cS.getGreen(), cS.getBlue(), mo);
        Path2D mu = new Path2D.Float();
        mu.moveTo(gx, dy - 11 * z);
        mu.curveTo(gx + 7 * z, dy - 10 * z, gx + 9 * z, dy - 3 * z, gx + 8 * z, dy + 3 * z);
        mu.curveTo(gx + 7 * z, dy + 8 * z, gx + 4 * z, dy + 9 * z, gx, dy + 9 * z);
        mu.curveTo(gx - 4 * z, dy + 9 * z, gx - 7 * z, dy + 8 * z, gx - 8 * z, dy + 3 * z);
        mu.curveTo(gx - 9 * z, dy - 3 * z, gx - 7 * z, dy - 10 * z, gx, dy - 11 * z);
        mu.closePath();
        veKhoi(g, mu, cM, cS, nen + 6, mo);

        Path2D mao = new Path2D.Float();
        mao.moveTo(gx - 3 * z, dy - 9 * z);
        mao.quadTo(gx, dy - (SASUKE ? 21 : 17) * z, gx + 3 * z, dy - 9 * z);
        mao.closePath();
        veKhoi(g, mao, cM, cS, nen + 10, mo);

        // Hai sừng cong -- lý do giữ kiểu này: chúng nằm NGOÀI đường bao của đầu nên thu nhỏ
        // xuống cỡ thật vẫn còn bóng, khác hẳn cái mũi nằm gọn trong mặt.
        g.setStroke(new BasicStroke(1.7f * z, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(net);
        for (int b = -1; b <= 1; b += 2) {
            Path2D sung = new Path2D.Float();
            sung.moveTo(gx + b * 6 * z, dy - 6 * z);
            if (MADARA) {
                // Madara: GAI THẲNG vút ra ngoài, không cong mềm. Vẽ bằng nét dày như sừng của
                // Itachi chứ không đắp khối viền mỏng -- bản trước tôi vẽ tam giác kẻ viền, thu
                // nhỏ lại chỉ còn mấy sợi chỉ, nhìn như cái vương miện giấy.
                sung.curveTo(gx + b * 12 * z, dy - 10 * z, gx + b * 15 * z, dy - 14 * z,
                        gx + b * 17 * z, dy - 20 * z);
            } else if (SASUKE) {
                // Sasuke: sừng vút thẳng và nhọn, không cong mềm như Itachi.
                sung.curveTo(gx + b * 11 * z, dy - 12 * z, gx + b * 13 * z, dy - 18 * z,
                        gx + b * 12 * z, dy - 24 * z);
            } else {
                sung.curveTo(gx + b * 12 * z, dy - 11 * z, gx + b * 13 * z, dy - 16 * z,
                        gx + b * 8 * z, dy - 19 * z);
            }
            g.draw(sung);
        }

        for (int b = -1; b <= 1; b += 2) {
            Path2D goMa = new Path2D.Float();
            goMa.moveTo(gx + b * 7 * z, dy + 1 * z);
            goMa.quadTo(gx + b * 11 * z, dy + 6 * z, gx + b * 8 * z, dy + 11 * z);
            goMa.quadTo(gx + b * 5 * z, dy + 8 * z, gx + b * 7 * z, dy + 1 * z);
            goMa.closePath();
            veKhoi(g, goMa, cM, cS, nen + 4, mo - 20);
        }

        g.setStroke(new BasicStroke(1.2f * z, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(cS.getRed(), cS.getGreen(), cS.getBlue(), ap(mo - 40)));
        g.draw(new Arc2D.Float(gx - 8 * z, dy - 8 * z, 16 * z, 12 * z, 200, 140, Arc2D.OPEN));
        for (int b = -1; b <= 1; b += 2) {
            g.fill(new Ellipse2D.Float(gx + b * 6.4f * z - 0.9f * z, dy - 2 * z, 1.8f * z, 1.8f * z));
        }
        Path2D cam = new Path2D.Float();
        cam.moveTo(gx - 4.5f * z, dy + 6 * z);
        cam.quadTo(gx, dy + 11 * z, gx + 4.5f * z, dy + 6 * z);
        cam.quadTo(gx, dy + 8 * z, gx - 4.5f * z, dy + 6 * z);
        cam.closePath();
        veKhoi(g, cam, cM, cS, nen + 12, mo - 20);
        g.setColor(new Color(cS.getRed(), cS.getGreen(), cS.getBlue(), ap(mo - 55)));
        g.draw(new Line2D.Float(gx, dy - 9 * z, gx, dy - 3 * z));

        if (MADARA) {
            // Viên ngọc trên trán -- NHỎ thôi. Bản trước tôi vẽ ngũ giác to trắng loá, nó nuốt
            // sạch cả cái đầu; ở cỡ thật chỉ còn thấy một đốm trắng chứ không thấy mặt.
            Path2D lt = new Path2D.Float();
            float r2 = 2.2f * z, cy2 = dy - 6f * z;
            for (int i = 0; i < 5; i++) {
                double a = Math.toRadians(-90 + i * 72);
                float px = (float) (gx + Math.cos(a) * r2), py = (float) (cy2 + Math.sin(a) * r2);
                if (i == 0) {
                    lt.moveTo(px, py);
                } else {
                    lt.lineTo(px, py);
                }
            }
            lt.closePath();
            g.setColor(new Color(235, 245, 255, ap(mo)));
            g.fill(lt);
        }
        g.setColor(new Color(255, 246, 230, ap(mo + 30)));
        for (int b = -1; b <= 1; b += 2) {
            Path2D mat = new Path2D.Float();
            mat.moveTo(gx + b * 1.8f * z, dy + 1.5f * z);
            mat.quadTo(gx + b * 4 * z, dy - 1.2f * z, gx + b * 5.6f * z, dy - 0.6f * z);
            mat.quadTo(gx + b * 4 * z, dy + 3 * z, gx + b * 1.8f * z, dy + 1.5f * z);
            mat.closePath();
            g.fill(mat);
        }
    }

    /**
     * Mũi đắp khối. Chính diện (k=3) thì ngắn và dựng đứng; quay đầu (k=1,2) thì mũi ngả sang
     * trái và dài hẳn ra -- vì lúc đó chiều dài mới nhìn thấy được, không còn bị nén.
     */
    private static void veMuiKhoi(Graphics2D g, float mx, float dy, float z,
            Color cM, Color cS, int nen, int mo, int k) {
        float dai = k == 3 ? 8.5f : (k == 1 ? 10.5f : 11.5f);   // chiều dài mũi
        float nga = k == 3 ? 0f : (k == 1 ? -6.2f * z : -9.4f * z);  // chóp ngả sang trái
        float be = k == 3 ? 2.1f : 2.3f;                    // nửa bề ngang ở gốc

        // Bóng hắt sang má bên kia -- thiếu cái này thì mũi bẹt dí thành một vệt kẻ.
        Path2D bong = new Path2D.Float();
        bong.moveTo(mx + 0.5f * z, dy - 2.2f * z);
        bong.curveTo(mx + (be + 1.4f) * z, dy + dai * 0.3f * z,
                mx + (be + 1.1f) * z, dy + dai * 0.7f * z, mx + nga * 0.35f + 1.6f * z, dy + dai * z);
        bong.curveTo(mx + nga * 0.3f + 1.3f * z, dy + dai * 0.62f * z,
                mx + (be - 0.6f) * z, dy + dai * 0.22f * z, mx + 0.3f * z, dy - 1 * z);
        bong.closePath();
        g.setColor(new Color(0, 0, 0, ap(mo / 4)));
        g.fill(bong);

        // Bề ngang giữ HẸP: mặt chỉ rộng 20z, mũi bè quá 5z là nuốt sạch hai hốc mắt thành cái
        // mõm. Chóp bo tròn chứ không tụ về một điểm -- nhọn hoắt thì ra củ cà rốt.
        Path2D mui = new Path2D.Float();
        if (k == 1 || k == 2) {
            // NÊM THẲNG. Lối dựng đối xứng ở dưới kéo chóp đi mà giữ nguyên hai sườn, nên chóp
            // càng ngả thì thân càng cong -- ra cái vòi voi. Ở đây gốc bám vào trán, hai sườn
            // chạy THẲNG ra chóp, và chóp cố tình vượt qua mép má để cắt được đường bao: đó mới
            // là thứ khiến mắt tin cái mũi chìa ra trước chứ không phải thõng xuống.
            mui.moveTo(mx + be * z, dy - 1.0f * z);
            mui.curveTo(mx + be * z, dy - 3.0f * z, mx + 1.4f * z, dy - 4.0f * z, mx - 0.2f * z, dy - 3.8f * z);
            mui.curveTo(mx - 2.0f * z, dy - 3.4f * z, mx - be * z, dy - 2.0f * z, mx - be * z, dy + 0.4f * z);
            mui.curveTo(mx + nga * 0.42f, dy + dai * 0.44f * z, mx + nga * 0.82f, dy + dai * 0.79f * z,
                    mx + nga - 0.5f * z, dy + dai * z);
            mui.quadTo(mx + nga + 0.4f * z, dy + (dai + 0.75f) * z, mx + nga + 1.3f * z, dy + dai * 0.93f * z);
            mui.curveTo(mx + nga * 0.66f + 2.3f * z, dy + dai * 0.70f * z, mx + 2.5f * z,
                    dy + dai * 0.34f * z, mx + be * z, dy - 1.0f * z);
            mui.closePath();
        } else {
        mui.moveTo(mx + nga - 0.55f * z, dy + dai * z);
        mui.curveTo(mx + nga * 0.62f - 1.55f * z, dy + dai * 0.78f * z,
                mx - 2.05f * z, dy + dai * 0.42f * z, mx - (be - 0.05f) * z, dy + 1.2f * z);
        mui.curveTo(mx - (be + 0.2f) * z, dy - 1.7f * z, mx - 1.65f * z, dy - 3.6f * z, mx, dy - 3.6f * z);
        mui.curveTo(mx + 1.65f * z, dy - 3.6f * z, mx + (be + 0.2f) * z, dy - 1.7f * z,
                mx + (be - 0.05f) * z, dy + 1.2f * z);
        mui.curveTo(mx + 2.05f * z, dy + dai * 0.42f * z,
                mx + nga * 0.62f + 1.55f * z, dy + dai * 0.78f * z, mx + nga + 0.55f * z, dy + dai * z);
        mui.curveTo(mx + nga + 0.3f * z, dy + (dai + 0.9f) * z, mx + nga - 0.3f * z, dy + (dai + 0.9f) * z,
                mx + nga - 0.55f * z, dy + dai * z);
        mui.closePath();
        }

        // Tô dốc NGANG (trái sáng, phải tối) cho ra khối trụ, không dốc dọc như veKhoi.
        java.awt.Rectangle b = mui.getBounds();
        g.setPaint(new GradientPaint(b.x, 0,
                new Color(cS.getRed(), cS.getGreen(), cS.getBlue(), ap(nen + 78)),
                b.x + b.width, 0, new Color(0, 0, 0, ap(nen + 26))));
        g.fill(mui);
        g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(cM.getRed(), cM.getGreen(), cM.getBlue(), mo));
        g.draw(mui);
        g.setStroke(new BasicStroke(0.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(cS.getRed(), cS.getGreen(), cS.getBlue(), ap(mo - 30)));
        g.draw(mui);

        g.setStroke(new BasicStroke(1.1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(cS.getRed(), cS.getGreen(), cS.getBlue(), ap(mo - 25)));
        Path2D song = new Path2D.Float();
        song.moveTo(mx - 0.75f * z, dy - 2.0f * z);
        song.quadTo(mx - 1.05f * z, dy + dai * 0.45f * z, mx + nga * 0.55f, dy + dai * 0.85f * z);
        g.draw(song);
    }

    /**
     * Mũi chỉ GỢI, không đắp khối: hai nét sáng dọc làm sống mũi, một vệt tối bên phải làm bóng.
     *
     * Đây là lối thoát cho góc chính diện -- không có khối nào chìa ra thì không có gì để nhìn
     * sai, mà mắt vẫn đọc ra chỗ gồ lên giữa mặt. Đổi lại: ở cỡ thật gần như không thấy gì.
     */
    private static void veMuiGoi(Graphics2D g, float gx, float dy, float z, Color cS, int mo) {
        g.setColor(new Color(0, 0, 0, ap(mo / 5)));
        Path2D bong = new Path2D.Float();
        bong.moveTo(gx + 0.4f * z, dy - 2f * z);
        bong.quadTo(gx + 2.6f * z, dy + 3f * z, gx + 1.6f * z, dy + 8.5f * z);
        bong.quadTo(gx + 0.9f * z, dy + 4f * z, gx + 0.2f * z, dy - 1f * z);
        bong.closePath();
        g.fill(bong);
        g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(cS.getRed(), cS.getGreen(), cS.getBlue(), ap(mo - 35)));
        g.draw(new Line2D.Float(gx - 0.9f * z, dy - 2.2f * z, gx - 0.5f * z, dy + 8f * z));
        g.setColor(new Color(cS.getRed(), cS.getGreen(), cS.getBlue(), ap(mo - 90)));
        g.draw(new Line2D.Float(gx + 0.9f * z, dy - 1.8f * z, gx + 0.6f * z, dy + 7f * z));
    }

    private static void veKhoi(Graphics2D g, Shape hinh, Color cM, Color cS, int nen, int mo) {
        java.awt.Rectangle b = hinh.getBounds();
        g.setPaint(new GradientPaint(b.x, b.y,
                new Color(cM.getRed(), cM.getGreen(), cM.getBlue(), ap(nen + 42)),
                b.x, b.y + b.height, new Color(0, 0, 0, ap(nen - 4))));
        g.fill(hinh);

        java.awt.Shape cu = g.getClip();
        g.clip(hinh);

        // Bóng đổ ở đáy: dải tối áp sát mép dưới, cho cảm giác khối này đứng trước khối khác.
        g.setColor(new Color(0, 0, 0, ap(mo / 3)));
        g.setStroke(new BasicStroke(b.height * 0.22f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Float(b.x, b.y + b.height, b.x + b.width, b.y + b.height));

        // Highlight mép trên, như bản cũ.
        g.setColor(new Color(cS.getRed(), cS.getGreen(), cS.getBlue(), ap(mo - 70)));
        g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Arc2D.Float(b.x + b.width * 0.12f, b.y + b.height * 0.06f,
                b.width * 0.76f, b.height * 0.5f, 30, 120, Arc2D.OPEN));

        // Vân texture: vài mảng sáng/tối nhỏ rải trong khối, giả-ngẫu-nhiên theo toạ độ chứ
        // không dùng Math.random() (sẽ phá tính lặp lại giữa các lần chạy công cụ). Đây là cách
        // rẻ nhất để bề mặt bớt phẳng như nhựa đúc -- một khối tô gradient đơn thuần luôn quá
        // mượt, mắt người quen nhìn bề mặt có chút bất toàn.
        int soVet = Math.max(3, b.width * b.height / 900);
        for (int i = 0; i < soVet; i++) {
            int hx = (i * 977 + b.x * 131) % Math.max(1, b.width);
            int hy = (i * 613 + b.y * 271) % Math.max(1, b.height);
            int hr = 2 + (i * 37) % 4;
            boolean sang = (i % 3) != 0;
            Color c = sang
                    ? new Color(cS.getRed(), cS.getGreen(), cS.getBlue(), ap((mo - 130) / 2))
                    : new Color(0, 0, 0, ap((mo - 100) / 3));
            g.setColor(c);
            g.fillOval(b.x + hx - hr / 2, b.y + hy - hr / 2, hr, hr);
        }
        g.setClip(cu);

        // Viền chính.
        g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(cM.getRed(), cM.getGreen(), cM.getBlue(), mo));
        g.draw(hinh);

        // Rim light: viền mảnh, sáng hơn hẳn, bám sát đường bao -- vẽ SAU viền chính, mảnh hơn,
        // để nó nằm như một sợi chỉ sáng ngay bên trong cạnh chứ không lấn ra ngoài.
        g.setStroke(new BasicStroke(0.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(cS.getRed(), cS.getGreen(), cS.getBlue(), ap(mo - 30)));
        g.draw(hinh);
    }

    /** Vẽ một cánh tay thon dần, trả về toạ độ bàn tay để đặt khiên hoặc kiếm vào. */
    /**
     * Tấm ngực giáp của Sasuke: khối kín, cạnh thẳng, gãy góc.
     *
     * Cố tình dùng lineTo chứ không curveTo. Đường cong đọc ra "xương, sinh vật"; cạnh thẳng gãy
     * góc đọc ra "giáp, kim loại". Đó là toàn bộ khác biệt giữa hai Susanoo, và ở cỡ thật thì
     * dáng đường bao là thứ duy nhất còn phân biệt được -- chi tiết bên trong nhoè hết.
     */
    private static void veNgucGiap(Graphics2D g, float gx, float ny, float z,
            Color cM, Color cS, int nen, int mo, float nhip) {
        Path2D nguc = new Path2D.Float();
        nguc.moveTo(gx - 9 * z, ny - 22 * z);
        nguc.lineTo(gx - 18 * z, ny - 17 * z);
        nguc.lineTo(gx - 20 * z, ny - 4 * z);
        nguc.lineTo(gx - 13 * z, ny + 9 * z);
        nguc.lineTo(gx, ny + 20 * z);
        nguc.lineTo(gx + 13 * z, ny + 9 * z);
        nguc.lineTo(gx + 20 * z, ny - 4 * z);
        nguc.lineTo(gx + 18 * z, ny - 17 * z);
        nguc.lineTo(gx + 9 * z, ny - 22 * z);
        nguc.closePath();
        veKhoi(g, nguc, cM, cS, nen + 6, mo);

        // Hai chevron chìm trên ngực -- thay cho mấy cung xương sườn
        g.setStroke(new BasicStroke(1.5f * z, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER));
        for (int i = 0; i < 2; i++) {
            float yy = ny - 9 * z + i * 8 * z;
            float w = (15 - i * 3) * z;
            g.setColor(new Color(cS.getRed(), cS.getGreen(), cS.getBlue(), ap(mo - 40 - i * 25)));
            Path2D ch = new Path2D.Float();
            ch.moveTo(gx - w, yy);
            ch.lineTo(gx, yy + 5 * z);
            ch.lineTo(gx + w, yy);
            g.draw(ch);
        }
        // Sống giữa và viên lõi
        g.setColor(new Color(cS.getRed(), cS.getGreen(), cS.getBlue(), ap(mo - 60)));
        g.setStroke(new BasicStroke(1.4f * z, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Float(gx, ny - 20 * z, gx, ny + 16 * z));
        float q = (2.6f + 1.2f * nhip) * z;
        g.setColor(new Color(255, 245, 255, ap(mo)));
        g.fill(new Ellipse2D.Float(gx - q, ny - 3 * z - q, q * 2, q * 2));
    }

    /** Vai giáp: ba tấm xếp lớp, hất chéo ra -- khối lớn hơn hẳn vai bo tròn của Itachi. */
    private static void veVaiGiap(Graphics2D g, float gx, float ny, float z,
            Color cM, Color cS, int nen, int mo) {
        for (int b = -1; b <= 1; b += 2) {
            for (int i = 0; i < 3; i++) {
                float vx = gx + b * (18 + i * 3.8f) * z, vy = ny - 16 * z + i * 5.5f * z;
                float w = (9 - i * 1.6f) * z, h = (5.5f - i * 0.6f) * z;
                Path2D t = new Path2D.Float();
                t.moveTo(vx - b * w * 0.75f, vy - h);
                t.lineTo(vx + b * w, vy - h * 0.35f);
                t.lineTo(vx + b * w * 0.82f, vy + h);
                t.lineTo(vx - b * w * 0.75f, vy + h * 0.5f);
                t.closePath();
                veKhoi(g, t, cM, cS, nen + 10 - i * 3, mo - i * 18);
            }
        }
    }

    /** Tay giáp: hai đốt gãy khuỷu, buông xuống -- khác hẳn tay cong ôm ra trước của Itachi. */
    private static float[] tayGiap(Graphics2D g, float gx, float ny, int b, float z,
            Color cM, Color cS, int nen, int mo) {
        // Cả ba mốc đẩy ra NGOÀI mép tấm ngực (mép ở 20z). Bản trước để vai ở 22z mà bản tay
        // thụt về 23z với bề ngang 3z, nên cánh tay cắt chéo qua mép ngực -- nhìn như que gãy
        // cắm vào thân. Và phải dày hẳn: 3z thu về cỡ thật còn chưa tới một điểm ảnh, mất tiêu.
        float vx = gx + b * 23 * z, vy = ny - 9 * z;      // vai
        float kx = gx + b * 27 * z, ky = ny + 4 * z;      // khuỷu
        float bx = gx + b * 25 * z, by = ny + 15 * z;     // bàn tay
        Path2D tren = new Path2D.Float();
        tren.moveTo(vx - b * 5.5f * z, vy - 5 * z);
        tren.lineTo(vx + b * 5.5f * z, vy - 2 * z);
        tren.lineTo(kx + b * 4.6f * z, ky + 1 * z);
        tren.lineTo(kx - b * 4.6f * z, ky - 2 * z);
        tren.closePath();
        veKhoi(g, tren, cM, cS, nen + 8, mo - 6);
        Path2D duoi = new Path2D.Float();
        duoi.moveTo(kx - b * 4.8f * z, ky - 2 * z);
        duoi.lineTo(kx + b * 4.8f * z, ky + 1 * z);
        duoi.lineTo(bx + b * 3.6f * z, by);
        duoi.lineTo(bx - b * 3.6f * z, by - 2 * z);
        duoi.closePath();
        veKhoi(g, duoi, cM, cS, nen + 5, mo - 14);
        // Khớp khuỷu che chỗ nối hai đốt. Vẽ NHỎ và KHÔNG viền sáng: bản trước để đường tròn
        // 8.8z có viền, thu về cỡ thật thì hai cái khớp nổi hơn cả cánh tay, đọc ra hai hòn bi
        // lơ lửng hai bên. Ở cỡ này thứ gì tròn và sáng đều giành mất sự chú ý.
        g.setColor(new Color(cM.getRed(), cM.getGreen(), cM.getBlue(), ap(nen + 40)));
        g.fill(new Ellipse2D.Float(kx - 3.4f * z, ky - 3.4f * z, 6.8f * z, 6.8f * z));
        return new float[] {bx, by};
    }

    private static float[] tay(Graphics2D g, float gx, float ny, int b, int z,
            Color cM, Color cS, int nen, int mo) {
        float vx = gx + b * 20 * z, vy = ny - 8 * z;
        // Bàn tay kéo vào sát thân: bản trước để 30 nên khiên và kiếm rời hẳn ra.
        float bx = gx + b * 24 * z, by = ny + 4 * z;
        Path2D t = new Path2D.Float();
        t.moveTo(vx - b * 2 * z, vy - 3 * z);
        t.curveTo(vx + b * 6 * z, vy + 1 * z, bx - b * 1 * z, by - 5 * z, bx + b * 2 * z, by);
        t.curveTo(bx - b * 2 * z, by + 4 * z, vx + b * 4 * z, vy + 7 * z, vx - b * 2 * z, vy + 4 * z);
        t.closePath();
        veKhoi(g, t, cM, cS, nen + 4, mo - 15);
        return new float[] {bx, by};
    }

    private static String cauSql(int ma) {
        StringBuilder sp = new StringBuilder("["), fr = new StringBuilder("["), rn = new StringBuilder("[");
        for (int i = 0; i < SO_KHUNG; i++) {
            if (i > 0) {
                sp.append(',');
                fr.append(',');
            }
            sp.append("{\"id\":").append(i)
              .append(",\"x\":").append((i % COT) * RONG)
              .append(",\"y\":").append((i / COT) * CAO)
              .append(",\"w\":").append(RONG).append(",\"h\":").append(CAO).append('}');
            // onTop = 0: vẽ SAU lưng nhân vật, để người chơi vẫn thấy mình bên trong bộ giáp.
            fr.append("[{\"id\":").append(i).append(",\"dx\":").append(-RONG / 2)
              .append(",\"dy\":").append(DY).append(",\"onTop\":0,\"flip\":0}]");
            for (int k = 0; k < LAP_KHUNG; k++) {
                rn.append(i == 0 && k == 0 ? "" : ",").append(i);
            }
        }
        return "REPLACE INTO `effect_data` (`id`,`sprites`,`frames`,`running`,`frame_char`) VALUES ("
                + ma + ",'" + sp + "]','" + fr + "]','" + rn + "]','[[],[],[],[]]');";
    }
}
