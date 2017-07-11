#! /usr/bin/env groovy

if(!args){
  println "Please specify the file you want to check. eg coffeeStyle hello.coffee"
  return -1;
}

def coffee = new File(args[0]);
if(!coffee.exists() || !coffee.getName().endsWith("coffee")){
  println "File is not exist or it's not a coffe file"
  return -1
}
def rule1 = ~/\(\s+|\s+\)/;
def rule2 = ~/\s+,/;
def rule3 = ~/\w\+=|\+=\w|\w\-=|\-=\w|\w==|==\w|\w<|<\w|\w>|>\w|\w<=|<=\w|\w>=|>=\w|\w[\+\-\*\/=]|[\+\-\*\/=]\w/
def rule4 = ~/\S->/;
def lineNumber = 0;
def misMap =["&&":"and","||":"or","==":"is","!":"not"]
coffee.eachLine{line ->
  lineNumber++;
  if(line.indexOf('"') < 0){
    if(line =~ rule1){
      println "Line : ${lineNumber} : Immediately inside parentheses, brackets or braces"
      println "--> ${line}";
    }
    if(line =~ rule2){
      println "Line : ${lineNumber} : Immediately before a comma"
      println "--> ${line}";
    }
    if(line =~ rule3){
      println "Line : ${lineNumber} : Always surround these binary operators with a single space on either side"
      println "--> ${line}";
    }
    if(line =~ rule4){
      println "Line : ${lineNumber} : When declaring a function that takes arguments, always use a single space after the closing parenthesis of the arguments list"
      println "--> ${line}";
    }
    if(linde.length == line.indexOf(";")+1 ){
      println "Line : ${lineNumber} : This ; is no need"
      println "--> ${line}";
    }
    misMap.each{k,v ->
      if(line.indexOf(k) > -1){
        println "&& -> and, || -> or , == -> is, ! -> not";
        println "--> ${line}";
      }
    }
  }
}
