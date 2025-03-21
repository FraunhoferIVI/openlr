package HereApi;
import lombok.Getter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
/**
 * The traffic item object read from the Traffic API requested xml.
 *
 * @author Emily Kast
 */
@Getter        
@AllArgsConstructor 
@NoArgsConstructor   
public class TrafficItem {

    private String id;
    private String status;
    private String type;
    private String start;
    private String end;
    private String criticality;
    private String openLR;
    private String closure;
    private String shortDesc;
    private String longDesc;
   
    @Override
    public String toString() {
        return "HereApi.TrafficItem [id = " + id + ", status = " + status + ", type = " + type + ", start = " + start +
                ", end = " + end + ", critcality = " + criticality + ", OpenLR = " + openLR + ", closure = " + closure
                + ", shortDesc = " + shortDesc + ", longDesc = " + longDesc + "]";
    }
}
