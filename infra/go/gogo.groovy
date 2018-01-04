#! /usr/bin/env groovy

def currentPath = new File(getClass().protectionDomain.codeSource.location.path).parent
GroovyShell groovyShell = new GroovyShell()
def shell = groovyShell.parse(new File(currentPath, "../../core/Shell.groovy"))

if(!args || args.length !=1){
    println "Create go project with command : gogo.groovy {projectName}"
    return -1
}

def gopath = new File(args[0]);
if(gopath.exists()){
    println "${args[0]} already exists ... "
    return -1
}

def src = new File(gopath, "src")
src.mkdirs()


def env = new File(gopath, "env.sh");
env << "#!/bin/bash\n"
env << "export GOPATH=\$PWD\n"
env << "export PATH=\$PATH:\$GOPATH/bin"
shell.exec("chmod +x ${env.absolutePath}")
shell.exec("${env.absolutePath}")


