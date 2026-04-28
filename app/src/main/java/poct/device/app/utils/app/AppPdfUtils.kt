package poct.device.app.utils.app

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.graphics.scale
import androidx.core.graphics.toColorInt
import poct.device.app.App
import poct.device.app.R
import poct.device.app.bean.CaseBean
import poct.device.app.bean.PdfBean
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

object AppPdfUtils {
    fun generatePdf(pdfBean: PdfBean) {
        if (pdfBean.type == CaseBean.TYPE_4LJ) {
            generate4LjPdf(pdfBean)
        } else if (pdfBean.type == CaseBean.TYPE_IGE) {
            generateIgePdf(pdfBean)
        } else if (pdfBean.type == CaseBean.TYPE_CRP) {
            generateCrpPdf(pdfBean)
        } else if (pdfBean.type == CaseBean.TYPE_SF) {
            generateSfCrpPdf(pdfBean)
        } else if (pdfBean.type == CaseBean.TYPE_3LJ) {
            generate3LjPdf(pdfBean)
        } else if (pdfBean.type == CaseBean.TYPE_2LJ_A) {
            generate2LJAPdf(pdfBean)
        } else if (pdfBean.type == CaseBean.TYPE_2LJ_B) {
            generate2LJBPdf(pdfBean)
        }
    }

    private fun generateIgePdf(pdfBean: PdfBean) {
        // for our PDF file. A4纸适配
        val pageWidth = 596
        val pageHeight = 842
        val pdfDocument = PdfDocument()
        val paint = Paint()
        val myPageInfo: PdfDocument.PageInfo? =
            PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val myPage: PdfDocument.Page = pdfDocument.startPage(myPageInfo)
        val canvas: Canvas = myPage.canvas
        // 背景
        paint.color = "#FFFFFF".toColorInt()
        canvas.drawRect(0F, 0F, pageWidth.toFloat(), pageHeight.toFloat(), paint)

        /**
         * 1、标题与LOGO
         */
        // logo
        pdfBean.logo?.apply {
            val scaledLogo = this.scale(87, 87, false)
            canvas.drawBitmap(scaledLogo, 36F, 22F, paint)
        }
        paint.textAlign = Paint.Align.CENTER
        // 标题
        val title = pdfBean.title
        paint.typeface = Typeface.defaultFromStyle(Typeface.BOLD)
        paint.textSize = 28F
        paint.color = "#FF0000".toColorInt()
        canvas.drawText(title, 298F, 30F + paint.textSize, paint)

        // 副标题
        val subTitle = pdfBean.subTitle
        paint.typeface = Typeface.defaultFromStyle(Typeface.NORMAL)
        paint.color = "#512DA8".toColorInt()
        paint.textSize = 24F
        canvas.drawText(subTitle, 298F, 70F + paint.textSize, paint)

        paint.textAlign = Paint.Align.LEFT

        /**
         * 2、基础信息
         */
        // 检测信息 ：编号与日期
        val jcbhLabel = App.getContext().getString(R.string.jcbhLabel)
        val jcrqLabel = App.getContext().getString(R.string.jcrqLabel)
        val jcbh = pdfBean.jcbh
        val jcrq = pdfBean.jcrq
        paint.typeface = Typeface.defaultFromStyle(Typeface.NORMAL)
        paint.color = "#171A1D".toColorInt()
        paint.textSize = 20F
        canvas.drawText("$jcbhLabel $jcbh", 43F, 140F + paint.textSize, paint)
        canvas.drawText("$jcrqLabel $jcrq", 360F, 140F + paint.textSize, paint)
        // 分割线
        paint.color = "#FF171A1D".toColorInt()
        canvas.drawRect(36F, 180F, 524F + 36F, 180F + 3F, paint)

        val xmLabel = App.getContext().getString(R.string.xmLabel)
        val xbLabel = App.getContext().getString(R.string.xbLabel)
        val nlLabel = App.getContext().getString(R.string.nlLabel)
        val xm = pdfBean.xm
        val xb = pdfBean.xb
        val nl = pdfBean.nl
        paint.typeface = Typeface.defaultFromStyle(Typeface.NORMAL)
        paint.color = "#FF171A1D".toColorInt()
        paint.textSize = 20F
        canvas.drawText("$xmLabel $xm", 65F, 210F + paint.textSize, paint)
        canvas.drawText("$xbLabel $xb", 246F, 210F + paint.textSize, paint)
        canvas.drawText("$nlLabel $nl", 426F, 210F + paint.textSize, paint)
        // 分割线
        paint.color = "#FFD4D4D4".toColorInt()
        canvas.drawRect(54F, 260F, 488F + 54F, 260F + 1F, paint)

        val sjIdLabel = App.getContext().getString(R.string.sjIdLabel)
//        val ybIdLabel = App.getContext().getString(R.string.ybIdLabel)
        val sjId = pdfBean.sjId
//        val ybId = pdfBean.ybId
        paint.typeface = Typeface.defaultFromStyle(Typeface.NORMAL)
        paint.color = "#FF171A1D".toColorInt()
        paint.textSize = 20F
        canvas.drawText("$sjIdLabel $sjId", 65F, 280F + paint.textSize, paint)
        //canvas.drawText("$ybIdLabel $ybId", 246F, 214F + paint.textSize, paint)

        // 分割线
        paint.color = "#FF171A1D".toColorInt()
        canvas.drawRect(36F, 330F, 524F + 36F, 330F + 2F, paint)
        /**
         * 3、检查结果
         */
        val jcjgLabel = App.getContext().getString(R.string.jcjgLabel)
        paint.typeface = Typeface.defaultFromStyle(Typeface.NORMAL)
        paint.color = "#171A1D".toColorInt()
        paint.textSize = 20F
        canvas.drawText(jcjgLabel, 47F, 350F + paint.textSize, paint)
        val jcxmLabel = App.getContext().getString(R.string.jcxmLabel)
        val jgpdLabel = App.getContext().getString(R.string.jgpdLabel)
        val ckbzLabel = App.getContext().getString(R.string.ckbzLabel)

        paint.typeface = Typeface.defaultFromStyle(Typeface.BOLD)
        paint.color = "#FF171A1D".toColorInt()
        paint.textSize = 20F

        canvas.drawText(jcxmLabel, 60F, 400F + paint.textSize, paint)
        canvas.drawText(jgpdLabel, 180F, 400F + paint.textSize, paint)
        canvas.drawText(ckbzLabel, 320F, 400F + paint.textSize, paint)

        val item1Left = 60F
        val item2Left = 180F
        val item3Left = 320F
        val item4Left = 300F

        var itemTopStart = 400F
        paint.typeface = Typeface.defaultFromStyle(Typeface.NORMAL)
        paint.color = "#FF171A1D".toColorInt()
        paint.textSize = 20F
        val data = pdfBean.data
        Timber.w("当前页面data${App.gson.toJson(data)}")
        for (index in data.indices) {
            val columns = data[index]
            val remarkList = columns[3].split("@")
            val top1 = itemTopStart + (paint.textSize + 40F) * remarkList.size / 2 - 40F
            canvas.drawText(columns[0], item1Left, top1, paint)
            canvas.drawText(columns[2], item2Left, top1, paint)
            if (columns[4] == "1") {
                canvas.drawText("↑", item4Left, top1, paint)
            }
            Timber.w("当前页面remark${App.gson.toJson(remarkList)}")
            for (remark in remarkList) {
                itemTopStart += 40F
                val top2 = itemTopStart + paint.textSize
                canvas.drawText(remark, item3Left, top2, paint)
            }
        }
        // 分割线
        paint.color = "#FF171A1D".toColorInt()
        canvas.drawRect(36F, 770F, 524F + 36F, 770F + 3F, paint)
        /**
         * 4、说明
         */
        paint.typeface = Typeface.defaultFromStyle(Typeface.NORMAL)
        paint.color = "#FF171A1D".toColorInt()
        paint.textSize = 14F
        val smLabel = App.getContext().getString(R.string.smLabel)
        val smContent1 = App.getContext().getString(R.string.smContent1)

        canvas.drawText(smLabel, 43F, 775F + 1 * 20F, paint)
        canvas.drawText(smContent1, 43F, 775F + 2 * 20F, paint)
        pdfDocument.finishPage(myPage)
        val file = File(pdfBean.outPath)
        if (file.parentFile?.exists() != true) {
            file.parentFile?.mkdirs()
        }

        try {
            pdfDocument.writeTo(FileOutputStream(file))
        } catch (e: Exception) {
            Timber.e(e.message ?: "unknown error")
        } finally {
            pdfDocument.close()
        }
    }

