package com.hommlie.partner.ui.setting

import android.animation.Animator
import android.animation.AnimatorInflater
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.bumptech.glide.Glide
import com.hommlie.partner.R
import com.hommlie.partner.databinding.ActivityIdcardBinding
import com.hommlie.partner.utils.CommonMethods
import com.hommlie.partner.utils.CommonMethods.toCapwords
import com.hommlie.partner.utils.PrefKeys
import com.hommlie.partner.utils.SharePreference
import com.hommlie.partner.utils.setupToolbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.div
import kotlin.text.toFloat

@AndroidEntryPoint
class IDCard : AppCompatActivity() {

    private lateinit var binding : ActivityIdcardBinding
    @Inject
    lateinit var sharePreference: SharePreference

    private var selectedType = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityIdcardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val toolbarView = binding.root.findViewById<View>(R.id.include_toolbar)
        setupToolbar(toolbarView, "Virtual ID Card", this, R.color.white, R.color.black)


        binding.apply {
            tvName.text = sharePreference.getString(PrefKeys.userName).replace(",", "").toCapwords()
            tvEmpcode.text = sharePreference.getString(PrefKeys.emp_code)
            tvPhone.text = sharePreference.getString(PrefKeys.userMobile).replace("+91", "+91 ")
            tvDesignation.text = sharePreference.getString(PrefKeys.Designation, "").takeIf { !it.isNullOrEmpty() } ?: "Designation"
            binding.tvBloodgroup.text = sharePreference.getString(PrefKeys.BloodGroup,"").takeIf { !it.isNullOrEmpty() } ?: "N/A"
            binding.tvdob.text = CommonMethods.formatDateToReadable(sharePreference.getString(PrefKeys.DOB))
        }

        Glide.with(this)
            .load(sharePreference.getString(PrefKeys.userProfile))
            .placeholder(R.drawable.ic_dummy_profile)
            .into(binding.ivProfile)

        selectedType = if (binding.backCard.visibility == View.VISIBLE) "BACK" else "FRONT"

        initSideSelection()

        binding.tvWeblink.setOnClickListener {
            val url = "https://www.hommlie.com"
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            startActivity(intent)
        }

        binding.ivLinkedin.setOnClickListener {
            val url = "https://www.linkedin.com/company/hommlie/posts/?feedView=all"
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            startActivity(intent)
        }
        binding.ivInsta.setOnClickListener {
            val url = "https://www.instagram.com/hommlieofficial/"
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            startActivity(intent)
        }
        binding.ivFacebook.setOnClickListener {
            val url = "https://www.facebook.com/hommlie"
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            startActivity(intent)
        }
        binding.ivTwitter.setOnClickListener {
            val url = "https://x.com/hommlie"
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            startActivity(intent)
        }

    }

    private fun initSideSelection() {

        binding.mcvAppointmentTypeInperson.setOnClickListener {
            if (selectedType != "FRONT") {
                setSelectedType("FRONT")
                CommonMethods.vibratePhone(this)
                flipCard(true)
            }
        }

        binding.mcvAppointmentTypeAudiovideo.setOnClickListener {
            if (selectedType != "BACK") {
                setSelectedType("BACK")
                CommonMethods.vibratePhone(this)
                flipCard(false)
            }
        }

        // Initial selection
        setSelectedType(selectedType)

        binding.frontCard.setOnSwipeListener(
            onSwipeLeft = {
//                setSelectedType("BACK")
//                CommonMethods.vibratePhone(this)
//                flipCard(false)
            },
            onSwipeRight = {
                setSelectedType("BACK")
                CommonMethods.vibratePhone(this)
                flipCard(false)
            }
        )

        binding.backCard.setOnSwipeListener(
            onSwipeLeft = {
                setSelectedType("FRONT")
                CommonMethods.vibratePhone(this)
                flipCard(true)
            },
            onSwipeRight = {
//                setSelectedType("FRONT")
//                CommonMethods.vibratePhone(this)
//                flipCard(true)
            }
        )
    }

    private fun setSelectedType(type: String) {
        selectedType = type

        if (type == "FRONT") {

            binding.tvFront.apply {
                setTextColor(ContextCompat.getColor(context, R.color.color_90C01F))
                typeface = ResourcesCompat.getFont(context, R.font.sfprodisplay_bold)
            }

            binding.tvBack.apply {
                setTextColor(ContextCompat.getColor(context, R.color.dark_gray))
                typeface = ResourcesCompat.getFont(context, R.font.sfprodisplay_medium)
            }

        } else {

            binding.tvFront.apply {
                setTextColor(ContextCompat.getColor(context, R.color.dark_gray))
                typeface = ResourcesCompat.getFont(context, R.font.sfprodisplay_medium)
            }

            binding.tvBack.apply {
                setTextColor(ContextCompat.getColor(context, R.color.color_90C01F))
                typeface = ResourcesCompat.getFont(context, R.font.sfprodisplay_bold)
            }
        }

        binding.appointmentContainer.post {

            val containerWidth = binding.appointmentContainer.width
            val margin = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._8sdp)

            val indicatorWidth = (containerWidth - margin * 2) / 2

            binding.selectionIndicator.layoutParams =
                binding.selectionIndicator.layoutParams.apply {
                    width = indicatorWidth
                }

            val targetX = if (type == "FRONT") {
                0f
            } else {
                (indicatorWidth + margin).toFloat()
            }

            binding.selectionIndicator.animate()
                .translationX(targetX)
                .setDuration(250)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    private fun flipCard(showFront: Boolean) {

        val visibleCard = if (showFront) binding.frontCard else binding.backCard
        val hiddenCard = if (showFront) binding.backCard else binding.frontCard

        val flipOut = AnimatorInflater.loadAnimator(
            this,
            if (showFront)
                R.animator.back_to_front_out
            else
                R.animator.front_to_back_out
        )

        val flipIn = AnimatorInflater.loadAnimator(
            this,
            if (showFront)
                R.animator.back_to_front_in
            else
                R.animator.front_to_back_in
        )

        val scale = resources.displayMetrics.density
        binding.frontCard.cameraDistance = 8000 * scale
        binding.backCard.cameraDistance = 8000 * scale

        flipOut.setTarget(hiddenCard)
        flipIn.setTarget(visibleCard)

        flipOut.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                hiddenCard.visibility = View.GONE
                visibleCard.visibility = View.VISIBLE
                flipIn.start()
            }
        })

        flipOut.start()
    }

    fun View.setOnSwipeListener(
        onSwipeLeft: () -> Unit,
        onSwipeRight: () -> Unit
    ) {
        val gestureDetector = GestureDetector(
            context,
            object : GestureDetector.SimpleOnGestureListener() {

                private val SWIPE_THRESHOLD = 100
                private val SWIPE_VELOCITY_THRESHOLD = 100

                override fun onFling(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    velocityX: Float,
                    velocityY: Float
                ): Boolean {
                    val diffX = e2.x - (e1?.x ?: 0f)

                    if (kotlin.math.abs(diffX) > SWIPE_THRESHOLD &&
                        kotlin.math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD
                    ) {
                        if (diffX > 0) {
                            onSwipeRight()
                        } else {
                            onSwipeLeft()
                        }
                        return true
                    }
                    return false
                }
            }
        )

        setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
    }

}