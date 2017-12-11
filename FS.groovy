#! /usr/bin/env groovy
import java.text.SimpleDateFormat
import groovy.time.TimeCategory

//
//
//if(args.size() !=1){
//    println "FS.groovy folder"
//    return -1;
//}
//File root = new File(args[0]);
//if(!root.exists()){
//    println "The file ${args[0]} does not exits";
//    return  -1;
//}
//def f = new File("folder.txt");
//def fw = new FileWriter(f);
//def bw = new BufferedWriter(fw);
////searchInputFile(root);
////def searchInputFile(inputFile){
//    if (root.isDirectory()){
//        root.eachFileRecurse{
//            if(it.isDirectory()){
//                println "--> "+ it.getPath();
//                bw.write(it.getPath());
//                bw.newLine();
//            }
//        }
//    }
////}
//bw.close();

def sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");


def today = Calendar.getInstance().previous()
def validDay = today.getTime()
println sdf.format(today.getTime())
today.set(Calendar.DAY_OF_MONTH, 1)
println sdf.format(today.getTime())
println sdf.format(validDay)