    private fun generateCrpPdf(pdfBean: PdfBean) {
        // for our PDF file. A4纸适配
        val pageWidth = 596
        val pageHeight = 842
        val pdfDocument = PdfDocument()
        val paint = Paint()
        val myPageInfo: PdfDocument.PageInfo? =
            PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val myPage: PdfDocument.Page = pdfDocument.startPage(myPageInfo)
        val canvas: Canvas = myPage.canvas
        // 背景
        paint.setColor("#FFFFFF".toColorInt())
        canvas.drawRect(0F, 0F, pageWidth.toFloat(), pageHeight.toFloat(), paint)

        /**
         * 1、标题与LOGO
         */
        // logo
        pdfBean.logo?.apply {
            val scaledLogo = this.scale(87, 87, false)
            canvas.drawBitmap(scaledLogo, 36F, 22F, paint)
        }
        paint.setTextAlign(Paint.Align.CENTER)
        // 标题
        val title = pdfBean.title
        paint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD))
        paint.textSize = 28F
        paint.setColor("#FF0000".toColorInt())
        canvas.drawText(title, 298F, 30F + paint.textSize, paint)

        // 副标题
        val subTitle = pdfBean.subTitle
        paint.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL))
        paint.setColor("#512DA8".toColorInt())
        paint.textSize = 24F
        canvas.drawText(subTitle, 298F, 70F + paint.textSize, paint)

        paint.textAlign = Paint.Align.LEFT

        /**
         * 2、基础信息
         */
        // 检测信息 ：编号与日期
        val jcbhLabel = App.getContext().getString(R.string.jcbhLabel)
        val jcrqLabel = App.getContext().getString(R.string.jcrqLabel)
        val jcbh = pdfBean.jcbh
        val jcrq = pdfBean.jcrq
        paint.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL))
        paint.setColor("#171A1D".toColorInt())
        paint.textSize = 20F
        canvas.drawText("$jcbhLabel $jcbh", 43F, 140F + paint.textSize, paint)
        canvas.drawText("$jcrqLabel $jcrq", 360F, 140F + paint.textSize, paint)
        // 分割线
        paint.setColor("#FF171A1D".toColorInt())
        canvas.drawRect(36F, 180F, 524F + 36F, 180F + 3F, paint)

        val xmLabel = App.getContext().getString(R.string.xmLabel)
        val xbLabel = App.getContext().getString(R.string.xbLabel)
        val nlLabel = App.getContext().getString(R.string.nlLabel)
        val xm = pdfBean.xm
        val xb = pdfBean.xb
        val nl = pdfBean.nl
        paint.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL))
        paint.setColor("#FF171A1D".toColorInt())
        paint.textSize = 20F
        canvas.drawText("$xmLabel $xm", 65F, 210F + paint.textSize, paint)
        canvas.drawText("$xbLabel $xb", 246F, 210F + paint.textSize, paint)
        canvas.drawText("$nlLabel $nl", 426F, 210F + paint.textSize, paint)
        // 分割线
        paint.setColor("#FFD4D4D4".toColorInt())
        canvas.drawRect(54F, 260F, 488F + 54F, 260F + 1F, paint)

        val sjIdLabel = App.getContext().getString(R.string.sjIdLabel)
//        val ybIdLabel = App.getContext().getString(R.string.ybIdLabel)
        val sjId = pdfBean.sjId
