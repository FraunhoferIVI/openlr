package HereApi;

/**
 * Contains information needed to describe the line affected by an traffic incident.
 * Information needed are the lineId of the affected line, the incidentId of the traffic
 * incident affecting the line and the negative and positive offsets for the incident.
 *
 * @author Emily Kast
 */

public class AffectedLine {

    private long lineId;
    private String incidentId;
    // positive offset
    private int posOff;
    // negative offset
    private int negOff;

    /**
     * Constructor for lines affected by incidents.
     * @param lineId Id of the affected line
     * @param incidentId Id of the traffic incident
     */
    public AffectedLine(long lineId, String incidentId) {
        this.lineId = lineId;
        this.incidentId = incidentId;
    }

    public long getLineId() {
        return lineId;
    }

    public String getIncidentId() {
        return incidentId;
    }

    public int getPosOff() {
        return posOff;
    }

    public void setPosOff(int posOff) {
        this.posOff = posOff;
    }

    public int getNegOff() {
        return negOff;
    }

    public void setNegOff(int negOff) {
        this.negOff = negOff;
    }
}
