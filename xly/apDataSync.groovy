#! /usr/bin/env groovy
import groovy.time.TimeCategory
import java.text.SimpleDateFormat
import groovy.json.JsonSlurper

import static groovyx.net.http.ContentType.JSON
import groovy.time.TimeCategory
import groovyx.net.http.HTTPBuilder
import groovy.json.JsonOutput
import groovy.text.SimpleTemplateEngine

@Grab(group = 'org.codehaus.groovy.modules.http-builder', module = 'http-builder', version = '0.5.0')
import groovyx.net.http.RESTClient

def currentPath = new File(getClass().protectionDomain.codeSource.location.path).parent
GroovyShell groovyShell = new GroovyShell()
def cron4j = groovyShell.parse(new File(currentPath, "../core/Cron4J.groovy"))
def shell = groovyShell.parse(new File(currentPath, "../core/Shell.groovy"))
def logback = groovyShell.parse(new File(currentPath, "../core/Logback.groovy"))
def db = groovyShell.parse(new File(currentPath, "../core/Db.groovy"))

def configFile = new File(currentPath, 'apDataSyncCfg.groovy')
def config = new ConfigSlurper().parse(configFile.text)

def logPath = config.settings.logPath
def localPath = new File(config.settings.localPath)
def hdfsRoot = config.settings.hdfsPath

def logger = logback.getLogger("apDataSync", logPath)
def url = config.settings.url

def games = config.settings.games

def sql = db.h2mCon("apDataSync")

sql.execute('''
	CREATE TABLE IF NOT EXISTS APUL (
	ID INTEGER IDENTITY,
	APP_ID VARCHAR(20),
	APP_NAME VARCHAR(40),
	UID VARCHAR(20),
	PLATFORM VARCHAR(20),
	LOGIN_TIME BIGINT
)
''');
sql.execute('''
CREATE INDEX IF NOT EXISTS INFO_IDX1 ON APUL(APP_ID);
''')



def fetchData = {
    if (localPath.exists()) {
        localPath.deleteDir()
    }
    localPath.mkdirs()
    def pathFormat = new SimpleDateFormat("yyyy/MM/dd/HH")
    def now = Calendar.getInstance().getTime();
    def jsonSlurper = new JsonSlurper()
    def insert = '''INSERT INTO APUL(APP_ID,APP_NAME,UID,PLATFORM,LOGIN_TIME) VALUES (?,?,?,?)'''

    use(TimeCategory) {
        def previousHour = now - 1.hours
        def command = "hadoop fs -get ${hdfsRoot}/${pathFormat.format(previousHour)}/input/*  ${localPath}"
        def rt = shell.exec(command)
        if (rt["code"] != 0) {
            logger.warn("can not download file from ${hdfsRoot}/${pathFormat.format(first)}/p* ")
        }
        localPath.eachFile { f ->
            if (f.name.indexOf("userlogin") > -1) {
                sql.withTransaction {
                    sql.withBatch(100, insert) { stmt ->
                        (1..2).find {
                            def format = (now - it.hours).format("yyyy-MM-dd-HH")
                            if (f.name =~ /.*\.$format\.log$/) {
                                f.eachLine { line ->
                                    def obj = jsonSlurper.parseText(line)
                                    if (games.contains(obj['appid'])) {
                                        stmt.addBatch(obj["appid"], games[obj['appid']], obj["uid"], obj["platform"], obj["time"])
                                    }
                                }
                            }
                        }
                    }

                }
            }
        }
    }

} as Runnable

def dataSync = {

    def template = '''
 {
    "biz_content": {
        "scene_code": "personal_base_data",
        "op_code": "data_save",
        "channel": "xiangley_game",
        "version": "2.0",
        "target_id": "${uid}",
        "op_data": [
            {
                "game_appid": "${appId}",
                "game_name": "${appName}",
                "game_achieve": "",
                "last_online_duration ": "0",
                "last_logon_time": "${lastLogin}"
            }
        ]
    }
}
'''
    def query = "SELECT APP_ID, APP_NAME, UID, MAX(LOGIN_TIME) LAST_LOGIN FROM APUL GROUP BY APP_ID, APP_NAME,UID ORDER BY APP_ID"
//    def client = new RESTClient(url)
    sql.eachRow(query) { row ->
        logger.info(row)
//        def simple = new SimpleTemplateEngine()
//        def binding = [uid: row['UID'], appId: row['APP_ID'], appName: row['APP_NAME'], lastLogin: row['LAST_LOGIN']]
//        def msg = simple.createTemplate(template).make(binding).toString()
//        logger.info("msg: ${msg}")
//        def response = client.post(path: context, contentType: JSON, body: msg, query: [access_token: access_token], headers: [Accept: 'application/json'])
//        logger.info("Response -> " + response)
    }

    logger.info("clean up database ...... ")
    def delete = "DELETE FROM APUL";
    sql.execute(delete);
    def check = "SELECT COUNT(1) as CNT FROM APUL"
    def rt = sql.firstRow(check)
    logger.info("[cleanup]: There are ${rt.CNT} rows in db.")
}
fetchData()
dataSync()
def cron = "25 * * * *"
/*
cron4j.start(cron, {
    fetchData()
    dataSync()
})

*/


