#! /usr/bin/env groovy

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


def validate = {
    def configFile = new File('hdInitCfg.groovy')

    if (!configFile.exists()) {
        logger.error "Can not find the ${configFile.absolutePath}, please init first"
        return -1
    }
    def config = new ConfigSlurper().parse(configFile.text)
    def hosts = config.hadoopenv.dataNode << config.hadoopenv.masterNode << config.hadoopenv.secondNode

    [config: config, hosts: hosts]

}



def buildOs = { onRemote ->
    def env = validate()
    logger.info("** Check and set /etc/hosts for all servers ...")
    osBuilder.etcHost(evn.hosts, onRemote)
}


def cfg = { onRemote ->


    def env = validate()
    logger.info("** Generate configurations ...")
    def generate = new File("hdfs")

    if (generate.exists()) {
        generate.deleteDir()
    }
    generate.mkdirs()

    logger.info("** Generate core-site.xml ...")

    ["coreSite","hdfsSite","mapredSite"].each{prop ->

        def fileName = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, prop)

        logger.info "** Generate ${fileName}.xml ..."
        def file = new File(generate,"${fileName}.xml");
        def writer = new FileWriter(file)
        def xml = new MarkupBuilder(writer)

        xml.mkp.xmlDeclaration([version: '1.0', encoding: 'UTF-8'])
        xml.mkp.pi("xml-stylesheet": [type: "text/xsl", href: "configuration.xsl"])

        xml.configuration {
            env.config.get(prop).flatten().each { sec ->
                property {
                    name(sec.key)
                    value(sec.value)
                }

            }
        }
        writer.close()
    }



    logger.info("** Generate masters & slaves ...")
//    def masters = new File(generate, "masters")
//    masters << config.hadoopenv.secondNode << "\n"
//    def slaves = new File(generate, "slaves")
//    slaves.withWriter { w ->
//        w.write(config.hadoopenv.masterNode)
//        w.write("\n")
//        w.write(config.hadoopenv.secondNode)
//        w.write("\n")
//        config.hadoopenv.dataNode.each {
//            w.write(it)
//            w.write("\n")
//        }
//    }

    logger.info("** Generate folder list ...")

//    new File(generate, "folder").withWriter { w ->
//        config.flatten().each { k, v ->
//            if (k.indexOf("dir") > -1) {
//                w.write(v)
//                w.write("\n")
//            }
//        }
//        config.hadoopenv.dataVols.each { rootPath ->
//            w.write(rootPath)
//            w.write("\n")
//        }
//    }

    logger.info("** Configurations are generated at {}", generate.absolutePath)
}


def deploy = { host, deployable ->

    if (hosts.contains(host)) {
        def folder = new File(generate, "folder")

        if (!generate.exists() || !folder.exists()) {
            logger.error(">> No configurations are found, please run 'cfg' first !")
            return -1
        }
        if (System.currentTimeMillis() - generate.lastModified() > 1000 * 60 * 30) {
            logger.error(">> Configurations are generated 30 minutes ago, please re-generate it")
            return -1
        }


        def rt = osBuilder.deploy(host, deployable, "hadoop", "HADOOP_HOME");

        if (rt != 1) {
            logger.error "Failed to deploy ${deployable} on ${host}"
            return -1
        }


        logger.info "Create corresponding folders ...."

        def ug = shell.sshug(host)
        def group = ug.g
        def user = ug.u
        folder.eachLine { f ->
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
                            /*
                           else {
                               rt = shell.exec("stat  -c '%U' ${pathEle.toString()}", host)
                               logger.info("**[@${host}]: ${pathEle.toString()} exists")
                               rt.msg.each{msg ->
                                   logger.info("***[@${host}] ${msg}")
                               }
                           }
                           */
                        }
                    }
                }
            }
        }
    }
}

if (!args) {
    logger.info("make sure your settings are correct and then run the command : [init], [build], [cfg] or [deploy]")
} else {
    if ("init".equalsIgnoreCase(args[0])) {
        def configuration = new File("hdInitCfg.groovy")
        configuration << new File(currentPath, "hdInitCfg.groovy").bytes
        logger.info "*** Please do the changes according to your environments in ${configuration.absolutePath}"
    } else if ('build'.equalsIgnoreCase(args[0])) {
        buildOs(args.length > 1 ? true : false)
    } else if ("cfg".equalsIgnoreCase(args[0])) {
        cfg()
    } else if ("deploy".equalsIgnoreCase(args[0]) && args.length == 3) {
        deploy(args[1], args[2])
    } else {
        logger.error("Can not find the command ${args[0]} ...")
    }
}

