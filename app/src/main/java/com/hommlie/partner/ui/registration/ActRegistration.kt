package com.hommlie.partner.ui.registration

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.hommlie.partner.R
import com.hommlie.partner.apiclient.UIState
import com.hommlie.partner.databinding.ActivityActRegistrationBinding
import com.hommlie.partner.utils.PrefKeys
import com.hommlie.partner.utils.ProgressDialogUtil
import com.hommlie.partner.utils.SharePreference
import com.hommlie.partner.utils.setupToolbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar
import javax.inject.Inject

@AndroidEntryPoint
class ActRegistration : AppCompatActivity() {

    private lateinit var binding : ActivityActRegistrationBinding
    private val viewModel : RegistrationViewModel by viewModels()
    private var selectedDocumentUri: Uri? = null
    private var selectedProfileUri: Uri? = null


    @Inject
    lateinit var sharePreference: SharePreference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
        binding = ActivityActRegistrationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val toolbarView = binding.root.findViewById<View>(R.id.include_toolbar)
        setupToolbar(toolbarView, "Registration Form", this, R.color.transparent, R.color.black)

        binding.etMobile.setText(sharePreference.getString(PrefKeys.userMobile))



        binding.dateOfBirth.setOnClickListener {
            val calendar = Calendar.getInstance()

            val year = calendar[Calendar.YEAR]
            val month = calendar[Calendar.MONTH]
            val day = calendar[Calendar.DAY_OF_MONTH]

            // Subtract 18 years for max selectable date
            val maxSelectableCalendar = Calendar.getInstance()
            maxSelectableCalendar.set(year - 18, month, day)

            val datePickerDialog = DatePickerDialog(
                this@ActRegistration,
                { _, selectedYear, selectedMonth, selectedDay ->
                    val d = if (selectedDay < 10) "0$selectedDay" else "$selectedDay"
                    val m = if (selectedMonth + 1 < 10) "0${selectedMonth + 1}" else "${selectedMonth + 1}"
                    val date = "$d-$m-$selectedYear"
                    binding.dateOfBirth.setText(date)

                    // Calculate age
                    val dob = Calendar.getInstance()
                    dob.set(selectedYear, selectedMonth, selectedDay)

                    val today = Calendar.getInstance()

                    var age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR)
                    if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
                        age--
                    }

                    println("Selected Age: $age")
                    binding.age.setText(age.toString())
                    // You can also store/display this age in a TextView if needed
                },
                year - 18, month, day // Start picker at 18 years ago
            )

            // Set maximum selectable date to exactly 18 years ago from today
            datePickerDialog.datePicker.maxDate = maxSelectableCalendar.timeInMillis

            // Optional: Set minimum year (e.g., 60 years ago)
//            val minSelectableCalendar = Calendar.getInstance()
//            minSelectableCalendar.set(year - 60, month, day)
//            datePickerDialog.datePicker.minDate = minSelectableCalendar.timeInMillis

            datePickerDialog.show()
        }

        // Observe gender list from ViewModel
        viewModel.genderList.observe(this@ActRegistration) { genders ->
            val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, genders)
            binding.actvGender.setAdapter(adapter)
        }

        // Show dropdown on click
        binding.actvGender.setOnClickListener {
            binding.actvGender.showDropDown()
        }

        // Handle selection and send to ViewModel
        binding.actvGender.setOnItemClickListener { parent, _, position, _ ->
            val selected = parent.getItemAtPosition(position).toString()
            viewModel.setSelectedGender(selected)
        }

