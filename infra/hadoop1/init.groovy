#! /usr/bin/env groovy

import groovy.text.*

def currentPath = new File(getClass().protectionDomain.codeSource.location.path).parent
GroovyShell groovyShell = new GroovyShell()
def cron4j = groovyShell.parse(new File(currentPath, "../../core/Cron4J.groovy"))
def shell = groovyShell.parse(new File(currentPath, "../../core/Shell.groovy"))
def logback = groovyShell.parse(new File(currentPath, "../../core/Logback.groovy"))
def configFile = new File(currentPath, 'initCfg.groovy')
def config = new ConfigSlurper().parse(configFile.text)

def logger = logback.getLogger("infra-hadoop1")

logger.info("********************")
config.flatten().each {k,v ->
   logger.info("${k} : ${v}")
}
logger.info("********************")

def generate = new File(currentPath,"generate")
if(generate.exists()) {
    generate.deleteDir()
}
generate.mkdirs()

def stringEngine = new GStringTemplateEngine()
logger.info("Generate hadoop-env.sh ...")
def hadoopenv = new File(currentPath,"env.sh")
def envString = stringEngine.createTemplate(hadoopenv).make(config).toString()
def template = new File(currentPath,"hadoop-env.sh")
def env = new File(generate,"hadoop-env.sh")
env << template.text
env.append(envString)



logger.info("Generate core-site.xml ...")
def core = new File(currentPath,"core-site.xml")
def coreString = stringEngine.createTemplate(core).make(config).toString()
def coreSite = new File(generate,"core-site.xml")
coreSite << coreString


logger.info("Generate hdfs-site.xml ...")
def hdfs = new File(currentPath,"hdfs-site.xml")
def hdfsString = stringEngine.createTemplate(hdfs).make(config).toString()
def hdfsSite= new File(generate,"hdfs-site.xml")
hdfsSite << hdfsString


logger.info("Generate mapred-site.xml ...")
def mapred = new File(currentPath,"mapred-site.xml")
def mapredString = stringEngine.createTemplate(mapred).make(config).toString()
def mapredSite= new File(generate,"mapred-site.xml")
mapredSite << mapredString

logger.info("Generate masters & slaves ...")
def masters = new File(generate,"masters")
masters << config.nodes.second
def slaves = new File(generate,"slaves")
slaves.withWriter { w ->
    w.write(config.nodes.second)
    w.write("\n")
    config.nodes.data.each {
        w.write(it)
        w.write("\n")
    }
}







