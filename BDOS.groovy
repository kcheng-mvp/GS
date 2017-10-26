#!/usr/bin/env groovy

import groovy.transform.BaseScript
import core.Shell
@BaseScript Shell shell

/*
def home = new File(System.getProperty("user.home"));

def groovyShell = new GroovyShell()
def scriptDir = new File(getClass().protectionDomain.codeSource.location.path).parent
def shell = groovyShell.parse(new File(scriptDir, "core/Shell.groovy"))
*/

/*
if (!args) {
    println "Please specified the manifist"
    return
}

def manifist = new File(args[0]);
if (!manifist.exists()) {
    println "Can not find the file ${args[0]}"
    return -1
}
def props = new Properties()
manifist.withInputStream {
    stream -> props.load(stream)
}

if (props.get("hosts")) {
    println "*******************001 : /etc/hosts *******************"
    props.get("hosts").split(",").each {host ->
        //ssh dev1 'grep `hostname` /etc/hosts'
        def hosts = shell.exec("grep `hostname` /etc/hosts", host.trim())
        println "${host} :"
        hosts.each {println it}
    }
}
*/

def etcHosts(hosts) {
    hosts.each { host ->
        def settings = exec("grep `hostname` /etc/hosts", host.trim())

        def list = settings.collect {
            if(it.indexOf("#") < 0){
                it.split("\\s+")[1]
            }
        }
        if(! hosts.equals(list)){
            println "${host}'s /etc/hosts does not contains all the hosts: ${hosts}"
        }
    }

}


etcHosts(["dev1","dev2"])
// http://hadoop.apache.org/docs/



