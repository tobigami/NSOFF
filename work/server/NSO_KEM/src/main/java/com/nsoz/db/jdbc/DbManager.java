/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nsoz.db.jdbc;

import com.nsoz.constants.SQLStatement;
import com.nsoz.item.Item;
import com.nsoz.server.Config;
import com.nsoz.stall.StallManager;
import com.nsoz.util.Log;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

public class DbManager {
    private static final Logger logger = LoggerFactory.getLogger(DbManager.class);
    private static String DRIVER = "com.mysql.jdbc.Driver";
    private static String URL = "jdbc:mysql://%s:%s/%s?useUnicode=yes&characterEncoding=UTF-8";
    private static String DB_HOST = "localhost";
    private static String DB_PORT = "3306";
    private static String DB_NAME = "cbro_girlkun";
    private static String DB_USER = "root";
    private static String DB_PASSWORD = "123456";
    private static int MIN_CONN = 1;
    private static int MAX_CONN = 1;
    private static long MAX_LIFE_TIME = 120000L;
    private static long IDLE_TIMEOUT = 600000L;
    public static boolean LOG_QUERY = false;
    private static final HikariConfig config = new HikariConfig();
    private static HikariDataSource ds;
    private static DbManager instance = null;
    public static final int GAME = 0;

    public DbManager() {

    }


    public static Connection getConnection() throws SQLException {

        return ds.getConnection();
    }

    public static void close() {
        ds.close();
    }

    public static void closeConnection(Connection conn) {
        try {
            if (conn != null) {
                conn.close();
            }
        } catch (Exception e) {
             Log.logException("Close connection error",DbManager.class, e);
        }
    }

    private static void loadProperties() {
        Properties properties = new Properties();

        try {
            FileInputStream input = new FileInputStream("mysql.properties");
            Properties props = new Properties();
            props.load(new InputStreamReader(input, StandardCharsets.UTF_8));
            props.forEach((t, u) -> {
                System.out.println(String.format("MYSQCFG - %s: %s", t, u));
            });
            if ((props.containsKey("nsoz.database.driver"))) {
                DRIVER = props.getProperty("nsoz.database.driver");
            }

            if ((props.containsKey("nsoz.database.host"))) {
                DB_HOST = props.getProperty("nsoz.database.host");
            }

            if ((props.containsKey("nsoz.database.port"))) {
                DB_PORT = props.getProperty("nsoz.database.port");
            }

            if ((props.containsKey("nsoz.database.name"))) {
                DB_NAME = props.getProperty("nsoz.database.name");
            }

            if ((props.containsKey("nsoz.database.user"))) {
                DB_USER = props.getProperty("nsoz.database.user");
            }

            if ((props.containsKey("nsoz.database.pass"))) {
                DB_PASSWORD = props.getProperty("nsoz.database.pass");
            }

            if ((props.containsKey("nsoz.database.min"))) {
                MIN_CONN = Integer.parseInt(props.getProperty("nsoz.database.min"));
            }

            if ((props.containsKey("nsoz.database.max"))) {
                MAX_CONN = Integer.parseInt(props.getProperty("nsoz.database.max"));
            }

            if ((props.containsKey("nsoz.database.lifetime"))) {
                MAX_LIFE_TIME = (long) Integer.parseInt(props.getProperty("nsoz.database.lifetime"));
            }

            if ((props.containsKey("nsoz.database.log"))) {
                LOG_QUERY = Boolean.parseBoolean(props.getProperty("nsoz.database.log"));
            }
            if ((props.containsKey("nsoz.database.idleTimeout"))) {
                IDLE_TIMEOUT = (long) Integer.parseInt(props.getProperty("nsoz.database.idleTimeout"));
            }

            System.out.println("Load file load mysql.properties thành công!");
        } catch (Exception var5) {
            System.out.println("Load file load mysql.properties thất bại!");
            Log.logException("Load file load mysql.properties thất bại!", DbManager.class, var5);

        }

    }

