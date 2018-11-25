package aot.cs491.com.aot_ar.aothttpapi;

import java.util.ArrayList;
import java.util.List;

public class AOTSensor {
    String path;
    String subsystem;
    String sensor;
    String parameter;
    String ontology;
    String uom;
    Float min;
    Float max;
    String dataSheet;

    private List<AOTObservation> observations;

    public AOTSensor() {}

    public AOTSensor(String path) {
        setPath(path);
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getSubsystem() {
        return subsystem;
    }

    public void setSubsystem(String subsystem) {
        this.subsystem = subsystem;
    }

    public String getSensor() {
        return sensor;
    }

    public void setSensor(String sensor) {
        this.sensor = sensor;
    }

    public String getParameter() {
        return parameter;
    }

    public void setParameter(String parameter) {
        this.parameter = parameter;
    }

    public String getOntology() {
        return ontology;
    }

    public void setOntology(String ontology) {
        this.ontology = ontology;
    }

    public String getUom() {
        return uom;
    }

    public void setUom(String uom) {
        this.uom = uom;
    }

    public Float getMin() {
        return min;
    }

    public void setMin(Float min) {
        this.min = min;
    }

    public Float getMax() {
        return max;
    }

    public void setMax(Float max) {
        this.max = max;
    }

    public String getDataSheet() {
        return dataSheet;
    }

    public void setDataSheet(String dataSheet) {
        this.dataSheet = dataSheet;
    }

    public List<AOTObservation> getObservations() {
        return observations;
    }

    public void setObservations(List<AOTObservation> observations) {
        this.observations = observations;
    }

    public static List<AOTSensor> fromPaths(List<String> paths) {
        List<AOTSensor> requiredSensors = null;

        if(paths != null && paths.size() > 0) {
            requiredSensors = new ArrayList<>();
            for (String aPath: paths) {
                requiredSensors.add(new AOTSensor(aPath));
            }
        }
        return requiredSensors;
    }

    public static List<String> extractPaths(List<AOTSensor> sensors) {
        List<String> requiredSensors = null;

        if(sensors != null && sensors.size() > 0) {
            requiredSensors = new ArrayList<>();
            for (AOTSensor aSensor: sensors) {
                requiredSensors.add(aSensor.path);
            }
        }
        return requiredSensors;
    }

    @Override
    public int hashCode() {
        return path == null ? super.hashCode() : path.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof AOTSensor && path != null ? path.equals(((AOTSensor) obj).path) : super.equals(obj);
    }
}