//        val ybId = pdfBean.ybId
        paint.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL))
        paint.setColor("#FF171A1D".toColorInt())
        paint.textSize = 20F
        canvas.drawText("$sjIdLabel $sjId", 65F, 280F + paint.textSize, paint)
        //canvas.drawText("$ybIdLabel $ybId", 246F, 214F + paint.textSize, paint)

        // 分割线
        paint.setColor("#FF171A1D".toColorInt())
        canvas.drawRect(36F, 330F, 524F + 36F, 330F + 2F, paint)
        /**
         * 3、检查结果
         */
        val jcjgLabel = App.getContext().getString(R.string.jcjgLabel)
        paint.typeface = Typeface.defaultFromStyle(Typeface.NORMAL)
        paint.color = "#171A1D".toColorInt()
        paint.textSize = 20F
        canvas.drawText(jcjgLabel, 47F, 350F + paint.textSize, paint)
        val jcxmLabel = App.getContext().getString(R.string.jcxmLabel)
        val jgpdLabel = App.getContext().getString(R.string.jgpdLabel)
        val ckbzLabel = App.getContext().getString(R.string.ckbzLabel)
        paint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD))
        paint.setColor("#FF171A1D".toColorInt())
        paint.textSize = 20F

        canvas.drawText(jcxmLabel, 60F, 400F + paint.textSize, paint)
        canvas.drawText(jgpdLabel, 220F, 400F + paint.textSize, paint)
        canvas.drawText(ckbzLabel, 380F, 400F + paint.textSize, paint)

        val item1Left = 60F
        val item2Left = 220F
        val item4Left = 320F
        val item3Left = 380F

        var itemTopStart = 400F
        paint.typeface = Typeface.defaultFromStyle(Typeface.NORMAL)
        paint.color = "#FF171A1D".toColorInt()
        paint.textSize = 20F
        val data = pdfBean.data
        Timber.w("当前页面data${App.gson.toJson(data)}")
        for (index in data.indices) {
            val columns = data[index]
            val remarkList = columns[3].split("@")
            val top1 = itemTopStart + paint.textSize + 40F + (40F) * (remarkList.size - 1) / 2
            canvas.drawText("${index + 1}. ${columns[0]}", item1Left, top1, paint)
            canvas.drawText(columns[2], item2Left, top1, paint)
            if (columns[4] == "1" && columns[0] == "CRP") {
                canvas.drawText("↑", item4Left, top1, paint)
            } else if (columns[4] == "1" && columns[0] == "SF") {
                canvas.drawText("↓", item4Left, top1, paint)
            }
            Timber.w("当前页面remark${App.gson.toJson(remarkList)}")
            for (remark in remarkList) {
                itemTopStart += 40F
                val top2 = itemTopStart + paint.textSize
                canvas.drawText(remark, item3Left, top2, paint)
            }
        }
        // 分割线
        paint.color = "#FF171A1D".toColorInt()
        canvas.drawRect(36F, 770F, 524F + 36F, 770F + 3F, paint)
        /**
         * 4、说明
         */
        paint.typeface = Typeface.defaultFromStyle(Typeface.NORMAL)
        paint.color = "#FF171A1D".toColorInt()
        paint.textSize = 14F
        val smLabel = App.getContext().getString(R.string.smLabel)
        val smContent1 = App.getContext().getString(R.string.smContent1)
        canvas.drawText(smLabel, 43F, 775F + 1 * 20F, paint)
        canvas.drawText(smContent1, 43F, 775F + 2 * 20F, paint)

        pdfDocument.finishPage(myPage)
        val file = File(pdfBean.outPath)
        if (file.parentFile?.exists() != true) {
            file.parentFile?.mkdirs()
        }

        try {
            pdfDocument.writeTo(FileOutputStream(file))
        } catch (e: Exception) {
            Timber.e(e.message ?: "unknown error")
        } finally {
            pdfDocument.close()
        }
    }

    private fun generateSfCrpPdf(pdfBean: PdfBean) {
        // for our PDF file. A4纸适配
        val pageWidth = 596
        val pageHeight = 842
        val pdfDocument = PdfDocument()
        val paint = Paint()
        val myPageInfo: PdfDocument.PageInfo? =
            PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val myPage: PdfDocument.Page = pdfDocument.startPage(myPageInfo)
        val canvas: Canvas = myPage.canvas
        // 背景
        paint.setColor("#FFFFFF".toColorInt())
        canvas.drawRect(0F, 0F, pageWidth.toFloat(), pageHeight.toFloat(), paint)

        /**
         * 1、标题与LOGO
         */
        // logo
        pdfBean.logo?.apply {
            val scaledLogo = this.scale(87, 87, false)
            canvas.drawBitmap(scaledLogo, 36F, 22F, paint)
        }
        paint.setTextAlign(Paint.Align.CENTER)
        // 标题
        val title = pdfBean.title
        paint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD))
        paint.textSize = 28F
        paint.setColor("#FF0000".toColorInt())
        canvas.drawText(title, 298F, 30F + paint.textSize, paint)

        // 副标题
        val subTitle = pdfBean.subTitle
        paint.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL))
        paint.setColor("#512DA8".toColorInt())
        paint.textSize = 24F
        canvas.drawText(subTitle, 298F, 70F + paint.textSize, paint)

        paint.textAlign = Paint.Align.LEFT

        /**
         * 2、基础信息
         */
        // 检测信息 ：编号与日期
        val jcbhLabel = App.getContext().getString(R.string.jcbhLabel)
        val jcrqLabel = App.getContext().getString(R.string.jcrqLabel)
        val jcbh = pdfBean.jcbh
        val jcrq = pdfBean.jcrq
        paint.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL))
        paint.setColor("#171A1D".toColorInt())
        paint.textSize = 20F
        canvas.drawText("$jcbhLabel $jcbh", 43F, 140F + paint.textSize, paint)
        canvas.drawText("$jcrqLabel $jcrq", 360F, 140F + paint.textSize, paint)
        // 分割线
        paint.setColor("#FF171A1D".toColorInt())
        canvas.drawRect(36F, 180F, 524F + 36F, 180F + 3F, paint)

        val xmLabel = App.getContext().getString(R.string.xmLabel)
        val xbLabel = App.getContext().getString(R.string.xbLabel)
        val nlLabel = App.getContext().getString(R.string.nlLabel)
        val xm = pdfBean.xm
        val xb = pdfBean.xb
        val nl = pdfBean.nl
        paint.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL))
        paint.setColor("#FF171A1D".toColorInt())
        paint.textSize = 20F
        canvas.drawText("$xmLabel $xm", 65F, 210F + paint.textSize, paint)
        canvas.drawText("$xbLabel $xb", 246F, 210F + paint.textSize, paint)
        canvas.drawText("$nlLabel $nl", 426F, 210F + paint.textSize, paint)
        // 分割线
        paint.setColor("#FFD4D4D4".toColorInt())
        canvas.drawRect(54F, 260F, 488F + 54F, 260F + 1F, paint)

        val sjIdLabel = App.getContext().getString(R.string.sjIdLabel)
//        val ybIdLabel = App.getContext().getString(R.string.ybIdLabel)
        val sjId = pdfBean.sjId
