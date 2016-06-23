#! /usr/bin/env groovy

@Grab(group='net.sourceforge.plantuml', module='plantuml', version='7994')
import net.sourceforge.plantuml.SourceFileReader

// GRAPHVIZ_HOME
// D:\tools\graphviz-2.34\release

def env = System.getenv();
String GRAPHVIZ_HOME = env['GRAPHVIZ_HOME']
if(!GRAPHVIZ_HOME){
	println "Error : Please set environment GRAPHVIZ_HOME, refer to http://plantuml.sourceforge.net/graphvizdot.html";
	return -1;
}

def home= new File(System.getProperty("user.home"));
def temp = System.getProperty("java.io.tmpdir");

if(!args) {
	 println "Please specify the file eg: plantuml hello";
	 return -1;
}


def scriptDir = new File(getClass().protectionDomain.codeSource.location.path);
def base = new File(scriptDir.parentFile.parentFile,"diagram");
def output = new File(temp,"plantuml");
if (output.exists()){
	output.eachFileRecurse(groovy.io.FileType.FILES){file ->
		file.delete();
	}
}
def uml = new File(base, "${args[0]}");
if(uml.exists()){
	def reader = new SourceFileReader(uml, output);
	def list = reader.getGeneratedImages();
	def png = list.get(0).getPngFile();
	println "Generate file at "+png.absolutePath;
} else {
	println "Can not find the file ${args[0]} ......";
	uml = new File(base, "script");
	uml.eachFileRecurse{file ->
		if(file.name.endsWith(".uml")){
			println file.name;
		}
	}
}



