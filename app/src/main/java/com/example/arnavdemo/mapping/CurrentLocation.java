package com.example.arnavdemo.mapping;

import java.util.ArrayList;

public class CurrentLocation implements Location{

    private String name;

    public CurrentLocation(String name) {
        this.name = name;
    }

    @Override
    public ArrayList<Integer> getCoordinates() {

        ArrayList<Integer> coords = new ArrayList<>();

        switch (name) {
//            case "entrance":
//                coords.add(48);
//                coords.add(190);
//                return coords;
//            case "graduateStudentOfficeA1":
//                coords.add(280);
//                coords.add(213);
//                return coords;
            case "blekasOffice":
                coords.add(1);
                coords.add(365);
                coords.add(231);
                return coords;
            case "vlachosOffice":
                coords.add(2);
                coords.add(390);
                coords.add(234);
                return coords;
            case "lykasOffice":
                coords.add(3);
                coords.add(420);
                coords.add(229);
                return coords;
//            case "graduateStudentOfficeA5":
//                coords.add(508);
//                coords.add(212);
//                return coords;
//            case "graduateStudentOfficeA6":
//                coords.add(816);
//                coords.add(212);
//                return coords;
            case "zarrasOffice":
                coords.add(4);
                coords.add(902);
                coords.add(228);
                return coords;
            case "polenakisOffice":
                coords.add(5);
                coords.add(929);
                coords.add(234);
                return coords;
            case "mamoulisOffice":
                coords.add(6);
                coords.add(958);
                coords.add(229);
                return coords;
//            case "graduateStudentOfficeA10":
//                coords.add(1042);
//                coords.add(213);
//                return coords;
//            case "secretariat":
//                coords.add(186);
//                coords.add(156);
//                return coords;
//            case "laboratory":
//                coords.add(278);
//                coords.add(171);
//                return coords;
//            case "telecommunicationsLaboratory":
//                coords.add(803);
//                coords.add(171);
//                return coords;
//            case "edipMembers":
//                coords.add(1140);
//                coords.add(158);
//                return coords;
        }

        return null;

    }
}
