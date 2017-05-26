#!/usr/bin/env groovy

import groovy.json.JsonOutput;
if (!args || args.size() != 1) {
    println "please input your folder"
    return -1;
}
def file = new File(args[0]);

def set = new HashSet<String>();
file.eachLine { line ->
    set.add(line.toLowerCase().trim());
//    set2.add(line.toLowerCase());
}


//
def json = JsonOutput.toJson(set);
println json;