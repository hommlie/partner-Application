package com.hommlie.partner.adapter

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.hommlie.partner.R
import com.hommlie.partner.apiclient.AppConfig
import com.hommlie.partner.databinding.RowQuestionsBinding
import com.hommlie.partner.model.DaoCollectAllQuestionsOfAllServices
import com.hommlie.partner.model.DaocollectAnswer
import com.hommlie.partner.ui.jobs.ActQuestionary

class QuestionAdaptor(
    private val context: Context
) : ListAdapter<DaoCollectAllQuestionsOfAllServices, QuestionAdaptor.ViewHolder>(DiffCallback()) {

    private val imageSlotMap = mutableMapOf<Pair<Int, Int>, MutableList<String?>>()
    val serviceAnswerMap = mutableMapOf<Int, MutableList<DaocollectAnswer>>()

    inner class ViewHolder(val binding: RowQuestionsBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var textWatcher: TextWatcher? = null
        private var textWatcherNumber: TextWatcher? = null

        fun bind(questionData: DaoCollectAllQuestionsOfAllServices) {
            val position = bindingAdapterPosition

            if (position == RecyclerView.NO_POSITION) return

            val isFirstQuestionOfService =
                position == 0 || getItem(position - 1).service_id != questionData.service_id

            if (isFirstQuestionOfService) {
                binding.tvServiceName.visibility = View.VISIBLE
                binding.tvServiceName.text = questionData.service_name
            } else {
                binding.tvServiceName.visibility = View.GONE
            }

            binding.tvQuestionNO.text = "Qes : ${bindingAdapterPosition + 1}"
            binding.tvQuestion.text = if (questionData.required == "1") questionData.label
            else "${questionData.label} (optional)"

            // Reset views first
            resetViews()

            // Show/hide based on type
            binding.edtAnswer.visibility =
                if (questionData.type == "text") View.VISIBLE else View.GONE
            binding.edtNumberAnswer.visibility =
                if (questionData.type == "number") View.VISIBLE else View.GONE
            binding.card4.visibility =
                if (questionData.type == "file") View.VISIBLE else View.GONE
            binding.radiogroup.visibility =
                if (questionData.type == "radio") View.VISIBLE else View.GONE
            binding.llForchekbox.visibility =
                if (questionData.type == "checkbox") View.VISIBLE else View.GONE

            // Set options
            setRadioOptions(questionData)
            setCheckboxOptions(questionData)

            // Restore previous answer
            restoreAnswer(questionData)

            // Handle image clicks
            binding.iv1.setOnClickListener {
                (context as ActQuestionary).pickImageForQuestion(
                    serviceId = questionData.service_id,
                    questionId = questionData.id,
                    imageSlot = 0,
                    imageView = binding.iv1
                )
            }

            binding.iv2.setOnClickListener {
                (context as ActQuestionary).pickImageForQuestion(
                    serviceId = questionData.service_id,
                    questionId = questionData.id,
                    imageSlot = 1,
                    imageView = binding.iv2
                )
            }

            binding.iv3.setOnClickListener {
                (context as ActQuestionary).pickImageForQuestion(
                    serviceId = questionData.service_id,
                    questionId = questionData.id,
                    imageSlot = 2,
                    imageView = binding.iv3
                )
            }

            binding.iv4.setOnClickListener {
                (context as ActQuestionary).pickImageForQuestion(
                    serviceId = questionData.service_id,
                    questionId = questionData.id,
                    imageSlot = 3,
                    imageView = binding.iv4
                )
            }
        }

        private fun resetViews() {

            textWatcher?.let {
                binding.edtAnswer.removeTextChangedListener(it)
            }
            textWatcher = null
            binding.edtAnswer.setText("")

            textWatcherNumber?.let {
                binding.edtNumberAnswer.removeTextChangedListener(it)
            }
            textWatcherNumber = null
            binding.edtNumberAnswer.setText("")

            binding.radiogroup.setOnCheckedChangeListener(null)
            binding.radiogroup.clearCheck()

            listOf(
                binding.checkbox1,
                binding.checkbox2,
                binding.checkbox3,
                binding.checkbox4
            ).forEach {
                it.setOnCheckedChangeListener(null)
                it.isChecked = false
            }
        }

        private fun setRadioOptions(question: DaoCollectAllQuestionsOfAllServices) {
            if (question.type != "radio" || question.options.isNullOrEmpty()) return
            val options = question.options.split(",")
            val radios = listOf(binding.radio1, binding.radio2, binding.radio3, binding.radio4)
            options.forEachIndexed { index, option ->
                if (index < radios.size) {
                    radios[index].text = option
                    radios[index].visibility = View.VISIBLE
                }
            }
        }

        private fun setCheckboxOptions(question: DaoCollectAllQuestionsOfAllServices) {
            if (question.type != "checkbox" || question.options.isNullOrEmpty()) return
            val options = question.options.split(",")
            val checkBoxes =
                listOf(binding.checkbox1, binding.checkbox2, binding.checkbox3, binding.checkbox4)
            options.forEachIndexed { index, option ->
                if (index < checkBoxes.size) {
                    checkBoxes[index].text = option
                    checkBoxes[index].visibility = View.VISIBLE
                }
            }
        }

        private fun restoreAnswer(
            question: DaoCollectAllQuestionsOfAllServices
        ) {
            val saved = serviceAnswerMap[question.service_id]
                ?.find { it.question_id == question.id }
                ?.answer
                ?: ""

            when (question.type) {

                "text" -> {

                    binding.edtAnswer.setText(saved)

                    textWatcher = object : TextWatcher {

                        override fun beforeTextChanged(
                            s: CharSequence?,
                            start: Int,
                            count: Int,
                            after: Int
                        ) = Unit

                        override fun onTextChanged(
                            s: CharSequence?,
                            start: Int,
                            before: Int,
                            count: Int
                        ) {
                            saveAnswer(
                                question,
                                s?.toString().orEmpty()
                            )
                        }

                        override fun afterTextChanged(
                            s: Editable?
                        ) = Unit
                    }

                    binding.edtAnswer.addTextChangedListener(textWatcher)
                }

                "number" -> {

                    binding.edtNumberAnswer.setText(saved)

                    textWatcherNumber = object : TextWatcher {

                        override fun beforeTextChanged(
                            s: CharSequence?,
                            start: Int,
                            count: Int,
                            after: Int
                        ) = Unit

                        override fun onTextChanged(
                            s: CharSequence?,
                            start: Int,
                            before: Int,
                            count: Int
                        ) {
                            saveAnswer(
                                question,
                                s?.toString().orEmpty()
                            )
                        }

                        override fun afterTextChanged(
                            s: Editable?
                        ) = Unit
                    }

                    binding.edtNumberAnswer.addTextChangedListener(textWatcherNumber)
                }

                "radio" -> {
                    val radios = listOf(
                        binding.radio1,
                        binding.radio2,
                        binding.radio3,
                        binding.radio4
                    )

                    radios.forEach {
                        it.isChecked = it.text.toString() == saved
                    }

                    binding.radiogroup.setOnCheckedChangeListener { group, checkedId ->

                        if (checkedId == -1) return@setOnCheckedChangeListener

                        val rb =
                            group.findViewById<android.widget.RadioButton>(checkedId)

                        saveAnswer(
                            question,
                            rb.text.toString()
                        )
                    }
                }

                "checkbox" -> {
                    val checkBoxes = listOf(
                        binding.checkbox1,
                        binding.checkbox2,
                        binding.checkbox3,
                        binding.checkbox4
                    )

                    val selected = saved
                        .split(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }

                    checkBoxes.forEach { cb ->

                        cb.isChecked = selected.contains(
                            cb.text.toString()
                        )

                        cb.setOnCheckedChangeListener { _, _ ->

                            val combined = checkBoxes
                                .filter { it.isChecked }
                                .joinToString(",") {
                                    it.text.toString()
                                }

                            saveAnswer(
                                question,
                                combined
                            )
                        }
                    }
                }
//                "file" -> {
//
//                    val key = question.service_id to question.id
//
//                    val imageViews = listOf(
//                        binding.iv1,
//                        binding.iv2,
//                        binding.iv3,
//                        binding.iv4
//                    )
//
//                    // Always keep all 4 slots visible
//                    imageViews.forEach { imageView ->
//                        imageView.visibility = View.VISIBLE
//                        Glide.with(imageView).clear(imageView)
//                        imageView.setImageResource(R.drawable.ic_photo_camera)
//                    }
//
//                    /*
//                     * First time:
//                     * Create slot state from saved API answer.
//                     *
//                     * After that:
//                     * imageSlotMap becomes the source of truth.
//                     */
//                    val slots = imageSlotMap.getOrPut(key) {
//
//                        val savedImages = saved
//                            .split(",")
//                            .map { it.trim() }
//                            .filter { it.isNotEmpty() }
//                            .take(4)
//
//                        MutableList<String?>(4) { index ->
//                            savedImages.getOrNull(index)
//                        }
//                    }
//
//                    // Restore each exact slot
//                    imageViews.forEachIndexed { index, imageView ->
//
//                        val imageName = slots.getOrNull(index)
//
//                        if (!imageName.isNullOrBlank()) {
//
//                            Glide.with(imageView)
//                                .load(AppConfig.IMAGE_BASE_URL + imageName)
//                                .placeholder(R.drawable.ic_photo_camera)
//                                .error(R.drawable.ic_photo_camera)
//                                .into(imageView)
//
//                        } else {
//
//                            imageView.setImageResource(
//                                R.drawable.ic_photo_camera
//                            )
//                        }
//                    }
//                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            RowQuestionsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun setImageAnswer(
        serviceId: Int,
        questionId: Int,
        imageSlot: Int,
        imageName: String
    ) {
        if (imageSlot !in 0..3) return

        val question = currentList.find {
            it.service_id == serviceId &&
                    it.id == questionId
        } ?: return

        val key = serviceId to questionId

        val slots = imageSlotMap.getOrPut(key) {
            MutableList(4) { null }
        }

        // Exact slot update
        slots[imageSlot] = imageName

        val answer = slots
            .filterNotNull()
            .filter { it.isNotBlank() }
            .joinToString(",")

        val answers = serviceAnswerMap.getOrPut(serviceId) {
            mutableListOf()
        }

        val index = answers.indexOfFirst {
            it.question_id == questionId
        }

        if (index >= 0) {

            answers[index] = answers[index].copy(
                answer = answer
            )

        } else {

            answers.add(
                DaocollectAnswer(
                    question_id = questionId,
                    question = question.label,
                    answer = answer
                )
            )
        }

        Log.d(
            "IMAGE_STATE",
            "key=$key slots=$slots answer=$answer"
        )
    }

    fun getServiceWiseAnswers(): List<Map<String, Any>> {
        val servicesList = mutableListOf<Map<String, Any>>()
        serviceAnswerMap.forEach { (serviceId, answersList) ->
            servicesList.add(mapOf("service_id" to serviceId, "answers" to answersList))
        }
        Log.d("FINAL_SERVICES_JSON", Gson().toJson(servicesList))
        return servicesList
    }

    fun validateRequiredQuestions(): Boolean {

        for (question in currentList) {

            if (question.required != "1") continue

            val answer = serviceAnswerMap[question.service_id]
                ?.firstOrNull { it.question_id == question.id }
                ?.answer
                ?.trim()

            if (answer.isNullOrEmpty()) {
                return false
            }
        }

        return true
    }

    private fun saveAnswer(question: DaoCollectAllQuestionsOfAllServices, answer: String) {
        val list = serviceAnswerMap.getOrPut(question.service_id) { mutableListOf() }
        val index = list.indexOfFirst { it.question_id == question.id }
        if (index >= 0) list[index] = list[index].copy(answer = answer)
        else list.add(DaocollectAnswer(question.id, question.label, answer))
    }

    class DiffCallback : DiffUtil.ItemCallback<DaoCollectAllQuestionsOfAllServices>() {
        override fun areItemsTheSame(
            oldItem: DaoCollectAllQuestionsOfAllServices,
            newItem: DaoCollectAllQuestionsOfAllServices
        ): Boolean {
            return oldItem.service_id == newItem.service_id &&
                    oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: DaoCollectAllQuestionsOfAllServices,
            newItem: DaoCollectAllQuestionsOfAllServices
        ): Boolean = oldItem == newItem
    }
}
