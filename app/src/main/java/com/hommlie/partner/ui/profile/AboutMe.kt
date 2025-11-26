package com.hommlie.partner.ui.profile

import android.Manifest
import android.app.DatePickerDialog
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.hommlie.partner.R
import com.hommlie.partner.apiclient.UIState
import com.hommlie.partner.databinding.ActivityAboutMeBinding
import com.hommlie.partner.model.BankDetails
import com.hommlie.partner.model.ContactDetails
import com.hommlie.partner.model.PersonalDetails
import com.hommlie.partner.utils.CommonMethods
import com.hommlie.partner.utils.PrefKeys
import com.hommlie.partner.utils.ProgressDialogUtil
import com.hommlie.partner.utils.SharePreference
import com.hommlie.partner.utils.setupToolbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar
import javax.inject.Inject

@AndroidEntryPoint
class AboutMe : AppCompatActivity() {

    private lateinit var binding : ActivityAboutMeBinding

    private val viewModel : AboutMeViewModel by viewModels()

    private lateinit var contact_editTexts: List<EditText>
    private lateinit var bank_editTexts: List<EditText>
    private lateinit var personal_editTexts: List<EditText>

    private val selectedImages = mutableListOf<Uri>()
    private var cameraImageUri: Uri? = null

    private val hashMap = HashMap<String,String>()

    @Inject
    lateinit var sharePreference: SharePreference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAboutMeBinding.inflate(layoutInflater)
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
        setupToolbar(toolbarView, "About Me", this, R.color.activity_bg, R.color.black)

        Glide.with(this).load(sharePreference.getString(PrefKeys.userProfile)).placeholder(R.drawable.ic_placeholder_profile).into(binding.ivProfile)
        binding.tvName.text = sharePreference.getString(PrefKeys.userName,"")?.replace(",","")
        binding.tvEmpcode.text = "Emp Code - ${sharePreference.getString(PrefKeys.emp_code)}"


        observCardClicks()
        observeFullDetails()
        observeUpdateProfilePhoto()
        observerContactDetailsUIState()
        observerBankDetailsUIState()
        observerPersonalDetailsUIState()

        hashMap["user_id"] = sharePreference.getString(PrefKeys.userId)
        viewModel.fetchUserFullDetails(hashMap)

        contact_editTexts = listOf(
           binding.etPersonalEmail,
            binding.etAlternateEmail,
            binding.etAlternateMobile
        )

        bank_editTexts = listOf(
            binding.etAccounttype,
            binding.etBankname,
            binding.etBranchanme,
            binding.etAccholdername,
            binding.etAccnumber,
            binding.etIfsccode
        )

        personal_editTexts = listOf(
            binding.etSalution,
            binding.etFirstname,
            binding.etMiddlename,
            binding.etLastname,
            binding.etGender,
            binding.etMartialstatus
        )



        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isEditable.collect { editable ->
                    toggleEditTexts(editable,contact_editTexts)
                    toggleIcons(editable, binding.ivEditcontact, binding.ivSavecontact, binding.ivClosecontact)
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.personalisEditable.collect { editable ->
                    toggleEditTexts(editable,personal_editTexts)
                    binding.etDob.isEnabled = editable
                    toggleIcons(editable, binding.ivEditpersonal, binding.ivSavepersonal, binding.ivClosepersonal)
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.bankisEditable.collect { editable ->
                    toggleEditTexts(editable,bank_editTexts)
                    toggleIcons(editable, binding.ivEditbank, binding.ivSavebank, binding.ivClosebank)
                }
            }
        }

