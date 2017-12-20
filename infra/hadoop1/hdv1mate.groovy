#! /usr/bin/env groovy

import groovy.text.*

def currentPath = new File(getClass().protectionDomain.codeSource.location.path).parent
GroovyShell groovyShell = new GroovyShell()
def cron4j = groovyShell.parse(new File(currentPath, "../../core/Cron4J.groovy"))
def shell = groovyShell.parse(new File(currentPath, "../../core/Shell.groovy"))
def logback = groovyShell.parse(new File(currentPath, "../../core/Logback.groovy"))
def configFile = new File(currentPath, 'hdv1mateCfg.groovy')
def config = new ConfigSlurper().parse(configFile.text)

def logger = logback.getLogger("infra-hadoop1")

logger.info("********************")
config.flatten().each { k, v ->
    logger.info("${k} : ${v}")
}
logger.info("********************")

def tmpDir = new File(System.getProperty("java.io.tmpdir"));

def cfg = {

    def generate = new File(tmpDir, "hdfs-v1")
    if (generate.exists()) {
        generate.deleteDir()
    }
    generate.mkdirs()

    def stringEngine = new GStringTemplateEngine()
    logger.info("Generate hadoop-env.sh ...")
    def tempEnv = new File(currentPath, "temp-env.sh")
    def envString = stringEngine.createTemplate(tempEnv).make(config).toString()
    def template = new File(currentPath, "hadoop-env.sh")
    def env = new File(generate, "hadoop-env.sh")
    env << template.text
    env.append(envString)



    logger.info("Generate core-site.xml ...")
    def core = new File(currentPath, "core-site.xml")
    def coreString = stringEngine.createTemplate(core).make(config).toString()
    def coreSite = new File(generate, "core-site.xml")
    coreSite << coreString


    logger.info("Generate hdfs-site.xml ...")
    def hdfs = new File(currentPath, "hdfs-site.xml")
    def hdfsString = stringEngine.createTemplate(hdfs).make(config).toString()
    def hdfsSite = new File(generate, "hdfs-site.xml")
    hdfsSite << hdfsString


    logger.info("Generate mapred-site.xml ...")
    def mapred = new File(currentPath, "mapred-site.xml")
    def mapredString = stringEngine.createTemplate(mapred).make(config).toString()
    def mapredSite = new File(generate, "mapred-site.xml")
    mapredSite << mapredString

    logger.info("Generate masters & slaves ...")
    def masters = new File(generate, "masters")
    masters << config.hadoopenv.secondNode << "\n"
    def slaves = new File(generate, "slaves")
    slaves.withWriter { w ->
        w.write(config.hadoopenv.secondNode)
        w.write("\n")
        config.hadoopenv.dataNode.each {
            w.write(it)
            w.write("\n")
        }
    }


    logger.info("Generate folder list ...")

    def folder = new File(generate, "folder").withWriter { w ->
        config.hadoopenv.paths.each { rootPath ->
            w.write(rootPath)
            w.write("\n")
        }
        config.flatten().each { k, v ->
            if (k.indexOf("dir") > -1) {
                w.write(v)
                w.write("\n")
            }
        }
    }

    logger.info("Configurations are generated at {}", generate.absolutePath)
}


def apply = { flag ->
    def generate = new File(tmpDir, "hdfs-v1")
    logger.info("${"0".equalsIgnoreCase(flag) ? 'Check' : 'Create'} folders based on the configurations ${generate.absolutePath} ...")
    def folder = new File(generate, "folder")
    if (!generate.exists() || !folder.exists()) {
        logger.error("No configurations are found, please run 'cfg' first !")
        return -1
    }
    if (System.currentTimeMillis() - generate.lastModified() > 1000 * 60 * 30) {
        logger.error("Configurations are generated 30 minutes ago, please re-generate it")
        return -1
    }


    def hosts = config.hadoopenv.dataNode << config.hadoopenv.masterNode << config.hadoopenv.secondNode

    logger.info("There are total ${hosts.size()} nodes : ${hosts};" +
            " master node is ${config.hadoopenv.masterNode}, second node is ${config.hadoopenv.secondNode}")


    logger.info("******************* ${"0".equalsIgnoreCase(flag) ? 'Check' : 'Create'} folders *******************")
    hosts.each { host ->
        def rt = null
        def exits = true
        rt = shell.exec("id -u -n", host)
        def user = rt.msg[0]
        rt = shell.exec("id -g -n", host)
        def group = rt.msg[0]
        logger.info("All the folders will be created as user : ${user} and group :${group}")
        folder.each { f ->
            if (f) {
                f = f.replaceAll(",", " ")
                rt = shell.exec("ls ${f}", host)
                if("0".equalsIgnoreCase(flag) && rt.code){
                    exits = false
                    rt.msg.each {msg ->
                        logger.info("[check][@${host}]:${msg}")
                    }
                }
                if ("1".equalsIgnoreCase(flag) && rt.code) {
                    rt = shell.exec("sudo mkdir -p ${f}", host)
                    rt.msg.each { msg ->
                        logger.info("[@${host}]:${msg}")
                    }
                    rt = shell.exec("sudo chown ${user}:${group} ${f}", host)
                }
            }
        }
        if("0".equalsIgnoreCase(flag) && exits){
            logger.info("All the folders exits on the [@${host}] ...")
        }
    }


}

if (!args) {
    logger.info("make sure your settings are correct and then run the command : cfg or apply")
} else {
    if ("cfg".equalsIgnoreCase(args[0])) {
        cfg()
    } else if ("apply".equalsIgnoreCase(args[0])) {
        def flag = args.length == 2 ? args[1]:"0";
        apply(flag)
    }
}

