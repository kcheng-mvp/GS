#! /usr/bin/env groovy

import groovy.transform.Field
import java.text.SimpleDateFormat
import groovy.io.FileType
import groovy.xml.MarkupBuilder

/*
1: hostname
2: add new user and add user to the group wheel(manual)
3: adjust sshd(manual)
2: /etc/hosts
3: max open files and max users process
*/

@Field
def currentPath = new File(getClass().protectionDomain.codeSource.location.path).parent
@Field
def groovyShell = new GroovyShell()
@Field
def shell = groovyShell.parse(new File(currentPath, "../../core/Shell.groovy"))
@Field
def logback = groovyShell.parse(new File(currentPath, "../../core/Logback.groovy"))
@Field
def logger = logback.getLogger("infra.os")


@Field
def home = System.getProperty("user.home")

def etcHost(hosts) {
    logger.info("******************** Start building os ********************")
    hosts.sort()
    def ips = [] as List
    new File("${home}/.ssh/config").eachLine { line ->
        if (line.trim().startsWith("HostName")) {
            ips.add(line.split()[1].trim())
        }
    }
    def hostMap = [:];
    def rt = null
    def con = hosts.find { host ->
        logger.info("** Checking hosts for {}", host)
        rt = shell.exec("hostname", host)
        def remoteName = rt.msg.get(0)
        if (!host.equals(remoteName)) {
            logger.error(">> local host name ${host},remote host name ${remoteName} please fix it")
            logger.info("** sudo hostnamectl")
            logger.info("** sudo hostnamectl set-hostname '{hostname}'")
            return true
        }
        rt = shell.exec("hostname -I", host)
        def hostIps = new ArrayList()
        //10.0.0.0,172.16.0.0,192.168.0.0
        hostIps.addAll(rt.msg.get(0).split().findAll { it -> it.startsWith("10") || it.startsWith("172") || it.startsWith("192") })
//        hostIps.addAll(rt.msg.get(0).split())
        if (hostIps.size() != 1) {
            logger.error "** Runs into error when try to get host's IP address:${hostIps.toString()} ..."
            return -1
        }
        hostMap.put(hostIps[0], host.trim())


        logger.info("** Checking ssh key for {}", host)
        rt = shell.exec("ls ~/.ssh/id_rsa", host)
        if (rt.code) {
            logger.info("** Generate ssh key for [@${host}]")
            rt = shell.exec("ssh-keygen -b 4096 -q -N '' -C '${host}' -f ~/.ssh/id_rsa", host)
        }

        logger.info("** Checking max open files for {}", host)
        rt = shell.exec("cat /etc/security/limits.conf", host)
        rt.msg.each { msg ->
            if (msg.startsWith("*")) {
                logger.info("** /etc/security/limits.conf@[${host}]: ${msg}")
            }
        }

        logger.info("** Checking max processes for {}", host)
        rt = shell.exec("ls /etc/security/limits.d", host)
        def proc = rt.msg[0]
        rt = shell.exec("cat /etc/security/limits.d/${proc}", host)
        rt.msg.each { msg ->
            if (msg && !msg.startsWith("#")) {
                logger.info("** /etc/security/limits.d/${proc}@[${host}]:${msg}")
            }
        }
        return false
    }
    if (con) {
        logger.error "There are errors in host /etc/hosts"
        return
    }
    // sort the map
    hostMap.sort({ a, b -> a.value <=> b.value })
    hosts.each { h ->
        logger.info("** Setting hosts for {}", h)
        File file = File.createTempFile(h, ".etchosts");
        file.deleteOnExit();
        rt = shell.exec("cat /etc/hosts", h)
        file.withWriter { writer ->
            def w = new BufferedWriter(writer);
            rt.msg.each { m ->
                if (m.trim()) {
                    def entries = m.split()
                    if (entries.size() != 2) {
                        w.write(m)
                    } else {
                        if (!entries[1].trim().equals(hostMap.get(entries[0].trim()))) {
                            w.write(m)
                            if (hostMap.get(entries[0].trim()))
                                logger.error("@${h}:There are multiple mapping for ip ${entries[0].trim()}, please fix it ...")
                        }

                    }
                    w.newLine()
                }
            }
            hostMap.each { k, v ->
                w.write("${k} ${v}")
                w.newLine()
            }
            w.close()
        }
        rt = shell.exec("sudo mv /etc/hosts /etc/hosts.back", h)
        file.eachLine { line ->
            if (line.trim()) {
                shell.exec("echo ${line} | sudo tee -a /etc/hosts >/dev/null", h)
            }
        }
    }
}


