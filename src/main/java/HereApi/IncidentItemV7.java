package HereApi;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Timestamp;

/**
 * container for information of a single Item from HERE's traffic incident API version 7
 */
public class IncidentItemV7 {

    @NotNull
    private String id;
    @NotNull
    private String originalId;
    @NotNull
    private String olr;
    @Nullable
    private Timestamp startTime;
    @Nullable
    private Timestamp endTime;
    @Nullable
    private Boolean roadClosed;
    @Nullable
    private String description;
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
     */
    public IncidentItemV7(@NotNull String id, @NotNull String originalId, @NotNull String olr,
                          @Nullable Timestamp startTime, @Nullable Timestamp endTime,
                          @Nullable Boolean roadClosed, @Nullable String description,
                          @Nullable String summary, @Nullable JunctionTraversability junctionTraversability,
                          @Nullable Integer posoff, @Nullable Integer negoff)
    {
        this.id = id;
        this.originalId = originalId;
        this.olr = olr;
        this.startTime = startTime;
        this.endTime = endTime;
        this.roadClosed = roadClosed;
        this.description = description;
        this.summary = summary;
        this.junctionTraversability = junctionTraversability;
        this.posoff = posoff;
        this.negoff = negoff;
    }

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

    /**
     * identifier for the latest update (can change over time)
     */
    @NotNull
    public String getId() { return id; }

    /**
     * identifier of the first occurrence of this incident
     */
    @NotNull
    public String getOriginalId() { return originalId; }

    @NotNull
    public String getOlr() { return olr; }

    /**
     * the time from which the incident is valid, before this time the incident should not be considered.
     */
    @Nullable
    public Timestamp getStartTime() { return startTime; }

    /**
     * the time until which the incident is valid, after this time the incident should not be considered.
     */
    @Nullable
    public Timestamp getEndTime() { return endTime; }

    /**
     * weather the incident prevents travel along the roadway
     */
    @Nullable
    public Boolean isRoadClosed() { return roadClosed; }

    /**
     * a longer textual description of the incident, often with location information
     */
    @Nullable
    public String getDescription() { return description; }

    /**
     * a short textual description of the incident without location information
     */
    @Nullable
    public String getSummary() { return summary; }

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
    public JunctionTraversability getJunctionTraversability() { return junctionTraversability; }

    @Nullable
    public Integer getPosOff() { return posoff; }

    @Nullable
    public Integer getNegOff() { return negoff; }
}
