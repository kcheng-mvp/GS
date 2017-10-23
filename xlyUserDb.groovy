#! /usr/bin/env groovy


import java.text.SimpleDateFormat;

if (!args || args.length < 3) {
    println "Please input appid: eg xlyUserDb.groovy filename appid namePattern"
    return -1;
}

def scriptDir = new File(getClass().protectionDomain.codeSource.location.path).parent
GroovyShell shell = new GroovyShell()
def plainText = shell.parse(new File(scriptDir, "core/PlainText.groovy"))
def db = shell.parse(new File(scriptDir, "core/Db.groovy"))

def online = args[0].equalsIgnoreCase("o");
def file = new File(args[1].trim());
def appID = args[2].trim();
def namePattern = null;
if(args.length > 3) namePattern = args[3]

def sqlCon = db.h2Con(appID)

sqlCon.execute('''
	CREATE TABLE IF NOT EXISTS USERiNFO (
	ID INTEGER IDENTITY,
	LOG_TIME timestamp,
	LOG_DAY varchar(10),
	SDK_VERSION varchar(200),
	APP_ID varchar(50),
	UID varchar(50),
	ACCOUNT_ID varchar(50),
	PLATFORM varchar(50),
	CHANNEL varchar(50),
	ACCOUNT_TYPE varchar(10),
	GENDER varchar(10),
	AGE  varchar(2),
	GAME_SERVER varchar(100),
	RESOLUTION varchar(30),
	OS varchar(30),
	BRAND varchar(30),
	NET_TYPE varchar(10),
	COUNTRY varchar(10),
	PROVINCE varchar(50),
	CARRIER varchar(50),
	Extend1 varchar(30),
    Extend2 varchar(30),
    Extend3 varchar(30),
    Extend4 varchar(30),
    Extend5 varchar(2048),
    ACT_TIME INTEGER,
    REG_TIME INTEGER,
    ACCOUNT_NUM varchar(30)
)
''');

sqlCon.execute('''
CREATE INDEX IF NOT EXISTS INFO_IDX1 ON USERINFO(LOG_DAY);
CREATE INDEX IF NOT EXISTS INFO_IDX2 ON USERINFO(UID,ACCOUNT_ID);
''')


sqlCon.execute('''
	CREATE TABLE IF NOT EXISTS USERONLINE (
	ID INTEGER IDENTITY,
	LOG_TIME timestamp,
	LOG_DAY varchar(10),
	SDK_VERSION varchar(200),
	APP_ID varchar(50),
	UID varchar(50),
	ACCOUNT_ID varchar(50),
	PLATFORM varchar(50),
	CHANNEL varchar(50),
	ACCOUNT_TYPE varchar(10),
	GENDER varchar(10),
	AGE  varchar(2),
	GAME_SERVER varchar(100),
	RESOLUTION varchar(30),
	OS varchar(30),
	BRAND varchar(30),
	NET_TYPE varchar(10),
	COUNTRY varchar(10),
	PROVINCE varchar(50),
	CARRIER varchar(50),
	Extend1 varchar(30),
    Extend2 varchar(30),
    Extend3 varchar(30),
    Extend4 varchar(30),
    Extend5 varchar(2048),
    LOGIN_TIME INTEGER,
    ONLINE_TIME INTEGER,
    LEVEL varchar(30)
)
''');
sqlCon.execute('''
CREATE INDEX IF NOT EXISTS ONLINE_IDX1 ON USERONLINE(LOG_DAY);
CREATE INDEX IF NOT EXISTS ONLINE_IDX2 ON USERONLINE(UID,ACCOUNT_ID);
''')

if (file.exists()) {
    def dataMap = new HashMap();
    def dataList = new ArrayList();
    if (file.isDirectory()) {
        file.eachFileRecurse {
            if(!namePattern || (it.name.indexOf(namePattern) > -1)){
                println "Process file ${it.name} ..."
                dataList.addAll(plainText.split(it, appID));
            }
        }
    } else {
        if(!namePattern || (file.name.indexOf(namePattern) > -1)){
            println "Process file ${file.name} ..."
            dataList.addAll(plainText.split(file, appID));
        }
    }

    def insertUserInfo = '''INSERT INTO USERiNFO(
	LOG_TIME,LOG_DAY,SDK_VERSION,APP_ID,UID,
	ACCOUNT_ID,PLATFORM,CHANNEL,ACCOUNT_TYPE,
	GENDER,AGE,GAME_SERVER,RESOLUTION,OS,BRAND,
	NET_TYPE,COUNTRY,PROVINCE,CARRIER,
	Extend1,Extend2,Extend3,Extend4,
    Extend5,ACT_TIME,REG_TIME,ACCOUNT_NUM)
    values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
    '''

    def insertUserOnline = '''INSERT INTO USERoNLINE(
	LOG_TIME,LOG_DAY,SDK_VERSION,APP_ID,UID,
	ACCOUNT_ID,PLATFORM,CHANNEL,ACCOUNT_TYPE,
	GENDER,AGE,GAME_SERVER,RESOLUTION,OS,BRAND,
	NET_TYPE,COUNTRY,PROVINCE,CARRIER,
	Extend1,Extend2,Extend3,Extend4,
    Extend5,LOGIN_TIME,ONLINE_time,LEVEL)
    values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
    '''

    def insert = online ? insertUserOnline : insertUserInfo

    def sdf = new SimpleDateFormat("yyyy/MM/dd");
    sqlCon.withTransaction {
        sqlCon.withBatch(100, insert){stmt ->
            dataList.each {
                def logTime = new Date(Long.valueOf(it[0]))
                def logDay = sdf.format(logTime)
                def actTime =  Integer.valueOf(it[23])
                def regTime = Integer.valueOf(it[24])
                stmt.addBatch(logTime,logDay,it[1],it[2],it[3],it[4],it[5],it[6],it[7],it[8],it[9],it[10],
                        it[11],it[12],it[13],it[14],it[15],it[16],it[17],it[18],it[19],it[20],it[21],
                        it[22],actTime,regTime,it[25])
            }

        }
    }
}

sqlCon.close();
