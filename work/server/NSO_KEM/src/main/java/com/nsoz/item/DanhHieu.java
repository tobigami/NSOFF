package com.nsoz.item;

import com.nsoz.option.ItemOption;
import com.nsoz.util.NinjaUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Chỉ số cố định của các bí kíp danh hiệu -- một bảng duy nhất thay cho chuỗi if-else trong
 * {@code Item.initOption}.
 *
 * Vì sao gom lại: mỗi danh hiệu là một khối 5-7 dòng giống hệt nhau về hình dạng, chỉ khác con số.
 * Để rải trong initOption thì thêm một danh hiệu là thêm một nhánh if, và không ai nhìn ra được
 * hai danh hiệu có bị trùng vai không. Ở đây xếp cạnh nhau, đọc dọc xuống là thấy ngay cái nào ăn
 * vào ô nào.
 *
 * NGUYÊN TẮC ĐẶT CHỈ SỐ, giữ đúng như bộ Đệ Nhất - Đệ Tứ đã làm:
 * <ul>
 *   <li>Mỗi danh hiệu có MỘT vai rõ rệt, không xếp bậc cao thấp. Người chơi chọn theo lối đánh
 *       chứ không phải cứ cái sau mạnh hơn cái trước.</li>
 *   <li>Nhiều nhất 7 dòng: khung thông tin trong game vừa 9 dòng, tính cả tên và mô tả.</li>
 *   <li>Mỗi giá trị phải lọt short (&le; 32767) -- giao thức gửi bằng {@code writeShort}.</li>
 *   <li>Chí mạng và né đòn tính theo phần nghìn: 200 là 20%.</li>
 * </ul>
 *
 * Bảng này chỉ chạy lúc món RA ĐỜI. Món đã nằm trong hành trang người chơi giữ chỉ số cũ trong
 * cột dữ liệu của họ, sửa ở đây không đụng tới -- muốn nâng cho món cũ thì phải phát lại món mới.
 */
public final class DanhHieu {

    /** id danh hiệu -> danh sách {mã chỉ số, giá trị}. Giá trị CỐ ĐỊNH, món nào cũng như nhau. */
    private static final Map<Integer, int[][]> BANG = new HashMap<Integer, int[][]>();

    /**
     * id -> danh sách {mã chỉ số, cận dưới, cận trên}. Mỗi món RA LÒ một bộ số riêng.
     *
     * Vì sao tách hẳn bảng thứ hai thay vì nhét cờ vào bảng cũ: hai loại có vòng đời khác nhau.
     * Món chỉ số cố định thì nhìn bảng là biết món đó mạnh cỡ nào; món ngẫu nhiên thì phải mở
     * từng cái ra xem. Trộn chung một bảng là sau này không ai đọc ra được món nào thuộc loại nào.
     *
     * LUẬT BẤT BIẾN: cận TRÊN của mọi dòng phải thấp hơn con số tương ứng của Susano Itachi.
     * Itachi là món duy nhất bị khoá gia tộc -- phải trả giá mới dùng được -- nên nó giữ ngôi
     * mạnh nhất. Nếu để cận trên chạm hoặc vượt, sẽ có người quay được món tự do mạnh hơn món
     * khoá tộc, và cả cái lý do tồn tại của việc khoá tộc sụp theo.
     */
    private static final Map<Integer, int[][]> BANG_NGAU = new HashMap<Integer, int[][]>();

    /**
     * id danh hiệu -> mã hiệu ứng biểu ngữ bay trên đầu.
     *
     * Mã hiệu ứng phải NHỎ HƠN 256 và lấp vào chỗ trống của dải cũ. Máy khách giữ mã hiệu ứng
     * trong một byte; đặt 280 thì bên kia đọc ra 24 và đi tìm nhầm hiệu ứng khác. Chỗ trống dưới
     * 256 chỉ còn đúng 22 mã, nên đây là tài nguyên hiếm.
     *
     * Biểu ngữ là ảnh trong bảng {@code effect_data}, mã do tools/VeBangDanhHieu.java sinh, LẤP VÀO CHỖ TRỐNG DƯỚI 256
     * ra; bốn danh hiệu Hokage dùng lại biểu ngữ vẽ tay sẵn có (201-204). Danh hiệu nào không có
     * trong bảng này thì đeo vào không hiện chữ gì -- đó là cách tắt biểu ngữ.
     */
    private static final Map<Integer, Integer> BIEU_NGU = new HashMap<Integer, Integer>();

