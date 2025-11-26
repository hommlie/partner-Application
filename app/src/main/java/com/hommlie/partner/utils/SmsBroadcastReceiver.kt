package com.hommlie.partner.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.hommlie.partner.ui.login.ActOTP

class SmsBroadcastReceiver : BroadcastReceiver() {

    var listener: OTPReceiveListener? = null

    override fun onReceive(context: Context?, intent: Intent?) {
        if (SmsRetriever.SMS_RETRIEVED_ACTION == intent?.action) {
            val extras = intent.extras
            val status = extras?.get(SmsRetriever.EXTRA_STATUS) as? Status

            when (status?.statusCode) {
                CommonStatusCodes.SUCCESS -> {
                    val message = extras.getString(SmsRetriever.EXTRA_SMS_MESSAGE)
                    if (!message.isNullOrEmpty()) {
                        val otp = extractOtpFromMessage(message)
                        ActOTP.autoOTP.value=otp
                        Log.d("autootp",ActOTP.autoOTP.value.toString())
                        Log.d("OTP", "📩 OTP Received: $otp")
                        listener?.onOTPReceived(otp)
                    } else {
                        Log.e("OTP", "🚨 No SMS message found!")
                    }
                }
                CommonStatusCodes.TIMEOUT -> {
                    Log.e("OTP", "⏳ SMS Retriever Timeout!")
                }
            }
        }
    }

    private fun extractOtpFromMessage(message: String): String {
        val pattern = Regex("\\b\\d{4}\\b") // Adjust for 4-digit OTP
        return pattern.find(message)?.value.orEmpty()
    }

    interface OTPReceiveListener {
        fun onOTPReceived(otp: String)
    }
}