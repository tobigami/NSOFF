package com.nsoz.server;

import com.nsoz.item.Item;
import com.nsoz.option.ItemOption;
import com.nsoz.util.NinjaUtils;

/**
 * Bộ chỉ số kiểu menu "Trang bị" của NPC Vua Hùng: mạnh bất thường, không đạt được bằng đường chơi.
 *
 * Đoạn này chép nguyên văn từ AdminService.openUIAdmin để hai nơi cho ra món giống hệt nhau, kể cả
 * mấy chỗ khác thường -- cố ý giữ nguyên chứ không "sửa cho đúng":
 *
 *   - upgrade bị cộng hai lần: gán 12 rồi gọi next(12), mà next cộng dồn chứ không gán, nên ô nâng
 *     cấp hiện +24 trong khi chỉ số chỉ được cộng phần của 12 bậc.
 *   - tinh luyện chạy đủ 9 bậc và CỘNG DỒN từng bậc, trong khi đường tinh luyện thật (Char.java)
 *     chỉ lấy đúng một mức theo bậc hiện có. Chênh 3 đến 5 lần tuỳ chỉ số.
 *   - ghép thêm hai chỉ số cho mỗi ô, kể cả chỉ số món đó vốn không có.
 *
 * Sửa mấy chỗ trên thì món phát ra sau này sẽ yếu hơn hẳn món đã phát trước đó, nên để nguyên và
 * ghi rõ ở đây là lựa chọn có chủ ý.
 */
public final class VuaHungBoost {

    private VuaHungBoost() {
    }

    /** Nhồi chỉ số vào một món đã dựng sẵn ở mức MAX. sys là hệ muốn gán (1 Phong, 2 Hoả, 3 Băng). */
    public static void apply(Item itm, int sys) {
        apply(itm, sys, true);
    }

