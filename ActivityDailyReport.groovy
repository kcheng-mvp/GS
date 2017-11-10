#! /usr/bin/env groovy

import java.text.SimpleDateFormat
import groovy.json.JsonSlurper
import groovy.transform.ToString

def scriptDir = new File(getClass().protectionDomain.codeSource.location.path).parent
GroovyShell shell = new GroovyShell()
def xls = shell.parse(new File(scriptDir, "core/Xls.groovy"))



@ToString
class Icon {
    String adv_appid
    Long pv
    Long uv
}

@ToString
class Page{
    String adv_appid
    Long pv
    Long uv
}

@ToString
class GameDaily {
    String appid;
    String platform;
    String date;
    Long newuser;
    Long dau;
    Long onlineTime;
    List<Icon> icon;
    List<Page> page;
}
//advagg.2017-11-01-01.log
def gameMap = ["2016052401435705":"猪来了",
               "2017090108502293":"猪来了",
               "2017090408550279":"百战斗斗堂",
        "2015122301031978":"口袋德州",
        "2017090508566356":"口袋德州",
        "2016052301432568":"口袋斗地主",
        "2017090508566176":"口袋斗地主",
        "2017091508737604":"疯狂飞车",
        "2017092208870109":"大天使之剑",
        "2017101609329388":"魔晶猎人",
        "2017051207220849":"口袋小镇(H5)",
        "2017073107972674":"小游戏中心(H5)",
        "2017102409497949":"小游戏中心(H5)"
] as Map
def sdf = new SimpleDateFormat("yyyy-MM")
def today = Calendar.getInstance().getTime();
def path = new File("/Users/kcheng/Downloads");
def pattern = ~ /advagg\.${sdf.format(today)}.*\.log$/
def jsonSlurper= new JsonSlurper();
path.eachFile { f ->
    if(f.name =~ pattern){
        f.eachLine {line ->
            def obj = jsonSlurper.parseText(line);
            def daily = new GameDaily(obj)
            def int a = 1+1
        }
    }
}
