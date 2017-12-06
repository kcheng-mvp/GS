#! /usr/bin/env groovy


import groovyx.net.http.RESTClient
import static groovyx.net.http.ContentType.JSON
import groovy.json.*
import java.util.concurrent.*
import groovyx.net.http.AsyncHTTPBuilder

import groovyx.net.http.HTTPBuilder
import org.apache.http.impl.client.AbstractHttpClient
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.impl.conn.PoolingClientConnectionManager
import org.apache.http.params.HttpParams

@Grapes(
        @Grab(group='org.apache.httpcomponents', module='httpclient', version='4.5.2')
)




def url = "http://120.78.27.91"
//def url = "http://localhost/"

def context = ['/adv/agg', '/event/adv', '/user/register']

@Grab(group = 'org.codehaus.groovy.modules.http-builder', module = 'http-builder', version = '0.5.0')


class User {
    def appid;
    def uid;
    def time;
}

class Agg {
    def appid;
    def day;
    def newuser;
    def dau;
    def onlineTime;
    List<PUV> icon;
    List<PUV> page;
}

class PUV {
    def adv_appid;
    def pv;
    def uv;
}


class Adv {
    def appid
    def adv_appid;
    def uid;
    def time;
}

def randomString = { String alphabet, int n ->
    new Random().with {
        (1..n).collect { alphabet[nextInt(alphabet.length())] }.join()
    }
}

def random = new Random()


def randomObject = {
    if (it == 0) {
        return new User(appid: randomString((('A'..'Z') + ('0'..'9')).join(), 20),
                uid: randomString((('A'..'Z') + ('0'..'9')).join(), 20),
                time: System.currentTimeMillis())
    } else if (it == 1) {
        return new Adv(appid: randomString((('A'..'Z') + ('0'..'9')).join(), 20),
                uid: randomString((('A'..'Z') + ('0'..'9')).join(), 20),
                adv_appid: randomString((('A'..'Z') + ('0'..'9')).join(), 20),
                time: System.currentTimeMillis())
    } else {
        def icon = (0..2).collect {
            new PUV(adv_appid: randomString((('A'..'Z') + ('0'..'9')).join(), 20),
                    pv: random.nextInt(3000),
                    uv: random.nextInt(3000))
        }
        def page = (0..1).collect {
            new PUV(adv_appid: randomString((('A'..'Z') + ('0'..'9')).join(), 20),
                    pv: random.nextInt(3000),
                    uv: random.nextInt(3000))
        }

        new Agg(appid: randomString((('A'..'Z') + ('0'..'9')).join(), 20),
                day: new Date().format('yyyy/MM/dd'),
                newuser: random.nextInt(20000),
                dau: random.nextInt(10000),
                onlineTime: random.nextInt(20000),
                icon: icon,
                page: page)

    }

}





println "start"
def map = new HashMap<Integer, String>();
def post = { it ->
    def index = it % 3
//    println "${url}/${context[index]}"

    println it
    def jsonObj = map.get(index);
    if(!jsonObj){
        def o = randomObject(index);
        jsonObj = new JsonBuilder(o).toPrettyString();
        map.put(index,jsonObj)
    }
    def client = new RESTClient(url)

    def response = client.post(path: context[index], contentType: JSON, body: jsonObj, headers: [Accept: 'application/json'])

    client = null;
    response;
}

def threadPool = Executors.newFixedThreadPool(100)
try {
    List<Future> futures = Collections.synchronizedList((1..10000).collect {
        threadPool.submit({ ->
            post(it)
        } as Callable);
    })
    // recommended to use following statement to ensure the execution of all tasks.
    futures.each { println it.get() }
} finally {
    threadPool.shutdown()
}




println "Finished"