//        val ybId = pdfBean.ybId
        paint.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL))
        paint.setColor("#FF171A1D".toColorInt())
        paint.textSize = 20F
        canvas.drawText("$sjIdLabel $sjId", 65F, 280F + paint.textSize, paint)
        //canvas.drawText("$ybIdLabel $ybId", 246F, 214F + paint.textSize, paint)

        // 分割线
        paint.setColor("#FF171A1D".toColorInt())
        canvas.drawRect(36F, 330F, 524F + 36F, 330F + 2F, paint)
        /**
         * 3、检查结果
         */
        val jcjgLabel = App.getContext().getString(R.string.jcjgLabel)
        paint.typeface = Typeface.defaultFromStyle(Typeface.NORMAL)
        paint.color = "#171A1D".toColorInt()
        paint.textSize = 20F
        canvas.drawText(jcjgLabel, 47F, 350F + paint.textSize, paint)
        val jcxmLabel = App.getContext().getString(R.string.jcxmLabel)
        val jgpdLabel = App.getContext().getString(R.string.jgpdLabel)
        val ckbzLabel = App.getContext().getString(R.string.ckbzLabel)
        paint.typeface = Typeface.defaultFromStyle(Typeface.BOLD)
        paint.color = "#FF171A1D".toColorInt()
        paint.textSize = 20F

        canvas.drawText(jcxmLabel, 60F, 400F + paint.textSize, paint)
        canvas.drawText(jgpdLabel, 220F, 400F + paint.textSize, paint)
        canvas.drawText(ckbzLabel, 380F, 400F + paint.textSize, paint)

        val item1Left = 60F
        val item2Left = 220F
        val item4Left = 320F
        val item3Left = 380F

        var itemTopStart = 400F
        paint.typeface = Typeface.defaultFromStyle(Typeface.NORMAL)
        paint.color = "#FF171A1D".toColorInt()
        paint.textSize = 20F
        val data = pdfBean.data
        Timber.w("当前页面data${App.gson.toJson(data)}")
        for (index in data.indices) {
            val columns = data[index]
            val remarkList = columns[3].split("@")
            val top1 = itemTopStart + paint.textSize + 40F + (40F) * (remarkList.size - 1) / 2
            canvas.drawText("${index + 1}. ${columns[0]}", item1Left, top1, paint)
            canvas.drawText(columns[2], item2Left, top1, paint)
            if (columns[4] == "1" && columns[0] == "CRP") {
                canvas.drawText("↑", item4Left, top1, paint)
            } else if (columns[4] == "1" && columns[0] == "SF") {
                canvas.drawText("↓", item4Left, top1, paint)
            }
            Timber.w("当前页面remark${App.gson.toJson(remarkList)}")
            for (remark in remarkList) {
                itemTopStart += 40F
                val top2 = itemTopStart + paint.textSize
                canvas.drawText(remark, item3Left, top2, paint)
            }
        }
        // 分割线
        paint.color = "#FF171A1D".toColorInt()
        canvas.drawRect(36F, 770F, 524F + 36F, 770F + 3F, paint)
        /**
         * 4、说明
         */
        paint.typeface = Typeface.defaultFromStyle(Typeface.NORMAL)
        paint.color = "#FF171A1D".toColorInt()
        paint.textSize = 14F
        val smLabel = App.getContext().getString(R.string.smLabel)
        val smContent1 = App.getContext().getString(R.string.smContent1)
        canvas.drawText(smLabel, 43F, 775F + 1 * 20F, paint)
        canvas.drawText(smContent1, 43F, 775F + 2 * 20F, paint)

        pdfDocument.finishPage(myPage)
        val file = File(pdfBean.outPath)
        if (file.parentFile?.exists() != true) {
            file.parentFile?.mkdirs()
        }

        try {
            pdfDocument.writeTo(FileOutputStream(file))
        } catch (e: Exception) {
            Timber.e(e.message ?: "unknown error")
        } finally {
            pdfDocument.close()
        }
    }

    private fun generate4LjPdf(pdfBean: PdfBean) {
        // for our PDF file. A4纸适配
        val pageWidth = 596
        val pageHeight = 842
        val pdfDocument = PdfDocument()
        val paint = Paint()
        val myPageInfo: PdfDocument.PageInfo? =
            PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val myPage: PdfDocument.Page = pdfDocument.startPage(myPageInfo)
        val canvas: Canvas = myPage.canvas
        // 背景
        paint.setColor("#FFFFFF".toColorInt())
        canvas.drawRect(0F, 0F, pageWidth.toFloat(), pageHeight.toFloat(), paint)

        /**
         * 1、标题与LOGO
         */
        // logo
        pdfBean.logo?.apply {
            val scaledLogo = this.scale(87, 87, false)
            canvas.drawBitmap(scaledLogo, 36F, 22F, paint)
        }
        paint.setTextAlign(Paint.Align.CENTER)
        // 标题
        val title = pdfBean.title
        paint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD))
        paint.textSize = 28F
        paint.setColor("#FF0000".toColorInt())
        canvas.drawText(title, 298F, 30F + paint.textSize, paint)

        // 副标题
        val subTitle = pdfBean.subTitle
        paint.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL))
        paint.setColor("#512DA8".toColorInt())
        paint.textSize = 24F
        canvas.drawText(subTitle, 298F, 70F + paint.textSize, paint)

        paint.textAlign = Paint.Align.LEFT

        /**
         * 2、基础信息
         */
        // 检测信息 ：编号与日期
        val jcbhLabel = App.getContext().getString(R.string.jcbhLabel)
        val jcrqLabel = App.getContext().getString(R.string.jcrqLabel)
        val jcbh = pdfBean.jcbh
        val jcrq = pdfBean.jcrq
        paint.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL))
        paint.setColor("#171A1D".toColorInt())
        paint.textSize = 20F
        canvas.drawText("$jcbhLabel $jcbh", 43F, 140F + paint.textSize, paint)
        canvas.drawText("$jcrqLabel $jcrq", 390F, 140F + paint.textSize, paint)
        // 分割线
        paint.setColor("#FF171A1D".toColorInt())
        canvas.drawRect(36F, 180F, 524F + 36F, 180F + 3F, paint)

        val xmLabel = App.getContext().getString(R.string.xmLabel)
        val xbLabel = App.getContext().getString(R.string.xbLabel)
        val nlLabel = App.getContext().getString(R.string.nlLabel)
        val xm = pdfBean.xm
        val xb = pdfBean.xb
        val nl = pdfBean.nl
        paint.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL))
        paint.setColor("#FF171A1D".toColorInt())
        paint.textSize = 20F
        canvas.drawText("$xmLabel $xm", 65F, 210F + paint.textSize, paint)
        canvas.drawText("$xbLabel $xb", 246F, 210F + paint.textSize, paint)
        canvas.drawText("$nlLabel $nl", 426F, 210F + paint.textSize, paint)
        // 分割线
        paint.setColor("#FFD4D4D4".toColorInt())
        canvas.drawRect(54F, 260F, 488F + 54F, 260F + 1F, paint)

        val sjIdLabel = App.getContext().getString(R.string.sjIdLabel)
