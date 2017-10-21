#! /usr/bin/env groovy


import java.text.SimpleDateFormat;

if (!args || args.length != 2) {
    println "Please input appid: eg xlyUserRetain.groovy filename appid"
    return -1;
}

def scriptDir = new File(getClass().protectionDomain.codeSource.location.path).parent
GroovyShell shell = new GroovyShell()
def plainText = shell.parse(new File(scriptDir, "core/PlainText.groovy"))
def xls = shell.parse(new File(scriptDir, "core/xls.groovy"))

def file = new File(args[0].trim());
def appID = args[1].trim();


def process = { File f, String... filters ->
    plainText.split(f, filters);
}




def dateFormat = "yyyy-MM-dd HH:mm:ss";
def sdf = new SimpleDateFormat(dateFormat)

def xlsClosure = {rowIndex,colIndex, value ->
    def rtValue = value;
    def rtFormat = null;
    if (rowIndex > 0 && colIndex == 0) {
        def date = new Date(Long.valueOf(value));
        rtValue = sdf.format(date);
        rtStyle = dateFormat;
    }
    [
            value: rtValue,
            format: rtFormat
    ]
}

if (file.exists()) {
    def dataMap = new HashMap();
    def dataList = new ArrayList();
    if (file.isDirectory()) {
        file.eachFileRecurse {
            dataList.addAll(plainText.split(it, appID));
        }
    } else {
        dataList.addAll(plainText.split(it, appID));
    }
    def headerList = ["Date","SDK Version","App ID","UID","Account ID","Platform","Channel","AccountType","Gender","Age","Game Server","Resolution",
    "OS","Brand","Net Type","Country","Province","Carrier","Extend1","Extend2","Extend3","Extend4","Extend5"]
    dataList.add(0, headerList)
    dataMap.put("UserInfo", dataList)
    xls.generateXls(dataMap, xlsClosure)
}

