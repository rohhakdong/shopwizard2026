package com.shopwizard.framework.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateTimeUtil {

    public static final String LastDate = "2999-12-31";

    public static String getDateTimeByPattern(String pattern) {
        SimpleDateFormat formatter = new SimpleDateFormat(pattern, Locale.KOREA);
        return formatter.format(new Date());
    }

    public static String getDateTimeByPattern(String pattern, int offset) {
        SimpleDateFormat formatter = new SimpleDateFormat(pattern, Locale.KOREA);
        Calendar today = Calendar.getInstance();
        today.add(Calendar.DATE, offset);
        return formatter.format(today.getTime());
    }

    public static String getParseDateString(String dateTime, String pattern) {
        if (dateTime != null) {
            String year  = dateTime.substring(0, 4);
            String month = dateTime.substring(4, 6);
            String day   = dateTime.substring(6, 8);
            return year + pattern + month + pattern + day;
        }
        return "";
    }

    public static String getDateString(int year, int month, int day, String pattern) {
        Calendar cal = Calendar.getInstance();
        if (year != 0 && month != 0 && day != 0) cal.set(year, month - 1, day);
        SimpleDateFormat formatter = new SimpleDateFormat(pattern, Locale.KOREA);
        return formatter.format(cal.getTime());
    }

    public static String getFirstDayOfMonthByPattern(String pattern) {
        SimpleDateFormat formatter = new SimpleDateFormat(pattern, Locale.KOREA);
        Calendar firstDay = Calendar.getInstance();
        firstDay.set(firstDay.get(Calendar.YEAR), firstDay.get(Calendar.MONTH), 1);
        return formatter.format(firstDay.getTime());
    }

    public static String getLastDayOfMonthByPattern(String pattern) {
        SimpleDateFormat formatter = new SimpleDateFormat(pattern, Locale.KOREA);
        Calendar lastDay = Calendar.getInstance();
        lastDay.set(lastDay.get(Calendar.YEAR), lastDay.get(Calendar.MONTH),
                lastDay.getActualMaximum(Calendar.DAY_OF_MONTH));
        return formatter.format(lastDay.getTime());
    }

    public static long getDiffBetweenDates(String begin, String end) throws Exception {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        Date beginDate = formatter.parse(begin);
        Date endDate   = formatter.parse(end);
        long diff = endDate.getTime() - beginDate.getTime();
        return diff / (24 * 60 * 60 * 1000);
    }

    public static boolean isDate(String date, String format) {
        SimpleDateFormat dateFormatParser = new SimpleDateFormat(format, Locale.KOREA);
        dateFormatParser.setLenient(false);
        try {
            dateFormatParser.parse(date);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
