package poct.device.app.utils.app

import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import poct.device.app.App
import poct.device.app.R
import poct.device.app.state.ViewState
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

object AppExcelUtils {

    @Throws(IOException::class)
    fun readExcelSheetName(file: File?, sheetName: String?): List<List<String>> {
        val dataList: MutableList<List<String>> = ArrayList()
        val inputStream = FileInputStream(file)
        val workbook: Workbook = XSSFWorkbook(inputStream)
        val sheet = workbook.getSheet(sheetName)
        for (row in sheet) {
            if (row.rowNum == 0) {
                // 跳过表头
                continue
            }
            val rowData: MutableList<String> = ArrayList()
            for (cell in row) {
                cell.setCellType(CellType.STRING)
                rowData.add(cell.stringCellValue)
            }
            dataList.add(rowData)
        }
        workbook.close()
        inputStream.close()
        return dataList
    }

    @Throws(IOException::class)
    fun readExcelSheet1(file: File?): List<List<String>>? {
        val dataList: MutableList<List<String>> = ArrayList()
        val inputStream = FileInputStream(file)
        val workbook: Workbook = XSSFWorkbook(inputStream)
        val sheet = workbook.getSheetAt(0)
        for (row in sheet) {
            val rowData: MutableList<String> = ArrayList()
            for (cell in row) {
                rowData.add(cell.stringCellValue)
            }
            dataList.add(rowData)
        }
        workbook.close()
        inputStream.close()
        return dataList
    }

    fun exportExcel(filePath: String, data: List<List<String?>>) {
        // 创建一个Excel工作簿
        val workbook: Workbook = XSSFWorkbook()
        // 创建一个工作表
        val sheet: Sheet = workbook.createSheet("Sheet1")

        for((index, cellList) in data.withIndex()) {
            // 创建一行
            val row: Row = sheet.createRow(index)
            for ((cellIndex, cellValue) in cellList.withIndex()) {
                // 创建单元格并设置值
                val cell: Cell = row.createCell(cellIndex)
                cell.setCellValue(cellValue)
            }
        }
        Timber.w("====${filePath}")
        try {
            // 导出到硬盘
            FileOutputStream(filePath).use {
                workbook.write(it)
            }
        }catch (e: Exception) {
            Timber.w("====${e.message}")
            ViewState.LoadError(App.getContext().getString(R.string.report_export_fail))
        }finally {
            // 关闭工作簿
            workbook.close()
        }
    }

    /**
     * 把值转换为字符串，防止出现null导致错误
     * @param obj
     * @return
     */
    fun valueOf(obj: Any?): String? {
        return obj?.toString() ?: ""
    }


}