    static {
        // ---- Bộ Hokage: chuyển nguyên từ Item.initOption sang, không đổi một con số nào ----
        dat(1150, new int[][] {{82, 5000}, {128, 12}, {99, 200}, {124, 800}, {118, 100}, {87, 1000}});
        dat(1151, new int[][] {{87, 2000}, {116, 600}, {126, 500}, {113, 300}, {117, 5000}, {119, 400}});
        dat(1152, new int[][] {{82, 2500}, {117, 2500}, {87, 2500}, {92, 50}, {115, 250}, {116, 250},
            {118, 150}, {124, 400}});
        dat(1153, new int[][] {{82, 1000}, {87, 1000}, {93, 2}, {115, 600}, {92, 200}, {67, 30},
            {105, 800}});

        // ---- Thiên bảng: thưởng theo hạng, giữ nguyên số cũ của TOP1..TOP5 ----
        dat(1224, new int[][] {{82, 5000}, {87, 5000}, {92, 100}, {94, 10}});
        dat(1225, new int[][] {{82, 4000}, {87, 4000}, {92, 80}, {94, 10}});
        dat(1226, new int[][] {{82, 3000}, {87, 3000}, {92, 70}, {94, 10}});
        dat(1227, new int[][] {{82, 2000}, {87, 2000}, {92, 60}, {94, 10}});
        dat(1228, new int[][] {{82, 1000}, {87, 1000}, {92, 50}, {94, 10}});

        // ---- Bốn danh hiệu vốn có tên mà rỗng chỉ số ----
        // Trùm Phái: kẻ đứng đầu một môn phái -- công thủ đều, không có lỗ hổng.
        dat(1118, new int[][] {{82, 2000}, {87, 1800}, {118, 80}, {124, 300}, {92, 40}});
        // Trùm Trường: danh hiệu của trường -- thiên kinh nghiệm và sức bền để cày.
        dat(1121, new int[][] {{82, 2500}, {100, 10}, {120, 300}, {124, 250}, {87, 1200}});
        // Mangekyo Sharingan: con mắt nhìn thấu -- chính xác và sát thương chuẩn.
        dat(1131, new int[][] {{87, 1500}, {116, 500}, {113, 250}, {101, 15}, {92, 60}});
        // Akatsuki: tổ chức săn vĩ thú -- sát thủ, đánh người là chính.
        dat(1133, new int[][] {{87, 2200}, {103, 250}, {92, 120}, {105, 500}, {63, 8}});

        // ---- Mười bốn danh hiệu mới, mỗi cái một lối đánh ----
        // Tam Nhẫn Truyền Kỳ: ba học trò của Tam Đại -- dày dặn, thiên chakra.
        dat(1119, new int[][] {{82, 2200}, {117, 6000}, {87, 1600}, {119, 500}, {118, 90}});
        // Tiên Nhân Diệu Mộc: thu tự nhiên năng lượng -- hồi phục không dứt.
        dat(1120, new int[][] {{82, 3000}, {99, 250}, {120, 400}, {124, 350}, {87, 900}});
        // Nhân Trụ Lực: mang vĩ thú trong người -- máu dày khủng khiếp.
        dat(1122, new int[][] {{82, 6000}, {128, 10}, {124, 500}, {87, 800}, {126, 300}});
        // Băng Độn Huyết Kế: máu băng của Haku -- băng công và kháng băng.
        dat(1132, new int[][] {{89, 1200}, {95, 200}, {84, 350}, {87, 1300}, {130, 12}});
        // Kiếm Hào Thất Nhẫn: bảy kiếm sĩ Sương Ẩn -- một nhát chí mạng.
        dat(1145, new int[][] {{87, 2400}, {105, 900}, {92, 90}, {67, 25}, {113, 200}});
        // Ám Bộ Anbu: đội ám sát -- nhanh, né, đánh trúng.
        dat(1149, new int[][] {{84, 500}, {86, 450}, {93, 2}, {87, 1400}, {92, 70}});
        // Uchiha Nhất Tộc: dòng máu chí mạng.
        dat(1154, new int[][] {{92, 180}, {105, 700}, {87, 1500}, {67, 20}, {79, 10}});
        // Senju Nhất Tộc: dòng máu sinh mệnh.
        dat(1155, new int[][] {{82, 4000}, {128, 8}, {124, 450}, {118, 120}, {87, 800}});
        // Cửu Vĩ Yêu Hồ: sức mạnh mượn từ vĩ thú -- đánh mạnh, đổi lại mỏng manh.
        dat(1162, new int[][] {{94, 12}, {87, 2600}, {105, 800}, {93, 2}, {92, 100}});
        // Lục Đạo Tiên Nhân: đỉnh cao toàn diện, nhưng không dẫn đầu ô nào.
        dat(1206, new int[][] {{82, 3000}, {117, 3000}, {87, 2000}, {92, 80}, {84, 300}, {86, 300},
            {118, 130}});
        // Thần Uy Susanoo: bộ giáp chakra -- thủ tuyệt đối.
        dat(1207, new int[][] {{124, 900}, {126, 600}, {118, 160}, {82, 2500}, {79, 15}});
        // Luân Hồi Nhãn: xuyên qua mọi kháng cự.
        dat(1209, new int[][] {{101, 25}, {113, 400}, {87, 1800}, {116, 400}, {92, 60}});
        // Bát Môn Độn Giáp: mở cổng, đốt sinh mệnh lấy sức mạnh -- công cực cao, không một dòng máu.
        dat(1210, new int[][] {{94, 18}, {87, 3000}, {93, 3}, {92, 110}, {98, 5}});
        // Nhẫn Giả Đào Vong: kẻ phản bội làng -- sinh ra để đánh người.
        dat(1235, new int[][] {{103, 350}, {87, 1700}, {63, 12}, {92, 80}, {84, 250}});
    }

