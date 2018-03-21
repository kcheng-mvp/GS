#! /usr/bin/env groovy
import groovy.time.TimeCategory
import java.nio.file.Files

def currentPath = new File(getClass().protectionDomain.codeSource.location.path).parent
GroovyShell groovyShell = new GroovyShell()
def cron4j = groovyShell.parse(new File(currentPath, "../core/Cron4J.groovy"))
def shell = groovyShell.parse(new File(currentPath, "../core/Shell.groovy"))
def logback = groovyShell.parse(new File(currentPath, "../core/Logback.groovy"))



def logger = logback.getLogger("hdfsSync", config.settings.logDir)
def hostName = InetAddress.getLocalHost().getHostName()
// hadoop version get hadoop version

def configFile = new File("hdfsSyncCfg.groovy")
if (!configFile.exists()) {
    logger.error "** Can't find file hdfsSyncCfg.groovy in current folder ......"
    return -1
}
def config = new ConfigSlurper().parse(configFile.text)

def dataDir = config.settings.dataDir
def hdfsRoot =config.settings.hdfsRoot
def backup = new File(dataDir,"backup")
if(!backup.exists()) backup.mkdirs()

def dataSync = {
    def now = Calendar.getInstance().getTime();
    use(TimeCategory) {
        dataDir.eachFile { f ->
            (1..5).find {
                def namePattern = (now - it.hours).format(config.settings.namePattern)
                def pathPattern = (now - it.hours).format(config.settings.pathPattern)
                if (f.name =~ /.*\.$namePattern\.log$/) {


                    //todo: change file owner
                    logger.info("** Change owner for ${f.absolutePath}")
                    def command = "sudo chown ${config.settings.hdfsPermission} ${f.absolutePath}"
                    def rt = shell.exec(command)
                    if (rt.code) {
                        log.warn("** Failed to change owner for ${f.absolutePath}")
                        return true
                    }

                    //todo: get file category from file name
                    def category = f.name.split("\\.")[0]

                    //todo: check the target folder exits on hdfs or not
                    logger.info("** Check ${hdfsRoot}/${category}/${pathPattern}/input exits or not")
                    command = "hadoop fs -test -e ${hdfsRoot}/${category}/${pathPattern}/input"
                    rt = shell.exec(command)
                    if (rt.code) {
                        logger.info("** Create folder ${hdfsRoot}/${category}/${pathPattern}/input")
                        command = "hadoop fs -mkdir ${hdfsRoot}/${category}/${pathPattern}/input"
                        rt = shell.exec(command)
                        if (!rt.code) {
                            logger.info("** Create folder ${hdfsRoot}/${category}/${pathPattern}/input success ...")
                        } else {
                            rt.msg.each {
                                logger.error("** Failed to create folder {}", "${hdfsRoot}/${category}/${pathPattern}/input")
                            }
                            return true
                        }
                    }
                    //todo: put the file the hdfs
                    command = "hadoop fs -put ${f.absolutePath} ${hdfsRoot}/${category}/${pathPattern}/input/${f.name}.${hostName}"
                    rt = shell.exec(command)
                    if (rt.code) {
                        rt.msg.each {
                            logger.error("** Failed to put file {}", "${f.absolutePath}")
                        }
                        return true
                    } else {
                        logger.info("** Put file ${f.absolutePath} successfully ......")
                    }

                    //todo: mark it done
                    command = "hadoop fs -touchz ${hdfsRoot}/${category}/${pathPattern}/done.${hostName}"
                    rt = shell.exec(command)
                    if (rt.code) {
                        rt.msg.each {
                            logger.error("** Failed to touch the file: ${hdfsRoot}/${category}/${datePath}/done.${hostName}")
                        }
                        return true
                    }
                    //todo: backup files
                    if (f.renameTo(new File(backup, f.name))) {
                        logger.info("** Backup file ${f.absolutePath} successfully ......")
                    } else {
                        logger.error("** Failed to backup the file ${f.absolutePath}")
                    }


               }
            }
        }
        //todo Delete backup 24 hours ago
        backup.eachFile{ f ->
            (24..36).each {
                def namePattern = (now - it.hours).format(config.settings.namePattern)
                if (f.name =~ /.*\.$namePattern\.log$/) {
                    f.delete()
                }
            }
        }
    }

} as Runnable

def cron = "25 * * * *"
cron4j.start(cron, dataSync)


