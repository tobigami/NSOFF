/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nsoz.server;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Admin
 */
public class AutoSaveData implements Runnable {

    @Override
    public void run() { /// fix lưu dữ liệu
        while (Server.start) {
            try {
                Thread.sleep(10000);
                Server.saveAll();
//                System.out.println("Lưu data");

            } catch (InterruptedException ex) {
                Logger.getLogger(AutoSaveData.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
    }

}
