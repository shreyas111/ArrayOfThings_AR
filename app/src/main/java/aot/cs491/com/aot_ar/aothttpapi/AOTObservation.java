package aot.cs491.com.aot_ar.aothttpapi;

import java.util.Date;

public class AOTObservation {
    String nodeVsn;
    String sensorPath;
    Date timestamp;
    Float value;

    public String getNodeVsn() {
        return nodeVsn;
    }

    public void setNodeVsn(String nodeVsn) {
        this.nodeVsn = nodeVsn;
    }

    public String getSensorPath() {
        return sensorPath;
    }

    public void setSensorPath(String sensorPath) {
        this.sensorPath = sensorPath;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public Float getValue() {
        return value;
    }

    public void setValue(Float value) {
        this.value = value;
    }
}
