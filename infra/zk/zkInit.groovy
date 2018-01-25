#! /usr/bin/env groovy

import groovy.text.*

def currentPath = new File(getClass().protectionDomain.codeSource.location.path).parent
GroovyShell groovyShell = new GroovyShell()
def shell = groovyShell.parse(new File(currentPath, "../../core/Shell.groovy"))
def logback = groovyShell.parse(new File(currentPath, "../../core/Logback.groovy"))
def clipboard = groovyShell.parse(new File(currentPath, "../../core/Clipboard.groovy"))
def osBuilder = groovyShell.parse(new File(currentPath, "../os/osBuilder.groovy"))
def configFile = new File(currentPath, 'zkInitCfg.groovy')
def config = new ConfigSlurper().parse(configFile.text)

def logger = logback.getLogger("infra-zk")

def cfg= {

    osBuilder.etcHost(config.settings.server)


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

    logger.info("******************** Start creating ${config.settings.dataDir} ********************")
    config.settings.server.eachWithIndex { s, idx ->
        logger.info("**** Creating ${config.settings.dataDir} for {}",s)
        def ug = shell.sshug(s)
        def rt = shell.exec("ls -l ${config.settings.dataDir}", s);
        if (rt.code) {
            def pathBuffer = new StringBuilder();
            config.settings.dataDir.split("/").each { p ->
                pathBuffer.append("/").append(p)
                rt = shell.exec("ls -l ${pathBuffer.toString()}", s);
                if (rt.code) {
                    rt = shell.exec("sudo mkdir ${pathBuffer.toString()}", s)
                    rt = shell.exec("sudo chown ${ug.u}:${ug.g} ${pathBuffer.toString()}", s)
                }
            }

            logger.info("**** Creating ${config.settings.log4j} for {}",s)
            config.settings.log4j.split("/").each { p ->
                pathBuffer.append("/").append(p)
                rt = shell.exec("ls -l ${pathBuffer.toString()}", s);
                if (rt.code) {
                    rt = shell.exec("sudo mkdir ${pathBuffer.toString()}", s)
                    rt = shell.exec("sudo chown ${ug.u}:${ug.g} ${pathBuffer.toString()}", s)
                }
            }

            logger.info("**** Creating ${config.settings.traceLog} for {}",s)
            config.settings.traceLog.split("/").each { p ->
                pathBuffer.append("/").append(p)
                rt = shell.exec("ls -l ${pathBuffer.toString()}", s);
                if (rt.code) {
                    rt = shell.exec("sudo mkdir ${pathBuffer.toString()}", s)
                    rt = shell.exec("sudo chown ${ug.u}:${ug.g} ${pathBuffer.toString()}", s)
                }
            }
        }

        logger.info("**** Creating ${config.settings.dataDir}/myid for {}",s)
        rt = shell.exec("ls -l ${config.settings.dataDir}/myid", s);
        if (rt.code) {
            rt = shell.exec("echo ${idx+1} > ${config.settings.dataDir}/myid",s)
            if (!rt.code) {
                rt = shell.exec("sudo chown ${ug.u}:${ug.g} ${config.settings.dataDir}/myid", s)
                rt = shell.exec("stat -c '%n %U %G %y' ${config.settings.dataDir}/myid", s)
            }
        }


    }

}

def deploy = {host, deployable ->
    if (config.settings.server.contains(host)){

        deployable = new File(deployable)
        if(!deployable.exists()) logger.error "Can't find the deployable ${deployable}"
        def rt = osBuilder.deploy(host,deployable,"zkCli.sh","ZK_HOME")
        if(rt != 1){
            logger.error "Failed to deploy ${deployable} on ${host}"
            return -1
        }
    } else {
        logger.error "${host} is not in the server list"
    }
}

if (!args) {
    logger.info("make sure your settings are correct and then run the command : cfg or deploy {zookeeper.tar} {host} ")
} else {
    if ("cfg".equalsIgnoreCase(args[0])) {
        cfg()
        logger.info("********************************************************************************************")
        logger.info("**** 1: Download zookeeper                                                              ****")
        logger.info("**** 2: Unzip zookeeper and create zoo.cfg from clipboard                               ****")
        logger.info("**** 3: Change log4j.properties zookeeper.log.dir to ${config.settings.log4j}           ****")
        logger.info("**** 4: Change log4j.properties zookeeper.tracelog.dir to ${config.settings.traceLog}   ****")
        logger.info("**** 5: Tar zookeeper                                                                   ****")
        logger.info("**** 6: Deploy zookeeper by zkInit.groovy deploy {zookeeper.tar}{host}                  ****")
        logger.info("********************************************************************************************")
    } else if ("deploy".equalsIgnoreCase(args[0])) {
        deploy(args[1],args[2])
    }
}
