package com.example.indoornavigation;

public class ConstantsVariables {
    public static boolean activity_started_bluetooth = false;
    public static String uuid = "7777772e6b6b6d636e2e636f6d000001";
    public static int minor_437 = 1;
    public static int minor_570 = 2;
    public static int minor_574 = 3;
    public static float sensor = 0;

    //Database related items
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "beacon_db";
    public static final String TABLE_NAME = "beacons";

    //Beacons table columns names
    public static final String KEY_ID = "id";
    public static final String KEY_MINOR = "minor";
    public static final String KEY_DISTANCE = "distance";
    public static final String KEY_RSSI = "rssi";

}
