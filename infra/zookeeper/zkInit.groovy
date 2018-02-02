#! /usr/bin/env groovy

import groovy.text.*
import org.codehaus.groovy.runtime.StringBufferWriter

def currentPath = new File(getClass().protectionDomain.codeSource.location.path).parent
GroovyShell groovyShell = new GroovyShell()
def shell = groovyShell.parse(new File(currentPath, "../../core/Shell.groovy"))
def logback = groovyShell.parse(new File(currentPath, "../../core/Logback.groovy"))
def clipboard = groovyShell.parse(new File(currentPath, "../../core/Clipboard.groovy"))
def osBuilder = groovyShell.parse(new File(currentPath, "../os/osBuilder.groovy"))

def logger = logback.getLogger("infra-zk")
def configFile = new File( 'zkInitCfg.groovy')
if(!configFile.exists()){
    logger.error "Can not find the ${configFile.absolutePath} ..."
    return -1
}
def config = new ConfigSlurper().parse(configFile.text)



def cfg = {onRemote ->
    osBuilder.etcHost(config.settings.server,onRemote)
}

def deploy = { deployable, host ->
    if (config.settings.server.contains(host)) {

        deployable = new File(deployable)
        if (!deployable.exists()) logger.error "Can't find the deployable ${deployable}"
        def rootName = deployable.name.replace(".tar", "").replace(".gz", "");
        def tmpDir = File.createTempDir()

        def rt = shell.exec("tar -vxf ${deployable.absolutePath} -C ${tmpDir.absolutePath}")


        logger.info("******************** Start building zoo.cfg(clipboard) ********************")
        def sb = new StringBuilder();
        sb.append("tickTime=${config.settings.tickTime}\n")
        sb.append("initLimit=${config.settings.initLimit}\n")
        sb.append("syncLimit=${config.settings.syncLimit}\n")
        sb.append("clientPort=${config.settings.clientPort}\n")
        sb.append("dataDir=${config.settings.dataDir}\n")
        config.settings.server.eachWithIndex { s, idx ->
            sb.append("server.${idx + 1}=${s}:${config.settings.serverPort}:${config.settings.leaderPort}\n")
        }
        clipboard.copy(sb.toString())

        def zooCfg = new File("${tmpDir.absolutePath}/${rootName}/conf/zoo.cfg")
        zooCfg << clipboard.paste()
        def log4j = new File("${tmpDir.absolutePath}/${rootName}/conf/log4j.properties")


        def lines = log4j.readLines();
        log4j.withWriter { w ->
            def bw = new BufferedWriter(w)
            lines.each { line ->
                if (line.startsWith("zookeeper.log.dir")) {
                    line = "#${line}${System.getProperty("line.separator")}zookeeper.log.dir=${config.settings.log4j}"
                } else if (line.startsWith("zookeeper.tracelog.dir")) {
                    line = "#${line}${System.getProperty("line.separator")}zookeeper.tracelog.dir=${config.settings.traceLog}"
                }
                bw << line
                bw.newLine()
            }
            bw.close()
        }
        rt = shell.exec("tar -cvzf  ${tmpDir.absolutePath}/${rootName}.tar -C ${tmpDir.absolutePath} ./${rootName}")

        rt = osBuilder.deploy(host, new File("${tmpDir.absolutePath}/${rootName}.tar"), "zkCli.sh", "ZK_HOME")
        tmpDir.deleteDir()

        if (rt != 1) {
            logger.error "Failed to deploy ${deployable} on ${host}"
            return -1
        }

        logger.info("**** Creating ${config.settings.dataDir} for {}", host)
        def ug = shell.sshug(host)
        rt = shell.exec("ls -l ${config.settings.dataDir}", host);
        if (rt.code) {
            def pathBuffer = new StringBuilder();
            config.settings.dataDir.split("/").each { p ->
                pathBuffer.append("/").append(p)
                rt = shell.exec("ls -l ${pathBuffer.toString()}", host);
                if (rt.code) {
                    rt = shell.exec("sudo mkdir ${pathBuffer.toString()}", host)
                    rt = shell.exec("sudo chown ${ug.u}:${ug.g} ${pathBuffer.toString()}", host)
                }
            }

            logger.info("**** Creating ${config.settings.log4j} for {}", host)
            config.settings.log4j.split("/").each { p ->
                pathBuffer.append("/").append(p)
                rt = shell.exec("ls -l ${pathBuffer.toString()}", host);
                if (rt.code) {
                    rt = shell.exec("sudo mkdir ${pathBuffer.toString()}", host)
                    rt = shell.exec("sudo chown ${ug.u}:${ug.g} ${pathBuffer.toString()}", host)
                }
            }

            logger.info("**** Creating ${config.settings.traceLog} for {}", host)
            config.settings.traceLog.split("/").each { p ->
                pathBuffer.append("/").append(p)
                rt = shell.exec("ls -l ${pathBuffer.toString()}", host);
                if (rt.code) {
                    rt = shell.exec("sudo mkdir ${pathBuffer.toString()}", host)
                    rt = shell.exec("sudo chown ${ug.u}:${ug.g} ${pathBuffer.toString()}", host)
                }
            }
        }

        logger.info("**** Creating ${config.settings.dataDir}/myid for {}", host)
        rt = shell.exec("ls -l ${config.settings.dataDir}/myid", host);
        if (rt.code) {
            rt = shell.exec("echo ${config.settings.server.indexOf(host) + 1} > ${config.settings.dataDir}/myid", host)
            if (!rt.code) {
                rt = shell.exec("sudo chown ${ug.u}:${ug.g} ${config.settings.dataDir}/myid", host)
                rt = shell.exec("stat -c '%n %U %G %y' ${config.settings.dataDir}/myid", host)
            }
        }


    } else {
        logger.error "${host} is not in the server list"
    }
}

if (!args) {
    logger.info("make sure your settings are correct and then run the command : cfg or deploy {zookeeper.tar} {host} ")
} else {
    if ("cfg".equalsIgnoreCase(args[0])) {
        cfg(args.length > 1 ? true:false)
        logger.info("********************************************************************************************")
        logger.info("**** 1: Download zookeeper                                                              ****")
        logger.info("**** 2: Unzip zookeeper and create zoo.cfg from clipboard                               ****")
        logger.info("**** 3: Change log4j.properties zookeeper.log.dir to ${config.settings.log4j}           ****")
        logger.info("**** 4: Change log4j.properties zookeeper.tracelog.dir to ${config.settings.traceLog}   ****")
        logger.info("**** 5: Tar zookeeper                                                                   ****")
        logger.info("**** 6: Deploy zookeeper by zkInit.groovy deploy {zookeeper.tar}{host}                  ****")
        logger.info("********************************************************************************************")
    } else if ("deploy".equalsIgnoreCase(args[0])) {
        deploy(args[1], args[2])
    }
}
