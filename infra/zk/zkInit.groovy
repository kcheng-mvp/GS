#! /usr/bin/env groovy

import groovy.text.*

def currentPath = new File(getClass().protectionDomain.codeSource.location.path).parent
GroovyShell groovyShell = new GroovyShell()
def cron4j = groovyShell.parse(new File(currentPath, "../../core/Cron4J.groovy"))
def shell = groovyShell.parse(new File(currentPath, "../../core/Shell.groovy"))
def logback = groovyShell.parse(new File(currentPath, "../../core/Logback.groovy"))
def configFile = new File(currentPath, 'zkInitCfg.groovy')
def config = new ConfigSlurper().parse(configFile.text)

def logger = logback.getLogger("infra-zk")


println "*****************"
println "\n"

println "tickTime=${config.settings.tickTime}"
println "initLimit=${config.settings.initLimit}"
println "syncLimit=${config.settings.syncLimit}"
println "clientPort=${config.settings.clientPort}"
println "dataDir=${config.settings.dataDir}"
config.settings.server.eachWithIndex {s, idx ->
    println "server.${idx+1}=${s}:${config.settings.serverPort}:${config.settings.leaderPort}"
}
