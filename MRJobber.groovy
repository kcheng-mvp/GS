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
    def command = "hadoop jar ${config.get('cfg.jarHome')}/Dau.jar ${input} ${output} ATM-DAU"
    def rs = shell.exec(command);
    rs["msg"].each {
        logger.info(it);
    }

    //wau
    output = "/atmm/wau/${today.format("yyyy/MM/dd")}/w"
    input = new StringBuffer("/atmm/login")
    def cal = Calendar.getInstance();
    cal.setTime(today);
    while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
        cal.add(Calendar.DAY_OF_WEEK, -1)
    }
    Date lastMonday = cal.time

    input = new ArrayList<>();
    lastMonday.upto(today) {
        input.add("/atmm/login/${it.format('yyyy/MM/dd')}/*/input")
    }
    input = input.join(",")

    logger.info("wau input ${input}")
    logger.info("wau output ${output}")
    command = "hadoop jar ${config.get('cfg.jarHome')}/Dau.jar ${input} ${output} ATM-WAU"
    rs = shell.exec(command);
    rs["msg"].each {
        logger.info(it);
    }

    //mau
    cal = Calendar.getInstance();
    cal.setTime(today);

    input = new StringBuffer("/atmm/login/")
    input.append(cal.format("yyyy/MM")).append("/{")
            .append("01..").append(cal.get(Calendar.DAY_OF_MONTH)).append("}/*/input")
    output = "/atmm/mau/${today.format('yyyy/MM/dd')}/m"

    logger.info("mau input ${input}")
    logger.info("mau output ${output}")
    command = "hadoop jar ${config.get('cfg.jarHome')}/Dau.jar ${input} ${output} ATM-MAU"
    rs = shell.exec(command);
    rs["msg"].each {
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
            def output = "/atmm/retain/${registerDay.format("yyyy/MM/dd")}/${d}"
            def input = login +","+ register
            logger.info("Retain:${d} -> input dir is ${input}")
            logger.info("Retain:${d} -> output dir is ${output}")
            /*
            def command = "hadoop jar ${config.get('cfg.jarHome')}/Retain.jar ${input} ${output} ATM-RETAIN-${d}"
            def rs = shell.exec(command);
            rs["msg"].each {
                logger.info(it);
            }
            */
        }
    }


}
//dau()
retain()
//cron4j.start("30 11 * * *", crmr)
//cron4j.start("40 11 * * *", ccmr)


