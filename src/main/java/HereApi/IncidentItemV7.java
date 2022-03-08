package HereApi;

import java.time.LocalDateTime;

public class IncidentItemV7 {

    private String id;
    // TODO add originalId, junktionTraversability (enum) and restrictions (Class)
    private String olr;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Boolean roadClosed;
    private String description;
    private String summary;

    public IncidentItemV7(String id, String olr, LocalDateTime startTime, LocalDateTime endTime,
                          Boolean roadClosed, String description, String summary)
    {
        this.id = id;
        this.olr = olr;
        this.startTime = startTime;
        this.endTime = endTime;
        this.roadClosed = roadClosed;
        this.description = description;
        this.summary = summary;
    }

    @Override
    public String toString() {
        return "IncidentV7: {" +
                "id: " + id +
                ", olr: " + olr +
                ", startTime: " + startTime +
                ", endTime: " + endTime +
                ", roadClosed: " + roadClosed +
                ", description: " + description +
                ", summary: " + summary +
                '}';
    }

    // TODO java doc für alle getter
    public String getId() { return id; }

    public String getOlr() { return olr; }

    public LocalDateTime getStartTime() { return startTime; }

    public LocalDateTime getEndTime() { return endTime; }

    public Boolean isRoadClosed() { return roadClosed; }

    public String getDescription() { return description; }

    public String getSummary() { return summary; }
}
