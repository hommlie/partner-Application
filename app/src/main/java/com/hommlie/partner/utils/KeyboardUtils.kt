package com.hommlie.partner.utils

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment

object KeyboardUtils {

    fun hideKeyboard(activity: Activity) {
        val view = activity.currentFocus ?: View(activity)
        val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    fun hideKeyboard(view: View) {
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    fun Fragment.hideKeyboard() {
        view?.let { activity?.let { act -> hideKeyboard(act) } }
    }

    // usage
    // from activity   KeyboardUtils.hideKeyboard(this)
    // from fragment   KeyboardUtils.hideKeyboard()
    // from view        KeyboardUtils.hideKeyboard(yourview)  e.g.  KeyboardUtils.hideKeyboard(binding.edtMobile)

}
