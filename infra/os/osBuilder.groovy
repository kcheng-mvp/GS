#! /usr/bin/env groovy

import groovy.transform.Field

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

def etcHost(hosts, boolean onRemote) {
    logger.info("******************** Start building os ********************")
    def ips = [] as List
    new File("${home}/.ssh/config").eachLine { line ->
        if(line.trim().startsWith("HostName")){
            ips.add(line.split()[1].trim())
        }
    }
    def hostMap = [:];
    def rt = null
    def con = hosts.find { host ->
        logger.info("**** Checking hosts for {}", host)
        rt = shell.exec("hostname", host)
        def remoteName = rt.msg.get(0)
        if (!host.equals(remoteName)) {
            logger.error(">> local host name ${host},remote host name ${remoteName} please fix it")
            logger.info("sudo hostnamectl")
            logger.info("sudo hostnamectl set-hostname '{hostname}'")
            return true
        }
        rt = shell.exec("hostname -i", host)
        def hostIps = new ArrayList()
        hostIps.addAll(rt.msg.get(0).split())
        if(onRemote){
           hostIps.retainAll(ips)
        } else {
           hostIps.removeAll(ips)
        }
        hostMap.put(hostIps[0], host.trim())

        logger.info("**** Checking ssh key for {}", host)
        rt = shell.exec("ls ~/.ssh/id_rsa", host)
        if (rt.code) {
            logger.info("Generate ssh key for [@${host}]")
            rt = shell.exec("ssh-keygen -b 4096 -q -N '' -C '${host}' -f ~/.ssh/id_rsa", host)
        }

        logger.info("**** Checking max open files for {}", host)
        rt = shell.exec("cat /etc/security/limits.conf", host)
        rt.msg.each { msg ->
            if (msg.startsWith("*")) {
                logger.info("/etc/security/limits.conf@[${host}]: ${msg}")
            }
        }

        logger.info("**** Checking max processes for {}", host)
        rt = shell.exec("ls /etc/security/limits.d", host)
        def proc = rt.msg[0]
        rt = shell.exec("cat /etc/security/limits.d/${proc}", host)
        rt.msg.each { msg ->
            if (msg && !msg.startsWith("#")) {
                logger.info("/etc/security/limits.d/${proc}@[${host}]:${msg}")
            }
        }
        return false
    }
    if (con) {
        logger.error "There are errors in host /etc/hosts"
        return
    }
    // sort the map
    hostMap.sort({a,b -> a.value <=> b.value})
    hosts.each { h ->
        logger.info("**** Setting hosts for {}", h)
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
                        if (!hostMap.get(entries[0].trim()).equals(entries[1].trim())) {
                            w.write(m)
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


def deploy(host, deployable, command, homeVar) {

    logger.info("**** Deploy ${deployable} on {}", host)
//    deployable = new File(deployable);
    def targetFolder = deployable.name.replace(".tar", "")
    def rt = shell.exec("ls -l /usr/local/${targetFolder}", host)
    if (rt.code) {
        logger.info "scp ${deployable.absolutePath} ${host} ......(This may take minutes)"
        rt = shell.exec("scp ${deployable.absolutePath} ${host}:~/");
        logger.info "unzip the file to target folder ..."
        rt = shell.exec("sudo tar -vxf  ~/${deployable.name} --no-same-owner -C /usr/local", host);
    }

    if(rt.code) return -1

    rt = shell.exec("type ${command}", host);
    if (rt.code) {
        logger.info("**** Create ${homeVar} environment on {}", host)
        rt = shell.exec("cat ~/.bash_profile", host)
        File file = File.createTempFile(host, ".bash_profile");
        file.deleteOnExit();
        file.withWriter { write ->
            def w = new BufferedWriter(write);
            rt.msg.eachWithIndex { m, idx ->
                if(m.indexOf("export ${homeVar}") > -1)
                    logger.error "Variable ${homeVar} has been definied ..."
                if (idx + 1 == rt.msg.size) {
                    w.newLine();
                    w.write("export ${homeVar}=/usr/local/${targetFolder}")
                    w.newLine();
                    def sec = m.split("=");
                    if(sec.length ==2){
                        w.write("${sec[0]}=\$${homeVar}/bin:${sec[1]}")
                    } else {
                        w.write("export PATH=\$${homeVar}/bin:\$PATH")
                    }
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
        rt.msg.each {
            logger.info it
        }
    }
    return 1
}

