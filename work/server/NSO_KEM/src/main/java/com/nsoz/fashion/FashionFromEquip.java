/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nsoz.fashion;

import com.nsoz.constants.ItemName;
import com.nsoz.item.Equip;
import com.nsoz.item.Item;
import com.nsoz.item.ItemTemplate;
import com.nsoz.item.Mount;
import com.nsoz.model.Char;
import com.nsoz.util.NinjaUtils;
import com.nsoz.option.ItemOption;

import java.util.ArrayList;

/**
 * @author Admin
 */
// // fix vk 10x
public class FashionFromEquip implements FashionStrategy {

    /**
     * Mảnh dùng cho Ngọc Lục Đạo. Đặt được từ ngoài để so nhanh mảnh cũ (331, đã chạy) với mảnh
     * quỹ đạo mới (334) mà không phải dịch lại: chạy pm2 với biến môi trường NLD_MANH.
     */
    private static final short MANH_THU = (short) Integer.parseInt(
            System.getenv("NLD_MANH") == null ? "334" : System.getenv("NLD_MANH"));


    /**
     * Bảng cải trang: mã trong cột fashion của vật phẩm ứng với ba mảnh đầu, thân, chân trong
     * nj_part. Trả về null nghĩa là bộ đó chưa dựng mảnh, và khi ấy không cải trang gì cả.
     *
     * Trước đây chỗ này ghi những con số lấy nhầm từ bảng khác -- mã 205 trỏ vào 206/207 vốn là
     * thân và chân của bộ Thánh Gióng -- nên dù client có vẽ thì cũng ra sai người. Mọi mã chưa
     * dò lại đều để ngoài bảng, giữ đúng hành vi cũ là không đổi hình.
     */
    private static int[] caiTrang(int ma) {
        switch (ma) {
            case 260:
                return new int[]{309, -1, -1};        // Nón Hokage
            case 261:
                return new int[]{-1, 310, -1};        // Áo Hokage
            case 262:
                return new int[]{-1, -1, 308};        // Quần Hokage
            case 263:
                return new int[]{311, -1, -1};        // Nón Mizukage
            case 264:
                return new int[]{-1, 312, -1};        // Áo Mizukage
            case 265:
                return new int[]{-1, -1, 313};        // Quần Mizukage
            case 266:
                return new int[]{314, -1, -1};        // Nón Deidara
            case 267:
                return new int[]{-1, 315, -1};        // Áo Akatsuki Nam
            case 268:
                return new int[]{-1, -1, 316};        // Quần Akatsuki Nam
            case 269:
                return new int[]{317, -1, -1};        // Nón Deidara Nữ
            case 270:
                return new int[]{-1, 318, -1};        // Áo Akatsuki Nữ
            case 271:
                return new int[]{-1, -1, 319};        // Quần Akatsuki Nữ
            case 272:
                return new int[]{320, -1, -1};        // Nón Naruto Cửu Vĩ
            case 273:
                return new int[]{-1, 321, -1};        // Áo Naruto Cửu Vĩ
            case 274:
                return new int[]{-1, -1, 322};        // Quần Naruto Cửu Vĩ
            case 275:
                return new int[]{323, -1, -1};        // Nón Obito Lục Đạo
            case 276:
                return new int[]{-1, 324, -1};        // Áo Obito Lục Đạo
            case 277:
                return new int[]{-1, -1, 325};        // Quần Obito Lục Đạo
            case 278:
                return new int[]{326, -1, -1};        // Nón Naruto Hiền Nhân
            case 279:
                return new int[]{-1, 327, -1};        // Áo Naruto Hiền Nhân
            case 280:
                return new int[]{-1, -1, 328};        // Quần Naruto Hiền Nhân
            case 281:
                return new int[]{329, -1, -1};        // Mặt nạ Pain
            case 285:
                return new int[]{330, -1, -1};        // Mặt nạ Madara
            default:
                return null;
        }
    }

