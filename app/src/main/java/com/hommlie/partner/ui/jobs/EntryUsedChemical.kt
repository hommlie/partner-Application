package com.hommlie.partner.ui.jobs

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Rect
import android.os.Bundle
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hommlie.partner.R
import com.hommlie.partner.apiclient.UIState
import com.hommlie.partner.databinding.ActivityEntryUsedChemicalBinding
import com.hommlie.partner.model.ChemicalItemUI
import com.hommlie.partner.model.UpdateFilledChemical
import com.hommlie.partner.model.UpdateFilledChemicalRequestBody
import com.hommlie.partner.model.VisitChemicals
import com.hommlie.partner.utils.CommonMethods
import com.hommlie.partner.utils.CommonMethods.showToast
import com.hommlie.partner.utils.Constants.EXTRA_CHEMICALS
import com.hommlie.partner.utils.ExtentionMethods.finishSlideActivity
import com.hommlie.partner.utils.PrefKeys
import com.hommlie.partner.utils.ProgressDialogUtil
import com.hommlie.partner.utils.SharePreference
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class EntryUsedChemical : AppCompatActivity() {

    @Inject
    lateinit var sharePreference : SharePreference
    private lateinit var binding : ActivityEntryUsedChemicalBinding
    private val viewModel : EntryUsedChemicalViewModel by viewModels()

    private lateinit var chemicalAdapter: ChemicalUsedAdapter
    private var chemicalList = mutableListOf<ChemicalItemUI>()
    private var visitID : String = "0"
    private var userId : String = "0"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEntryUsedChemicalBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            insets
        }
        val statusBarHeight = CommonMethods.getStatusBarHeight(this)
        val viewStatusBar = binding.statusBar.layoutParams
        viewStatusBar.height = statusBarHeight
        binding.statusBar.layoutParams = viewStatusBar

        CommonMethods.setSystemBarsColor(this@EntryUsedChemical,R.color.ub__transparent,R.color.ub__transparent,false)

        onBackPressedDispatcher.addCallback(this){
//            Do Nothing mean Block backpress
        }

        observeUpdateFilledChemical()

        visitID = intent.getStringExtra("visit_id")?:"0"
        userId = sharePreference.getString(PrefKeys.userId)

        val json = intent.getStringExtra(EXTRA_CHEMICALS).orEmpty()
        val type = object : TypeToken<List<VisitChemicals>>() {}.type
        val chemicals: List<VisitChemicals> = Gson().fromJson(json, type) ?: emptyList()

        val uiList = chemicals.map {
            ChemicalItemUI(
                chemicalId = it.id,
                inventory_id = it.inventoryId,
                chemicalName = it.chemicalName?:"",
                availableQty = it.quantity?:"",
                unit = it.unit?:"",
                usedQty = ""
            )
        }

        setUpChemicalUsedAdapter(uiList)

        binding.btnSubmit.setOnClickListener {

            val invalid = chemicalList.firstOrNull {

                val used = it.usedQty.toDoubleOrNull() ?: 0.0

                used > (it.availableQty.toDoubleOrNull() ?: 0.0)
            }

            if (invalid != null) {

                showToast("${invalid.chemicalName} exceeds available quantity")

                return@setOnClickListener
            }

            val request = UpdateFilledChemicalRequestBody(
                visitId = visitID.toInt(),
                userId = userId.toInt(),
                filledList = chemicalList.map {
                    UpdateFilledChemical(
                        assignedInventoryId = it.inventory_id,
                        usedQty = it.usedQty.toDoubleOrNull() ?: 0.0
                    )
                }
            )
            viewModel.updateFilledChemical(request)
        }



    }

    private fun setUpChemicalUsedAdapter(chemicals: List<ChemicalItemUI>){
        chemicalList = chemicals.toMutableList()

        binding.rvChemicals.apply {
            layoutManager = LinearLayoutManager(this@EntryUsedChemical)
            setHasFixedSize(true)
            chemicalAdapter = ChemicalUsedAdapter { changedItem ->
                val index = chemicalList.indexOfFirst {
                    it.chemicalId == changedItem.chemicalId
                }
                if (index != -1) {
                    chemicalList[index] = changedItem
                }
                updateFilledState()
            }
            adapter = chemicalAdapter
        }
        chemicalAdapter.submitList(chemicals)
        updateFilledState()
    }

    private fun observeUpdateFilledChemical() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiStateUpdateFilledVisitChemical.collect { state ->
                    when (state) {
                        is UIState.Idle -> {
                            ProgressDialogUtil.dismiss()
                        }

                        is UIState.Loading -> {
                            ProgressDialogUtil.showAleartLoadingProgress(this@EntryUsedChemical,lifecycleScope,"Please wait!...","Please wait we are scheduling gel service")
                        }

                        is UIState.Success -> {
                            ProgressDialogUtil.dismiss()
                            viewModel.resetuiStateUpdateFilledVisitChemical()
                            setResult(
                                RESULT_OK,
                                Intent().putExtra("chemical_updated",true)
                            )
                            finish()
                            finishSlideActivity()
                        }

                        is UIState.Error -> {
                            ProgressDialogUtil.dismiss()
                            CommonMethods.alertErrorOrValidationDialog(this@EntryUsedChemical,state.message)
                            viewModel.resetuiStateUpdateFilledVisitChemical()
                        }
                    }
                }
            }
        }
    }

    private fun updateFilledState() {

        val filled = chemicalList.count {
            it.usedQty.isNotBlank()
        }

        binding.tvFilledCount.text = "$filled of ${chemicalList.size}"

        val allFilled = filled == chemicalList.size

        binding.btnSubmit.isEnabled = allFilled

        binding.btnSubmit.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this@EntryUsedChemical,
                    if (allFilled)
                        R.color.color_primary
                    else
                        R.color.disable_btn
                )
            )

        binding.btnSubmit.text =
            if (allFilled)
                "Save Chemical Usage"
            else
                "Filled $filled/${chemicalList.size}"
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            val view = currentFocus
            if (view is EditText) {
                val outRect = Rect()
                view.getGlobalVisibleRect(outRect)
                if (!outRect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                    val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(view.windowToken, 0)
                    view.clearFocus()
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

}