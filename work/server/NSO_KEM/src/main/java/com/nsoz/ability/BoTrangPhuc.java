package com.nsoz.ability;

import com.nsoz.item.Equip;
import com.nsoz.item.Item;
import com.nsoz.model.Char;

/**
 * Thưởng theo bộ cho mấy bộ trang phục trang bị 2 tự thêm: mặc hai mảnh được một mớ, đủ ba mảnh
 * được thêm một mớ nữa.
 *
 * Chỗ móc là {@link AbilityFromEquip#setAbility} -- nơi máy chủ dồn mọi chỉ số từ đồ vào mảng
 * {@code owner.options}. Cộng thẳng vào mảng ấy nên thưởng bộ đi đúng đường của chỉ số thường:
 * vào sát thương, vào HP tối đa, vào kháng, không cần đụng gì tới client.
 *
 * Chọn cách này thay vì nhét chỉ số vào chính món đồ, vì món đồ thì lưu xuống cơ sở dữ liệu --
 * cởi ra một mảnh mà chỉ số cộng thêm vẫn nằm đó thì hỏng. Mảng {@code owner.options} dựng lại
 * từ đầu mỗi lần tính, nên cởi ra là mất, mặc vào là có, không cần dọn gì.
 *
 * Mã chỉ số chỉ lấy trong nhóm máy chủ thật sự có đọc (đã dò trong mã nguồn), và tránh nhóm
 * type 3..7 vì nhóm ấy còn đòi bậc nâng cấp mới tính.
 */
public final class BoTrangPhuc {

    /** Một bộ: tên, ba mã món (nón, áo, quần), thưởng khi đủ hai mảnh, thưởng thêm khi đủ ba. */
    private static final class Bo {
        final String ten;
        final int[] mon;
        final int[][] hai;
        final int[][] ba;

        Bo(String ten, int[] mon, int[][] hai, int[][] ba) {
            this.ten = ten;
            this.mon = mon;
            this.hai = hai;
            this.ba = ba;
        }
    }

    // 125 HP tối đa · 117 MP tối đa · 94 tấn công +% · 118 kháng tất cả · 114 chí mạng
    // 115 né đòn · 116 chính xác · 105 sát thương chí mạng · 102 st lên quái · 103 st lên người
    // 119 hồi MP mỗi 5 giây · 120 hồi HP mỗi 5 giây · 121 kháng st chí mạng % · 124 giảm trừ st
    // 100 tăng % kinh nghiệm · 58 cộng thêm tiềm năng %
    private static final Bo[] BANG = {
            new Bo("Hokage", new int[]{1242, 1236, 1241},
                    new int[][]{{125, 3000}, {118, 30}},
                    new int[][]{{94, 8}, {124, 200}, {120, 150}}),
            new Bo("Mizukage", new int[]{1243, 1244, 1245},
                    new int[][]{{117, 3000}, {118, 30}},
                    new int[][]{{114, 15}, {105, 800}, {119, 150}}),
            new Bo("Akatsuki Nam", new int[]{1246, 1247, 1248},
                    new int[][]{{116, 150}, {102, 800}},
                    new int[][]{{94, 10}, {105, 1000}}),
            new Bo("Akatsuki Nữ", new int[]{1249, 1250, 1251},
                    new int[][]{{115, 150}, {103, 600}},
                    new int[][]{{94, 10}, {121, 10}}),
            new Bo("Naruto Cửu Vĩ", new int[]{1252, 1253, 1254},
                    new int[][]{{125, 4000}, {120, 200}},
                    new int[][]{{94, 12}, {102, 1200}}),
            new Bo("Obito Lục Đạo", new int[]{1255, 1256, 1257},
                    new int[][]{{115, 200}, {118, 40}},
                    new int[][]{{124, 300}, {94, 8}}),
            new Bo("Naruto Hiền Nhân", new int[]{1258, 1259, 1260},
                    new int[][]{{117, 4000}, {116, 150}},
                    new int[][]{{58, 20}, {100, 15}}),
    };

    private BoTrangPhuc() {
    }

