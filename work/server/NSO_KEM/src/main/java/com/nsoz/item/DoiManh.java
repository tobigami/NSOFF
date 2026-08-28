package com.nsoz.item;

/**
 * Đổi mảnh lấy đồ khoá.
 *
 * VÌ SAO CÓ: 240 món đã bị khoá không phát qua bảng web nữa (xem CharAdmin.monBiKhoa). Trước đó
 * bảng web là đường DUY NHẤT để có chúng -- không món nào trong số đó rơi ở bất cứ đâu trong game,
 * đã soát toàn bộ hũ rơi đồ. Khoá mà không mở đường khác thì chúng thành hàng chết, và server mất
 * luôn phần nội dung đáng thèm nhất.
 *
 * MỘT LOẠI MẢNH CHO CẢ TẦNG, không phải mỗi món một loại. Trang bị 100 làm kiểu mỗi món một mảnh
 * và cần tới 14 mã item; làm vậy ở đây sẽ tốn 31 mã, nhét đầy túi đồ người chơi bằng 31 chồng
 * mảnh lẻ. Quan trọng hơn: một mảnh cho cả tầng nghĩa là người chơi CHỌN được thứ mình muốn khi
 * đủ số. Cày ba trăm giờ rồi vẫn không ra đúng món cần là kiểu bất công khiến người ta bỏ game,
 * và nó không hề "khó hơn", chỉ "hên xui hơn".
 *
 * Chỉnh cân bằng cũng gọn: đổi một con số ở đây, không phải sửa 31 bảng rơi đồ.
 */
public final class DoiManh {

    /** Mảnh tầng A -- y phục. Rơi từ boss ngoài và boss sự kiện. */
    public static final int MANH_Y_PHUC = 1268;

    /** Mảnh tầng B -- ấn tộc và vũ khí Thiên Vương. Rơi từ boss vùng đất ma quỷ và làng cổ. */
    public static final int MANH_CHIEN_TICH = 1269;

    /**
     * Số mảnh cần cho mỗi tầng.
     *
     * Đặt theo độ mạnh chứ không theo độ hiếm của ảnh: y phục chủ yếu là ngoại hình cộng chút chỉ
     * số nên để vừa phải, ấn tộc và vũ khí Thiên Vương cộng thẳng vào sức đánh nên gấp đôi.
     */
    public static final int GIA_Y_PHUC = 300;
    public static final int GIA_CHIEN_TICH = 600;

    /**
     * Tầng A -- y phục. Toàn bộ đều là món có `fashion > -1`, tức vào ô trang bị 2.
     *
     * KHÔNG gồm Susano, cầu lục đạo và danh hiệu bí kíp: đó là đỉnh tháp, để mở sau khi thấy nhịp
     * cày của hai tầng này đã đúng. Danh hiệu Thiên bảng vốn đã có đường riêng -- thưởng xếp hạng.
     */
    public static final int[][] Y_PHUC = {
        {1236, 0}, {1241, 0}, {1242, 0},          // bộ Hokage: áo, quần, nón
        {1237, 0},                                 // Áo Kage nữ
        {1247, 0}, {1248, 0},                      // Akatsuki nam: áo, quần
        {1250, 0}, {1251, 0}, {1249, 0},           // Akatsuki nữ: áo, quần, nón Deidara
        {1256, 0}, {1257, 0}, {1255, 0},           // Obito Lục Đạo: áo, quần, nón
        {1252, 0},                                 // Nón Naruto Cửu Vĩ
        {1117, 0},                                 // RUBY
        {1157, 0},                                 // Thần Thoại Kiếm
    };

    /** Tầng B -- ấn tộc (870-879) và vũ khí Thiên Vương 100x (1111-1116). */
    public static final int[][] CHIEN_TICH = {
        {870, 0}, {871, 0}, {872, 0}, {873, 0}, {874, 0},
        {875, 0}, {876, 0}, {877, 0}, {878, 0}, {879, 0},
        {1111, 0}, {1112, 0}, {1113, 0}, {1114, 0}, {1115, 0}, {1116, 0},
    };

    private DoiManh() {
    }

    /** Danh sách món đổi được của một loại mảnh, hoặc null nếu không phải mảnh đổi đồ. */
    public static int[][] danhSach(int idManh) {
        if (idManh == MANH_Y_PHUC) {
            return Y_PHUC;
        }
        if (idManh == MANH_CHIEN_TICH) {
            return CHIEN_TICH;
        }
        return null;
    }

    /** Số mảnh cần, hoặc -1 nếu không phải mảnh đổi đồ. */
    public static int gia(int idManh) {
        if (idManh == MANH_Y_PHUC) {
            return GIA_Y_PHUC;
        }
        if (idManh == MANH_CHIEN_TICH) {
            return GIA_CHIEN_TICH;
        }
        return -1;
    }
}
