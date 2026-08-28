package com.nsoz.fake;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.nsoz.constants.SQLStatement;
import com.nsoz.db.jdbc.DbManager;
import com.nsoz.effect.EffectCharPaint;
import com.nsoz.effect.EffectInfoPaint;
import com.nsoz.lib.ParseData;
import com.nsoz.map.Waypoint;
import com.nsoz.model.Clazz;
import com.nsoz.option.SkillOption;
import com.nsoz.server.Config;
import com.nsoz.server.GameData;
import com.nsoz.skill.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ReadData {
    public static EffectCharPaint[] efs;
    public static SkillPaint[] sks;

    public static void main(String[] args) {
//        if (Config.getInstance().load()) {
//            if (!DbManager.start()) {
//                return;
//            }
//            readEfect();
//            GameData.getInstance().init();
////            readSkill();
//        }
        convert();
    }

    public static void readEfect() {
        DataInputStream dataInputStream = null;
        try {
            PreparedStatement stmt = DbManager.getConnection().prepareStatement(SQLStatement.NJ_EFF);

            byte[] ab = GameData.getInstance().loadFile("src/main/java/com/nsoz/fake/nj_effect");
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(ab);

            dataInputStream = new DataInputStream(byteArrayInputStream);
            int num = dataInputStream.readShort();
            efs = new EffectCharPaint[num];
            JSONObject data = new JSONObject();

            for (int i = 0; i < num; i++) {
                efs[i] = new EffectCharPaint();
                efs[i].idEf = dataInputStream.readShort();
                efs[i].arrEfInfo = new EffectInfoPaint[dataInputStream.readByte()];
                for (int j = 0; j < efs[i].arrEfInfo.length; j++) {
                    efs[i].arrEfInfo[j] = new EffectInfoPaint();
                    efs[i].arrEfInfo[j].idImg = dataInputStream.readShort();
                    efs[i].arrEfInfo[j].dx = dataInputStream.readByte();
                    efs[i].arrEfInfo[j].dy = dataInputStream.readByte();
                }


            }
            for (int j = 0; j < efs.length; j++) {
                stmt.setString(1, convertArrEfInfoToJson(efs[j].arrEfInfo));
                stmt.executeUpdate();
            }
            stmt.close();
            String json = convertEfArrayToJson(efs);
//            saveJsonToFile(json, "logs/nj_eff.json");
        } catch (Exception ex) {
            System.out.println("loi doc eff");
            System.out.println(ex.getMessage() + ex.getStackTrace());
        } finally {
            try {
                dataInputStream.close();
            } catch (Exception ex2) {
                System.out.println(ex2.getMessage() + ex2.getStackTrace());
            }
        }
    }


    public static void readSkill()
    {
        DataInputStream dataInputStream = null;
        try
        {
            byte[] ab = GameData.getInstance().loadFile("src/main/java/com/nsoz/fake/nj_skill");

            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(ab);

            dataInputStream = new DataInputStream(byteArrayInputStream);
            int num = dataInputStream.readShort();
            System.out.println(num);
            int num2 = 0;
            System.out.println(GameData.getInstance().getClazzs().size());
            System.out.println(num);
            for (int i = 0; i < GameData.getInstance().getClazzs().size(); i++)
            {
                num2 += GameData.getInstance().getClazzs().get(i).getSkillTemplates().size();
            }
            sks = new SkillPaint[num2];
            JsonArray json1 = new JsonArray();
            for (int j = 0; j < num; j++)
            {
                short num3 =  (dataInputStream.readShort() );
                sks[num3] = new SkillPaint();
                sks[num3].id = num3;
                sks[num3].effId = dataInputStream.readShort();
                sks[num3].numEff = dataInputStream.readByte();
                sks[num3].skillStand = new SkillInfoPaint[dataInputStream.readByte()];
                for (int k = 0; k < sks[num3].skillStand.length; k++)
                {
                    sks[num3].skillStand[k] = new SkillInfoPaint();
                    sks[num3].skillStand[k].status = dataInputStream.readByte();
                    sks[num3].skillStand[k].effS0Id = dataInputStream.readShort();
                    sks[num3].skillStand[k].e0dx = dataInputStream.readShort();
                    sks[num3].skillStand[k].e0dy = dataInputStream.readShort();
                    sks[num3].skillStand[k].effS1Id = dataInputStream.readShort();
                    sks[num3].skillStand[k].e1dx = dataInputStream.readShort();
                    sks[num3].skillStand[k].e1dy = dataInputStream.readShort();
                    sks[num3].skillStand[k].effS2Id = dataInputStream.readShort();
                    sks[num3].skillStand[k].e2dx = dataInputStream.readShort();
                    sks[num3].skillStand[k].e2dy = dataInputStream.readShort();
                    sks[num3].skillStand[k].arrowId = dataInputStream.readShort();
                    sks[num3].skillStand[k].adx = dataInputStream.readShort();
                    sks[num3].skillStand[k].ady = dataInputStream.readShort();
                }
                sks[num3].skillfly = new SkillInfoPaint[dataInputStream.readByte()];
                for (int l = 0; l < sks[num3].skillfly.length; l++)
                {
                    sks[num3].skillfly[l] = new SkillInfoPaint();
                    sks[num3].skillfly[l].status = dataInputStream.readByte();
                    sks[num3].skillfly[l].effS0Id = dataInputStream.readShort();
                    sks[num3].skillfly[l].e0dx = dataInputStream.readShort();
                    sks[num3].skillfly[l].e0dy = dataInputStream.readShort();
                    sks[num3].skillfly[l].effS1Id = dataInputStream.readShort();
                    sks[num3].skillfly[l].e1dx = dataInputStream.readShort();
                    sks[num3].skillfly[l].e1dy = dataInputStream.readShort();
                    sks[num3].skillfly[l].effS2Id = dataInputStream.readShort();
                    sks[num3].skillfly[l].e2dx = dataInputStream.readShort();
                    sks[num3].skillfly[l].e2dy = dataInputStream.readShort();
                    sks[num3].skillfly[l].arrowId = dataInputStream.readShort();
                    sks[num3].skillfly[l].adx = dataInputStream.readShort();
                    sks[num3].skillfly[l].ady = dataInputStream.readShort();
                }
                String json = convertSkillArrayToJson(sks);
//                System.out.println(json);
                saveJsonToFile(json, "logs/nj_skill.json");
            }

//
//            saveJsonToFile(json, "logs/nj_skill.json");
//
        }
        catch (Exception ex)
        {
            System.out.println(ex.getMessage() + ex.getStackTrace());
        }
        finally
        {
            try
            {
                dataInputStream.close();
            }
            catch (Exception ex2)
            {
                System.out.println(ex2.getMessage() + ex2.getStackTrace());
            }
        }
    }

    public static void convert(){
        JSONArray Option = (JSONArray)JSONValue.parse("");
        Waypoint waypoint=new Waypoint();
        parseWaypoints("[[1416,192,1440,216,70,35,528],[24,0,96,24,23,80,1848]]");
    }
    private static void parseWaypoints(String waypointJson) {
        List<Waypoint> waypoints = new ArrayList<>();
        JSONArray jArr = (JSONArray) JSONValue.parse(waypointJson);
        JSONArray jar2;
        for(int j = 0; j < jArr.size(); ++j) {
            Waypoint waypoint = new Waypoint();
            jar2 = (JSONArray)JSONValue.parse(jArr.get(j).toString());
            waypoint.minX = Short.parseShort(jar2.get(0).toString());
            waypoint.minY = Short.parseShort(jar2.get(1).toString());
            waypoint.maxX = Short.parseShort(jar2.get(2).toString());
            waypoint.maxY = Short.parseShort(jar2.get(3).toString());
            waypoint.next = Short.parseShort(jar2.get(4).toString());
            waypoint.x = Short.parseShort(jar2.get(5).toString());
            waypoint.y = Short.parseShort(jar2.get(6).toString());
            waypoints.add(waypoint);
        }

        String json = convertWaupointArrayToJson(waypoints);
        System.out.println(json);
    }
    private static String convertEfArrayToJson(EffectCharPaint[] efs) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(efs);
    }
    private static String convertWaupointArrayToJson(List<Waypoint>  efs) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(efs);
    }

    private static String convertSkillArrayToJson(SkillPaint[] sks) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(sks);
    }

    private static String convertArrEfInfoToJson(EffectInfoPaint[] arrEfInfo) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(arrEfInfo);
    }

    static void saveJsonToFile(String json, String filePath) {
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(json);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
