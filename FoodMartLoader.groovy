#!/usr/bin/env groovy
import groovy.sql.Sql;
import groovy.grape.Grape;
import java.util.zip.ZipFile;
import java.sql.Types;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.MySQL5Dialect;
import org.hibernate.dialect.H2Dialect;




@Grapes([
	@Grab(group='com.h2database', module='h2', version='1.3.174'),
    @GrabConfig(systemClassLoader=true),
	@Grab(group = 'mysql', module = 'mysql-connector-java', version = '5.1.30'),
	@GrabConfig(systemClassLoader=true),
	@Grab(group = 'org.hibernate', module = 'hibernate', version = '3.2.3.ga'),
    @GrabExclude('javax.transaction#jta'),
    @GrabConfig(systemClassLoader=true)
])


class DbConfig {
	String url;
	String user;
	String password;
	String driver;
	Dialect dialect;
	Sql sql;
	String nl = System.getProperty("line.separator");
	
	def init(){
		sql = Sql.newInstance(url, user,password, driver);
	}
	def createTable(String tableName,Column... columns){
		try{
			StringBuilder sb = new StringBuilder("DROP TABLE IF EXISTS ");
			sb.append(tableName);
			sql.execute(sb.toString());
		} catch(Exception e){
			println "Delete table: ${tableName.toUpperCase()} failed, ignore";
		}
		println "CREATE TABLE ${tableName.toUpperCase()}";
		StringBuilder sb = new StringBuilder("CREATE TABLE ${tableName} (");
		sb.append(nl);
		columns.eachWithIndex{Column col,i ->
			if(i > 0) {
				sb.append(",${nl}");
			} 
			sb.append(col.name).append(" ");
			sb.append(col.getDbColumnName(dialect))
			sb.append(col.constraint);
		}
		sb.append(")");
		//println sb.toString();
		sql.execute(sb.toString());
		
	}
	
	def createIndex(Boolean isUnique,String tableName, String indexName, List cols){
		def buf = new StringBuilder();
		buf.append(isUnique ? "CREATE UNIQUE INDEX " : "CREATE INDEX ")
		.append(indexName)
		.append(" ON ").append(tableName)
		.append(" (");
		cols.eachWithIndex{col,i ->
		   if( i > 0){
		   	  buf.append(",");
		   }
		   buf.append(col);
		}
		buf.append(")");
		println buf.toString();
		sql.execute(buf.toString());
	}
}

class Column {
    String name;
    int type;
    String typeName;
    int length;
    int precision; 
    int scale;
    Boolean nullsAllowed;
    final String constraint;
    
    public Column(String name, int type, Boolean nullsAllowed) {
        this.name = name;
        this.type = type;
        this.constraint = nullsAllowed ? "" : " NOT NULL";
	    // default length of string is 30
        this.length=30;
	    // default precision for numerical is (10,4)
        this.precision=10;
        this.scale=4;
    }
    
    public Column(String name, int type, Boolean nullsAllowed,int length) {
        this.name = name;
        this.type = type;
        this.length=length;
        this.constraint = nullsAllowed ? "" : "NOT NULL";
        this.length=length;
        this.precision=10;
        this.scale=4;
    }
    
    public String getDbColumnName(Dialect dialect){
    	return dialect.getTypeName(type,length,precision,scale)
    }
    
}

class MySQL5DialectExt extends MySQL5Dialect {  
    public MySQL5DialectExt () {  
        super();  
        registerColumnType(Types.DECIMAL, "decimal(\$p,\$s)"); 
        registerColumnType(Types.REAL, "real");
        registerColumnType(Types.BOOLEAN, "boolean");
    }  
}  


def supportedDb = ["mysql","h2"];
def dbPlatform = args.size() >=1 ? args[0] : "h2";
assert supportedDb.contains(dbPlatform) : "The support database is ${supportedDb}"
println "Using database : ${dbPlatform}";
def homeDir = System.getProperty("user.home");
def dbConfig = new DbConfig();
if (dbPlatform == "mysql") {
   dbConfig.url = "jdbc:mysql://localhost/foodmart?createDatabaseIfNotExist=true";
   dbConfig.user ="root";
   dbConfig.password="";
   dbConfig.driver="com.mysql.jdbc.Driver";
   dbConfig.dialect= new MySQL5DialectExt();
} else if (dbPlatform == "h2"){
   dbConfig.url = "jdbc:h2:~/.h2/foodmart";
   dbConfig.user ="sa";
   dbConfig.password="";
   dbConfig.driver="org.h2.Driver"
   dbConfig.dialect= new H2Dialect();
   
}
dbConfig.init();


