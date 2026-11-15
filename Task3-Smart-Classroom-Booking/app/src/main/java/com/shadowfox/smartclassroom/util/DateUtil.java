package com.shadowfox.smartclassroom.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public final class DateUtil {

    private DateUtil() { }

    private static SimpleDateFormat keyFormat() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    }

    public static String todayKey() {
        return keyFormat().format(new Date());
    }

    public static String nowTime() {
        return new SimpleDateFormat("HH:mm", Locale.US).format(new Date());
    }

    /** MaterialDatePicker returns UTC midnight millis. */
    public static String keyFromUtcMillis(long millis) {
        SimpleDateFormat f = keyFormat();
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        return f.format(new Date(millis));
    }

    public static String pretty(String key) {
        try {
            Date d = keyFormat().parse(key);
            return new SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()).format(d);
        } catch (ParseException e) {
            return key;
        }
    }

    /** True when the session on that date has already finished. */
    public static boolean isPast(String date, String endTime) {
        int c = date.compareTo(todayKey());
        if (c < 0) return true;
        if (c > 0) return false;
        return endTime.compareTo(nowTime()) <= 0;
    }

    public static String to12h(String hhmm) {
        try {
            Date d = new SimpleDateFormat("HH:mm", Locale.US).parse(hhmm);
            return new SimpleDateFormat("hh:mm a", Locale.US).format(d);
        } catch (ParseException e) {
            return hhmm;
        }
    }

    public static boolean isValidTime(String t) {
        return t != null && t.matches("([01]\\d|2[0-3]):[0-5]\\d");
    }

    public static String sessionLabel(String start, String end) {
        return to12h(start) + " \u2013 " + to12h(end);
    }
}
