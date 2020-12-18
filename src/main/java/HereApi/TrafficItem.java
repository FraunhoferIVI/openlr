package HereApi;

/**
 * The traffic item object read from the Traffic API requested xml.
 *
 * @author Emily Kast
 */

public class TrafficItem {

    private String id;
    private String type;
    private String status;
    private String start;
    private String end;
    private String criticality;
    private String openLR;
    private String shortDesc;
    private String longDesc;
    private String closure;

    public TrafficItem(String id, String status, String type, String start, String end, String criticality, String openLR, String closure, String shortDesc, String longDesc) {

        this.id = id;
        this.status = status;
        this.type = type;
        this.start = start;
        this.end = end;
        this.criticality = criticality;
        this.openLR = openLR;
        this.closure = closure;
        this.shortDesc = shortDesc;
        this.longDesc = longDesc;
    }

    public TrafficItem() {
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getStatus() {
        return status;
    }

    public String getStart() {
        return start;
    }

    public String getCriticality() {
        return criticality;
    }

    public String getOpenLR() {
        return openLR;
    }

    public String getEnd() {
        return end;
    }

    public String getShortDesc() {
        return shortDesc;
    }

    public String getLongDesc() {
        return longDesc;
    }

    public String getClosure() {
        return closure;
    }


    @Override
    public String toString() {
        return "HereApi.TrafficItem [id = " + id + ", status = " + status + ", type = " + type + ", start = " + start +
                ", end = " + end + ", critcality = " + criticality + ", OpenLR = " + openLR + ", closure = " + closure
                + ", shortDesc = " + shortDesc + ", longDesc = " + longDesc + "]";
    }
}
