package aot.cs491.com.aot_ar.utils;

import android.util.Log;

import aot.cs491.com.aot_ar.aothttpapi.AOTService;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
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
        return new StringBuilder(String.valueOf(year))
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

    public static Date stringToLocalDate(String dateString) {
        return stringToDate(dateString, AOTService.DATE_FORMAT, TimeZone.getDefault());
    }

    public static String dateToLocalString(Date date) {
        return dateToString(date, AOTService.DATE_FORMAT, TimeZone.getDefault());
    }

    public static Date stringToServerDate(int year, int month, int day) {
        return stringToServerDate(buildDateString(year, month, day));
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
        return setHoursForDate(0, date, timeZone);
    }

    public static Date setHoursForLocalDate(int hour, Date date) {
        return setHoursForDate(hour, date, TimeZone.getDefault());
    }

    public static Date setHoursForServerDate(int hour, Date date) {
        return setHoursForDate(hour, date, TimeZone.getTimeZone(AOTService.TIME_ZONE));
    }

    public static Date setHoursForDate(int hour, Date date, TimeZone timeZone) {
        Calendar calendar = Calendar.getInstance(timeZone);
        calendar.setTime(date);
        calendar.set(Calendar.HOUR, hour);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

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

    public <T> CompletableFuture<List<T>> allOf(List<CompletableFuture<T>> futuresList) {
        CompletableFuture<Void> allFuturesResult =
                CompletableFuture.allOf(futuresList.toArray(new CompletableFuture[futuresList.size()]));
        return allFuturesResult.thenApply(v ->
                futuresList.stream().
                        map(future -> future.join()).
                        collect(Collectors.<T>toList())
        );
    }
}