        binding.ivProfile.setOnClickListener {
            showAttachmentOptions()
        }


//        contact
        binding.ivEditcontact.setOnClickListener {
            viewModel.toggleEditable(true)
        }
        binding.ivSavecontact.setOnClickListener {
            val contactData = ContactDetails(
                personalEmail = binding.etPersonalEmail.text.toString(),
                mobile = binding.etMobile.text.toString(),
                alternateEmail = binding.etAlternateEmail.text.toString(),
                alternateMobile = binding.etAlternateMobile.text.toString()
            )
            viewModel.saveContactData(contactData)
        }
        binding.ivClosecontact.setOnClickListener {
            viewModel.toggleEditable(false)
        }


//        personal
        binding.ivEditpersonal.setOnClickListener {
            viewModel.togglepersonalEditable(true)
        }
        binding.ivSavepersonal.setOnClickListener {
            val details = PersonalDetails(
                salutation = binding.etSalution.text.toString(),
                fullName = listOfNotNull(
                        binding.etFirstname.text?.toString()?.trim(),
                binding.etMiddlename.text?.toString()?.trim(),
                binding.etLastname.text?.toString()?.trim()
            ).filter { it.isNotBlank() }
                .joinToString(", "),
                name = "",
                nationality = "Indian",
                gender = binding.etGender.text.toString(),
                maritalStatus = binding.etMartialstatus.text.toString(),
                dateOfBirth = binding.etDob.text.toString(),
                profilePhoto = ""
            )
            viewModel.savePersonalData(details)
        }
        binding.ivClosepersonal.setOnClickListener {
            viewModel.togglepersonalEditable(false)
        }


//        bank
        binding.ivEditbank.setOnClickListener {
            viewModel.toggleBankEditable(true)
        }
        binding.ivSavebank.setOnClickListener {
            val bank = BankDetails(
                accountType = binding.etAccounttype.text.toString(),
                bankName = binding.etBankname.text.toString(),
                branchName = binding.etBranchanme.text.toString(),
                accountHolderName = binding.etAccholdername.text.toString(),
                accountNumber = binding.etAccnumber.text.toString(),
                ifscCode = binding.etIfsccode.text.toString()
            )
            viewModel.saveBankData(bank)
        }
        binding.ivClosebank.setOnClickListener {
            viewModel.toggleBankEditable(false)
        }



        binding.etDob.setOnClickListener {
            val calendar = Calendar.getInstance()

            val year = calendar[Calendar.YEAR]
            val month = calendar[Calendar.MONTH]
            val day = calendar[Calendar.DAY_OF_MONTH]

            // Subtract 18 years for max selectable date
            val maxSelectableCalendar = Calendar.getInstance()
            maxSelectableCalendar.set(year - 18, month, day)

            val datePickerDialog = DatePickerDialog(
                this@AboutMe,
                { _, selectedYear, selectedMonth, selectedDay ->
                    val d = if (selectedDay < 10) "0$selectedDay" else "$selectedDay"
                    val m = if (selectedMonth + 1 < 10) "0${selectedMonth + 1}" else "${selectedMonth + 1}"
                    val date = "$d-$m-$selectedYear"
                    binding.etDob.setText(date)

                    // Calculate age
                    val dob = Calendar.getInstance()
                    dob.set(selectedYear, selectedMonth, selectedDay)

                    val today = Calendar.getInstance()

                    var age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR)
                    if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
                        age--
                    }

                    println("Selected Age: $age")
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


    }


    fun observCardClicks(){
        binding.mcvPersonal.setOnClickListener {
            binding.mcvPersonaldetails.visibility = View.VISIBLE
            binding.mcvContactdetails.visibility = View.GONE
            binding.mcvBakdetails.visibility = View.GONE
        }
        binding.mcvContact.setOnClickListener {
            binding.mcvPersonaldetails.visibility = View.GONE
            binding.mcvContactdetails.visibility = View.VISIBLE
            binding.mcvBakdetails.visibility = View.GONE
        }
        binding.mcvBank.setOnClickListener {
            binding.mcvPersonaldetails.visibility = View.GONE
            binding.mcvContactdetails.visibility = View.GONE
            binding.mcvBakdetails.visibility = View.VISIBLE
        }
    }



    private fun toggleEditTexts(editable: Boolean, editTexts: List<EditText>) {
        for (et in editTexts) {
            et.isEnabled = editable
            et.isFocusable = editable
            et.isFocusableInTouchMode = editable
            et.isCursorVisible = editable
            if (!editable) et.clearFocus()
        }
        if (editable) editTexts[0].requestFocus()
    }

    private fun toggleIcons(
        editable: Boolean,
        edit: ImageView,
        save: ImageView,
        close: ImageView
    ) {
        edit.visibility = if (editable) View.GONE else View.VISIBLE
        save.visibility = if (editable) View.VISIBLE else View.GONE
        close.visibility = if (editable) View.VISIBLE else View.GONE
    }


