package com.example.reto1appsmoviles.models;

public class PotHole {

    public double latitude;
    public double longitude;
    public String streetAddress;
    public boolean confirmed;

    public PotHole(double latitude, double longitude,String streetAddress,boolean confirmed){
        this.latitude = latitude;
        this.longitude = longitude;
        this.streetAddress =streetAddress;
        this.confirmed=confirmed;
    }
}
