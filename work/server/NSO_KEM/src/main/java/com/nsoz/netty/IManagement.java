/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nsoz.netty;

/**
 * @author PC
 */
public interface IManagement {

    // Player service
    public int addGold(int id, int gold) throws Exception;

    public int addCoin(int id, int gem) throws Exception;

    public int addYen(int id, int gemLock) throws Exception;

    public int addItem(int id, int itemId, int quantity, String options, int upgrade, boolean lock, int expires) throws Exception;

    public int removeItemBag(int id, int itemId) throws Exception;

    public int removeItemBox(int id, int itemId) throws Exception;

    public int saveData(int id) throws Exception;

    public int forceOut(int id) throws Exception;

    // Server service
    public int serverAlert(String message) throws Exception;

    public int serverMaintenance(String message) throws Exception;

    public int serverSaveData() throws Exception;

    public int updateLuckyWheel() throws Exception;
}
