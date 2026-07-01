package com.hommlie.partner.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Build
import android.os.Parcelable
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.google.android.material.card.MaterialCardView
import com.hommlie.partner.R
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import kotlin.collections.joinToString
import kotlin.jvm.java
import kotlin.let
import kotlin.text.indexOf
import kotlin.text.isLowerCase
import kotlin.text.lowercase
import kotlin.text.replaceFirstChar
import kotlin.text.split
import kotlin.text.titlecase
import kotlin.text.uppercase

object ExtentionMethods {

    fun String.toPlainRequestBody(): RequestBody =
        this.toRequestBody("text/plain".toMediaTypeOrNull())

    fun String.toCapWords(): String {
        return this.lowercase()
            .split(" ")
            .joinToString(" ") { word ->
                word.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase() else it.toString()
                }
            }
    }

    fun String.toCapOnlyFirstLetter() : String{
        return this.lowercase().replaceFirstChar { it.uppercase() }
    }

    fun Context.hasPermission(permissionType: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permissionType) ==
                PackageManager.PERMISSION_GRANTED
    }

    fun Context.hasRequiredBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            hasPermission(Manifest.permission.BLUETOOTH_SCAN) &&
                    hasPermission(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) &&
                    hasPermission(Manifest.permission.BLUETOOTH_ADMIN)
        }
    }

    fun Context.hasRequiredBluetoothConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            hasPermission(Manifest.permission.BLUETOOTH_CONNECT)
        } else true
    }

    fun AppCompatActivity.openNewFragment(fragment: Fragment, tag: String, fragmentContainer: Int) {
        supportFragmentManager.beginTransaction()
            .replace(fragmentContainer, fragment, tag) // replace current tab fragment
            .addToBackStack(tag) // allow back navigation
            .commit()
    }
//   Usage   (activity as? AppCompatActivity)?.openNewFragment(AddMember(), "AddMember",R.id.fragment_container)


    fun TextView.colorStar(starColorRes: Int) {
        val textValue = this.text?.toString() ?: return
        val spannable = SpannableString(textValue)
        val starIndex = textValue.indexOf("*")
        if (starIndex != -1) {
            spannable.setSpan(
                ForegroundColorSpan(ContextCompat.getColor(context, starColorRes)),
                starIndex,
                starIndex + 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            this.text = spannable
        }
    }


    fun MaterialCardView.showDisable() {
        this.isEnabled = false
        this.isClickable = false
        this.alpha = 0.4f
    }
    fun MaterialCardView.showEnable() {
        this.isEnabled = true
        this.isClickable = true
        this.alpha = 1f
    }

    fun TextView.setColoredText(fullText: String, colorMap: Map<String, Int>) {
        val spannable = SpannableString(fullText)

        for ((word, color) in colorMap) {
            var startIndex = fullText.indexOf(word)
            while (startIndex >= 0) {
                val endIndex = startIndex + word.length
                spannable.setSpan(
                    ForegroundColorSpan(color),
                    startIndex,
                    endIndex,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                startIndex = fullText.indexOf(word, endIndex)
            }
        }
        this.text = spannable
    }
//    fun TextView.setBoldText(fullText: String, wordsToBold: List<String>, fontResId: Int) {
//        val spannable = SpannableString(fullText)
//        val boldTypeface: Typeface? = ResourcesCompat.getFont(context, fontResId)
//
//        for (word in wordsToBold) {
//            var startIndex = fullText.indexOf(word)
//            while (startIndex >= 0) {
//                val endIndex = startIndex + word.length
//
//                boldTypeface?.let {
//                    spannable.setSpan(
//                        CustomTypefaceSpan(it),
//                        startIndex,
//                        endIndex,
//                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
//                    )
//                }
//
//                startIndex = fullText.indexOf(word, endIndex)
//            }
//        }
//
//        this.text = spannable
//    }


    fun Context.startSlideActivity(intent: Intent) {
        val options = ActivityOptionsCompat.makeCustomAnimation(
            this,
            R.anim.slide_in_right,
            R.anim.no_animation
        )
        this.startActivity(intent, options.toBundle())
    }
    fun Context.startSlideTopActivity(intent: Intent) {
        val options = ActivityOptionsCompat.makeCustomAnimation(
            this,
            R.anim.slide_in_top,
            R.anim.no_animation
        )
        this.startActivity(intent, options.toBundle())
    }
    fun Activity.finishSlideActivity(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(
                Activity.OVERRIDE_TRANSITION_CLOSE,
                R.anim.no_animation,
                R.anim.slide_out_right
            )
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.no_animation, R.anim.slide_out_right)
        }
    }

    fun Activity.finishSlideBottomActivity(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(
                Activity.OVERRIDE_TRANSITION_CLOSE,
                R.anim.no_animation,
                R.anim.slide_out_top
            )
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.no_animation, R.anim.slide_out_top)
        }
    }
    inline fun <reified T : Parcelable> Intent.getParcelableCompat(key: String): T? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(key, T::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelableExtra(key)
        }
    }
}