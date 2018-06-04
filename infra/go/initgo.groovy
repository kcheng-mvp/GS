#! /usr/bin/env groovy


@Grapes([
        @Grab(group = 'com.google.guava', module = 'guava', version = '18.0')
])
import com.google.common.base.CaseFormat

def currentPath = new File(getClass().protectionDomain.codeSource.location.path).parent
GroovyShell groovyShell = new GroovyShell()
def shell = groovyShell.parse(new File(currentPath, "../../core/Shell.groovy"))

if(!args || args.length !=1){
    println "Create go project with command : gogo.groovy {projectName}"
    return -1
}

def projectName = new File(args[0]);
if(projectName.exists()){
    println "${args[0]} already exists ... "
    return -1
}
def ws = new File("${projectName}_ws")
ws.mkdir()
new File(ws , "src/${projectName}").mkdirs()


def env = new File(ws, "gopath.sh");
env << "#!/bin/bash\n"
env << "export GOPATH=\$PWD\n"
env << "export PATH=\$PATH:\$GOPATH/bin"
shell.exec("chmod +x ${env.absolutePath}")


println "please run 'source ./gopath.sh' ..."


