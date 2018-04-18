import groovy.io.FileType
import groovy.time.TimeCategory

def currentPath = new File(getClass().protectionDomain.codeSource.location.path).parent
GroovyShell groovyShell = new GroovyShell()
def shell = groovyShell.parse(new File(currentPath, "core/Shell.groovy"))
def cron4j = groovyShell.parse(new File(currentPath, "core/Cron4J.groovy"))
def db = groovyShell.parse(new File(currentPath, "core/Db.groovy"))
def logback = groovyShell.parse(new File(currentPath, "core/Logback.groovy"))
def config = new File(currentPath, 'atmDataLoaderCfg.groovy')
config = new ConfigSlurper().parse(config.text).flatten();
def logger = logback.getLogger("amtDataLoader", config.get("setting.logPath"))
def baseDir = new File(config.get("setting.localPath"))
def hdfsRoot = config.get("setting.hdfsRoot");

def sql = db.h2mCon("atm")

sql.execute('''
	CREATE TABLE IF NOT EXISTS T_ATM (
	ID INTEGER IDENTITY,
	UID VARCHAR(8),
	CHANNEL INTEGER, 
	DATE_STR VARCHAR(12),
	COUNT INTEGER,
	CATEGORY VARCHAR(12)
)
''');
sql.execute('''
CREATE INDEX IF NOT EXISTS INFO_IDX1 ON T_ATM(CHANNEL);
CREATE INDEX IF NOT EXISTS INFO_IDX2 ON T_ATM(CATEGORY);
''')

if (!baseDir.exists()) baseDir.mkdirs();


hdfssync = { remote, localDir ->

    new File(logger).with {
        if(it.exists()) {
            it.deleteDir()
        }
        it.mkdirs();
    }
    def command = "hadoop fs -get ${hdfsRoot}/${remote}  ${localDir}"
    def rt = shell.exec(command)
    rt.msg.each { it ->
        logger.info it
    }
    rt.code
}



dau = { it ->


    def dauBaseDir = new File(baseDir,"dau");
    if(!dauBaseDir.exists()) dauBaseDir.mkdirs();
    logger.info("** Today is ${Calendar.getInstance().format("yyyy/MM/dd")}")
    def today = it ? (Date.parse("yyyy/MM/dd", it)) : use(TimeCategory) {
        Calendar.getInstance().getTime() - 1.days;
    }
//    def localDir = new File(baseDir, "dau-${today.format("yyyyMMdd")}")
//    if (!localDir.exists()) localDir.mkdirs();
    def remote = "/dau/${today.format("yyyy/MM/dd")}"

    def insert = "INSERT INTO T_ATM(CHANNEL,UID,CATEGORY,COUNT) VALUES (?,?,?,?)"
    def numOfLines = 0;
    if (!hdfssync(remote, new File(dauBaseDir,day.format("yyyy/MM/dd")).absolutePath)) {
        localDir.eachFileRecurse(FileType.FILES, { f ->
            //@todo need to check should be processed or not and file name pattern
            if (true) {
                def category = f.getParent();
                sql.withTransaction {
                    sql.withBatch(100, insert) { stmt ->
                        f.eachLine { line ->
                            numOfLines++
                            def entries = line.split()
                            stmt.addBatch(entries[0], entries[1], category);
                        }
                    }
                }
            }

        })
    }
    logger.info("There are totally ${numOfLines} have been processed !")

    def query = "SELECT COUNT(1) AS DAU,SUM(COUNT) AS TOTAL_LOGIN, CHANNEL,CATEGORY FROM T_ATM GROUP BY CHANNEL,CATEGORY ORDER BY CHANNEL,CATEGORY"
    sql.eachRow(query) { row ->
        logger.info("${row.CNT}, ${row.CHANNEL}, ${row.CATEGORY}")
        //@todo need to insert the result to database
        //@todo update process status in hdfs
    }


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
        hdfssync("/retain/${day.format("yyyy/MM/dd/*")}","${new File(baseDir,day.format("yyyy/MM/dd")).absolutePath}")
    }
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



