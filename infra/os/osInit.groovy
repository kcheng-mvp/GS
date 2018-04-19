#! /usr/bin/env groovy

import groovy.text.*
import org.codehaus.groovy.runtime.StringBufferWriter

def currentPath = new File(getClass().protectionDomain.codeSource.location.path).parent
GroovyShell groovyShell = new GroovyShell()
def shell = groovyShell.parse(new File(currentPath, "../../core/Shell.groovy"))
def logback = groovyShell.parse(new File(currentPath, "../../core/Logback.groovy"))
def osBuilder = groovyShell.parse(new File(currentPath, "osBuilder.groovy"))


def logger = logback.getLogger("osInit")
def CONFIG_FILE_NAME = "osBuilderCfg.groovy"

def buildOs = { config ->
    osBuilder.etcHost(config.settings.hosts)
}

if (!args) {
    logger.info("** Available commands : init and build")
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
        if ("build".equalsIgnoreCase(args[0])) {
            buildOs(config)
        }
    }
}
