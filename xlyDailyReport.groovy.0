#! /usr/bin/env groovy
import groovy.text.SimpleTemplateEngine
import groovy.time.TimeCategory
@Grab(group = 'org.codehaus.groovy.modules.http-builder', module = 'http-builder', version = '0.5.0')
import groovyx.net.http.RESTClient
@Grab(group = 'org.codehaus.groovy.modules.http-builder', module = 'http-builder', version = '0.5.0')
import groovyx.net.http.RESTClient

import java.text.SimpleDateFormat

import static groovyx.net.http.ContentType.JSON
import groovy.json.JsonSlurper

def url = "https://oapi.dingtalk.com"
def context = "/robot/send"
def access_token = "c711dbfef25e0ac8e909e0f5185a9b21e18d00d840de050a231d21e2a623d610"

def currentPath = new File(getClass().protectionDomain.codeSource.location.path).parent
GroovyShell groovyShell = new GroovyShell()
def cron4j = groovyShell.parse(new File(currentPath, "core/Cron4J.groovy"))
def db = groovyShell.parse(new File(currentPath, "core/Db.groovy"))
def shell = groovyShell.parse(new File(currentPath, "core/Shell.groovy"))
def xls = groovyShell.parse(new File(currentPath, "core/Xls.groovy"))
def mailMan = groovyShell.parse(new File(currentPath, "core/Mailman.groovy"))
def logback = groovyShell.parse(new File(currentPath, "core/Logback.groovy"))
def configFile = new File(currentPath, 'xlyDailyReportCfg.groovy')
//def config = new ConfigSlurper().parse(configFile.text).get("config").flatten()
def config = new ConfigSlurper().parse(configFile.text)

def localPath = config.settings.localPath
def hdfsRoot = config.settings.hdfsPath
def logPath = config.settings.logPath
def cron = config.settings.cron
def logger = logback.getLogger("xlyDailyReport", logPath);

def sql = db.h2mCon("xlyDailyReport")


def gameName = {
    def name = config.settings.games.get(it);
    return name ? (name.padRight(10 - name.length() * 2)) : it
}


def rappid = { id ->
    config.settings.appids.get(id) ?: id
}


sql.execute('''
	CREATE TABLE IF NOT EXISTS CRMR (
	ID INTEGER IDENTITY,
	DAY_STR VARCHAR(8),
	APP_ID VARCHAR(20),
	UID VARCHAR(20),
	PLATFORM VARCHAR(20),
	ADV_APP_ID VARCHAR(20),
	CLICK_TIME_LONG BIGINT,
	REGISTER_TIME_LONG BIGINT
)
''');
sql.execute('''
CREATE INDEX IF NOT EXISTS INFO_IDX1 ON CRMR(DAY_STR);
CREATE INDEX IF NOT EXISTS INFO_IDX2 ON CRMR(APP_ID);
CREATE INDEX IF NOT EXISTS INFO_IDX2 ON CRMR(ADV_APP_ID);
''')
sql.execute('''
	CREATE TABLE IF NOT EXISTS REGISTER (
	ID INTEGER IDENTITY,
	DAY_STR VARCHAR(8),
	APP_ID VARCHAR(20),
	UID VARCHAR(60),
	PLATFORM VARCHAR(20)
)
''');
sql.execute('''
CREATE INDEX IF NOT EXISTS INFO_IDX1 ON REGISTER(DAY_STR);
CREATE INDEX IF NOT EXISTS INFO_IDX2 ON REGISTER(APP_ID);
CREATE INDEX IF NOT EXISTS INFO_IDX2 ON REGISTER(PLATFORM);
''')



def sdf = new SimpleDateFormat("yyyyMMdd");
def pathFormat = new SimpleDateFormat("yyyy/MM/dd")
def timeFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
def monthFormat = new SimpleDateFormat("yyyy/MM")

/*
def first = Calendar.getInstance()
first.set(Calendar.DAY_OF_MONTH, 1)
first = first.getTime()

def validDate = Calendar.getInstance().getTime();
use(TimeCategory) {
    validDate = validDate - 1.days
}
def monthFolder = new File("${localPath}/${monthFormat.format(validDate)}");
if (monthFolder.exists()) monthFolder.deleteDir();
*/

def dateRange = {
    def previous = Calendar.getInstance().previous()
    def validDate = previous.getTime();
    previous.set(Calendar.DAY_OF_MONTH, 1)
    def first = previous.getTime()

    logger.info("Valid Date is : ${validDate}")
    logger.info("First Date is : ${first}")
    ['first': first, 'validDate': validDate]
}

