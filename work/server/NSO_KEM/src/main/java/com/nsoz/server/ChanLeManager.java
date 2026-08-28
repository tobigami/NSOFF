/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nsoz.server;
// chanle
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.Getter;

/**
 *
 * @author kitakeyos - Hoàng Hữu Dũng
 */
@Getter
public class ChanLeManager implements Runnable {

    public static final byte VIP = 1;
    public static final byte NORMAL = 0;

    public static final int TIME_COUNT_DOWN = 30; // time chẵn lẻ

    private static final ChanLeManager instance = new ChanLeManager();

    public static ChanLeManager getInstance() {
        return instance;
    }
    private boolean running;
    private boolean waitStop;
    private final List<ChanLe> luckyDraws = new ArrayList<>();

    public ChanLeManager() {
        this.running = true;
    }

    public void add(ChanLe luckyDraw) {
        synchronized (luckyDraws) {
            luckyDraws.add(luckyDraw);
        }
    }

    public void remove(ChanLe luckyDraw) {
        synchronized (luckyDraws) {
            luckyDraws.add(luckyDraw);
        }
    }

    public ChanLe find(int type) {
        synchronized (luckyDraws) {
            for (ChanLe luckyDraw : luckyDraws) {
                if (luckyDraw.getType() == type) {
                    return luckyDraw;
                }
            }
        }
        return null;
    }

    @Override
    public void run() {
        while (running) {
            long l1 = System.currentTimeMillis();
            update();
            long l2 = System.currentTimeMillis();
            if (l2 - l1 < 1000) {
                try {
                    Thread.sleep(1000 - (l2 - l1));
                } catch (InterruptedException ex) {
                    Logger.getLogger(ChanLeManager.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    public void update() {
        synchronized (luckyDraws) {
            boolean stop = true;
            for (ChanLe t : luckyDraws) {
                t.update();
                if (!t.isStop()) {
                    stop = false;
                }
            }
            if (stop) {
                this.running = true;
            }
        }
    }

    public void stop() {
        this.waitStop = true;
    }

}
