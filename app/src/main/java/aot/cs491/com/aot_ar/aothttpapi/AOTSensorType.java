package aot.cs491.com.aot_ar.aothttpapi;

public enum AOTSensorType {

    // Weather (Meteorology)
    TEMPERATURE("metsense.pr103j2.temperature"),
    PRESSURE("metsense.bmp180.pressure"),
    HUMIDITY("metsense.hih4030.humidity"),


    // Cloud cover
//    LIGHT_INTENSITY("chemsense.si1145.visible_light_intensity"),
//    INFRA_RED_LIGHT("chemsense.si1145.ir_intensity"),
//    ULTRA_VIOLET_LIGHT("chemsense.si1145.uv_intensity"),
    LIGHT_INTENSITY("lightsense.apds_9006_020.intensity"),
    INFRA_RED_LIGHT("lightsense.tsl260rd.intensity"),
    ULTRA_VIOLET_LIGHT("lightsense.ml8511.intensity"),

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

    public static AOTSensorType fromSensorPath(String sensorPath) throws IllegalArgumentException {
        for(AOTSensorType sensorType : AOTSensorType.values()) {
            if(sensorType.sensorPath.equalsIgnoreCase(sensorPath)) {
                return sensorType;
            }
        }
        throw new IllegalArgumentException("Unrecognized sensor path: " +sensorPath);
    }

    public String getUnit(boolean imperial) {
        switch (this) {

            case TEMPERATURE:
                return imperial ? "°F" : "°C";

            case HUMIDITY:
                return "%";

            case PRESSURE:
                return imperial ? "hPa" : "inHg";

            case LIGHT_INTENSITY:
                return "lux";
            case INFRA_RED_LIGHT:
            case ULTRA_VIOLET_LIGHT:
                return "μW/cm²";

            case CARBON_MONOXIDE:
            case SULPHUR_DIOXIDE:
            case NITROGEN_DIOXIDE:
                return "ppm";
        }

        return "";
    }
}
