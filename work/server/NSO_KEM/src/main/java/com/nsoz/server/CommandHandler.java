package com.nsoz.server;

import com.nsoz.event.Event;
import com.nsoz.model.Char;
import com.nsoz.util.Log;

import java.util.List;
import java.util.Scanner;

public class CommandHandler implements Runnable{
    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in);
        while (true) {

                System.out.println("Options:");
                System.out.println("1. Bảo trì");
                System.out.println("2. Lưu dữ liệu");


                System.out.print("Choose an option: ");
                String choice = scanner.nextLine().trim();

                switch (choice) {
                    case "1":
                        System.out.println("Lưu dữ liệu người chơi");
                        List<Char> chars = ServerManager.getChars();
                        for (Char _char : chars) {
                            try {
                                if (_char != null && !_char.isCleaned) {
                                    _char.saveData();
                                    if (Event.getEvent() != null) {
                                        _char.updateEventPoint();
                                    }
                                    if (_char.clone != null && !_char.clone.isCleaned) {
                                        _char.clone.saveData();
                                    }
                                    if (_char.user != null && !_char.user.isCleaned) {
                                        if (_char.user != null) {
                                            _char.user.saveData();
                                        }

                                    }

                                }
                            } catch (Exception ex) {
                                Log.logException("Lỗi Lưu data player action: ", NinjaSchool.class, ex);

                            }
                        }
                        System.out.println("Lưu dữ liệu người chơi xong");
                        break;
                    case "2":
                        System.out.println("You selected Option 2");
                        // Perform task 2
                        break;
                    case "3":
                        System.out.println("You selected Option 3");
                        // Perform task 3
                        break;
                    default:
                        System.out.println("Invalid choice");
                        break;
                }

        }
    }
}
