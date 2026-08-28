import com.nsoz.server.GameData;

/** Doi chieu chien luoc "it da nhat ma cham tran ti le" voi kho da that cua itachi. */
public class UpCheck {
    // so da itachi dang co, theo id (Da cap N = id N-1)
    static int[] have = new int[12];
    static { have[4]=1; have[5]=2; have[7]=17; have[8]=21; have[9]=20; }

    public static void main(String[] a) {
        System.out.println("UP_CRYSTAL: da cap 1..12 = ");
        for (int i = 0; i < 12; i++) System.out.print("  " + (i+1) + ":" + GameData.UP_CRYSTAL[i]);
        System.out.println("\n");
        System.out.printf("%-6s %-9s %-8s %-12s %s%n", "muc", "can diem", "tran %", "chien luoc", "con lai");
        int[] stock = have.clone();
        for (int up = 0; up <= 11; up++) {
            long need = (long) Math.ceil(GameData.MAX_PERCENT[up] * (double) GameData.UP_WEAPON[up] / 100.0);
            StringBuilder pick = new StringBuilder();
            long got = 0; int n = 0;
            int[] tmp = stock.clone();
            for (int id = 11; id >= 0 && got < need && n < 17; id--) {
                while (tmp[id] > 0 && got < need && n < 17) {
                    tmp[id]--; got += GameData.UP_CRYSTAL[id]; n++;
                    pick.append(pick.length() > 0 ? "+" : "").append("da").append(id + 1);
                }
            }
            System.out.printf("+%-5d %-9d %-8d %-12s %s%n", up, need, GameData.MAX_PERCENT[up],
                    n == 0 ? "HET DA" : pick.toString(), got >= need ? "cham tran" : "chi duoc " + (got * 100 / GameData.UP_WEAPON[up]) + "%");
        }
    }
}
