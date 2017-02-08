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

def voa = "http://learningenglish.voanews.com/"
def downloadUrl = "https://ssl.gstatic.com/dictionary/static/sounds/de/0"
def bbc = "http://www.bbc.com/"
def usNews = "http://www.usnews.com/news"


def crawlerVoa = {
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

    def doc = Jsoup.connect(voa).get()

    def links = doc.select("a[href]");
    def levels = ["Level One", "Level Two", "Level Three"] as List

    def articles = new HashSet();
    links.each {
        if (levels.contains(it.ownText())) {
            def link = "${voa}${it.attr('href')}"

            doc = Jsoup.connect(link).get()
            def aLinks = doc.select("a[href]");
            aLinks.each { ai ->
                if (ai.attr("href").contains("/a/")) {
                    articles.add(ai.attr("href"))
                }
            }
        }
    }

    println "Total pages(VOA) -> " + articles.size()
    def wordMap = new HashMap();
    articles.each {
        doc = Jsoup.connect("${voa}${it}").get()
        def divs = doc.select("div.wysiwyg")
        if (divs) {
            def ps = divs.select("p")
            ps.each { p ->
                def words = p.text().split(" ");
                words.each { w ->
                    w = w.trim()
                    if (w && w.length() > 1) {

                        if (w ==~ /[a-zA-Z]*$/) {
                            def cnt = wordMap.get(w.toLowerCase()) ?: 1;
                            wordMap.put(w.toLowerCase(), cnt + 1)
                        }

                    }
                }
            }
        }
    }
    println "Update directory(VOA).....${wordMap.size()}"
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

def crawlerBbc = {
    def sqlCon = Sql.newInstance(url, "sa", "", driver);
    def doc = Jsoup.connect(bbc).get()
    def links = doc.select("a[href]");
    def articles = new HashSet();
    links.each {
        def link = it.attr('href')
        if(link.indexOf("/news/") > -1 && link.indexOf("http://www") < 0){
            articles.add(link)
        }
    }
    println "Total pages(BBC) -> " + articles.size()
    def wordMap = new HashMap();
    articles.each {
        try {
            doc = Jsoup.connect("${bbc}${it}").get()
            def divs = doc.select("div.story-body__inner")
            divs.each { div ->
                def ps = divs.select("p")
                ps.each { p ->
                    def words = p.text().split(" ");
                    words.each { w ->
                        w = w.trim()
                        if (w && w.length() > 1) {
                            if (w ==~ /[a-zA-Z]*$/) {
                                def cnt = wordMap.get(w.toLowerCase()) ?: 1;
                                wordMap.put(w.toLowerCase(), cnt + 1)
                            }

                        }
                    }
                }
            }
        }catch (Exception e){

        }

    }
    println "Update directory (BBC).....${wordMap.size()}"
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

def crawlerUSNews = {
    def sqlCon = Sql.newInstance(url, "sa", "", driver);
    def doc = Jsoup.connect(usNews).get()
    def links = doc.select("a[href]");
    def articles = new HashSet();
    links.each {
        def link = it.attr('href')
        if(link.indexOf("www.usnews.com/news") > -1 && link.indexOf("articles/2017")> -1){
            articles.add(link)
        }
    }
    println "Total pages(BBC) -> " + articles.size()
    def wordMap = new HashMap();
    articles.each {
        try {
            doc = Jsoup.connect("http:${it}").get()
            def divs = doc.select("div.ad-in-text-target")
            divs.each { div ->
                def ps = divs.select("p")
                ps.each { p ->
                    def words = p.text().split(" ");
                    words.each { w ->
                        w = w.trim()
                        if (w && w.length() > 1) {
                            if (w ==~ /[a-zA-Z]*$/) {
                                def cnt = wordMap.get(w.toLowerCase()) ?: 1;
                                wordMap.put(w.toLowerCase(), cnt + 1)
                            }

                        }
                    }
                }
            }
        }catch (Exception e){

        }

    }
    println "Update directory (USNews).....${wordMap.size()}"
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
    int cnt = 0;
    sqlCon.eachRow(getWords, {
        def mp3Url = "${downloadUrl}/${it.spelling}.mp3"
//        println mp3Url
        def ops = null;
        def file = new File(vFolder, "${it.spelling}.mp3");
        try {
            ops = file.newOutputStream()
            ops << new URL(mp3Url).openStream()
            sqlCon.executeUpdate(updateDown, [1, it.spelling])
            cnt ++;
        } catch (Exception e) {
            sqlCon.executeUpdate(updateDown, [-1, it.spelling])
            file.delete()
        } finally {
            ops.close()
        }
    })
    sqlCon.close();
    println "New words -> ${cnt}"
}

println "start ......"
def scheduler = new Scheduler();
scheduler.schedule("30 */4 * * *", new Runnable() {
    public void run() {
        crawlerVoa()
        crawlerBbc()
        crawlerUSNews()
        println "Start download ......"
        download()
        println "finished ......"
    }
})
scheduler.start()
