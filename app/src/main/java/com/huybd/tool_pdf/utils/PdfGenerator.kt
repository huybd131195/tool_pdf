package com.huybd.tool_pdf.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object PdfGenerator {


    private const val A4_WIDTH = 595
    private const val A4_HEIGHT = 842

    suspend fun generatePdf(
        context: Context,
        imageUris: List<Uri>,
        outputFileName: String
    ): File = withContext(Dispatchers.IO) {

        val pdfDocument = PdfDocument()
        val paint = Paint()

        try {
            for ((index, uri) in imageUris.withIndex()) {
                // 1. Load Bitmap từ Uri (Cần xử lý cẩn thận để tránh OOM)
                val bitmap = loadBitmapFromUri(context, uri) ?: continue

                // 2. Tạo trang PDF mới
                val pageInfo = PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, index + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas: Canvas = page.canvas

                // 3. Vẽ nền trắng (Mặc định PDF trong suốt)
                canvas.drawColor(Color.WHITE)

                // 4. Tính toán tỷ lệ để Scale ảnh vừa khít trang A4 (Fit Center)
                val scaleX = A4_WIDTH.toFloat() / bitmap.width
                val scaleY = A4_HEIGHT.toFloat() / bitmap.height
                val scale = minOf(scaleX, scaleY) // Lấy tỷ lệ nhỏ hơn để ảnh không bị cắt

                // Kích thước ảnh sau khi scale
                val drawWidth = bitmap.width * scale
                val drawHeight = bitmap.height * scale

                // Vị trí để vẽ (Căn giữa trang)
                val left = (A4_WIDTH - drawWidth) / 2
                val top = (A4_HEIGHT - drawHeight) / 2

                // Vẽ ảnh lên Canvas của PDF
                val destRect = RectF(left, top, left + drawWidth, top + drawHeight)
                canvas.drawBitmap(bitmap, null, destRect, paint)

                // 5. Kết thúc trang
                pdfDocument.finishPage(page)

                // 6. QUAN TRỌNG: Giải phóng Bitmap ngay lập tức để tiết kiệm RAM
                bitmap.recycle()
            }

            // 7. Lưu file ra bộ nhớ
            val file = File(context.getExternalFilesDir(null), "$outputFileName.pdf")
            pdfDocument.writeTo(FileOutputStream(file))

            return@withContext file

        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        } finally {
            pdfDocument.close()
        }
    }

    // Hàm tiện ích: Load Bitmap tối ưu
    private fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}