    public static ResultSet executeQuery(String query) throws Exception {
        try {
            Connection con = getConnection();
            Throwable var2 = null;

            Object var7;
            try {
                PreparedStatement ps = con.prepareStatement(query);
                Throwable var4 = null;

                try {
                    ResultSet rs = ps.executeQuery();
                    Throwable var6 = null;

                    try {


                        var7 = rs;
                    } catch (Throwable var54) {
                        var7 = var54;
                        var6 = var54;
                        throw var54;
                    } finally {
                        if (rs != null) {
                            if (var6 != null) {
                                try {
                                    rs.close();
                                } catch (Throwable var53) {
                                    var6.addSuppressed(var53);
                                }
                            } else {
                                rs.close();
                            }
                        }

                    }
                } catch (Throwable var56) {
                    var4 = var56;
                    throw var56;
                } finally {
                    if (ps != null) {
                        if (var4 != null) {
                            try {
                                ps.close();
                            } catch (Throwable var52) {
                                var4.addSuppressed(var52);
                            }
                        } else {
                            ps.close();
                        }
                    }

                }
            } catch (Throwable var58) {
                var2 = var58;
                throw var58;
            } finally {
                if (con != null) {
                    if (var2 != null) {
                        try {
                            con.close();
                        } catch (Throwable var51) {
                            var2.addSuppressed(var51);
                        }
                    } else {
                        con.close();
                    }
                }

            }

            return (ResultSet) var7;
        } catch (Exception var60) {
            Log.logException("Có lỗi xảy ra khi thực thi câu lệnh: " + query, DbManager.class, var60);
            throw var60;
        }
    }

