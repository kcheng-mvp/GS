#! /usr/bin/env groovy
@Grapes([
        @Grab(group = 'com.google.guava', module = 'guava', version = '18.0')
])
import com.google.common.base.CaseFormat
@Grapes([
        @Grab(group = 'com.google.guava', module = 'guava', version = '18.0')
])
import com.google.common.base.CaseFormat

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

    def hosts = new HashSet();
    hosts.addAll(config.setting.ka.hosts)
    hosts.addAll(config.setting.zk.hosts)

    osBuilder.etcHost(hosts, onRemote)

}

def deploy = { deployable, host ->
    if (config.setting.ka.hosts.contains(host)) {


        config = new ConfigSlurper().with { it ->
            it.setBinding(currentHost: host)
            it.parse(configFile.text)
        }

        logger.info("** unzip ${deployable.absolutePath}")

        def rootName = deployable.name.replace(".tar", "").replace(".gz", "").replace(".tgz", "");
        def tmpDir = File.createTempDir()
        rt = shell.exec("tar -vxf ${deployable.absolutePath} -C ${tmpDir.absolutePath}")

        logger.info("** Generate configurations ...")
        def found = config.setting.findAll{entry -> entry.key.length() > 2}.collect{it.key.trim()}.find { prop ->
            def fileName = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, prop)
            def original = new File("${tmpDir.absolutePath}/${rootName}/config/${fileName}.properties")
            if(!original.exists()) {
                logger.error "** Can't find the file ${fileName} ..."
                return true
            }

            logger.info "** Generate ${fileName}.properties ..."
            def bak = new File("${tmpDir.absolutePath}/${rootName}/config/${fileName}.properties.bak")
            bak << original.bytes
            def propsMap = config.setting.get(prop).flatten()
            original.withWriter { w ->
                def bw = new BufferedWriter(w)
                bak.eachLine { line ->
                    def find = propsMap.find { entry -> line.startsWith(entry.key) || line.startsWith("#${entry.key}") }
                    if (find) {
                        bw << "#${line}"
                        bw.newLine()
                        bw << "${find.key}=${find.value}"
                        propsMap.remove(find.key)
                    } else {
                        bw << line
                    }
                    bw.newLine()
                }
                propsMap.each { entry ->
                    bw << "${entry.key}=${entry.value}"
                    bw.newLine()
                }
                bw.close()
            }
            new File("kafka/${host}").with{d ->
                if(!d.exists()) d.mkdirs()
                new File(d, "${fileName}.properties").withWriter {w ->
                    w << original.text
                }
            }
            return false
        }
        if(found) return -1

        rt = shell.exec("tar -cvzf  ${tmpDir.absolutePath}/${rootName}.tar -C ${tmpDir.absolutePath} ./${rootName}")

        rt = osBuilder.deploy(new File("${tmpDir.absolutePath}/${rootName}.tar"), host, "kafka-server-start.sh", "KA_HOME")

        tmpDir.deleteDir()
        if (rt != 1) {
            logger.error "** Failed to deploy ${deployable} on ${host}"
            return -1
        }
        def dirs = config.flatten().findAll { it -> it.key.toUpperCase().indexOf("DIR") > -1 }.collect { it.value }
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
    if ("init".equalsIgnoreCase(args[0])) {
        new File("kafkaCfg.groovy").withWriter { w ->
            w << new File(currentPath, "kafkaCfg.groovy").text
        }
        logger.info "** Please do the changes according to your environments in kafkaCfg.groovy "
    } else {
        if (!configFile.exists()) {
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