    private fun observerPersonalDetailsUIState(){
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.personalDetailsUIState.collect { state ->
                    when (state) {
                        is UIState.Idle -> {}
                        is UIState.Loading -> ProgressDialogUtil.showAleartLoadingProgress(this@AboutMe,lifecycleScope,"Loading...","Please wait while we are saving your data.")
                        is UIState.Success -> {

                            val details = state.data
                            // update UI fields
                            binding.etSalution.setText(details.salutation)
                            val fullName = details.name ?: ""
                            // Normalize string: replace commas and "and" with space
                            val parts = fullName
                                .replace("and", " ")
                                .replace(",", " ")
                                .trim()
                                .split("\\s+".toRegex()) // split by one or more spaces

                            val firstName = parts.getOrNull(0) ?: ""
                            val middleName = if (parts.size > 2) parts.subList(1, parts.size - 1).joinToString(" ") else ""
                            val lastName = if (parts.size > 1) parts.last() else ""

                            binding.etFirstname.setText(firstName)
                            binding.etMiddlename.setText(middleName)
                            binding.etLastname.setText(lastName)
                            binding.etGender.setText(details.gender)
                            binding.etMartialstatus.setText(details.maritalStatus)
                            binding.etDob.setText(details.dateOfBirth)
//                            binding.etNationality.setText(details.nationality)

                            details.profilePhoto?.let { sharePreference.setString(PrefKeys.userProfile, it) }
                            sharePreference.setString(PrefKeys.userName,details.name?:"")

                            Glide.with(this@AboutMe).load(sharePreference.getString(PrefKeys.userProfile)).placeholder(R.drawable.ic_placeholder_profile).into(binding.ivProfile)
                            binding.tvName.text = sharePreference.getString(PrefKeys.userName,"")?.replace(",","")
                            CommonMethods.alertErrorOrValidationDialog(this@AboutMe,"Your personal details updated successfully")
                            viewModel.resetUIPersonal()
                            ProgressDialogUtil.dismiss()
                            viewModel.togglepersonalEditable(false)

                        }
                        is UIState.Error -> {
                            ProgressDialogUtil.dismiss()
                            viewModel.resetUIPersonal()
                            CommonMethods.alertErrorOrValidationDialog(this@AboutMe,state.message)
                        }
                    }
                }
            }
        }

    }

    private fun observerContactDetailsUIState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.contactDetailsUIState.collect { state ->
                    when (state) {
                        is UIState.Idle -> { }

                        is UIState.Loading -> {
                            ProgressDialogUtil.showAleartLoadingProgress(
                                this@AboutMe,
                                lifecycleScope,
                                "Loading...",
                                "Please wait while we are saving your contact details."
                            )
                        }

                        is UIState.Success -> {
                            val details = state.data

                            //  set all contact fields
                            binding.etPersonalEmail.setText(details.personalEmail)
                            binding.etAlternateEmail.setText(details.alternateEmail ?: "")
                            binding.etAlternateMobile.setText(details.alternateMobile ?: "")

                            sharePreference.setString(PrefKeys.userEmail,details.personalEmail?:"")

                            CommonMethods.alertErrorOrValidationDialog(this@AboutMe,"Your contact details updated successfully")

                            ProgressDialogUtil.dismiss()
                            viewModel.resetUIContact()
                            viewModel.toggleEditable(false)
                        }

                        is UIState.Error -> {
                            ProgressDialogUtil.dismiss()
                            viewModel.resetUIContact()
                            CommonMethods.alertErrorOrValidationDialog(
                                this@AboutMe,
                                state.message
                            )
                        }
                    }
                }
            }
        }
    }


    private fun observerBankDetailsUIState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.bankDetailsUIState.collect { state ->
                    when (state) {
                        is UIState.Idle -> {

                        }

                        is UIState.Loading -> {
                            ProgressDialogUtil.showAleartLoadingProgress(
                                this@AboutMe,
                                lifecycleScope,
                                "Loading...",
                                "Please wait while we are saving your bank details."
                            )
                        }

                        is UIState.Success -> {
                            val details = state.data

                            binding.etAccounttype.setText(details.accountType)
                            binding.etBankname.setText(details.bankName)
                            binding.etBranchanme.setText(details.branchName)
                            binding.etAccholdername.setText(details.accountHolderName)
                            binding.etAccnumber.setText(details.accountNumber)
                            binding.etIfsccode.setText(details.ifscCode)

                            CommonMethods.alertErrorOrValidationDialog(this@AboutMe,"Change Bank account request sent succesfully.\nYour request is in under review")
                            ProgressDialogUtil.dismiss()
                            viewModel.resetUIBank()
                            viewModel.toggleBankEditable(false)
                        }

                        is UIState.Error -> {
                            ProgressDialogUtil.dismiss()
                            viewModel.resetUIBank()
                            CommonMethods.alertErrorOrValidationDialog(
                                this@AboutMe,
                                state.message
                            )
                        }
                    }
                }
            }
        }
    }



    private fun observeFullDetails() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.getFullDetailsUIState.collect { state ->
                    when (state) {
                        is UIState.Idle -> {

                        }
                        is UIState.Loading -> ProgressDialogUtil.showAleartLoadingProgress(
                            this@AboutMe,
                            lifecycleScope,
                            "Loading...",
                            "Please wait while we fetching your details."
                        )
                        is UIState.Success -> {

                            val details = state.data

                            // -------- Personal Details --------
                            binding.etSalution.setText(details.personalDetails.salutation)
                            val fullName = details.personalDetails.fullName ?: ""

                            // Normalize string: replace commas and "and" with space
                            val parts = fullName
                                .replace("and", " ")
                                .replace(",", " ")
                                .trim()
                                .split("\\s+".toRegex()) // split by one or more spaces

                            val firstName = parts.getOrNull(0) ?: ""
                            val middleName = if (parts.size > 2) parts.subList(1, parts.size - 1).joinToString(" ") else ""
                            val lastName = if (parts.size > 1) parts.last() else ""

                            binding.etFirstname.setText(firstName)
                            binding.etMiddlename.setText(middleName)
                            binding.etLastname.setText(lastName)

                            binding.etGender.setText(details.personalDetails.gender)
                            binding.etMartialstatus.setText(details.personalDetails.maritalStatus)
                            binding.etDob.setText(details.personalDetails.dateOfBirth)
//                            binding.etNationality.setText(details.personalDetails.nationality)

                            sharePreference.setString(PrefKeys.userProfile,details.personalDetails.profilePhoto?:"")
                            sharePreference.setString(PrefKeys.userName,details.personalDetails.fullName?:"")
                            sharePreference.setString(PrefKeys.userEmail,details.contactDetails.personalEmail?:"")
                            sharePreference.setString(PrefKeys.userProfile,details.personalDetails.profilePhoto?:"")

                            Glide.with(this@AboutMe).load(sharePreference.getString(PrefKeys.userProfile)).placeholder(R.drawable.ic_placeholder_profile).into(binding.ivProfile)
                            binding.tvName.text = sharePreference.getString(PrefKeys.userName,"")?.replace(",","")

                            // -------- Contact Details --------
                            binding.etPersonalEmail.setText(details.contactDetails.personalEmail)
                            var mobile = details.contactDetails.mobile ?: ""

                            if (mobile.startsWith("+91") && !mobile.startsWith("+91 ")) {
                                mobile = mobile.replaceFirst("+91", "+91 ")
                            }
                            binding.etMobile.setText(mobile)

                            binding.etAlternateEmail.setText(details.contactDetails.alternateEmail ?: "")
                            binding.etAlternateMobile.setText(details.contactDetails.alternateMobile ?: "")

                            // -------- Bank Details --------
                            binding.etBankname.setText(details.bankDetails.bankName)
                            binding.etAccnumber.setText(details.bankDetails.accountNumber)
                            binding.etIfsccode.setText(details.bankDetails.ifscCode)
                            binding.etBranchanme.setText(details.bankDetails.branchName)
                            binding.etAccounttype.setText(details.bankDetails.accountType)

                            ProgressDialogUtil.dismiss()
                            viewModel.resetUIfetchAllDetails()
                        }
                        is UIState.Error -> {
                            ProgressDialogUtil.dismiss()
                            viewModel.resetUIfetchAllDetails()
                            if (state.message.equals("User Not Found", true) ||
                                state.message.equals("Employee Not Found", true)
                            ) {
                                Toast.makeText(this@AboutMe, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show()
                                CommonMethods.logOut(sharePreference, this@AboutMe)
                                return@collect
                            }
//                            CommonMethods.alertErrorOrValidationDialog(this@AboutMe, state.message)
                        }
                    }
                }
            }
        }
    }

    private fun observeUpdateProfilePhoto() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.updateProfilePhotoUIState.collect { state ->
                    when (state) {
                        is UIState.Idle -> {

                        }
                        is UIState.Loading -> ProgressDialogUtil.showAleartLoadingProgress(
                            this@AboutMe,
                            lifecycleScope,
                            "Loading...",
                            "Please wait while we updating your profile photo"
                        )
                        is UIState.Success -> {

                            val response = state.data
                            if (response.status==1){
                                sharePreference.setString(PrefKeys.userProfile,response.message?:"")
                                Glide.with(this@AboutMe).load(sharePreference.getString(PrefKeys.userProfile)).placeholder(R.drawable.ic_placeholder_profile).into(binding.ivProfile)
                            }

                            ProgressDialogUtil.dismiss()
                            viewModel.resetUIUpdateProfilePhoto()
                        }
                        is UIState.Error -> {
                            ProgressDialogUtil.dismiss()
                            viewModel.resetUIUpdateProfilePhoto()
                            CommonMethods.alertErrorOrValidationDialog(this@AboutMe, state.message)
                        }
                    }
                }
            }
        }
    }


    private fun showAttachmentOptions() {
        val options = arrayOf("Take Photo", "Choose from Gallery", "Cancel")

        AlertDialog.Builder(this@AboutMe)
            .setTitle("Select Option")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> requestCameraPermission()
                    1 -> openGallery()
                    else -> dialog.dismiss()
                }
            }
            .show()
    }

    private fun requestCameraPermission() {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun openGallery() {
        // Use GetContent instead of GetMultipleContents
        galleryLauncher.launch("image/*")
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) launchCameraInternal()
        else Toast.makeText(this@AboutMe, "Camera permission denied", Toast.LENGTH_SHORT).show()
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            ProgressDialogUtil.showAleartLoadingProgress(
                this@AboutMe,
                lifecycleScope,
                "Processing...",
                "Please wait while we are processing your image"
            )

            lifecycleScope.launch {
                val compressedUri = withContext(Dispatchers.IO) {
                    CommonMethods.compressImageFromUri(this@AboutMe, it)
                }

                compressedUri?.let { finalUri ->
                    updateImage(finalUri) // update single image
                }

                ProgressDialogUtil.dismiss()
            }
        }
    }

    private fun launchCameraInternal() {
        val imageFile = File.createTempFile("photo_", ".jpg", cacheDir)
        val uri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            imageFile
        )
        cameraImageUri = uri
        takePhotoLauncher.launch(uri)
    }

    private val takePhotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && cameraImageUri != null) {
            lifecycleScope.launch {
                ProgressDialogUtil.showAleartLoadingProgress(
                    this@AboutMe,
                    lifecycleScope,
                    "Processing...",
                    "Please wait while we are processing your image"
                )

                val compressedUri = withContext(Dispatchers.IO) {
                    CommonMethods.compressImageFromUri(this@AboutMe, cameraImageUri!!)
                }

                compressedUri?.let { finalUri ->
                    updateImage(finalUri) // update single image
                }

                ProgressDialogUtil.dismiss()
            }
        }
    }

    private fun updateImage(uri: Uri) {
        val profilePhotoPart = uriToMultipart(uri, "profile_photo")

        val userId = sharePreference.getString(PrefKeys.userId) ?: ""
        val userIdBody = userId.toRequestBody("text/plain".toMediaTypeOrNull())

        viewModel.updateProfilePhoto(userIdBody, profilePhotoPart)
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

    private fun uriToMultipart(uri: Uri, partName: String): MultipartBody.Part? {
        return try {
            val file: File = if (uri.scheme == "file") {
                File(uri.path!!)
            } else {
                // Copy content:// to temp file
                val fileName = getFileName(uri) ?: "${System.currentTimeMillis()}.jpg"
                val tempFile = File(cacheDir, fileName)
                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }
                tempFile
            }

            Log.d("UploadDebug", " Using file: ${file.absolutePath}, size=${file.length()} bytes")

            if (file.length() == 0L) {
                Log.e("UploadDebug", " File is empty — aborting Multipart creation")
                return null
            }

            val requestBody = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
            MultipartBody.Part.createFormData(partName, file.name, requestBody)
        } catch (e: Exception) {
            Log.e("UploadDebug", " uriToMultipart failed: ${e.message}")
            null
        }
    }




}