//        val ybIdLabel = App.getContext().getString(R.string.ybIdLabel)
        val sjId = pdfBean.sjId
//        val ybId = pdfBean.ybId
        paint.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL))
        paint.setColor("#FF171A1D".toColorInt())
        paint.textSize = 20F
        canvas.drawText("$sjIdLabel $sjId", 65F, 280F + paint.textSize, paint)
        //canvas.drawText("$ybIdLabel $ybId", 246F, 214F + paint.textSize, paint)

        // 分割线
        paint.setColor("#FF171A1D".toColorInt())
        canvas.drawRect(36F, 330F, 524F + 36F, 330F + 2F, paint)
        /**
         * 3、检查结果
         */
        val jcjgLabel = App.getContext().getString(R.string.jcjgLabel)
        paint.typeface = Typeface.defaultFromStyle(Typeface.NORMAL)
        paint.color = "#171A1D".toColorInt()
        paint.textSize = 20F
        canvas.drawText(jcjgLabel, 47F, 350F + paint.textSize, paint)
        val jcxmLabel = App.getContext().getString(R.string.jcxmLabel)
        val radioLabel = App.getContext().getString(R.string.radioLabel)
        val jgpdLabel = App.getContext().getString(R.string.jgpdLabel)
        val ckbzLabel = App.getContext().getString(R.string.ckbzLabel)
        paint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD))
        paint.setColor("#FF171A1D".toColorInt())
        paint.textSize = 20F

        canvas.drawText(jcxmLabel, 60F, 400F + paint.textSize, paint)
        canvas.drawText(radioLabel, 240F, 400F + paint.textSize, paint)
        canvas.drawText(jgpdLabel, 420F, 400F + paint.textSize, paint)
//        canvas.drawText(ckbzLabel, 460F, 300F + paint.textSize, paint)

        val item1Left = 60F
        val item2Left = 240F
        val item3Left = 420F
        val item4Left = 460F

        var itemTopStart = 400F
        paint.typeface = Typeface.defaultFromStyle(Typeface.NORMAL)
        paint.color = "#FF171A1D".toColorInt()
        paint.textSize = 20F
        val data = pdfBean.data
        Timber.w("当前页面data${App.gson.toJson(data)}")
        for (index in data.indices) {
            itemTopStart += 40F
            val columns = data[index]
            val top = itemTopStart + paint.textSize
            val number = index + 1
            canvas.drawText("$number. ${columns[0]}", item1Left, top, paint)
            canvas.drawText(columns[1], item2Left, top, paint)
            canvas.drawText(columns[2], item3Left, top, paint)
//            canvas.drawText("${columns[3]}", item4Left, top, paint)
        }
        // 分割线
        paint.color = "#FF171A1D".toColorInt()
        canvas.drawRect(36F, 770F, 524F + 36F, 770F + 3F, paint)
        /**
         * 4、说明
         */
        paint.typeface = Typeface.defaultFromStyle(Typeface.NORMAL)
        paint.color = "#FF171A1D".toColorInt()
        paint.textSize = 14F
        val smLabel = App.getContext().getString(R.string.smLabel)
        val smContent1 = App.getContext().getString(R.string.smContent1)
        val smContent2 = App.getContext().getString(R.string.smContent2)
        canvas.drawText(smLabel, 43F, 775F + 1 * 20F, paint)
        canvas.drawText(smContent1, 43F, 775F + 2 * 20F, paint)
        canvas.drawText(smContent2, 43F, 775F + 3 * 20F, paint)

        pdfDocument.finishPage(myPage)
        val file = File(pdfBean.outPath)
        if (file.parentFile?.exists() != true) {
            file.parentFile?.mkdirs()
        }

        try {
            pdfDocument.writeTo(FileOutputStream(file))
        } catch (e: Exception) {
            Timber.e(e.message ?: "unknown error")
        } finally {
            pdfDocument.close()
        }
    }

    private fun generate3LjPdf(pdfBean: PdfBean) {
        // for our PDF file. A4纸适配
        val pageWidth = 596
        val pageHeight = 842
        val pdfDocument = PdfDocument()
        val paint = Paint()
        val myPageInfo: PdfDocument.PageInfo? =
            PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val myPage: PdfDocument.Page = pdfDocument.startPage(myPageInfo)
        val canvas: Canvas = myPage.canvas
        // 背景
        paint.setColor("#FFFFFF".toColorInt())
        canvas.drawRect(0F, 0F, pageWidth.toFloat(), pageHeight.toFloat(), paint)

        /**
         * 1、标题与LOGO
         */
        // logo
        pdfBean.logo?.apply {
            val scaledLogo = this.scale(87, 87, false)
            canvas.drawBitmap(scaledLogo, 36F, 22F, paint)
        }
        paint.setTextAlign(Paint.Align.CENTER)
        // 标题
        val title = pdfBean.title
        paint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD))
        paint.textSize = 28F
        paint.setColor("#FF0000".toColorInt())
        canvas.drawText(title, 298F, 30F + paint.textSize, paint)

        // 副标题
        val subTitle = pdfBean.subTitle
        paint.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL))
        paint.setColor("#512DA8".toColorInt())
        paint.textSize = 24F
        canvas.drawText(subTitle, 298F, 70F + paint.textSize, paint)

        paint.textAlign = Paint.Align.LEFT

        /**
         * 2、基础信息
         */
        // 检测信息 ：编号与日期
        val jcbhLabel = App.getContext().getString(R.string.jcbhLabel)
        val jcrqLabel = App.getContext().getString(R.string.jcrqLabel)
        val jcbh = pdfBean.jcbh
        val jcrq = pdfBean.jcrq
        paint.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL))
        paint.setColor("#171A1D".toColorInt())
        paint.textSize = 20F
        canvas.drawText("$jcbhLabel $jcbh", 43F, 140F + paint.textSize, paint)
        canvas.drawText("$jcrqLabel $jcrq", 390F, 140F + paint.textSize, paint)
        // 分割线
        paint.setColor("#FF171A1D".toColorInt())
        canvas.drawRect(36F, 180F, 524F + 36F, 180F + 3F, paint)

        val xmLabel = App.getContext().getString(R.string.xmLabel)
        val xbLabel = App.getContext().getString(R.string.xbLabel)
        val nlLabel = App.getContext().getString(R.string.nlLabel)
        val xm = pdfBean.xm
        val xb = pdfBean.xb
        val nl = pdfBean.nl
        paint.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL))
        paint.setColor("#FF171A1D".toColorInt())
        paint.textSize = 20F
        canvas.drawText("$xmLabel $xm", 65F, 210F + paint.textSize, paint)
        canvas.drawText("$xbLabel $xb", 246F, 210F + paint.textSize, paint)
        canvas.drawText("$nlLabel $nl", 426F, 210F + paint.textSize, paint)
        // 分割线
        paint.setColor("#FFD4D4D4".toColorInt())
        canvas.drawRect(54F, 260F, 488F + 54F, 260F + 1F, paint)

        val sjIdLabel = App.getContext().getString(R.string.sjIdLabel)
