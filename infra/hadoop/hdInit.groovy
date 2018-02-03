#! /usr/bin/env groovy
import groovy.io.FileType
@Grapes([
        @Grab(group = 'com.google.guava', module = 'guava', version = '18.0')
])
import groovy.text.*
import groovy.xml.*
import groovy.xml.MarkupBuilder
import com.google.common.base.CaseFormat

def currentPath = new File(getClass().protectionDomain.codeSource.location.path).parent
GroovyShell groovyShell = new GroovyShell()
def shell = groovyShell.parse(new File(currentPath, "../../core/Shell.groovy"))
def logback = groovyShell.parse(new File(currentPath, "../../core/Logback.groovy"))
def osBuilder = groovyShell.parse(new File(currentPath, "../os/osBuilder.groovy"))
def logger = logback.getLogger("infra-hd")

def configFile = new File('hdInitCfg.groovy')
def config = null
def hosts = null
if (configFile.exists()) {
    config = new ConfigSlurper().parse(configFile.text)
    hosts = config.setting.dataNode << config.setting.masterNode << config.setting.secondNode
}



def buildOs = { onRemote ->
    logger.info("** Check and set /etc/hosts for all servers ...")
    osBuilder.etcHost(hosts, onRemote)
}


def deploy = { deployable, host ->

    if (hosts.contains(host)) {

        def tmpDir = File.createTempDir()

        logger.info("** Generate configurations ...")
        def generate = new File(tmpDir,"hdfs")


        logger.info("** Generate core-site.xml ...")

        ["coreSite", "hdfsSite", "mapredSite"].each { prop ->

            def fileName = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, prop)

            logger.info "** Generate ${fileName}.xml ..."
            def file = new File(generate, "${fileName}.xml");
            def writer = new FileWriter(file)
            def xml = new MarkupBuilder(writer)

            xml.mkp.xmlDeclaration([version: '1.0', encoding: 'UTF-8'])
            xml.mkp.pi("xml-stylesheet": [type: "text/xsl", href: "configuration.xsl"])

            xml.configuration {
                config.get(prop).flatten().each { sec ->
                    property {
                        name(sec.key)
                        value(sec.value)
                    }

                }
            }
            writer.close()
        }



        logger.info("** Generate masters & slaves ...")
        def masters = new File(generate, "masters")
        masters << config.setting.secondNode << "\n"
        def slaves = new File(generate, "slaves")
        slaves.withWriter { w ->
            w.write(config.setting.masterNode)
            w.write("\n")
            w.write(config.setting.secondNode)
            w.write("\n")
            config.setting.dataNode.each {
                w.write(it)
                w.write("\n")
            }
        }

        logger.info("** Generate folder list ...")

        new File(generate, "folder").withWriter { w ->
            config.flatten().each { k, v ->
                if (k.indexOf("dir") > -1 || k.indexOf("DIR") > -1) {
                    w.write(v)
                    w.write("\n")
                }
            }
            config.setting.dataVols.each { rootPath ->
                w.write(rootPath)
                w.write("\n")
            }
        }

        logger.info("** Configurations are generated at {}", generate.absolutePath)


        def rootName = deployable.name.replace(".tar", "").replace(".gz", "").replace(".tgz", "");
        logger.info("** unzipping ${deployable.absolutePath} at ${tmpDir.absolutePath} ......")
        def rt = shell.exec("tar -vxf ${deployable.absolutePath} -C ${tmpDir.absolutePath}")
        if (!rt.code) {
            generate.eachFileRecurse(FileType.FILES) { f ->
                if (!f.name.equalsIgnoreCase("folder")) {
                    logger.info("** copy ${f.absolutePath}......")
                    shell.exec("cp ${f.absolutePath} ${tmpDir.absolutePath}/${rootName}/conf")
                }
            }
        }

        logger.info("** Generate hadoop-env.sh ......")
        def hadoopenv = new File("${tmpDir.absolutePath}/${rootName}/conf/hadoop-env.sh")
        config.hadoopenv.flatten().each { entry ->
            logger.info "** Add ${entry.key}=${entry.value}"
            hadoopenv.append("${System.getProperty("line.separator")}${entry.key}=${entry.value}")
        }


        logger.info("** Re-generate ${rootName}.tar ......")
        rt = shell.exec("tar -cvzf  ${tmpDir.absolutePath}/${rootName}.tar -C ${tmpDir.absolutePath} ./${rootName}")

        logger.info("** Deploy ${rootName}.tar ......")

        rt = osBuilder.deploy(new File("${tmpDir.absolutePath}/${rootName}.tar"), host, "hadoop", "HADOOP_HOME");
        if (rt != 1) {
            logger.error "Failed to deploy ${deployable} on ${host}"
            tmpDir.deleteDir()
            return -1
        }


        logger.info "** Create corresponding folders on ${host} ...."

        def ug = shell.sshug(host)
        def group = ug.g
        def user = ug.u
        new File(generate, "folder").eachLine { f ->
            if (f) {
                f = f.replaceAll(",", " ")
                f.split().each { p ->
                    def pathEle = new StringBuffer()
                    p.split(File.separator).each { ele ->
                        if (ele) {
                            pathEle.append(File.separator).append(ele)
                            rt = shell.exec("ls -l ${pathEle.toString()}", host)
                            if (rt.code) {
                                logger.info("**[@${host}]: Creating folder: ${pathEle.toString()} ... ")
                                rt = shell.exec("sudo mkdir ${pathEle.toString()}", host)
                                rt = shell.exec("sudo chown ${user}:${group} ${pathEle.toString()}", host)
                                logger.info("**[@${host}]: Changing owner: ${pathEle.toString()}")
                            }

                        }
                    }
                }
            }
        }
        tmpDir.deleteDir()
    }
}

if (!args) {
    logger.info("make sure your settings are correct and then run the command : [init], [build], [cfg] or [deploy]")
} else {
    if ("init".equalsIgnoreCase(args[0])) {
        def configuration = new File("hdInitCfg.groovy")
        configuration << new File(currentPath, "hdInitCfg.groovy").bytes
        logger.info "** Please do the changes according to your environments in ${configuration.absolutePath}"
    } else {
        if (!hosts) {
            logger.error "** hosts is null, please run init first ......"
            return -1
        }
        if ('build'.equalsIgnoreCase(args[0])) {
            buildOs(args.length > 1 ? true : false)
        } else if ("deploy".equalsIgnoreCase(args[0]) && args.length == 3) {
            def deployable = new File(args[1])
            if (!deployable.exists()) {
                logger.error "Can't find the file ${deployable.absolutePath} ......"
                return -1
            }
            deploy(deployable, args[2])
        } else {
            logger.error("Can not find the command ${args[0]} ...")
        }
    }
}

