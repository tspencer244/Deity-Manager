package com.wurmonline.server;

import java.lang.String;

public enum DbConnector$WurmDatabaseSchema {
    CREATURES("WURMCREATURES"),
    SPELLS("WURMSPELLS"),
    DEITIES("WURMDEITIES"),
    ECONOMY("WURMECONOMY"),
    ITEMS("WURMITEMS"),
    LOGIN("WURMLOGIN"),
    LOGS("WURMLOGS"),
    PLAYERS("WURMPLAYERS"),
    TEMPLATES("WURMTEMPLATES"),
    ZONES("WURMZONES"),
    SITE("WURMSITE");

    private final String database;

    private DbConnector$WurmDatabaseSchema(String database) {
        this.database = database;
    }

    public String toString() {
        return this.name();
    }

    public String getDatabase() {
        return this.database;
    }
}
