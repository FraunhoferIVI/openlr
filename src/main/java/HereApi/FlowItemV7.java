package HereApi;

import org.jetbrains.annotations.Nullable;

public class FlowItemV7 {

    // TODO add junktionTraversability

    @Nullable
    private String name;
    private String olr;
    @Nullable
    private Double speed;
    @Nullable
    private Double speedUncapped;
    private Double freeFlow;
    private Double jamFactor;
    private Double confidence;
    private String traversability;

    public FlowItemV7(String name, String olr, Double speed, Double speedUncapped, Double freeFlow,
                      Double jamFactor, Double confidence, String traversability)
    {
        this.name = name;
        this.olr = olr;
        this.speed = speed;
        this.speedUncapped = speedUncapped;
        this.freeFlow = freeFlow;
        this.jamFactor = jamFactor;
        this.confidence = confidence;
        this.traversability = traversability;
    }

    @Override
    public String toString() {
        return "FlowItemV7: {" +
                "name: " + name +
                ", olr: " + olr +
                ", speed: " + speed +
                ", speedUncapped: " + speedUncapped +
                ", freeFlow: " + freeFlow +
                ", jamFactor: " + jamFactor +
                ", confidence: " + confidence +
                ", traversability: " + traversability +
                '}';
    }

    @Nullable
    public String getName() { return name; }

    public String getOlr() { return olr; }

    /**
     * expected speed along the roadway; doesn't exceed the legal speed limit.
     */
    @Nullable
    public Double getSpeed() { return speed; }

    /**
     * expected speed along the roadway; may exceed the legal speed limit
     */
    @Nullable
    public Double getSpeedUncapped() { return speedUncapped; }

    /**
     * reference speed along the roadway when no traffic is present.
     */
    public Double getFreeFlow() { return freeFlow; }

    /**
     * value between 0 and 10; the higher the more traffic. 10 stands for road closure.
     */
    public Double getJamFactor() { return jamFactor; }

    /**
     * indicates the proportion of real time data included in the speed calculation.
     * It is a normalized value between 0.0 and 1.0 with the following meaning:
     *
     * 0.7 < confidence <= 1.0 indicates real time speeds
     * 0.5 < confidence <= 0.7 indicates historical speeds
     * 0.0 < confidence <= 0.5 indicates speed limit
     */
    public Double getConfidence() { return confidence; }

    /**
     * The traversability describes whether the roadway is drivable.
     * can be:
     * open - the roadway can be driven
     * closed - the roadway cannot be driven (jamFactor is 10.0)
     * reversibleNotRoutable - the roadway is reversible and currently not routable
     */
    public String getTraversability() { return traversability; }
}
