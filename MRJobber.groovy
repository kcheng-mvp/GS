#! /usr/bin/env groovy
import groovy.time.TimeCategory

def currentPath = new File(getClass().protectionDomain.codeSource.location.path).parent
GroovyShell groovyShell = new GroovyShell()
def shell = groovyShell.parse(new File(currentPath, "core/Shell.groovy"))
def cron4j = groovyShell.parse(new File(currentPath, "core/Cron4J.groovy"))
def logback = groovyShell.parse(new File(currentPath, "core/Logback.groovy"))
def config = new File(currentPath, 'MRJobberCfg.groovy')

config = new ConfigSlurper().parse(config.text).flatten();

def logger = logback.getLogger("MRJob", config.get("cfg.logPath"))


crmr = { it ->
    def today = it ?: use(TimeCategory) {
        (Calendar.getInstance().getTime() - 1.days).format("yyyy/MM/dd");
    }
    def command = "hadoop jar ${config.get('cfg.jarHome')}/CRMR.jar ${today}"
    def rs = shell.exec(command);
    rs["msg"].each {
        logger.info(it);
    }
}
ccmr = { it ->
    def today = it ?: use(TimeCategory) {
        (Calendar.getInstance().getTime() - 1.days).format("yyyy/MM/dd")
    }
    def command = "hadoop jar ${config.get('cfg.jarHome')}/CCMR.jar ${today}"
    def rs = shell.exec(command);
    rs["msg"].each {
        logger.info(it);
    }
}

// 3 jobs
dau = { it ->

    logger.info("** Today is ${Calendar.getInstance().format("yyyy/MM/dd")}")
    def today = it ? (Date.parse("yyyy/MM/dd", it)) : use(TimeCategory) {
        Calendar.getInstance().getTime() - 1.days;
    }

    // dau
    def input = "/atmm/login/${today.format("yyyy/MM/dd")}/*/input"
    def output = "/atmm/dau/${today.format("yyyy/MM/dd")}/d"

    logger.info("dau input ${input}")
    logger.info("dau output ${output}")
    def command = "hadoop jar ${config.get('cfg.jarHome')}/DAU.jar ${input} ${output} ATM-DAU"
    def rs = shell.exec(command);
    rs.msg.each {
        logger.info(it);
    }

    //wau
    output = "/atmm/dau/${today.format("yyyy/MM/dd")}/w"
    input = new StringBuffer("/atmm/login")
    def cal = Calendar.getInstance();
    cal.setTime(today);
    while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
        cal.add(Calendar.DAY_OF_WEEK, -1)
    }
    Date lastMonday = cal.time

    input = new ArrayList<>();
    lastMonday.upto(today) {
        command = "hadoop fs -test -d /atmm/login/${it.format('yyyy/MM/dd')}"
        logger.info("Testing file exists : ${command}")
        rs = shell.exec(command)
        if(!rs.code){
            input.add("/atmm/login/${it.format('yyyy/MM/dd')}/*/input")
        } else {
            logger.error("** Can't not find the path : ${command}")
            rs.msg.each{
                logger.error(it)
            }
        }
    }
    input = input.join(",")

    logger.info("wau input ${input}")
    logger.info("wau output ${output}")
    command = "hadoop jar ${config.get('cfg.jarHome')}/DAU.jar ${input} ${output} ATM-WAU"
    rs = shell.exec(command);
    rs.msg.each {
        logger.info(it);
    }

    //mau
    cal = Calendar.getInstance();
    cal.setTime(today);


    input = new StringBuffer("/atmm/login/").append(cal.format("yyyy/MM/"))
//    def range = 01..cal.get(Calendar.DAY_OF_MONTH)
    def validDays = []
    01..cal.get(Calendar.DAY_OF_MONTH).each {
        def dayPath = input.toString()+it.toString().padLeft(2, '0')
        command = "hadoop fs -test -d ${dayPath}"
        rs = shell.exec(command)
        if(!rs.code){
            validDays.add(it.toString().padLeft(2, '0'))
        } else {
            rs.msg.each {
                logger.warn(it);
            }
        }
    }
    logger.info("Valid days : {}", validDays)
    input.append("{").append(validDays.join(",")).append("}/*/input")
//    input.append("/{").append("01..").append(cal.get(Calendar.DAY_OF_MONTH)).append("}/*/input")
    output = "/atmm/dau/${today.format('yyyy/MM/dd')}/m"

    logger.info("mau input ${input}")
    logger.info("mau output ${output}")
    command = "hadoop jar ${config.get('cfg.jarHome')}/DAU.jar ${input} ${output} ATM-MAU"
    rs = shell.exec(command);
    rs.msg.each {
        logger.info(it);
    }

}
// 9 jobs
retain = {
    // input path args[0] /atmm/login/{day}/*/input,/atmm/register/{day-1}/*/input
    // output path args[1] /atmm/retain/{day}
    //1,2,3,4,5,6,7,15,30

    logger.info("** Today is ${Calendar.getInstance().format("yyyy/MM/dd")}")
    def today = it ? (Date.parse("yyyy/MM/dd", it)) : use(TimeCategory) {
        Calendar.getInstance().getTime() - 1.days;
    }

    def login = "/atmm/login/${today.format("yyyy/MM/dd")}/*/input"

    use(TimeCategory) {
        logger.info("Login day is ${today.format("yyyy/MM/dd")}")
        [1, 2, 3, 4, 5, 6, 7, 15, 30].each { d ->
            def registerDay = today - d.days
            def register = "/atmm/register/${registerDay.format("yyyy/MM/dd")}/*/input"

            def command = "hadoop fs -test -e ${register}"
            def rs = shell.exec(command);
            if(!rs.code){
                def output = "/atmm/retain/${registerDay.format("yyyy/MM/dd")}/${d}"
                def input = login + "," + register
                logger.info("RETAIN:${d} -> input dir is ${input}")
                logger.info("RETAIN:${d} -> output dir is ${output}")
                command = "hadoop jar ${config.get('cfg.jarHome')}/RETAIN.jar ${input} ${output} ATM-RETAIN-${d}"
                rs = shell.exec(command);
                rs.msg.each {
                    logger.info(it);
                }
            } else {
                logger.error("**No Register data at {}", registerDay.format("yyyy/MM/dd"))
            }
        }
    }


}
dau("2018/04/23")
//retain()
//cron4j.start("30 11 * * *", crmr)
//cron4j.start("40 11 * * *", ccmr)


