#! /usr/bin/env groovy
import java.text.SimpleDateFormat


def scriptDir = new File(getClass().protectionDomain.codeSource.location.path).parent
GroovyShell groovyShell = new GroovyShell()
def mailMan = groovyShell.parse(new File(scriptDir, "core/Mailman.groovy"))
def cron4j = groovyShell.parse(new File(scriptDir, "core/Cron4J.groovy"))
def config = new File(scriptDir, 'MailAlertConfig.groovy')
def sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
def subjects = ["【服务器警告】：CPU 15分钟负载过高", "【服务警告】：JStrom计算任务失败"]
//"【服务警告】：Data Node 进程挂机", "【服务器警告】：硬盘使用超过80%" , "【服务警告】：Second Name Node 进程挂机",

def hosts = [
        "hbhadoop103",
        "hbhadoop101",
        "hbhadoop102",
        "hbase001",
        "hbase002",
        "hbase003",
        "web103",
        "web102",
        "web101",
        "jstrom001"]

def current = Calendar.getInstance().getTimeInMillis();
def index = current % 2 as Integer
def host = null
if (index != 1 && index != 2 && index != 3 && index != 4) {
    host = hosts.get(index % 10)
} else {
    if (index == 1 || index == 2 || index == 3) host = hosts.get(index % 6)
    if (index == 4) host = "jstrom001";
}
def msg = new StringBuffer("主机:${host}");

def subject = "${sdf.format(Calendar.getInstance().getTime())} ${subjects.get(index)} 主机：${host}"
if (index == 0) msg.append("CPU 15分钟负载 超过5")
println config
mailMan.sendMail(subject, msg.toString(), config)



