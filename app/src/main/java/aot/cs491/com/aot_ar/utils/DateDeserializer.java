package aot.cs491.com.aot_ar.utils;

import android.util.Log;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class DateDeserializer implements JsonDeserializer<Date> {

    private static final String TAG = DateDeserializer.class.getSimpleName();

    private String dateFormat;
    private TimeZone timeZone;

    public DateDeserializer() {
        this("yyyy-MM-dd'T'HH:mm:dd'Z'", TimeZone.getDefault());
    }

    public DateDeserializer(String dateFormat, TimeZone timeZone) {
        this.dateFormat = dateFormat;
        this.timeZone = timeZone;
    }

    @Override
    public Date deserialize(JsonElement element, Type arg1, JsonDeserializationContext arg2) throws JsonParseException {
        String date = element.getAsString();

        SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);
        formatter.setTimeZone(timeZone);;

        try {
            return formatter.parse(date);
        } catch (ParseException e) {
            Log.e(TAG, "Failed to parse Date due to:", e);
            throw new JsonParseException("Invalid date format", e);
        }
    }
}
