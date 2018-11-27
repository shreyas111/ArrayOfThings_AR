package aot.cs491.com.aot_ar.aothttpapi;

public enum AOTSensorType {

    // Weather (Meteorology)
    TEMPERATURE("metsense.pr103j2.temperature"),
    HUMIDITY("metsense.hih4030.humidity"),
    PRESSURE("metsense.bmp180.pressure"),

    // Cloud cover
    LIGHT_INTENSITY("intensity"),
    INFRA_RED_LIGHT("infrared"),
    ULTRA_VIOLET_LIGHT("ultra-violet"),

    // Air Quality
    CARBON_MONOXIDE("chemsense.co.concentration"),
    SULPHUR_DIOXIDE("chemsense.so2.concentration"),
    NITROGEN_DIOXIDE("chemsense.no2.concentration")
    ;

    private final String sensorPath;

    AOTSensorType(final String sensorPath) {
        this.sensorPath = sensorPath;
    }

    @Override
    public String toString() {
        return sensorPath;
    }
}
