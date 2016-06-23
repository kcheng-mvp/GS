#!/usr/bin/env groovy


def home= new File(System.getProperty("user.home"));

if(! args || args.size() != 3){
	println "please run this command with folder sourceStr, replaceStr"
	return -1;
}



def folder = new File(args[0])
def sourceStr = args[1]
def replaceStr = args[2]



folder.eachFileRecurse{
	if(!it.isDirectory()){
		def text = it.text
    	it.write(text.replaceAll(sourceStr, replaceStr));
	} 
}
