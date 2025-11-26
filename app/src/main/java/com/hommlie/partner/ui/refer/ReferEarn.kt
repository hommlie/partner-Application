package com.hommlie.partner.ui.refer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.hommlie.partner.R
import com.hommlie.partner.databinding.ActivityReferEarnBinding
import com.hommlie.partner.utils.PrefKeys
import com.hommlie.partner.utils.SharePreference
import com.hommlie.partner.utils.setupToolbar
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@AndroidEntryPoint
class ReferEarn : AppCompatActivity() {

    private lateinit var binding : ActivityReferEarnBinding
    private lateinit var referalCode:String

    @Inject
    lateinit var sharePreference: SharePreference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityReferEarnBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            WindowCompat.getInsetsController(window, window.decorView)?.apply {
                isAppearanceLightStatusBars = true // or false for light theme
                isAppearanceLightNavigationBars = true
            }

        } else {
            // This is Android 14 or below

        }

        val toolbarView = binding.root.findViewById<View>(R.id.include_toolbar)
        setupToolbar(toolbarView, "Refer & Earn", this, R.color.activity_bg, R.color.black)


        Glide.with(this@ReferEarn).load("https://www.hommlie.com/panel/public/storage/app/public/images/banner/topbanner-67dc24b0f2df3.png").thumbnail(0.1f).into(binding.ivReferbanner)

        referalCode = sharePreference.getString(PrefKeys.userId)


        binding.cardReferNow.setOnClickListener{
            shareReferralCode(referalCode,packageName)
        }
        binding.cardInvite.setOnClickListener {
            shareReferralCode(referalCode,"com.hommlie.user")
        }
        binding.tvRefferalCode.setOnClickListener {
            copyTextToClipboard(referalCode)
        }

    }

    private fun copyTextToClipboard(text: String) {
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("", text)
        clipboardManager.setPrimaryClip(clipData)
    }

    private fun shareReferralCode(referralCode: String, userApp: String) {
        val shareText = "Get an instant ₹100 voucher to use on Hommlie's trusted services! \n\n" +
                "Download here: https://play.google.com/store/apps/details?id=$userApp \n\n" +
                "To redeem the voucher, install the app and enter the referral code: $referralCode \n\n" +
                "Hurry, the reward expires in 4 weeks!"

        // Save drawable to cache
        val imageFile = File(cacheDir, "referandearn.jpeg")
        val drawable = resources.getDrawable(R.drawable.referandearn, null) as BitmapDrawable
        val bitmap = drawable.bitmap
        FileOutputStream(imageFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        // Get content URI
        val imageUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", imageFile)

        // Hybrid Share
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_STREAM, imageUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = ClipData.newUri(contentResolver, "Referral Image", imageUri)
        }

        // Grant permission to all resolved apps
        val resInfoList = packageManager.queryIntentActivities(shareIntent, PackageManager.MATCH_DEFAULT_ONLY)
        for (resolveInfo in resInfoList) {
            val packageName = resolveInfo.activityInfo.packageName
            grantUriPermission(packageName, imageUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(shareIntent, "Share Referral Code"))
    }



}