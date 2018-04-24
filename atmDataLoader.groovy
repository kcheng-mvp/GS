import groovy.io.FileType
import groovy.time.TimeCategory


@Grapes(
        @Grab(group='mysql', module='mysql-connector-java', version='5.1.16')
)
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
	UID VARCHAR(8),
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



hdfssync = { remote, localDir ->
    
    def type = localDir.split("/")[0]
    new File(localBaseDir, type).with {
        if(it.exists()){
            it.deleteDir();
        }
    }
    def f = new File(localBaseDir,localDir)
    f.mkdirs();
    def command = "hadoop fs -get ${hdfsRoot}/${remote}  ${localDir}"
    def rt = shell.exec(command)
    rt.msg.each { it ->
        logger.info it
    }
    [code: rt.code, msg: f.absolutePath]
}



dau = { it ->

    def h2= db.h2mCon("atm")

    h2.execute(createTable)
    h2.execute(createIndex)

    def mysql = Sql.newInstance(config.get("db.host"), config.get("db.username"), config.get("db.username"), "com.mysql.jdbc.Driver");

    logger.info("** Today is ${Calendar.getInstance().format("yyyy/MM/dd")}")
    def today = it ? (Date.parse("yyyy/MM/dd", it)) : use(TimeCategory) {
        Calendar.getInstance().getTime() - 1.days;
    }
    def remote = "/dau/${today.format("yyyy/MM/dd")}"

    def insert = "INSERT INTO T_ATM(CHANNEL,UID,CATEGORY,COUNT) VALUES (?,?,?,?)"
    def numOfLines = 0;
    if (!hdfssync(remote, "dau/${day.format('yyyy/MM/dd')}")) {
        localDir.eachFileRecurse(FileType.FILES, { f ->
            //@todo need to check should be processed or not and file name pattern
            if (f.name.indexOf("part-r-") > -1) {
                def category = f.getParent().toUpperCase();
                h2.withTransaction {
                    h2.withBatch(100, insert) { stmt ->
                        f.eachLine { line ->
                            numOfLines++
                            def entries = line.split("\t")
                            stmt.addBatch(entries[0], entries[1], category,entries[2]);
                        }
                    }
                }
            }

        })
    }
    logger.info("There are totally ${numOfLines} have been processed !")

    def query = "SELECT CHANNEL,CATEGORY,COUNT(1) AS DAU,SUM(COUNT) AS TOTAL_LOGIN FROM T_ATM GROUP BY CHANNEL,CATEGORY ORDER BY CHANNEL,CATEGORY"
    def check = "select 1 from T_USER_ACTIVITY where REPORT_DAY = ? CHANNEL = ?"
    def categoryColumnMap = ["D":"DAU","W":"WAU","M":"MAU"]
    h2.eachRow(query) { row ->
        def rs = mysql.first(check, [today, row.CHANNEL])

        def actionSql = rs ? new StringBuffer("UPDATE T_USER_ACTIVITY") : new StringBuffer("INSERT INTO T_USER_ACTIVITY");
        logger.info("${row.CNT}, ${row.CHANNEL}, ${row.CATEGORY}")
        if(rs){
           // update
            mysql.execute("UPDATE T_USER_ACTIVITY set ${categoryColumnMap.get(row.CATEGORY)} = ? WHERE REPORT_DAY = ? and CHANNEL = ?", [row.CNT, today, row.CHANNEL])
        } else {
           // insert
            mysql.execute("INSERT INTO T_USER_ACTIVITY(${categoryColumnMap.get(row.CATEGORY)}, REPORT_DAY,CHANNEL) values (?,?,?)", [row.CNT, today, row.CHANNEL])
        }
        logger.info("${row.CNT}, ${row.CHANNEL}, ${row.CATEGORY}")
        //@todo need to insert the result to database
        //@todo update process status in hdfs
    }
    h2.close()
    mysql.close()


}
retain = { it ->

    logger.info("** Today is ${Calendar.getInstance().format("yyyy/MM/dd")}")
    def today = it ? (Date.parse("yyyy/MM/dd", it)) : use(TimeCategory) {
        Calendar.getInstance().getTime() - 1.days;
    }

    def sameDayOfPreviousMonth = use(TimeCategory){
        today - 1.month -1.days;
    }

    println today.format("yyyy/MM/dd")
    println sameDayOfPreviousMonth.format("yyyy/MM/dd")

//    def remote = "/dau/${today.format("yyyy/MM/dd")}"
    sameDayOfPreviousMonth.upto(today){day ->
        hdfssync("/retain/${day.format("yyyy/MM/dd/*")}","retain/${day.format('yyyy/MM/dd')}")
    }
}

register = { it ->

    def today = Calendar.getInstance().getTime();
    hdfssync("/register/${day.format("yyyy/MM/dd/*/input/*")}","register/${day.format('yyyy/MM/dd')}")

}

def cleanup = {
    logger.info("Clean up database ...... ")
    def delete = "DELETE FROM T_ATM";
    sql.execute(delete);
    def check = "SELECT COUNT(1) as CNT FROM CRMR"
    def rt = sql.firstRow(check)
    logger.info("There are ${rt.CNT} rows in db.")
}

//cron4j.start("30 11 * * *", crmr)
//cron4j.start("40 11 * * *", ccmr)

retain()




