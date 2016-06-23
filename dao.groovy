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
def url = "jdbc:h2:~/.h2/fccode";

/*
def text ="sendFailCallBack"
def name="public CallBackResponse sendFailCallBack("
def strPattern = ~ /\s+${text}\s*\(/

if (name =~ strPattern){
	println 1
} else {
	println 0
}
*/





def start = {
	def xmlFunction = [];
	def sqlCon = Sql.newInstance(url,"sa","",driver);
	def query ="select count(1),xmlfunction from xmlmapping group by xmlfunction having count(1) =1";
	sqlCon.eachRow(query){row ->
		xmlFunction.add(row['XMLFUNCTION']);
	}

	args[0].each{searchInputFile(xmlFunction,new File(it))};

	sqlCon.close();
}


def searchInputFile(xmlFunction, inputFile){
	def filePattern = ~/.*\.(java)$/;
	//def strPattern = ~ /\s+${text}\s*\(/
	if (inputFile.isDirectory()){
		inputFile.eachFileRecurse{
			if(!it.isDirectory() && it.getName().toLowerCase() =~ filePattern ){
				//println it.getName();
				processMapper(xmlFunction,it);	
			}
		}
	} else {
		if(inputFile.getName() =~ filePattern ){
			//println it.getName();
			processMapper(xmlFunction,inputFile);
		}
	}
}


def processMapper(xmlFunction,file){
	xmlFunction.each{text ->
		def strPattern = ~ /\s+${text}\s*\(/;
		file.eachLine{line ->
			if(line =~ strPattern){
				println "${file.getName()} -> ${line}";
			}
		}
	}
}

start();

	