    /** Xoá sạch cải trang, để mỗi lượt tính lại từ đầu. */
    private static void xoaCaiTrang(Char owner) {
        owner.ID_HAIR = -1;
        owner.ID_BODY = -1;
        owner.ID_LEG = -1;
    }

    /**
     * Góp phần cải trang của một món vào hình đang dựng. Món chỉ ghi đè đúng phần nó có, nên đội
     * nón thì chỉ đổi đầu, mặc áo thì chỉ đổi thân -- ba món rời đắp chồng lên nhau được.
     */
    private static void gopCaiTrang(Char owner, int ma) {
        int[] bo = ma > -1 ? caiTrang(ma) : null;
        if (bo == null) {
            return;
        }
        if (bo[0] > -1) {
            owner.ID_HAIR = (short) bo[0];
        }
        if (bo[1] > -1) {
            owner.ID_BODY = (short) bo[1];
        }
        if (bo[2] > -1) {
            owner.ID_LEG = (short) bo[2];
        }
    }

    private static void gopCaiTrang(Char owner, Equip mon) {
        if (mon != null && mon.template != null) {
            gopCaiTrang(owner, mon.template.fashion);
        }
    }

    public void set(Char owner) {
        Equip eVuKhi = owner.equipment[ItemTemplate.TYPE_VUKHI];
        if (eVuKhi != null) {
            short idFashion = eVuKhi.template.fashion;
            if (idFashion > -1) {
                owner.ID_WEA_PONE = idFashion;
                owner.weapon = -1;
            } else {
                owner.ID_WEA_PONE = -1;
                owner.weapon = eVuKhi.template.part;
            }
        } else {
            owner.ID_WEA_PONE = -1;
            owner.weapon = 15;
        }


        Equip eAo = owner.equipment[ItemTemplate.TYPE_AO];
        if (eAo != null) {
            owner.body = eAo.template.part;
        } else {
            if (owner.gender == 0) {
                owner.body = 10;
            } else {
                owner.body = 1;
            }
        }
        Equip eQuan = owner.equipment[ItemTemplate.TYPE_QUAN];
        if (eQuan != null) {
            owner.leg = eQuan.template.part;
        } else {
            if (owner.gender == 0) {
                owner.leg = 9;
            } else {
                owner.leg = 0;
            }
        }
        Equip eMatNa = owner.equipment[ItemTemplate.TYPE_MATNA];
        Item maskI = owner.getMask();

        if (eMatNa != null || maskI != null) {
            int id = -1;
            short idFasion = -1;
            if (eMatNa != null) {
                id = eMatNa.id;
                owner.head = eMatNa.template.part;
            }
            if (maskI != null) {
                id = maskI.id;
                idFasion = maskI.template.fashion;
                if (idFasion > -1) {
                    // phần cải trang do chỗ tính tập trung phía dưới lo, đây chỉ ghi nhận mã
                } else {
                    owner.head = maskI.template.part;
                }
            }
            switch (id) {
                case ItemName.THUY_TINH:
//                    owner.head = maskI.template.part;
                    owner.body = 186;
                    owner.leg = 187;
                    break;

                case ItemName.SON_TINH:
                    owner.body = 189;
                    owner.leg = 190;
                    break;

                case ItemName.MAT_NA_THANH_GIONG_:
                    owner.body = 206;
                    owner.leg = 207;
                    break;

                case ItemName.MAT_NA_JIRAI_:
                    owner.body = 224;
                    owner.leg = 225;
                    break;

                case ItemName.MAT_NA_JUMITO:
                    owner.body = 227;
                    owner.leg = 228;
                    break;

                case ItemName.TOC_3:
                    owner.body = 230;
                    owner.leg = 231;
                    break;

                case ItemName.TOC_4:
                    owner.body = 233;
                    owner.leg = 234;
                    break;

                case ItemName.TOC_5:
                    owner.body = 236;
                    owner.leg = 237;
                    break;

                case ItemName.TOC_6:
                    owner.body = 239;
                    owner.leg = 240;
                    break;

                case ItemName.TOC_7:
                    owner.body = 242;
                    owner.leg = 243;
                    break;

                case ItemName.TOC_8:
                    owner.body = 245;
                    owner.leg = 246;
                    break;

                case ItemName.MAT_NA_CHUOT:
                    owner.body = 265;
                    owner.leg = 266;
                    break;

                case ItemName.JACK_HOLLOW:
                    owner.body = 259;
                    owner.leg = 260;
                    break;

                case ItemName.SANTA_CLAUS:
                    owner.body = 268;
                    owner.leg = 269;
                    break;

                case ItemName.SUMIMURA_:
                    owner.body = 271;
                    owner.leg = 272;
                    break;

                case ItemName.YUKIMURA_:
                    owner.body = 277;
                    owner.leg = 278;
                    break;

                case ItemName.TON_HANH_GIA:
                    owner.body = 202;
                    owner.leg = 203;
                    owner.weapon = 204;
                    break;
                case ItemName.AKATSUKI_NAM:
                case ItemName.AKATSUKI_NU:
                    break;                            // cải trang tính ở chỗ tập trung phía dưới
                
            }

        } else {
            owner.head = owner.original_head;
        }
        if (owner.fashion[11] != null) {
            owner.ID_MAT_NA = owner.fashion[11].template.fashion;
        } else {
            owner.ID_MAT_NA = -1;
        }
        // Cải trang (trang bị 2). Ba số này client nay hiểu thẳng là chỉ số mảnh trong nj_part --
        // xem tools/ThoiTrangHook.java. Bảng caiTrang() là chỗ duy nhất quyết định bộ nào có hình,
        // mã nào chưa dựng mảnh thì trả về null và nhân vật giữ nguyên đồ đang mặc.
        xoaCaiTrang(owner);
        if (maskI != null) {
            gopCaiTrang(owner, maskI.template.fashion);       // hộp mặt nạ
        }
        gopCaiTrang(owner, owner.fashion[ItemTemplate.TYPE_MATNA]);
        gopCaiTrang(owner, owner.fashion[ItemTemplate.TYPE_AO]);
        gopCaiTrang(owner, owner.fashion[ItemTemplate.TYPE_QUAN]);
        // Ô áo choàng: đọc cả trang bị 1 lẫn trang bị 2, ô nào có đồ thì lấy, trang bị 2 ưu tiên.
        // Bản gốc chỉ đọc trang bị 1, nên áo choàng đeo ở trang bị 2 không hiện gì -- cùng một
        // kiểu bỏ sót đã thấy ở tóc, thân và chân.
        /*
         * Ngọc Lục Đạo mượn **lớp vũ khí** để hiện sáu quả cầu sau lưng.
         *
         * Hình nhân vật chỉ có bốn lớp -- vũ khí, chân, đầu, thân -- vẽ đúng thứ tự đó, nên lớp
         * duy nhất nằm sau lưng là lớp vũ khí. Không có lớp trống nào để thêm trang trí.
         *
         * Đi bằng `owner.weapon` (mã mảnh) chứ không phải `ID_WEA_PONE` (mã cải trang): mã cải
         * trang nằm trong mảng mười số mà client gốc đọc rồi vứt, còn `weapon` là dữ liệu diện mạo
         * lõi, client vốn đã biết vẽ. Đó cũng là chỗ đường áo choàng đã chết.
         *
         * Cái giá: mặc vào thì mất **hình** vũ khí. Chỉ số vũ khí không đụng tới.
         */
        Equip eNgocLucDao = owner.fashion[ItemTemplate.TYPE_AOCHOANG];
        short manhCau = NgocLucDao.manhHienTai(owner);
        if (manhCau > 0) {
            // Mảnh lấy theo pha hiện tại, nên mỗi lần setFashion chạy là một khung quỹ đạo mới.
            owner.weapon = manhCau;
            owner.ID_WEA_PONE = -1;
            NgocLucDao.batDau();
        }

        Equip eAoChoang = owner.fashion[ItemTemplate.TYPE_AOCHOANG];
        if (eAoChoang == null) {
            eAoChoang = owner.equipment[ItemTemplate.TYPE_AOCHOANG];
        }
        if (manhCau > 0) {
            // Cầu lục đạo nằm ở ô áo choàng nhưng KHÔNG được vẽ như áo choàng.
            //
            // Nó vẽ bằng lớp vũ khí (xem đoạn trên). Nếu đặt thêm ID_PP thì client vẽ chồng thêm
            // một lớp áo choàng nữa bằng bộ ảnh cũ 3370-3372 -- di sản của đường áo choàng đã bỏ.
            // Chính người mặc không thấy, vì client bỏ qua gói đổi diện mạo gửi cho nhân vật của
            // mình; người đứng cạnh thì thấy mấy quả cầu tím loé lên mỗi lần đổi pha, trông như
            // khói. Chặn ngay ở đây, đừng để hai lớp cùng vẽ một món.
            owner.ID_PP = -1;
            owner.coat = -1;
        } else if (eAoChoang != null) {
            short idFashion = eAoChoang.template.fashion;
            if (idFashion > -1) {
                owner.ID_PP = idFashion;
                owner.coat = -1;
            } else {
                owner.coat = (short) eAoChoang.id;
                owner.ID_PP = -1;
            }
        } else {
            owner.coat = -1;
            owner.ID_PP = -1;
        }
        if (owner.fashion[ItemTemplate.TYPE_THUNUOI] != null && owner.fashion[ItemTemplate.TYPE_THUNUOI].id != 864) {
            owner.ID_NAME = owner.fashion[ItemTemplate.TYPE_THUNUOI].template.fashion;
        } else {
            owner.ID_NAME = -1;
        }
        Equip eBaoTay = owner.equipment[ItemTemplate.TYPE_BAOTAY];
        if (eBaoTay != null) {
            owner.glove = (short) eBaoTay.id;
        } else {
            owner.glove = -1;
        }

        Mount mount = owner.mount[4];  // hóa hình xe máy
        if (mount != null) {
            short idFashion = mount.template.fashion;
            if (mount.id == ItemName.XE_MAY || mount.id == ItemName.HARLEY_DAVIDSON || mount.id == ItemName.XICH_NHAN_NGAN_LANG) {
                for (ItemOption op : mount.options) {
                    if (op.optionTemplate.id == 161 && op.param > 0) {
                        idFashion = 253;
                    }
                }
            } else if (mount.id == ItemName.SIEU_XE_HALLOWEEN) {
                if (mount.sys == 2) {
                    idFashion = 235 ;
                } else if (mount.sys == 4) {
                    idFashion = 236 ;
                }

            }


            if (idFashion > -1) {
                owner.ID_HORSE = idFashion;
            }
        } else {
            owner.ID_HORSE = -1;
        }
        ArrayList<Integer> listMax = new ArrayList<>();
        for (Equip equip : owner.equipment) {
            if (equip != null && (equip.template.isTypeClothe() || equip.template.isTypeAdorn()
                    || equip.template.isTypeWeapon())) {
                listMax.add(equip.getMaxUpgradeGem());
            }
        }
        if (listMax.size() == 10) {
            listMax.sort((o1, o2) -> o1 - o2);
            int min = listMax.get(0);
            switch (min) {
                case 6:
                case 7:
                    owner.haoQuang = 0;
                    break;
                case 8:
                case 9:
                    owner.haoQuang = 1;
                    break;
                case 10:
                    owner.haoQuang = 2;
                    break;
            }
        } else {
            owner.haoQuang = -1;
        }
        Equip eThuNuoi2 = owner.fashion[ItemTemplate.TYPE_THUNUOI];
        if (eThuNuoi2 != null && eThuNuoi2.id == 864) {
            if (owner.classId > 0) {
                short[] honorIds = new short[]{16, 17, 15, 13, 12, 14};
                owner.honor = honorIds[owner.classId - 1];
            }
        } else {
            owner.honor = -1;
        }
    }

}
