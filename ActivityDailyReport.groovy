#! /usr/bin/env groovy
import groovy.json.JsonSlurper
import groovy.transform.Field
import groovy.transform.Sortable
import groovy.transform.ToString
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.HorizontalAlignment

import org.apache.poi.ss.util.CellRangeAddress
import java.text.SimpleDateFormat

import static org.apache.poi.hssf.util.HSSFColor.*
import static org.apache.poi.ss.usermodel.Cell.*

@Grab(group = 'org.apache.poi', module = 'poi-ooxml', version = '3.17')
@GrabExclude('xml-apis:xml-apis')

def scriptDir = new File(getClass().protectionDomain.codeSource.location.path).parent
GroovyShell groovyShell = new GroovyShell()
def shell = groovyShell.parse(new File(scriptDir, "core/Shell.groovy"))
def db = groovyShell.parse(new File(scriptDir, "core/Db.groovy"))
def logger = groovyShell.parse(new File(scriptDir, "core/Logback.groovy")).getLogger(this.class.getTypeName())
def config = new ConfigSlurper().parse(new File(scriptDir, 'adrConfig.groovy').text)

@Field
String tmpDir = System.getProperty("java.io.tmpdir");

@ToString
@Sortable(includes = ['adv_appid'])
class Icon {
    String adv_appid
    Long pv
    Long uv
}

@ToString
@Sortable(includes = ['adv_appid'])
class Page {
    String adv_appid
    Long pv
    Long uv
}

@ToString
@Sortable(includes = ['appid', "platform"])
class GameDaily {
    String appid;
    String platform;
    String date;
    Long newuser;
    Long dau;
    Long onlineTime;
    List<Icon> icon;
    List<Page> page;
}
//advagg.2017-11-01-01.log
def games = config.setting.games
def aggPath = config.setting.aggPath
def title1 = config.setting.title1
def title2 = config.setting.title2
def title3 = config.setting.title3
def title4 = config.setting.title4
println title1
println title2
println title3
println title4

//assert args: "Please input date !"

def inputDate = "2017/11/02"


def date = Date.parse("yyyy/MM/dd", inputDate)

def sdf = new SimpleDateFormat("yyyy-MM-dd")
def path = new File(aggPath);
def pattern = ~/advagg\.${sdf.format(date)}.*\.log$/

def sqlCon = db.h2mCon("DailyReport")
def jsonSlurper = new JsonSlurper();
def aggList = [] as List
path.eachFile { f ->
    if (f.name =~ pattern) {
        f.eachLine { line ->
            def obj = jsonSlurper.parseText(line);
            aggList.add(new GameDaily(obj))
        }
    }
}

logger.info("There are total : ${aggList.size()} games agg data ...")

def loadData = {
    logger.info("Process click data ...")
    def ccmrFolder = new File(tmpDir, "ccmr/${sdf.format(date)}");
    ccmrFolder.deleteDir()
    ccmrFolder.mkdirs()

    logger.info("Files copied at : ${ccmrFolder.absolutePath}")
    def command = "hadoop fs -get /advdata/ccmr/2017/11/01/part* ${tmpDir}/ccmr/${sdf.format(date)}"
    def rt = shell.exec(command)
    if (rt["code"] != 0) {
        rt["msg"].each {
            logger.error(it)
        }
        return -1
    }

    sqlCon.execute('''
	CREATE TABLE IF NOT EXISTS t_click (
	ID INTEGER IDENTITY,
	adv_appid varchar(20),
	platform varchar(10),
	adved_appid varchar(20),
	uid varchar(20),
	times INTEGER)
''');


    def insert = '''insert into t_click(adv_appid, platform, adved_appid, uid, times) 
values(?,?,?,?,?)
'''
    sqlCon.withTransaction {
        ccmrFolder.eachFile { f ->
            logger.info("process file ${f.absolutePath}")
            f.splitEachLine("\t", { items ->
                sqlCon.withBatch(100, insert) { stmt ->
                    stmt.addBatch(items[0], items[1], items[2], items[3], Integer.valueOf(items[4]))
                }
            })
        }
    }

    logger.info("Process transform rate data ...")

    def crmrFolder = new File("${tmpDir}/crmr/${sdf.format(date)}");
    crmrFolder.deleteDir()
    crmrFolder.mkdirs()
    command = "hadoop fs -get /advdata/crmr/2017/11/01/part* ${tmpDir}/crmr/${sdf.format(date)}"

    rt = shell.exec(command)
    if (rt["code"] != 0) {
        rt["msg"].each {
            logger.error(it)
        }
        return -1
    }
    sqlCon.execute('''
	CREATE TABLE IF NOT EXISTS t_rate (
	ID INTEGER IDENTITY,
	adved_appid varchar(20),
	uid varchar(20),
	platform varchar(10),
	adv_appid varchar(20),
	click_time BIGINT,
	click_at timestamp,
	reg_time BIGINT,
	reg_at timestamp,
	status integer 
)
''');

    insert = '''insert into t_rate(adved_appid, uid, platform, adv_appid, click_time, click_at, reg_time, reg_at, status)  
values (?,?,?,?,?,?,?,?,?)'''
    sqlCon.withTransaction {
        crmrFolder.eachFile { f ->
            f.splitEachLine("\t", { items ->
                sqlCon.withBatch(100, insert) { stmt ->
                    stmt.addBatch(items[0], items[1], items[2], items[3], items[4], Date.parse("yyyy/MM/dd HH:mm:ss", items[5]), items[6], Date.parse("yyyy/MM/dd HH:mm:ss", items[7]), items[8])
                }
            })
        }
    }

}

