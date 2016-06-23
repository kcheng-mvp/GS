#!/usr/bin/env groovy



@Grab(group='jaxen', module='jaxen', version='1.1.4')
@Grab(group='com.h2database', module='h2', version='1.3.175')
@GrabConfig(systemClassLoader=true)
@GrabExclude('xml-apis:xml-apis')

import javax.xml.parsers.DocumentBuilderFactory;
import groovy.sql.Sql;

/*
if(args.size() < 2){
	println "Desc  -> Find jar/zip files which its entry's name matches the given name pattern from the target folder recursively";
	println "Usage -> findJarZip arg1 arg2";
	println "arg1  -> File name pattern";
	println "arg2  -> Target folder";
	println "Sample -> findJarZip.groovy Map.class /usr/local/java";
	return -1;
}
*/
def searchStr = args[0].toUpperCase();

//def strPattern = ~ /(?i)[^a-zA-Z_]${searchStr}\s+/
//println (("1T_htl_SUPPLY _a" =~ strPattern)[0])
//return -1


def home= new File(System.getProperty("user.home"));
def dbHome = new File(home,".h2");
def driver ="org.h2.Driver";
def url = "jdbc:h2:~/.h2/fccode";



def init = {
	def sqlCon = Sql.newInstance(url,"sa","",driver);
	sqlCon.execute("drop table if exists xmlmapping;");
	sqlCon.execute('''
	create table if not exists xmlmapping (
	ID INTEGER IDENTITY,
	file varchar(150),
	sqlType varchar(10),
	xmlfunction varchar(50),
	javaFile varchar(200),
	javaMethod varchar(50),
);
''');
	sqlCon.close();
}


init();

def result = new File("/tmp/result.csv");
result = new FileOutputStream(result);
def bw = new BufferedWriter(new OutputStreamWriter(result,"UTF-8"));
bw.write("|File|SQLType|ID |Remote Call |Call Stack |新表是否满足本SQL")
bw.newLine();

args[0].each{searchInputFile(bw, searchStr,new File(it))};

def searchInputFile(bw, text, inputFile){
	def filePattern = ~/.*\.(xml)$/;
	def strPattern = ~ /(?i)[^a-zA-Z_]${text}\s+/
	if (inputFile.isDirectory()){
		inputFile.eachFileRecurse{
			if(!it.isDirectory() && it.getName().toLowerCase() =~ filePattern && it.getName() !="temp.xml"
				&& it.getName() != "build.xml" && it.getName() != "ivy.xml" && it.getName().startsWith("Sql")){
				def tx = it.getText().toUpperCase()
				//if(tx =~ strPattern){
					processMapper(bw, text,it);
				//}
				
			}
		}
	} else {
		if(inputFile.getName() =~ filePattern && inputFile.getName() !="temp.xml" &&
			it.getName() != "build.xml" && it.getName() != "ivy.xml" && it.getName().startsWith("Sql")){
			def tx = inputFile.getText().toUpperCase()
			//if(tx =~ strPattern){
				processMapper(bw, text,inputFile);
			//}
			
		}
	}
}

bw.close();


def processMapper(bw, text, file){
	def driver ="org.h2.Driver";
    def url = "jdbc:h2:~/.h2/fccode";
	def sqlCon = Sql.newInstance(url,"sa","",driver);
	def sqlType =["select","update","insert"];
	def strPattern = ~ /(?i)[^a-zA-Z_]${text}\s+/
	def parser = new XmlParser() 
	parser.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false); 
	// disable dtd
	parser.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false); 
	parser.setFeature("http://xml.org/sax/features/namespaces", false) 

//	println file.name;
	def mapXml = parser.parse(file)
	mapXml.children().each { 
		def sqlText = it.text();
		//if(sqlText =~ strPattern){
			//bw.write("${text}|${file.getPath()}|${it.name()}|${it.@id}|${sqlText}")
			println it.name();
			if(it.@id && sqlType.contains(it.name())){
				bw.write("|${file.getPath()}|${it.name()}|${it.@id}| - | - | - ")
				bw.newLine();
				//println "${file.getCanonicalPath()} - ${it.@id}"
				println "${file.getPath()} - ${it.name()} - ${it.@id}"
				sqlCon.execute("insert into XMLMAPPING(file,sqlType,xmlfunction) values('${file.getPath()}','${it.name()}','${it.@id}')");
			}

			
			//println sqlText	
		//}
		
	}
	sqlCon.close();
}



