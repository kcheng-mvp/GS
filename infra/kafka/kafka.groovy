#! /usr/bin/env groovy
import groovy.json.StringEscapeUtils
import groovy.text.*

def currentPath = new File(getClass().protectionDomain.codeSource.location.path).parent
GroovyShell groovyShell = new GroovyShell()
def shell = groovyShell.parse(new File(currentPath, "../../core/Shell.groovy"))
def logback = groovyShell.parse(new File(currentPath, "../../core/Logback.groovy"))
//def clipboard = groovyShell.parse(new File(currentPath, "../../core/Clipboard.groovy"))
def osBuilder = groovyShell.parse(new File(currentPath, "../os/osBuilder.groovy"))

def logger = logback.getLogger("infra-ka")
def configFile = new File('kafkaCfg.groovy')
def config = null
if (configFile.exists()) {
    config = new ConfigSlurper().parse(configFile.text)
}


def buildOs = { onRemote ->

    def hosts = new ArrayList();
    hosts.addAll(config.setting.ka.hosts)
    hosts.addAll(config.setting.zk.hosts)

    osBuilder.etcHost(hosts, onRemote)

}

def deploy = { deployable, host ->
    if (config.setting.ka.hosts.contains(host)) {

        logger.info("** unzip ${deployable.absolutePath}")

        def rootName = deployable.name.replace(".tar", "").replace(".gz", "").replace(".tgz", "");
        def tmpDir = File.createTempDir()
        tmpDir.deleteOnExit()
        rt = shell.exec("tar -vxf ${deployable.absolutePath} -C ${tmpDir.absolutePath}")
        if (!rt.code) {
            logger.info "** Process config/server.properties ......";
            def settings = config.setting.server.flatten()
            def original = new File("${tmpDir.absolutePath}/${rootName}/config/server.properties")
            def bak = new File("${tmpDir.absolutePath}/${rootName}/config/server.properties.bak")
            bak << original.bytes
            original.withWriter { w ->
                def bw = new BufferedWriter(w)
                bak.eachLine { line ->
                    def find = settings.find { it ->
                        return line.startsWith("#${it.key}=") || line.startsWith("${it.key}=")
                    }.collect { it -> "${it.key}=${it.value}" }
                    if (find) {
                        bw << "#${line}"
                        bw.newLine()
                        bw << find.first()
                    } else if (line.startsWith("broker.id=")) {
                        bw << "broker.id=${config.setting.ka.hosts.indexOf(host)}"
                    } else {
                        bw << line
                    }

                    bw.newLine()
                }
                bw.close()
            }
            logger.info "** Process config/log4j.properties ......";
            settings = config.setting.log4j.flatten()
            original = new File("${tmpDir.absolutePath}/${rootName}/config/log4j.properties")
            bak = new File("${tmpDir.absolutePath}/${rootName}/config/log4j.properties.bak")
            bak << original.bytes

            def processed = false
            original.withWriter { w ->
                def bw = new BufferedWriter(w)
                bak.eachLine { line ->
                    if (!processed && !line.startsWith('#')) {
                        settings.each { it ->
                            bw << "${it.key}=${it.value}"
                            bw.newLine()
                        }
                        processed = true
                    }
                    bw << line
                    bw.newLine()
                }
                bw.close()
            }

            logger.info "** Process config/producer.properties ......";
            settings = config.setting.producer.flatten()
            original = new File("${tmpDir.absolutePath}/${rootName}/config/producer.properties")
            bak = new File("${tmpDir.absolutePath}/${rootName}/config/producer.properties.bak")
            bak << original.bytes
            original.withWriter { w ->
                def bw = new BufferedWriter(w)
                bak.eachLine { line ->
                    def find = settings.find { it ->
                        return line.startsWith("#${it.key}=") || line.startsWith("${it.key}=")
                    }.collect { it -> "${it.key}=${it.value}" }
                    if (find) {
                        bw << "#${line}"
                        bw.newLine()
                        bw << find.first()
                    } else {
                        bw << line
                    }
                    bw.newLine()
                }
                bw.close()
            }
            logger.info "** Process config/consumer.properties ......";
            settings = config.setting.consumer.flatten()
            original = new File("${tmpDir.absolutePath}/${rootName}/config/consumer.properties")
            bak = new File("${tmpDir.absolutePath}/${rootName}/config/consumer.properties.bak")
            bak << original.bytes
            //bootstrap.servers=
            original.withWriter { w ->
                def bw = new BufferedWriter(w)
                bak.eachLine { line ->
                    def find = settings.find { it ->
                        return line.startsWith("#${it.key}=") || line.startsWith("${it.key}=")
                    }.collect { it -> "${it.key}=${it.value}" }
                    if (find) {
                        bw << "#${line}"
                        bw.newLine()
                        bw << find.first()
                    } else {
                        bw << line
                    }
                    bw.newLine()
                }
                bw.close()
            }

        }
        rt = shell.exec("tar -cvzf  ${tmpDir.absolutePath}/${rootName}.tar -C ${tmpDir.absolutePath} ./${rootName}")

        rt = osBuilder.deploy(new File("${tmpDir.absolutePath}/${rootName}.tar"), host, "kafka-configs.sh", "KA_HOME")

        if (rt != 1) {
            logger.error "** Failed to deploy ${deployable} on ${host}"
            return -1
        }
        def dirs = config.flatten().findAll { it -> it.key.indexOf("dir") > -1 }.collect { it.value }
        logger.info "** Creating dirs : ${dirs}"
        def ug = shell.sshug(host)
        dirs.each { dir ->
            def rt = shell.exec("ls -l ${dir}", host);
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
            }
        }

    } else {
        logger.error "${host} is not in the server list: ${config.setting.ka.hosts.toString()}"
    }
}

if (!args) {
    logger.info("** make sure your settings are correct and then run the command : build or deploy {zookeeper.tar} {host} ")
} else {
    if("init".equalsIgnoreCase(args[0])){
        new File("kafkaCfg.groovy").withWriter { w ->
            w << new File(currentPath, "kafkaCfg.groovy").text
        }
        logger.info "** Please do the changes according to your environments in kafkaCfg.groovy "
    } else {
        if(!configFile.exists()){
            logger.error "** Can't find the file ${configFile.absolutePath}, please init project first"
            return -1
        }
        if ("build".equalsIgnoreCase(args[0])) {
            buildOs(args.length > 1 ? true : false)
        } else if ("deploy".equalsIgnoreCase(args[0])) {
            def deployable = new File(args[1])
            if (!deployable.exists()) {
                logger.error "** Can't not find the file : {}", deployable.absolutePath
                return -1
            }
            deploy(deployable, args[2])
        }
    }
}