dbConfig.createTable(
   "sales_fact_1997",
    new Column("product_id", Types.INTEGER, false),
    new Column("time_id", Types.INTEGER, false),
    new Column("customer_id", Types.INTEGER, false),
    new Column("promotion_id", Types.INTEGER, false),
    new Column("store_id", Types.INTEGER, false),
    new Column("store_sales", Types.DECIMAL, false),
    new Column("store_cost", Types.DECIMAL, false),
    new Column("unit_sales", Types.DECIMAL, false)	
);

dbConfig.createTable(
    "sales_fact_1998",
    new Column("product_id", Types.INTEGER, false),
    new Column("time_id", Types.INTEGER, false),
    new Column("customer_id", Types.INTEGER, false),
    new Column("promotion_id", Types.INTEGER, false),
    new Column("store_id", Types.INTEGER, false),
    new Column("store_sales", Types.DECIMAL, false),
    new Column("store_cost", Types.DECIMAL, false),
    new Column("unit_sales", Types.DECIMAL, false));

dbConfig.createTable(
    "sales_fact_dec_1998", 
    new Column("product_id", Types.INTEGER, false),
    new Column("time_id", Types.INTEGER, false),
    new Column("customer_id", Types.INTEGER, false),
    new Column("promotion_id", Types.INTEGER, false),
    new Column("store_id", Types.INTEGER, false),
    new Column("store_sales", Types.DECIMAL, false),
    new Column("store_cost", Types.DECIMAL, false),
    new Column("unit_sales", Types.DECIMAL, false));

dbConfig.createTable(
    "inventory_fact_1997", 
    new Column("product_id", Types.INTEGER, false),
    new Column("time_id", Types.INTEGER, true),
    new Column("warehouse_id", Types.INTEGER, true),
    new Column("store_id", Types.INTEGER, true),
    new Column("units_ordered", Types.INTEGER, true),
    new Column("units_shipped", Types.INTEGER, true),
    new Column("warehouse_sales", Types.DECIMAL, true),
    new Column("warehouse_cost", Types.DECIMAL, true),
    new Column("supply_time", Types.SMALLINT , true),
    new Column("store_invoice", Types.DECIMAL, true));
dbConfig.createTable(
    "inventory_fact_1998", 
    new Column("product_id", Types.INTEGER, false),
    new Column("time_id", Types.INTEGER, true),
    new Column("warehouse_id", Types.INTEGER, true),
    new Column("store_id", Types.INTEGER, true),
    new Column("units_ordered", Types.INTEGER, true),
    new Column("units_shipped", Types.INTEGER, true),
    new Column("warehouse_sales", Types.DECIMAL, true),
    new Column("warehouse_cost", Types.DECIMAL, true),
    new Column("supply_time", Types.SMALLINT , true),
    new Column("store_invoice", Types.DECIMAL, true));

//  Aggregate tables

dbConfig.createTable(
    "agg_pl_01_sales_fact_1997",
    new Column("product_id", Types.INTEGER, false),
    new Column("time_id", Types.INTEGER, false),
    new Column("customer_id", Types.INTEGER, false),
    new Column("store_sales_sum", Types.DECIMAL, false),
    new Column("store_cost_sum", Types.DECIMAL, false),
    new Column("unit_sales_sum", Types.DECIMAL, false),
    new Column("fact_count", Types.INTEGER, false));
dbConfig.createTable(
    "agg_ll_01_sales_fact_1997",
    new Column("product_id", Types.INTEGER, false),
    new Column("time_id", Types.INTEGER, false),
    new Column("customer_id", Types.INTEGER, false),
    new Column("store_sales", Types.DECIMAL, false),
    new Column("store_cost", Types.DECIMAL, false),
    new Column("unit_sales", Types.DECIMAL, false),
    new Column("fact_count", Types.INTEGER, false));
dbConfig.createTable(
    "agg_l_03_sales_fact_1997", 
    new Column("time_id", Types.INTEGER, false),
    new Column("customer_id", Types.INTEGER, false),
    new Column("store_sales", Types.DECIMAL, false),
    new Column("store_cost", Types.DECIMAL, false),
    new Column("unit_sales", Types.DECIMAL, false),
    new Column("fact_count", Types.INTEGER, false));
dbConfig.createTable(
    "agg_l_04_sales_fact_1997", 
    new Column("time_id", Types.INTEGER, false),
    new Column("store_sales", Types.DECIMAL, false),
    new Column("store_cost", Types.DECIMAL, false),
    new Column("unit_sales", Types.DECIMAL, false),
    new Column("customer_count", Types.INTEGER, false),
    new Column("fact_count", Types.INTEGER, false));
dbConfig.createTable(
    "agg_l_05_sales_fact_1997",
    new Column("product_id", Types.INTEGER, false),
    new Column("customer_id", Types.INTEGER, false),
    new Column("promotion_id", Types.INTEGER, false),
    new Column("store_id", Types.INTEGER, false),
    new Column("store_sales", Types.DECIMAL, false),
    new Column("store_cost", Types.DECIMAL, false),
    new Column("unit_sales", Types.DECIMAL, false),
    new Column("fact_count", Types.INTEGER, false));
