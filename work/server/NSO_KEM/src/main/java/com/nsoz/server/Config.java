package com.nsoz.server;

import com.nsoz.util.Log;
import com.nsoz.util.StringUtils;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * @author Administrator
 */
@Getter
@Setter
public class Config {

    @Getter
    private static final Config instance = new Config();
    // Version
    private int dataVersion = 26;
    private int itemVersion = 26;
    private int mapVersion = 26;
    private int skillVersion = 26;
    private int rateEXP;
    private int maxLv;
    private int rmiPort;
    private String rmiHost;
    // Server
    private int serverID;
    private int port;
    // MySql

    // MongoDB
    private boolean mongodbBat;  // tắt mongodb
    private String mongodbHost;
    private int limitTrade;
    private int mongodbPort;
    private String mongodbName;
    private String mongodbUser;
    private String mongodbPassword;
    // Socket.IO
    private String websocketHost;
    private int websocketPort;
    // Game
    private double maxPercentAdd;
    private int sale;
    private boolean showLog;
    private boolean shinwa;
    private int shinwaFee;
    private int shinwaDiscount;
    private int auctionMax;
    private int shinwaMax;
    private boolean arena;
    private int ipAddressLimit;
    private int maxQuantity;
    private String serverDir;
    private String notification;
    private int messageSizeMax;
    private int maintenanceHour;
    private int maintenanceMinute;
    private String passAdmin; // fix bug buff
    public static String hostAdmin = "127.0.0.1"; // fix bug buff
    private String event;

    public boolean load() {
        try {
            FileInputStream input = new FileInputStream("config.properties");
            Properties props = new Properties();
            props.load(new InputStreamReader(input, StandardCharsets.UTF_8));
            props.forEach((t, u) -> {
                System.out.println(String.format("Config - %s: %s", t, u));
            });
            showLog = Boolean.parseBoolean(props.getProperty("server.log.display"));
            serverID = Integer.parseInt(props.getProperty("server.id"));
            port = Integer.parseInt(props.getProperty("server.port"));

            mongodbBat = Boolean.parseBoolean(props.getProperty("mongodb.bat")); // tắt mongodb
            mongodbHost = props.getProperty("mongodb.host");
            mongodbPort = Integer.parseInt(props.getProperty("mongodb.port"));
            mongodbUser = props.getProperty("mongodb.user");
            mongodbPassword = props.getProperty("mongodb.password");
            mongodbName = props.getProperty("mongodb.dbname");
            maxPercentAdd = Integer.parseInt(props.getProperty("game.upgrade.percent.add"));
            sale = Integer.parseInt(props.getProperty("game.store.discount"));
            shinwa = Boolean.parseBoolean(props.getProperty("game.shinwa.active"));
            shinwaFee = Integer.parseInt(props.getProperty("game.shinwa.fee"));
            shinwaDiscount = Integer.parseInt(props.getProperty("game.shinwa.discount"));
            auctionMax = Integer.parseInt(props.getProperty("game.shinwa.max"));
            shinwaMax = Integer.parseInt(props.getProperty("game.shinwa.player.max"));
            websocketHost = props.getProperty("websocket.host");
            websocketPort = Integer.parseInt(props.getProperty("websocket.port"));
            arena = Boolean.parseBoolean(props.getProperty("game.arena.active"));
            ipAddressLimit = Integer.parseInt(props.getProperty("game.login.limit"));
            maxQuantity = Integer.parseInt(props.getProperty("game.quantity.display.max"));
            rateEXP = Integer.parseInt(props.getProperty("game.server.exp"));
            maxLv = Integer.parseInt(props.getProperty("game.server.maxLV"));
            rmiPort = Integer.parseInt(props.getProperty("server.rmi.port"));
            limitTrade = Integer.parseInt(props.getProperty("game.trade.limit"));
            maintenanceHour = Integer.parseInt(props.getProperty("server.maintenance.hour"));
            maintenanceMinute = Integer.parseInt(props.getProperty("server.maintenance.minute"));
            passAdmin = props.getProperty("server.pass.admin"); // fix bug buff
            hostAdmin = props.getProperty("server.host.admin"); // fix bug buff
           rmiHost = props.getProperty("server.rmi.host");
            if (props.containsKey("server.resources.dir")) {
                serverDir = props.getProperty("server.resources.dir");
            } else {
                serverDir = System.getProperty("user.dir");
            }
            if (props.containsKey("server.notification")) {
                notification = props.getProperty("server.notification");
            }
            if (props.containsKey("game.data.version")) {
                dataVersion = Integer.parseInt(props.getProperty("game.data.version"));
            }
            if (props.containsKey("game.item.version")) {
                itemVersion = Integer.parseInt(props.getProperty("game.item.version"));
            }
            if (props.containsKey("game.map.version")) {
                mapVersion = Integer.parseInt(props.getProperty("game.map.version"));
            }
            if (props.containsKey("game.skill.version")) {
                skillVersion = Integer.parseInt(props.getProperty("game.skill.version"));
            }
            if (props.containsKey("client.data.size.max")) {
                messageSizeMax = Integer.parseInt(props.getProperty("client.data.size.max"));
            } else {
                messageSizeMax = 1024;
            }
            if (props.containsKey("game.event")) {
                event = props.getProperty("game.event");
            }
        } catch (Exception ex) {
           ex.printStackTrace(); // op lai file nay len cho toi
            return false;
        }
        return true;
    }

    public String getMongodbUrl() {
        if (!StringUtils.isNullOrEmpty(mongodbUser) && !StringUtils.isNullOrEmpty(mongodbPassword)) {
            return String.format("mongodb://%s:%s@%s:%d/%s", mongodbUser, mongodbPassword, mongodbHost, mongodbPort, mongodbName);
        }
        return String.format("mongodb://%s:%d", mongodbHost, mongodbPort);
    }
}
