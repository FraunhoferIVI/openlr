package HereApi;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import lombok.AllArgsConstructor;
import lombok.Getter;
/**
 * container for information of a single Item from HERE's traffic flow API version 7 (json).
 */
@AllArgsConstructor
@Getter
public class FlowItemV7 {

    @Nullable
    private String name;
    /**
     * Open Location Reference of this Item
     */
    @NotNull
    private String olr;
      /**
     * expected speed along the roadway; doesn't exceed the legal speed limit.
     * presumably given in meter/second
     */
    @Nullable
    private Double speed;
    /**
     * expected speed along the roadway; may exceed the legal speed limit.
     * presumably given in meter/second
     */
    @Nullable
    private Double speedUncapped;
     /**
     * reference speed along the roadway when no traffic is present.
     * presumably given in meter/second
     */
    @Nullable
    private Double freeFlow;
      /**
     * value between 0 and 10; the higher, the more traffic. 10 stands for road closure.
     */
    @Nullable
    private Double jamFactor;
      /**
     * indicates the proportion of real time data included in the speed calculation.
     * It is a normalized value between 0.0 and 1.0 with the following meaning:
     *
     * 0.7 < confidence <= 1.0 indicates real time speeds
     * 0.5 < confidence <= 0.7 indicates historical speeds
     * 0.0 < confidence <= 0.5 indicates speed limit
     */
    @Nullable
    private Double confidence;
    /**
     * The traversability describes whether the roadway is drivable.
     * can be:
     * open - the roadway can be driven
     * closed - the roadway cannot be driven (jamFactor is 10.0)
     * reversibleNotRoutable - the roadway is reversible and currently not routable
     */
    @Nullable
    private String traversability;
    /**
     * Used for road closures to indicate if the junctions along the closure can be crossed.
     * can be:
     * ALL_OPEN
     * ALL_CLOSED
     * INTERMEDIATE_CLOSED_EDGE_OPEN
     * START_OPEN_OTHERS_CLOSED
     * END_OPEN_OTHERS_CLOSED
     */
    @Nullable
    private JunctionTraversability junctionTraversability;
    @Nullable
    private Integer posoff;
    @Nullable
    private Integer negoff;
    
    /**
     * @param name street-name
     * @param olr Open Location Reference
     * @param speed expected speed along the roadway; doesn't exceed the legal speed limit.
     * @param speedUncapped expected speed along the roadway; may exceed the legal speed limit.
     * @param freeFlow reference speed along the roadway when no traffic is present.
     * @param jamFactor value between 0 and 10; the higher, the more traffic. 10 stands for road closure.
     * @param confidence indicates the proportion of real time data included in the speed calculation.
     * It is a normalized value between 0.0 and 1.0 with the following meaning:
     * 0.7 < confidence <= 1.0 indicates real time speeds
     * 0.5 < confidence <= 0.7 indicates historical speeds
     * 0.0 < confidence <= 0.5 indicates speed limit
     * @param traversability The traversability describes whether the roadway is drivable.
     * can be:
     * open - the roadway can be driven
     * closed - the roadway cannot be driven (jamFactor is 10.0)
     * reversibleNotRoutable - the roadway is reversible and currently not routable
     * @param junctionTraversability Used for road closures to indicate if the junctions along the closure can be crossed.
     * can be:
     * ALL_OPEN
     * ALL_CLOSED
     * INTERMEDIATE_CLOSED_EDGE_OPEN
     * START_OPEN_OTHERS_CLOSED
     * END_OPEN_OTHERS_CLOSED
     * @param posoff needed for visualization in QGIS
     * @param negoff needed for visualization in QGIS
     */

    public boolean isInvalid() {
        return speed == null || speedUncapped == null || freeFlow == null ||
               jamFactor == null || confidence == null || posOff == null || negOff == null;
    }

    @Override
    public String toString() {
        return "FlowItemV7: {" +
                "name: " + name +
                ", olr: " + olr +
                ", speed: " + speed +
                ", speed uncapped: " + speedUncapped +
                ", free flow speed: " + freeFlow +
                ", jam factor: " + jamFactor +
                ", confidence: " + confidence +
                ", traversability: " + traversability +
                ", junction traversability: " + junctionTraversability +
                ", posoff: " + posoff +
                ", negoff: " + negoff +
                '}';
    }
}
