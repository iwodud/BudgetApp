package com.example.budgetapp.presentation.common

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

object FormatUtils {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pl", "PL"))
    private val monthKeyFormat = SimpleDateFormat("yyyy-MM", Locale("pl", "PL"))
    private val monthDisplayFormat = SimpleDateFormat("LLLL yyyy", Locale("pl", "PL"))
    private val dateDisplayFormat = SimpleDateFormat("dd.MM.yyyy", Locale("pl", "PL"))
    private val yearFormat = SimpleDateFormat("yyyy", Locale("pl", "PL"))

    fun formatAmount(amount: Double): String = currencyFormat.format(amount)

    fun formatDate(timestamp: Long): String = dateDisplayFormat.format(Date(timestamp))

    fun currentMonth(): String = monthKeyFormat.format(Date())

    fun currentYear(): String = yearFormat.format(Date())

    fun monthToDisplay(month: String): String {
        return try {
            val date = monthKeyFormat.parse(month) ?: return month
            monthDisplayFormat.format(date).replaceFirstChar { it.uppercase() }
        } catch (e: Exception) { month }
    }

    fun timestampToMonth(timestamp: Long): String = monthKeyFormat.format(Date(timestamp))

    fun previousMonth(month: String): String = shiftMonth(month, -1)

    fun nextMonth(month: String): String = shiftMonth(month, 1)

    private fun shiftMonth(month: String, delta: Int): String {
        return try {
            val date = monthKeyFormat.parse(month) ?: return month
            val cal = Calendar.getInstance()
            cal.time = date
            cal.add(Calendar.MONTH, delta)
            monthKeyFormat.format(cal.time)
        } catch (e: Exception) { month }
    }

    fun getMonthRange(month: String): Pair<Long, Long> {
        return try {
            val date = monthKeyFormat.parse(month) ?: return Pair(0L, Long.MAX_VALUE)
            val cal = Calendar.getInstance()
            cal.time = date
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
            val start = cal.timeInMillis
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
            cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999)
            Pair(start, cal.timeInMillis)
        } catch (e: Exception) { Pair(0L, Long.MAX_VALUE) }
    }

    fun availableYears(): List<String> {
        val currentYear = currentYear().toInt()
        return (currentYear - 2..currentYear).map { it.toString() }.reversed()
    }
}
