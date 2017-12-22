#! /usr/bin/env groovy

/*
1: hostname
2: add new user and add user to the group wheel(manual)
3: adjust sshd(manual)
2: /etc/hosts
3: max open files and max users process
*/

import java.text.SimpleDateFormat
import groovy.time.TimeCategory

def currentPath = new File(getClass().protectionDomain.codeSource.location.path).parent
GroovyShell groovyShell = new GroovyShell()
def shell = groovyShell.parse(new File(currentPath, "../../core/Shell.groovy"))
def logback = groovyShell.parse(new File(currentPath, "../../core/Logback.groovy"))
def cfg = new File(currentPath, 'osBuilderCfg.groovy')
cfg = new ConfigSlurper().parse(cfg.text);
def logger = logback.getLogger("infra.os")


def tmpDir = new File(System.getProperty("java.io.tmpdir"));
def etcHosts = new File(tmpDir,"etcHosts");
def rt = null
etcHosts.withWriter { w ->
    cfg.os.hosts.each {
        rt = shell.exec("hostname", it)
        if (!it.equals(rt['msg'].get(0))) {
            logger.error(">> local host name ${it},remote host name ${rt['msg'].get(0)} please fix it")
        } else {
            logger.info("** ${it}: local & remote matchs")
        }
        rt = shell.exec("hostname -I", it)
        w.write("${rt['msg'].get(0).trim()} ${it.trim()}")
        w.write("\n")
    }
}

logger.info("** /etc/hosts is generated at :${etcHosts.absolutePath}")




logger.info("Checking max open files ....")
cfg.os.hosts.each {
    rt = shell.exec("cat /etc/security/limits.conf", it)
    rt.msg.each { msg ->
        if (msg.startsWith("*")) {
            logger.info("/etc/security/limits.conf@[${it}]: ${msg}")
        }
    }
}

logger.info("Checking max process ....")
cfg.os.hosts.each {
    rt = shell.exec("ls /etc/security/limits.d", it)
    def proc = rt.msg[0]
    rt = shell.exec("cat /etc/security/limits.d/${proc}", it)
    rt.msg.each { msg ->
        if(msg && !msg.startsWith("#")){
            logger.info("/etc/security/limits.d/${proc}@[${it}]:${msg}")
        }
    }
}




