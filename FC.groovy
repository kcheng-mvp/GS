#! /usr/bin/env groovy

@Grab(group='com.h2database', module='h2', version='1.3.173')
@GrabConfig(systemClassLoader=true)
@Grab(group='jaxen', module='jaxen', version='1.1.4')

import groovy.sql.Sql;
import org.jaxen.dom.DOMXPath;
import javax.xml.parsers.DocumentBuilderFactory;
import org.h2.tools.Csv;
import java.sql.*;
import org.h2.Driver;


def module = null;
def folder = null;
if (!args || args.length !=2){
	println "Please Input module name and folder";
	return -1
} else{
	module = args[0]
	folder = new File(args[1]);
}

def scriptDir = new File(getClass().protectionDomain.codeSource.location.path);
def home =System.getProperty("user.home");
def dbHome = new File(home,".h2");

def driver ="org.h2.Driver";
def url = "jdbc:h2:~/.h2/FC";



def PK_SET = new HashSet<String>();



def backupData = {
	// backup column
	def sqlCon = Sql.newInstance(url,"sa","",driver);
	def query ='''SELECT COUNT(1) - 4 cnt FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE='TABLE' and TABLE_NAME IN ('SYS_TABLE',
		'SYS_TABLE_COLUMN','SYS_TABLE_COLUMN_BK','SYS_TABLE_ASSOCIATION','SYS_TABLE_ASSOCIATION_BK')''';
	def rs = sqlCon.firstRow(query);
	if(rs.cnt > 0){
		sqlCon.withTransaction {
			println "Back up table ......";
			query='''INSERT INTO SYS_TABLE_COLUMN_BK(TABLE_NAME,COLUMN_NAME,COLUMN_LABEL,COMMENTS,VERSION)
					SELECT TABLE_NAME ,COLUMN_NAME ,COLUMN_LABEL ,COMMENTS ,CURRENT_TIMESTAMP()
					FROM SYS_TABLE_COLUMN
					WHERE (SYS_TABLE_COLUMN.COLUMN_LABEL IS NOT NULL OR SYS_TABLE_COLUMN.COMMENTS IS NOT NULL)''';
			sqlCon.executeInsert(query);
			println "Back up association ......";
			query ='''INSERT INTO SYS_TABLE_ASSOCIATION_BK (INDEX_KEY,T_A,T_B,CARDINALITY ,JOIN_CONDITION ,JOIN_NAME,USER_DEF,COMMENTS ,VERSION  )
					SELECT INDEX_KEY,T_A ,T_B ,CARDINALITY ,JOIN_CONDITION,JOIN_NAME ,USER_DEF ,COMMENTS ,CURRENT_TIMESTAMP()
					FROM SYS_TABLE_ASSOCIATION 
					WHERE (SYS_TABLE_ASSOCIATION.USER_DEF=TRUE OR SYS_TABLE_ASSOCIATION.COMMENTS IS NOT NULL)'''
			sqlCon.executeInsert(query);
		}
	}
	sqlCon.close();
}

def reinitDB = {
	println "Init DB......";
	def sqlCon = Sql.newInstance(url,"sa","",driver);
	sqlCon.withTransaction{
		sqlCon.execute("DROP TABLE IF EXISTS SYS_TABLE_COLUMN");
		sqlCon.execute("DROP TABLE IF EXISTS SYS_TABLE");
		sqlCon.execute("DROP TABLE IF EXISTS SYS_TABLE_ASSOCIATION");
		def initSqls = new File(scriptDir.parentFile, "kettle/kettle_h2.sql");
		def statements = initSqls.text.split(";")
		statements.each {
			if(it.trim()) sqlCon.execute(it);
		}
	}
	sqlCon.close();
}





def loadTableData = {

	def tableCSV = new File(folder, "table.csv");
	def sqlCon = Sql.newInstance(url,"sa","",driver);
	//def moduleSet = getModuleSet();
	sqlCon.withTransaction {
		println "Load tables ......";
		int cnt =0;
		sqlCon.withBatch(100) {stmt ->
			tableCSV.eachLine{ line ->
				def fields = line.split(";");
				def tableName = (fields[0]).trim();
				//def module = (tableName.split("_"))[0];
				//if(moduleSet.contains(module)){
					println "Module : ${module}";
					stmt.addBatch("INSERT INTO SYS_TABLE(MODULE_NAME,TABLE_NAME) values('${module}','${tableName}')");
					for(int i =2; i < fields.length;i++){
						def pkColumn = fields[i].trim();
						if(!pkColumn || "Oracle JDBC driver".equals(pkColumn)) continue;
						def temp = (pkColumn.split(" "))[0];
						PK_SET.add("${tableName}.${temp.trim()}");
					}
					cnt++;
				//}	
			}
		}
		//assert cnt == 6338 : "Missed table, please check"
		//println "${cnt} tables have been loaded....."
	}
	sqlCon.close();
}

