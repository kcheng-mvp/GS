#! /usr/bin/env groovy


if(args.size() !=1){
    println "FS.groovy folder"
    return -1;
}
File root = new File(args[0]);
if(!root.exists()){
    println "The file ${args[0]} does not exits";
    return  -1;
}
searchInputFile(root);
def searchInputFile(inputFile){
    if (inputFile.isDirectory()){
        inputFile.eachFileRecurse{
            if(it.isDirectory()){
                println "--> "+ it.getPath();
            }
        }
    }
}