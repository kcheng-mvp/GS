#! /usr/bin/env groovy
import groovy.time.TimeCategory
import java.nio.file.Files

def currentPath = new File(getClass().protectionDomain.codeSource.location.path).parent
GroovyShell groovyShell = new GroovyShell()
def cron4j = groovyShell.parse(new File(currentPath, "core/Cron4J.groovy"))
def shell = groovyShell.parse(new File(currentPath, "core/Shell.groovy"))



def hostName = InetAddress.getLocalHost().getHostName()
// hadoop version get hadoop version
def properties = new Properties()
properties.load(getClass().getResourceAsStream('hdfsSync.properties'));
def localPath = new File(properties.get("localPath"))
def hdfsRoot = properties.get("hdfsRoot")
def backup = new File(localPath,"backup")
if(!backup.exists()) backup.mkdirs()
def dataSync = {
    def now = Calendar.getInstance().getTime();
    use(TimeCategory) {
        localPath.eachFile { f ->
            if(f.name.indexOf("advagg") > -1) return 0
            (1..5).find {
                def format = (now - it.hours).format("yyyy-MM-dd-HH")
                def datePath = (now - it.hours).format("yyyy/MM/dd/HH")
                if (f.name =~ /.*\.$format\.log$/) {

                    def command = "sudo chown hadoop ${f.absolutePath}"
                    shell.exec(command)
                    command = "sudo chgrp hadoop ${f.absolutePath}"
                    shell.exec(command)
                    command = "ls -l ${f.absolutePath}"
                    def rt = shell.exec(command)
                    if (rt["code"] != 0) {
                        return true
                    } else {
                        rt = rt["msg"][0].split().findAll { p -> "hadoop".equals(p) }
                        println rt
                        if (rt.size() < 2) return true
                    }

                    def category = f.name.split("\\.")[0]
                    //todo: check the folder exists or not
                    command = "hadoop fs -test -e ${hdfsRoot}/${category}/${datePath}/input"
                    rt = shell.exec(command)
                    if (rt["code"] != 0) {
                        command = "hadoop fs -mkdir ${hdfsRoot}/${category}/${datePath}/input"
                        rt = shell.exec(command)
                        if (rt["code"] == 0) {
                            println "Create folder ${hdfsRoot}/${category}/${datePath}/input success ..."
                        } else {
                            rt["msg"].each {
                                println it;
                            }
                            return true
                        }
                    }
                    //todo: put the file the hdfs
                    command = "hadoop fs -put ${f.absolutePath} ${hdfsRoot}/${category}/${datePath}/input/${f.name}.${hostName}"
                    rt = shell.exec(command)
                    if (rt["code"] != 0) {
                        rt["msg"].each {
                            println it;
                        }
                        return true
                    } else {
                        println "Put file ${f.absolutePath} successfully ......"
                    }

                    //todo: mark it done
                    command = "hadoop fs -touchz ${hdfsRoot}/${category}/${datePath}/done.${hostName}"
                    rt = shell.exec(command)
                    if (rt["code"] != 0) {
                        rt["msg"].each {
                            println it
                        }
                        return true
                    }
                    //todo: backup files
                    if (f.renameTo(new File(backup, f.name))) println "Backup file ${f.absolutePath} successfully ......"



               }
            }

        }
    }

} as Runnable

//def cron = "30 * * * *"
def cron = "* * * * *"

//cron4j.start(cron, sayHello)


dataSync()
