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



def buildOs = { config ->

    def hosts = new HashSet();
    hosts.addAll(config.setting.ka.hosts)
    hosts.addAll(config.setting.zk.hosts)

    osBuilder.etcHost(hosts)

}

def cfg = { configFile ->

    def configDir = new File("kafka")
    configDir.mkdirs()
    def config = new ConfigSlurper().parse(configFile.text)
    def foundSection = osBuilder.findBound(configFile, config.settings.ka.hosts)

    logger.info("** Generate configuration ......")
    config.keySet().each { dir ->
        if (!"settings".equals(dir)) {
            def currentDir = new File(configDir, dir).with { it.mkdirs() ;it }
            config.get(dir).keySet().each { fileName ->
                def once = !fileName.equals(foundSection)
                config.settings.ka.hosts.find { host ->
                    def boundConfig = new ConfigSlurper().with {
                        it.setBinding(host: host)
                        it.parse(configFile.text)
                    }
                    def targetName = once ? "${fileName}.properties" : "${fileName}-(${host}).properties"
                    new File(currentDir, targetName).withWriter { w ->
                        def bw = new BufferedWriter(w)
                        boundConfig."${dir}".get(fileName).flatten().each { k, v ->
                            bw.write("${k}=${v}")
                            bw.newLine()
                        }
                        bw.close()
                    }
                    return once
                }

            }
        }
    }

}

def deploy = { config, deployable, host ->
    if (config.settings.ka.hosts.contains(host)) {

        def configDir = new File("kafka")
        if (!configDir.exists() || !configDir.isDirectory() || configDir.list().length < 1) {
            logger.error("** Can not find the folder kafka or it's empty folder")
            return -1
        }



        def rootName = deployable.name.replace(".tar", "").replace(".gz", "").replace(".tgz", "");
        def tmpDir = File.createTempDir()
        logger.info("** unzip ${deployable.absolutePath} to ${tmpDir.absolutePath}")

        rt = shell.exec("tar -vxf ${deployable.absolutePath} -C ${tmpDir.absolutePath}")




        def sdf = new SimpleDateFormat("yyyyMMddHHmm")
        def pattern = ~/-\(.*\)/
        if (!rt.code) {
            configDir.eachFileRecurse(FileType.FILES) {  f  ->
                if(!(f.name =~ pattern) || (f.name =~ pattern && f.name.indexOf(host) > -1)){
                    def target = f.name.replaceAll(pattern,"")
                    target  = new File("${tmpDir.absolutePath}/${rootName}/${f.getParentFile().getName()}/${target}")
                    if(target.exists()){
                        def backup = new File("${tmpDir.absolutePath}/${rootName}/${f.getParentFile().getName()}/${target.name}.${sdf.format(Calendar.getInstance().getTime())}").with{
                            it << target.text
                            it
                        }
                        def keyMap = [:]
                        f.eachLine {line ->
                            def entries = line.split("=")
                            keyMap.put(entries[0].trim(),entries.length > 1 ? entries[1].trim() : "")
                        }
                        target.withWriter {w ->
                            def bw = new BufferedWriter(w)
                            backup.eachLine {line ->
                                def item = keyMap.find {k,v ->
                                    def ll = line.replaceAll("\\s","")
                                    ll.startsWith("${k}=")  || ll.startsWith("#${k}=")
                                }

                                bw.write(item ? "${item.key}=${item.value}" : line)
                                bw.newLine()
                                if(item) keyMap.remove(item.key)
                            }
                            keyMap.each{ item ->
                                bw.write("${item.key}=${item.value}")
                                bw.newLine()
                            }
                            bw.close()
                        }
                    } else {
                        shell.exec("cp ${f.absolutePath} ${tmpDir.absolutePath}/${rootName}/${f.getParentFile().getName()}")
                    }
                }
            }
        }

        logger.info("** Re-generate ${rootName}.tar ")
        rt = shell.exec("tar -cvzf  ${tmpDir.absolutePath}/${rootName}.tar -C ${tmpDir.absolutePath} ./${rootName}")

        rt = osBuilder.deploy(new File("${tmpDir.absolutePath}/${rootName}.tar"), host, "KA_HOME")

        tmpDir.deleteDir()
        if (rt != 1) {
            logger.error "** Failed to deploy ${deployable} on ${host}"
            return -1
        }
        def dirs = config.flatten().findAll { it -> it.key.toUpperCase().indexOf("DIR") > -1 }.collect { it.value }

        osBuilder.mkdirs(host, dirs)
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