//        // Observe selected gender if needed
//        viewModel.selectedGender.observe(this) { gender ->
//            Toast.makeText(this, "Selected: $gender", Toast.LENGTH_SHORT).show()
//        }

        // Optional: Prevent keyboard from showing
        binding.actvGender.setOnTouchListener { _, _ ->
            binding.actvGender.showDropDown()
            true
        }

        observeWorkZones()

        setFieldListeners()
        observeValidationErrors()

        viewModel.getWorkZones()

        viewModel.termsError.observe(this) { error ->
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
            }
        }



        binding.btnLogin.setOnClickListener {
            val isValid = viewModel.validateAllFields(
                firstName = binding.edtFirstname.text.toString(),
                lastName = binding.edtLastname.text.toString(),
                dob = binding.dateOfBirth.text.toString(),
                age = binding.age.text.toString(),
                gender = binding.actvGender.text.toString(),
                email = binding.edtEmail.text.toString(),
                workZone = binding.actvWorkzones.text.toString(),
                exp = binding.edtExp.text.toString()
            )

            //  checkbox validation
            viewModel.validateTerms(binding.checkbox.isChecked)

            if (isValid && binding.checkbox.isChecked) {
                callRegisterApi()
            }
        }


        viewModel.registerState.observe(this) { state ->
            when (state) {
                is UIState.Idle -> {}
                is UIState.Loading -> ProgressDialogUtil.showAleartLoadingProgress(this,lifecycleScope,"Registering...","Please wait while we are saving your details for verification.")
                is UIState.Success -> {

                    sharePreference.setInt(PrefKeys.is_reg_form_sub,1)

                    ProgressDialogUtil.dismiss()
                    viewModel.resetRegisterUserUi()
                    Toast.makeText(this, state.data.message, Toast.LENGTH_LONG).show()
                    lifecycleScope.launch {
                        delay(500)
                        val intent = Intent(this@ActRegistration,ActKycPending::class.java)
                        startActivity(intent)
                        finish()
                        finishAffinity()
                    }


                }
                is UIState.Error -> {
                    ProgressDialogUtil.dismiss()
                    viewModel.resetRegisterUserUi()
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
            }
        }



        // Document button
        binding.btnUploadDoc.setOnClickListener {
            pickDocumentLauncher.launch("*/*") // all files, later restrict
            // 👉 safer: pickDocumentLauncher.launch("application/pdf") for pdf
            // or pickDocumentLauncher.launch("image/*") for jpg/png
        }

        // Profile photo button
        binding.btnUploadProfile.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }


    }


    private fun observeWorkZones() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.workZones.collect { state ->
                    when (state) {
                        is UIState.Idle -> {

                        }

                        is UIState.Loading -> {
                            ProgressDialogUtil.showAleartLoadingProgress(this@ActRegistration, lifecycleScope, "Loading...", "")
                        }

                        is UIState.Success -> {
                            val zones = state.data.data ?: emptyList()

                            // Extract only names for adapter
                            val zoneNames = zones.map { it.name }

                            // Name to ID mapping
                            val zoneMap = zones.associateBy({ it.name }, { it.id })

                            val adapter = ArrayAdapter(
                                this@ActRegistration,
                                android.R.layout.simple_dropdown_item_1line,
                                zoneNames
                            )
                            binding.actvWorkzones.setAdapter(adapter)

                            // Show dropdown on click
                            binding.actvWorkzones.setOnClickListener {
                                binding.actvWorkzones.showDropDown()
                            }

                            // Handle selection
                            binding.actvWorkzones.setOnItemClickListener { parent, _, position, _ ->
                                val selectedZoneName = parent.getItemAtPosition(position).toString()
                                val selectedZoneId = zoneMap[selectedZoneName] ?: -1

                                Toast.makeText(this@ActRegistration, "Selected: $selectedZoneName (ID: $selectedZoneId)", Toast.LENGTH_SHORT).show()

                                // Pass selected ID to ViewModel
                                viewModel.setSelectedWorkZoneId(selectedZoneId)
                            }

                            // Prevent keyboard from opening
                            binding.actvWorkzones.setOnTouchListener { _, _ ->
                                binding.actvWorkzones.showDropDown()
                                true
                            }

                            viewModel.resetGetWorkZones()
                            ProgressDialogUtil.dismiss()
                        }

                        is UIState.Error -> {
                            viewModel.resetGetWorkZones()
                            ProgressDialogUtil.dismiss()

                            val defaultZone = listOf("Default Zone")
                            val adapter = ArrayAdapter(
                                this@ActRegistration,
                                android.R.layout.simple_dropdown_item_1line,
                                defaultZone
                            )
                            binding.actvWorkzones.setAdapter(adapter)

                            binding.actvWorkzones.setOnClickListener {
                                binding.actvWorkzones.showDropDown()
                            }

                            binding.actvWorkzones.setOnItemClickListener { parent, _, position, _ ->
                                val selectedZone = parent.getItemAtPosition(position).toString()
                                Toast.makeText(
                                    this@ActRegistration,
                                    "Selected: $selectedZone",
                                    Toast.LENGTH_SHORT
                                ).show()

                                // Default zone id = -1 (not valid)
                                viewModel.setSelectedWorkZoneId(-1)
                            }

                            binding.actvWorkzones.setOnTouchListener { _, _ ->
                                binding.actvWorkzones.showDropDown()
                                true
                            }
                        }
                    }
                }
            }
        }
    }



    private fun callRegisterApi() {

        // Required fields check
        if (selectedDocumentUri == null) {
            Toast.makeText(this, "Please upload Aadhaar/PAN document", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedProfileUri == null) {
            Toast.makeText(this, "Please upload Profile Photo", Toast.LENGTH_SHORT).show()
            return
        }

        val name = listOfNotNull(
            binding.edtFirstname.text?.toString()?.trim(),
            binding.edtMiddlename.text?.toString()?.trim(),
            binding.edtLastname.text?.toString()?.trim()
        ).filter { it.isNotBlank() }
            .joinToString(", ")

        val dob = binding.dateOfBirth.text.toString().trim()
        val age = binding.age.text.toString().trim().toInt()
        val gender = binding.actvGender.text.toString().trim()
        val email = binding.edtEmail.text.toString().trim()
        val workZone = binding.actvWorkzones.text.toString().trim()
        val expInYear = binding.edtExp.text.toString().trim().toFloat()

        // Convert Uris to Multipart
        val profilePart = selectedProfileUri?.let { uriToMultipart(it, "profilePhoto") }
        val documentPart = selectedDocumentUri?.let { uriToMultipart(it, "document") }

        viewModel.registerUser(sharePreference.getString(PrefKeys.userId),
            name, dob, age, gender, email, viewModel.selectedWorkZoneId.value, expInYear,
            profilePhoto = profilePart,
            document = documentPart
        )
    }

    private fun setFieldListeners() {
        binding.edtFirstname.doAfterTextChanged { viewModel.validateFirstName(it.toString()) }
        binding.edtLastname.doAfterTextChanged { viewModel.validateLastName(it.toString()) }
        binding.dateOfBirth.doAfterTextChanged { viewModel.validateDob(it.toString()) }
        binding.age.doAfterTextChanged { viewModel.validateAge(it.toString()) }
        binding.actvGender.doAfterTextChanged { viewModel.validateGender(it.toString()) }
        binding.edtEmail.doAfterTextChanged { viewModel.validateEmail(it.toString()) }
        binding.actvWorkzones.doAfterTextChanged { viewModel.validateWorkZone(it.toString()) }
        binding.edtExp.doAfterTextChanged { viewModel.validateExp(it.toString()) }
    }

    private fun observeValidationErrors() {
        viewModel.firstNameError.observe(this) { binding.edtFirstname.error = it }
        viewModel.lastNameError.observe(this) { binding.edtLastname.error = it }
        viewModel.dobError.observe(this) { binding.dateOfBirth.error = it }
        viewModel.ageError.observe(this) { binding.age.error = it }
        viewModel.genderError.observe(this) { binding.actvGender.error = it }
        viewModel.emailError.observe(this) { binding.edtEmail.error = it }
        viewModel.workZoneError.observe(this) { binding.actvWorkzones.error = it }
        viewModel.expError.observe(this) { binding.edtExp.error = it }
    }


    fun TextView.setDrawableEnd(drawableRes: Int?) {
        val drawable = drawableRes?.let { ContextCompat.getDrawable(context, it) }
        setCompoundDrawablesWithIntrinsicBounds(null, null, drawable, null)
    }


    // Document picker
    private val pickDocumentLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                selectedDocumentUri = it
                val fileName = getFileName(it)
                binding.btnUploadDoc.text = "Change Document"
                binding.tvDocName.text = (fileName ?: "Document Selected") + "   "
                binding.tvDocName.setDrawableEnd(R.drawable.ic_right_dispatch)
            }
        }

    // Profile picker
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                selectedProfileUri = it
                val fileName = getFileName(it)
                binding.btnUploadProfile.text = "Change Photo"
                binding.tvPhoto.text = (fileName ?: "Document Selected") + "   "
//                binding.ivProfilePreview.setImageURI(it) // Preview
                binding.tvPhoto.setDrawableEnd(R.drawable.ic_right_dispatch)
            }
        }


    private fun uriToMultipart(uri: Uri, partName: String): MultipartBody.Part? {
        val contentResolver = applicationContext.contentResolver
        val inputStream = contentResolver.openInputStream(uri) ?: return null

        val fileName = getFileName(uri) ?: "$partName-${System.currentTimeMillis()}"
        val file = File(cacheDir, fileName)

        // Copy input stream to temp file
        inputStream.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }

        val requestBody = file.asRequestBody(contentResolver.getType(uri)?.toMediaTypeOrNull())
        return MultipartBody.Part.createFormData(partName, file.name, requestBody)
    }






    // Helper to get file name from Uri
    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != -1) {
                result = result?.substring(cut!! + 1)
            }
        }
        return result
    }


}