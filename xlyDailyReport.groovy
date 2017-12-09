#! /usr/bin/env groovy
import groovy.text.SimpleTemplateEngine
import groovy.time.TimeCategory
@Grab(group = 'org.codehaus.groovy.modules.http-builder', module = 'http-builder', version = '0.5.0')
import groovyx.net.http.RESTClient
@Grab(group = 'org.codehaus.groovy.modules.http-builder', module = 'http-builder', version = '0.5.0')
import groovyx.net.http.RESTClient

import java.text.SimpleDateFormat

import static groovyx.net.http.ContentType.JSON

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
def logger = logback.getLogger("xlyDailyReport");

def localPath = config.settings.localPath
def hdfsRoot = config.settings.hdfsPath
def logPath = config.settings.logPath
def cron = config.settings.cron

def sql = db.h2mCon("xlyDailyReport")


def gameName = {
    def name = config.settings.games.get(it);
    return name ? (name.padRight(20 - name.length() * 2)) : it
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
	CLICK_TIME DATE,
	REGISTER_TIME_LONG BIGINT,
	REGISTER_TIME DATE
)
''');
sql.execute('''
CREATE INDEX IF NOT EXISTS INFO_IDX1 ON CRMR(DAY_STR);
CREATE INDEX IF NOT EXISTS INFO_IDX2 ON CRMR(APP_ID);
CREATE INDEX IF NOT EXISTS INFO_IDX2 ON CRMR(ADV_APP_ID);
''')



def sdf = new SimpleDateFormat("yyyyMMdd");
def pathFormat = new SimpleDateFormat("yyyy/MM/dd")
def timeFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
def monthFormat = new SimpleDateFormat("yyyy/MM")

def first = Calendar.getInstance()
first.set(Calendar.DAY_OF_MONTH, 1)
first = first.getTime()

def validDate = Calendar.getInstance().getTime();
use(TimeCategory) {
    validDate = validDate - 1.days
}
def monthFolder = new File("${localPath}/${monthFormat.format(validDate)}");
if (monthFolder.exists()) monthFolder.deleteDir();

def fetchDataFile = {
    use(TimeCategory) {
        while (first <= validDate) {
            def command = "hadoop fs -test -e ${hdfsRoot}/${pathFormat.format(first)}/_SUCCESS"
            def rt = shell.exec(command)
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
    def insert = '''
INSERT INTO CRMR(DAY_STR,APP_ID,UID,PLATFORM,ADV_APP_ID,
CLICK_TIME_LONG, CLICK_TIME,REGISTER_TIME_LONG,REGISTER_TIME) VALUES (?,?,?,?,?,?,?,?,?)'''
    sql.withTransaction {
        sql.withBatch(100, insert) { stmt ->
            monthFolder.eachFileRecurse { f ->
                if (f.name.indexOf("part-") > -1) {
                    f.eachLine { line ->
                        def segs = line.split("\t");
                        stmt.addBatch(
                                sdf.format(Long.parseLong(segs[4]) * 1000L),
                                segs[0], segs[1], segs[2], segs[3],
                                Long.parseLong(segs[4]), new Date(Long.parseLong(segs[4]) * 1000),
                                Long.parseLong(segs[6]), new Date(Long.parseLong(segs[6]) * 1000)
                        );
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
    def summarySql = '''
SELECT APP_ID,DAY_STR,COUNT(UID) AS CNT
FROM CRMR 
GROUP BY APP_ID,DAY_STR
ORDER BY APP_ID ASC,DAY_STR ASC
'''

    def total = 0;
    def last = 0;
    def lastDay = null;
    def previous = null;
    def textDetail = new StringBuffer("### **导量日报：本月累计/当日（${pathFormat.format(validDate).padLeft(20)}）** \n");
    sql.eachRow(summarySql) { row ->
        if (previous && !row['APP_ID'].equals(previous)) {
            textDetail.append("- ${gameName(row['APP_ID'])}: ${total.toString().padRight(20)}/${last.toString().padRight(20)}\n");
            total = 0;
        }
        previous = row['APP_ID']
        last = row['CNT']
        lastDay = row['DAY_STR']
        total = total + last
    }
    if (textDetail.toString().length() > 0) {
        def client = new RESTClient(url)
        def simple = new SimpleTemplateEngine()
        def binding = [detail: textDetail.toString(), lastDay: lastDay]
        def msg = simple.createTemplate(template).make(binding).toString()
        println msg
        def response = client.post(path: context, contentType: JSON, body: msg, query: [access_token: access_token], headers: [Accept: 'application/json'])
    }



    def detailSql = '''
SELECT *
FROM CRMR
'''

    def rows = [] as List
    sql.eachRow(detailSql) { row ->
        rows.add([row['DAY_STR'], row['APP_ID'], row['UID'], row['PLATFORM'], row['ADV_APP_ID'], timeFormat.format(row['CLICK_TIME']), timeFormat.format(row['REGISTER_TIME'])])
    }

    def dataMap = new HashMap();
    dataMap.put("AdvReport", rows)
    def xlsPath = xls.generateXls(dataMap)

    mailMan.sendMail("导量日报（${lastDay}）", "导量日报（${lastDay}）", configFile, xlsPath)

    new  File(xlsPath).delete();
}

cron4j.start(cron,{
    fetchDataFile()
    loadData()
    genReport()
})


