package com.example.indoornavigation;

public class BeaconInfo {
    private int id;
    private int minor;
    private float distance;
    private int rssi;


    public BeaconInfo(int id, int minor, float distance, int rssi) {
        this.id = id;
        this.minor = minor;
        this.distance = distance;
        this.rssi = rssi;
    }

    public BeaconInfo(int minor, float distance, int rssi) {
        this.minor = minor;
        this.distance = distance;
        this.rssi = rssi;
    }


    public BeaconInfo() {

    }

    public int getMinor() {
        return minor;
    }

    public float getDistance() {
        return distance;
    }

    public int getRssi() {
        return rssi;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setMinor(int minor) {
        this.minor = minor;
    }

    public void setDistance(float distance) {
        this.distance = distance;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }
}