    public static ResultSet executeQueryType(String query, Object... objs) throws Exception {
        try {
            Connection con = getConnection();
            Throwable var3 = null;

            Object var6;
            try {
                PreparedStatement ps = con.prepareStatement(query, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
                Throwable var5 = null;

                try {
                    for (int i = 0; i < objs.length; ++i) {
                        ps.setObject(i + 1, objs[i]);
                    }

                    if (LOG_QUERY) {
                        LogSQL.gI().log(ps.toString());
                    }

                    var6 =ps.executeQuery();
                } catch (Throwable var31) {
                    var6 = var31;
                    var5 = var31;
                    throw var31;
                } finally {
                    if (ps != null) {
                        if (var5 != null) {
                            try {
                                ps.close();
                            } catch (Throwable var30) {
                                var5.addSuppressed(var30);
                            }
                        } else {
                            ps.close();
                        }
                    }

                }
            } catch (Throwable var33) {
                var3 = var33;
                throw var33;
            } finally {
                if (con != null) {
                    if (var3 != null) {
                        try {
                            con.close();
                        } catch (Throwable var29) {
                            var3.addSuppressed(var29);
                        }
                    } else {
                        con.close();
                    }
                }

            }

            return (ResultSet) var6;
        } catch (Exception var35) {
            Log.logException("Có lỗi xảy ra khi thực thi câu lệnh: " + query, DbManager.class, var35);
            throw var35;
        }
    }

    public static ResultSet executeQuery(String query, Object... objs) throws Exception {
        try {
            Connection con = getConnection();
            Throwable var3 = null;

            Object var6;
            try {
                PreparedStatement ps = con.prepareStatement(query);
                Throwable var5 = null;

                try {
                    for (int i = 0; i < objs.length; ++i) {
                        ps.setObject(i + 1, objs[i]);
                    }

                    if (LOG_QUERY) {
                        LogSQL.gI().log(ps.toString());
                    }

                    var6 = ps.executeQuery();
                } catch (Throwable var31) {
                    var6 = var31;
                    var5 = var31;
                    throw var31;
                } finally {
                    if (ps != null) {
                        if (var5 != null) {
                            try {
                                ps.close();
                            } catch (Throwable var30) {
                                var5.addSuppressed(var30);
                            }
                        } else {
                            ps.close();
                        }
                    }

                }
            } catch (Throwable var33) {
                var3 = var33;
                throw var33;
            } finally {
                if (con != null) {
                    if (var3 != null) {
                        try {
                            con.close();
                        } catch (Throwable var29) {
                            var3.addSuppressed(var29);
                        }
                    } else {
                        con.close();
                    }
                }

            }

            return (ResultSet) var6;
        } catch (Exception var35) {
            Log.logException("Có lỗi xảy ra khi thực thi câu lệnh: " + query, DbManager.class, var35);
            throw var35;
        }
    }

    public static int executeUpdate(String query) throws Exception {
        int rowUpdated = -1;

        try {
            Connection con = getConnection();
            Throwable var3 = null;

            try {
                PreparedStatement ps = con.prepareStatement(query);
                Throwable var5 = null;

                try {
                    if (LOG_QUERY) {
                        LogSQL.gI().log(ps.toString());
                    }

                    rowUpdated = ps.executeUpdate();
                } catch (Throwable var30) {
                    var5 = var30;
                    throw var30;
                } finally {
                    if (ps != null) {
                        if (var5 != null) {
                            try {
                                ps.close();
                            } catch (Throwable var29) {
                                var5.addSuppressed(var29);
                            }
                        } else {
                            ps.close();
                        }
                    }

                }
            } catch (Throwable var32) {
                var3 = var32;
                throw var32;
            } finally {
                if (con != null) {
                    if (var3 != null) {
                        try {
                            con.close();
                        } catch (Throwable var28) {
                            var3.addSuppressed(var28);
                        }
                    } else {
                        con.close();
                    }
                }

            }

            return rowUpdated;
        } catch (Exception var34) {

            Log.logException("Có lỗi xảy ra khi thực thi câu lệnh: " + query, DbManager.class, var34);
            throw var34;
        }
    }

    public static int executeUpdate(String query, Object... objs) throws Exception {
        if (query.indexOf("insert") == 0 && query.lastIndexOf("()") == query.length() - 2) {
            StringBuilder sb = new StringBuilder();
            sb.append("(");

            for (int i = 0; i < objs.length; ++i) {
                sb.append("?");
                if (i < objs.length - 1) {
                    sb.append(",");
                } else {
                    sb.append(")");
                }
            }

            query = query.replace("()", sb.toString());
        }

        try {
            Connection con = getConnection();
            Throwable var38 = null;

            int var6;
            try {
                PreparedStatement ps = con.prepareStatement(query);
                Throwable var5 = null;

                try {
                    for (int i = 0; i < objs.length; ++i) {
                        ps.setObject(i + 1, objs[i]);
                    }

                    if (LOG_QUERY) {
                        LogSQL.gI().log(ps.toString());
                    }

                    var6 = ps.executeUpdate();
                } catch (Throwable var31) {
                    var6 = -1;
                    var5 = var31;
                    throw var31;
                } finally {
                    if (ps != null) {
                        if (var5 != null) {
                            try {
                                ps.close();
                            } catch (Throwable var30) {
                                var5.addSuppressed(var30);
                            }
                        } else {
                            ps.close();
                        }
                    }

                }
            } catch (Throwable var33) {
                var38 = var33;
                throw var33;
            } finally {
                if (con != null) {
                    if (var38 != null) {
                        try {
                            con.close();
                        } catch (Throwable var29) {
                            var38.addSuppressed(var29);
                        }
                    } else {
                        con.close();
                    }
                }

            }

            return var6;
        } catch (Exception var35) {
            Log.logException("Có lỗi xảy ra khi thực thi câu lệnh: " + query, DbManager.class, var35);
            throw var35;
        }
    }

    public static ArrayList<HashMap<String, Object>> convertResultSetToList(ResultSet rs) {
        ArrayList<HashMap<String, Object>> list = new ArrayList<HashMap<String, Object>>();
        try {
            ResultSetMetaData resultSetMetaData = rs.getMetaData();
            int count = resultSetMetaData.getColumnCount();
            while (rs.next()) {
                HashMap<String, Object> map = new HashMap<>();
                int index = 1;
                while (index <= count) {
                    int type = resultSetMetaData.getColumnType(index);
                    String name = resultSetMetaData.getColumnName(index);
                    switch (type) {
                        case -5: {
                            map.put(name, rs.getLong(index));
                            break;
                        }
                        case 6: {
                            map.put(name, rs.getFloat(index));
                            break;
                        }
                        case 12: {
                            map.put(name, rs.getString(index));
                            break;
                        }
                        case 1: {
                            map.put(name, rs.getString(index));
                            break;
                        }
                        case 4: {
                            map.put(name, rs.getInt(index));
                            break;
                        }
                        case 16: {
                            map.put(name, rs.getBoolean(index));
                            break;
                        }
                        case -7: {
                            map.put(name, rs.getByte(index));
                            break;
                        }

                        default: {
                            map.put(name, rs.getObject(index));
                            break;
                        }
                    }
                    ++index;
                }
                list.add(map);
            }
            rs.close();
        } catch (SQLException sQLException) {
            Log.logException("convertResultSetToList Lỗi", DbManager.class, sQLException);

        }
        return list;
    }


    public static int updateAmountUnpaid(int userID, int amount) {
        try {
            return executeUpdate(SQLStatement.UPDATE_AMOUNT_UNPAID, amount, userID);
        } catch (Exception e) {
            Log.logException("updateAmountUnpaid lỗi", DbManager.class, e);
            return -1;
        }
    }

    public static int updateYenUnpaid(int userID, int amount) {
        try {
            return executeUpdate(SQLStatement.UPDATE_YEN_UNPAID, amount, userID);
        } catch (Exception e) {
            Log.logException("updateYenUnpaid lỗi", DbManager.class, e);

            return -1;
        }
    }

    public static int updateCoinUnpaid(int userID, int amount) {
        try {
            return executeUpdate(SQLStatement.UPDATE_COIN_UNPAID, amount, userID);
        } catch (Exception e) {
            Log.logException("updateCoinUnpaid lỗi", DbManager.class, e);
            return -1;
        }
    }

    public static int updateLuong(int userID, int amount) {
        try {
            return executeUpdate(SQLStatement.UPDATE_AMOUNT_UNPAID, amount, userID);
        } catch (Exception e) {
            Log.logException("updateLuong lỗi", DbManager.class, e);
            return -1;
        }
    }

    public static int updateGold(int userID, int amount) {
        try {
            return executeUpdate(SQLStatement.UPDATE_GOLD, amount, userID);
        } catch (Exception e) {
            Log.logException("updateGold lỗi", DbManager.class, e);

            return -1;
        }
    }


    public int updateNinja(int userID, String ninja) {
        try {
            return executeUpdate(SQLStatement.UPDATE_NINJA, ninja, userID);
        } catch (Exception e) {
            return -1;
        }

    }

    public static int updateCoin(int playerID, int amount) {
        try {
            return executeUpdate(SQLStatement.UPDATE_COIN, amount, playerID);
        } catch (Exception e) {
            Log.logException("updateCoin lỗi", DbManager.class, e);
            return -1;
        }

    }

    // điểm vxmm
    public static int updateTopvxmm(int playerId, int top) {
        try {
            return executeUpdate(SQLStatement.UPDATE_TOP_VXMM, top, playerId);
        } catch (Exception e) {
            Log.logException("updateTopvxmm error :", DbManager.class,e);

            return -1;
        }

    }

    public int updateTopvxmmLuong(int playerId, int top) {
        try {
            return executeUpdate(SQLStatement.UPDATE_TOP_VXMM_LUONG, top, playerId);
        } catch (Exception e) {
            Log.logException("updateTopvxmm error :", DbManager.class,e);

            return -1;
        }

    }

    public static int updateYen(int playerID, int amount) {
        try {
            return executeUpdate(SQLStatement.UPDATE_YEN, amount, playerID);
        } catch (Exception e) {
            Log.logException("updateYen lỗi", DbManager.class, e);
            return -1;
        }

    }

    // điểm vxmm
//    public int updateTopvxmm(int playerId, int top) {
//        return update(SQLStatement.UPDATE_TOP_VXMM, top, playerId);
//    }


    public void addGold(int userID, int amount) {
        try {
            executeUpdate(SQLStatement.ADD_GOLD, amount, userID);
        } catch (Exception e) {
            Log.logException("addGold lỗi", DbManager.class, e);
        }
    }

    public static int addCoin(int playerID, int amount) {
        try {
            return executeUpdate(SQLStatement.ADD_COIN, amount, playerID);
        } catch (Exception e) {
            Log.logException("addCoin lỗi", DbManager.class, e);
            return -1;
        }

    }

    public int addYen(int playerID, int amount) {
        try {
            return executeUpdate(SQLStatement.ADD_YEN, amount, playerID);
        } catch (Exception e) {
            Log.logException("addYen lỗi", DbManager.class, e);
            return -1;
        }

    }

    public static int updateMessage(int playerID, String message) {
        try {
            return executeUpdate(SQLStatement.UPDATE_MESSAGE, message, playerID);
        } catch (Exception e) {
            Log.logException("updateMessage lỗi", DbManager.class, e);
            return -1;
        }

    }

    public static int updateProduct(Item item) {
        try {
            return executeUpdate(SQLStatement.UPDATE_PRODUCT, item.getProductStatus(), item.getProductTime(), item.getProductUniqueId());
        } catch (Exception e) {
            Log.logException("updateProduct lỗi", DbManager.class, e);
            return -1;
        }

    }

    public static void insertItemToStall(Item item) {
        try (Connection conn = getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(SQLStatement.INSERT_ITEM_TO_STALL,
                    Statement.RETURN_GENERATED_KEYS);

            stmt.setInt(1, Config.getInstance().getServerID());
            stmt.setString(2, item.getProductSeller());
            stmt.setString(3, item.toJSONObject().toJSONString());
            stmt.setInt(4, item.getProductPrice());
            stmt.setInt(5, item.getProductStatus());
            stmt.setInt(6, item.getProductTime());
            stmt.executeUpdate();
            ResultSet generatedKeys = stmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                item.setProductUniqueId(generatedKeys.getInt(1));
            }
            stmt.close();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            Log.logException("insertItemToStall lỗi", DbManager.class, e);
        }
    }