    private DanhHieu() {
    }

    static {
        // Bốn Hokage giữ biểu ngữ vẽ tay sẵn có.
        bn(1150, 201); bn(1151, 202); bn(1152, 203); bn(1153, 204);
        // Mười chín biểu ngữ mới.
        bn(1119, 180); bn(1120, 181); bn(1122, 188); bn(1132, 189); bn(1145, 190);
        bn(1149, 191); bn(1154, 214); bn(1155, 215); bn(1162, 216); bn(1206, 217);
        bn(1207, 218); bn(1209, 232); bn(1210, 233); bn(1235, 237);
        bn(1224, 238); bn(1225, 239); bn(1226, 240); bn(1227, 242); bn(1228, 243);
        bn(1131, 244); bn(1133, 254);
        // Tà thuật Uchi: món duy nhất trong game nói tới Susano, mà suốt từ trước tới giờ mô tả
        // "tăng cường sức mạnh triệu hồi Susano" không ứng với một dòng mã nào. Nay nó gọi ra bộ
        // giáp chakra bao quanh người (hiệu ứng 255, vẽ bằng tools/VeSusano.java).
        bn(1096, 255);
        // Susano Sasuke -- KHÔNG có mặt trong RIENG_TOC, tức ai cũng dùng được. Cố ý khác Susano
        // Itachi: món kia là nhẫn thuật riêng của nhà Uchiha nên bị chặn ở cửa mặc đồ, món này thì
        // không. Muốn chặn về sau thì thêm một dòng vào RIENG_TOC, không phải sửa chỗ nào khác.
        bn(1266, 32);
        bn(1267, 237);
        // Trùm Phái và Trùm Trường KHÔNG có ở đây: hai cái đó đã có biểu ngữ vẽ tay riêng theo
        // phái (12-17) và theo hệ (18-20), đẹp hơn hẳn đồ máy sinh -- xem khối trong Char.java.
    }

    private static void bn(int id, int hieuUng) {
        BIEU_NGU.put(Integer.valueOf(id), Integer.valueOf(hieuUng));
    }

    /**
     * Món nào chỉ riêng một gia tộc dùng được. Khoá là mã món, giá trị là tên gia tộc (chữ thường).
     *
     * Để ở đây cùng chỗ với biểu ngữ vì cả hai đều là "luật riêng của món danh hiệu"; tách ra hai
     * nơi thì sớm muộn thêm món mới lại quên mất một nửa.
     */
    private static final Map<Integer, String> RIENG_TOC = new HashMap<Integer, String>();

    static {
        // Susano: nhẫn thuật của nhà Uchiha, người ngoài tộc mặc vào là vô nghĩa.
        RIENG_TOC.put(Integer.valueOf(1096), "uchiha");
    }