//        val ybIdLabel = App.getContext().getString(R.string.ybIdLabel)
        val sjId = pdfBean.sjId
//        val ybId = pdfBean.ybId
        paint.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL))
        paint.setColor("#FF171A1D".toColorInt())
        paint.textSize = 20F
        canvas.drawText("$sjIdLabel $sjId", 65F, 280F + paint.textSize, paint)
        //canvas.drawText("$ybIdLabel $ybId", 246F, 214F + paint.textSize, paint)

        // 分割线
        paint.setColor("#FF171A1D".toColorInt())
        canvas.drawRect(36F, 330F, 524F + 36F, 330F + 2F, paint)
        /**
         * 3、检查结果
         */
        val jcjgLabel = App.getContext().getString(R.string.jcjgLabel)
        paint.typeface = Typeface.defaultFromStyle(Typeface.NORMAL)
        paint.color = "#171A1D".toColorInt()
        paint.textSize = 20F
        canvas.drawText(jcjgLabel, 47F, 350F + paint.textSize, paint)
        val jcxmLabel = App.getContext().getString(R.string.jcxmLabel)
        val radioLabel = App.getContext().getString(R.string.radioLabel)
        val jgpdLabel = App.getContext().getString(R.string.jgpdLabel)
        val ckbzLabel = App.getContext().getString(R.string.ckbzLabel)
        paint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD))
        paint.setColor("#FF171A1D".toColorInt())
        paint.textSize = 20F

        canvas.drawText(jcxmLabel, 60F, 400F + paint.textSize, paint)
        canvas.drawText(radioLabel, 240F, 400F + paint.textSize, paint)
        canvas.drawText(jgpdLabel, 420F, 400F + paint.textSize, paint)
//        canvas.drawText(ckbzLabel, 460F, 300F + paint.textSize, paint)

        val item1Left = 60F
        val item2Left = 240F
        val item3Left = 420F
        val item4Left = 460F

        var itemTopStart = 400F
        paint.typeface = Typeface.defaultFromStyle(Typeface.NORMAL)
        paint.color = "#FF171A1D".toColorInt()
        paint.textSize = 20F
        val data = pdfBean.data
        Timber.w("当前页面data${App.gson.toJson(data)}")
        for (index in data.indices) {
            itemTopStart += 40F
            val columns = data[index]
            val top = itemTopStart + paint.textSize
            val number = index + 1
            canvas.drawText("$number. ${columns[0]}", item1Left, top, paint)
            canvas.drawText(columns[1], item2Left, top, paint)
            canvas.drawText(columns[2], item3Left, top, paint)
//            canvas.drawText("${columns[3]}", item4Left, top, paint)
        }
        // 分割线
        paint.color = "#FF171A1D".toColorInt()
        canvas.drawRect(36F, 770F, 524F + 36F, 770F + 3F, paint)
        /**
         * 4、说明
         */
        paint.typeface = Typeface.defaultFromStyle(Typeface.NORMAL)
        paint.color = "#FF171A1D".toColorInt()
        paint.textSize = 14F
        val smLabel = App.getContext().getString(R.string.smLabel)
        val smContent1 = App.getContext().getString(R.string.smContent1)
        val smContent2 = App.getContext().getString(R.string.smContent2)
        canvas.drawText(smLabel, 43F, 775F + 1 * 20F, paint)
        canvas.drawText(smContent1, 43F, 775F + 2 * 20F, paint)
        canvas.drawText(smContent2, 43F, 775F + 3 * 20F, paint)

        pdfDocument.finishPage(myPage)
        val file = File(pdfBean.outPath)
        if (file.parentFile?.exists() != true) {
            file.parentFile?.mkdirs()
        }

        try {
            pdfDocument.writeTo(FileOutputStream(file))
        } catch (e: Exception) {
            Timber.e(e.message ?: "unknown error")
        } finally {
            pdfDocument.close()
        }
    }

    private fun generate2LJAPdf(pdfBean: PdfBean) {
        // for our PDF file. A4纸适配
        val pageWidth = 596
        val pageHeight = 842
        val pdfDocument = PdfDocument()
        val paint = Paint()
        val myPageInfo: PdfDocument.PageInfo? =
            PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val myPage: PdfDocument.Page = pdfDocument.startPage(myPageInfo)
        val canvas: Canvas = myPage.canvas
        // 背景
        paint.setColor("#FFFFFF".toColorInt())
        canvas.drawRect(0F, 0F, pageWidth.toFloat(), pageHeight.toFloat(), paint)

        /**
         * 1、标题与LOGO
         */
        // logo
        pdfBean.logo?.apply {
            val scaledLogo = this.scale(87, 87, false)
            canvas.drawBitmap(scaledLogo, 36F, 22F, paint)
        }
        paint.setTextAlign(Paint.Align.CENTER)
        // 标题
        val title = pdfBean.title
        paint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD))
        paint.textSize = 28F
        paint.setColor("#FF0000".toColorInt())
        canvas.drawText(title, 298F, 30F + paint.textSize, paint)

        // 副标题
        val subTitle = pdfBean.subTitle
        paint.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL))
        paint.setColor("#512DA8".toColorInt())
        paint.textSize = 24F
        canvas.drawText(subTitle, 298F, 70F + paint.textSize, paint)

        paint.textAlign = Paint.Align.LEFT

        /**
         * 2、基础信息
         */
        // 检测信息 ：编号与日期
        val jcbhLabel = App.getContext().getString(R.string.jcbhLabel)
        val jcrqLabel = App.getContext().getString(R.string.jcrqLabel)
        val jcbh = pdfBean.jcbh
        val jcrq = pdfBean.jcrq
        paint.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL))
        paint.setColor("#171A1D".toColorInt())
        paint.textSize = 20F
        canvas.drawText("$jcbhLabel $jcbh", 43F, 140F + paint.textSize, paint)
        canvas.drawText("$jcrqLabel $jcrq", 390F, 140F + paint.textSize, paint)
        // 分割线
        paint.setColor("#FF171A1D".toColorInt())
        canvas.drawRect(36F, 180F, 524F + 36F, 180F + 3F, paint)

        val xmLabel = App.getContext().getString(R.string.xmLabel)
        val xbLabel = App.getContext().getString(R.string.xbLabel)
        val nlLabel = App.getContext().getString(R.string.nlLabel)
        val xm = pdfBean.xm
        val xb = pdfBean.xb
        val nl = pdfBean.nl
        paint.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL))
        paint.setColor("#FF171A1D".toColorInt())
        paint.textSize = 20F
        canvas.drawText("$xmLabel $xm", 65F, 210F + paint.textSize, paint)
        canvas.drawText("$xbLabel $xb", 246F, 210F + paint.textSize, paint)
        canvas.drawText("$nlLabel $nl", 426F, 210F + paint.textSize, paint)
        // 分割线
        paint.setColor("#FFD4D4D4".toColorInt())
        canvas.drawRect(54F, 260F, 488F + 54F, 260F + 1F, paint)

        val sjIdLabel = App.getContext().getString(R.string.sjIdLabel)
