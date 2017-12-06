#! /usr/bin/env groovy
import java.sql.Time
import java.text.SimpleDateFormat
import groovy.time.TimeCategory
def currentPath = new File(getClass().protectionDomain.codeSource.location.path).parent
GroovyShell groovyShell = new GroovyShell()
def shell = groovyShell.parse(new File(currentPath, "core/Shell.groovy"))
def cron4j = groovyShell.parse(new File(currentPath, "core/Cron4J.groovy"))
def logback = groovyShell.parse(new File(currentPath, "core/Logback.groovy"))
def config = new File(currentPath, 'MRJobberCfg.groovy')

config = new ConfigSlurper().parse(config.text).flatten();

def logger = logback.getLogger("MRJob",config.get("cfg.logPath"))


def sdf = new SimpleDateFormat("yyyy/MM/dd")
crmr = { it ->
    def today = it ?:  use(TimeCategory){
        sdf.format(Calendar.getInstance().getTime() - 1.days)
    }
    println today
    return
    def command = "hadoop jar ${config.get('cfg.jarHome')}/CRMR.jar ${today}"
    def rs = shell.exec(command);
    rs["msg"].each {
        logger.info(it);
    }
}
ccmr = { it ->
    def today = it ?: use(TimeCategory) {
        sdf.format(Calendar.getInstance().getTime())
    }
    println today
    return
    def command = "hadoop jar ${config.get('cfg.jarHome')}/CCMR.jar ${today}"
    def rs = shell.exec(command);
    rs["msg"].each {
        logger.info(it);
    }
}

crmr()
//cron4j.start("30 11 * * *", crmr)
//cron4j.start("40 11 * * *", ccmr)
