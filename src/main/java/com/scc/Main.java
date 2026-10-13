
package com.exchange;

import com.scc.dao.DatabaseConnection;
import com.scc.ui.MenuScreen;

public class Main {

    public static void main(String[] args) {
        DatabaseConnection.initializeDatabase();

        MenuScreen menuScreen = new MenuScreen();
        menuScreen.start();
    }
}
