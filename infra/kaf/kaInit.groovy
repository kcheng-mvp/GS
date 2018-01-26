#! /usr/bin/env groovy

import groovy.text.*

def currentPath = new File(getClass().protectionDomain.codeSource.location.path).parent
GroovyShell groovyShell = new GroovyShell()
def shell = groovyShell.parse(new File(currentPath, "../../core/Shell.groovy"))
def logback = groovyShell.parse(new File(currentPath, "../../core/Logback.groovy"))
def clipboard = groovyShell.parse(new File(currentPath, "../../core/Clipboard.groovy"))
def osBuilder = groovyShell.parse(new File(currentPath, "../os/osBuilder.groovy"))
def configFile = new File(currentPath, 'kaInitCfg.groovy')
def config = new ConfigSlurper().parse(configFile.text)

def logger = logback.getLogger("infra-ka")


def cfg= {



    def hosts = new ArrayList();
    hosts.addAll(config.settings.ka.server)
    hosts.addAll(config.settings.zk.server)

    osBuilder.etcHost(hosts)

    logger.info("******************** Start creating ${config.settings.ka.log.dirs} ********************")
    config.settings.ka.server.eachWithIndex { s, idx ->
        logger.info("**** Creating ${config.settings.ka.log.dirs} for {}",s)
        def ug = shell.sshug(s)
        def rt = shell.exec("ls -l ${config.settings.ka.log.dirs}", s);
        if (rt.code) {
            def pathBuffer = new StringBuilder();
            config.settings.ka.log.dirs.split("/").each { p ->
                pathBuffer.append("/").append(p)
                rt = shell.exec("ls -l ${pathBuffer.toString()}", s);
                if (rt.code) {
                    rt = shell.exec("sudo mkdir ${pathBuffer.toString()}", s)
                    rt = shell.exec("sudo chown ${ug.u}:${ug.g} ${pathBuffer.toString()}", s)
                }
            }
        }
    }
    def sb = new StringBuilder("zookeeper.connect=")
    config.settings.zk.server.eachWithIndex{server,index ->
        sb.append("${server}:${config.settings.zk.clientPort}")
        if(index +1 < config.settings.zk.server.size()){
           sb.append(",")
        } else {
            sb.append("/kafka")
        }
    }
    sb.append("\n")
    sb.append("metadata.broker.list=")
    config.settings.ka.server.eachWithIndex{server,index ->
        sb.append("${server}:${config.settings.ka.port}")
        if(index +1 < config.settings.ka.server.size()){
           sb.append(",")
        }
    }
    clipboard.copy(sb.toString())



}

def deploy = {host, deployable ->
    if (config.settings.ka.server(host)){

        deployable = new File(deployable)
        if(!deployable.exists()) logger.error "Can't find the deployable ${deployable}"
        def rt = osBuilder.deploy(host,deployable,"kafka-server-start.sh","KA_HOME")
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
        logger.info("****************************************************************************")
        logger.info("**** 1: Download kafa                                                   ****")
        logger.info("**** 2: Unzip kafka                                                     ****")
        logger.info("**** 3: Tar kafka                                                       ****")
        logger.info("**** 4: Deploy kafka by kaInit.groovy deploy {kafka.tar}{host}          ****")
        logger.info("****************************************************************************")
    } else if ("deploy".equalsIgnoreCase(args[0])) {
        deploy(args[1],args[2])
    }
}
