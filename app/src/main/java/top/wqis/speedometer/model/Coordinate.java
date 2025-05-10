package top.wqis.speedometer.model;

public class Coordinate {
    private final double lat;
    private final double lng;
    private final double speed;

    public Coordinate(double lt, double lg, double spd){
        lat = lt;
        lng = lg;
        speed = spd;
    }

    public double getLat(){
        return lat;
    }

    public double getLng(){
        return lng;
    }

    public double getSpeed(){
        return speed;
    }
}
