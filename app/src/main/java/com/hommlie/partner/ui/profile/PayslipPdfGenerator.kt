package com.hommlie.partner.ui.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Environment
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.hommlie.partner.R
import com.itextpdf.io.font.constants.StandardFonts
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.colors.DeviceRgb
import java.io.ByteArrayOutputStream
import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.geom.Rectangle
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.canvas.PdfCanvas
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.element.*
import com.itextpdf.layout.properties.HorizontalAlignment
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import com.itextpdf.layout.properties.VerticalAlignment
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class PayslipPdfGenerator(private val context: Context) {

    data class PayslipData(
        val empName: String,
        val empId: String,
        val empPhone: String,
        val aadharNo: String?=null,
        val uinNo: String?=null,
        val selectedMonth: String,
        val cycleStart: String,
        val cycleEnd: String,
        val presentDays: Int,
        val paidLeaves: Int,
        val location: String,
        val payDate: String,
        val earnings: Map<String, String>,
        val deductions: Map<String, String>,
        val netPay: String
    )

    val borderColorInt = ContextCompat.getColor(context, R.color.gray_border)
    val r = Color.red(borderColorInt)
    val g = Color.green(borderColorInt)
    val b = Color.blue(borderColorInt)
    val borderColor = DeviceRgb(r, g, b)

    fun generatePayslipPdf(data: PayslipData, filePath: String): File {
        val file = File(filePath)

        // ---------- CREATE PDF ----------
        val pdfWriter = PdfWriter(file)
        val pdfDocument = PdfDocument(pdfWriter)
        pdfDocument.addNewPage()

        val document = Document(pdfDocument, PageSize.A4)
        document.setMargins(40f, 40f, 40f, 40f)

        val font = PdfFontFactory.createFont(StandardFonts.HELVETICA)
        document.setFont(font)

        // ---------- ADD CONTENT ----------
        addExactHeader(document)
        addCompanyLogo(document,R.drawable.app_logo_forpdf)
        addExactTitle(document, data.selectedMonth)
        addDashedLineSeparator(document,borderColor)
        addExactEmployeeDetails(document, data)
        addDashedLineSeparator(document,borderColor)
        addExactSalaryDetails(document, data)
        addDashedLineSeparator(document,borderColor)
//        addExactFooterSignatures(document)
        addExactFooterNote(document)

        // If you want a second page, uncomment below
        // document.add(AreaBreak())
        // addExactPageTwoContent(document)

        // ---------- DRAW BORDER ON ALL PAGES ----------
        val totalPages = pdfDocument.numberOfPages
        for (i in 1..totalPages) {
            val page = pdfDocument.getPage(i)
            val canvas = PdfCanvas(page)
            val pageSize = page.pageSize

            // Rectangle for border (20 margin from all sides)
            val borderRect = Rectangle(
                20f,
                20f,
                pageSize.width - 40f,
                pageSize.height - 40f
            )

            val cornerRadius = 15f // adjust as needed for curvature

// Solid border with custom color
            canvas.setLineWidth(1f)
            canvas.setStrokeColor(borderColor)

// Draw rounded rectangle
            canvas.roundRectangle(
                borderRect.x.toDouble(),
                borderRect.y.toDouble(),
                borderRect.width.toDouble(),
                borderRect.height.toDouble(),
                cornerRadius.toDouble()
            )
            canvas.stroke()

        }

        // ---------- CLOSE DOCUMENT ----------
        document.close()  // Important: closes pdfDocument internally too

        return file
    }

    private fun addCompanyLogo(document: Document, drawableResId: Int) {
        val pdfDoc = document.pdfDocument
        val firstPage = pdfDoc.getPage(1) // get the first page
        val pageHeight = firstPage.pageSize.height
        val pageWidth = firstPage.pageSize.width

        // Decode and downscale bitmap
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(context.resources, drawableResId, options)
        options.inSampleSize = calculateInSampleSize(options, 300, 92)
        options.inJustDecodeBounds = false
        val bitmap = BitmapFactory.decodeResource(context.resources, drawableResId, options)

        // Convert bitmap to ImageData
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 80, stream)
        val imageData = ImageDataFactory.create(stream.toByteArray())
        val logo = Image(imageData)

        // Set exact size
        logo.scaleToFit(240f, 52f)

        // Absolute position: 20px from top, right aligned
        val topOffset = pageHeight - 50f - logo.imageScaledHeight
        val rightOffset = pageWidth - 40f - logo.imageScaledWidth

        // Pass page number directly: first page = 1
        logo.setFixedPosition(1, rightOffset, topOffset)

        // Add image
        document.add(logo)

        // Free memory
        bitmap.recycle()
    }


    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height, width) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun addExactHeader(document: Document) {
        // EXACT Company Name styling
        val companyName = Paragraph("ADML TECHNO SERVICE PVT LTD.")
            .setFontSize(16f)  // EXACT size
            .setBold()  // EXACT bold
            .setTextAlignment(TextAlignment.LEFT)
            .setMarginBottom(4f)  // EXACT spacing
            .setFontColor(ColorConstants.BLACK)
            .setFixedLeading(16f)  // EXACT line height

        // EXACT Address - 3 lines exactly as in PDF
        val addressLines = arrayOf(
            "57 2nd floor, Place building, 6th Main Rd,",  // Line 1
            "Nagendra Block, Banashankari 1st Stage,",     // Line 2
            "Bengaluru, Karnataka 560050"                  // Line 3
        )

        document.add(companyName)
        for ((index, line) in addressLines.withIndex()) {
            val addressLine = Paragraph(line)
                .setFontSize(10f)  // EXACT size
                .setTextAlignment(TextAlignment.LEFT)
                .setMarginBottom(if (index < 2) 0f else 16f)  // EXACT spacing
                .setFontColor(ColorConstants.BLACK)
                .setFixedLeading(12f)  // EXACT line height
            document.add(addressLine)
        }

    }

    private fun addDashedLineSeparator(document: Document, borderColor: com.itextpdf.kernel.colors.Color) {

        val solid = SolidLine()
        solid.color = borderColor        // ✔ Correct color type
        solid.lineWidth = 0.6f           // ✔ Border जैसा thin

        val separator = LineSeparator(solid)
        separator.setMarginTop(2f)
        separator.setMarginBottom(12f)

        document.add(separator)
    }




    private fun addExactTitle(document: Document, month: String) {
        val title = Paragraph("Pay Slip - $month")
            .setFontSize(14f)  // EXACT size from PDF
            .setBold()  // EXACT bold
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(10f)  // EXACT spacing
            .setFontColor(ColorConstants.BLACK)
            .setFixedLeading(14f)  // EXACT line height

        document.add(title)
    }

    private fun addExactEmployeeDetails(document: Document, data: PayslipData) {
        // Create table with EXACT 4 columns like in PDF
        val table = Table(UnitValue.createPercentArray(floatArrayOf(28f, 30f, 28f, 14f)))
            .setWidth(UnitValue.createPercentValue(100f))
            .setMarginBottom(16f)

        // NO BORDERS (exactly like PDF)
        table.setBorder(Border.NO_BORDER)

        // ROW 1: Employee Name | Value | Adhaar No. | Value
        addEmployeeDetailRow(
            table,
            "Employee Name:",
            data.empName,
            "Adhaar No.",
            data.aadharNo?:"-"
        )

        // ROW 2: Employee ID | Value | UIN No. | Value
        addEmployeeDetailRow(
            table,
            "Employee ID:",
            data.empId,
            "UIN No.",
            data.uinNo ?: "null"
        )

        // ROW 3: Phone | Value | Work Location | Value
        addEmployeeDetailRow(
            table,
            "Phone:",
            data.empPhone,
            "Work Location",
            data.location
        )

        // ROW 4: Salary Duration | Value | Pay Date | Value
        addEmployeeDetailRow(
            table,
            "Salary Duration:",
            "${data.cycleStart} to ${data.cycleEnd}",
            "Pay Date:",
            data.payDate
        )

        // ROW 5: Working Days | Value | Paid Leaves | Value
        addEmployeeDetailRow(
            table,
            "Working Days:",
            data.presentDays.toString(),
            "Paid Leaves:",
            data.paidLeaves.toString()
        )

        document.add(table)
    }

    private fun addEmployeeDetailRow(
        table: Table,
        label1: String,
        value1: String,
        label2: String,
        value2: String
    ) {
        // EXACT formatting: Labels bold, values normal
        // Cell 1: Label 1 (bold)
        val cell1 = Cell()
            .add(Paragraph(label1)
                .setFontSize(10f)
                .setBold()
                .setFontColor(ColorConstants.BLACK))
            .setBorder(Border.NO_BORDER)
            .setPadding(4f)
            .setPaddingLeft(0f)
            .setVerticalAlignment(VerticalAlignment.MIDDLE)

        // Cell 2: Value 1 (normal)
        val cell2 = Cell()
            .add(Paragraph(value1)
                .setFontSize(10f)
                .setFontColor(ColorConstants.BLACK))
            .setBorder(Border.NO_BORDER)
            .setPadding(4f)
            .setVerticalAlignment(VerticalAlignment.MIDDLE)

        // Cell 3: Label 2 (bold)
        val cell3 = Cell()
            .add(Paragraph(label2)
                .setFontSize(10f)
                .setBold()
                .setFontColor(ColorConstants.BLACK))
            .setBorder(Border.NO_BORDER)
            .setPadding(4f)
            .setVerticalAlignment(VerticalAlignment.MIDDLE)

        // Cell 4: Value 2 (normal)
        val cell4 = Cell()
            .add(Paragraph(value2)
                .setFontSize(10f)
                .setFontColor(ColorConstants.BLACK))
            .setBorder(Border.NO_BORDER)
            .setPadding(4f)
            .setPaddingRight(0f)
            .setVerticalAlignment(VerticalAlignment.MIDDLE)

        table.addCell(cell1)
        table.addCell(cell2)
        table.addCell(cell3)
        table.addCell(cell4)
    }

    private fun addExactSalaryDetails(document: Document, data: PayslipData) {
        // EXACT "Salary Details" title (left-aligned, bold)
        val title = Paragraph("Salary Details")
            .setFontSize(12f)
            .setBold()
            .setMarginBottom(12f)
            .setFontColor(ColorConstants.BLACK)
            .setFixedLeading(12f)
        document.add(title)

        // Create table with EXACT 4 columns: Earnings | Amount | Deductions | Amount
        val table = Table(UnitValue.createPercentArray(floatArrayOf(32f, 18f, 32f, 18f)))
            .setWidth(UnitValue.createPercentValue(100f))

        // NO BORDERS (exactly like PDF)
        table.setBorder(Border.NO_BORDER)

        // HEADER ROW (Earnings, Amount, Deductions, Amount)
        table.addHeaderCell(createHeaderCell("Earnings", TextAlignment.LEFT))
        table.addHeaderCell(createHeaderCell("Amount", TextAlignment.RIGHT))
        table.addHeaderCell(createHeaderCell("Deductions", TextAlignment.LEFT))
        table.addHeaderCell(createHeaderCell("Amount", TextAlignment.RIGHT))

        // EARNINGS ROWS (EXACT ORDER from PDF)
        addSalaryRow(table, "Basic & DA", data.earnings["basic"] ?: "0", "Advance Paid", data.deductions["advance"] ?: "0")
        addSalaryRow(table, "HRA", data.earnings["hra"] ?: "0", "", "")
        addSalaryRow(table, "Conveyance", data.earnings["conveyance"] ?: "0", "", "")
        addSalaryRow(table, "Medical Allowance", data.earnings["medicalAllowance"] ?: "0", "", "")
        addSalaryRow(table, "Grooming Allowance", data.earnings["groomingAllowance"] ?: "0", "", "")
        addSalaryRow(table, "Travel Allowance", data.earnings["travel_allowance"] ?: "0", "", "")
        addSalaryRow(table, "Incentives", data.earnings["extra"] ?: "0", "", "")

        // Calculate totals
        val totalEarnings = data.earnings.values.sumOf { it.toDoubleOrNull() ?: 0.0 }
        val totalDeductions = data.deductions.values.sumOf { it.toDoubleOrNull() ?: 0.0 }

        // TOTAL EARNINGS ROW (bold)
        addBoldTotalRow(table, "Total Earnings", totalEarnings, "Total Deductions", totalDeductions)

        // EMPTY ROW (for spacing - exactly like PDF)
        table.addCell(createTableCell("", TextAlignment.LEFT, false))
        table.addCell(createTableCell("", TextAlignment.RIGHT, false))
        table.addCell(createTableCell("", TextAlignment.LEFT, false))
        table.addCell(createTableCell("", TextAlignment.RIGHT, false))

        // NET PAY ROW (bold, right side only)
        table.addCell(createTableCell("", TextAlignment.LEFT, false))
        table.addCell(createTableCell("", TextAlignment.RIGHT, false))
        table.addCell(createTableCell("Net Pay", TextAlignment.LEFT, true))
        table.addCell(createTableCell("₹${data.netPay}", TextAlignment.RIGHT, true))

        document.add(table)
    }

    private fun createHeaderCell(text: String, alignment: TextAlignment): Cell {
        return Cell()
            .add(Paragraph(text)
                .setFontSize(11f)
                .setBold()
                .setFontColor(ColorConstants.BLACK))
            .setBorder(Border.NO_BORDER)
            .setPadding(4f)
            .setTextAlignment(alignment)
            .setVerticalAlignment(VerticalAlignment.MIDDLE)
    }

    private fun createTableCell(text: String, alignment: TextAlignment, isBold: Boolean): Cell {
        val paragraph = Paragraph(text)
            .setFontSize(10f)
            .setFontColor(ColorConstants.BLACK)

        if (isBold) {
            paragraph.setBold()
        }

        return Cell()
            .add(paragraph)
            .setBorder(Border.NO_BORDER)
            .setPadding(4f)
            .setTextAlignment(alignment)
            .setVerticalAlignment(VerticalAlignment.MIDDLE)
    }

    private fun addSalaryRow(
        table: Table,
        earningsLabel: String,
        earningsAmount: String,
        deductionsLabel: String,
        deductionsAmount: String
    ) {
        // Earnings cell (left aligned)
        table.addCell(createTableCell(earningsLabel, TextAlignment.LEFT, false))

        // Earnings amount (right aligned)
        val earningsValue = if (earningsAmount.isNotEmpty() && earningsAmount != "0") "₹$earningsAmount" else "₹0"
        table.addCell(createTableCell(earningsValue, TextAlignment.RIGHT, false))

        // Deductions cell (left aligned)
        table.addCell(createTableCell(deductionsLabel, TextAlignment.LEFT, false))

        // Deductions amount (right aligned)
        val deductionsValue = if (deductionsAmount.isNotEmpty() && deductionsAmount != "0") "₹$deductionsAmount" else ""
        table.addCell(createTableCell(deductionsValue, TextAlignment.RIGHT, false))
    }

    private fun addBoldTotalRow(
        table: Table,
        earningsLabel: String,
        earningsTotal: Double,
        deductionsLabel: String,
        deductionsTotal: Double
    ) {
        // Total Earnings (bold)
        table.addCell(createTableCell(earningsLabel, TextAlignment.LEFT, true))
        table.addCell(createTableCell("₹${String.format("%.2f", earningsTotal)}", TextAlignment.RIGHT, true))

        // Total Deductions (bold)
        table.addCell(createTableCell(deductionsLabel, TextAlignment.LEFT, true))
        table.addCell(createTableCell("₹${String.format("%.2f", deductionsTotal)}", TextAlignment.RIGHT, true))
    }

    private fun addExactFooterSignatures(document: Document) {
        // EXACT spacing from salary table to signatures
        document.add(Paragraph("\n"))

        val signatureTable = Table(UnitValue.createPercentArray(floatArrayOf(50f, 50f)))
            .setWidth(UnitValue.createPercentValue(100f))
            .setMarginTop(40f)  // EXACT spacing from your PDF

        signatureTable.setBorder(Border.NO_BORDER)

        // EXACT "Employee Signature" (left aligned)
        val employeeSig = Cell()
            .add(Paragraph("Employee Signature")
                .setFontSize(10f)
                .setFontColor(ColorConstants.BLACK)
                .setFixedLeading(12f))
            .setBorder(Border.NO_BORDER)
            .setPaddingTop(20f)  // Space for signature line
            .setTextAlignment(TextAlignment.LEFT)

        // EXACT "Authorized Signature" (right aligned)
        val authSig = Cell()
            .add(Paragraph("Authorized Signature")
                .setFontSize(10f)
                .setFontColor(ColorConstants.BLACK)
                .setFixedLeading(12f))
            .setBorder(Border.NO_BORDER)
            .setPaddingTop(20f)  // Space for signature line
            .setTextAlignment(TextAlignment.RIGHT)

        signatureTable.addCell(employeeSig)
        signatureTable.addCell(authSig)

        document.add(signatureTable)
    }

    private fun addExactFooterNote(document: Document) {
        // EXACT note text and styling
        val note = Paragraph("This is a computer-generated payslip and does not require a physical signature.")
            .setFontSize(8f)
            .setFontColor(ColorConstants.BLACK)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginTop(12f)
            .setFixedLeading(10f)

        val cinNote = Paragraph("CIN no: null")
            .setFontSize(8f)
            .setFontColor(ColorConstants.BLACK)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(20f)
            .setFixedLeading(10f)

        document.add(note)
        document.add(cinNote)
    }

    private fun addExactPageTwoContent(document: Document) {
        // EXACT fraction on page 2: centered vertically and horizontally
        // The fraction in your PDF is: \[ \frac { 1 } { 2 } \]

        // We'll create a paragraph with the exact text representation
        // For a more exact visual, we would need a math font, but this will be close

        val fraction = Paragraph("\\[ \\frac { 1 } { 2 } \\]")
            .setFontSize(36f)  // Large size like in PDF
            .setBold()
            .setFontColor(ColorConstants.BLACK)
            .setTextAlignment(TextAlignment.CENTER)
            .setHorizontalAlignment(HorizontalAlignment.CENTER)
            .setVerticalAlignment(VerticalAlignment.MIDDLE)
            .setFixedPosition(
                0f,  // X position (will be centered)
                PageSize.A4.height / 2 - 50,  // Center vertically
                PageSize.A4.width  // Full width for centering
            )

        document.add(fraction)
    }

    companion object {
        fun getPayslipFileName(empName: String, month: String): String {
            // EXACT filename format: "PaySlip-Vishal, rathaur-2025-11.pdf"
            val sanitizedName = empName.trim()
            val sanitizedMonth = month.replace(" ", "-")
            return "PaySlip-$sanitizedName-$sanitizedMonth.pdf"
        }

        fun getPayslipDirectory(context: Context): File {
            val directory = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                "Payslips"
            )
            if (!directory.exists()) {
                directory.mkdirs()
            }
            return directory
        }
    }
}