dbConfig.createTable(
    "agg_c_10_sales_fact_1997", 
    new Column("month_of_year", Types.SMALLINT , false),
    new Column("quarter", Types.VARCHAR, false),
    new Column("the_year", Types.SMALLINT , false),
    new Column("store_sales", Types.DECIMAL, false),
    new Column("store_cost", Types.DECIMAL, false),
    new Column("unit_sales", Types.DECIMAL, false),
    new Column("customer_count", Types.INTEGER, false),
    new Column("fact_count", Types.INTEGER, false));
dbConfig.createTable(
    "agg_c_14_sales_fact_1997", 
    new Column("product_id", Types.INTEGER, false),
    new Column("customer_id", Types.INTEGER, false),
    new Column("store_id", Types.INTEGER, false),
    new Column("promotion_id", Types.INTEGER, false),
    new Column("month_of_year", Types.SMALLINT , false),
    new Column("quarter", Types.VARCHAR, false),
    new Column("the_year", Types.SMALLINT , false),
    new Column("store_sales", Types.DECIMAL, false),
    new Column("store_cost", Types.DECIMAL, false),
    new Column("unit_sales", Types.DECIMAL, false),
    new Column("fact_count", Types.INTEGER, false));
dbConfig.createTable(
    "agg_lc_100_sales_fact_1997", 
    new Column("product_id", Types.INTEGER, false),
    new Column("customer_id", Types.INTEGER, false),
    new Column("quarter", Types.VARCHAR, false),
    new Column("the_year", Types.SMALLINT , false),
    new Column("store_sales", Types.DECIMAL, false),
    new Column("store_cost", Types.DECIMAL, false),
    new Column("unit_sales", Types.DECIMAL, false),
    new Column("fact_count", Types.INTEGER, false));
dbConfig.createTable(
    "agg_c_special_sales_fact_1997", 
    new Column("product_id", Types.INTEGER, false),
    new Column("promotion_id", Types.INTEGER, false),
    new Column("customer_id", Types.INTEGER, false),
    new Column("store_id", Types.INTEGER, false),
    new Column("time_month", Types.SMALLINT , false),
    new Column("time_quarter", Types.VARCHAR, false),
    new Column("time_year", Types.SMALLINT , false),
    new Column("store_sales_sum", Types.DECIMAL, false),
    new Column("store_cost_sum", Types.DECIMAL, false),
    new Column("unit_sales_sum", Types.DECIMAL, false),
    new Column("fact_count", Types.INTEGER, false));
dbConfig.createTable(
    "agg_g_ms_pcat_sales_fact_1997", 
    new Column("gender", Types.VARCHAR, false),
    new Column("marital_status", Types.VARCHAR, false),
    new Column("product_family", Types.VARCHAR, true),
    new Column("product_department", Types.VARCHAR, true),
    new Column("product_category", Types.VARCHAR, true),
    new Column("month_of_year", Types.SMALLINT , false),
    new Column("quarter", Types.VARCHAR, false),
    new Column("the_year", Types.SMALLINT , false),
    new Column("store_sales", Types.DECIMAL, false),
    new Column("store_cost", Types.DECIMAL, false),
    new Column("unit_sales", Types.DECIMAL, false),
    new Column("customer_count", Types.INTEGER, false),
    new Column("fact_count", Types.INTEGER, false));
dbConfig.createTable(
    "agg_lc_06_sales_fact_1997", 
    new Column("time_id", Types.INTEGER, false),
    new Column("city", Types.VARCHAR, false),
    new Column("state_province", Types.VARCHAR, false),
    new Column("country", Types.VARCHAR, false),
    new Column("store_sales", Types.DECIMAL, false),
    new Column("store_cost", Types.DECIMAL, false),
    new Column("unit_sales", Types.DECIMAL, false),
    new Column("fact_count", Types.INTEGER, false));
dbConfig.createTable(
    "currency", 
    new Column("currency_id", Types.INTEGER, false),
    new Column("date", Types.DATE, false),
    new Column("currency", Types.VARCHAR, false),
    new Column("conversion_ratio", Types.DECIMAL, false));
dbConfig.createTable(
    "account", 
    new Column("account_id", Types.INTEGER, false),
    new Column("account_parent", Types.INTEGER, true),
    new Column("account_description", Types.VARCHAR, true),
    new Column("account_type", Types.VARCHAR, false),
    new Column("account_rollup", Types.VARCHAR, false),
    new Column("Custom_Members", Types.VARCHAR, true,255));
dbConfig.createTable(
    "category", 
    new Column("category_id", Types.VARCHAR, false),
    new Column("category_parent", Types.VARCHAR, true),
    new Column("category_description", Types.VARCHAR, false),
    new Column("category_rollup", Types.VARCHAR, true));
