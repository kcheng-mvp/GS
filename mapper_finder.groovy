#!/usr/bin/env groovy

@Grapes([
	@Grab(group='jaxen', module='jaxen', version='1.1.4')
])
@GrabExclude('xml-apis:xml-apis')

import javax.xml.parsers.DocumentBuilderFactory;


if(args.size() < 2){
	println "Desc  -> Find jar/zip files which its entry's name matches the given name pattern from the target folder recursively";
	println "Usage -> findJarZip arg1 arg2";
	println "arg1  -> File name pattern";
	println "arg2  -> Target folder";
	println "Sample -> findJarZip.groovy Map.class /usr/local/java";
	return -1;
}
def searchStr = args[0].toUpperCase();

//def strPattern = ~ /(?i)[^a-zA-Z_]${searchStr}\s+/
//println (("1T_htl_SUPPLY _a" =~ strPattern)[0])
//return -1

def home= new File(System.getProperty("user.home"));

def result = new File("/tmp/result.csv");
result = new FileOutputStream(result);
def bw = new BufferedWriter(new OutputStreamWriter(result,"UTF-8"));
bw.write("|File|SQLType|ID |Remote Call |Call Stack |新表是否满足本SQL")
bw.newLine();

args[1..-1].each{searchInputFile(bw, searchStr,new File(it))};

def searchInputFile(bw, text, inputFile){
	def filePattern = ~/.*\.(xml)$/;
	def strPattern = ~ /(?i)[^a-zA-Z_]${text}\s+/
	if (inputFile.isDirectory()){
		inputFile.eachFileRecurse{
			if(!it.isDirectory() && it.getName().toLowerCase() =~ filePattern ){
				def tx = it.getText().toUpperCase()
				if(tx =~ strPattern){
					processMapper(bw, text,it);
				}
				
			}
		}
	} else {
		if(inputFile.getName() =~ filePattern){
			def tx = inputFile.getText().toUpperCase()
			if(tx =~ strPattern){
				processMapper(bw, text,inputFile);
			}
			
		}
	}
}

bw.close();

def processMapper(bw, text, file){
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
		if(sqlText =~ strPattern){
			//bw.write("${text}|${file.getPath()}|${it.name()}|${it.@id}|${sqlText}")
			bw.write("|${file.getPath()}|${it.name()}|${it.@id}| - | - | - ")
			bw.newLine();
			//println "${file.getCanonicalPath()} - ${it.@id}"
			println "${file.getPath()} - ${it.name()} - ${it.@id}"
			
			//println sqlText	
		}
		
	}
}


