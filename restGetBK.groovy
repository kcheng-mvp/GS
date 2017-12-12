#! /usr/bin/env groovy
import java.text.SimpleDateFormat

import static groovyx.net.http.ContentType.JSON
import groovy.time.TimeCategory
import groovyx.net.http.HTTPBuilder
import groovy.json.JsonOutput

@Grab(group = 'org.codehaus.groovy.modules.http-builder', module = 'http-builder', version = '0.5.0')

def currentPath = new File(getClass().protectionDomain.codeSource.location.path).parent
GroovyShell groovyShell = new GroovyShell()
def cron4j = groovyShell.parse(new File(currentPath, "core/Cron4J.groovy"))
def digestUtils = groovyShell.parse(new File(currentPath, "core/DigestUtils.groovy"))
def logback = groovyShell.parse(new File(currentPath, "core/Logback.groovy"))

def properties = new Properties()
properties.load(getClass().getResourceAsStream('restGet.properties'));

def logPath = properties.get("logPath")
def log = logback.getLogger("restGet", "/home/hadoop/sandbox")
//def dataLog = logback.getDataLogger("userregister.dts", properties.get("dataPath"))


def url = properties.get("url")
def appid = properties.get("key1")

def key = properties.get("key2")

def sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss")

def start = Calendar.getInstance().getTime();
def hourlyRegister = { hour ->
    def sign = digestUtils.md5("app_id=${appid}&key=${key}")

//    def start = Calendar.getInstance().getTime();
    use(TimeCategory) {
        start = start - hour.hours
//        start = start - 4.days
        start.set(minute: 0, second: 0, millisecond: 0)
//        start.set(hour:0,minute: 0, second: 0, millisecond: 0)
    }



    def begin = start.getTime();

//    def end = start + 24*60 * 60 * 1000
    def end = begin + 60 * 60 * 1000

//    log.info("fetch data for : [${sdf.format(new Date(start))}, ${sdf.format(new Date(end))}]");
//    def  f  = new File("/home/hadoop/tools/shell/dts/userregister.dts.${sdf.format(new Date(start))}.log")

    log.info("fetch data for : [${sdf.format(new Date(begin))}, ${sdf.format(new Date(end))}]");
    log.info("url -> ${url}/api/xlygetpuid/?start_time=${(begin / 1000) as Long}&end_time=${(end / 1000) as Long}&sign=${sign}")

    def http = new HTTPBuilder(url)

    def nsdf = new SimpleDateFormat("yyyy-MM-dd-HH");
    http.get(path: '/api/xlygetpuid',
            contentType: JSON,
            query: [start_time: (begin / 1000) as Long, end_time: (end / 1000) as Long, sign: sign]) { resp, reader ->
        def f = new File("/home/hadoop/sandbox/dts/userregister.dts.${nsdf.format(start)}.log")
        f.withWriter { out ->
            reader.data.each {
                out.println(JsonOutput.toJson(it))
            }
        }

    }

} as Runnable

//def cron = "05 * * * *"
//cron4j.start(cron, hourlyRegister)
for (int i = 300; i--; i > 0) {
    hourlyRegister(1)
}

