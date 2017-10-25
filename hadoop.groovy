#!/usr/bin/env groovy


def home = new File(System.getProperty("user.home"));

GroovyShell groovyShell = new GroovyShell()
def scriptDir = new File(getClass().protectionDomain.codeSource.location.path).parent
def shell = groovyShell.parse(new File(scriptDir, "core/Shell.groovy"))




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

// http://hadoop.apache.org/docs/



