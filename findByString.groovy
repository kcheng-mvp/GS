#! /usr/bin/env groovy
import groovy.io.FileType

def folder = new File(args[0])
def find = args[1]

new File(args[2]).withWriter {w ->
    def bw = new BufferedWriter(w)
    folder.eachFileRecurse(FileType.FILES) { f ->
        println "Process file ${f.name} ..."
        f.eachLine {line ->
            if(line.indexOf(find) > -1){
                bw.write(line)
                bw.newLine()
            }
        }
    }
    bw.close()
}
