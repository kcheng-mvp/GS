#! /usr/bin/env groovy
@Grapes([
        @Grab(group = 'com.google.guava', module = 'guava', version = '18.0')
])
import com.google.common.base.CaseFormat
import groovy.io.FileType
import groovy.xml.MarkupBuilder

def currentPath = new File(getClass().protectionDomain.codeSource.location.path).parent
GroovyShell groovyShell = new GroovyShell()
def shell = groovyShell.parse(new File(currentPath, "../../core/Shell.groovy"))
def logback = groovyShell.parse(new File(currentPath, "../../core/Logback.groovy"))
def osBuilder = groovyShell.parse(new File(currentPath, "../os/osBuilder.groovy"))
def logger = logback.getLogger("infra-hd")

def CONFIG_FOLDER = "hadoop"
def DEPLOYABLE_HOME = "HADOOP_HOME"
def CONFIG_FILE_NAME = "hadoopCfg.groovy"

def cfg = { config ->
    osBuilder.generateCfg(config, CONFIG_FOLDER)
}


def buildOs = { config ->
    logger.info("** Check and set /etc/hosts for all servers ...")
    osBuilder.etcHost(config.settings.hosts)
}

def mkdir = { config, host ->
    if (config.settings.hosts.contains(host)) {
        def dirs = config.flatten().findAll {
            it -> it.key.toUpperCase().indexOf("DIR") > -1 && it.key.indexOf("mapred.system.dir") < 0 &&
                    it.key.indexOf("mapreduce.jobtracker.staging.root.dir") < 0 &&
                    it.key.indexOf("dataDirs") < 0
        }.collect(new HashSet<>()) {
            it.value.split(",")
        }.flatten()
        osBuilder.mkdirs(host, dirs)
    } else {
        logger.error "${host} is not in the server list: ${config.settings.hosts.toString()}"
    }
}


def deploy = { config, deployable, host ->
    if (config.settings.hosts.contains(host)) {
        def consolidated = osBuilder.consolidate(deployable, CONFIG_FOLDER, host, { dir ->
            ["conf/masters", "conf/slaves", "conf/core-site.xml", "conf/hdfs-site.xml", "conf/mapred-site.xml"].each { f ->
                def file = new File(dir, f)
                if (file.exists()) {
                    logger.info("** Delete ${file.absolutePath}")
                    file.delete()
                }
            }

        })
        if (consolidated) {
            def rt = osBuilder.deploy(consolidated, host, DEPLOYABLE_HOME)
            if (rt < 0) {
                logger.error "** Failed to deploy ${deployable} on host ${host}"
                return -1
            }
            def dirs = config.flatten().findAll {
                it -> it.key.toUpperCase().indexOf("DIR") > -1 && it.key.indexOf("mapred.system.dir") < 0 &&
                        it.key.indexOf("mapreduce.jobtracker.staging.root.dir") < 0 &&
                        it.key.indexOf("dataDirs") < 0
            }.collect(new HashSet<>()) {
                it.value.split(",")
            }.flatten()
            osBuilder.mkdirs(host, dirs)
        }
    } else {
        logger.error "${host} is not in the server list: ${config.settings.hosts.toString()}"
    }
}



if (!args) {
    logger.info("** Available commands : init, cfg, build, mkdir and deploy")
    logger.info("** dfs health page http://namoenode:50070/dfshealth.jsp")
} else {
    if ("init".equalsIgnoreCase(args[0])) {
        new File(CONFIG_FILE_NAME).withWriter { w ->
            versionCfg = args.length > 1 ? "hadoopCfg${args[1].toUpperCase()}.groovy" : CONFIG_FILE_NAME
            f = new File(currentPath, versionCfg)
            if (f.exists()){
                w << new File(currentPath, versionCfg).text
            } else {
                logger.error "Can't find the file ${f.absolutePath}"
            }
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
        } else if ("mkdir".equalsIgnoreCase(args[0])) {
            mkdir(config, args[1])
        }
        else if ("deploy".equalsIgnoreCase(args[0])) {
            deploy(config, args[1], args[2])
        }
    }
}
