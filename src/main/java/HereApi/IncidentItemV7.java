package HereApi;

import org.jetbrains.annotations.Nullable;

import java.sql.Timestamp;

public class IncidentItemV7 {

    private String id;
    private String originalId;
    private String olr;
    private Timestamp startTime;
    @Nullable
    private Timestamp endTime;
    private Boolean roadClosed;
    private String description;
    private String summary;
    @Nullable
    private JunctionTraversability junctionTraversability;

    public IncidentItemV7(String id, String originalId, String olr, Timestamp startTime, Timestamp endTime,
                          Boolean roadClosed, String description, String summary,
                          JunctionTraversability junctionTraversability)
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
                '}';
    }

    /**
     * identifier for this incident message (can change over time)
     */
    public String getId() { return id; }

    /**
     * identifier of the first message for this incident
     */
    public String getOriginalId() { return originalId; }

    public String getOlr() { return olr; }

    /**
     * the time from which the incident is valid, before this time the incident should not be considered.
     */
    public Timestamp getStartTime() { return startTime; }

    /**
     * the time until which the incident is valid, after this time the incident should not be considered.
     */
    @Nullable
    public Timestamp getEndTime() { return endTime; }

    /**
     * weather the incident prevents travel along the roadway
     */
    public Boolean isRoadClosed() { return roadClosed; }

    /**
     * a longer textual description of the incident, often with location information
     */
    public String getDescription() { return description; }

    /**
     * a short textual description of the incident without location information
     */
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
}
