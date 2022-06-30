package HereApi;

/**
 * container for information of a single Item from HERE's traffic flow API version 6 (xml).
 */
public class FlowItem {

    private String id;
    private String name;
    private double accuracy;
    private double freeFlowSpeed;
    private double jamFactor;
    private double speedLimited;
    private double speed;

    public FlowItem(String id, String name, double accuracy, double freeFlowSpeed,
                    double jamFactor, double speedLimited, double speed) {
        this.id = id;
        this.name = name;
        this.accuracy = accuracy;
        this.freeFlowSpeed = freeFlowSpeed;
        this.jamFactor = jamFactor;
        this.speedLimited = speedLimited;
        this.speed = speed;
    }

    public String getId() { return id; }

    public String getName() { return name; }

    public double getAccuracy() { return accuracy; }

    public double getFreeFlowSpeed() { return freeFlowSpeed; }

    public double getJamFactor() { return jamFactor; }

    public double getSpeedLimited() { return speedLimited; }

    public double getSpeed() { return speed; }

    @Override
    public String toString() {
        return String.format("HereApi.FlowItem " +
                "[name = %s, accuracy = %f, freeFlowSpeed = %f," +
                " jamFactor = %f, speedLimited = %f, speed = %f",
                name, accuracy, freeFlowSpeed, jamFactor, speedLimited, speed);

    }
}
