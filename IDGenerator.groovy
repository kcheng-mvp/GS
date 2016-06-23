#! /usr/bin/env groovy
def checkCode="10X98765432";
def weightArray = [7,9,10,5,8,4,2,1,6,3,7,9,10,5,8,4,2] as List<Integer>;
def calendar = Calendar.instance;
calendar.add(Calendar.YEAR, -20);
def prefix ="440304";
def fixedPart = "${prefix}${calendar.getTime().format("yyyyMMdd")}"

def genCheckCode = {str ->
    def sum = 0;
    str.eachWithIndex{it, idx ->
        sum += Integer.valueOf(it) * weightArray[idx];
    }
    return checkCode.charAt(sum%11);
}


(1..5).each{
    def sb = new StringBuffer(fixedPart);
    def random = new Random();
    (1..3).each{
        sb.append(random.nextInt(10));
    }
    sb.append(genCheckCode(sb.toString()));
    println sb.toString();
}


