#! /usr/bin/env groovy

@Grapes([
        @Grab(group = 'org.jsoup', module = 'jsoup', version = '1.10.2'),
        @Grab(group = 'it.sauronsoftware.cron4j', module = 'cron4j', version = '2.2.5'),
        @GrabConfig(systemClassLoader = true),
        @Grab(group = 'com.h2database', module = 'h2', version = '1.3.175')
])

//@Grab(group = 'org.codehaus.groovy.modules.http-builder', module = 'http-builder', version = '0.5.2'),
import org.jsoup.Jsoup
import groovyx.net.http.*
import groovy.sql.Sql
import it.sauronsoftware.cron4j.Scheduler

def home = new File(System.getProperty("user.home"));
def dbHome = new File(home, ".h2");
def driver = "org.h2.Driver";
def url = "jdbc:h2:~/.h2/vocabulary";

def vFolder = new File(home, "vocabulary");
if (!vFolder.exists()) vFolder.mkdirs()


def insertSql = "insert into words(spelling,freq,create_time) values (?,?,CURRENT_TIMESTAMP())"
def querySql = "select 1 from words where spelling = ? "
def updateFreq = "update words set freq = freq + ?, update_time = CURRENT_TIMESTAMP() where spelling = ?"
def getWords = "select * from words where downloaded = 0";
def updateDown = "update words set downloaded = ?, download_time = CURRENT_TIMESTAMP() where spelling = ? "

def context = "http://learningenglish.voanews.com/"
def downloadUrl = "https://ssl.gstatic.com/dictionary/static/sounds/de/0"


def scrawle = {
    def sqlCon = Sql.newInstance(url, "sa", "", driver);

    sqlCon.execute('''
	create table if not exists words (
	ID INTEGER IDENTITY,
	spelling varchar(200),
	freq int,
	downloaded int default(0),
	create_time timestamp,
	update_time timestamp,
	download_time timestamp
);
''')

    def doc = Jsoup.connect("http://learningenglish.voanews.com/p/5609.html").get()

    def links = doc.select("a[href]");
    def levels = ["Level One", "Level Two", "Level Three"] as List

    def articles = new HashSet();
    links.each {
        if (levels.contains(it.ownText())) {
            def link = "${context}${it.attr('href')}"

            doc = Jsoup.connect(link).get()
            def aLinks = doc.select("a[href]");
            aLinks.each { ai ->
                if (ai.attr("href").contains("/a/")) {
                    articles.add(ai.attr("href"))
                }
            }
        }
    }

    println "Total pages -> " + articles.size()
    def wordMap = new HashMap();
    articles.each {
        doc = Jsoup.connect("${context}${it}").get()
        def divs = doc.select("div.wysiwyg")
        if (divs) {
            println "page >> ${context}${it}"
            def ps = divs.select("p")
            ps.each { p ->
                def words = p.text().split(" ");
                words.each { w ->
                    w = w.trim()
                    if (w && w.length() > 1) {

                        if (w ==~ /[a-zA-Z]*$/) {
//                        println w
                            def cnt = wordMap.get(w.toLowerCase()) ?: 1;
                            wordMap.put(w.toLowerCase(), cnt + 1)
                        }

                    }
                }
            }
        }
    }
    println "update directory....."
    wordMap.each { k, v ->
        def rs = sqlCon.firstRow(querySql, [k])
        if (rs) {
            sqlCon.executeUpdate(updateFreq, [v, k])
        } else {
            sqlCon.executeInsert(insertSql, [k, v])
        }
    }
    sqlCon.close()
}


def download = {

    def sqlCon = Sql.newInstance(url, "sa", "", driver);
    sqlCon.eachRow(getWords, {
        def mp3Url = "${downloadUrl}/${it.spelling}.mp3"
        println mp3Url
        def ops = null;
        def file = new File(vFolder, "${it.spelling}.mp3");
        try {
            ops = file.newOutputStream()
            ops << new URL(mp3Url).openStream()
            sqlCon.executeUpdate(updateDown, [1, it.spelling])
        } catch (Exception e) {
            println e
            sqlCon.executeUpdate(updateDown, [-1, it.spelling])
            file.delete()
        } finally {
            ops.close()
        }
    })
    sqlCon.close();
}

def dummy = {
    println "hello..."
}
println "start ......"
def scheduler = new Scheduler();
scheduler.schedule("30 13 * * *", new Runnable() {
    public void run() {
        println "start crawle ......"
        scrawle()
        println "start download ......"
        download()
        println "finished ......"
    }
});
scheduler.start()
