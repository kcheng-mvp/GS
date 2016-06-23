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


@Grapes([
	@Grab(group='org.apache.poi', module='poi-ooxml', version='3.8'),
	@GrabConfig(systemClassLoader=true)
])


def xls = null;
def xwiki = null;
if(args.size()==0){
	println "Please input xls file! xls2xwiki file_name";
	return -1;
} 

xls = new File(args[0]);
if(! xls.exists()  || ! xls.isFile()){
	println "can not find this file ${args[0]}, or it's a folder";
	return -1;
} else {
	def path = xls.getAbsolutePath().minus(xls.getName());
	xwiki = new File(path, "${xls.getName()}.xwiki");
	xwiki = new FileOutputStream(xwiki);
}

class MergedRange {
	int fromRow;
	int toRow;
	int fromCol;
	int toCol;
	boolean processed= false;
	static mergedMap = new HashMap<String,MergedRange>();
	static MergedRange getMergedRange(int row, col){
		for(Map.Entry<String,MergedRange> item : mergedMap.entrySet()){
			MergedRange v = item.getValue();
			if(row >= v.fromRow && row <=v.toRow && col >= v.fromCol && col <=v.toCol){
				return mergedMap.get(item.getKey());
			}
		}
		return null;
	}
}

//out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(args[1]), "UTF-8"));

def bw = new BufferedWriter(new OutputStreamWriter(xwiki,"UTF-8"));



def wb = new HSSFWorkbook(new FileInputStream(xls));
def sheet = wb.getSheetAt(0);


for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
	MergedRange merge = new MergedRange();
	def mRegion = sheet.getMergedRegionAt(i);
	
	merge.fromRow = mRegion.getRowFrom();
	merge.toRow = mRegion.getRowTo();
	merge.fromCol = mRegion.getColumnFrom();
	merge.toCol = mRegion.getColumnTo();
	//println "${merge.fromRow} -> ${merge.fromCol} -> ${merge.fromRow+""+merge.fromCol}";
	MergedRange.mergedMap.put(merge.fromRow+""+merge.fromCol,merge);
	
}
def rowIterator = sheet.rowIterator();

//bw.write('''{|class="dotted" ''');
//bw.newLine();

def maxCellNum = 0;
rowIterator.eachWithIndex {row , idx ->
	maxCellNum = row.getLastCellNum() > maxCellNum ? row.getLastCellNum() : maxCellNum;
}

println maxCellNum;
def sdf = new SimpleDateFormat("yyyy-MM-dd");

def range = 0..(maxCellNum-1);
rowIterator = sheet.rowIterator();
rowIterator.eachWithIndex {row , idx ->
	range.each{coldx ->
		def cell = row.getCell(coldx);
		def value ="";
		if(cell != null){
			def mergedRange = MergedRange.getMergedRange(idx, coldx);
			//def pos = idx+""+coldx;
			//def merge = MergedRange.mergedMap.get(pos);
			if(mergedRange && !mergedRange.processed){
				bw.write("|");
				println "${mergedRange.fromRow} -> ${mergedRange.toRow}";
				def rowSpan = mergedRange.toRow - mergedRange.fromRow;
				def colSpan = mergedRange.toCol - mergedRange.fromCol;
				if(rowSpan > 0) bw.write("rowspan='${rowSpan+1}'")
				if(colSpan > 0){
					if(rowSpan > 0) {
						bw.write(",colspan='${colSpan+1}' ")
					} else {
						bw.write("colspan='${colSpan+1}' ")
					}
				}
				value = processCellValue(cell);
				
				mergedRange.processed = true;
				bw.write("|${value}");
			} else if(!mergedRange) {
				
				value = processCellValue(cell);
				if(!value) value =""
				def v = coldx == 0 ? "|[[${value}]]" : "|${value}";
				bw.write(v);
			}
			
			
		} else {
			bw.write("|");
		}
	}
	bw.newLine();
}
bw.close();

def processCellValue(def cell){
	def value = null;
	try{
		value = cell.getDateCellValue();
		value = sdf.format(value);
		if(value.indexOf("1900") > -1) {
			throw new RuntimeException("Invalid Date!");
		}
	} catch(Exception e){
		cell.setCellType(CELL_TYPE_STRING);
		value = cell.getRichStringCellValue().getString();
	}
	return value;
}

def processStyle(def cell) {
	def cellStyle = cell.getCellStyle();
	// align
	def align = cellStyle.getAlignment(); 
	def style = "";
	def temp ='''style="text-align:''';
	if(align == CellStyle.ALIGN_CENTER){
		style= temp+'''center;'''
	} else if (align ==CellStyle.ALIGN_RIGHT){
		style= temp+'''right;'''
	}
	return style+ '"';
}
