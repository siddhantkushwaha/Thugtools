package com.siddhantkushwaha.thugtools


object CommonEnums {
    enum class Intervals(val valueInMillis: Long) {
        Second(1000),
        Minute(60 * Second.valueInMillis),
        Hour(60 * Minute.valueInMillis),
        Day(24 * Hour.valueInMillis),
        Week(7 * Day.valueInMillis),
        Month(30 * Day.valueInMillis);

        fun millis(multiple: Long = 1): Long {
            return valueInMillis * multiple
        }
    }
}