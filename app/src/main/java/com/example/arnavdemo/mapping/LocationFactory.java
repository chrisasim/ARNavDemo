package com.example.arnavdemo.mapping;

public class LocationFactory {

    public Location getLocation(String locationType, String name) {

        if (locationType == null) {
            return null;
        }
        if(locationType.equalsIgnoreCase("CURRENTLOCATION")) {
            return new CurrentLocation(name);
        }
        else if (locationType.equalsIgnoreCase("DESTINATION")) {
            return new Destination(name);
        }

        return null;
    }
}
