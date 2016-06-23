#! /usr/bin/env groovy

import java.util.jar.*;

if(args.size() < 2){
	println "Desc  -> Find jar/zip files which its entry's name matches the given name pattern from the target folder recursively";
	println "Usage -> findJarZip arg1 arg2";
	println "arg1  -> File name pattern";
	println "arg2  -> Target folder";
	println "Sample -> findJarZip.groovy Map.class /usr/local/java";
	return -1;
}
def searchStr = args[0];
args[1..-1].each{searchInputFile(searchStr,new File(it))};
def searchInputFile(text, inputFile){
	def filePattern = ~/.*\.(jar|zip|ear|war)$/;
	if (inputFile.isDirectory()){
		inputFile.eachFileRecurse{
			if(!it.isDirectory() && it.getName() =~ filePattern){
				searchCompressedFile(text,it);
			}
		}
	} else {
		if(inputFile.getName() =~ filePattern){
			searchCompressedFile(text,inputFile);
		}
	}
}

def searchCompressedFile(text, file){
	try{
		new JarFile(file).entries().each{ entry ->
			if(entry.name =~text){
				println "$entry.name : $file.canonicalPath";
			}
		}
	} catch (Exception e){
		println "Failed to open $file.canonicalPath:${e.toString()}";
	}
}