    /**
     * Như trên, nhưng {@code napCap = false} thì không đụng tới ô nâng cấp.
     *
     * Mặt nạ, áo choàng, mắt thần, bí kíp trong game không nâng cấp được lấy một bậc. Nhồi +24 vào
     * chúng thì món hiện một con số mà cả game không có đường nào tạo ra, mà nhìn vào không ai
     * ngờ. Chỉ số vẫn nhồi như cũ -- chỉ riêng ô nâng cấp là để yên.
     */
    public static void apply(Item itm, int sys, boolean napCap) {
        if (itm == null || itm.template == null || itm.options == null) {
            return;
        }
        // optionId[itm.sys - 1] bên dưới sẽ tràn mảng nếu hệ là 0 (không hệ), nên kẹp về 1..3.
        if (sys < 1 || sys > 3) {
            sys = 1;
        }
        if (napCap) {
            itm.upgrade = (byte) 12;
            itm.next(itm.upgrade);
        }
        itm.sys = (byte) sys;
        ItemOption option = new ItemOption(85, 0);
        itm.options.add(option);
        switch (itm.template.type) {
            case 0: {
                int[] optionId = {95, 96, 97};
                itm.options.add(new ItemOption(optionId[itm.sys - 1], 5));
                itm.options.add(new ItemOption(79, 5));
                break;
            }
            case 1: {
                itm.options.add(new ItemOption(87, NinjaUtils.nextInt(250, 400)));
                int[] optionId = {88, 89, 90};
                itm.options.add(new ItemOption(optionId[itm.sys - 1], NinjaUtils.nextInt(350, 600)));
                break;
            }
            case 2:
                itm.options.add(new ItemOption(80, NinjaUtils.nextInt(24, 28)));
                itm.options.add(new ItemOption(91, NinjaUtils.nextInt(10, 14)));
                break;
            case 3:
                itm.options.add(new ItemOption(81, 5));
                itm.options.add(new ItemOption(79, 5));
                break;
            case 4:
                itm.options.add(new ItemOption(86, NinjaUtils.nextInt(76, 124)));
                itm.options.add(new ItemOption(94, NinjaUtils.nextInt(76, 124)));
                break;
            case 5: {
                int[] optionId = {95, 96, 97};
                itm.options.add(new ItemOption(optionId[itm.sys - 1], 5));
                itm.options.add(new ItemOption(92, NinjaUtils.nextInt(9, 11)));
                break;
            }
            case 6:
                itm.options.add(new ItemOption(83, NinjaUtils.nextInt(250, 450)));
                itm.options.add(new ItemOption(82, NinjaUtils.nextInt(250, 450)));
                break;
            case 7: {
                int[] optionId = {95, 96, 97};
                itm.options.add(new ItemOption(optionId[itm.sys - 1], 5));
                optionId = new int[]{88, 89, 90};
                itm.options.add(new ItemOption(optionId[itm.sys - 1], NinjaUtils.nextInt(350, 600)));
                break;
            }
            case 8:
                itm.options.add(new ItemOption(83, NinjaUtils.nextInt(250, 450)));
                itm.options.add(new ItemOption(84, NinjaUtils.nextInt(76, 124)));
                break;
            case 9:
                itm.options.add(new ItemOption(84, NinjaUtils.nextInt(76, 124)));
                itm.options.add(new ItemOption(82, NinjaUtils.nextInt(250, 450)));
                break;
            default:
                break;
        }
        for (int i = option.param; i < 9; i++) {
            for (ItemOption option1 : itm.options) {
                if (option1.optionTemplate.type != 8 || option1.optionTemplate.id == 85) {
                    continue;
                }
                switch (option1.optionTemplate.id) {
                    case 94: {
                        int[] percentIncreases = new int[]{10, 10, 10, 20, 20, 30, 40, 50, 60};
                        option1.param += percentIncreases[option.param];
                        break;
                    }
                    case 86: {
                        int[] percentIncreases = new int[]{25, 30, 35, 40, 50, 60, 80, 115, 165};
                        option1.param += percentIncreases[option.param];
                        break;
                    }
                    case 87: {
                        int[] percentIncreases = new int[]{50, 60, 70, 90, 130, 180, 250, 330,
                                500};
                        option1.param += percentIncreases[option.param];
                        break;
                    }
                    case 88:
                    case 89:
                    case 90: {
                        int[] percentIncreases = new int[]{50, 70, 100, 140, 190, 250, 320, 400,
                                500};
                        option1.param += percentIncreases[option.param];
                        break;
                    }
                    case 92: {
                        int[] percentIncreases = new int[]{5, 5, 5, 5, 5, 5, 10, 10, 20};
                        option1.param += percentIncreases[option.param];
                        break;
                    }
                    case 95:
                    case 96:
                    case 97: {
                        int[] percentIncreases = new int[]{5, 5, 5, 5, 5, 5, 10, 10, 15};
                        option1.param += percentIncreases[option.param];
                        break;
                    }
                    case 82:
                    case 83: {
                        int[] percentIncreases = new int[]{40, 60, 80, 100, 140, 220, 300, 420,
                                590};
                        option1.param += percentIncreases[option.param];
                        break;
                    }
                    case 84: {
                        int[] percentIncreases = new int[]{25, 30, 35, 40, 50, 60, 80, 115, 165};
                        option1.param += percentIncreases[option.param];
                        break;
                    }
                    case 79: {
                        int[] percentIncreases = new int[]{1, 2, 2, 2, 2, 2, 3, 3, 4};
                        option1.param += percentIncreases[option.param];
                        break;
                    }
                    case 81: {
                        int[] percentIncreases = new int[]{1, 2, 2, 2, 2, 2, 3, 3, 4};
                        option1.param += percentIncreases[option.param];
                        break;
                    }
                    case 80: {
                        int[] percentIncreases = new int[]{5, 5, 5, 5, 10, 10, 15, 15, 20};
                        option1.param += percentIncreases[option.param];
                        break;
                    }
                    case 91: {
                        int[] percentIncreases = new int[]{5, 5, 5, 5, 5, 5, 10, 10, 15};
                        option1.param += percentIncreases[option.param];
                        break;
                    }
                    default:
                        break;
                }
            }
            option.param++;
        }
    }
}
