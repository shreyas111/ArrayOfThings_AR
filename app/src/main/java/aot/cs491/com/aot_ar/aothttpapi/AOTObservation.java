package aot.cs491.com.aot_ar.aothttpapi;

import java.util.Date;

import aot.cs491.com.aot_ar.utils.Utils;

public class AOTObservation {
    String nodeVsn;
    String sensorPath;
    Date timestamp;
    Float value;
    
    private AOTSensorType sensorType;

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
        return getValue(false);
    }

    public Float getValue(boolean useImperialUnits) {
        Float returnValue;

        switch (getSensorType()) {
            case TEMPERATURE:
                returnValue = useImperialUnits ? Utils.celsiusToFahrenheit(value) : value;
                break;

            case PRESSURE:
                returnValue = useImperialUnits ? Utils.hectoPascalToInchesOfMercury(value) : value;
                break;

            default:
                returnValue = value;
        }
        return Utils.round(returnValue);
    }

    public void setValue(Float value) {
        this.value = value;
    }

    public String getUnits() {
        return getUnits(false);
    }

    public String getUnits(boolean useImperialUnits) {
        return getSensorType() == null ? "" : getSensorType().getUnit(useImperialUnits);
    }

    public AOTSensorType getSensorType() {
        String sensorPath = getSensorPath();
        
        if(sensorPath == null) {
            sensorType = null;
        }
        else if(sensorType == null || !sensorType.toString().equals(sensorPath)) {
            sensorType = AOTSensorType.fromSensorPath(sensorPath);
        }
        
        return sensorType;
    }
}
