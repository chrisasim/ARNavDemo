package com.example.indoornavigation;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHandler extends SQLiteOpenHelper {


    public DatabaseHandler(Context context ) {
        super(context, ConstantsVariables.DATABASE_NAME, null, ConstantsVariables.DATABASE_VERSION);
    }


    //We create our table
    @Override
    public void onCreate(SQLiteDatabase db) {
        //SQL - Structured Query Language
        /*
           create table _name(id, minor, distance, rssi);
         */
        String CREATE_BEACON_TABLE = "CREATE TABLE " + ConstantsVariables.TABLE_NAME + "("
                + ConstantsVariables.KEY_ID + " INTEGER PRIMARY KEY," + ConstantsVariables.KEY_MINOR + " TEXT,"
                + ConstantsVariables.KEY_DISTANCE + " TEXT," + ConstantsVariables.KEY_RSSI + " TEXT" + ")";
        db.execSQL(CREATE_BEACON_TABLE); //creating our table
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + ConstantsVariables.TABLE_NAME);

        //Create a table again
        onCreate(db);
    }

    /*
       CRUD = Create, Read, Update, Delete
     */
    //Add Beacon
    public void addBeacon(BeaconInfo beaconInfo) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(ConstantsVariables.KEY_MINOR, beaconInfo.getMinor());
        values.put(ConstantsVariables.KEY_DISTANCE, beaconInfo.getDistance());
        values.put(ConstantsVariables.KEY_RSSI, beaconInfo.getRssi());

        //Insert to row
        db.insert(ConstantsVariables.TABLE_NAME, null, values);

        db.close(); //closing db connection!
    }


    //Get a beaconInfo
    public BeaconInfo getBeaconInfo(int minor) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(ConstantsVariables.TABLE_NAME,
                new String[]{ ConstantsVariables.KEY_ID, ConstantsVariables.KEY_MINOR, ConstantsVariables.KEY_DISTANCE, ConstantsVariables.KEY_RSSI},
                ConstantsVariables.KEY_MINOR +"=?",new String[]{String.valueOf(minor)},
                null, null, null);

        if (cursor != null)
            cursor.moveToFirst();

        BeaconInfo beaconInfo = new BeaconInfo();
        beaconInfo.setId(Integer.parseInt(cursor.getString(0)));
        beaconInfo.setMinor(Integer.parseInt(cursor.getString(1)));
        beaconInfo.setDistance(Float.parseFloat(cursor.getString(2)));
        beaconInfo.setRssi(Integer.parseInt(cursor.getString(3)));

        return beaconInfo;
    }


    //Get all Beacons
    public List<BeaconInfo> getAllBeacons() {
        List<BeaconInfo> beaconList = new ArrayList<>();

        SQLiteDatabase db = this.getReadableDatabase();

        //Select all beacons
        String selectAll = "SELECT * FROM " + ConstantsVariables.TABLE_NAME;
        Cursor cursor = db.rawQuery(selectAll, null);

        //Loop through our data
        if (cursor.moveToFirst()) {
            do {
                BeaconInfo beaconInfo = new BeaconInfo();
                beaconInfo.setId(Integer.parseInt(cursor.getString(0)));
                beaconInfo.setMinor(Integer.parseInt(cursor.getString(1)));
                beaconInfo.setDistance(Float.parseFloat(cursor.getString(2)));
                beaconInfo.setRssi(Integer.parseInt(cursor.getString(3)));

                //add beacon objects to our list
                beaconList.add(beaconInfo);
            }while (cursor.moveToNext());
        }

        return beaconList;
    }


    //Update beacon
    public void updateBeacon(BeaconInfo beaconInfo) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(ConstantsVariables.KEY_MINOR, beaconInfo.getMinor());
        values.put(ConstantsVariables.KEY_DISTANCE, beaconInfo.getDistance());
        values.put(ConstantsVariables.KEY_RSSI, beaconInfo.getRssi());

        //update the row
        //update(tablename, values, where id = x)
        db.update(ConstantsVariables.TABLE_NAME, values, ConstantsVariables.KEY_ID + "=?",
                new String[]{String.valueOf(beaconInfo.getId())});
    }

//    //Delete single contact
//    public void deleteContact(Contact contact) {
//        SQLiteDatabase db = this.getWritableDatabase();
//
//        db.delete(Util.TABLE_NAME, Util.KEY_ID + "=?",
//                new String[]{String.valueOf(contact.getId())});
//
//        db.close();
//    }

    //Get beaconsInfo count
    public int getCount() {
        String countQuery = "SELECT * FROM " + ConstantsVariables.TABLE_NAME;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);

        return cursor.getCount();

    }

    public boolean checkIfSpecificMinorIsInDB(int minor) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM " + ConstantsVariables.TABLE_NAME + " WHERE " + ConstantsVariables.KEY_MINOR + "=?", new String[] { String.valueOf(minor) });
        if (cursor.getCount() > 0) { // This will get the number of rows
            return true;
        }

        return false;
    }
}