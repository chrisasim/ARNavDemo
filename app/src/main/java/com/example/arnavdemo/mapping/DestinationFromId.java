package com.example.arnavdemo.mapping;

import java.util.ArrayList;

public class DestinationFromId {

    private String id;

    public DestinationFromId(String id) {
        this.id = id;
    }

    public ArrayList<Integer> getCoordinates() {

        ArrayList<Integer> coords = new ArrayList<>();

        switch (id) {
            case "A.1":
                coords.add(181);
                coords.add(227);
                coords.add(281);
                coords.add(225);
                coords.add(282);
                coords.add(341);
                coords.add(182);
                coords.add(340);
                return coords;
            case "A.2":
                coords.add(294);
                coords.add(226);
                coords.add(347);
                coords.add(226);
                coords.add(343);
                coords.add(337);
                coords.add(299);
                coords.add(335);
                return coords;
            case "A.3":
                coords.add(362);
                coords.add(249);
                coords.add(418);
                coords.add(249);
                coords.add(413);
                coords.add(338);
                coords.add(364);
                coords.add(338);
                return coords;
            case "A.4":
                coords.add(431);
                coords.add(227);
                coords.add(485);
                coords.add(225);
                coords.add(483);
                coords.add(337);
                coords.add(435);
                coords.add(336);
                return coords;
            case "A.5":
                coords.add(503);
                coords.add(229);
                coords.add(605);
                coords.add(226);
                coords.add(601);
                coords.add(338);
                coords.add(509);
                coords.add(333);
                return coords;
            case "A.6":
                coords.add(719);
                coords.add(228);
                coords.add(814);
                coords.add(226);
                coords.add(810);
                coords.add(333);
                coords.add(723);
                coords.add(333);
                return coords;
            case "A.7":
                coords.add(835);
                coords.add(225);
                coords.add(887);
                coords.add(225);
                coords.add(878);
                coords.add(342);
                coords.add(838);
                coords.add(336);
                return coords;
            case "A.8":
                coords.add(903);
                coords.add(246);
                coords.add(950);
                coords.add(247);
                coords.add(951);
                coords.add(338);
                coords.add(902);
                coords.add(338);
                return coords;
            case "A.9":
                coords.add(967);
                coords.add(224);
                coords.add(1022);
                coords.add(223);
                coords.add(1021);
                coords.add(337);
                coords.add(969);
                coords.add(336);
                return coords;
            case "A.10":
                coords.add(1042);
                coords.add(224);
                coords.add(1136);
                coords.add(224);
                coords.add(1136);
                coords.add(335);
                coords.add(1041);
                coords.add(331);
                return coords;
            case "A.11":
                coords.add(180);
                coords.add(18);
                coords.add(281);
                coords.add(19);
                coords.add(272);
                coords.add(130);
                coords.add(186);
                coords.add(138);
                return coords;
            case "A.12":
                coords.add(311);
                coords.add(28);
                coords.add(575);
                coords.add(38);
                coords.add(561);
                coords.add(150);
                coords.add(299);
                coords.add(157);
                return coords;
            case "A.13":
                coords.add(732);
                coords.add(24);
                coords.add(1038);
                coords.add(30);
                coords.add(1018);
                coords.add(159);
                coords.add(806);
                coords.add(144);
                return coords;
            case "A.14":
                coords.add(1065);
                coords.add(25);
                coords.add(1138);
                coords.add(24);
                coords.add(1137);
                coords.add(140);
                coords.add(1065);
                coords.add(143);
                return coords;
            default:
                return null;
        }

    }
}
