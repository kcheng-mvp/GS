#! /usr/bin/env groovy

if(args || args.length !=1){
    println "Create go project with command : gogo.groovy {projectName}"
    return -1
}

def gopath = new File(args[0]);
if(gopath.exists()){
    println "${args[0]} already exists ... "
    return -1
}

gopath.mkdir();
