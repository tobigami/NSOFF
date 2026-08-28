/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nsoz.server;

import com.nsoz.model.Char;
import com.nsoz.model.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author PC
 */
public class ServerManager {

    public static ConcurrentHashMap<String, Integer> countAttendanceByIp = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, Integer> countUseGiftCodeByIp = new ConcurrentHashMap<>();
    private static List<User> users = new CopyOnWriteArrayList<>();
    private static List<Char> chars = new CopyOnWriteArrayList<>();
    private static List<String> ips = new CopyOnWriteArrayList<>();
    private static List<String> ipsNotActive = new CopyOnWriteArrayList<>();

    public static List<Char> getChars() {
        return new ArrayList<>(chars);
    }

    public static List<User> getUsers() {
        return new ArrayList<>(users);
    }

    public static int getNumberOnline() {
        return chars.size();
    }

    public static int frequency(String ip) {
        return Collections.frequency(ips, ip);
    }

    public static void add(String ip) {
        ips.add(ip);
    }

    public static void remove(String ip) {
        ips.remove(ip);
    }

    public static int frequencyNotactive(String ip) {
        return Collections.frequency(ipsNotActive, ip);
    }

    public static void addNotactive(String ip) {
        ipsNotActive.add(ip);
    }

    public static void removeNotactive(String ip) {
        ipsNotActive.remove(ip);
    }

    public static User findUserByUserID(int id) {
        for (User user : users) {
            if (user.id == id) {
                return user;
            }
        }
        return null;
    }

    public static long count(long id) {
        return users.stream().filter(u -> u.id == id).count();
    }

    public static User findUserByUsername(String username) {

        for (User user : users) {
            if (user.username.equals(username)) {
                return user;
            }
        }
        return null;

    }

    public static Char findCharById(int id) {

        for (Char _char : chars) {
            if (_char.id == id) {
                return _char;
            }
        }
        return null;

    }

    public static Char findCharByName(String name) {
        for (Char _char : chars) {
//                if(_char.user.SVIP){
//                    name = "[SVIP] "+name;
//                }
            if (_char != null && !_char.isCleaned && _char.name.equals(name)) {
                if (_char.clone != null && !_char.clone.isNhanBan) {
                    return _char.clone;
                } else {
                    return _char;
                }
            }
        }
        return null;
    }

    public static void addUser(User user) {
        users.add(user);
    }

    public static void addChar(Char _char) {
        chars.add(_char);
    }

    public static void removeUser(User user) {

        users.removeIf(u -> {
            if (u.id == user.id) {
                return true;
            }
            return false;
        });
    }

    public static void removeChar(Char _char) {
        chars.removeIf(c -> {
            if (c.id == _char.id) {
                return true;
            }
            return false;
        });
    }
}
