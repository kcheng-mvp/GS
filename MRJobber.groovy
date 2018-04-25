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

    logger.info("** Start DAU ${today.format('yyyy/MM/dd')}")
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

    logger.info("** Start WAU ${today.format('yyyy/MM/dd')}")
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
            logger.error("**  wau Can't not find the path : ${command}")
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

    logger.info("** Start MAU ${today.format('yyyy/MM/dd')}")
    //mau
    cal = Calendar.getInstance();
    cal.setTime(today);

    def previous = cal
    def validDays = [] as List
    while(previous.get(Calendar.MONTH).equals(cal.get(Calendar.MONTH))){
        command = "hadoop fs -test -d /atmm/login/${previous.format('yyyy/MM/dd')}"
        logger.info("Testing file exists : ${command}")
        rs = shell.exec(command)
        if(!rs.code){
//            input.add("/atmm/login/${it.format('yyyy/MM/dd')}/*/input")
            validDays.add(previous.format("dd"))
        } else {
            logger.error("**  mau Can't not find the path : ${command}")
            rs.msg.each{
                logger.error(it)
            }
        }
        previous = previous.previous();
    }

    input = new StringBuffer("/atmm/login/").append(today.format("yyyy/MM/"))
    logger.info("Valid days : {}", validDays.join(","))
    input.append("{").append(validDays.join(",")).append("}/*/input")
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



    def today = it ? (Date.parse("yyyy/MM/dd", it)) : use(TimeCategory) {
        Calendar.getInstance().getTime() - 1.days;
    }

    logger.info("** retain base on login day : ${today.format("yyyy/MM/dd")}")

    def login = "/atmm/login/${today.format("yyyy/MM/dd")}/*/input"
    logger.info("login data :{}", login)

    use(TimeCategory) {
        [1, 2, 3, 4, 5, 6, 7, 15, 30].each { d ->
            def registerDay = today - d.days
            def register = "/atmm/register/${registerDay.format("yyyy/MM/dd")}"
            logger.info("Retain[${d}]:${registerDay.format('yyyy/MM/dd')}-${today.format('yyyy/MM/dd')}" )
            def command = "hadoop fs -test -d ${register}"
            def rs = shell.exec(command);
            if(!rs.code){
                def output = "/atmm/retain/${registerDay.format("yyyy/MM/dd")}/${d}"
                def input = login+ "," + register+"/*/input"
                logger.info("Retain[${d}] -> input dir is ${input}")
                logger.info("Retain[${d}] -> output dir is ${output}")
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
//dau()
//retain()

cron4j.start("10 03 * * *", crmr)
cron4j.start("20 03 * * *", ccmr)
cron4j.start("30 03 * * *", dau)
cron4j.start("50 03 * * *", retain)