dbConfig.createTable(
    "customer", 
    new Column("customer_id", Types.INTEGER, false),
    new Column("account_num", Types.BIGINT, false),
    new Column("lname", Types.VARCHAR, false),
    new Column("fname", Types.VARCHAR, false),
    new Column("mi", Types.VARCHAR, true),
    new Column("address1", Types.VARCHAR, true),
    new Column("address2", Types.VARCHAR, true),
    new Column("address3", Types.VARCHAR, true),
    new Column("address4", Types.VARCHAR, true),
    new Column("city", Types.VARCHAR, true),
    new Column("state_province", Types.VARCHAR, true),
    new Column("postal_code", Types.VARCHAR, false),
    new Column("country", Types.VARCHAR, false),
    new Column("customer_region_id", Types.INTEGER, false),
    new Column("phone1", Types.VARCHAR, false),
    new Column("phone2", Types.VARCHAR, false),
    new Column("birthdate", Types.DATE, false),
    new Column("marital_status", Types.VARCHAR, false),
    new Column("yearly_income", Types.VARCHAR, false),
    new Column("gender", Types.VARCHAR, false),
    new Column("total_children", Types.SMALLINT , false),
    new Column("num_children_at_home", Types.SMALLINT , false),
    new Column("education", Types.VARCHAR, false),
    new Column("date_accnt_opened", Types.DATE, false),
    new Column("member_card", Types.VARCHAR, true),
    new Column("occupation", Types.VARCHAR, true),
    new Column("houseowner", Types.VARCHAR, true),
    new Column("num_cars_owned", Types.INTEGER, true),
    new Column("fullname", Types.VARCHAR, false,60));
dbConfig.createTable(
    "days", 
    new Column("day", Types.INTEGER, false),
    new Column("week_day", Types.VARCHAR, false));
dbConfig.createTable(
    "department", 
    new Column("department_id", Types.INTEGER, false),
    new Column("department_description", Types.VARCHAR, false));
dbConfig.createTable(
    "employee", 
    new Column("employee_id", Types.INTEGER, false),
    new Column("full_name", Types.VARCHAR, false),
    new Column("first_name", Types.VARCHAR, false),
    new Column("last_name", Types.VARCHAR, false),
    new Column("position_id", Types.INTEGER, true),
    new Column("position_title", Types.VARCHAR, true),
    new Column("store_id", Types.INTEGER, false),
    new Column("department_id", Types.INTEGER, false),
    new Column("birth_date", Types.DATE, false),
    new Column("hire_date", Types.TIMESTAMP, true),
    new Column("end_date", Types.TIMESTAMP, true),
    new Column("salary", Types.DECIMAL, false),
    new Column("supervisor_id", Types.INTEGER, true),
    new Column("education_level", Types.VARCHAR, false),
    new Column("marital_status", Types.VARCHAR, false),
    new Column("gender", Types.VARCHAR, false),
    new Column("management_role", Types.VARCHAR, true));
dbConfig.createTable(
    "employee_closure", 
    new Column("employee_id", Types.INTEGER, false),
    new Column("supervisor_id", Types.INTEGER, false),
    new Column("distance", Types.INTEGER, true));
dbConfig.createTable(
    "expense_fact", 
    new Column("store_id", Types.INTEGER, false),
    new Column("account_id", Types.INTEGER, false),
    new Column("exp_date", Types.TIMESTAMP, false),
    new Column("time_id", Types.INTEGER, false),
    new Column("category_id", Types.VARCHAR, false),
    new Column("currency_id", Types.INTEGER, false),
    new Column("amount", Types.DECIMAL, false));
dbConfig.createTable(
    "position", 
    new Column("position_id", Types.INTEGER, false),
    new Column("position_title", Types.VARCHAR, false),
    new Column("pay_type", Types.VARCHAR, false),
    new Column("min_scale", Types.DECIMAL, false),
    new Column("max_scale", Types.DECIMAL, false),
    new Column("management_role", Types.VARCHAR, false));
dbConfig.createTable(
    "product", 
    new Column("product_class_id", Types.INTEGER, false),
    new Column("product_id", Types.INTEGER, false),
    new Column("brand_name", Types.VARCHAR, true,60),
    new Column("product_name", Types.VARCHAR, false,60),
    new Column("SKU", Types.BIGINT, false),
    new Column("SRP", Types.DECIMAL, true),
    new Column("gross_weight", Types.REAL, true),
    new Column("net_weight", Types.REAL, true),
    new Column("recyclable_package", Types.BOOLEAN, true),
    new Column("low_fat", Types.BOOLEAN, true),
    new Column("units_per_case", Types.SMALLINT , true),
    new Column("cases_per_pallet", Types.SMALLINT , true),
    new Column("shelf_width", Types.REAL, true),
    new Column("shelf_height", Types.REAL, true),
    new Column("shelf_depth", Types.REAL, true));