    public static DbManager getInstance() {
        if (instance == null) {
            instance = new DbManager();
        }
        return instance;
    }

    public static boolean start() {
        if (ds != null) {
            Log.warn("DB Connection Pool has already been created.");
            return false;
        } else {
            try {
                loadProperties();
                config.setDriverClassName(DRIVER);
                config.setJdbcUrl(String.format(URL, DB_HOST, DB_PORT, DB_NAME));
                config.setUsername(DB_USER);
                config.setPassword(DB_PASSWORD);
                config.setMinimumIdle(MIN_CONN);
                config.setMaximumPoolSize(MAX_CONN);
                config.setPoolName("THNS_pool");
                config.setMaxLifetime(MAX_LIFE_TIME);
                config.setConnectionTimeout(30_000L);
                config.setIdleTimeout(IDLE_TIMEOUT);
                config.addDataSourceProperty("cachePrepStmts", "true");
                config.addDataSourceProperty("prepStmtCacheSize", "250");
                config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
                config.addDataSourceProperty("useServerPrepStmts", "true");
                config.addDataSourceProperty("useLocalSessionState", "true");
                config.addDataSourceProperty("rewriteBatchedStatements", "true");
                config.addDataSourceProperty("cacheResultSetMetadata", "true");
                config.addDataSourceProperty("cacheServerConfiguration", "true");
                config.addDataSourceProperty("elideSetAutoCommits", "true");
                config.addDataSourceProperty("maintainTimeStats", "false");
//                config.setLeakDetectionThreshold(20000);
                ds = new HikariDataSource(config);
                System.out.println("DB Connection Pool has created.");
                return true;
            } catch (Exception e) {
                System.out.println("DB Connection Pool Creation has failed. " + e.getMessage());
                Log.logException("DB Connection Pool Creation has failed.", DbManager.class, e);
                System.out.println("DB Connection Pool Creation has failed. " + e.getMessage());
                return false;
            }
        }
    }

    public static String getInfo() {
        HikariPoolMXBean poolMXBean = ds.getHikariPoolMXBean();
        StringBuilder sb = new StringBuilder();
        sb.append("DB Connection Pool Info:\n");
        sb.append(" |- ActiveConnections:   ").append(poolMXBean.getActiveConnections());
        sb.append(" |- IdleConnections:     ").append(poolMXBean.getIdleConnections());
        sb.append(" |- TotalConnections:    ").append(poolMXBean.getTotalConnections());
        sb.append(" |- ThreadsAwaitingConnection: ").append(poolMXBean.getThreadsAwaitingConnection());
        sb.append(" |- MaxConnections: ").append(ds.getMaximumPoolSize());
        sb.append(" |- MinConnections: ").append(ds.getMinimumIdle());
        return sb.toString();
    }
}
