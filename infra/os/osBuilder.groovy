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
import java.awt.datatransfer.StringSelection
import java.awt.Toolkit
import java.awt.datatransfer.*

def currentPath = new File(getClass().protectionDomain.codeSource.location.path).parent
GroovyShell groovyShell = new GroovyShell()
def shell = groovyShell.parse(new File(currentPath, "../../core/Shell.groovy"))
def logback = groovyShell.parse(new File(currentPath, "../../core/Logback.groovy"))
def cfg = new File(currentPath, 'osBuilderCfg.groovy')
cfg = new ConfigSlurper().parse(cfg.text);
def logger = logback.getLogger("infra.os")


def tmpDir = new File(System.getProperty("java.io.tmpdir"));
def etcHosts = new StringBuffer();
def rt = null
cfg.os.hosts.each {host ->
    rt = shell.exec("hostname", host)
    if (!host.equals(rt['msg'].get(0))) {
        logger.error(">> local host name ${host},remote host name ${rt['msg'].get(0)} please fix it")
    }
    rt = shell.exec("hostname -I", host)
    etcHosts.append("${rt['msg'].get(0).trim()} ${host.trim()}").append("\n")
    rt = shell.exec("ls ~/.ssh/id_rsa", host)
    if(!rt.code){
        logger.info("** ssh key ~/.ssh/id_rsa exists on ${host}")
    } else {
        logger.info("Generate ssh key for [@${host}]")
        rt = shell.exec("ssh-keygen -b 4096 -q -N '' -C '${host}' -f ~/.ssh/id_rsa",host)
    }
}
def clipboard = Toolkit.getDefaultToolkit().getSystemClipboard()
clipboard.setContents(new StringSelection(etcHosts.toString()), null)
logger.info("Host info is in System Clipboard ...")



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
        if (msg && !msg.startsWith("#")) {
            logger.info("/etc/security/limits.d/${proc}@[${it}]:${msg}")
        }
    }
}


