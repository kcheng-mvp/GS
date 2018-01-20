#! /usr/bin/env groovy

import groovy.text.*

def currentPath = new File(getClass().protectionDomain.codeSource.location.path).parent
GroovyShell groovyShell = new GroovyShell()
def shell = groovyShell.parse(new File(currentPath, "../../core/Shell.groovy"))
def logback = groovyShell.parse(new File(currentPath, "../../core/Logback.groovy"))
def clipboard = groovyShell.parse(new File(currentPath, "../../core/Clipboard.groovy"))
def osBuilder = groovyShell.parse(new File(currentPath, "../os/osBuilder.groovy"))
def configFile = new File(currentPath, 'zkInitCfg.groovy')
def config = new ConfigSlurper().parse(configFile.text)

def logger = logback.getLogger("infra-zk")

def cfg= {

    osBuilder.etcHost(config.settings.server)


    logger.info("******************** Start building zoo.cfg(clipboard) ********************")
    def sb = new StringBuilder();
    sb.append("tickTime=${config.settings.tickTime}\n")
    sb.append("initLimit=${config.settings.initLimit}\n")
    sb.append("syncLimit=${config.settings.syncLimit}\n")
    sb.append("clientPort=${config.settings.clientPort}\n")
    sb.append("dataDir=${config.settings.dataDir}\n")
    config.settings.server.eachWithIndex { s, idx ->
        sb.append("server.${idx + 1}=${s}:${config.settings.serverPort}:${config.settings.leaderPort}\n")
    }
    clipboard.copy(sb.toString())

    logger.info("******************** Start creating ${config.settings.dataDir} ********************")
    config.settings.server.eachWithIndex { s, idx ->
        logger.info("**** Creating ${config.settings.dataDir} for {}",s)
        def ug = shell.sshug(s)
        def rt = shell.exec("ls -l ${config.settings.dataDir}", s);
        if (rt.code) {
            def pathBuffer = new StringBuilder();
            config.settings.dataDir.split("/").each { p ->
                pathBuffer.append("/").append(p)
                rt = shell.exec("ls -l ${pathBuffer.toString()}", s);
                if (rt.code) {
                    rt = shell.exec("sudo mkdir ${pathBuffer.toString()}", s)
                    rt = shell.exec("sudo chown ${ug.u}:${ug.g} ${pathBuffer.toString()}", s)
                }
            }
        }

        logger.info("**** Creating ${config.settings.dataDir}/myid for {}",s)
        rt = shell.exec("ls -l ${config.settings.dataDir}/myid", s);
        if (rt.code) {
            rt = shell.exec("echo ${idx+1} > ${config.settings.dataDir}/myid",s)
            if (!rt.code) {
                rt = shell.exec("sudo chown ${ug.u}:${ug.g} ${config.settings.dataDir}/myid", s)
                rt = shell.exec("stat -c '%n %U %G %y' ${config.settings.dataDir}/myid", s)
            }
        }

    }

}

def deploy = {host, deployable ->
    if (config.settings.server.contains(host)){

        logger.info("**** Deploy ${deployable.name} on {}",host)
        def targetFolder = new File(deployable).getName().replace(".tar","")
        def rt = shell.exec("ls -l /usr/local/${targetFolder}",host)
        if(rt.code){
            rt = shell.exec("sudo tar -vxf  ${deployable} --no-same-owner -C /usr/local", host);
        }

        rt = shell.exec("type zkCli.sh", host);
        if(rt.code){
            logger.info("**** Create ZK_HOME environment on {}",host)
            rt = shell.exec("cat ~/.bash_profile", host)
            File file = File.createTempFile(host, ".bash_profile");
            file.deleteOnExit();
            file.withWriter { write ->
                def w = new BufferedWriter(write);
                rt.msg.eachWithIndex { m, idx ->
                    if (idx + 1 == rt.msg.size) {
                        w.newLine();
                        w.write("export ZK_HOME=/usr/local/${targetFolder}")
                        w.newLine();
                        def sec = m.split("=");
                        w.write("${sec[0]}=\$ZK_HOME/bin:${sec[1]}")
                    } else {
                        w.newLine()
                        w.write(m)
                    }
                }
                w.close()
            }
            logger.info file.absolutePath
            rt = shell.exec("mv ~/.bash_profile ~/.bash_profile.bak", host)
            rt = shell.exec("scp ${file.absolutePath} ${host}:~/.bash_profile")
            rt = shell.exec("cat ~/.bash_profile", host)
            rt.msg.each{
               logger.info it
            }
        }

    } else {
        logger.error "${host} is not in the server list"
    }
}

if (!args) {
    logger.info("make sure your settings are correct and then run the command : cfg or deploy {zookeeper.tar} {host} ")
} else {
    if ("cfg".equalsIgnoreCase(args[0])) {
        cfg()
        logger.info("****************************************************************************")
        logger.info("**** 1: Download zookeeper                                              ****")
        logger.info("**** 2: Unzip zookeeper and create zoo.cfg from clipboard               ****")
        logger.info("**** 3: Tar zookeeper                                                   ****")
        logger.info("**** 4: Deploy zookeeper by zkInit.groovy deploy {zookeeper.tar}{host}  ****")
        logger.info("****************************************************************************")
    } else if ("deploy".equalsIgnoreCase(args[0])) {
        deploy(args[1],args[2])
    }
}
