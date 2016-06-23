#!/usr/bin/env groovy


@Grapes([
	@Grab(group='com.sbs', module='ZHConverter', version='1.0')
])


import com.spreada.utils.chinese.ZHConverter;

def home= new File(System.getProperty("user.home"));

if(! args || args.size() != 1){
	println "please input your folder"
	return -1;
}

def zh_CN = "zh-CN"


def summary = new File("/tmp/result.csv");
summary = new FileOutputStream(summary);
def bws = new BufferedWriter(new OutputStreamWriter(summary,"UTF-8"));

def folder = new File(args[0])

def converter = ZHConverter.getInstance(ZHConverter.SIMPLIFIED); 

folder.eachFileRecurse{
	if(!it.isDirectory()){
		/*
		def result = new File(it.path.replace("zh-TW","zh-CN"));
		result = new FileOutputStream(result);
		def bw = new BufferedWriter(new OutputStreamWriter(result,"UTF-8"));
		*/
		it.eachWithIndex{line,idx ->
			//def sb = new StringBuilder();
			def ssb = new StringBuilder();
			line.each{ c ->
				
				if (isChinese(c)) {
					ssb.append(c);
				} else{
					if(ssb){
						bws.write("${it.getName()},${idx},${ssb.toString()}")
						bws.newLine();
						ssb = new StringBuilder();
					}
				}
				
				//def nc = isChinese(c) ? converter.convert(c) : c;
				//sb.append(nc)
			}
			//bw.write(sb.toString())
			//bw.newLine();
		}
		//bw.close()
	} else{
		//(new File(it.path.replace("zh-TW","zh-CN"))).mkdirs()
	}
}
bws.close()




def isChinese(c) {
	def cc = c[0] as char;
    Character.UnicodeBlock ub = Character.UnicodeBlock.of(cc);  
    if (ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS  
            || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS  
            || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A  
            || ub == Character.UnicodeBlock.GENERAL_PUNCTUATION  
            || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION  
            || ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS) {  
        return true;  
    }  
    return false;  
}