dbConfig.createTable(
    "product_class", 
    new Column("product_class_id", Types.INTEGER, false),
    new Column("product_subcategory", Types.VARCHAR, true),
    new Column("product_category", Types.VARCHAR, true),
    new Column("product_department", Types.VARCHAR, true),
    new Column("product_family", Types.VARCHAR, true));
dbConfig.createTable(
    "promotion", 
    new Column("promotion_id", Types.INTEGER, false),
    new Column("promotion_district_id", Types.INTEGER, true),
    new Column("promotion_name", Types.VARCHAR, true),
    new Column("media_type", Types.VARCHAR, true),
    new Column("cost", Types.DECIMAL, true),
    new Column("start_date", Types.TIMESTAMP, true),
    new Column("end_date", Types.TIMESTAMP, true));
dbConfig.createTable(
    "region", 
    new Column("region_id", Types.INTEGER, false),
    new Column("sales_city", Types.VARCHAR, true),
    new Column("sales_state_province", Types.VARCHAR, true),
    new Column("sales_district", Types.VARCHAR, true),
    new Column("sales_region", Types.VARCHAR, true),
    new Column("sales_country", Types.VARCHAR, true),
    new Column("sales_district_id", Types.INTEGER, true));
dbConfig.createTable(
    "reserve_employee", 
    new Column("employee_id", Types.INTEGER, false),
    new Column("full_name", Types.VARCHAR, false),
    new Column("first_name", Types.VARCHAR, false),
    new Column("last_name", Types.VARCHAR, false),
    new Column("position_id", Types.INTEGER, true),
    new Column("position_title", Types.VARCHAR, true),
    new Column("store_id", Types.INTEGER, false),
    new Column("department_id", Types.INTEGER, false),
    new Column("birth_date", Types.TIMESTAMP, false),
    new Column("hire_date", Types.TIMESTAMP, true),
    new Column("end_date", Types.TIMESTAMP, true),
    new Column("salary", Types.DECIMAL, false),
    new Column("supervisor_id", Types.INTEGER, true),
    new Column("education_level", Types.VARCHAR, false),
    new Column("marital_status", Types.VARCHAR, false),
    new Column("gender", Types.VARCHAR, false));
dbConfig.createTable(
    "salary", 
    new Column("pay_date", Types.TIMESTAMP, false),
    new Column("employee_id", Types.INTEGER, false),
    new Column("department_id", Types.INTEGER, false),
    new Column("currency_id", Types.INTEGER, false),
    new Column("salary_paid", Types.DECIMAL, false),
    new Column("overtime_paid", Types.DECIMAL, false),
    new Column("vacation_accrued", Types.REAL, false),
    new Column("vacation_used", Types.REAL, false));
dbConfig.createTable(
    "store", 
    new Column("store_id", Types.INTEGER, false),
    new Column("store_type", Types.VARCHAR, true),
    new Column("region_id", Types.INTEGER, true),
    new Column("store_name", Types.VARCHAR, true),
    new Column("store_number", Types.INTEGER, true),
    new Column("store_street_address", Types.VARCHAR, true),
    new Column("store_city", Types.VARCHAR, true),
    new Column("store_state", Types.VARCHAR, true),
    new Column("store_postal_code", Types.VARCHAR, true),
    new Column("store_country", Types.VARCHAR, true),
    new Column("store_manager", Types.VARCHAR, true),
    new Column("store_phone", Types.VARCHAR, true),
    new Column("store_fax", Types.VARCHAR, true),
    new Column("first_opened_date", Types.TIMESTAMP, true),
    new Column("last_remodel_date", Types.TIMESTAMP, true),
    new Column("store_sqft", Types.INTEGER, true),
    new Column("grocery_sqft", Types.INTEGER, true),
    new Column("frozen_sqft", Types.INTEGER, true),
    new Column("meat_sqft", Types.INTEGER, true),
    new Column("coffee_bar", Types.BOOLEAN, true),
    new Column("video_store", Types.BOOLEAN, true),
    new Column("salad_bar", Types.BOOLEAN, true),
    new Column("prepared_food", Types.BOOLEAN, true),
    new Column("florist", Types.BOOLEAN, true));