    /** Tên gia tộc bắt buộc để dùng món, hoặc null nếu ai cũng dùng được. */
    public static String riengToc(int id) {
        return RIENG_TOC.get(Integer.valueOf(id));
    }

    /**
     * Người này có được phép dùng món đó không.
     *
     * So tên gia tộc không phân biệt hoa thường và bỏ khoảng trắng thừa -- tên gia tộc do người
     * chơi tự đặt, "Uchiha" với "uchiha " phải tính là một.
     */
    public static boolean duocDung(int id, String tenToc) {
        String can = riengToc(id);
        if (can == null) {
            return true;
        }
        return tenToc != null && tenToc.trim().equalsIgnoreCase(can);
    }

    /** Mã biểu ngữ của danh hiệu, hoặc -1 nếu danh hiệu này không có biểu ngữ. */
    public static int bieuNgu(int id) {
        Integer v = BIEU_NGU.get(Integer.valueOf(id));
        return v == null ? -1 : v.intValue();
    }

    static {
        // ---- Susano: nhẫn thuật gia tộc Uchiha, riêng tộc mới dùng được (xem RIENG_TOC) ----
        //
        // Đối chiếu với hai món mạnh nhất game trước khi Susano ra đời:
        //   Sukaigan (mắt thần bậc 10, cần cày 1000 điểm danh vọng x9 loại + 5-10% may rủi):
        //     113 = 5000 -- một chỉ số duy nhất, không có gì khác.
        //   Cầu lục đạo (tím) (rơi hiếm ở Làng Cổ):
        //     58=15, 87=6000, 94=25, 113=800, 114=120, 67=50, 105=3000, 125=5000.
        //
        // Susano cao hơn Cầu lục đạo tím ở MỌI trục trùng nhau, cộng thêm 103 -- chỉ số chưa
        // món nào trong game từng mang. 113 thấp hơn hẳn Sukaigan có chủ đích: Sukaigan là grind
        // khó nhất server, không nên bị một món cấp thẳng qua tay vượt mặt trên chính thế mạnh
        // của nó. Cộng dồn toàn bộ 9 dòng lại thì Susano vẫn là món mạnh nhất, chỉ không đạp lên
        // kỷ lục riêng của Sukaigan.
        dat(1096, new int[][] {
            {58, 20},       // Cộng thêm tiềm năng +20% -- lan sang mọi chỉ số dẫn xuất
            {87, 7000},     // Tấn công +7000
            {94, 30},       // Tấn công +30% (nhân potentialDame*4, đòn bẩy lớn)
            {113, 1000},    // Sát thương chuẩn +1000 -- xuyên kháng và % giảm sát thương
            {103, 300},     // St lên người +300 -- CHỈ sống ở PvP, độc quyền của Susano
            {114, 150},     // Chí mạng +15%
            {67, 60},       // Tấn công khi đánh chí mạng +60%
            {105, 3500},    // Sát thương chí mạng +3500
            {125, 6000},    // HP tối đa +6000
            // ---- Phòng thủ: khiên Bát Chỉ Kính trong hình vẽ phải có tác dụng thật ----
            // Trần cao nhất đang thấy trong game: 98 (miễn giảm) = 20%, 118 (kháng tất cả) = 500
            // (đúng chuỗi ấn tộc, CÙNG Ô trang bị với Susano), 124 (giảm trừ cộng thẳng) = 600.
            // Để dưới trần một khoảng: mạnh rõ rệt nhưng không kịch trần, phòng lẫn công cùng
            // đứng vững chứ không cái này đạp bẹp cái kia.
            {98, 12},       // Miễn giảm sát thương +12% -- nhân vào TOÀN BỘ sát thương trước khi
                             //   nén qua bước chia ~16 lần, chỉ số phòng thủ mạnh nhất PvP
            {124, 500},     // Giảm trừ sát thương +500 -- cộng thẳng, xuyên được cả 113/103
            {118, 350},     // Kháng tất cả +350
            {79, 20},       // Kháng sát thương chí mạng +20%
        });
    }

