package com.champlain.soft.game;

import javafx.application.Application;

public final class GameLauncher {
    private GameLauncher() {
    }

    public static void main(String[] args) {
        Application.launch(Gameboard.class, args);
    }
}
