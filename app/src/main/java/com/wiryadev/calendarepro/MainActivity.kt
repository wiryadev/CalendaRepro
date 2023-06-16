package com.wiryadev.calendarepro

import android.graphics.drawable.Drawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.kizitonwose.calendarview.model.CalendarDay
import com.kizitonwose.calendarview.model.DayOwner
import com.kizitonwose.calendarview.ui.DayBinder
import com.kizitonwose.calendarview.ui.ViewContainer
import com.kizitonwose.calendarview.utils.yearMonth
import com.wiryadev.calendarepro.databinding.ActivityMainBinding
import com.wiryadev.calendarepro.databinding.LayoutCalendarDayBinding
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var selectedYearMonth: YearMonth = LocalDate.now().yearMonth
    private var today: LocalDate = LocalDate.now()
    private var firstDate: LocalDate = LocalDate.of(today.year, today.month, 1)
    private val initialSelection = DateSelection(firstDate, today)
    var selection = initialSelection

    private val daysOfWeek = arrayOf(
        DayOfWeek.SUNDAY,
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
        DayOfWeek.SATURDAY,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupCalendar()
        setOnMonthClick()
    }

    private fun setOnMonthClick() {
        binding.apply {
            btnNextMonth.setOnClickListener {
                calendarContainer.smoothScrollToMonth(selectedYearMonth.plusMonths(1))
            }
            btnPreviousMonth.setOnClickListener {
                calendarContainer.smoothScrollToMonth(selectedYearMonth.minusMonths(1))
            }
        }
    }

    private fun setupCalendar() {
        val earliestAvailableMonth = YearMonth.now().minusMonths(5)
        val currentMonth = YearMonth.now()

        binding.calendarContainer.apply {
            setup(earliestAvailableMonth, currentMonth, daysOfWeek.first())
            scrollToMonth(currentMonth)

            dayBinder = object : DayBinder<DayViewContainer> {
                override fun create(view: View) = DayViewContainer(view)

                override fun bind(container: DayViewContainer, day: CalendarDay) {

                    with(container) {
                        container.bindCalendarDay(day)
                        tvCalendar.text = day.date.dayOfMonth.toString()
                        val (startDate, endDate) = selection

                        roundBg.visibility = View.INVISIBLE
                        continuesBg.visibility = View.INVISIBLE

                        when {

                            day.date == startDate || day.date == endDate -> setStartEndSelectionMarkerAppearance()

                            endDate != null && (day.date > startDate && day.date < endDate) -> {
                                setSelectionRangeAppearance()
                            }

                            else -> setDateFromCurrentSelectedMonthAppearance()
                        }
                    }
                }
            }

            monthScrollListener = {
                selectedYearMonth = it.yearMonth
                val monthTitleFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
                binding.tvMonthYear.text = monthTitleFormatter.format(it.yearMonth)
            }
        }
    }

    inner class DayViewContainer(view: View) : ViewContainer(view) {
        private lateinit var calendarDay: CalendarDay
        val tvCalendar = LayoutCalendarDayBinding.bind(view).tvCalendar
        val roundBg = LayoutCalendarDayBinding.bind(view).viewRound
        val continuesBg = LayoutCalendarDayBinding.bind(view).viewContinues

        // Background for start, end and middle selection date
        private val startEndBackground = ContextCompat.getDrawable(
            this@MainActivity, R.color.colorGreenLeaf
        )
        private val rangeMiddleBackground = ContextCompat.getDrawable(
            this@MainActivity, R.color.colorGreenTea
        )

        fun bindCalendarDay(day: CalendarDay) {
            calendarDay = day
        }

        fun setStartEndSelectionMarkerAppearance() {
            tvCalendar.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.white))
            roundBg.applyBackground(startEndBackground)
        }

        fun setSelectionRangeAppearance() {
            tvCalendar.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.black))
            continuesBg.applyBackground(rangeMiddleBackground)
        }

        fun setDateFromCurrentSelectedMonthAppearance() {
            tvCalendar.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.white))
        }

        private fun View.applyBackground(resource: Drawable?) {
            isVisible = true
            background = resource
        }

        init {
            view.setOnClickListener {
                if (calendarDay.date > today) return@setOnClickListener
                selection = RangeCalendarUtils.getSelection(
                    clickedDate = calendarDay.date,
                    dateSelection = selection
                )
                binding.calendarContainer.notifyCalendarChanged()
            }
        }
    }

}

object RangeCalendarUtils {

    fun getSelection(
        clickedDate: LocalDate,
        dateSelection: DateSelection,
    ): DateSelection {
        val (selectionStartDate, selectionEndDate) = dateSelection
        return if (selectionEndDate != null) {
            DateSelection(clickedDate, null)
        } else {
            if (clickedDate < selectionStartDate) {
                DateSelection(clickedDate, selectionStartDate)
            } else {
                DateSelection(selectionStartDate, clickedDate)
            }
        }
    }
}

data class DateSelection(
    val startDate: LocalDate,
    val endDate: LocalDate? = null
) {
    fun getLong(): Pair<Long, Long> {
        return Pair(
            startDate.toLong(),
            endDate?.toLong() ?: startDate.toLong()
        )
    }
}

fun LocalDate.toLong(): Long {
    return this.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}