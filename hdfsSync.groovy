#! /usr/bin/env groovy
import groovy.time.TimeCategory

def currentPath = new File(getClass().protectionDomain.codeSource.location.path).parent
GroovyShell groovyShell = new GroovyShell()
def cron4j = groovyShell.parse(new File(currentPath, "core/Cron4J.groovy"))
def shell = groovyShell.parse(new File(currentPath, "core/Shell.groovy"))

///data/logserver/userinfo/2017/10/01/00/input/UserInfo-2017-10-01-00.log_hbweb102

//def config = new File(currentPath, "hdfsSync.properties");
def properties = new Properties()
properties.load(getClass().getResourceAsStream('hdfsSync.properties'));
println properties;


def localPath = new File(properties.get("localPath"))
def hdfsRoot = properties.get({hdfsRoot})
def dataSync = {
    def now = Calendar.getInstance().getTime();
    use(TimeCategory) {
        localPath.eachFile { f ->
            (1..5).collect {
                def format= (now - it.hours).format("yyyy-MM-dd-HH")
                def datePath= (now - it.hours).format("yyyy/MM/dd/HH")
                if(f.name =~ /.*\.$format\.log$/) {
                    def category = f.name.split("\\.")[0]
                    def command = "hadoop fs -ls ${hdfsRoot}/${category}/${datePath}/input"
                    def rt = shell.exec(command)
                    println "code ->" + rt["code"]
                    println "msg -> "+ rt["msg"]

                }
            }
        }

    }
} as Runnable

//def cron = "30 * * * *"
def cron = "* * * * *"

//cron4j.start(cron, sayHello)


dataSync()
