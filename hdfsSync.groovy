#! /usr/bin/env groovy
import groovy.time.TimeCategory
import java.nio.file.Files

def currentPath = new File(getClass().protectionDomain.codeSource.location.path).parent
GroovyShell groovyShell = new GroovyShell()
def cron4j = groovyShell.parse(new File(currentPath, "core/Cron4J.groovy"))
def shell = groovyShell.parse(new File(currentPath, "core/Shell.groovy"))
def logback = groovyShell.parse(new File(currentPath, "core/Logback.groovy"))



def hostName = InetAddress.getLocalHost().getHostName()
// hadoop version get hadoop version
def properties = new Properties()
properties.load(getClass().getResourceAsStream('hdfsSync.properties'));
def log = logback.getLogger("hdfsSync", properties.get("logPath"))

def dataSync = { localPath, hdfsRoot ->
    def backup = new File(localPath, "backup")
    if (!backup.exists()) backup.mkdirs()
    def now = Calendar.getInstance().getTime();
    use(TimeCategory) {
        localPath.eachFile { f ->
//            if(f.name.indexOf("advagg") > -1) return 0
            def isAgg = f.name.indexOf("advagg") > -1
            (1..10).find {
                def format = (now - it.hours).format("yyyy-MM-dd-HH")
                def datePath = (now - it.hours).format("yyyy/MM/dd/HH")
                if (isAgg) datePath = (now - it.hours).format("yyyy/MM/dd");
                if (f.name =~ /.*\.$format\.log.*/) {
                    println f.name
                    def command = "sudo chown hadoop ${f.absolutePath}"
                    shell.exec(command)
                    command = "sudo chgrp hadoop ${f.absolutePath}"
                    shell.exec(command)
                    command = "ls -l ${f.absolutePath}"
                    def rt = shell.exec(command)
                    if (rt["code"] != 0) {
                        log.warn("Failed to change file owner/group ${f.absolutePath}")
                        return true
                    } else {
                        rt = rt["msg"][0].split().findAll { p -> "hadoop".equals(p) }
                        if (rt.size() < 2) {
                            log.warn("Failed to change file owner/group ${f.absolutePath}")
                            return true
                        } else {
                            log.info("Change file owner/group ${f.absolutePath} success ......")
                        }
                    }

                    def category = f.name.split("\\.")[0]
                    //todo: check the folder exists or not
                    command = "hadoop fs -test -e ${hdfsRoot}/${category}/${datePath}/input"
                    rt = shell.exec(command)
                    if (rt["code"] != 0) {
                        command = "hadoop fs -mkdir ${hdfsRoot}/${category}/${datePath}/input"
                        rt = shell.exec(command)
                        if (rt["code"] == 0) {
                            log.info("Create folder ${hdfsRoot}/${category}/${datePath}/input success ...")
                        } else {
                            rt["msg"].each {
                                log.warn("Failed to create folder {}", "${hdfsRoot}/${category}/${datePath}/input")
                            }
                            return true
                        }
                    }
                    //todo: put the file the hdfs
                    command = "hadoop fs -put ${f.absolutePath} ${hdfsRoot}/${category}/${datePath}/input/${f.name}"
                    rt = shell.exec(command)
                    if (rt["code"] != 0) {
                        rt["msg"].each {
                            log.warn("Failed to put file {}", "${f.absolutePath}")
                        }
                        return true
                    } else {
                        log.info("Put file ${f.absolutePath} successfully ......")
                    }

                    //todo: mark it done
                    command = "hadoop fs -touchz ${hdfsRoot}/${category}/${datePath}/done.${hostName}"
                    rt = shell.exec(command)
                    if (rt["code"] != 0) {
                        rt["msg"].each {
                            log.warn("Failed to touch the file: ${hdfsRoot}/${category}/${datePath}/done.${hostName}")
                        }
                        return true
                    }
                    //todo: backup files
                    if (f.renameTo(new File(backup, f.name))) {
                        log.info("Backup file ${f.absolutePath} successfully ......")
                    } else {
                        log.warn("Failed to backup the file ${f.absolutePath}")
                    }
                }
            }
        }
        // clean up backup
        backup.eachFile { bf ->
            (73..96).each {hg ->
                def format = (now - hg.hours).format("yyyy-MM-dd-HH")
                if (bf.name =~ /.*\.$format\.log.*/) {
                    log.info("Delete backup file ${bf.absolutePath} ......");
                    bf.delete();
                }
            }
        }
    }

} as Runnable

def cron = "08 * * * *"

def localPaths = properties.get("localPath").split(",")
def hdfsRoots = properties.get("hdfsRoot").split(",")
cron4j.start(cron, {
    localPaths.eachWithIndex { path, idx ->
        log.info("Syncing ${path}  -> ${hdfsRoots[idx]}...")
        dataSync(new File(path), hdfsRoots[idx])
    }
})


