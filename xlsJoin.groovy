#! /usr/bin/env groovy
import org.apache.poi.hssf.usermodel.HSSFWorkbook

import static org.apache.poi.hssf.util.HSSFColor.*
import static org.apache.poi.ss.usermodel.Cell.*
import org.apache.poi.ss.usermodel.CellType
import java.text.DecimalFormat

@Grapes([
        @Grab(group = 'com.google.guava', module = 'guava', version = '18.0'),
        @Grab(group = 'jaxen', module = 'jaxen', version = '1.1.4'),
        @Grab(group = 'org.apache.poi', module = 'poi-ooxml', version = '3.17'),
        @GrabExclude('xml-apis:xml-apis')
])

def home = System.getProperty("user.home");

println args

if (!args || args.length != 2) {
    println "Please input file names"
    return -1
}

def rowMap = new HashMap<String, String>()
def cellRange = 1..4
args.each { name ->
    def wba = new HSSFWorkbook(new FileInputStream(name));
    def evaluator = wba.getCreationHelper().createFormulaEvaluator()
    def rowIterator = wba.getSheetAt(0).rowIterator();
    rowIterator.eachWithIndex { row, idx ->
        if (row && row.getCell(0)) {
            def key = row.getCell(0).getStringCellValue()
            if (!rowMap.containsKey(key)) {
                def list = [] as List
                list.add(name)
                list.add(key)
                for (int r = 1; r < 5; r++) {
                    def cell = row.getCell(r)
                    if (cell != null) {
                        def value = null
                        try {
                            value = cell.getStringCellValue()
                        } catch (Exception e) {
                            value = cell.getNumericCellValue()
                            DecimalFormat df = new DecimalFormat("#");
                            def fractionalDigits = 2; // say 2
                            df.setMaximumFractionDigits(fractionalDigits);
                            value = df.format(value);
                            value = df.format(value);
                        }

                        println value
                        list.add(value)
                    }
                }
                def valueString = list.join("#")
//				println valueString
                rowMap.put(key, valueString);
            }
        }
    }
}
def wb = new HSSFWorkbook()
def sheet = wb.createSheet()
rowMap.values().eachWithIndex { String entry, int i ->
    def row = sheet.createRow(i)
    def items = entry.split("#")
    items.eachWithIndex { String colValue, int colIdx ->
        def cell = row.createCell(colIdx)
        cell.setCellValue(colValue)
    }

}

def f = new File("${home}/workbook.xls")
println "Controller info is generated at : ${f.absolutePath}"
FileOutputStream fileOut = new FileOutputStream(f);
wb.write(fileOut);
fileOut.close();




