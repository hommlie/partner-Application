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
import com.hommlie.partner.databinding.ActivityActNewChemicalBinding
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
class ActNewChemical : AppCompatActivity(), OnChemicalActionListener  {

    private lateinit var binding : ActivityActNewChemicalBinding
    private lateinit var userId : String

    private val viewModel: NewChemicalViewModel by viewModels()

    @Inject
    lateinit var sharePreference: SharePreference

    private lateinit var adapter: NewChemicalAdapter

    private val VOICE_SEARCH_REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityActNewChemicalBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val toolbarView = binding.root.findViewById<View>(R.id.include_toolbar)
        setupToolbar(toolbarView, "New Chemicals", this, R.color.activity_bg, R.color.black)

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // This is Android 15 or above
            WindowCompat.getInsetsController(window, window.decorView)?.apply {
                isAppearanceLightStatusBars = true // or false for light theme
                isAppearanceLightNavigationBars = true
            }
        } else {
            // This is Android 14 or below
        }

        adapter = NewChemicalAdapter(this,this) { selectedItems, isAllSelected  ->
//            if (selectedList.isNotEmpty()) {
//                binding.llActAction.visibility = View.VISIBLE
//            } else {
//                binding.llActAction.visibility = View.GONE
//            }

            binding.llActAction.visibility = if (selectedItems.isNotEmpty()) View.VISIBLE else View.GONE

            // Sync "Select All" checkbox without triggering listener again
            binding.checkSelectall.setOnCheckedChangeListener(null)
            binding.checkSelectall.isChecked = isAllSelected
            binding.checkSelectall.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    val selected = adapter.selectAll(true)
                    binding.llActAction.visibility = if (selected.isNotEmpty()) View.VISIBLE else View.GONE
                } else {
                    adapter.clearSelection()
//                    binding.llActAction.visibility = View.GONE
                }
            }

        }

        binding.rvNewchemicals.layoutManager = LinearLayoutManager(this)
        binding.rvNewchemicals.adapter = adapter



        if (sharePreference.getBoolean(PrefKeys.IS_LOGGED_IN)) {
            userId = sharePreference.getString(PrefKeys.userId) ?: ""
            val map = HashMap<String, String>()
            map["user_id"] = userId
            viewModel.fetchChemicals(map)

            binding.swipeRefresh.setOnRefreshListener {
                viewModel.fetchChemicals(map) // Refresh data from server
            }
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
                            is UIState.Loading -> ProgressDialogUtil.showLoadingProgress(this@ActNewChemical, lifecycleScope)
                            is UIState.Success -> {
                                binding.swipeRefresh.isRefreshing = false
                                ProgressDialogUtil.dismiss()
                                viewModel.resetUIState()
                            }
                            is UIState.Error -> {
                                binding.swipeRefresh.isRefreshing = false
                                ProgressDialogUtil.dismiss()
                                if (state.message.equals("User Not Found", true) ||
                                    state.message.equals("Employee Not Found", true)
                                ) {
                                    Toast.makeText(this@ActNewChemical, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show()
                                    CommonMethods.logOut(sharePreference, this@ActNewChemical)
                                    return@collect
                                }
                                viewModel.resetUIState()
                                Toast.makeText(this@ActNewChemical, state.message, Toast.LENGTH_SHORT).show()
                            }
                            else -> Unit
                        }
                    }
                }

                launch {
                    viewModel.filteredChemicals.collectLatest {
                        adapter.updateList(it)
                    }
                }
            }
        }


        observeVerifyResponse()

        binding.mcvAcknowledge.setOnClickListener {
//            val map = HashMap<String, String>()
//            map["user_id"] = userId

            val selectedIds = adapter.getSelectedItems()
                .map { it.id } // or whatever field holds the ID
                .joinToString(",") // converts list to: "2,3,1,34,22,9"

            val ids = adapter.getSelectedItems().map { it.id }

//            map["chemical_id"] = selectedIds

//            viewModel.verifyMultipleChemicals(map, ids)

//            val idList = selectedIds.split(",").map { it.toInt() }

            val label = if (ids.size == 1) "chemical" else "chemicals"
            CommonMethods.showConfirmationDialog(
                this,
                "Confirmation",
                "Are you sure you want to verify these ${ids.size} $label?",
                false,
                true,
            ) {
                val map = hashMapOf("user_id" to userId, "chemical_id" to selectedIds)
                viewModel.verifyMultipleChemicals(map, ids)
            }

        }


    }


    private fun observeVerifyResponse(){
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.chemicalverifyState.collect { state ->
                    when (state) {
                        is UIState.Loading -> ProgressDialogUtil.showLoadingProgress(this@ActNewChemical, lifecycleScope)

                        is UIState.Success -> {
                            ProgressDialogUtil.dismiss()
                            viewModel.resetChemicalVerifyState()
                        }

                        is UIState.Error -> {
                            ProgressDialogUtil.dismiss()
                            if (state.message.equals("User Not Found", true) ||
                                state.message.equals("Employee Not Found", true)
                            ) {
                                Toast.makeText(this@ActNewChemical, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show()
                                CommonMethods.logOut(sharePreference, this@ActNewChemical)
                                return@collect
                            }
                            viewModel.resetChemicalVerifyState()
                            Toast.makeText(this@ActNewChemical, state.message, Toast.LENGTH_SHORT).show()
                        }

                        else -> Unit
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

    override fun onAcknowledgeClicked(chemicalId: Int) {
//        val map = HashMap<String, String>()
//        map["user_id"] = userId
//        map["chemical_id"] = chemicalId.toString()
//        viewModel.verifyChemical(map, chemicalId)

        CommonMethods.showConfirmationDialog(
            this,
            "Confirmation",
            "Are you sure you want to verify this chemical?",
            false,
            true
        ) {
            val map = hashMapOf("user_id" to userId, "chemical_id" to chemicalId.toString())
            viewModel.verifyChemical(map, chemicalId)
        }
    }

    override fun onReportIssueClicked(chemicalId: Int) {
        val map = HashMap<String, String>()
        map["user_id"] = userId
        map["chemical_id"] = chemicalId.toString()
        viewModel.verifyChemical(map, chemicalId)
    }
}