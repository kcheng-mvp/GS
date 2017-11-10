#! /usr/bin/env groovy
import groovy.time.TimeCategory
import java.text.SimpleDateFormat

def currentPath = new File(getClass().protectionDomain.codeSource.location.path).parent
GroovyShell groovyShell = new GroovyShell()
def shell = groovyShell.parse(new File(currentPath, "core/Shell.groovy"))

//hadoop jar CRMR.jar 2017/11/01 /advdata/crmr/2017/11/01 /advdata/eventadv/2017/11/01/*/input,/advdata/userregister/2017/11/01/*/input,advdata/userregister/2017/11/02/*/input,/advdata/userregister/2017/11/03/*/input
//def command = "hadoop jar"
//shell.exec(command)
//hadoop jar CCMR.jar /advdata/eventadv/2017/11/01/*/input /advdata/ccmr/2017/11/01

assert args.length ==2 : "Please input jar and date. eg : hello.jar 2017/11/01"
def jar = new File(args[0])
assert jar.exists() : "Can not find the jar ${args[0]}"
def sdf = new SimpleDateFormat("yyyy/MM/dd")
def dataDay = Date.parse("yyyy/MM/dd",args[1])

def command = null;
if(jar.name.equals("CRMR.jar")){
    use(TimeCategory){
        def regDay1 = sdf.format(dataDay + 1.days)
        def regDay2 = sdf.format(dataDay + 2.days)
        command = "hadoop jar ${args[0]} ${args[1]} /advdata/crmr/${args[1]} /advdata/eventadv/${args[1]}/*/input," +
                "/advdata/userregister/${args[1]}/*/input,/advdata/userregister/${regDay1}/*/input,/advdata/userregister/${regDay2}/*/input"
        shell.exec(command)
    }
} else {
    command = "hadoop jar ${args[0]} /advdata/eventadv/${args[1]}/*/input /advdata/ccmr/${args[1]}"
    shell.exec(command)
}

