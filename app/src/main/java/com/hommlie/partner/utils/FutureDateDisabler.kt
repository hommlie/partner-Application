package com.hommlie.partner.utils

import android.graphics.Color
import android.text.style.ForegroundColorSpan
import com.prolificinteractive.materialcalendarview.*
import java.util.*

class FutureDateDisabler : DayViewDecorator {
    private val today = CalendarDay.today()

    override fun shouldDecorate(day: CalendarDay): Boolean {
        return day.isAfter(today)
    }

    override fun decorate(view: DayViewFacade) {
        view.setDaysDisabled(true)
        view.addSpan(object : ForegroundColorSpan(Color.LTGRAY) {}) // Grayed-out look
    }
}
