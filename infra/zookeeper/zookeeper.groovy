#! /usr/bin/env groovy

import groovy.text.*
import org.codehaus.groovy.runtime.StringBufferWriter

def currentPath = new File(getClass().protectionDomain.codeSource.location.path).parent
GroovyShell groovyShell = new GroovyShell()
def shell = groovyShell.parse(new File(currentPath, "../../core/Shell.groovy"))
def logback = groovyShell.parse(new File(currentPath, "../../core/Logback.groovy"))
//def clipboard = groovyShell.parse(new File(currentPath, "../../core/Clipboard.groovy"))
def osBuilder = groovyShell.parse(new File(currentPath, "../os/osBuilder.groovy"))

def logger = logback.getLogger("infra-zk")
def configFile = new File( 'zookeeperCfg.groovy')
def config = null
if(configFile.exists()){
    config = new ConfigSlurper().parse(configFile.text)
}



def buildOs = {onRemote ->
    osBuilder.etcHost(config.setting.hosts,onRemote)
}

def deploy = { deployable, host ->
    if (config.setting.hosts.contains(host)) {

        deployable = new File(deployable)
        if (!deployable.exists()) logger.error "Can't find the deployable ${deployable}"
        def rootName = deployable.name.replace(".tar", "").replace(".gz", "");
        def tmpDir = File.createTempDir()

        def rt = shell.exec("tar -vxf ${deployable.absolutePath} -C ${tmpDir.absolutePath}")


        logger.info("** Building zoo.cfg")
        def sb = new StringBuilder();
        sb.append("tickTime=${config.setting.tickTime}\n")
        sb.append("initLimit=${config.setting.initLimit}\n")
        sb.append("syncLimit=${config.setting.syncLimit}\n")
        sb.append("clientPort=${config.setting.clientPort}\n")
        sb.append("dataDir=${config.setting.dataDir}\n")
        config.setting.hosts.eachWithIndex { s, idx ->
            sb.append("server.${idx + 1}=${s}:${config.setting.serverPort}:${config.setting.leaderPort}\n")
        }
//        clipboard.copy(sb.toString())

        def zooCfg = new File("${tmpDir.absolutePath}/${rootName}/conf/zoo.cfg")
//        zooCfg << clipboard.paste()
        zooCfg << sb.toString().bytes
        def log4j = new File("${tmpDir.absolutePath}/${rootName}/conf/log4j.properties")


        def lines = log4j.readLines();
        log4j.withWriter { w ->
            def bw = new BufferedWriter(w)
            lines.each { line ->
                if (line.startsWith("zookeeper.log.dir")) {
                    line = "#${line}${System.getProperty("line.separator")}zookeeper.log.dir=${config.setting.log4j}"
                } else if (line.startsWith("zookeeper.tracelog.dir")) {
                    line = "#${line}${System.getProperty("line.separator")}zookeeper.tracelog.dir=${config.setting.traceLog}"
                }
                bw << line
                bw.newLine()
            }
            bw.close()
        }
        rt = shell.exec("tar -cvzf  ${tmpDir.absolutePath}/${rootName}.tar -C ${tmpDir.absolutePath} ./${rootName}")

        rt = osBuilder.deploy(new File("${tmpDir.absolutePath}/${rootName}.tar"), host,"zkCli.sh", "ZK_HOME")
        tmpDir.deleteDir()

        if (rt != 1) {
            logger.error "Failed to deploy ${deployable} on ${host}"
            return -1
        }

        logger.info("** Creating ${config.setting.dataDir} for {}", host)
        def ug = shell.sshug(host)
        rt = shell.exec("ls -l ${config.setting.dataDir}", host);
        if (rt.code) {
            def pathBuffer = new StringBuilder();
            config.setting.dataDir.split("/").each { p ->
                pathBuffer.append("/").append(p)
                rt = shell.exec("ls -l ${pathBuffer.toString()}", host);
                if (rt.code) {
                    rt = shell.exec("sudo mkdir ${pathBuffer.toString()}", host)
                    rt = shell.exec("sudo chown ${ug.u}:${ug.g} ${pathBuffer.toString()}", host)
                }
            }

            logger.info("** Creating ${config.setting.log4j} for {}", host)
            config.setting.log4j.split("/").each { p ->
                pathBuffer.append("/").append(p)
                rt = shell.exec("ls -l ${pathBuffer.toString()}", host);
                if (rt.code) {
                    rt = shell.exec("sudo mkdir ${pathBuffer.toString()}", host)
                    rt = shell.exec("sudo chown ${ug.u}:${ug.g} ${pathBuffer.toString()}", host)
                }
            }

            logger.info("** Creating ${config.setting.traceLog} for {}", host)
            config.setting.traceLog.split("/").each { p ->
                pathBuffer.append("/").append(p)
                rt = shell.exec("ls -l ${pathBuffer.toString()}", host);
                if (rt.code) {
                    rt = shell.exec("sudo mkdir ${pathBuffer.toString()}", host)
                    rt = shell.exec("sudo chown ${ug.u}:${ug.g} ${pathBuffer.toString()}", host)
                }
            }
        }

        logger.info("** Creating ${config.setting.dataDir}/myid for {}", host)
        rt = shell.exec("ls -l ${config.setting.dataDir}/myid", host);
        if (rt.code) {
            rt = shell.exec("echo ${config.setting.hosts.indexOf(host) + 1} > ${config.setting.dataDir}/myid", host)
            if (!rt.code) {
                rt = shell.exec("sudo chown ${ug.u}:${ug.g} ${config.setting.dataDir}/myid", host)
                rt = shell.exec("stat -c '%n %U %G %y' ${config.setting.dataDir}/myid", host)
            }
        }


    } else {
        logger.error "${host} is not in the server list : ${config.setting.hosts.toString()}"
    }
}

if (!args) {
    logger.info("** make sure your settings are correct and then run the command : build or deploy {zookeeper.tar} {host} ")
} else {
    if("init".equalsIgnoreCase(args[0])){
        new File("zookeeperCfg.groovy").withWriter {w ->
            w << new File(currentPath, "zookeeperCfg.groovy").text
        }
        logger.info "** Please do the changes according to your environments in zookeeperCfg.groovy"

    } else {
        if(!configFile.exists()){
            logger.error "Can't find ${configFile.absolutePath}, please init project first"
            return -1
        }
        if ("build".equalsIgnoreCase(args[0])) {
            buildOs(args.length > 1 ? true:false)
        } else if ("deploy".equalsIgnoreCase(args[0])) {
            deploy(args[1], args[2])
        }
    }
}
