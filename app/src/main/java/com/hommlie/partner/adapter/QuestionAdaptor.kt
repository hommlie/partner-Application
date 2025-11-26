package com.hommlie.partner.adapter

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.hommlie.partner.R
import com.hommlie.partner.model.DaocollectAnswer
import com.hommlie.partner.model.Questions
import com.hommlie.partner.ui.jobs.ActQuestionary
import com.hommlie.partner.utils.CommonMethods


class QuestionAdaptor(private val context: Context, public var data: List<Questions>)
    : RecyclerView.Adapter<QuestionAdaptor.ViewHolder>() {

    private val imageAnswers = mutableMapOf<Int, Bitmap?>()


    private val answers = mutableMapOf<Int, String>()

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val question: TextView = itemView.findViewById(R.id.tv_question)
        val answer: EditText = itemView.findViewById(R.id.edt_answer)
        val questionNO: TextView = itemView.findViewById(R.id.tv_questionNO)
        val radioGroup: RadioGroup = itemView.findViewById(R.id.radiogroup)
        val radio1: RadioButton = itemView.findViewById(R.id.radio1)
        val radio2: RadioButton = itemView.findViewById(R.id.radio2)
        val radio3: RadioButton = itemView.findViewById(R.id.radio3)
        val radio4: RadioButton = itemView.findViewById(R.id.radio4)
        val showCheckbox: LinearLayout = itemView.findViewById(R.id.ll_forchekbox)
        val checkbox1: CheckBox = itemView.findViewById(R.id.checkbox1)
        val checkbox2: CheckBox = itemView.findViewById(R.id.checkbox2)
        val checkbox3: CheckBox = itemView.findViewById(R.id.checkbox3)
        val checkbox4: CheckBox = itemView.findViewById(R.id.checkbox4)
        val imageCard: CardView = itemView.findViewById(R.id.card_4)
        val image1: ImageView = itemView.findViewById(R.id.iv1)
        val image2: ImageView = itemView.findViewById(R.id.iv2)
        val image3: ImageView = itemView.findViewById(R.id.iv3)
        val image4: ImageView = itemView.findViewById(R.id.iv4)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.row_questions, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val questionData = data[position]

        holder.questionNO.text = "Qes : ${position + 1}"
        if (questionData.required=="1") {
            holder.question.text = questionData.label
        }else{
            holder.question.text = questionData.label+" (optional)"
        }

        // Set visibility based on question type
        holder.answer.visibility = if (questionData.type == "text") View.VISIBLE else View.GONE
        holder.imageCard.visibility = if (questionData.type == "file") View.VISIBLE else View.GONE
        holder.radioGroup.visibility = if (questionData.type == "radio") View.VISIBLE else View.GONE
        holder.showCheckbox.visibility = if (questionData.type == "checkbox") View.VISIBLE else View.GONE

        // Handle radio button options
        if (questionData.type == "radio" && questionData.options != null) {
            val options = questionData.options.split(",")
            val radioButtons = listOf(holder.radio1, holder.radio2, holder.radio3, holder.radio4)
            for (i in options.indices) {
                if (i < radioButtons.size) {
                    radioButtons[i].text = options[i]
                    radioButtons[i].visibility = View.VISIBLE
                }
            }
        }

        // Handle checkbox options
        if (questionData.type == "checkbox" && questionData.options != null) {
            val options = questionData.options.split(",")
            val checkBoxes = listOf(holder.checkbox1, holder.checkbox2, holder.checkbox3, holder.checkbox4)
            for (i in options.indices) {
                if (i < checkBoxes.size) {
                    checkBoxes[i].text = options[i]
                    checkBoxes[i].visibility = View.VISIBLE
                }
            }
        }

        // Save answers based on question type
        when (questionData.type) {
            "text" -> {
                holder.answer.setText(answers[questionData.id])
                holder.answer.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                        // Not used
                    }

                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        // Update answers map when text changes
                        answers[questionData.id] = s?.toString() ?: ""
                    }

                    override fun afterTextChanged(s: Editable?) {
                        // Not used
                    }
                })

            }
            "radio" -> {
                holder.radioGroup.setOnCheckedChangeListener { group, checkedId ->
                    val radioButton = group.findViewById<RadioButton>(checkedId)
                    answers[questionData.id] = radioButton?.text.toString()
                }
            }
            "checkbox" -> {
                val checkboxList = listOf(
                    holder.checkbox1,
                    holder.checkbox2,
                    holder.checkbox3,
                    holder.checkbox4
                )
                checkboxList.forEach { checkbox ->
                    checkbox.setOnCheckedChangeListener { _, _ ->
                        val selectedOptions = checkboxList
                            .filter { it.isChecked }
                            .map { it.text.toString() }
                            .joinToString(",")
                        answers[questionData.id] = selectedOptions
                    }
                }
            }
            // Handle other types if necessary
        }


        holder.image1.setOnClickListener {
            // Launch image picker (you'll handle this in Activity/Fragment)
            (context as ActQuestionary).pickImageForQuestion(questionData.id,holder.image1)
        }
        holder.image2.setOnClickListener {
            // Launch image picker (you'll handle this in Activity/Fragment)
            (context as ActQuestionary).pickImageForQuestion(questionData.id,holder.image2)
        }
        holder.image3.setOnClickListener {
            // Launch image picker (you'll handle this in Activity/Fragment)
            (context as ActQuestionary).pickImageForQuestion(questionData.id,holder.image3)
        }
        holder.image4.setOnClickListener {
            // Launch image picker (you'll handle this in Activity/Fragment)
            (context as ActQuestionary).pickImageForQuestion(questionData.id,holder.image4)
        }


    }

    fun getAnswers(): List<DaocollectAnswer>? {
        val questionDetailsList = mutableListOf<DaocollectAnswer>()
        var allQuestionsAnswered = true

        for (question in data) {
            if (question.required == "1" && answers[question.id].isNullOrEmpty()) {
                CommonMethods.alertErrorOrValidationDialog(context as Activity, "Attend required questions")
                allQuestionsAnswered = false
                break
            }

            val answer = answers[question.id] ?: ""
            questionDetailsList.add(
                DaocollectAnswer(
                    id = question.id,
                    question = question.label,
                    answer = answer
                )
            )
        }

        return if (allQuestionsAnswered) questionDetailsList else null
    }


    override fun getItemCount(): Int {
        return data.size
    }


    fun setImageAnswer(questionId: Int, bitmap: Bitmap) {
        imageAnswers[questionId] = bitmap
        notifyDataSetChanged()
    }

    fun getImageAnswers(): Map<Int, Bitmap?> {
        return imageAnswers
    }


}