    static {
        // ---- Susano Sasuke (1266) và Susano Madara (1267) ----
        //
        // Cả hai KHÔNG bị chặn gia tộc (không có mặt trong RIENG_TOC) và cố ý YẾU HƠN Susano
        // Itachi ở MỌI trục -- Itachi là món riêng của nhà Uchiha, phải trả giá bằng việc bị
        // khoá tộc, nên nó giữ ngôi mạnh nhất. Hai món này ai cũng dùng được nên phải nhẹ tay.
        //
        // Trần đối chiếu (Susano Itachi):
        //   58=20, 87=7000, 94=30, 113=1000, 103=300, 114=150, 67=60, 105=3500, 125=6000,
        //   98=12, 124=500, 118=350, 79=20
        //
        // Hai bộ dưới bằng khoảng 70-80% con số đó, và lệch vai nhau cho có lựa chọn:
        // Sasuke thiên công và chí mạng, Madara thiên máu và phòng thủ.

        // Cột: {mã chỉ số, cận dưới, cận trên}. Số trong ngoặc ở chú thích là trần của Itachi.
        //
        // Biên độ để rộng vừa phải (quanh +-25%): hẹp quá thì quay cũng như không, rộng quá thì
        // người xui cầm món vô dụng còn người may cầm món lệch hẳn cán cân.

        // Sasuke -- sát thủ: chí mạng và sát thương cao, phòng thủ mỏng nhất trong ba món.
        datNgau(1266, new int[][] {
            {58, 10, 18},        // tiềm năng %        (Itachi 20)
            {87, 3800, 6200},    // tấn công           (7000)
            {94, 16, 27},        // tấn công %         (30)
            {113, 500, 900},     // sát thương chuẩn   (1000)
            {103, 140, 260},     // st lên người       (300)
            {114, 90, 140},      // chí mạng           (150)
            {67, 35, 55},        // tấn công khi chí mạng (60)
            {105, 1900, 3200},   // st chí mạng        (3500)
            {125, 3000, 5200},   // HP tối đa          (6000)
            {98, 5, 11},         // miễn giảm %        (12)
            {124, 240, 450},     // giảm trừ           (500)
            {118, 170, 320},     // kháng tất cả       (350)
            {79, 9, 18},         // kháng chí mạng     (20)
        });

        // Madara -- trấn thủ: máu và phòng thủ nhỉnh hơn, đổi lại chí mạng thấp hơn hẳn.
        datNgau(1267, new int[][] {
            {58, 11, 18},
            {87, 4000, 6400},
            {94, 15, 25},
            {113, 550, 950},
            {103, 160, 280},
            {114, 75, 130},
            {67, 28, 52},
            {105, 1600, 2900},
            {125, 3800, 5800},
            {98, 7, 11},
            {124, 320, 480},
            {118, 220, 340},
            {79, 12, 19},
        });
    }

    private static void dat(int id, int[][] chiSo) {
        BANG.put(Integer.valueOf(id), chiSo);
    }

    private static void datNgau(int id, int[][] khoang) {
        BANG_NGAU.put(Integer.valueOf(id), khoang);
    }

    /** Có bảng chỉ số cho danh hiệu này không. */
    public static boolean co(int id) {
        Integer k = Integer.valueOf(id);
        return BANG.containsKey(k) || BANG_NGAU.containsKey(k);
    }

    /**
     * Đổ chỉ số của danh hiệu vào món. Trả về false nếu id không nằm trong bảng.
     *
     * Gọi lúc dựng món; danh sách chỉ số của món phải đang rỗng hoặc chấp nhận cộng thêm.
     */
    public static boolean nap(Item it) {
        if (it == null || it.options == null) {
            return false;
        }
        Integer k = Integer.valueOf(it.id);
        int[][] ds = BANG.get(k);
        if (ds != null) {
            for (int[] d : ds) {
                it.options.add(new ItemOption(d[0], d[1]));
            }
            return true;
        }
        // Bảng ngẫu nhiên: quay từng dòng một, nextInt(min, max) lấy CẢ hai đầu mút.
        // Quay ở đây -- lúc món ra lò -- chứ không quay lúc đọc, để con số dính luôn vào món và
        // không đổi mỗi lần đăng nhập. Người chơi phải cầm được cái mình đã xem.
        int[][] kh = BANG_NGAU.get(k);
        if (kh != null) {
            for (int[] d : kh) {
                it.options.add(new ItemOption(d[0], NinjaUtils.nextInt(d[1], d[2])));
            }
            return true;
        }
        return false;
    }
}