dbConfig.createTable(
    "store_ragged", 
    new Column("store_id", Types.INTEGER, false),
    new Column("store_type", Types.VARCHAR, true),
    new Column("region_id", Types.INTEGER, true),
    new Column("store_name", Types.VARCHAR, true),
    new Column("store_number", Types.INTEGER, true),
    new Column("store_street_address", Types.VARCHAR, true),
    new Column("store_city", Types.VARCHAR, true),
    new Column("store_state", Types.VARCHAR, true),
    new Column("store_postal_code", Types.VARCHAR, true),
    new Column("store_country", Types.VARCHAR, true),
    new Column("store_manager", Types.VARCHAR, true),
    new Column("store_phone", Types.VARCHAR, true),
    new Column("store_fax", Types.VARCHAR, true),
    new Column("first_opened_date", Types.TIMESTAMP, true),
    new Column("last_remodel_date", Types.TIMESTAMP, true),
    new Column("store_sqft", Types.INTEGER, true),
    new Column("grocery_sqft", Types.INTEGER, true),
    new Column("frozen_sqft", Types.INTEGER, true),
    new Column("meat_sqft", Types.INTEGER, true),
    new Column("coffee_bar", Types.BOOLEAN, true),
    new Column("video_store", Types.BOOLEAN, true),
    new Column("salad_bar", Types.BOOLEAN, true),
    new Column("prepared_food", Types.BOOLEAN, true),
    new Column("florist", Types.BOOLEAN, true));
dbConfig.createTable(
    "time_by_day", 
    new Column("time_id", Types.INTEGER, false),
    new Column("the_date", Types.TIMESTAMP, true),
    new Column("the_day", Types.VARCHAR, true),
    new Column("the_month", Types.VARCHAR, true),
    new Column("the_year", Types.SMALLINT , true),
    new Column("day_of_month", Types.SMALLINT , true),
    new Column("week_of_year", Types.INTEGER, true),
    new Column("month_of_year", Types.SMALLINT , true),
    new Column("quarter", Types.VARCHAR, true),
    new Column("fiscal_period", Types.VARCHAR, true));
dbConfig.createTable(
    "warehouse", 
    new Column("warehouse_id", Types.INTEGER, false),
    new Column("warehouse_class_id", Types.INTEGER, true),
    new Column("stores_id", Types.INTEGER, true),
    new Column("warehouse_name", Types.VARCHAR, true,60),
    new Column("wa_address1", Types.VARCHAR, true),
    new Column("wa_address2", Types.VARCHAR, true),
    new Column("wa_address3", Types.VARCHAR, true),
    new Column("wa_address4", Types.VARCHAR, true),
    new Column("warehouse_city", Types.VARCHAR, true),
    new Column("warehouse_state_province", Types.VARCHAR, true),
    new Column("warehouse_postal_code", Types.VARCHAR, true),
    new Column("warehouse_country", Types.VARCHAR, true),
    new Column("warehouse_owner_name", Types.VARCHAR, true),
    new Column("warehouse_phone", Types.VARCHAR, true),
    new Column("warehouse_fax", Types.VARCHAR, true));
dbConfig.createTable(
    "warehouse_class", 
    new Column("warehouse_class_id", Types.INTEGER, false),
    new Column("description", Types.VARCHAR, true));
    
println "Loading foodmart data ..."
def scriptDir = new File(getClass().protectionDomain.codeSource.location.path);
def zipFile = new ZipFile("${scriptDir.parentFile.absolutePath}/olap/FoodMartCreateData.zip");
def start = System.currentTimeMillis();
def insertClause = null;
zipFile.entries().each{ entry ->
	   zipFile.getInputStream(entry).eachLine{ line ->
		//dbConfig.sql.execute(line.replaceAll("\"",""));
		dbConfig.sql.withBatch(100){stmt ->
			stmt.addBatch(line.replaceAll("\"",""));
		}
	}
}
println "Data loaded, lapse time : ${System.currentTimeMillis() - start}";






println "Creating index for normal tables ..."