def deploy(deployable, host, homeVar) {

    logger.info("** Deploy ${deployable} on {} ......", host)
    def targetFolder = deployable.name.replace(".tar", "")
    def rt = shell.exec("ls -l /usr/local/${targetFolder}", host)
    if (rt.code) {
        def sdf = new SimpleDateFormat("yyyyMMddHHmmss")
        logger.info "** scp ${deployable.absolutePath} ${host} (This may take minutes) ......"
        def targetName = "${targetFolder}.${sdf.format(Calendar.getInstance().getTime())}.tar"
        rt = shell.exec("scp ${deployable.absolutePath} ${host}:~/${targetName}");
        logger.info "** unzip the file to target folder ..."
        rt = shell.exec("sudo tar -vxf  ~/${targetName} --no-same-owner -C /usr/local", host)
        rt = shell.sshug(host)
        rt = shell.exec("sudo chown -R ${rt.u}:${rt.g} /usr/local/${targetFolder}", host)
    } else {
        logger.error "** Folder /usr/local/${targetFolder} already exists on ${host}, please delete it first"
        return -1
    }

    if (rt.code) {
        rt.msg.each {
            logger.error it
        }
        return -1
    }

    logger.info("** Create ${homeVar} environment variable on {} ......", host)
    rt = shell.exec("cat ~/.bash_profile", host)
    def exists = rt.msg.any { v -> v.indexOf("export ${homeVar}") > -1 }
    if (exists) {
        logger.error(" ** Variable ${homeVar} has been definied ...")
        return -1
    } else {
        rt = shell.exec("echo '' >> ~/.bash_profile", host)
        rt = shell.exec("echo 'export ${homeVar}=/usr/local/${targetFolder}' >> ~/.bash_profile", host)
        rt = shell.exec("echo 'export PATH=\$${homeVar}/bin:\$PATH' >> ~/.bash_profile", host)
    }
    return 1
}

def mkdirs(host, dirs) {
    logger.info "** Create corresponding folders on ${host} "
    def ug = shell.sshug(host)
    def group = ug.g
    def user = ug.u
    dirs.each { dir ->
        def rt = shell.exec("ls -l ${dir}", host);
        if (rt.code) {
            logger.info("** [${host}]: Creating folder: ${dir} ...... ")
            def pathEle = new StringBuffer()
            dir.split(File.separator).each { ele ->
                if (ele) {
                    pathEle.append(File.separator).append(ele)
                    rt = shell.exec("ls -l ${pathEle.toString()}", host)
                    if (rt.code) {
                        rt = shell.exec("sudo mkdir ${pathEle.toString()}", host)
                        rt = shell.exec("sudo chown ${user}:${group} ${pathEle.toString()}", host)
                    }
                }
            }
        } else {
            logger.warn "** Folder ${dir} exits on ${host}, ignore "
        }
    }

}

// utils

def findBound(configFile, hosts) {
    assert hosts.size() >= 2
    def config1 = new ConfigSlurper().with {
        it.setBinding(host: hosts[0])
        it.parse(configFile.text)
    }.flatten()
    def config2 = new ConfigSlurper().with {
        it.setBinding(host: hosts[1])
        it.parse(configFile.text)
    }.flatten()
    def result = config1.collectEntries { k, v ->
        v.equals(config2[k]) ? [:] : ["${k}": 1]
    }.keySet()[0]
    return result ? result.split("[.]")[1] : ""
}

