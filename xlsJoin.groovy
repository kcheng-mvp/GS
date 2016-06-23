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
import java.util.jar.*;
import groovy.sql.Sql;
import javax.xml.parsers.DocumentBuilderFactory;

@Grab(group='org.apache.poi', module='poi-ooxml', version='3.10-beta2')
@GrabExclude('xml-apis:xml-apis')

def home= System.getProperty("user.home");
def XLS_A_COL = [0,4,5];
def XLS_B_COL = [1,5,6];

def xlsa = "C:/Users/xianwche/Desktop/log/2014w03/slc01bpx.xls"
def xlsb = "C:/Users/xianwche/Desktop/log/2014w03/rel9mat3.xls"
def wba = new HSSFWorkbook(new FileInputStream(xlsa));
def wbb = new HSSFWorkbook(new FileInputStream(xlsb));
def sheeta = wba.getSheetAt(0);
def sheetb = wbb.getSheetAt(0);

def valueSet = new HashSet<String>();
def rowIterator = sheeta.rowIterator();
rowIterator.eachWithIndex {row , idx ->
	def keyStr = new StringBuffer();
	XLS_A_COL.each{col ->
		def cell = row.getCell(col);
		if(cell) keyStr.append(cell.getStringCellValue());
	}
	if(keyStr){
		valueSet.add(keyStr.toString());
	}
}
rowIterator = sheetb.rowIterator();
rowIterator.eachWithIndex {row , idx ->
	def keyStr = new StringBuffer();
	XLS_B_COL.each{col ->
		def cell = row.getCell(col);
		if(cell) keyStr.append(cell.getStringCellValue());
	}
	if(valueSet.contains(keyStr.toString())){
		cell = row.getCell(0);
		if(!cell) cell = row.createCell(0);
		cell.setCellValue("Y");
	}
}


FileOutputStream fileOut = new FileOutputStream("${home}/workbook.xls");
wbb.write(fileOut);
fileOut.close();



