#! /usr/bin/env groovy

import groovy.io.FileType
import groovy.time.TimeCategory
import groovy.sql.Sql
import groovy.transform.ToString;
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovy.transform.ToString


@Grab(group = 'mysql', module = 'mysql-connector-java', version = '5.1.16')
@GrabConfig(systemClassLoader = true)

def currentPath = new File(getClass().protectionDomain.codeSource.location.path).parent
GroovyShell groovyShell = new GroovyShell()
def shell = groovyShell.parse(new File(currentPath, "core/Shell.groovy"))
def cron4j = groovyShell.parse(new File(currentPath, "core/Cron4J.groovy"))
def db = groovyShell.parse(new File(currentPath, "core/Db.groovy"))
def logback = groovyShell.parse(new File(currentPath, "core/Logback.groovy"))
def config = new File(currentPath, 'atmDataLoaderCfg.groovy')
config = new ConfigSlurper().parse(config.text).flatten();
def logger = logback.getLogger("amtDataLoader", config.get("setting.logPath"))
def localBaseDir = new File(config.get("setting.localPath"))
def hdfsRoot = config.get("setting.hdfsRoot");

//def sql = db.h2mCon("atm")


def createTable = '''
	CREATE TABLE IF NOT EXISTS T_ATM (
	ID INTEGER IDENTITY,
	UID VARCHAR(20),
	CHANNEL INTEGER, 
	DATE_STR VARCHAR(12),
	COUNT INTEGER,
	CATEGORY VARCHAR(12)
)
'''
def createIndex = '''
CREATE INDEX IF NOT EXISTS INFO_IDX1 ON T_ATM(CHANNEL);
CREATE INDEX IF NOT EXISTS INFO_IDX2 ON T_ATM(CATEGORY);
'''



if (!localBaseDir.exists()) localBaseDir.mkdirs();


@ToString
class Event {
    Integer type;
    Integer channel;
    String uid;
    Long timestamp;
    Long count;
    String taskId;
}


hdfssync = { remote, localDir ->

    def type = localDir.split("/")[0]

    new File(localBaseDir, type).with {
        if (it.exists()) it.deleteDir()
        it.mkdirs();
    };

    def f = new File(localBaseDir, localDir)
    f.mkdirs();

    def command = "hadoop fs -get ${hdfsRoot}/${remote}  ${f.absolutePath}"
    logger.info("hdfssync command -> {}", command)
    def rt = shell.exec(command)
    rt.msg.each { it ->
        logger.info it
    }
    [code: rt.code, msg: f.absolutePath]
}


checkExits = { mysql, reportDay, channel ->

    def check = "SELECT 1 FROM T_USER_ACTIVITY where REPORT_DAY = ? AND CHANNEL = ?"
    def rs = mysql.rows(check, [reportDay.format("yyyy/MM/dd"), channel])
    return !rs.isEmpty()
}

