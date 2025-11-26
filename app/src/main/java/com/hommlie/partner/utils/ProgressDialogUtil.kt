package com.hommlie.partner.utils

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.TypedValue
import android.view.Window
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.hommlie.partner.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch


object ProgressDialogUtil {

    private var dialog: Dialog? = null
    private var drawable: CircularProgressDrawable? = null
    private var colorJob: Job? = null

    fun showLoadingProgress(context: Activity, coroutineScope: CoroutineScope) {
        dismiss() // cleanup any previous

        val colors = getColors(context)
        var colorIndex = 0

        dialog = Dialog(context).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setContentView(R.layout.dlg_progress)
            setCancelable(false)
        }

        val progressBar = dialog?.findViewById<ProgressBar>(R.id.circularProgressBar)

//        var colorIndex = 0
        drawable = CircularProgressDrawable(context).apply {
            setStyle(CircularProgressDrawable.LARGE)
            setColorSchemeColors(colors[colorIndex])
            strokeWidth = 8f
            centerRadius = 30f
            start()
        }

        progressBar?.indeterminateDrawable = drawable
        dialog?.show()

        // Estimated full rotation = ~1333ms (default system behavior)
        val fullRotationDuration = 1333L

        colorJob = coroutineScope.launch(Dispatchers.Main) {
            while (isActive) {
                delay(fullRotationDuration)
                colorIndex = (colorIndex + 1) % colors.size
                drawable?.setColorSchemeColors(colors[colorIndex])
            }
        }
    }

    fun showAleartLoadingProgress(context: Activity, coroutineScope: CoroutineScope, title : String, desc : String) {
        dismiss() // cleanup any previous

        val colors = getColors(context)
        var colorIndex = 0

        dialog = Dialog(context,R.style.AppCompatAlertDialogStyleBig).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setContentView(R.layout.alert_progress)
            setCancelable(false)
        }

        val progressBar = dialog?.findViewById<ProgressBar>(R.id.circularProgressBar)
        val tv_title = dialog?.findViewById<TextView>(R.id.tv_title)
        val tv_description = dialog?.findViewById<TextView>(R.id.tv_desc)

        tv_title?.text = title
        tv_description?.text = desc

//        var colorIndex = 0
        drawable = CircularProgressDrawable(context).apply {
            setStyle(CircularProgressDrawable.LARGE)
            setColorSchemeColors(colors[colorIndex])
            strokeWidth = 8f
            centerRadius = 30f
            start()
        }

        progressBar?.indeterminateDrawable = drawable
        dialog?.show()

        // Estimated full rotation = ~1333ms (default system behavior)
        val fullRotationDuration = 1333L

        colorJob = coroutineScope.launch(Dispatchers.Main) {
            while (isActive) {
                delay(fullRotationDuration)
                colorIndex = (colorIndex + 1) % colors.size
                drawable?.setColorSchemeColors(colors[colorIndex])
            }
        }
    }



    fun dismiss() {
        colorJob?.cancel()
        colorJob = null
        dialog?.dismiss()
        dialog = null
    }

    private fun getColors(context: Context): List<Int> {
        return listOf(
            ContextCompat.getColor(context, R.color.color_primary),
            ContextCompat.getColor(context, R.color.orange),
            ContextCompat.getColor(context, R.color.parrotgreen),
            ContextCompat.getColor(context, R.color.purple)
        )
    }

}
