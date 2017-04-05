package com.graphhopper.android;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by martinwurflein on 29.03.17.
 */

/**
 * Class representing a single ParkingSlot
 * TODO -> direct connection, to a kind of "Paying Class"
 */
public class ParkingSlot {
    private static List<ParkingSlot> parkingSpace=new LinkedList<>();

    public static List<ParkingSlot> getParkingSpace(){

        return parkingSpace;
    }

    static{
        List<String> CSV=new LinkedList<>();
        CSV.add("number,lat,lon,type,occupied,occupiedBy,occupiedFrom,beacon");
        CSV.add("1,49.902549,10.870506,1,true,2345,12350,xx");
        CSV.add("2,49.902652,10.870646,1,true,231,12557,xx");
        CSV.add("3,49.902817,10.870847,1,false,491,12400,0x6e78426f6333");
        CSV.add("4,49.902908,10.870969,2,false,0,0,xx");
        CSV.add("5,49.902961,10.871041,2,false,0,0,xx");
        CSV.add("6,49.90304,10.871074,1,true,0,0,xx");
        CSV.add("7,49.903186,10.870797,3,false,0,0,xx");
        CSV.add("8,49.9033,10.870583,1,true,0,0,xx");
        CSV.add("9,49.9034,10.8704,1,true,0,0,xx");
        CSV.add("10,49.903531,10.87018,1,true,0,0,xx");
        CSV.add("11,49.903604,10.869998,1,true,0,0,xx");
        CSV.add("12,49.903545,10.869826,1,false,0,0,xx");
        CSV.add("13,49.903476,10.869735,1,true,0,0,xx");
        CSV.add("14,49.903397,10.869592,1,true,0,0,xx");
        CSV.add("15,49.903349,10.869508,1,true,0,0,xx");
        CSV.add("16,49.903264,10.869399,1,true,0,0,xx");
        CSV.add("17,49.903154,10.869218,1,true,0,0,XA");

        for(int i=0;i<CSV.size();i++)
        {
            if(i>0)
                parkingSpace.add(csvToParkingSlot(CSV.get(i)));
        }
    }

    public ParkingSlot(int number, double lat, double lon, int type, boolean occupied, String occupiedBy, long occupiedFrom, String beacon) {
        this.number = number;
        this.lat = lat;
        this.lon = lon;
        this.type = type;
        this.occupied = occupied;
        this.occupiedBy = occupiedBy;
        this.occupiedFrom = occupiedFrom;
        this.beacon = beacon;
    }

    public static ParkingSlot csvToParkingSlot(String csv){
        String fields[]=csv.split(",");
        return new ParkingSlot(Integer.parseInt(fields[0]),Double.parseDouble(fields[1]),Double.parseDouble(fields[2]),Integer.parseInt(fields[3]),Boolean.parseBoolean(fields[4]),fields[5],Long.parseLong(fields[6]),fields[7]);

    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public boolean isOccupied() {
        return occupied;
    }

    public void setOccupied(boolean occupied) {
        this.occupied = occupied;
    }

    public String getOccupiedBy() {
        return occupiedBy;
    }

    public void setOccupiedBy(String occupiedBy) {
        this.occupiedBy = occupiedBy;
    }

    public long getOccupiedFrom() {
        return occupiedFrom;
    }

    public void setOccupiedFrom(long occupiedFrom) {
        this.occupiedFrom = occupiedFrom;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    private int number;
    private double lat;
    private double lon;
    private int type;
    private boolean occupied;
    private String occupiedBy;
    private long occupiedFrom;

    public String getBeacon() {
        return beacon;
    }

    public void setBeacon(String beacon) {
        this.beacon = beacon;
    }

    private String beacon;
}
