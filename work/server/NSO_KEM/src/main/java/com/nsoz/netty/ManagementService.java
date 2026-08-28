package com.nsoz.netty;

public class ManagementService  implements IManagement{
    @Override
    public int addGold(int id, int gold) throws Exception {
        return 0;
    }

    @Override
    public int addCoin(int id, int gem) throws Exception {
        return 0;
    }

    @Override
    public int addYen(int id, int gemLock) throws Exception {
        return 0;
    }


    @Override
    public int addItem(int id, int itemId, int quantity, String options, int upgrade, boolean lock, int expires) throws Exception {
        return 0;
    }

    @Override
    public int removeItemBag(int id, int itemId) throws Exception {
        return 0;
    }

    @Override
    public int removeItemBox(int id, int itemId) throws Exception {
        return 0;
    }

    @Override
    public int saveData(int id) throws Exception {
        return 0;
    }

    @Override
    public int forceOut(int id) throws Exception {
        return 0;
    }

    @Override
    public int serverAlert(String message) throws Exception {
        return 0;
    }

    @Override
    public int serverMaintenance(String message) throws Exception {
        return 0;
    }

    @Override
    public int serverSaveData() throws Exception {
        return 0;
    }

    @Override
    public int updateLuckyWheel() throws Exception {
        return 0;
    }
}
