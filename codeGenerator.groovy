#! /usr/bin/env groovy

@Grapes([
        @Grab(group = 'com.google.guava', module = 'guava', version = '18.0'),
        @Grab(group = 'jaxen', module = 'jaxen', version = '1.1.4')
])

import com.google.common.base.CaseFormat
import groovy.io.FileType;
import groovy.xml.*

import java.text.SimpleDateFormat
import java.util.regex.Matcher
import java.util.regex.Pattern;

println "--- project.build.sourceDirectory: "+ project.build.sourceDirectory
println "--- project.build.scriptSourceDirectory: "+ project.build.scriptSourceDirectory
println "--- project.build.testSourceDirectory: "+ project.build.testSourceDirectory
println "--- project.build.outputDirectory: "+ project.build.outputDirectory
println "--- project.build.testOutputDirectory: "+ project.build.testOutputDirectory
println "--- project.build.directory:"+ project.build.directory



def linebreak = System.getProperty("line.separator");
def resources = null
project.build.resources.each {
    resources = it.directory
}

def getClaz = {
    def clzString = "${domainPackage}.${entity}"
    def clz = null;
    try {
        clz = this.class.classLoader.loadClass(clzString, true, false)
    } catch (Exception) {
        throw new RuntimeException("************* ERROR: Can't not find this class ${clzString} *************")
    }
    return clz;
}


def genSchema = {

    def clz = getClaz();

    def schemaFolder = new File(resources, "schema");
    if (!schemaFolder.exists()) schemaFolder.mkdirs()

    def typeMap = [
            "java.lang.String"    : "VARCHAR(10)",
            "java.lang.Integer"   : "INT",
            "int"   : "INT",
            "java.lang.Long"      : "BIGINT",
            "long"      : "BIGINT",
            "java.lang.Double"    : "NUMERIC(8,2)",
            "java.math.BigDecimal": "NUMERIC(12,2)",
            "java.util.Date"      : "DATETIME",
            "java.lang.Boolean"   : "BOOLEAN"
    ]


    def tableName = "${prefix}_${CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, entity)}";

    def targetFileName = null
    def columnOrderMap = [:]
    def columnTypeMap = [:]
    def indexClause = [] as List;
    schemaFolder.eachFileRecurse { f ->
        if (f.name.indexOf("${tableName}.sql") > -1) {
            targetFileName = f.name
            f.eachWithIndex { line, idx ->
                if (line && idx != 0) {
                    if (line.indexOf("ENGINE") < 0) {
                        line = line.subSequence(0, line.length() - 1);
                        def entries = line.split();
                        columnTypeMap.put(entries[0], entries[1])
                        columnOrderMap.put(entries[0], idx);
                    }
                    if (line.indexOf("CREATE INDEX") > -1 || line.indexOf("CREATE UNIQUE INDEX") > -1) {
                        indexClause.add(line)
                    }
                }
            }
        }
    }

    def sb = new StringBuffer("CREATE TABLE ${tableName}(");
    clz.metaClass.properties.sort {
        columnOrderMap.get(CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, it.name)) ?: it.name.length()
    }.eachWithIndex { prop, idx ->
        if (!prop.name.equals("class")) {
            def claz = (Class) prop.type
            def type = typeMap.get(prop.type.name);
            def column = CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, prop.name);
            if (!type) {
                if (claz.isEnum()) {
                    def fields = claz.getDeclaredFields()
                    def b = fields.stream().filter{p ->
                        p.type.name.indexOf(claz.name) < 0
                    }.findFirst().orElse(null)
                    type = typeMap.get(b.type.name)
                    column = CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, prop.name);
                } else if(java.util.Collection.class.isAssignableFrom(claz)) {
                    println "**Info**: ${prop.name} (${prop.type}) is assigned from  java.util.Collection"
                    type ="VARCHAR(20)"
                    column = CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, prop.name);
                } else {
                    println "**Info**: ${prop.name} (${prop.type}) is not a simple Type"
                    column = column + "_ID"
                    type = "BIGINT"
                }
            }
            type = columnTypeMap.get(column) ?: type;
            sb.append("${linebreak}").append("\t")
            if (prop.name.equals("id")) {
                sb.append("ID BIGINT NOT NULL AUTO_INCREMENT,")
            } else {
                sb.append("${column} ${type},")
            }
