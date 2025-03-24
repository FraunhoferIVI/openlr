package HereApi;
import lombok.Getter;
import lombok.AllArgsConstructor;
/**
 * container for information of a single Item from HERE's traffic flow API version 6 (xml).
 */
@Getter
@AllArgsConstructor
public class FlowItem {

    private String id;
    private String name;
    private double accuracy;
    private double freeFlowSpeed;
    private double jamFactor;
    private double speedLimited;
    private double speed;

    @Override
    public String toString() {
        return String.format("HereApi.FlowItem " +
                "[name = %s, accuracy = %f, freeFlowSpeed = %f," +
                " jamFactor = %f, speedLimited = %f, speed = %f",
                name, accuracy, freeFlowSpeed, jamFactor, speedLimited, speed);

    }
}