// sub function
def getTablePKMap = {
	def tablePKMap = new HashMap<String,Integer>();
	def sqlCon = Sql.newInstance(url,"sa","",driver);
	def query ="SELECT ID,TABLE_NAME FROM SYS_TABLE";
	sqlCon.eachRow(query,{ row ->
		tablePKMap.put(row.TABLE_NAME, row.ID);
	});
	sqlCon.close();
	return tablePKMap;
}

def loadColumnData = {
	println "Load columns ......";
	def columnCSV = new File(folder, "column.csv");
	def sqlCon = Sql.newInstance(url,"sa","",driver);
	def tablePKMap = getTablePKMap();
	def cnt =0;
	sqlCon.withTransaction{
		sqlCon.withBatch(300) {stmt ->
			columnCSV.eachLine{ line ->
				def columns = line.split(";");
				def tableName = columns[0].trim();
				if(tablePKMap.containsKey(tableName)){
					//def moduleName = (tableName.split("_"))[0];
					for(int i =1; i < columns.length; i++){
						if(!columns[i].trim()) continue;
						def tableId = tablePKMap.get(tableName);
						def temp = ((columns[i]).trim()).split(" ");
						def name = temp[0].trim();
						def type = temp[1].trim();
						def pk = PK_SET.contains("${tableName}.${name}") ? Boolean.TRUE: Boolean.FALSE;
						stmt.addBatch("INSERT INTO SYS_TABLE_COLUMN(MODULE_NAME,TABLE_NAME,COLUMN_NAME,COLUMN_TYPE,PK_COLUMN) values('${module}','${tableName}','${name}','${type}', ${pk})");
					}
				}
			}
		}
	}
	sqlCon.close();	
}

def loadAssociations = {
	println "Load associations ......";
	def associationCSV = new File(folder, "association.csv");
	def sqlCon = Sql.newInstance(url,"sa","",driver);
	def tablePKMap = getTablePKMap();
	def cnt =0;
	sqlCon.withTransaction{
		sqlCon.withBatch(100) {stmt ->
			associationCSV.eachLine{ line ->
				def columns = line.split(";");
				def ta = columns[0].trim();
				def tb = columns[1].trim();
				//def firstInsert = columns[2].trim();
				def cardinality= columns[3].trim();
				def joinCondition= columns[4].trim();
				def joinName = columns[5].trim();
				if(! tablePKMap.containsKey(ta)){
					//println "Waring: ${ta} is not in the table list"
				} else if (!tablePKMap.containsKey(tb)){
					//println "Waring: ${tb} is not in the table list"
				} else{
					// Add index column M_A, M_B
					def ma= (ta.split("_"))[0];
					def mb= (tb.split("_"))[0];
					def indexKey = "MIXED";
					if(ma&&ma.equals(mb)) indexKey= ma;
					stmt.addBatch("INSERT INTO SYS_TABLE_ASSOCIATION(INDEX_KEY,T_A,T_B,CARDINALITY,JOIN_CONDITION,JOIN_NAME) values('${indexKey}', '${ta}','${tb}','${cardinality}', '${joinCondition}','${joinName}')");
				}
			}
		}
	}
	sqlCon.close();	
}