//        val ybIdLabel = App.getContext().getString(R.string.ybIdLabel)
        val sjId = pdfBean.sjId
//        val ybId = pdfBean.ybId
        paint.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL))
        paint.setColor("#FF171A1D".toColorInt())
        paint.textSize = 20F
        canvas.drawText("$sjIdLabel $sjId", 65F, 280F + paint.textSize, paint)
        //canvas.drawText("$ybIdLabel $ybId", 246F, 214F + paint.textSize, paint)

        // 分割线
        paint.setColor("#FF171A1D".toColorInt())
        canvas.drawRect(36F, 330F, 524F + 36F, 330F + 2F, paint)
        /**
         * 3、检查结果
         */
        val jcjgLabel = App.getContext().getString(R.string.jcjgLabel)
        paint.typeface = Typeface.defaultFromStyle(Typeface.NORMAL)
        paint.color = "#171A1D".toColorInt()
        paint.textSize = 20F
        canvas.drawText(jcjgLabel, 47F, 350F + paint.textSize, paint)
        val jcxmLabel = App.getContext().getString(R.string.jcxmLabel)
        val radioLabel = App.getContext().getString(R.string.radioLabel)
        val jgpdLabel = App.getContext().getString(R.string.jgpdLabel)
        val ckbzLabel = App.getContext().getString(R.string.ckbzLabel)
        paint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD))
        paint.setColor("#FF171A1D".toColorInt())
        paint.textSize = 20F

        canvas.drawText(jcxmLabel, 60F, 400F + paint.textSize, paint)
        canvas.drawText(radioLabel, 240F, 400F + paint.textSize, paint)
        canvas.drawText(jgpdLabel, 420F, 400F + paint.textSize, paint)
//        canvas.drawText(ckbzLabel, 460F, 300F + paint.textSize, paint)

        val item1Left = 60F
        val item2Left = 240F
        val item3Left = 420F
        val item4Left = 460F

        var itemTopStart = 400F
        paint.typeface = Typeface.defaultFromStyle(Typeface.NORMAL)
        paint.color = "#FF171A1D".toColorInt()
        paint.textSize = 20F
        val data = pdfBean.data
        Timber.w("当前页面data${App.gson.toJson(data)}")
        for (index in data.indices) {
            itemTopStart += 40F
            val columns = data[index]
            val top = itemTopStart + paint.textSize
            val number = index + 1
            canvas.drawText("$number. ${columns[0]}", item1Left, top, paint)
            canvas.drawText(columns[1], item2Left, top, paint)
            canvas.drawText(columns[2], item3Left, top, paint)
//            canvas.drawText("${columns[3]}", item4Left, top, paint)
        }
        // 分割线
        paint.color = "#FF171A1D".toColorInt()
        canvas.drawRect(36F, 770F, 524F + 36F, 770F + 3F, paint)
        /**
         * 4、说明
         */
        paint.typeface = Typeface.defaultFromStyle(Typeface.NORMAL)
        paint.color = "#FF171A1D".toColorInt()
        paint.textSize = 14F
        val smLabel = App.getContext().getString(R.string.smLabel)
        val smContent1 = App.getContext().getString(R.string.smContent1)
        val smContent2 = App.getContext().getString(R.string.smContent2)
        canvas.drawText(smLabel, 43F, 775F + 1 * 20F, paint)
        canvas.drawText(smContent1, 43F, 775F + 2 * 20F, paint)
        canvas.drawText(smContent2, 43F, 775F + 3 * 20F, paint)

        pdfDocument.finishPage(myPage)
        val file = File(pdfBean.outPath)
        if (file.parentFile?.exists() != true) {
            file.parentFile?.mkdirs()
        }

        try {
            pdfDocument.writeTo(FileOutputStream(file))
        } catch (e: Exception) {
            Timber.e(e.message ?: "unknown error")
        } finally {
            pdfDocument.close()
        }
    }

    private fun generate2LJBPdf(pdfBean: PdfBean) {
        // for our PDF file. A4纸适配
        val pageWidth = 596
        val pageHeight = 842
        val pdfDocument = PdfDocument()
        val paint = Paint()
        val myPageInfo: PdfDocument.PageInfo? =
            PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val myPage: PdfDocument.Page = pdfDocument.startPage(myPageInfo)
        val canvas: Canvas = myPage.canvas
        // 背景
        paint.setColor("#FFFFFF".toColorInt())
        canvas.drawRect(0F, 0F, pageWidth.toFloat(), pageHeight.toFloat(), paint)

        /**
         * 1、标题与LOGO
         */
        // logo
        pdfBean.logo?.apply {
            val scaledLogo = this.scale(87, 87, false)
            canvas.drawBitmap(scaledLogo, 36F, 22F, paint)
        }
        paint.setTextAlign(Paint.Align.CENTER)
        // 标题
        val title = pdfBean.title
        paint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD))
        paint.textSize = 28F
        paint.setColor("#FF0000".toColorInt())
        canvas.drawText(title, 298F, 30F + paint.textSize, paint)

        // 副标题
        val subTitle = pdfBean.subTitle
        paint.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL))
        paint.setColor("#512DA8".toColorInt())
        paint.textSize = 24F
        canvas.drawText(subTitle, 298F, 70F + paint.textSize, paint)

        paint.textAlign = Paint.Align.LEFT

        /**
         * 2、基础信息
         */
        // 检测信息 ：编号与日期
        val jcbhLabel = App.getContext().getString(R.string.jcbhLabel)
        val jcrqLabel = App.getContext().getString(R.string.jcrqLabel)
        val jcbh = pdfBean.jcbh
        val jcrq = pdfBean.jcrq
        paint.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL))
        paint.setColor("#171A1D".toColorInt())
        paint.textSize = 20F
        canvas.drawText("$jcbhLabel $jcbh", 43F, 140F + paint.textSize, paint)
        canvas.drawText("$jcrqLabel $jcrq", 390F, 140F + paint.textSize, paint)
        // 分割线
        paint.setColor("#FF171A1D".toColorInt())
        canvas.drawRect(36F, 180F, 524F + 36F, 180F + 3F, paint)

        val xmLabel = App.getContext().getString(R.string.xmLabel)
        val xbLabel = App.getContext().getString(R.string.xbLabel)
        val nlLabel = App.getContext().getString(R.string.nlLabel)
        val xm = pdfBean.xm
        val xb = pdfBean.xb
        val nl = pdfBean.nl
        paint.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL))
        paint.setColor("#FF171A1D".toColorInt())
        paint.textSize = 20F
        canvas.drawText("$xmLabel $xm", 65F, 210F + paint.textSize, paint)
        canvas.drawText("$xbLabel $xb", 246F, 210F + paint.textSize, paint)
        canvas.drawText("$nlLabel $nl", 426F, 210F + paint.textSize, paint)
        // 分割线
        paint.setColor("#FFD4D4D4".toColorInt())
        canvas.drawRect(54F, 260F, 488F + 54F, 260F + 1F, paint)

        val sjIdLabel = App.getContext().getString(R.string.sjIdLabel)