dau = { it ->

    def h2 = db.h2mCon("atm")

    h2.execute(createTable)
    h2.execute(createIndex)

    def mysql = Sql.newInstance(config.get("setting.db.host"), config.get("setting.db.username"), config.get("setting.db.password"), "com.mysql.jdbc.Driver");

    logger.info("** Today is ${Calendar.getInstance().format("yyyy/MM/dd")}")
    def today = it ? (Date.parse("yyyy/MM/dd", it)) : use(TimeCategory) {
        Calendar.getInstance().getTime() - 1.days;
    }
    def remote = "dau/${today.format("yyyy/MM/dd")}/*"

    def insert = "INSERT INTO T_ATM(CHANNEL,UID,CATEGORY,COUNT) VALUES (?,?,?,?)"
    def numOfLines = 0;
    def syncStatus = hdfssync(remote, "dau/${today.format('yyyyMMdd')}")
    if (!syncStatus.code) {
        def path = new File(syncStatus.msg)
        path.eachFileRecurse(FileType.FILES, { f ->
            //@todo need to check should be processed or not and file name pattern
            if (f.name.indexOf("part-r-") > -1) {
                def category = f.getParentFile().name.toUpperCase();
                h2.withTransaction {
                    h2.withBatch(100, insert) { stmt ->
                        f.eachLine { line ->
                            numOfLines++
                            def entries = line.split("\t")
                            stmt.addBatch(entries[0], entries[1], category, entries[2]);
                        }
                    }
                }
            }

        })
    }
    logger.info("There are totally ${numOfLines} have been processed !")

    def query = "SELECT CHANNEL,CATEGORY,COUNT(1) AS DAU,SUM(COUNT) AS TOTAL_LOGIN FROM T_ATM GROUP BY CHANNEL,CATEGORY ORDER BY CHANNEL,CATEGORY"
//    def check = "SELECT 1 FROM T_USER_ACTIVITY where REPORT_DAY = ? AND CHANNEL = ?"
    def categoryColumnMap = ["D": "DAU", "W": "WAU", "M": "MAU"]
    h2.eachRow(query) { row ->

//        logger.info("today -> : {}", today)
//        logger.info("row.CHANNEL -> : {}", row.CHANNEL)
//        def rs = mysql.rows(check, [today.format("yyyy/MM/dd"), row.CHANNEL])

        if(row.CHANNEL){

            def exits = checkExits(mysql, today, row.CHANNEL)

            logger.info("${row.DAU}, ${row.CHANNEL}, ${row.CATEGORY}")
            if (exits) {
                // update
                mysql.execute("UPDATE T_USER_ACTIVITY set ${categoryColumnMap.get(row.CATEGORY)} = ? WHERE REPORT_DAY = ? and CHANNEL = ?", [row.DAU, today.format("yyyy/MM/dd"), row.CHANNEL])
            } else {
                // insert
                mysql.execute("INSERT INTO T_USER_ACTIVITY(${categoryColumnMap.get(row.CATEGORY)}, REPORT_DAY,CHANNEL) values (?,?,?)", [row.DAU, today.format("yyyy/MM/dd"), row.CHANNEL])
            }
        }
        //@todo need to insert the result to database
        //@todo update process status in hdfs
    }

    def summary = "select sum(dau) dau, sum(wau) wau, sum(mau) mau , report_day from T_USER_ACTIVITY WHERE REPORT_DAY = ? and channel != 9999 group by report_day"
    def sum = mysql.rows(summary, today.format("yyyy/MM/dd"));
    if (checkExits(mysql, today, 9999)) {
        mysql.execute("UPDATE T_USER_ACTIVITY set dau = ? , wau = ? , mau = ? where report_day = ? and channel = ?", [sum[0].dau, sum[0].wau, sum[0].mau, today.format("yyyy/MM/dd"), 9999]);
    } else {
        mysql.execute("INSERT INTO T_USER_ACTIVITY(dau, wau, mau, REPORT_DAY,CHANNEL) values (?,?,?,?,?)", [sum[0].dau, sum[0].wau, sum[0].mau, today.format("yyyy/MM/dd"), 9999])
    }
    h2.close()
    mysql.close()

}
retain = { it ->

    logger.info("** Today is ${Calendar.getInstance().format("yyyy/MM/dd")}")
    def today = it ? (Date.parse("yyyy/MM/dd", it)) : use(TimeCategory) {
        Calendar.getInstance().getTime() - 1.days;
    }

    def sameDayOfPreviousMonth = use(TimeCategory) {
        today - 1.month - 1.days;
    }

    println today.format("yyyy/MM/dd")
    println sameDayOfPreviousMonth.format("yyyy/MM/dd")

//    def remote = "/dau/${today.format("yyyy/MM/dd")}"
    sameDayOfPreviousMonth.upto(today) { day ->
        hdfssync("/retain/${day.format("yyyy/MM/dd/*")}", "retain/${day.format('yyyy/MM/dd')}")
    }
}

register = { it ->

    logger.info("** Today is ${Calendar.getInstance().format("yyyy/MM/dd")}")
    def today = it ? (Date.parse("yyyy/MM/dd", it)) : use(TimeCategory) {
        Calendar.getInstance().getTime() - 1.days;
    }

    def remote = "register/${today.format("yyyy/MM/dd")}/*/input/*"
    def syncStatus = hdfssync(remote, "register/${today.format('yyyyMMdd')}")

    def mysql = Sql.newInstance(config.get("setting.db.host"), config.get("setting.db.username"), config.get("setting.db.password"), "com.mysql.jdbc.Driver");
    def channelCounterMap = [:] as Map<Integer, Integer>
    if (!syncStatus.code) {
        def path = new File(syncStatus.msg)
        path.eachFileRecurse(FileType.FILES, { f ->
            //@todo need to check should be processed or not and file name pattern
            if (f.name.indexOf("register") > -1 && f.name.indexOf("log") > -1) {
                f.eachLine { line ->
                    def eventMap = new JsonSlurper().parseText(line)
                    String channel = eventMap.get("channel")
                    Integer cnt = channelCounterMap.get(channel) == null ? 1 : channelCounterMap.get(channel) + 1;
                    channelCounterMap.put(channel, cnt);
                }
            }
        })
    }
    logger.info("Register info {}", channelCounterMap)

    channelCounterMap.each { k, v ->
        if (k) {

            def exits = checkExits(mysql, today, k)
            if (exits) {
                // update
                mysql.execute("UPDATE T_USER_ACTIVITY set NEW_REGISTER = ? WHERE REPORT_DAY = ? and CHANNEL = ?", [v, today.format("yyyy/MM/dd"), k])
            } else {
                // insert
                mysql.execute("INSERT INTO T_USER_ACTIVITY(NEW_REGISTER, REPORT_DAY,CHANNEL) values (?,?,?)", [v, today.format("yyyy/MM/dd"), k])
            }
        }
    }
    def summary = "select sum(NEW_REGISTER) register , report_day from T_USER_ACTIVITY WHERE REPORT_DAY = ? and channel != 9999 group by report_day"
    def sum = mysql.rows(summary, today.format("yyyy/MM/dd"));
    if (checkExits(mysql, today, 9999)) {
        mysql.execute("UPDATE T_USER_ACTIVITY set NEW_REGISTER = ? where report_day = ? and channel = ?", [sum[0].register, today.format("yyyy/MM/dd"), 9999]);
    } else {
        mysql.execute("INSERT INTO T_USER_ACTIVITY(NEW_REGISTER, REPORT_DAY,CHANNEL) values (?,?,?)", [sum[0].register, today.format("yyyy/MM/dd"), 9999])
    }
    mysql.close()

}

def cleanup = {
    logger.info("Clean up database ...... ")
    def delete = "DELETE FROM T_ATM";
    sql.execute(delete);
    def check = "SELECT COUNT(1) as CNT FROM CRMR"
    def rt = sql.firstRow(check)
    logger.info("There are ${rt.CNT} rows in db.")
}

cron4j.start("10 04 * * *", dau)
cron4j.start("20 04 * * *", register)
//dau()
//register()




