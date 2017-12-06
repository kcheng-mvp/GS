#! /usr/bin/env groovy
import java.text.SimpleDateFormat
def currentPath = new File(getClass().protectionDomain.codeSource.location.path).parent
GroovyShell groovyShell = new GroovyShell()
def shell = groovyShell.parse(new File(currentPath, "core/Shell.groovy"))
def cron4j = groovyShell.parse(new File(currentPath, "core/Cron4J.groovy"))
def logback = groovyShell.parse(new File(currentPath, "core/Logback.groovy"))
def jarHome = "/home/hadoop/tools/MRJars"

def logger = logback.getLogger("MRJob")



def sdf = new SimpleDateFormat("yyyy/MM/dd")
crmr = { it ->
    def today = it ?:  sdf.format(Calendar.getInstance().getTime())
    println today
    return
    def command = "hadoop jar ${jarHome}/CRMR.jar ${today}"
    def rs = shell.exec(command);
    rs["msg"].each {
        logger.info(it);
    }
}
ccmr = { it ->
    def today = it ?:  sdf.format(Calendar.getInstance().getTime())
    println today
    return
    def command = "hadoop jar ${jarHome}/CCMR.jar ${today}"
    def rs = shell.exec(command);
    rs["msg"].each {
        logger.info(it);
    }
}

crmr()
//cron4j.start("30 11 * * *", crmr)
//cron4j.start("40 11 * * *", ccmr)
