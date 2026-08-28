package com.nsoz.model;

import com.nsoz.util.Log;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class GameUpdate {
    ScheduledThreadPoolExecutor executor;
    public GameUpdate() {
        executor = new ScheduledThreadPoolExecutor(1000);
    }
    public void add(IUpdate update) {
        if (update == null) {
             Log.info("Attempted to add a null IUpdate instance to GameUpdate");
            return;
        }
        System.out.println( "Adding IUpdate instance to GameUpdate");
        executor.scheduleAtFixedRate(update::update, 0, 1000, TimeUnit.MILLISECONDS);
    }
    public void remove() {
        executor.shutdown();
    }
}
