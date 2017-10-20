#! /usr/bin/env groovy

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import static org.apache.poi.ss.usermodel.Cell.*;
import java.text.DateFormat;
import static java.text.DateFormat.*;
import static org.apache.poi.hssf.util.HSSFColor.*;
import org.apache.poi.hssf.usermodel.HSSFCell;
import java.text.SimpleDateFormat;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.ss.usermodel.CellStyle;

@Grab(group = 'org.apache.poi', module = 'poi-ooxml', version = '3.17')
@GrabExclude('xml-apis:xml-apis')

/**
 * dataMap -> {sheetName : dataList}* dataList -> List<List>
 */


def FORMATTER_NUMBER = "###,##0.00";
def FORMATTER_DATE = "yyyy-MM-dd";
def FORMATTER_TIMESTAMP = "yyyy-MM-dd HH:mm:ss";


def generateXls(Map dataMap, Closure... closures) {
    if (!dataMap) return
    def wb = new HSSFWorkbook();
    def createHelper = wb.getCreationHelper();

    def cellStyleMap = new HashMap();


    dataMap.keySet().each {
        def sheet = wb.createSheet(it);
        def dataList = dataMap.get(it);
        dataList.eachWithIndex { rowData, index ->
            def row = sheet.createRow(index);
            rowData.eachWithIndex { colValue, colIndex ->
                def cell = row.createCell(colIndex);

                if (closures) {
                    def formatted = closures[0](colIndex, colValue);
                    // set value
                    cell.setCellValue(formatted['value']);

                    def format = formatted['format']
                    if (format) {
                        if (!cellStyleMap.get(format)) {
                            CellStyle cellStyle = wb.createCellStyle();
                            cellStyle.setDataFormat(createHelper.createDataFormat().getFormat(format));
                            cellStyleMap.put(format, cellStyle);
                        }
                        // set style
                        cell.setCellStyle(cellStyleMap.get(style));
                    }
                } else {
                    cell.setCellValue(colValue);
                }

            }
        }

    }
    // Write the output to a file
    def sdf = new SimpleDateFormat("yyyyMMdd-HHmmss")
    def calendar = Calendar.getInstance();
    FileOutputStream fileOut = new FileOutputStream("${sdf.format(calendar.getTime())}.xls");
    wb.write(fileOut);
    fileOut.close();
}