def generateCfg(config, dir) {
    def settings = new File(dir)
    settings.mkdir()
    config.keySet().each { key ->
        if (key.trim() && !"settings".equals(key)) {
            dir = new File(settings, key).with { it.mkdirs(); it }
            config."${key}".keySet().each { f ->
                logger.info "** Generate ${f}"
                new File(dir, f).withWriter { w ->
                    if (f.endsWith(".xml")) {
                        def xml = new MarkupBuilder(w)
                        xml.mkp.xmlDeclaration([version: '1.0', encoding: 'UTF-8'])
                        xml.mkp.pi("xml-stylesheet": [type: "text/xsl", href: "configuration.xsl"])
                        xml.configuration {
                            config."${key}".get(f).flatten().each { entry ->
                                property {
                                    name(entry.key)
                                    value(entry.value)
                                }
                            }
                        }
                        w.close()
                    } else {
                        def shellCommand = f.endsWith(".sh") ? "export " :""
                        def bw = new BufferedWriter(w)
                        def var = config."${key}".get(f).flatten()
                        var.each { item ->
                            if (var instanceof List) {
                                bw.write(item)
                            } else {

                                bw.write("${shellCommand}${item.key}=${item.value}")
                            }
                            bw.newLine()
                        }
                        bw.close()
                    }
                }
            }
        }
    }
}

def consolidate(deployable, configDir, host = null,Closure closure=null) {

    def settings = new File(configDir)
    if (!settings.exists() || !settings.isDirectory() || settings.list().length < 1) {
        logger.error("** Can not find the folder ${configDir} or it's empty folder")
        return null
    }

    deployable = new File(deployable)
    if (!deployable.exists()) logger.error "Can't find the deployable ${deployable}"
//    def rootName = deployable.name.replace(".tar", "").replace(".gz", "").replaceAll(".tgz", "");
//    logger.info "** Root file name is ${rootName}"
    def tmpDir = File.createTempDir()
    logger.info("** Unzip ${deployable.absolutePath} to ${tmpDir.absolutePath}")
    rt = shell.exec("tar -vxf ${deployable.absolutePath} -C ${tmpDir.absolutePath}")
    rootName = tmpDir.listFiles()[0].name
    logger.info "** Root file name is ${rootName}"
    if(closure) closure(new File(tmpDir,rootName))

    logger.info("** Update configurations")
    def sdf = new SimpleDateFormat("yyyyMMddHHmm")
    // for the case settings vary with host, in this the configuration file will be with the pattern -${host}
    def pattern = ~/-\(.*\)/
    if (!rt.code) {
        settings.eachFileRecurse(FileType.FILES) { f ->
            if (!(f.name =~ pattern) || (host && f.name =~ pattern && f.name.indexOf(host) > -1)) {
                def target = f.name.replaceAll(pattern, "")
                target = new File("${tmpDir.absolutePath}/${rootName}/${f.getParentFile().getName()}/${target}")
                if (target.exists()) {
                    def backup = new File("${tmpDir.absolutePath}/${rootName}/${f.getParentFile().getName()}/${target.name}.${sdf.format(Calendar.getInstance().getTime())}").with {
                        it << target.text
                        it
                    }
                    def keyMap = [:]
                    f.eachLine { line ->
                        def entries = line.split("=")
                        keyMap.put(entries[0].trim(), entries.length > 1 ? entries[1].trim() : "")
                    }
                    target.withWriter { w ->
                        def bw = new BufferedWriter(w)
                        backup.eachLine { line ->
                            def item = keyMap.find { k, v ->
                                def ll = line.replaceAll("\\s", "")
                                ll.startsWith("${k}=") || ll.startsWith("#${k}=")
                            }

                            bw.write(item ? "${item.key}=${item.value}" : line)
                            bw.newLine()
                            if (item) keyMap.remove(item.key)
                        }
                        keyMap.each { item ->
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
    if (rt.code) {
        rt.msg.each {
            logger.error it
        }
        return null
    } else {
        new File("${tmpDir.absolutePath}/${rootName}.tar")
    }
}
