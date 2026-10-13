
package com.exchange;

import com.exchange.dao.DatabaseConnection;
import com.exchange.ui.MenuScreen;

public class Main {

    public static void main(String[] args) {
        DatabaseConnection.initializeDatabase();

        MenuScreen menuScreen = new MenuScreen();
        menuScreen.start();
    }
}
