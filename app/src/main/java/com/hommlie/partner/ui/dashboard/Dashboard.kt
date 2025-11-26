package com.hommlie.partner.ui.dashboard

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.hommlie.partner.R
import com.hommlie.partner.databinding.FragmentDashboardBinding
import com.hommlie.partner.databinding.FragmentSettingBinding
import com.hommlie.partner.utils.CommonMethods
import com.hommlie.partner.utils.OnNearByServicesClickListener
import com.hommlie.partner.utils.PrefKeys
import com.hommlie.partner.utils.SharePreference
import com.hommlie.partner.utils.setupToolbar
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@AndroidEntryPoint
class Dashboard : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    lateinit var clickListener: OnNearByServicesClickListener

    private lateinit var referalCode:String

    @Inject
    lateinit var sharePreference : SharePreference


    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnNearByServicesClickListener) {
            clickListener = context
        } else {
            throw RuntimeException("$context must implement OnNearByServicesClickListener")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val statusBarHeight = CommonMethods.getStatusBarHeight(requireContext())

        val viewStatusBar = binding.viewStatusbar.layoutParams
        viewStatusBar.height = statusBarHeight
        binding.viewStatusbar.layoutParams = viewStatusBar

        binding.ivBack.setOnClickListener {
            clickListener.activateHome()
        }


        Glide.with(requireContext()).load("https://www.hommlie.com/panel/public/storage/app/public/images/banner/topbanner-67dc24b0f2df3.png").thumbnail(0.1f).into(binding.ivReferbanner)

        referalCode = sharePreference.getString(PrefKeys.userId)


        binding.cardReferNow.setOnClickListener{
            shareReferralCode(referalCode,requireContext().packageName)
        }
        binding.cardInvite.setOnClickListener {
            shareReferralCode(referalCode,"com.hommlie.user")
        }
        binding.tvRefferalCode.setOnClickListener {
            copyTextToClipboard(referalCode)
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    private fun copyTextToClipboard(text: String) {
        val clipboardManager = requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("", text)
        clipboardManager.setPrimaryClip(clipData)
    }


    private fun shareReferralCode(referralCode: String, userApp: String) {
        val shareText = "Get an instant ₹100 voucher to use on Hommlie's trusted services! \n\n" +
                "Download here: https://play.google.com/store/apps/details?id=$userApp \n\n" +
                "To redeem the voucher, install the app and enter the referral code: $referralCode \n\n" +
                "Hurry, the reward expires in 4 weeks!"

        // Save drawable to cache
        val imageFile = File(requireContext().cacheDir, "referandearn.jpeg")
        val drawable = resources.getDrawable(R.drawable.referandearn, null) as BitmapDrawable
        val bitmap = drawable.bitmap
        FileOutputStream(imageFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        // Get content URI
        val imageUri = FileProvider.getUriForFile(requireActivity(), "${requireContext().packageName}.fileprovider", imageFile)

        // Hybrid Share
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_STREAM, imageUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = ClipData.newUri(requireContext().contentResolver, "Referral Image", imageUri)
        }

        // Grant permission to all resolved apps
        val resInfoList = requireContext().packageManager.queryIntentActivities(shareIntent, PackageManager.MATCH_DEFAULT_ONLY)
        for (resolveInfo in resInfoList) {
            val packageName = resolveInfo.activityInfo.packageName
            requireActivity().grantUriPermission(packageName, imageUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(shareIntent, "Share Referral Code"))
    }



}