logger.info("Generate report ...")

def wb = new HSSFWorkbook();
def createHelper = wb.getCreationHelper();
def sheet = wb.createSheet();
def currrentRow = sheet.createRow(0);
def currentCell = currrentRow.createCell(0)
currentCell.setCellValue("报表日期:")
currentCell = currrentRow.createCell(1)
currentCell.setCellValue(inputDate)

currrentRow = sheet.createRow(1)
currrentRow = sheet.createRow(2)
currentCell = currrentRow.createCell(0)
currentCell.setCellValue(title1[0])
//cell = row.createCell(1)
//cell.setCellValue(title1[1])
(1..3).each {
    currrentRow.createCell(it)
}
def allignCenterCellStyle = wb.createCellStyle();
allignCenterCellStyle.setAlignment(HorizontalAlignment.CENTER);
currrentRow.getCell(1).setCellValue(title1[1])
currrentRow.getCell(1).setCellStyle(allignCenterCellStyle)
sheet.addMergedRegion(new CellRangeAddress(2, 2, 1, 3))
(4..6).each {
    currrentRow.createCell(it)
}
currrentRow.getCell(4).setCellValue(title1[2])
sheet.addMergedRegion(new CellRangeAddress(2, 2, 4, 6))
currrentRow.getCell(4).setCellStyle(allignCenterCellStyle)
currrentRow = sheet.createRow(currrentRow.rowNum + 1);
(0..6).each {
    currentCell = currrentRow.createCell(it)
    currentCell.setCellValue(title2[it])
}

def previousAppid = null;
Collections.sort(aggList)
aggList.each {

    if (!it.appid.equals(previousAppid)) {
        currrentRow = sheet.createRow(currrentRow.rowNum + 1)
        (0..6).each {
            currrentRow.createCell(it)
        }
        currrentRow.getCell(0).setCellValue(games.get(it.appid))
        currrentRow.getCell(1).setCellValue(it.newuser)
        currrentRow.getCell(2).setCellValue(it.dau)
        currrentRow.getCell(3).setCellValue(it.onlineTime)
        currrentRow.getCell(4).setCellValue("-")
        currrentRow.getCell(5).setCellValue("-")
        currrentRow.getCell(6).setCellValue("-")
        previousAppid = it.appid
    } else {
        currrentRow.getCell(4).setCellValue(it.newuser)
        currrentRow.getCell(5).setCellValue(it.dau)
        currrentRow.getCell(6).setCellValue(it.onlineTime)
    }


}
currrentRow = sheet.createRow(currrentRow.rowNum + 1)
currrentRow = sheet.createRow(currrentRow.rowNum + 1)
(0..11).each {
    currrentRow.createCell(it)
}
currrentRow.getCell(0).setCellValue(title3[0])
sheet.addMergedRegion(new CellRangeAddress(currrentRow.rowNum, currrentRow.rowNum, 0, 1))
currrentRow.getCell(0).setCellStyle(allignCenterCellStyle)

currrentRow.getCell(2).setCellValue(title3[1])
sheet.addMergedRegion(new CellRangeAddress(currrentRow.rowNum, currrentRow.rowNum, 2, 6))
currrentRow.getCell(2).setCellStyle(allignCenterCellStyle)


currrentRow.getCell(7).setCellValue(title3[2])
sheet.addMergedRegion(new CellRangeAddress(currrentRow.rowNum, currrentRow.rowNum, 7, 11))
currrentRow.getCell(7).setCellStyle(allignCenterCellStyle)

currrentRow = sheet.createRow(currrentRow.rowNum + 1)
(0..11).each {
    currrentRow.createCell(it).setCellValue(title4[it])
}

def iconMap = [:]
def pageMap = [:]
aggList.each { agg ->

    agg.icon.each {
        iconMap.put("${agg.appid}:${it.adv_appid}", "${pv}:${uv}");
    }
    agg.page.each {

        pageMap.put("${agg.appid}:${it.adv_appid}", "${pv}:${uv}");
    }

}

FileOutputStream fileOut = new FileOutputStream("${sdf.format(date)}.xls");
wb.write(fileOut);
fileOut.close();
sqlCon.close()
