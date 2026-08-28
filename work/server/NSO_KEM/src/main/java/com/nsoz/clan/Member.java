/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nsoz.clan;

import com.nsoz.item.Item;
import com.nsoz.model.Char;
import com.nsoz.option.ItemOption;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * @author Admin
 */
@Setter
@Getter
public class Member {

//    private int id;
    private int classId;
    private int level;
    private int type;
    private String name;
    private int pointClan;
    private int pointClanWeek;
    private boolean online;
    private long updated_at;
    private Char p;
    @Getter
    @Setter
    private boolean saving;

    @Builder
    public Member(int id, int classId, int level, int type, String name, int pointClan, int pointClanWeek) {
//        this.id = id;
        this.classId = classId;
        this.level = level;
        this.type = type;
        this.name = name;
        this.pointClan = pointClan;
        this.pointClanWeek = pointClanWeek;
    }

    public Char getChar() {
        return p;
    }

    public void setChar(Char p) {
        this.p = p;
    }

    public void addPointClan(int point) {
        this.pointClan += point;
    }

    public void addPointClanWeek(int point) {
        this.pointClanWeek += point;
    }

    public JSONObject toJSONObject() {
        JSONObject obj = new JSONObject();
        obj.put("name", this.name);
        obj.put("class_id", this.classId);
        obj.put("level", this.level);
        obj.put("point_clan", this.pointClan);
        obj.put("point_clan_week", this.pointClanWeek);
        obj.put("type", this.type);
        obj.put("updated_at", this.updated_at);
        return obj;
    }
}