def fetchDataFile = {

    def dates = dateRange();
    def validDate = dates['validDate']
    def first = dates['first']


    def rootPath = new File(localPath);
    if (rootPath.exists()) rootPath.deleteDir();
    // get all register users
    def hdfsPathRegister = new File(rootPath, "register")
    hdfsPathRegister.mkdirs()
    def command = "hadoop fs -get ${config.settings.hdfsPathRegister}/${monthFormat.format(first)}/*/*/input/*  ${hdfsPathRegister.absolutePath}"
    def rt = shell.exec(command)

    use(TimeCategory) {
        while (first <= validDate) {
            logger.info("Get Data for ${first} ...")
            command = "hadoop fs -test -e ${hdfsRoot}/${pathFormat.format(first)}/_SUCCESS"
            rt = shell.exec(command)
            if (rt["code"] == 0) {
                def f = new File("${localPath}/${pathFormat.format(first)}");
                f.mkdirs()
                command = "hadoop fs -get ${hdfsRoot}/${pathFormat.format(first)}/p*  ${localPath}/${pathFormat.format(first)}"
                rt = shell.exec(command)
                if (rt["code"] != 0) {
                    logger.warn("can not download file from ${hdfsRoot}/${pathFormat.format(first)}/p* ")
                }
            } else {
                logger.error("File ${hdfsRoot}/${pathFormat.format(first)} is not ready,code is:${rt['code']}")
                rt['msg'].each {
                    logger.error(it)
                }
            }
            first = first + 1.days
        }
    }
}

def loadData = {
    logger.info("load crmr data......")

    def targetFolder = new File(localPath);
    def insert = '''
INSERT INTO CRMR(DAY_STR,APP_ID,UID,PLATFORM,ADV_APP_ID,
CLICK_TIME_LONG, REGISTER_TIME_LONG) VALUES (?,?,?,?,?,?,?)'''
    sql.withTransaction {
        sql.withBatch(100, insert) { stmt ->
            targetFolder.eachFileRecurse { f ->
                if (f.name.indexOf("part-") > -1 && f.isFile()) {
                    logger.info("Process file ${f.absolutePath}")
                    f.eachLine { line ->
                        def segs = line.split("\t");
                        stmt.addBatch(
                                sdf.format(Long.parseLong(segs[4]) * 1000L),
                                segs[0], segs[1], segs[2], segs[3],
                                Long.parseLong(segs[4]),
                                Long.parseLong(segs[6])
                        );
                    }
                }
            }
        }
    }
    insert = '''INSERT INTO REGISTER(DAY_STR,APP_ID,UID,PLATFORM) VALUES (?,?,?,?)'''
    logger.info("load register data......")
    def jsonSlurper = new JsonSlurper()
    sql.withTransaction {
        sql.withBatch(100, insert) { stmt ->
            targetFolder.eachFileRecurse { f ->
                if (f.name.startsWith("userregister") && f.name.indexOf(".log") > -1 && f.isFile()) {
                    f.eachLine { line ->
                        //{"appid":"2016052401435705","uid":"2088122723655801","platform":"ios","time":1520322692}
                        //{"appid":"2017112900243210","platform":"android","uid":"2088822930020651","time":1520265759}
                        def jsonObj = jsonSlurper.parseText(line)
                        if (jsonObj.appid && jsonObj.platform && jsonObj.uid && jsonObj.time) {
                            stmt.addBatch(
                                    sdf.format(jsonObj.time * 1000L), rappid(jsonObj.appid), jsonObj.uid, jsonObj.platform
                            )
                        }
                    }
                }
            }
        }
    }
}

def template = '''
{
     "msgtype": "markdown",
     "markdown": {
         "title":"导量日报表 (${lastDay})",
         "text": "${detail}\n",
     },
    "at": {
        "isAtAll": true
    }
 }
''';

