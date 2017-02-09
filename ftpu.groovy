#! /usr/bin/env groovy

@Grapes([
        @Grab(group = 'it.sauronsoftware.cron4j', module = 'cron4j', version = '2.2.5'),
        @Grab(group = 'commons-net', module = 'commons-net', version = '3.5'),
        @Grab(group = 'com.google.guava', module = 'guava', version = '21.0'),
        @GrabConfig(systemClassLoader = true),
        @Grab(group = 'com.h2database', module = 'h2', version = '1.3.175')
])


import com.google.common.hash.Hashing;
import com.google.common.io.Files
import it.sauronsoftware.cron4j.Scheduler
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTP


def home = new File(System.getProperty("user.home"))
println home

def apkHome = new File("/home/apks")
def apkBack = new File("/home/apks-back")
def server = "192.168.123.37"
def userName = "abc"
def password = "123"
def path = "/file/samples"




def upload = {

    def ftpClient = new FTPClient()
    ftpClient.connect(server)
    ftpClient.login(userName, password)
    ftpClient.setFileType(FTP.BINARY_FILE_TYPE)
    //ftpClient.setFileTransferMode(FTP.BINARY_FILE_TYPE)

    def count = 0;
    def currentTime = System.currentTimeMillis()
    try {
        apkHome.eachFileMatch(~/.*.apk/) { f ->
            if (currentTime - f.lastModified() > 1000 * 60 * 60) {

                def sha256 = Files.hash(f, Hashing.sha256()).toString();
                count++;
                def bis = new BufferedInputStream(new FileInputStream(f))
                ftpClient.storeFile("${path}/${sha256}_ChinaStore.apk", bis)
                bis.close()
                //f.renameTo(new File(apkBack, f.name));
                f.delete()
                println "${count} -> ${new Date(System.currentTimeMillis())} -> ${path}/${sha256}_ChinaStore.apk ......";
            }
        }
    } catch (Exception e){
        println e
    } finally {
        ftpClient.logout()
        ftpClient.disconnect()
    }

    println "Upload ${count} apks ...."
    count;
}

def scheduler = new Scheduler();
def total = 0;
println "Started ...."
scheduler.schedule("52 */1 * * *", new Runnable() {
    public void run() {
        total = total + upload()
        println "Total ${total} apks uploaded ......"
    }
})
scheduler.start()



