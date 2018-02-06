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
def configFile = new File('zookeeperCfg.groovy')
def config = null
if (configFile.exists()) {
    config = new ConfigSlurper().parse(configFile.text)
}



def buildOs = { onRemote ->
    osBuilder.etcHost(config.setting.hosts, onRemote)
}

def deploy = { deployable, host ->
    if (config.setting.hosts.contains(host)) {

        deployable = new File(deployable)
        if (!deployable.exists()) logger.error "Can't find the deployable ${deployable}"
        def rootName = deployable.name.replace(".tar", "").replace(".gz", "");
        def tmpDir = File.createTempDir()

        def rt = shell.exec("tar -vxf ${deployable.absolutePath} -C ${tmpDir.absolutePath}")


        logger.info("** Building zoo.cfg")

        def generated = new File("zookeeper").with { d ->
            if (!d.exists()) d.mkdirs()
            d
        }

        def file = new File("${tmpDir.absolutePath}/${rootName}/conf/zoo.cfg")
        file.withWriter { w ->
            def bw = new BufferedWriter(w)
            config.setting.zooCfg.flatten().each { entry ->
                bw << "${entry.key}=${entry.value}"
                bw.newLine()
            }
            config.setting.hosts.eachWithIndex { s, idx ->
                bw << "server.${idx + 1}=${s}:${config.setting.serverPort}:${config.setting.leaderPort}"
                bw.newLine()
            }
            bw.close()
        }
        new File(generated, "zoo.cfg").withWriter { w ->
            w << file.text
        }

        logger.info "** Generate conf/zookeeper-env.sh"
        file = new File("${tmpDir.absolutePath}/${rootName}/conf/zookeeper-env.sh")
        file.withWriter { w ->
            def bw = new BufferedWriter(w)
            config.setting.zkenv.flatten().each {
                bw.write("${it.key}=${it.value}")
                bw.newLine()
            }
            bw.close()
        }
        new File(generated, "zookeeper-en.sh").withWriter { w ->
            w << file.text
        }



        logger.info "** Set log4j.properties"
        file = new File("${tmpDir.absolutePath}/${rootName}/conf/log4j.properties")
        file.withWriterAppend { w ->
            config.setting.log4j.flatten().each {
                w << ("${it.key}=${it.value}")
            }
        }
        new File(generated, "log4j.properties").withWriter { w ->
            w << file.text
        }



        rt = shell.exec("tar -cvzf  ${tmpDir.absolutePath}/${rootName}.tar -C ${tmpDir.absolutePath} ./${rootName}")

        rt = osBuilder.deploy(new File("${tmpDir.absolutePath}/${rootName}.tar"), host, "zkCli.sh", "ZK_HOME")

        tmpDir.deleteDir()
        if (rt != 1) {
            logger.error "** Failed to deploy ${deployable} on ${host}"
            return -1
        }



        def dirs = config.flatten().findAll { it -> it.key.toUpperCase().indexOf("DIR") > -1 }.collect { it.value }

        def ug = shell.sshug(host)
        logger.info "** Creating dirs : ${dirs}"
        dirs.each { dir ->
            rt = shell.exec("ls -l ${dir}", host);
            if (rt.code) {
                def pathBuffer = new StringBuilder();
                dir.split("/").each { p ->
                    pathBuffer.append("/").append(p)
                    rt = shell.exec("ls -l ${pathBuffer.toString()}", host);
                    if (rt.code) {
                        rt = shell.exec("sudo mkdir ${pathBuffer.toString()}", host)
                        rt = shell.exec("sudo chown ${ug.u}:${ug.g} ${pathBuffer.toString()}", host)
                    }
                }
            } else {
                logger.warn "** ${dir} exits, ignore ..."
            }
        }

        logger.info("** Creating ${config.setting.zooCfg.dataDir}/myid for {}", host)
        rt = shell.exec("ls -l ${config.setting.zooCfg.dataDir}/myid", host);
        if (rt.code) {
            rt = shell.exec("echo ${config.setting.hosts.indexOf(host) + 1} > ${config.setting.zooCfg.dataDir}/myid", host)
            if (!rt.code) {
                rt = shell.exec("sudo chown ${ug.u}:${ug.g} ${config.setting.zooCfg.dataDir}/myid", host)
                rt = shell.exec("stat -c '%n %U %G %y' ${config.setting.zooCfg.dataDir}/myid", host)
            }
        }


    } else {
        logger.error "${host} is not in the server list : ${config.setting.hosts.toString()}"
    }
}

if (!args) {
    logger.info("** make sure your settings are correct and then run the command : build or deploy {zookeeper.tar} {host} ")
} else {
    if ("init".equalsIgnoreCase(args[0])) {
        new File("zookeeperCfg.groovy").withWriter { w ->
            w << new File(currentPath, "zookeeperCfg.groovy").text
        }
        logger.info "** Please do the changes according to your environments in zookeeperCfg.groovy"

    } else {
        if (!configFile.exists()) {
            logger.error "Can't find ${configFile.absolutePath}, please init project first"
            return -1
        }
        if ("build".equalsIgnoreCase(args[0])) {
            buildOs(args.length > 1 ? true : false)
        } else if ("deploy".equalsIgnoreCase(args[0])) {
            deploy(args[1], args[2])
        }
    }
}
