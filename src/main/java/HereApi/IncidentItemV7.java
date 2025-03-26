package HereApi;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Getter;
/**
 * container for information of a single Item from HERE's traffic incident API version 7
 */
@Getter
@AllArgsConstructor;
public class IncidentItemV7 {

   
      /**
     * identifier for the latest update (can change over time)
     */
     @NotNull
    private String id;
      /**
     * identifier of the first occurrence of this incident
     */
    @NotNull
    private String originalId;
    /**
     * the time from which the incident is valid, before this time the incident should not be considered.
     */
    @NotNull
    private String olr;
    /**
     * the time until which the incident is valid, after this time the incident should not be considered.
     */
    @Nullable
    private Timestamp startTime;
     /**
     * weather the incident prevents travel along the roadway
     */
    @Nullable
    private Timestamp endTime;
        /**
     * a longer textual description of the incident, often with location information
     */
    @Nullable
    private Boolean roadClosed;
       /**
     * a short textual description of the incident without location information
     */
    @Nullable
    private String description;
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
    private String summary;
    @Nullable
    private JunctionTraversability junctionTraversability;
    @Nullable
    private Integer posoff;
    @Nullable
    private Integer negoff;

    /**
     * @param id identifier for the latest update (can change over time)
     * @param originalId identifier of the first occurrence for this incident
     * @param olr Open Location Reference
     * @param startTime the time from which this incident has to be considered
     * @param endTime the time until which this incident has to be considered
     * @param roadClosed weather the incident prevents travel along the roadway
     * @param description a longer textual description of the incident, often with location information
     * @param summary a short textual description of the incident without location information
     * @param junctionTraversability Used for road closures to indicate if the junctions along the closure can be crossed.
     * @param posoff needed for visualization in QGIS
     * @param negoff needed for visualization in QGIS
   

    @Override
    public String toString()
    {
        return "IncidentV7: {" +
                "id: " + id +
                ", olr: " + olr +
                ", startTime: " + startTime +
                ", endTime: " + endTime +
                ", roadClosed: " + roadClosed +
                ", junction traversability: " + junctionTraversability +
                ", description: " + description +
                ", summary: " + summary +
                ", posoff: " + posoff +
                ", negoff: " + negoff +
                '}';
    }

}
