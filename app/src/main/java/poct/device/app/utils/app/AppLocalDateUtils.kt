package poct.device.app.utils.app

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Period
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.Temporal

object AppLocalDateUtils {
    private val appBarTimeFormatter = DateTimeFormatter.ofPattern("MM月dd日 HH:mm")
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    private val timeFormatterForSet = DateTimeFormatter.ofPattern("MMddHHmmyyyy")
    fun formatAppBarTime(dateTime: Temporal): String {
        return appBarTimeFormatter.format(dateTime)
    }

    fun timestampFromDate(localDate: LocalDate): Long {
        return timestampFromDateTime(localDateTime = localDate.atStartOfDay())
    }

    fun timestampFromDateTime(localDateTime: LocalDateTime): Long {
        return localDateTime.toInstant(ZoneOffset.UTC).toEpochMilli()
    }

    fun dateFromTimestamp(timestamp: Long): LocalDate {
        return dateTimeFromTimestamp(timestamp = timestamp).toLocalDate()
    }

    fun dateTimeFromTimestamp(timestamp: Long): LocalDateTime {
        return LocalDateTime.ofInstant(
            Instant.ofEpochMilli(timestamp),
            ZoneOffset.UTC
        )
    }

    fun formatDate(date: LocalDate): String {
        return dateFormatter.format(date)
    }

    fun parseDate(dateValue: String): LocalDate {
        return try {
            LocalDate.parse(dateValue, dateFormatter)
        } catch (e: Exception) {
            LocalDate.of(1970, 1, 1)
        }
    }

    fun formatTime(time: LocalTime): String {
        return timeFormatter.format(time)
    }

    fun parseTime(timeValue: String): LocalTime {
        return try {
            LocalTime.parse(timeValue, timeFormatter)
        } catch (e: Exception) {
            LocalTime.of(0, 0, 0)
        }
    }

    fun formatDateTime(dateTime: Temporal): String {
        return dateTimeFormatter.format(dateTime)
    }

    fun parseDateTime(dateTimeValue: String): LocalDateTime {
        return try {
            LocalDateTime.parse(dateTimeValue, dateTimeFormatter)
        } catch (e: Exception) {
            LocalDateTime.of(1970, 1, 1, 0, 0, 0)
        }
    }

    /**
     * 计算年纪，不满1年的，按1年算
     */
    fun calcAge(dateStarted: LocalDate, dateEnded: LocalDate): Int {
        val months = calcMonth(dateStarted, dateEnded)
        val offset = if (months % 12 == 0) 0 else 1
        return months / 12 + offset
    }


    /**
     * 计算两个日期之间月份差距，不满1个月，为0
     */
    fun calcMonth(dateStarted: LocalDate, dateEnded: LocalDate): Int {
        return try {
            val period = Period.between(dateStarted, dateEnded)
            return period.toTotalMonths().toInt()
        } catch (e: Exception) {
            0
        }
    }

    fun formatForSet(dateTime: LocalDateTime): String {
        return timeFormatterForSet.format(dateTime)
    }
}