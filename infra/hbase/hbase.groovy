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
def logger = logback.getLogger("infra-hb")


def CONFIG_FOLDER = "hbase"
def DEPLOYABLE_HOME = "HBASE_HOME"
def CONFIG_FILE_NAME = "hbaseCfg.groovy"

def cfg = { config ->
    osBuilder.generateCfg(config, CONFIG_FOLDER)
}


def buildOs = { config ->
    logger.info("** Check and set /etc/hosts for all servers ...")
    osBuilder.etcHost(config.conf.regionservers)
}

def mkdir = { config, host ->
    logger.info "Region servers : {}", config.conf.regionservers
    logger.info "Hbase masters : {}", config.conf.'backup-masters'
    config.conf.regionservers.addAll(config.conf.'backup-masters')
    logger.info "Hbase nodes : {}", config.conf.regionservers

    if (config.conf.regionservers.contains(host)) {
        def dirs = config.flatten().findAll { it -> it.key.toUpperCase().indexOf("DIR") > -1 && it.key.indexOf("rootdir") < 0 }.collect(new HashSet<>()) {
            it.value.split(",")
        }.flatten()
        osBuilder.mkdirs(host, dirs)
    } else {
        logger.error "${host} is not in the server list: ${config.conf.regionservers.toString()}"
    }

}
def deploy = { config, deployable, host ->
    logger.info "Region servers : {}", config.conf.regionservers
    logger.info "Hbase masters : {}", config.conf.'backup-masters'
    config.conf.regionservers.addAll(config.conf.'backup-masters')
    logger.info "Hbase nodes : {}", config.conf.regionservers

    if (config.conf.regionservers.contains(host)) {
        def consolidated = osBuilder.consolidate(deployable, CONFIG_FOLDER, host, { dir ->
            ["conf/regionservers", "conf/hbase-site.xml"].each { f ->
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
            def dirs = config.flatten().findAll { it -> it.key.toUpperCase().indexOf("DIR") > -1 && it.key.indexOf("rootdir") < 0 }.collect(new HashSet<>()) {
                it.value.split(",")
            }.flatten()
            osBuilder.mkdirs(host, dirs)
            logger.info "Deleting consolidated file ......"
            consolidated.getParentFile().deleteDir()
        }
    } else {
        logger.error "${host} is not in the server list: ${config.conf.regionservers.toString()}"
    }
}

if (!args) {
    logger.info("** Available commands : init, cfg, build, mkdir and deploy")
} else {
    if ("init".equalsIgnoreCase(args[0])) {
//        new File(CONFIG_FILE_NAME).withWriter { w ->
//            w << new File(currentPath, CONFIG_FILE_NAME).text
//        }
//        logger.info "** Please do the changes according to your environments in ${CONFIG_FILE_NAME}"

        new File(CONFIG_FILE_NAME).withWriter { w ->
            versionCfg = args.length > 1 ? "hbaseCfg${args[1].toUpperCase()}.groovy" : CONFIG_FILE_NAME
            f = new File(currentPath, versionCfg)
            if (f.exists()) {
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
            if (args.length < 2) {
                logger.error "execute the command with mkdir host_name"
                return
            }
            mkdir(config, args[1])
        } else if ("deploy".equalsIgnoreCase(args[0])) {
            if (args.length < 3) {
                logger.error "execute the command with deploy tar host"
                return
            }
            deploy(config, args[1], args[2])
        }
    }
}

