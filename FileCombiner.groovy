#! /usr/bin/env groovy

if (!args || args.size() != 3) {
    println "Please input parameters"
    return -1
}


def folder = new File(args[0]);
if (!folder.isDirectory()) {
    println "${args[0]} is not a folder"
    return -1
}

def targetFile = new File(folder, args[2])
targetFile.withWriter('UTF-8') { bw ->
    folder.eachFileRecurse { f ->
        if (f.isFile() && f.name.indexOf(args[1]) > -1) {
            f.eachLine { line ->
                println line
                bw.writeLine line
            }
        }
    }
}