//            }
        }
    }
    sb.append("${linebreak}").append("\t")
    sb.append("PRIMARY KEY (ID)")
    sb.append("${linebreak})").append(" ").append("ENGINE=InnoDB CHARACTER SET utf8 COLLATE utf8_bin;")
    indexClause.each { line ->
        sb.append("${linebreak}");
        sb.append(line).append(";")
    }

    if (!targetFileName) {
        def sdf = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss");
        targetFileName = "V${sdf.format(Calendar.instance.getTime())}.${tableName}.sql"
        println "**Info**: ${targetFileName}  is generated !"
    } else {
        println "**Info**: ${targetFileName}  is updated !"
    }
    def sql = new File("${schemaFolder}/${targetFileName}");
    sql.newWriter().withWriter { w ->
        w << sb.toString();
    }

    println "**Info**: Generate ${resources}/schema-h2.sql"
    println "**Info**: Generate ${resources}/schema-msql.sql"
    def h2sb = new StringBuffer();
    def mysb = new StringBuffer();
    def h2sql = new File("${resources}/schema-h2.sql");
    def mysql = new File("${resources}/schema-mysql.sql");
    def cnt = 0;
    schemaFolder.listFiles().sort { it.name }.each { f ->
        if (f.name.indexOf(".sql") > -1) {
            cnt++;
            f.eachWithIndex { line, idx ->
                mysb.append(line).append(linebreak)
                if (line && line.indexOf("ENGINE") < 0) {
                    h2sb.append(line).append(linebreak);
                } else if (line.indexOf("ENGINE") > 0) {
                    h2sb.append(");${linebreak}");
                }
            }
        }
    }
    h2sql.newWriter().withWriter { w ->
        w << h2sb.toString();
    }
    mysql.newWriter().withWriter { w ->
        w << mysb.toString();
    }

}



def genInsert = {

    def clz = getClaz();
    def tableName = "${prefix}_${CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, entity)}";

    def insertBuffer = new StringBuffer(linebreak);
    def valueBuffer = new StringBuffer(" values (")
    insertBuffer.append("INSERT INTO ${tableName}(").append(linebreak);
    def size = clz.metaClass.properties.size();
    clz.metaClass.properties.eachWithIndex { prop, idx ->
        def claz = (Class) prop.type
        if (!prop.name.equals("id") && !prop.name.equals("class") && !java.util.Collection.class.isAssignableFrom(claz)) {
//            println "Property ==> ${prop.name}"
            def column = CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, prop.name);
            insertBuffer.append(column);
            if (idx + 1 < size) {
                insertBuffer.append(",");
            }
            if (prop.name.equals("createdAt") || prop.name.equals("updatedAt")) {
                valueBuffer.append("CURRENT_TIMESTAMP()")
            } else {
                valueBuffer.append("#{").append("${prop.name}").append("}")
            }
            if (idx + 1 < size) {
                valueBuffer.append(",")
            }
        }

    }
    insertBuffer.append(")").append(linebreak).append(valueBuffer).append(")")
    return insertBuffer.toString();

}

def genSelectById = {
    def clz = getClaz();
    def tableName = "${prefix}_${CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, entity)}";

    def selectBuffer = new StringBuffer(linebreak).append("SELECT ");
    def size = clz.metaClass.properties.size();
    clz.metaClass.properties.eachWithIndex { prop, idx ->

        def claz = (Class) prop.type
        if (!prop.name.equals("class") && !java.util.Collection.class.isAssignableFrom(claz)) {
            def column = CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, prop.name);
            selectBuffer.append(column);
            if (idx + 1 < size) {
                selectBuffer.append(",");
            }
        }

    }
    selectBuffer.append(linebreak).append(" FROM ${tableName}")
            .append(linebreak).append("WHERE ID = #{id}")