dbConfig.createIndex(true,"account","i_account_id",["account_id"]);
dbConfig.createIndex(false,"account","i_account_parent",["account_parent"]);
dbConfig.createIndex(true,"category","i_category_id",["category_id"]);
dbConfig.createIndex(false,"category","i_category_parent",["category_parent"]);
dbConfig.createIndex(true,"currency","i_currency",["currency_id", "date"]);
dbConfig.createIndex(false,"customer","i_cust_acct_num",["account_num"]);
dbConfig.createIndex(false,"customer","i_customer_fname",["fname"]);
dbConfig.createIndex(false,"customer","i_customer_lname",["lname"]);
dbConfig.createIndex(false,"customer","i_cust_child_home",["num_children_at_home"]);
dbConfig.createIndex(true,"customer","i_customer_id",["customer_id"]);
dbConfig.createIndex(false,"customer","i_cust_postal_code",["postal_code"]);
dbConfig.createIndex(false,"customer","i_cust_region_id",["customer_region_id"]);
dbConfig.createIndex(true,"department","i_department_id",["department_id"]);
dbConfig.createIndex(true,"employee","i_employee_id",["employee_id"]);
dbConfig.createIndex(false,"employee","i_empl_dept_id",["department_id"]);
dbConfig.createIndex(false,"employee","i_empl_store_id",["store_id"]);
dbConfig.createIndex(false,"employee","i_empl_super_id",["supervisor_id"]);
dbConfig.createIndex(true,"employee_closure","i_empl_closure",["supervisor_id", "employee_id"]);
dbConfig.createIndex(false,"employee_closure","i_empl_closure_emp",["employee_id"]);
dbConfig.createIndex(false,"expense_fact","i_expense_store_id",["store_id"]);
dbConfig.createIndex(false,"expense_fact","i_expense_acct_id",["account_id"]);
dbConfig.createIndex(false,"expense_fact","i_expense_time_id",["time_id"]);
dbConfig.createIndex(false,"inventory_fact_1997","i_inv_97_prod_id",["product_id"]);
dbConfig.createIndex(false,"inventory_fact_1997","i_inv_97_store_id",["store_id"]);
dbConfig.createIndex(false,"inventory_fact_1997","i_inv_97_time_id",["time_id"]);
dbConfig.createIndex(false,"inventory_fact_1997","i_inv_97_wrhse_id",["warehouse_id"]);
dbConfig.createIndex(false,"inventory_fact_1998","i_inv_98_prod_id",["product_id"]);
dbConfig.createIndex(false,"inventory_fact_1998","i_inv_98_store_id",["store_id"]);
dbConfig.createIndex(false,"inventory_fact_1998","i_inv_98_time_id",["time_id"]);
dbConfig.createIndex(false,"inventory_fact_1998","i_inv_98_wrhse_id",["warehouse_id"]);
dbConfig.createIndex(true,"position","i_position_id",["position_id"]);
dbConfig.createIndex(false,"product","i_prod_brand_name",["brand_name"]);
dbConfig.createIndex(true,"product","i_product_id",["product_id"]);
dbConfig.createIndex(false,"product","i_prod_class_id",["product_class_id"]);
dbConfig.createIndex(false,"product","i_product_name",["product_name"]);
dbConfig.createIndex(false,"product","i_product_SKU",["SKU"]);
dbConfig.createIndex(true,"promotion","i_promotion_id",["promotion_id"]);
dbConfig.createIndex(false,"promotion","i_promo_dist_id",["promotion_district_id"]);
dbConfig.createIndex(true,"reserve_employee","i_rsrv_empl_id",["employee_id"]);
dbConfig.createIndex(false,"reserve_employee","i_rsrv_empl_dept",["department_id"]);
dbConfig.createIndex(false,"reserve_employee","i_rsrv_empl_store",["store_id"]);
dbConfig.createIndex(false,"reserve_employee","i_rsrv_empl_sup",["supervisor_id"]);
dbConfig.createIndex(false,"salary","i_salary_pay_date",["pay_date"]);
dbConfig.createIndex(false,"salary","i_salary_employee",["employee_id"]);
dbConfig.createIndex(false,"sales_fact_1997","i_sls_97_cust_id",["customer_id"]);
dbConfig.createIndex(false,"sales_fact_1997","i_sls_97_prod_id",["product_id"]);
dbConfig.createIndex(false,"sales_fact_1997","i_sls_97_promo_id",["promotion_id"]);
dbConfig.createIndex(false,"sales_fact_1997","i_sls_97_store_id",["store_id"]);
dbConfig.createIndex(false,"sales_fact_1997","i_sls_97_time_id",["time_id"]);
dbConfig.createIndex(false,"sales_fact_dec_1998","i_sls_dec98_cust",["customer_id"]);
dbConfig.createIndex(false,"sales_fact_dec_1998","i_sls_dec98_prod",["product_id"]);
dbConfig.createIndex(false,"sales_fact_dec_1998","i_sls_dec98_promo",["promotion_id"]);
dbConfig.createIndex(false,"sales_fact_dec_1998","i_sls_dec98_store",["store_id"]);
dbConfig.createIndex(false,"sales_fact_dec_1998","i_sls_dec98_time",["time_id"]);
dbConfig.createIndex(false,"sales_fact_1998","i_sls_98_cust_id",["customer_id"]);
dbConfig.createIndex(false,"sales_fact_1998","i_sls_1998_prod_id",["product_id"]);
dbConfig.createIndex(false,"sales_fact_1998","i_sls_1998_promo",["promotion_id"]);
dbConfig.createIndex(false,"sales_fact_1998","i_sls_1998_store",["store_id"]);
dbConfig.createIndex(false,"sales_fact_1998","i_sls_1998_time_id",["time_id"]);
dbConfig.createIndex(true,"store","i_store_id",["store_id"]);
dbConfig.createIndex(false,"store","i_store_region_id",["region_id"]);
dbConfig.createIndex(true,"store_ragged","i_store_raggd_id",["store_id"]);
dbConfig.createIndex(false,"store_ragged","i_store_rggd_reg",["region_id"]);
dbConfig.createIndex(true,"time_by_day","i_time_id",["time_id"]);
dbConfig.createIndex(true,"time_by_day","i_time_day",["the_date"]);
dbConfig.createIndex(false,"time_by_day","i_time_year",["the_year"]);
dbConfig.createIndex(false,"time_by_day","i_time_quarter",["quarter"]);
dbConfig.createIndex(false,"time_by_day","i_time_month",["month_of_year"]);


