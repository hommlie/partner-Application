package com.hommlie.partner.ui.chemical

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.hommlie.partner.R
import com.hommlie.partner.apiclient.UIState
import com.hommlie.partner.databinding.ActivityActMyChemicalBinding
import com.hommlie.partner.utils.CommonMethods
import com.hommlie.partner.utils.PrefKeys
import com.hommlie.partner.utils.ProgressDialogUtil
import com.hommlie.partner.utils.SharePreference
import com.hommlie.partner.utils.setupToolbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class ActMyChemical : AppCompatActivity() {

    private lateinit var binding : ActivityActMyChemicalBinding
    private val viewModel: MyChemicalViewModel by viewModels()

    @Inject
    lateinit var sharePreference: SharePreference
    private lateinit var adapter: MyChemicalAdapter

    private val VOICE_SEARCH_REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityActMyChemicalBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // This is Android 15 or above

            WindowCompat.getInsetsController(window, window.decorView)?.apply {
                isAppearanceLightStatusBars = true // or false for light theme
                isAppearanceLightNavigationBars = true
            }

        } else {
            // This is Android 14 or below

        }

        val toolbarView = binding.root.findViewById<View>(R.id.include_toolbar)
        setupToolbar(toolbarView, "My Chemicals", this, R.color.activity_bg, R.color.black)


        adapter = MyChemicalAdapter()
        binding.rvChemicals.layoutManager = LinearLayoutManager(this)
        binding.rvChemicals.adapter = adapter


        if (sharePreference.getBoolean(PrefKeys.IS_LOGGED_IN)) {
            val userId = sharePreference.getString(PrefKeys.userId) ?: ""
            val map = HashMap<String, String>()
            map["user_id"] = userId
            viewModel.fetchChemicals(map)
        }

        binding.tvSeachText.addTextChangedListener {
            viewModel.filter(it.toString())
        }


        binding.ivMic.setOnClickListener {
            startVoiceSearch()
        }


        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is UIState.Loading -> ProgressDialogUtil.showLoadingProgress(this@ActMyChemical, lifecycleScope)
                            is UIState.Success -> {
                                ProgressDialogUtil.dismiss()
                                viewModel.resetUIState()
                            }
                            is UIState.Error -> {
                                ProgressDialogUtil.dismiss()
                                if (state.message.equals("User Not Found", true) ||
                                    state.message.equals("Employee Not Found", true)
                                ) {
                                    Toast.makeText(this@ActMyChemical, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show()
                                    CommonMethods.logOut(sharePreference, this@ActMyChemical)
                                    return@collect
                                }
                                viewModel.resetUIState()
                                Toast.makeText(this@ActMyChemical, state.message, Toast.LENGTH_SHORT).show()
                            }
                            else -> Unit
                        }
                    }
                }

                launch {
                    viewModel.filteredChemicals.collectLatest {
                        adapter.submitList(it)
                    }
                }
            }
        }

    }


    private fun startVoiceSearch() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")

        try {
            startActivityForResult(intent, VOICE_SEARCH_REQUEST_CODE)
        } catch (e: ActivityNotFoundException) {
            CommonMethods.getToast(this,"Voice search not supported on your device")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == VOICE_SEARCH_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val matches = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val recognizedText = matches?.get(0) ?: "No speech recognized"

            binding.tvSeachText.text = null
            binding.tvSeachText.setText(recognizedText)
            binding.tvSeachText.requestFocus()
            binding.tvSeachText.setSelection(recognizedText.length)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }


}