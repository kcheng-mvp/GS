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
import org.jaxen.dom.DOMXPath;
import javax.xml.parsers.DocumentBuilderFactory;


@Grab(group='com.h2database', module='h2', version='1.3.175')
@GrabConfig(systemClassLoader=true)
@Grab(group='jaxen', module='jaxen', version='1.1.4')
@Grab(group='org.apache.poi', module='poi-ooxml', version='3.10-beta2')
@GrabExclude('xml-apis:xml-apis')


def home= new File(System.getProperty("user.home"));
def dbHome = new File(home,".h2");
def driver ="org.h2.Driver";
def url = "jdbc:h2:~/.h2/fusionLabel";


if(args.size() !=2){
	println "Desc  -> Use this script to generate fusion Title/Label. FusionLabel.groovy folder type";
	println "eg : FusionLabel.groovy . 1 (1 -> label)"
	return -1;
}
//AttrBundle.xlf(Attribute Label), GenBundle.xlf(label)
def filter = args[1] == "1" ? "GenBundle.xlf" : "AttrBundle.xlf";


def init = {
	def sqlCon = Sql.newInstance(url,"sa","",driver);
	sqlCon.execute("drop table if exists label;");
	sqlCon.execute('''
	create table if not exists label (
	ID INTEGER IDENTITY,
	key varchar(150),
	file_name varchar(200),
	label varchar(500),
	module varchar(5)
);
''');
	sqlCon.close();
}
	
	
//

//def scriptDir = new File(getClass().protectionDomain.codeSource.location.path);
def resourceBundle = new File(args[0]);
def processFile = {
	def sqlCon = Sql.newInstance(url,"sa","",driver);
	

	// xml parser
	def dbf = DocumentBuilderFactory.newInstance();
	dbf.setValidating(false);
	dbf.setNamespaceAware(true);
	dbf.setFeature("http://xml.org/sax/features/namespaces", false);
	dbf.setFeature("http://xml.org/sax/features/validation", false);
	dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
	dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
	sqlCon.withTransaction {
		sqlCon.withBatch(100,'INSERT INTO label (key, file_name, label, module) values(?,?,?, ?) ') {stmt ->
			resourceBundle.eachFileRecurse{ 
				if(!it.isDirectory() && it.getName().endsWith(".zip")){
					def zip = new JarFile(it); 
					zip.entries().each{ entry ->
					
						if(entry.name.endsWith(filter)){
							println "entry.name : ${entry.name}";
							def builder    = dbf.newDocumentBuilder();
							//def inputStream = new ByteArrayInputStream(file.getBytes());
							def module = entry.name.split("/")[2];
							def records     = builder.parse(zip.getInputStream(entry)).documentElement;
							new DOMXPath('//trans-unit').selectNodes(records).each{it
								def key = new DOMXPath('@id').stringValueOf(it);
								def label =  new DOMXPath('source/text()').stringValueOf(it);
								stmt.addBatch(key, entry.name,label,module);
							}
						}
					}
				}
			}
		}
	}
	sqlCon.close();
}

def genReport = {
	
	def wb = new HSSFWorkbook();
	def sqlCon = Sql.newInstance(url,"sa","",driver);
	def query ="select module, key, label, file_name from label order by module, label";
	//def query ="select module, key, label, file_name from label where module in ('AP','AR')";
	def currentModule = null;
	def currentSheet = null;
	def cnt = 0;
	sqlCon.eachRow(query){row ->
		def temp = row.module;
		if(!temp.equals(currentModule)){
			if(currentSheet != null)  {
				adjustColumn(currentSheet);
			}
			currentSheet = wb.createSheet(temp);
			currentModule=temp;
			cnt = 0;
		}
		def xlsRow = currentSheet.createRow((short)cnt);
		def cell = xlsRow.createCell(0);
		cell.setCellValue(row.key);
		cell = xlsRow.createCell(1);
		cell.setCellValue(row.label);
		cell = xlsRow.createCell(2);
		cell.setCellValue(row.file_name);
		cnt++;
	}
	sqlCon.close();
	adjustColumn(currentSheet);
	def fileName = filter.equals("GenBundle.xlf") ? "Label1.xls":"Attribute1.xls";
	def wbf = new File(resourceBundle, "${fileName}");
	println "wbf : " + wbf.absolutePath;
	def fileOut = new FileOutputStream(wbf);
	wb.write(fileOut);
	fileOut.close();
}

def adjustColumn(sheet){
	for(i in 0..2){
		sheet.autoSizeColumn(i);
	}
}



init();
processFile();
genReport();