println "Loading data for agg tables ..."

def sqlFile = new File("${scriptDir.parentFile.absolutePath}/olap/insert.sql");
def insertSql = new StringBuilder();
sqlFile.eachLine { line ->
	if(!line.startsWith("#") && line.trim().length() > 0){
		if(line.startsWith("INSERT INTO")){
			if(insertSql.length() > 0){
				println insertSql.substring(0,insertSql.indexOf("(")).replaceAll("\"","");
				dbConfig.sql.execute(insertSql.toString().replaceAll("\"",""));
				insertSql = new StringBuilder();
			}
			insertSql.append(line).append(" ");
		} else {
			insertSql.append(line).append(" ");
		}
	}
}

dbConfig.sql.execute(insertSql.toString().replaceAll("\"",""));
println "Creating index for agg tables ..."

dbConfig.createIndex(false,"agg_pl_01_sales_fact_1997","i_sls97pl01cust",["customer_id"]);
dbConfig.createIndex(false,"agg_pl_01_sales_fact_1997","i_sls97pl01prod",["product_id"]);
dbConfig.createIndex(false,"agg_pl_01_sales_fact_1997","i_sls97pl01time",["time_id"]);
dbConfig.createIndex(false,"agg_ll_01_sales_fact_1997","i_sls97ll01cust",["customer_id"]);
dbConfig.createIndex(false,"agg_ll_01_sales_fact_1997","i_sls97ll01prod",["product_id"]);
dbConfig.createIndex(false,"agg_ll_01_sales_fact_1997","i_sls97ll01time",["time_id"]);
dbConfig.createIndex(false,"agg_l_05_sales_fact_1997","i_sls97l05cust",["customer_id"]);
dbConfig.createIndex(false,"agg_l_05_sales_fact_1997","i_sls97l05prod",["product_id"]);
dbConfig.createIndex(false,"agg_l_05_sales_fact_1997","i_sls97l05promo",["promotion_id"]);
dbConfig.createIndex(false,"agg_l_05_sales_fact_1997","i_sls97l05store",["store_id"]);
dbConfig.createIndex(false,"agg_c_14_sales_fact_1997","i_sls97c14cust",["customer_id"]);
dbConfig.createIndex(false,"agg_c_14_sales_fact_1997","i_sls97c14prod",["product_id"]);
dbConfig.createIndex(false,"agg_c_14_sales_fact_1997","i_sls97c14promo",["promotion_id"]);
dbConfig.createIndex(false,"agg_c_14_sales_fact_1997","i_sls97c14store",["store_id"]);
dbConfig.createIndex(false,"agg_lc_100_sales_fact_1997","i_sls97lc100cust",["customer_id"]);
dbConfig.createIndex(false,"agg_lc_100_sales_fact_1997","i_sls97lc100prod",["product_id"]);
dbConfig.createIndex(false,"agg_c_special_sales_fact_1997","i_sls97speccust",["customer_id"]);
dbConfig.createIndex(false,"agg_c_special_sales_fact_1997","i_sls97specprod",["product_id"]);
dbConfig.createIndex(false,"agg_c_special_sales_fact_1997","i_sls97specpromo",["promotion_id"]);
dbConfig.createIndex(false,"agg_c_special_sales_fact_1997","i_sls97specstore",["store_id"]);
dbConfig.createIndex(false,"agg_g_ms_pcat_sales_fact_1997","i_sls97gmp_gender",["gender"]);
dbConfig.createIndex(false,"agg_g_ms_pcat_sales_fact_1997","i_sls97gmp_ms",["marital_status"]);
dbConfig.createIndex(false,"agg_g_ms_pcat_sales_fact_1997","i_sls97gmp_pfam",["product_family"]);
dbConfig.createIndex(false,"agg_g_ms_pcat_sales_fact_1997","i_sls97gmp_pdept",["product_department"]);
dbConfig.createIndex(false,"agg_g_ms_pcat_sales_fact_1997","i_sls97gmp_pcat",["product_category"]);
dbConfig.createIndex(false,"agg_g_ms_pcat_sales_fact_1997","i_sls97gmp_tmonth",["month_of_year"]);
dbConfig.createIndex(false,"agg_g_ms_pcat_sales_fact_1997","i_sls97gmp_tquarter",["quarter"]);


dbConfig.sql.close();

