#! /usr/bin/env groovy
@Grapes([
        @Grab(group = 'com.google.guava', module = 'guava', version = '18.0')
])
import com.google.common.base.CaseFormat

import groovy.io.FileType
import java.text.SimpleDateFormat

def currentPath = new File(getClass().protectionDomain.codeSource.location.path).parent
GroovyShell groovyShell = new GroovyShell()
def shell = groovyShell.parse(new File(currentPath, "../../core/Shell.groovy"))
def logback = groovyShell.parse(new File(currentPath, "../../core/Logback.groovy"))
def osBuilder = groovyShell.parse(new File(currentPath, "../os/osBuilder.groovy"))

def logger = logback.getLogger("infra-ka")


def CONFIG_FOLDER = "kafka"
def DEPLOYABLE_HOME = "KAFKA_HOME"
def CONFIG_FILE_NAME = "kafkaCfg.groovy"

def cfg = { config ->
    osBuilder.generateCfg(config, CONFIG_FOLDER)
}



def buildOs = { config ->

    def hosts = new HashSet();
    hosts.addAll(config.settings.ka.hosts)
    hosts.addAll(config.settings.zk.hosts)

    osBuilder.etcHost(hosts)

}

def deploy = { config, deployable, host ->
    if (config.settings.ka.hosts.contains(host)) {
        def consolidated = osBuilder.consolidate(deployable, CONFIG_FOLDER,host)
        if (consolidated) {
            def rt = osBuilder.deploy(consolidated, host, DEPLOYABLE_HOME)
            if (rt < 0) {
                logger.error "** Failed to deploy ${deployable} on host ${host}"
                return -1
            }
            def dirs = config.flatten().findAll { it -> it.key.toUpperCase().indexOf("DIR") > -1 }.collect(new HashSet<>()) { it.value.split(",") }.flatten()
            osBuilder.mkdirs(host, dirs)
        }
    } else {
        logger.error "${host} is not in the server list: ${config.setting.ka.hosts.toString()}"
    }
}

if (!args) {
    logger.info("** Available commands : init, cfg, build(os,Optional) and deploy")
} else {
    if ("init".equalsIgnoreCase(args[0])) {
        new File(CONFIG_FILE_NAME).withWriter { w ->
            w << new File(currentPath, CONFIG_FILE_NAME).text
        }
        logger.info "** Please do the changes according to your environments in ${CONFIG_FILE_NAME}"

    } else {
        def configFile = new File(CONFIG_FILE_NAME)
        if (!configFile.exists()) {
            logger.error "** Can't find file ${CONFIG_FILE_NAME} in current folder ......"
            return -1
        }
        def config = new ConfigSlurper().parse(configFile.text)
        if ("cfg".equalsIgnoreCase(args[0])) {
            cfg(config)
        } else if ("build".equalsIgnoreCase(args[0])) {
            buildOs(config)
        } else if ("deploy".equalsIgnoreCase(args[0])) {
            deploy(config, args[1], args[2])
        }
    }
}