    /** Cộng thưởng bộ vào mảng chỉ số đang dựng. Gọi sau khi đã dồn xong chỉ số của từng món. */
    public static void ap(Char owner) {
        if (owner == null || owner.options == null) {
            return;
        }
        for (Bo bo : BANG) {
            int co = dem(owner, bo.mon);
            if (co >= 2) {
                cong(owner, bo.hai);
            }
            if (co >= 3) {
                cong(owner, bo.ba);
            }
        }
    }

    /**
     * Số mảnh của bộ đang mặc trên người.
     *
     * Phải soi ba chỗ chứ không phải hai: nón có thể nằm ở ô trang bị 1, ô trang bị 2, hoặc ô mặt
     * nạ riêng ({@code Char.mask}) -- FashionFromEquip cũng đọc cả ba, và bỏ sót chỗ nào thì bộ
     * "đủ ba mảnh" lại thành hai. Món hết hạn không tính, đúng như cách vòng cộng chỉ số bỏ qua.
     */
    private static int dem(Char owner, int[] mon) {
        Item matNa = owner.getMask();
        int n = 0;
        for (int id : mon) {
            boolean co = dangMac(owner.equipment, id) || dangMac(owner.fashion, id)
                    || (matNa != null && matNa.id == id && !matNa.isExpired());
            if (co) {
                n++;
            }
        }
        return n;
    }

    private static boolean dangMac(Equip[] o, int id) {
        if (o == null) {
            return false;
        }
        for (Equip e : o) {
            if (e != null && e.id == id && !e.isExpired()) {
                return true;
            }
        }
        return false;
    }

    private static void cong(Char owner, int[][] thuong) {
        for (int[] t : thuong) {
            if (t[0] >= 0 && t[0] < owner.options.length) {
                owner.options[t[0]] += t[1];
                owner.haveOptions[t[0]] = true;
            }
        }
    }

    /**
     * Mô tả thưởng bộ của một món, để ghi vào cột description. Trả về null nếu món không thuộc bộ.
     *
     * Một dòng, không xuống dòng: trong cả bảng vật phẩm không có mô tả nào chứa ký tự xuống dòng,
     * nên không rõ client dựng lại thế nào -- không thử vận may ở chỗ chỉ để cho đẹp.
     */
    public static String moTa(int idMon) {
        for (Bo bo : BANG) {
            for (int id : bo.mon) {
                if (id == idMon) {
                    return "Trang phục bộ " + bo.ten + ". Mặc 2 mảnh: " + ke(bo.hai)
                            + ". Đủ 3 mảnh thêm: " + ke(bo.ba) + ".";
                }
            }
        }
        return null;
    }

    private static String ke(int[][] thuong) {
        StringBuilder sb = new StringBuilder();
        for (int[] t : thuong) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            String m = TEN[t[0]] == null ? ("chỉ số #" + t[0] + " #") : TEN[t[0]];
            sb.append(m.replace("#", String.valueOf(t[1])));
        }
        return sb.toString();
    }

    private static final String[] TEN = new String[160];

    static {
        TEN[58] = "tiềm năng +#%";
        TEN[94] = "tấn công +#%";
        TEN[100] = "kinh nghiệm đánh quái +#%";
        TEN[102] = "sát thương lên quái +#";
        TEN[103] = "sát thương lên người +#";
        TEN[105] = "sát thương chí mạng +#";
        TEN[114] = "chí mạng +#";
        TEN[115] = "né đòn +#";
        TEN[116] = "chính xác +#";
        TEN[117] = "MP tối đa +#";
        TEN[118] = "kháng tất cả +#";
        TEN[119] = "hồi MP mỗi 5 giây +#";
        TEN[120] = "hồi HP mỗi 5 giây +#";
        TEN[121] = "kháng sát thương chí mạng +#%";
        TEN[124] = "giảm trừ sát thương +#";
        TEN[125] = "HP tối đa +#";
    }

    /** In ra "id<tab>mô tả" cho từng món có bộ, để tập lệnh ngoài ghi vào cơ sở dữ liệu. */
    public static void main(String[] args) {
        for (Bo bo : BANG) {
            for (int id : bo.mon) {
                System.out.println(id + "\t" + moTa(id));
            }
        }
    }
}