//        val ybIdLabel = App.getContext().getString(R.string.ybIdLabel)
        val sjId = pdfBean.sjId
//        val ybId = pdfBean.ybId
        paint.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL))
        paint.setColor("#FF171A1D".toColorInt())
        paint.textSize = 20F
        canvas.drawText("$sjIdLabel $sjId", 65F, 280F + paint.textSize, paint)
        //canvas.drawText("$ybIdLabel $ybId", 246F, 214F + paint.textSize, paint)

        // 分割线
        paint.setColor("#FF171A1D".toColorInt())
        canvas.drawRect(36F, 330F, 524F + 36F, 330F + 2F, paint)
        /**
         * 3、检查结果
         */
        val jcjgLabel = App.getContext().getString(R.string.jcjgLabel)
        paint.typeface = Typeface.defaultFromStyle(Typeface.NORMAL)
        paint.color = "#171A1D".toColorInt()
        paint.textSize = 20F
        canvas.drawText(jcjgLabel, 47F, 350F + paint.textSize, paint)
        val jcxmLabel = App.getContext().getString(R.string.jcxmLabel)
        val radioLabel = App.getContext().getString(R.string.radioLabel)
        val jgpdLabel = App.getContext().getString(R.string.jgpdLabel)
        val ckbzLabel = App.getContext().getString(R.string.ckbzLabel)
        paint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD))
        paint.setColor("#FF171A1D".toColorInt())
        paint.textSize = 20F

        canvas.drawText(jcxmLabel, 60F, 400F + paint.textSize, paint)
        canvas.drawText(radioLabel, 240F, 400F + paint.textSize, paint)
        canvas.drawText(jgpdLabel, 420F, 400F + paint.textSize, paint)
//        canvas.drawText(ckbzLabel, 460F, 300F + paint.textSize, paint)

        val item1Left = 60F
        val item2Left = 240F
        val item3Left = 420F
        val item4Left = 460F

        var itemTopStart = 400F
        paint.typeface = Typeface.defaultFromStyle(Typeface.NORMAL)
        paint.color = "#FF171A1D".toColorInt()
        paint.textSize = 20F
        val data = pdfBean.data
        Timber.w("当前页面data${App.gson.toJson(data)}")
        for (index in data.indices) {
            itemTopStart += 40F
            val columns = data[index]
            val top = itemTopStart + paint.textSize
            val number = index + 1
            canvas.drawText("$number. ${columns[0]}", item1Left, top, paint)
            canvas.drawText(columns[1], item2Left, top, paint)
            canvas.drawText(columns[2], item3Left, top, paint)
//            canvas.drawText("${columns[3]}", item4Left, top, paint)
        }
        // 分割线
        paint.color = "#FF171A1D".toColorInt()
        canvas.drawRect(36F, 770F, 524F + 36F, 770F + 3F, paint)
        /**
         * 4、说明
         */
        paint.typeface = Typeface.defaultFromStyle(Typeface.NORMAL)
        paint.color = "#FF171A1D".toColorInt()
        paint.textSize = 14F
        val smLabel = App.getContext().getString(R.string.smLabel)
        val smContent1 = App.getContext().getString(R.string.smContent1)
        val smContent2 = App.getContext().getString(R.string.smContent2)
        canvas.drawText(smLabel, 43F, 775F + 1 * 20F, paint)
        canvas.drawText(smContent1, 43F, 775F + 2 * 20F, paint)
        canvas.drawText(smContent2, 43F, 775F + 3 * 20F, paint)

        pdfDocument.finishPage(myPage)
        val file = File(pdfBean.outPath)
        if (file.parentFile?.exists() != true) {
            file.parentFile?.mkdirs()
        }

        try {
            pdfDocument.writeTo(FileOutputStream(file))
        } catch (e: Exception) {
            Timber.e(e.message ?: "unknown error")
        } finally {
            pdfDocument.close()
        }
    }
}