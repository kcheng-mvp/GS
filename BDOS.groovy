#!/usr/bin/env groovy

//import groovy.transform.BaseScript
//import core.Shell
//@BaseScript Shell shell

def home = new File(System.getProperty("user.home"));

def groovyShell = new GroovyShell()
def scriptDir = new File(getClass().protectionDomain.codeSource.location.path).parent
def shell = groovyShell.parse(new File(scriptDir, "core/Shell.groovy"))

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
def etcHosts =  {hosts ->
    hosts.each { host ->
        def settings = shell.exec("grep `hostname` /etc/hosts", host.trim())

        def list = settings.collect {
            if (it.indexOf("#") < 0) {
                it.split("\\s+")[1]
            }
        }
        if (!hosts.equals(list)) {
            println "${host}'s /etc/hosts does not contains all the hosts: ${hosts}"
        }
    }
}





def parted = { host, part = null, device = null ->

    def lsblk = shell.exec("sudo lsblk", host)

    def fstab = shell.exec("sudo cat /etc/fstab", host)


    def partedInfo = shell.exec("sudo parted -l -s", host)
    def end = 0
    def msg = new StringBuffer();
    partedInfo.each {
        if(it.indexOf("Error: Invalid argument during") > -1) {
            device = it.split(" ").last();
            println "**** Device ${device} need to be parted ****";
        }
        msg.append(it).append(System.getProperty("line.separator"));
    }
    if(device) {
        println msg
        shell.exec("sudo fdisk -l", host).each {
            end = it.split(",");
            if(end.size() == 3 && end[0].indexOf(device) > -1){
               end = end[0].split(":").last().trim()
            }
        }
        println "Device size is :${end}"

    };

    /*
    mklabel,mktable LABEL-TYPE               create a new disklabel (partition
            table)
    mkpart PART-TYPE [FS-TYPE] START END     make a partition
*/
    //Disk /dev/vdb: 1919.9 GB, 1919850381312 bytes, 3749707776 sectors
    if(part && !device){
        shell.exec("sudo parted ${device} mklabel Gpt", host)
        shell.exec("sudo parted ${device} mkpart ex2 0 ${end}", host)
    }
}

//etcHosts(["dev1","dev2"])
//parted("oltp001")

if(!args){
    println "Please input parameter"
    println '''

    Supported commands : etcHosts(hosts), parted(host)
'''
    return -1
} else {
    parted("oltp001", "p", "/dev/vdb")
}






