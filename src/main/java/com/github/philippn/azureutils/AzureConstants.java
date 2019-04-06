/**
 * 
 */
package com.github.philippn.azureutils;

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.DAY_OF_WEEK;
import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;
import static java.time.temporal.ChronoField.YEAR;

import java.time.ZoneOffset;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.time.format.SignStyle;
import java.time.temporal.ChronoField;
import java.util.HashMap;
import java.util.Map;

/**
 * @author nanz0phi
 */
public final class AzureConstants {

    /**
     * The RFC-1123 date-time formatter, such as 'Tue, 3 Jun 2008 11:05:30 GMT'.
     * <p>
     * This returns an immutable formatter capable of formatting and parsing
     * most of the RFC-1123 format.
     * RFC-1123 updates RFC-822 changing the year from two digits to four.
     * This implementation requires a four digit year.
     * This implementation also does not handle North American or military zone
     * names, only 'GMT' and offset amounts.
     * <p>
     * The format consists of:
     * <ul>
     * <li>If the day-of-week is not available to format or parse then jump to day-of-month.
     * <li>Three letter {@link ChronoField#DAY_OF_WEEK day-of-week} in English.
     * <li>A comma
     * <li>A space
     * <li>Two digits for the {@link ChronoField#DAY_OF_MONTH day-of-month}.
     * <li>A space
     * <li>Three letter {@link ChronoField#MONTH_OF_YEAR month-of-year} in English.
     * <li>A space
     * <li>Four digits for the {@link ChronoField#YEAR year}.
     *  Only years in the range 0000 to 9999 are supported.
     * <li>A space
     * <li>Two digits for the {@link ChronoField#HOUR_OF_DAY hour-of-day}.
     *  This is pre-padded by zero to ensure two digits.
     * <li>A colon
     * <li>Two digits for the {@link ChronoField#MINUTE_OF_HOUR minute-of-hour}.
     *  This is pre-padded by zero to ensure two digits.
     * <li>If the second-of-minute is not available then jump to the next space.
     * <li>A colon
     * <li>Two digits for the {@link ChronoField#SECOND_OF_MINUTE second-of-minute}.
     *  This is pre-padded by zero to ensure two digits.
     * <li>A space
     * <li>The {@link ZoneOffset#getId() offset ID} without colons or seconds.
     *  An offset of zero uses "GMT". North American zone names and military zone names are not handled.
     * </ul>
     * <p>
     * Parsing is case insensitive.
     * <p>
     * The returned formatter has a chronology of ISO set to ensure dates in
     * other calendar systems are correctly converted.
     * It has no override zone and uses the {@link ResolverStyle#SMART SMART} resolver style.
     */
    public static final DateTimeFormatter RFC_1123_DATE_TIME;
    static {
        // manually code maps to ensure correct data always used
        // (locale data can be changed by application code)
        Map<Long, String> dow = new HashMap<>();
        dow.put(1L, "Mon");
        dow.put(2L, "Tue");
        dow.put(3L, "Wed");
        dow.put(4L, "Thu");
        dow.put(5L, "Fri");
        dow.put(6L, "Sat");
        dow.put(7L, "Sun");
        Map<Long, String> moy = new HashMap<>();
        moy.put(1L, "Jan");
        moy.put(2L, "Feb");
        moy.put(3L, "Mar");
        moy.put(4L, "Apr");
        moy.put(5L, "May");
        moy.put(6L, "Jun");
        moy.put(7L, "Jul");
        moy.put(8L, "Aug");
        moy.put(9L, "Sep");
        moy.put(10L, "Oct");
        moy.put(11L, "Nov");
        moy.put(12L, "Dec");
        RFC_1123_DATE_TIME = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .parseLenient()
                .optionalStart()
                .appendText(DAY_OF_WEEK, dow)
                .appendLiteral(", ")
                .optionalEnd()
                .appendValue(DAY_OF_MONTH, 2, 2, SignStyle.NOT_NEGATIVE) // Azure requires padded value!!
                .appendLiteral(' ')
                .appendText(MONTH_OF_YEAR, moy)
                .appendLiteral(' ')
                .appendValue(YEAR, 4)  // 2 digit year not handled
                .appendLiteral(' ')
                .appendValue(HOUR_OF_DAY, 2)
                .appendLiteral(':')
                .appendValue(MINUTE_OF_HOUR, 2)
                .optionalStart()
                .appendLiteral(':')
                .appendValue(SECOND_OF_MINUTE, 2)
                .optionalEnd()
                .appendLiteral(' ')
                .appendOffset("+HHMM", "GMT")  // should handle UT/Z/EST/EDT/CST/CDT/MST/MDT/PST/MDT
                .toFormatter()
                .withResolverStyle(ResolverStyle.SMART)
                .withChronology(IsoChronology.INSTANCE);
    }
}
