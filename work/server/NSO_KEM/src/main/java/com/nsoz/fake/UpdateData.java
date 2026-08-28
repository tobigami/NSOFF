package com.nsoz.fake;

import com.nsoz.constants.SQLStatement;
import com.nsoz.db.jdbc.DbManager;
import com.nsoz.server.Config;
import com.nsoz.util.Log;
import com.nsoz.util.NinjaUtils;
import com.nsoz.util.StringUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Random;

public class UpdateData {
    public static void main(String[] args) {

        try {
            if (!DbManager.start()) {
                return;
            }
            for (int i = 69; i <= 99; i++) {
                String username = "oimaoi0" + i;
                String pass = StringUtils.genPass("big0copassnhe");
                DbManager.executeUpdate(SQLStatement.UPDATE_USER, pass, username);
            }
//            String username = "0857826200";
//            String pass = StringUtils.genPass("phone989");
//            DbManager.executeUpdate(SQLStatement.UPDATE_USER2, pass, username);

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String generateRandomUsername() {
        String characters = "abcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder username = new StringBuilder();

        for (int i = 0; i < 7; i++) {
            int index = NinjaUtils.nextInt(characters.length());
            username.append(characters.charAt(index));
        }

        return username.toString();
    }

    private static String generateRandomOTP() {
        Random rand = new Random();
        int otp = 100000 + rand.nextInt(900000); // Random 6-digit OTP without leading zeros
        return String.valueOf(otp);
    }

    private static String generateRandomPhoneNumber() {
        int prefix = 300000000 + NinjaUtils.nextInt(700000000); // Random 10-digit phone number starting with 03
        return "0" + prefix;
    }
}