//    println selectBuffer.toString();
    return selectBuffer.toString();
}
def genUpdate = {
    def clz = getClaz();
    def tableName = "${prefix}_${CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, entity)}";


    def ignoredProperties = ["createdAt", "createdBy", "id", "class", "updatedAt"]
    def updateBuffer = new StringBuffer(linebreak);
    updateBuffer.append("UPDATE ${tableName}").append(linebreak);
    updateBuffer.append("<set>")
    updateBuffer.append("\t")
    def size = clz.metaClass.properties.size();
    clz.metaClass.properties.eachWithIndex { prop, idx ->
        def claz = (Class) prop.type
        if (!ignoredProperties.contains(prop.name) && !java.util.Collection.class.isAssignableFrom(claz)) {
            def column = CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, prop.name);

            updateBuffer.append("<if test=\"${prop.name} != null\">").append(linebreak)
            updateBuffer.append("\t").append("${column} = #{").append(prop.name).append("}")
            if (idx + 1 < size) {
                updateBuffer.append(",");
            }
            updateBuffer.append("</if>").append(linebreak);
        }

    }
    updateBuffer.append("UPDATED_AT = CURRENT_TIMESTAMP()").append(linebreak)
    updateBuffer.append("</set>");
    updateBuffer.append("WHERE ID = #{id}")
    return updateBuffer.toString();
}
def genMapper = {

    getClaz()
    def clzOutPut = new File(project.build.outputDirectory);
    def mapperPackage = null;
    clzOutPut.eachFileRecurse(FileType.FILES) {
        if (it.name.endsWith(".class")) {
            def clzName = it.absolutePath.substring(project.build.outputDirectory.length() + 1)
                    .reverse().drop(6).reverse().replaceAll(Matcher.quoteReplacement(File.separator), ".")

            def clzT = this.class.classLoader.loadClass(clzName, true, false)

            clzT.getAnnotations().each {
                if (it.toString().indexOf("MapperScan") > -1) {
                    mapperPackage = it.basePackages()[0]
                }
            }
        }
    }
    if (!mapperPackage) throw new RuntimeException("Can not find the mapper package!")
//    println "**Info**: Mapper package ${mapperPackage}";
    def mapperPath = new File(project.build.sourceDirectory, mapperPackage.replaceAll(Pattern.quote("."), Matcher.quoteReplacement(File.separator)));
    if (!mapperPath.exists()) mapperPath.mkdirs()
    def mapper = new File(mapperPath, "${entity}Mapper.java");
    if (mapper.exists() && !mapper.isDirectory()) {
        println "**Warning**: Mapper ${mapper.absolutePath} exists";
    } else {
        def fw = new FileWriter(mapper);
        def bw = new BufferedWriter(fw);
        def now = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(Calendar.getInstance().time)
        bw.write("package ${mapperPackage};");
        bw.newLine();
        bw.write("import ${domainPackage}.${entity};")
        bw.write(linebreak)
        bw.write(" ")
        bw.newLine()
        bw.write("/**")
        bw.newLine()
        bw.write("* @author ${System.getProperty("user.name")}")
        bw.newLine()
        bw.write("* @version ${project.version}")
        bw.newLine()
        bw.write("* @date ${now}")
        bw.newLine()
        bw.write("*/")
        bw.newLine()
        bw.write(" ")
        bw.newLine()
        bw.write("public interface ${entity}Mapper {")
        bw.newLine()
        bw.write("\t");
        bw.write("Long create${entity}(${entity} ${entity.toLowerCase()});");
        bw.newLine()
        bw.write("\t");
        bw.write("Integer update${entity}(${entity} ${entity.toLowerCase()});");
        bw.newLine()
        bw.write("\t");
        bw.write("${entity} find${entity}ById(Long id);");
        bw.newLine()
        bw.write("}")
        bw.newLine()
        bw.close()
        println "**Info**: ${mapper.path}"
    }



    def parser = new XmlParser();
    parser.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false)
    parser.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

    def mapperXmlPath = new File(resources, mapperPackage.replaceAll(Pattern.quote("."), Matcher.quoteReplacement(File.separator)));
    if (!mapperXmlPath.exists()) mapperXmlPath.mkdirs()
    def mapperXml = new File(mapperXmlPath, "${entity}Mapper.xml");
//    println "Try to generate file ${entity}Mapper.xml";
    if (mapperXml.exists() && !mapperXml.isDirectory()) {
        println "**Warning**: Mapper ${mapperXml.absolutePath} exists";
        def response = parser.parseText(mapperXml.getText());

        println "................................................................"
        def node = parser.createNode(
                response,
                "insert",
                [id: "create${entity}", parameterType: "${entity}", keyProperty: "id", useGeneratedKeys: "true"]
        )
        node.setValue(genInsert());
        new XmlNodePrinter(preserveWhitespace: true).print(node);


        println "................................................................"
        node = parser.createNode(
                response,
                "update",
                [id: "update${entity}", parameterType: "${entity}"]
        )
        node.setValue(genUpdate())
        def xmlOutput = new StringWriter()
        def xmlNodePrinter = new XmlNodePrinter(new PrintWriter(xmlOutput))
        xmlNodePrinter.print(node)
        println xmlOutput.toString().replaceAll("&lt;", "<").replaceAll("&gt;", ">");

        println "................................................................"
        node = parser.createNode(
                response,
                "select",
                [id: "get${entity}ById", resultType: "${entity}"]
        )
        node.setValue(genSelectById());
        new XmlNodePrinter(preserveWhitespace: true).print(node);


    } else {
        def writer = new StringWriter()
        def xml = new MarkupBuilder(writer)
        xml.setEscapeAttributes(true);
        def helper = new groovy.xml.MarkupBuilderHelper(xml)
        helper.xmlDeclaration([version: '1.0', encoding: 'UTF-8', standalone: 'no'])
        helper.yieldUnescaped """<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd" >"""

        xml.mapper(namespace: "${mapperPackage}.${entity}Mapper") {
            insert(id: "create${entity}", parameterType: "${domainPackage}.${entity}", keyProperty: "id", useGeneratedKeys: "true", genInsert());
            update(id: "update${entity}", parameterType: "${domainPackage}.${entity}", genUpdate());
            select(id: "find${entity}ById", resultType: "${domainPackage}.${entity}", flushCache: "true", genSelectById())
        }

        mapperXml << XmlUtil.serialize(writer.toString().replaceAll("&lt;", "<").replaceAll("&gt;", ">"));
        println "**Info**: ${mapperXml.getAbsolutePath()}"

    }
}

println ">>> action is ${action} ......"

if (action.equalsIgnoreCase("schema")) {
    genSchema()
} else {
    genMapper()
}