def restore = {
	def sqlCon = Sql.newInstance(url,"sa","",driver);
	def query ="select max(version) maxv from SYS_TABLE_COLUMN_BK";
	def rs = sqlCon.firstRow(query);
	if(rs.maxv){
		println "Restore the column ......";
			def update = '''UPDATE SYS_TABLE_COLUMN
							SET SYS_TABLE_COLUMN.COLUMN_LABEL = (SELECT SYS_TABLE_COLUMN_BK.COLUMN_LABEL FROM SYS_TABLE_COLUMN_BK 
							WHERE SYS_TABLE_COLUMN.TABLE_NAME = SYS_TABLE_COLUMN_BK.TABLE_NAME
							AND SYS_TABLE_COLUMN.COLUMN_NAME = SYS_TABLE_COLUMN_BK.COLUMN_NAME AND SYS_TABLE_COLUMN_BK.VERSION = ?),
							SYS_TABLE_COLUMN.COMMENTS = (SELECT SYS_TABLE_COLUMN_BK.COMMENTS FROM SYS_TABLE_COLUMN_BK 
							WHERE SYS_TABLE_COLUMN.TABLE_NAME = SYS_TABLE_COLUMN_BK.TABLE_NAME
							AND SYS_TABLE_COLUMN.COLUMN_NAME = SYS_TABLE_COLUMN_BK.COLUMN_NAME AND SYS_TABLE_COLUMN_BK.VERSION = ?)''';
			def cnt = sqlCon.executeUpdate(update,[rs.maxv,rs.maxv]);
			
	}
	query ="select max(version) maxv from SYS_TABLE_ASSOCIATION_BK";
	rs = sqlCon.firstRow(query);
	if(rs.maxv){
		println "Restore the association ......";
		sqlCon.withTransaction{
			// update
			update = '''UPDATE SYS_TABLE_ASSOCIATION SET SYS_TABLE_ASSOCIATION.COMMENTS=(
						SELECT  SYS_TABLE_ASSOCIATION_BK.COMMENTS
						FROM SYS_TABLE_ASSOCIATION_BK
						WHERE SYS_TABLE_ASSOCIATION.T_A = SYS_TABLE_ASSOCIATION_BK.T_A
						AND SYS_TABLE_ASSOCIATION.T_B = SYS_TABLE_ASSOCIATION_BK.T_B
						AND SYS_TABLE_ASSOCIATION.JOIN_CONDITION = SYS_TABLE_ASSOCIATION_BK.JOIN_CONDITION
						AND SYS_TABLE_ASSOCIATION.CARDINALITY = SYS_TABLE_ASSOCIATION_BK.CARDINALITY
						AND SYS_TABLE_ASSOCIATION_BK.VERSION = ?
						AND SYS_TABLE_ASSOCIATION_BK.USER_DEF = FALSE)''';
			cnt = sqlCon.executeUpdate(update,[rs.maxv]);
			//Insert 
			update ='''INSERT INTO SYS_TABLE_ASSOCIATION (INDEX_KEY,T_A,T_B,CARDINALITY ,JOIN_CONDITION ,JOIN_NAME, USER_DEF,COMMENTS)
							SELECT INDEX_KEY, T_A ,T_B ,CARDINALITY ,JOIN_CONDITION ,JOIN_NAME, USER_DEF ,COMMENTS
							FROM SYS_TABLE_ASSOCIATION_BK 
							where SYS_TABLE_ASSOCIATION_BK.USER_DEF=TRUE
							AND SYS_TABLE_ASSOCIATION_BK.VERSION = ? '''
			cnt = sqlCon.executeUpdate(update, [rs.maxv]);
		}
		
	}
	sqlCon.close();
}

def clearup = {
	println "Clear up data ......";
	def sqlCon = Sql.newInstance(url,"sa","",driver);
	// within a single transaction
	sqlCon.withTransaction{
		def query ="select min(version) minv,count(distinct version) ver from SYS_TABLE_COLUMN_BK";
		def rs = sqlCon.firstRow(query);
		// keep only latest 2 version
		if(rs.ver > 2){
			def cnt = sqlCon.executeUpdate("DELETE FROM SYS_TABLE_COLUMN_BK where version = ?", [rs.minv]);
			//println "${cnt} rows has been deleted from SYS_TABLE_COLUMN_BK";
		}
		query ="select min(version) minv,count(distinct version) ver from SYS_TABLE_ASSOCIATION_BK";
		rs = sqlCon.firstRow(query);
		if(rs.ver > 2){
			def cnt = sqlCon.executeUpdate("DELETE FROM SYS_TABLE_ASSOCIATION_BK where version = ?", [rs.minv]);
			//println "${cnt} rows has been deleted from SYS_TABLE_ASSOCIATION_BK";
		}
	}
	
	sqlCon.close();
}


//backupData();
//reinitDB();
loadTableData();
loadColumnData();
loadAssociations();
//restore();
//clearup();





