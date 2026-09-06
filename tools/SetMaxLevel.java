import com.nsoz.db.jdbc.DbManager;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;

import java.io.FileWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Chỉnh level tối đa của nhân vật bằng cách nối dài/cắt ngắn bảng exp (others.name='exp').
 * Server đọc bảng này vào RAM một lần lúc khởi động (Server.getOther()), nên sau khi chạy tool
 * này BẮT BUỘC phải khởi động lại server thì mới có tác dụng.
 *
 * Trần kỹ thuật là level 254: cả độ dài bảng exp (Server.java, dos.writeByte(exps.length)) lẫn
 * trường level gửi cho client (writeByte(_char.level) ở nhiều chỗ) đều đóng gói vào 1 byte khi
 * gửi qua mạng -- vượt 255 phần tử/level thì bị quấn vòng về 0, hỏng dữ liệu phía client.
 *
 * Usage (chạy trong thư mục work/server/NSO_KEM, giống export-web.sh):
 *   java -cp target/Nso-jar-with-dependencies.jar:../../../build/srvcls SetMaxLevel <level tối đa>
 *   java -cp ... SetMaxLevel --xem              chỉ xem trần hiện tại, không sửa gì
 */
public class SetMaxLevel {

    static final int TRAN_BYTE = 254; // độ dài bảng tối đa 255 phần tử (byte) -> level tối đa 254
    static final long BUOC = 50_000_000_000L; // mỗi khối tăng thêm bấy nhiêu exp
    static final int CO_KHOI = 5; // số mức mỗi khối trước khi tăng bước

    public static void main(String[] args) throws Exception {
        if (!DbManager.start()) {
            System.out.println("Không kết nối được CSDL.");
            return;
        }

        long[] hienTai = docBangExp();
        System.out.println("Bảng exp hiện có " + hienTai.length + " phần tử -> level tối đa hiện tại: " + (hienTai.length - 1));

        if (args.length == 0 || args[0].equals("--xem")) {
            System.out.println("Trần kỹ thuật (giới hạn 1 byte của giao thức): " + TRAN_BYTE);
            return;
        }

        int levelMoi = Integer.parseInt(args[0]);
        if (levelMoi < 1 || levelMoi > TRAN_BYTE) {
            System.out.println("!! Từ chối: level phải trong khoảng 1.." + TRAN_BYTE
                    + " (byte giới hạn của giao thức, xem chú thích đầu file).");
            return;
        }

        int soPhanTuMoi = levelMoi + 1;
        if (soPhanTuMoi == hienTai.length) {
            System.out.println("Level tối đa đã đúng bằng " + levelMoi + " rồi, không cần đổi.");
            return;
        }

        int levelCaoNhatDangCo = levelCaoNhatCuaNguoiChoi();
        if (soPhanTuMoi - 1 < levelCaoNhatDangCo) {
            System.out.println("!! Từ chối: đang có nhân vật ở level " + levelCaoNhatDangCo
                    + ", không thể hạ trần xuống dưới mức đó (sẽ kẹt exp của họ).");
            return;
        }

        long[] moi = soPhanTuMoi > hienTai.length ? noiDai(hienTai, soPhanTuMoi) : cat(hienTai, soPhanTuMoi);

        saoLuu(hienTai);
        ghiBangExp(moi);

        System.out.println("Đã đổi level tối đa: " + (hienTai.length - 1) + " -> " + levelMoi);
        System.out.println("!! Nhớ khởi động lại server (bảng exp chỉ nạp lúc boot) thì mới có tác dụng.");
    }

    static long[] docBangExp() throws Exception {
        Connection conn = DbManager.getConnection();
        try {
            PreparedStatement ps = conn.prepareStatement("SELECT value FROM `others` WHERE name='exp';");
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) throw new IllegalStateException("Không thấy hàng others.name='exp'.");
            JSONArray mang = (JSONArray) JSONValue.parse(rs.getString("value"));
            long[] ra = new long[mang.size()];
            for (int i = 0; i < ra.length; i++) ra[i] = ((Long) mang.get(i)).longValue();
            rs.close();
            ps.close();
            return ra;
        } finally {
            DbManager.closeConnection(conn);
        }
    }

    static int levelCaoNhatCuaNguoiChoi() throws Exception {
        // level nhân vật nằm trong cột `data` (JSON), không có cột riêng -- quét bằng JSON_EXTRACT.
        Connection conn = DbManager.getConnection();
        try {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT MAX(CAST(JSON_EXTRACT(data, '$.level') AS UNSIGNED)) FROM players;");
            ResultSet rs = ps.executeQuery();
            int max = 0;
            if (rs.next()) max = rs.getInt(1);
            rs.close();
            ps.close();
            return max;
        } finally {
            DbManager.closeConnection(conn);
        }
    }

    /** Nối tiếp đúng nhịp thiết kế đang có: khối CO_KHOI mức, mỗi khối tăng thêm BUOC so với khối trước. */
    static long[] noiDai(long[] hienTai, int soPhanTuMoi) {
        long[] moi = new long[soPhanTuMoi];
        System.arraycopy(hienTai, 0, moi, 0, hienTai.length);
        long giaTriCuoi = hienTai[hienTai.length - 1];
        for (int i = hienTai.length; i < soPhanTuMoi; i++) {
            int khoi = (i - hienTai.length) / CO_KHOI;
            moi[i] = giaTriCuoi + BUOC * (khoi + 1);
        }
        return moi;
    }

    static long[] cat(long[] hienTai, int soPhanTuMoi) {
        long[] moi = new long[soPhanTuMoi];
        System.arraycopy(hienTai, 0, moi, 0, soPhanTuMoi);
        return moi;
    }

    static void saoLuu(long[] bang) throws Exception {
        String dau = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        String duongDan = "../../../luutru/csdl/" + dau + "-set-max-level";
        new java.io.File(duongDan).mkdirs();
        List<Long> list = new ArrayList<>();
        for (long v : bang) list.add(v);
        try (FileWriter fw = new FileWriter(duongDan + "/exp-truoc.json")) {
            fw.write(JSONArray.toJSONString(list));
        }
        System.out.println("Đã sao lưu bảng cũ vào " + duongDan + "/exp-truoc.json");
    }

    static void ghiBangExp(long[] bang) throws Exception {
        List<Long> list = new ArrayList<>();
        for (long v : bang) list.add(v);
        String json = JSONArray.toJSONString(list);
        Connection conn = DbManager.getConnection();
        try {
            PreparedStatement ps = conn.prepareStatement("UPDATE `others` SET value=? WHERE name='exp';");
            ps.setString(1, json);
            ps.executeUpdate();
            ps.close();
        } finally {
            DbManager.closeConnection(conn);
        }
    }
}
