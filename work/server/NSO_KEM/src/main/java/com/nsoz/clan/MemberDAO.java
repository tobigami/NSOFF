/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nsoz.clan;

import com.nsoz.api.Dao;
import com.nsoz.db.jdbc.DbManager;
import com.nsoz.item.Item;
import com.nsoz.lib.ParseData;
import com.nsoz.option.ItemOption;
import com.nsoz.util.Log;
import com.nsoz.util.NinjaUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * @author Admin
 */
public class MemberDAO implements Dao<Member> {

    private Clan clan;
    private List<Member> members = new ArrayList<>();

    public MemberDAO(Clan clan) {
        this.clan = clan;
    }

    public void loadMember(JSONArray jArr) {
        Date now = new Date();
        for (int i = 0; i < jArr.size(); i++) {
            JSONObject obj = (JSONObject) jArr.get(i);
            ParseData parse = new ParseData(obj);
            Member member = Member.builder()
                    .classId(parse.getInt("class_id"))
                    .level(parse.getInt("level"))
                    .type(parse.getInt("type"))
                    .name(parse.getString("name"))
                    .pointClan(parse.getInt("point_clan"))
                    .pointClanWeek(parse.getInt("point_clan_week")).build();
            Date updated_at = new Date(parse.getLong("updated_at"));
            if (!NinjaUtils.isSameWeek(now, updated_at)) {
                member.setPointClanWeek(0);
            }
            members.add(member);
        }
    }

    public void load() {
        Connection conn = null;
        try {
            Date now = new Date();
            conn = DbManager.getConnection();
            PreparedStatement st = conn.prepareStatement("SELECT * FROM `clan_member` WHERE `clan` = ?");
            try {
                st.setInt(1, clan.id);
                ResultSet rs = st.executeQuery();
                while (rs.next()) {
                    int id = rs.getInt("id");
                    byte classId = rs.getByte("class_id");
                    int level = rs.getInt("level");
                    String name = rs.getString("name");
                    byte type = rs.getByte("type");
                    int point_clan = rs.getInt("point_clan");
                    int point_clan_week = rs.getInt("point_clan_week");
                    Date updated_at = rs.getDate("updated_at");
                    if (!NinjaUtils.isSameWeek(now, updated_at)) {
                        point_clan_week = 0;
                        DbManager.executeUpdate("UPDATE `clan_member` SET `point_clan_week` = 0, `updated_at` = ? WHERE `id` = ? LIMIT 1;", NinjaUtils.dateToString(new Date(), "yyyy-MM-dd"), id);
                    }
                    Member member = Member.builder().id(id).classId(classId).level(level).type(type).name(name).pointClan(point_clan).pointClanWeek(point_clan_week).build();
                    members.add(member);
                }
                rs.close();
            } catch (Exception e) {
                 Log.logException("load member fail (1)" , MemberDAO.class,e);
            } finally {
                st.close();
            }
        } catch (SQLException ex) {
             Log.logException("load member fail(2) "  , MemberDAO.class,ex);
        } finally {
            DbManager.closeConnection(conn);
        }
    }


    @Override
    public Optional<Member> get(long id) {
        return members.stream().filter(mem -> mem.getClassId() == id).findFirst();
    }

    @Override
    public List<Member> getAll() {
        return members;
    }

    @Override
    public void save(Member member) {
        members.add(member);
//        Connection conn = null;
//        try {
//            conn = DbManager.getConnection();
//            PreparedStatement st = conn.prepareStatement("INSERT INTO `clan_member` (`name`, `class_id`, `level`, `clan`, `type`) VALUES (?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
//            ResultSet rs = null;
//            try {
//                st.setString(1, member.getName());
//                st.setInt(2, member.getClassId());
//                st.setInt(3, member.getLevel());
//                st.setInt(4, clan.id);
//                st.setInt(5, member.getType());
//                st.executeUpdate();
//                rs = st.getGeneratedKeys();
//                if (rs.next()) {
//                    member.setId(rs.getInt(1));
//                }
//            } finally {
//                st.close();
//                if (rs != null) {
//                    rs.close();
//                }
//            }
            updatePlayer(member);
//
//        } catch (SQLException ex) {
//             Log.logException("save err");
//        } finally {
//            DbManager.closeConnection(conn);
//        }
    }

    public void updatePlayer(Member member) {
        Connection conn = null;
        try {
            conn = DbManager.getConnection();
            PreparedStatement stmt2 = conn.prepareStatement("UPDATE `players` SET `clan` = ? WHERE `id` = ? LIMIT 1;");
            try {
                stmt2.setInt(1, clan.getId());
                stmt2.setInt(2, member.getChar().id);
                stmt2.executeUpdate();
            } finally {
                stmt2.close();
            }
        } catch (Exception e) {
             Log.logException("update member clan player err (2)"  , MemberDAO.class,e);
        } finally {
            DbManager.closeConnection(conn);
        }
    }

    @Override
    public void update(Member member) {
        if (!member.isSaving()) {
            member.setSaving(true);
            Connection conn = null;
            try {
                conn = DbManager.getConnection();
                PreparedStatement stmt2 = conn.prepareStatement("UPDATE `clan_member` SET `level` = ?, `point_clan` = ?, `point_clan_week` = ? WHERE `name` = ? LIMIT 1");
                stmt2.setInt(1, member.getLevel());
                stmt2.setInt(2, member.getPointClan());
                stmt2.setInt(3, member.getPointClanWeek());
                stmt2.setString(4, member.getName());
                stmt2.executeUpdate();
                stmt2.close();
            } catch (SQLException ex) {
                 Log.logException("update member clan err (1)" , MemberDAO.class, ex);
            } finally {
                DbManager.closeConnection(conn);
                member.setSaving(false);
            }
        }
    }

    @Override
    public void delete(Member member) {
        Connection conn = null;
        try {
            conn = DbManager.getConnection();

//            PreparedStatement stmt = conn.prepareStatement("DELETE FROM `clan_member` WHERE `id` = ?;");
//            try {
//                stmt.setInt(1, member.getId());
//                stmt.executeUpdate();
//            } finally {
//                stmt.close();
//            }
            PreparedStatement st = conn.prepareStatement("UPDATE `players` SET `clan` = 0 WHERE `name` = ? LIMIT 1;");
            try {
                st.setString(1, member.getName());
                st.executeUpdate();
            } finally {
                st.close();
            }
            PreparedStatement ps2 = conn.prepareStatement("UPDATE `players` SET `clan` = ? WHERE `name` = ? LIMIT 1;");
            try {
                ps2.setInt(1, 0);
                ps2.setString(2, member.getName());
                ps2.executeUpdate();
            } finally {
                ps2.close();
            }
            if (member.getType() == Clan.TYPE_TOCPHO) {
                PreparedStatement stmt3 = conn.prepareStatement("UPDATE `clan` SET `assist_name` = ? WHERE `id` = ? LIMIT 1;");
                try {
                    stmt3.setString(1, "");
                    stmt3.setInt(2, this.clan.id);
                    stmt3.executeUpdate();
                } finally {
                    stmt3.close();
                }
            }
            members.removeIf(mem -> mem.getName().equals(member.getName()));
        } catch (SQLException ex) {
             Log.logException("delete member clan err", MemberDAO.class,ex);
        } finally {
            DbManager.closeConnection(conn);
        }
    }

}
