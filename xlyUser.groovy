#! /usr/bin/env groovy



if (!args || args.length != 2) {
    println "Please input appid: eg xlyUserRetain.groovy filename appid"
    return -1;
}

def scriptDir = new File(getClass().protectionDomain.codeSource.location.path).parent
GroovyShell shell = new GroovyShell()
def plainText = shell.parse(new File(scriptDir, "core/PlainText.groovy"))

def file = new File(args[0].trim());
def appID = args[1].trim();


def process = {File f, String... filters ->
    println plainText.split(f, filters);
}

if (file.exists()) {
    if (file.isDirectory()) {
        file.eachFileRecurse {
            process(it, appID);
        }
    } else {
        process(file, appID);
    }
}


