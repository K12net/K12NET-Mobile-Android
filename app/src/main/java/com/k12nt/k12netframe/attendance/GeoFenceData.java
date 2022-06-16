package com.k12nt.k12netframe.attendance;

import androidx.annotation.Keep;

@Keep
public class GeoFenceData {
    int $id;
    String $type;
    double Latitude;
    double Longitude;
    float RadiusInMeter;
    int LocationIX;
    String LocationSummary;
    String Portal;

    public String GetRequestID() {
        String requestID = String.format("%1$f$$$%2$f$$$%3$f$$$%4$d$$$%5$s$$$%6$s",Latitude, Longitude, RadiusInMeter,LocationIX,Portal,LocationSummary);

        if(requestID.length() > 99) {
            requestID = requestID.substring(0,96) + "..";
        }

        return  requestID;
    }
}
