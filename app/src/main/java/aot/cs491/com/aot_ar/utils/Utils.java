package aot.cs491.com.aot_ar.utils;

import android.util.Log;

import aot.cs491.com.aot_ar.aothttpapi.AOTService;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Utils {
    private static final String TAG = Utils.class.getSimpleName();

    private Utils() {
    }

    // ======== DATE MANIPULATION =============

    public static String buildDateString(int year, int month, int day) {
        return buildDateString(year, month, day, 0, 0, 0);
    }

    public static String buildDateString(int year, int month, int day, int hours, int minutes, int seconds) {
        return new StringBuilder()
                .append(year)
                .append('-')
                .append(month)
                .append('-')
                .append(day)
                .append('T')
                .append(hours)
                .append(':')
                .append(minutes)
                .append(':')
                .append(seconds)
                .toString();
    }

    public static Date stringToLocalDate(int year, int month, int day) {
        return stringToLocalDate(buildDateString(year, month, day));
    }

    public static Date stringToLocalDate(int year, int month, int day, int hour, int minutes, int seconds) {
        return stringToLocalDate(buildDateString(year, month, day, hour, minutes, seconds));
    }

    public static Date stringToLocalDate(String dateString) {
        return stringToDate(dateString, AOTService.DATE_FORMAT, TimeZone.getDefault());
    }

    public static String dateToLocalString(Date date) {
        return dateToString(date, AOTService.DATE_FORMAT, TimeZone.getDefault());
    }

    public static Date stringToServerDate(int year, int month, int day) {
        return stringToServerDate(buildDateString(year, month, day));
    }

    public static Date stringToServerDate(int year, int month, int day, int hour, int minutes, int seconds) {
        return stringToServerDate(buildDateString(year, month, day, hour, minutes, seconds));
    }

    public static Date stringToServerDate(String dateString) {
        return stringToDate(dateString, AOTService.DATE_FORMAT, TimeZone.getTimeZone(AOTService.TIME_ZONE));
    }

    public static String dateToServerString(Date date) {
        return dateToString(date, AOTService.DATE_FORMAT, TimeZone.getTimeZone(AOTService.TIME_ZONE));
    }

    public static Date stringToDate(int year, int month, int day, String dateFormat, TimeZone timeZone) {
        return stringToDate(buildDateString(year, month, day), dateFormat, timeZone);
    }

    public static Date stringToDate(String dateString, String dateFormat, TimeZone timeZone) {
        SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);
        formatter.setTimeZone(timeZone);

        try {
            return formatter.parse(dateString);
        } catch (ParseException e) {
            Log.e(TAG, "Unable to parse dateString" + dateString, e);
            return null;
        }
    }

    public static String dateToString(Date date, String dateFormat, TimeZone timeZone) {
        SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);
        formatter.setTimeZone(timeZone);

        return formatter.format(date);
    }

    public static Date stripTimeFromLocalDate(Date date) {
        return stripTimeFromDate(date, TimeZone.getDefault());
    }

    public static Date stripTimeFromServerDate(Date date) {
        return stripTimeFromDate(date, TimeZone.getTimeZone(AOTService.TIME_ZONE));
    }

    public static Date stripTimeFromDate(Date date, TimeZone timeZone) {
        return setTimeForDate(0, 0, 0, date, timeZone);
    }

    public static Date setLocalTimeToEndOfDay(Date date) {
        return setTimeToEndOfDay(date, TimeZone.getDefault());
    }

    public static Date setServerTimeToEndOfDay(Date date) {
        return setTimeToEndOfDay(date, TimeZone.getTimeZone(AOTService.TIME_ZONE));
    }

    public static Date setTimeToEndOfDay(Date date, TimeZone timeZone) {
        return setTimeForDate(23, 59, 59, date, timeZone);
    }

    public static Date setTimeForLocalDate(int hours, int minutes, int seconds, Date date) {
        return setTimeForDate(hours, minutes, seconds, date, TimeZone.getDefault());
    }

    public static Date setTimeForServerDate(int hours, int minutes, int seconds, Date date) {
        return setTimeForDate(hours, minutes, seconds, date, TimeZone.getTimeZone(AOTService.TIME_ZONE));
    }

    public static Date setTimeForDate(int hours, int minutes, int seconds, Date date, TimeZone timeZone) {
        Calendar calendar = Calendar.getInstance(timeZone);
        calendar.setTime(date);
        calendar.set(Calendar.HOUR, hours);
        calendar.set(Calendar.MINUTE, minutes);
        calendar.set(Calendar.SECOND, seconds);

        return calendar.getTime();
    }

    // ======== AGGREGATION =============

    public static <T> Float sumItems(Iterable<T> items, Function<T, Float> key) {
        Float sum = Float.NaN;

        for (T anItem : items) {
            if (sum.isNaN()) {
                sum = 0f;
            }
            sum += key.apply(anItem);
        }
        return sum;
    }

    public static <T> Float meanOfItems(Collection<T> items, Function<T, Float> key) {
        return sumItems(items, key) / items.size();
    }

    public static <T> Float medianOfItems(List<T> items, Function<T, Float> key) {
        int itemsSize = items.size();

        if(itemsSize == 0) {
            return Float.NaN;
        }
        else if(itemsSize % 2 == 0) {
            int mid = itemsSize / 2;
            return (key.apply(items.get(mid)) + key.apply(items.get(mid + 1))) / 2f;
        }
        else {
            return key.apply(items.get(itemsSize / 2));
        }
    }

    public static <T> Float modeOfItems(Iterable<T> items, Function<T, Float> key) {
        HashMap<Float, Integer> frequencies = new HashMap<>();

        Integer maxValueFrequency = 0;
        Float maxValue = Float.NaN;

        for(T anItem: items) {
            Float value = key.apply(anItem);
            Integer valueFrequency = frequencies.getOrDefault(value, 0) + 1;

            frequencies.put(value, valueFrequency);

            if(maxValue.isNaN() || valueFrequency > maxValueFrequency) {
                maxValue = value;
                maxValueFrequency = valueFrequency;
            }
        }
        return maxValue;
    }

    public static <T> Float minOfItems(Iterable<T> items, Function<T, Float> key) {
        Float min = Float.NaN;

        for (T anItem : items) {
            Float itemValue = key.apply(anItem);

            if (min.isNaN()) {
                min = itemValue;
            } else if (itemValue < min) {
                min = itemValue;
            }
        }
        return min;
    }

    public static <T> Float maxOfItems(Iterable<T> items, Function<T, Float> key) {
        Float max = Float.NaN;

        for (T anItem : items) {
            Float itemValue = key.apply(anItem);

            if (max.isNaN()) {
                max = itemValue;
            } else if (itemValue > max) {
                max = itemValue;
            }
        }
        return max;
    }

    // ======== UNIT CONVERSION =============

    public static Float celsiusToFahrenheit(float celsiusValue) {
        return celsiusValue * 1.8f + 32;
    }

    public static Float hectoPascalToInchesOfMercury(Float hPaValue) {
        return hPaValue * 0.02953f;
    }

    // ======== OTHERS =============

    public static Float round(Float number) {
        return number.isNaN() || number.isInfinite() ? number : Math.round(number * 100)/100f;
    }
}
