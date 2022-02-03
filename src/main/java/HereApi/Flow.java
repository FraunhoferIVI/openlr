package HereApi;

public class Flow {

    private String roadName;
    private double accuracy;
    private double freeFlowSpeed;
    private double jamFactor;
    private double speedLimited;
    private double speed;

    public Flow(String roadName, double accuracy, double freeFlowSpeed, double jamFactor, double speedLimited,
                double speed) {
        this.roadName = roadName;
        this.accuracy = accuracy;
        this.freeFlowSpeed = freeFlowSpeed;
        this.jamFactor = jamFactor;
        this.speedLimited = speedLimited;
        this.speed = speed;
    }

    public String getName() {
        return roadName;
    }

    public double getAccuracy() {
        return accuracy;
    }

    public double getFreeFlowSpeed() {
        return freeFlowSpeed;
    }

    public double getJamFactor() {
        return jamFactor;
    }

    public double getSpeedLimited() {
        return speedLimited;
    }

    public double getSpeed() {
        return speed;
    }

}
