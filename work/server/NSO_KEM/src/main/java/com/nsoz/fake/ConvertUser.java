package com.nsoz.fake;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.nsoz.db.jdbc.DbManager;
import com.nsoz.lib.ParseData;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.sql.Array;
import java.sql.ResultSet;
import java.util.ArrayList;

public class ConvertUser {
//    public static void main(String[] args) {
//        DbManager.start();
////        readUser();
//        ConvertUser convertUser = new ConvertUser();
//
//        // Gọi phương thức không tĩnh getChar() thông qua đối tượng
//        convertUser.getChar();
//    }

    public static void readUser() {
        JSONParser jsonParser = new JSONParser();

        try (FileReader reader = new FileReader("src/main/java/com/nsoz/fake/user.json")) {
            // Đọc JSON từ tệp
            JSONObject jsonObject = (JSONObject) jsonParser.parse(reader);
            JSONArray users = (JSONArray) jsonObject.get("users");
            for (Object userObject : users) {
                JSONObject user = (JSONObject) userObject;

                long id = (long) user.get("id");
                String username = (String) user.get("username");
                long tongnap = (long) user.get("tongnap");

                if (tongnap == 0) {
                    continue;
                }
                System.out.println("Tongnap: " + tongnap);
//                updateTongnap((int) id, username, (int) tongnap);
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }

    public static void updateTongnap(int id, String name, int tongnap) {
        try {
            int status = DbManager.executeUpdate("UPDATE users SET tongnap = tongnap + ? WHERE id = ?", tongnap, id);
            if (status == 1) {
                System.out.println(" Convert user " + name + " to tongnap " + tongnap + " done");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    public void getChar() {
        try {
            ResultSet res = DbManager.executeQueryType("SELECT p.name,u.id, p.dayLogin,p.giftLogin , u.username\n" +
                    "FROM players p\n" +
                    "JOIN users u ON p.user_id = u.id\n" +
                    "WHERE p.dayLogin > 0 AND u.dayLogin = 0\n" +
                    "GROUP BY p.user_id\n" +
                    "ORDER BY p.id;"
            );
            ArrayList<UserConv> userConvs = new ArrayList<>();
            while (res.next()) {
                String username = res.getString("username");
                int id = res.getInt("id");
                int dayLogin = res.getInt("dayLogin");
                String giftLogin = res.getString("giftLogin");

                  DbManager.executeUpdate("UPDATE users SET dayLogin = ?,giftLogin=? WHERE id = ?", dayLogin,giftLogin, id);
                System.out.println("Convert user " + username + " done " + " day login " + dayLogin + " gift login " + giftLogin);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class UserConv {
        public int id;
        public String name;
        public int dayLogin;
        public int[] giftLogin;

        public UserConv(int id, String name, int dayLogin, int[] giftLogin) {
            this.id = id;
            this.name = name;
            this.dayLogin = dayLogin;
            this.giftLogin = giftLogin;

        }
    }
}
