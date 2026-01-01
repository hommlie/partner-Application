package com.hommlie.partner.adapter

import android.content.Context
import android.graphics.Bitmap
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.hommlie.partner.databinding.RowQuestionsBinding
import com.hommlie.partner.model.DaocollectAnswer
import com.hommlie.partner.model.Questions
import com.hommlie.partner.ui.jobs.ActQuestionary

class QuestionAdaptor(
    private val context: Context
) : ListAdapter<Questions, QuestionAdaptor.ViewHolder>(DiffCallback()) {

    private val imageAnswers = mutableMapOf<Int, MutableList<Bitmap>>()
    val serviceAnswerMap = mutableMapOf<Int, MutableList<DaocollectAnswer>>()

    inner class ViewHolder(val binding: RowQuestionsBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(questionData: Questions) {
            binding.tvQuestionNO.text = "Qes : ${adapterPosition + 1}"
            binding.tvQuestion.text = if (questionData.required == "1") questionData.label
            else "${questionData.label} (optional)"

            // Reset views first
            resetViews()

            // Show/hide based on type
            binding.edtAnswer.visibility = if (questionData.type == "text") android.view.View.VISIBLE else android.view.View.GONE
            binding.card4.visibility = if (questionData.type == "file") android.view.View.VISIBLE else android.view.View.GONE
            binding.radiogroup.visibility = if (questionData.type == "radio") android.view.View.VISIBLE else android.view.View.GONE
            binding.llForchekbox.visibility = if (questionData.type == "checkbox") android.view.View.VISIBLE else android.view.View.GONE

            // Set options
            setRadioOptions(questionData)
            setCheckboxOptions(questionData)

            // Restore previous answer
            restoreAnswer(questionData)

            // Handle image clicks
            binding.iv1.setOnClickListener { (context as ActQuestionary).pickImageForQuestion(questionData.id, binding.iv1) }
            binding.iv2.setOnClickListener { (context as ActQuestionary).pickImageForQuestion(questionData.id, binding.iv2) }
            binding.iv3.setOnClickListener { (context as ActQuestionary).pickImageForQuestion(questionData.id, binding.iv3) }
            binding.iv4.setOnClickListener { (context as ActQuestionary).pickImageForQuestion(questionData.id, binding.iv4) }
        }

        private fun resetViews() {
            binding.edtAnswer.setText("")
            binding.radiogroup.setOnCheckedChangeListener(null)
            binding.radiogroup.clearCheck()
            listOf(binding.checkbox1, binding.checkbox2, binding.checkbox3, binding.checkbox4).forEach {
                it.setOnCheckedChangeListener(null)
                it.isChecked = false
            }
        }

        private fun setRadioOptions(question: Questions) {
            if (question.type != "radio" || question.options.isNullOrEmpty()) return
            val options = question.options.split(",")
            val radios = listOf(binding.radio1, binding.radio2, binding.radio3, binding.radio4)
            options.forEachIndexed { index, option ->
                if (index < radios.size) {
                    radios[index].text = option
                    radios[index].visibility = android.view.View.VISIBLE
                }
            }
        }

        private fun setCheckboxOptions(question: Questions) {
            if (question.type != "checkbox" || question.options.isNullOrEmpty()) return
            val options = question.options.split(",")
            val checkBoxes = listOf(binding.checkbox1, binding.checkbox2, binding.checkbox3, binding.checkbox4)
            options.forEachIndexed { index, option ->
                if (index < checkBoxes.size) {
                    checkBoxes[index].text = option
                    checkBoxes[index].visibility = android.view.View.VISIBLE
                }
            }
        }

        private fun restoreAnswer(question: Questions) {
            val saved = serviceAnswerMap[question.serviceId]?.find { it.id == question.id }?.answer ?: ""
            when (question.type) {
                "text" -> {
                    binding.edtAnswer.setText(saved)
                    binding.edtAnswer.addTextChangedListener(object : TextWatcher {
                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                            saveAnswer(question, s.toString())
                        }
                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                        override fun afterTextChanged(s: Editable?) {}
                    })
                }
                "radio" -> {
                    val radios = listOf(binding.radio1, binding.radio2, binding.radio3, binding.radio4)
                    radios.forEach { it.isChecked = it.text.toString() == saved }
                    binding.radiogroup.setOnCheckedChangeListener { group, checkedId ->
                        val rb = group.findViewById<android.widget.RadioButton>(checkedId)
                        saveAnswer(question, rb.text.toString())
                    }
                }
                "checkbox" -> {
                    val checkBoxes = listOf(binding.checkbox1, binding.checkbox2, binding.checkbox3, binding.checkbox4)
                    val selected = saved.split(",").map { it.trim() }
                    checkBoxes.forEach { cb ->
                        cb.isChecked = selected.contains(cb.text.toString())
                        cb.setOnCheckedChangeListener { _, _ ->
                            val combined = checkBoxes.filter { it.isChecked }.joinToString(",") { it.text.toString() }
                            saveAnswer(question, combined)
                        }
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = RowQuestionsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun setImageAnswer(questionId: Int, bitmap: Bitmap) {
        val list = imageAnswers.getOrPut(questionId) { mutableListOf() }
        list.add(bitmap)
        Log.d("IMAGE_CAPTURE", "questionId=$questionId totalImages=${list.size}")
    }

    fun getImageAnswers(): Map<Int, List<Bitmap>> = imageAnswers

    fun getServiceWiseAnswers(): List<Map<String, Any>> {
        val servicesList = mutableListOf<Map<String, Any>>()
        serviceAnswerMap.forEach { (serviceId, answersList) ->
            servicesList.add(mapOf("order_id" to serviceId, "answers" to answersList))
        }
        Log.d("FINAL_SERVICES_JSON", Gson().toJson(servicesList))
        return servicesList
    }

    private fun saveAnswer(question: Questions, answer: String) {
        val list = serviceAnswerMap.getOrPut(question.serviceId) { mutableListOf() }
        val index = list.indexOfFirst { it.id == question.id }
        if (index >= 0) list[index] = list[index].copy(answer = answer)
        else list.add(DaocollectAnswer(question.id, question.label, answer))
    }

    class DiffCallback : DiffUtil.ItemCallback<Questions>() {
        override fun areItemsTheSame(oldItem: Questions, newItem: Questions): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Questions, newItem: Questions): Boolean = oldItem == newItem
    }
}
