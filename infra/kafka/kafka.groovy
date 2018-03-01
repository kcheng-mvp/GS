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
def osBuilder = groovyShell.parse(new File(currentPath, "../os/osBuilder.groovy"))

def logger = logback.getLogger("infra-ka")



def buildOs = { config ->

    def hosts = new HashSet();
    hosts.addAll(config.setting.ka.hosts)
    hosts.addAll(config.setting.zk.hosts)

    osBuilder.etcHost(hosts)

}

def cfg = { configFile ->

    def setting = new File("kafka")
    setting.mkdir()
    def config = new ConfigSlurper().parse(configFile.text)

    logger.info("** Generate configuration ......")
    config.ka.hosts.each { host ->
        def boundConfig = new ConfigSlurper().with {
            it.setBinding(currentHost: host)
            it.parse(configFile.text)
        }
        ["server"].find { name ->
            new File(setting, "${name}-${host}.properties").withWriter { w ->
                def bw = new BufferedWriter(w)
                boundConfig.get(name).flatten().each { k, v ->
                    bw.write("${k}=${v}")
                    bw.newLine()
                }
                bw.close()
            }
        }

    }
    ["producer", "consumer", "log4j"].find { name ->
        new File(setting, "${name}.properties").withWriter { w ->
            def bw = new BufferedWriter(w)
            config.get(name).flatten().each { k, v ->
                bw.write("${k}=${v}")
                bw.newLine()
            }
            bw.close()
        }
    }


}

def deploy = { config, deployable, host ->
    if (config.ka.hosts.contains(host)) {

        def settings = new File("kafka")
        if (!settings.exists() || !settings.isDirectory() || settings.list().length < 1) {
            logger.error("** Can not find the folder hadoop or it's empty folder")
            return -1
        }


        logger.info("** unzip ${deployable.absolutePath}")

        def rootName = deployable.name.replace(".tar", "").replace(".gz", "").replace(".tgz", "");
        def tmpDir = File.createTempDir()
        rt = shell.exec("tar -vxf ${deployable.absolutePath} -C ${tmpDir.absolutePath}")

        if (!rt.code) {
            settings.eachFileRecurse(FileType.FILES) { f ->
                if (!f.name.equalsIgnoreCase("folder")) {
                    if (f.name.indexOf("-") < 0 || f.name.indexOf(host) > 0) {
                        def targetName = f.name.replace("-${host}", "")
                        logger.info("** copy ${f.absolutePath}......")
                        shell.exec("cp ${f.absolutePath} ${tmpDir.absolutePath}/${rootName}/conf/${targetName}")
                    }
                }
            }
        }


        logger.info("** Re-generate ${rootName}.tar ......")
        rt = shell.exec("tar -cvzf  ${tmpDir.absolutePath}/${rootName}.tar -C ${tmpDir.absolutePath} ./${rootName}")

        rt = osBuilder.deploy(new File("${tmpDir.absolutePath}/${rootName}.tar"), host, "KA_HOME")

        tmpDir.deleteDir()
        if (rt != 1) {
            logger.error "** Failed to deploy ${deployable} on ${host}"
            return -1
        }
        def dirs = config.flatten().findAll { it -> it.key.toUpperCase().indexOf("DIR") > -1 }.collect { it.value }

        logger.info "** Create corresponding folders on ${host} ...."
        def ug = shell.sshug(host)
        def group = ug.g
        def user = ug.u
        dirs.each { dir ->
            def rt = shell.exec("ls -l ${dir}", host);
            if (rt.code) {
                def pathEle = new StringBuffer()
                dir.split(File.separator).each { ele ->
                    if (ele) {
                        pathEle.append(File.separator).append(ele)
                        rt = shell.exec("ls -l ${pathEle.toString()}", host)
                        if (rt.code) {
                            logger.info("** [${host}]: Creating folder: ${pathEle.toString()} ... ")
                            rt = shell.exec("sudo mkdir ${pathEle.toString()}", host)
                            rt = shell.exec("sudo chown ${user}:${group} ${pathEle.toString()}", host)
                            logger.info("** [${host}]: Changing owner: ${pathEle.toString()}")
                        }

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
        def configFile = new File('kafkaCfg.groovy')
        if (!configFile.exists()) {
            logger.error "** Can't find file kafkaCfg.groovy in current folder ......"
            return -1
        }
        def config = new ConfigSlurper().parse(configFile.text)
        if ("build".equalsIgnoreCase(args[0])) {
            buildOs(config)
        } else if ("cfg".equalsIgnoreCase(args[0])) {
            cfg(configFile)
        } else if ("deploy".equalsIgnoreCase(args[0])) {
            def deployable = new File(args[1])
            if (!deployable.exists()) {
                logger.error "** Can't not find the file : {}", deployable.absolutePath
                return -1
            }
            deploy(config, deployable, args[2])
        }
    }
}

