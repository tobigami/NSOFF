import com.nsoz.server.AutoUpgrade;
import com.nsoz.server.GameData;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/** Kiem ham chon da tren kho da that cua itachi: 1 da5, 2 da6, 17 da8, 21 da9, 20 da10. */
public class PickCheck {

    public static void main(String[] a) throws Exception {
        Method choose = AutoUpgrade.class.getDeclaredMethod("choose", long[].class, long.class);
        choose.setAccessible(true);

        List<Long> stock = new ArrayList<>();
        add(stock, 9, 20); add(stock, 8, 21); add(stock, 7, 17); add(stock, 5, 2); add(stock, 4, 1);
        long[] value = new long[stock.size()];
        for (int i = 0; i < value.length; i++) value[i] = stock.get(i);

        System.out.printf("%-5s %-10s %-7s %-46s %s%n", "muc", "can", "tran%", "chon", "tong (phi bao nhieu lan)");
        for (int up = 0; up <= 11; up++) {
            long need = (long) Math.ceil(GameData.MAX_PERCENT[up] * (double) GameData.UP_WEAPON[up] / 100.0);
            @SuppressWarnings("unchecked")
            List<Integer> pick = (List<Integer>) choose.invoke(null, value, need);
            long sum = 0;
            int[] byLevel = new int[13];
            for (int i : pick) { sum += value[i]; byLevel[lv(value[i])]++; }
            StringBuilder d = new StringBuilder();
            for (int L = 12; L >= 1; L--) if (byLevel[L] > 0) d.append(d.length() > 0 ? " + " : "").append(byLevel[L]).append("x da").append(L);
            System.out.printf("+%-4d %-10d %-7d %-46s %d  (%.1fx)%n", up, need, GameData.MAX_PERCENT[up],
                    d, sum, sum / (double) need);
        }
    }

    static void add(List<Long> l, int id, int n) { for (int i = 0; i < n; i++) l.add((long) GameData.UP_CRYSTAL[id]); }
    static int lv(long v) { for (int i = 0; i < 12; i++) if (GameData.UP_CRYSTAL[i] == v) return i + 1; return 0; }
}
