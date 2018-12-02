package aot.cs491.com.aot_ar.aothttpapi;

import com.github.filosganga.geogson.model.Point;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AOTNode {
    String vsn;
    Point location;
    String description;
    String address;
    Date commissionedOn;
    Date decommissionedOn;

    private Set<AOTSensor> sensors;
    private List<AOTObservation> observations;
    private Map<AOTSensorType, AOTObservation> aggregatedObservations = new HashMap<>();

    public AOTNode() {
    }

    public AOTNode(String vsn) {
        setVsn(vsn);
    }

    public static List<AOTNode> fromVSNs(List<String> vsns) {
        List<AOTNode> requiredNodes = null;

        if (vsns != null && vsns.size() > 0) {
            requiredNodes = new ArrayList<>();
            for (String aVsn : vsns) {
                requiredNodes.add(new AOTNode(aVsn));
            }
        }
        return requiredNodes;
    }

    public static List<String> extractVSNs(List<AOTNode> nodes) {
        List<String> requiredNodes = null;

        if (nodes != null && nodes.size() > 0) {
            requiredNodes = new ArrayList<>();
            for (AOTNode aNode : nodes) {
                requiredNodes.add(aNode.vsn);
            }
        }
        return requiredNodes;
    }

    public String getVsn() {
        return vsn;
    }

    public void setVsn(String vsn) {
        this.vsn = vsn;
    }

    public Point getLocation() {
        return location;
    }

    public void setLocation(Point location) {
        this.location = location;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Date getCommissionedOn() {
        return commissionedOn;
    }

    public void setCommissionedOn(Date commissionedOn) {
        this.commissionedOn = commissionedOn;
    }

    public Date getDecommissionedOn() {
        return decommissionedOn;
    }

    public void setDecommissionedOn(Date decommissionedOn) {
        this.decommissionedOn = decommissionedOn;
    }

    public Set<AOTSensor> getSensors() {
        return sensors;
    }

    public void setSensors(Set<AOTSensor> sensors) {
        this.sensors = sensors;
    }

    public List<AOTObservation> getObservations() {
        return observations;
    }

    public void setObservations(List<AOTObservation> observations) {
        this.observations = observations;
    }

    public Map<AOTSensorType, AOTObservation> getAggregatedObservations() {
        return aggregatedObservations;
    }

    public static List<AOTNode> fromVsns(List<String> vsns) {
        List<AOTNode> requiredNodes = null;

        if(vsns != null && vsns.size() > 0) {
            requiredNodes = new ArrayList<>();
            for (String aVsn: vsns) {
                requiredNodes.add(new AOTNode(aVsn));
            }
        }
        return requiredNodes;
    }

    public static List<String> extractVsns(List<AOTNode> nodes) {
        List<String> requiredVSNs = null;

        if(nodes != null && nodes.size() > 0) {
            requiredVSNs = new ArrayList<>();
            for (AOTNode aNode: nodes) {
                requiredVSNs.add(aNode.getVsn());
            }
        }
        return requiredVSNs;
    }

    @Override
    public String toString() {
        return new StringBuilder("[vsn: ")
                .append(getVsn())
                .append(", address: ")
                .append(getAddress())
                .append("]")
                .toString();
    }
}