def genReport = {
    logger.info("Generate Report ......")
    def summarySql = '''
SELECT APP_ID,DAY_STR,COUNT(UID) AS CNT
FROM CRMR 
GROUP BY APP_ID,DAY_STR
ORDER BY APP_ID ASC,DAY_STR ASC
'''

    def registerSql = '''
SELECT COUNT(DISTINCT UID) AS RCNT
FROM REGISTER
WHERE DAY_STR = ? AND APP_ID = ?
'''
    def total = 0;
    def last = 0;
    def lastDay = null;
    def previous = null;
    def validDate = Calendar.getInstance().previous().getTime();
    def textDetail = new StringBuffer("### **导量：(${pathFormat.format(validDate)})**\n");
    sql.eachRow(summarySql) { row ->
        if (previous && !row['APP_ID'].equals(previous)) {
            def rr = sql.firstRow(registerSql, [lastDay, previous])
            textDetail.append("- ${gameName(previous)}: ${total.toString().padRight(5)}/${last.toString()}/${rr.RCNT}\n");
            total = 0;
        }
        previous = row['APP_ID']
        last = row['CNT']
        lastDay = row['DAY_STR']
        total = total + last
        logger.info("[${gameName(previous)},${lastDay},${last},${total}]")
    }

    def rr = sql.firstRow(registerSql, [lastDay, previous])

    textDetail.append("- ${gameName(previous)}: ${total.toString().padRight(5)}/${last.toString()}/${rr.RCNT}\n");
    logger.info("MSG -> ${textDetail.toString()}");
    if (textDetail.toString().length() > 0) {
        def client = new RESTClient(url)
        def simple = new SimpleTemplateEngine()
        def binding = [detail: textDetail.toString(), lastDay: lastDay]
        def msg = simple.createTemplate(template).make(binding).toString()
        def response = client.post(path: context, contentType: JSON, body: msg, query: [access_token: access_token], headers: [Accept: 'application/json'])
    }




    def detailSql = '''
SELECT *
FROM CRMR
'''

    def csv = File.createTempFile("ADV_SUMMARY", ".CSV")
    csv.deleteOnExit()
    csv.withWriter { w ->
        def bw = new BufferedWriter(w)
        sql.eachRow(detailSql) { row ->
            bw << "${row['DAY_STR']}, '${row['APP_ID']}', '${row['UID']}', ${row['PLATFORM']}, '${row['ADV_APP_ID']}', ${timeFormat.format(row['CLICK_TIME_LONG'] * 1000L)}, ${timeFormat.format(row['REGISTER_TIME_LONG'] * 1000L)}"
            bw.newLine()
        }
        bw.close()
    }

    //
    detailSql = '''
SELECT DAY_STR,ADV_APP_ID, APP_ID , PLATFORM , COUNT(UID) AS CNT
FROM CRMR
GROUP BY DAY_STR,ADV_APP_ID, APP_ID, PLATFORM
ORDER BY DAY_STR,ADV_APP_ID ASC, APP_ID ASC, PLATFORM ASC
'''
    def csv1 = File.createTempFile("ADV_DETAIL", ".CSV")
    csv1.deleteOnExit()
    csv1.withWriter { w ->
        def bw = new BufferedWriter(w)
        sql.eachRow(detailSql) { row ->


            bw << "${row['DAY_STR']}, ${gameName(row['ADV_APP_ID'])}, ${gameName(row['APP_ID'])}, ${row['PLATFORM']}, ${row['CNT']}"
            bw.newLine()
        }
        bw.close()
    }


    def registerSqlDetail = '''
SELECT COUNT(DISTINCT UID) AS RCNT,PLATFORM,APP_ID,DAY_STR
FROM REGISTER
WHERE DAY_STR <=  ?
GROUP BY PLATFORM,APP_ID,DAY_STR
ORDER BY DAY_STR ASC, APP_ID ASC,PLATFORM ASC
'''
    def csv2 = File.createTempFile("REGISTER_DETAIL", ".CSV")
    csv2.deleteOnExit()
    csv2.withWriter { w ->
        def bw = new BufferedWriter(w)
        sql.eachRow(registerSqlDetail,[sdf.format(validDate)]) { row ->
            bw << "${row['DAY_STR']}, ${gameName(row['APP_ID'])}, ${row['PLATFORM']}, ${row['RCNT']}"
            bw.newLine()
        }
        bw.close()
    }




    mailMan.sendMail("导量日报（${lastDay}）", "导量日报（${lastDay}）", configFile, [csv.absolutePath, csv1.absolutePath,csv2.absolutePath])


}

def cleanup = {
    logger.info("clean up database ...... ")
    def delete = "DELETE FROM CRMR";
    sql.execute(delete);
    def check = "SELECT COUNT(1) as CNT FROM CRMR"
    def rt = sql.firstRow(check)
    logger.info("[cleanup]: There are ${rt.CNT} rows in db.")
}

cron4j.start(cron, {
    cleanup()
    fetchDataFile()
    loadData()
    genReport()
    cleanup()
})


