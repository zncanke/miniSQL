import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.HashMap;
import java.util.Vector;
import java.util.function.BiFunction;

/**
 * Created by lenovo on 2015/10/27.
 */

class TableNotExistsException extends Exception{}
class AttributeNotExistsException extends Exception{}
class NotUniqueException extends Exception{}
class IndexNotExistsException extends Exception{}

class CatalogManager {
    static HashMap<String, SQLTable> tables = new HashMap<String, SQLTable>();
    static HashMap<String, SQLIndex> indexes = new HashMap<String, SQLIndex>();
    static Document dbDoc;
    static Document indexDoc;
    static String dbName;
    static String INT = "java.lang.Integer";
    static String STRING = "java.lang.String";
    static String FLOAT = "java.lang.Float";

    /**
     * 初始化数据库，利用将传进的参数作为一层目录，数据库相关操作讲在这层目录下进行
     * 读取表信息和索引信息
     * @param name 数据库操作目录
     */
    public static void initDb(String name) {
        String path = System.getProperty("user.dir");
        System.setProperty("user.dir", path + "/" + name);
        path = System.getProperty("user.dir");
        System.out.println(path);
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            /*----------Get DataBase info----------------*/
            DocumentBuilder builder = dbf.newDocumentBuilder();
            File catalogFile = new File(path, "db_catalog.xml");
            FileInputStream in = new FileInputStream(catalogFile);
            dbDoc = builder.parse(in);
            /* Get Database */
            Element root = dbDoc.getDocumentElement();
            if (root == null) return;
            dbName = root.getAttribute("name");

            /* Get Tables and Index */
            NodeList tableList = root.getChildNodes();
            if (tableList == null) return;
            for (int i = 0;i < tableList.getLength();i++) {
                Node rootNode = tableList.item(i);
                if (rootNode != null && rootNode.getNodeType() == Node.ELEMENT_NODE) {
                    SQLTable table = new SQLTable(rootNode.getAttributes().getNamedItem("name").getNodeValue());
                    /* Get Attribute */
                    NodeList attrsList = rootNode.getChildNodes();
                    if (attrsList == null) continue;
                    for (int j = 0; j < attrsList.getLength(); j++) {
                        Node attrNode = attrsList.item(j);
                        if (attrNode != null && attrNode.getNodeType() == Node.ELEMENT_NODE) {
                            /* Add attribute to table */
                            SQLAttribute attr = new SQLAttribute(
                                    attrNode.getAttributes().getNamedItem("name").getNodeValue(),
                                    Class.forName(attrNode.getAttributes().getNamedItem("type").getNodeValue()),
                                    Integer.parseInt(attrNode.getAttributes().getNamedItem("length").getNodeValue()),
                                    Boolean.parseBoolean(attrNode.getAttributes().getNamedItem("unique").getNodeValue()),
                                    attrNode.getAttributes().getNamedItem("index").getNodeValue());
                            table.addAttribute(attr);
                        }
                    }
                    /* Add table to database */
                    tables.put(table.getName(), table);
                }
            }
            /*-------------Get Index info-----------------*/
            File indexFile = new File(path, "index_catalog.xml");
            FileInputStream indexInput = new FileInputStream(indexFile);
            indexDoc = builder.parse(indexInput);
            /* Get Database */
            root = indexDoc.getDocumentElement();
            if (root == null) return;
            NodeList indexList = root.getChildNodes();
            if (indexList != null) {
                for (int i = 0;i < indexList.getLength();i++) {
                    Node indexNode = indexList.item(i);
                    if (indexNode != null && indexNode.getNodeType() == Node.ELEMENT_NODE) {
                        String indexName = indexNode.getAttributes().getNamedItem("name").getNodeValue();
                        String tableName = indexNode.getAttributes().getNamedItem("base_table").getNodeValue();
                        String attrName = indexNode.getAttributes().getNamedItem("base_attribute").getNodeValue();
                        String rootIndex = indexNode.getAttributes().getNamedItem("root_index").getNodeValue();
                        if (tables.containsKey(tableName)) {
                            SQLIndex index = new SQLIndex(indexName, tables.get(tableName),
                                    tables.get(tableName).getAttribute(attrName),
                                    Integer.parseInt(rootIndex));
                            indexes.put(indexName, index);
                        }
                    }
                }
            }
        } catch (FileNotFoundException e) {
            File indexFile = new File(path, "index_catalog.xml");
            File dbFile = new File(path, "db_catalog.xml");
            try {
                indexFile.createNewFile();
                dbFile.createNewFile();
            } catch (IOException ee) {
                ee.printStackTrace();;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * 向数据库中添加一个索引
     * @param indexName 索引名
     * @param tableName 索引所在表名
     * @param attrName 索引所指向的属性名
     * @throws IOException 索引文件建立失败
     * @throws NotUniqueException 所指属性非唯一
     * @throws TableNotExistsException 所指表不存在
     * @throws AttributeNotExistsException 所指属性不存在
     */
    public static void addIndex(String indexName, String tableName, String attrName) throws IOException ,NotUniqueException, TableNotExistsException, AttributeNotExistsException {
        if (tables.containsKey(tableName)) {
            SQLTable table = tables.get(tableName);
            SQLAttribute attr = table.getAttribute(attrName);
            if (attr == null) {
                throw new AttributeNotExistsException();
            } else if (!attr.isUnique()) {
                throw new NotUniqueException();
            } else {
                SQLIndex index = new SQLIndex(indexName, table, attr, 0);
                indexes.put(indexName, index);
                attr.setIndex(indexName);
                BufferManager.CreateNewFile(indexName + ".ind");
//                /*--------------Modify db_catalog--------------*/
//                /* Search table */
//                NodeList tableList = dbDoc.getDocumentElement().getChildNodes();
//                for (int i = 0;i < tableList.getLength();i++) {
//                    Node tableNode = tableList.item(i);
//                    if (tableNode != null && tableNode.getNodeType() == Node.ELEMENT_NODE &&
//                            tableNode.getAttributes().getNamedItem("name").getNodeValue().equals(tableName)) {
//                        /* Search Attribute */
//                        NodeList attrList = tableNode.getChildNodes();
//                        for (int j = 0;j < attrList.getLength();j++) {
//                            Node attrNode = attrList.item(j);
//                            if (attrNode != null && attrNode.getNodeType() == Node.ELEMENT_NODE &&
//                                    attrNode.getAttributes().getNamedItem("name").getNodeValue().equals(attrName)) {
//                                attrNode.getAttributes().getNamedItem("indexed").setNodeValue("true");
//                            }
//                        }
//                    }
//                }
//                /*---------------Modify index_catalog-------------*/
//                Element indexNode = indexDoc.createElement("index");
//                indexNode.setAttribute("name", indexName);
//                indexNode.setAttribute("table_name", tableName);
//                indexNode.setAttribute("attribute_name", attrName);
//                indexDoc.getDocumentElement().appendChild(indexNode);
//                /*--------------Modify indexes-------------------*/
//                SQLIndex index = new SQLIndex(indexName, table, attr);
//                indexes.put(indexName, index);
            }
        } else {
            throw new TableNotExistsException();
        }
    }

    /**
     * 删除一个已经建立的索引
     * @param indexName 要删除的索引名
     * @throws IndexNotExistsException 索引不存在
     */
    public static void dropIndex(String indexName) throws IndexNotExistsException{
        if (indexes.containsKey(indexName)) {
            SQLIndex ind = indexes.get(indexName);
            ind.getBaseAttribute().setIndex(null);
            indexes.remove(indexName);
            BufferManager.DeleteFile(indexName + ".ind");
        } else {
            throw new IndexNotExistsException();
        }
    }

    /**
     * 创建一个新表
     * @param name 新表的表名
     * @param attrs 新表的各个属性的集合
     * @throws IOException 建立表数据文件错误
     */
    public static void createTable(String name, Vector<SQLAttribute> attrs) throws IOException{
//        Element tableNode = dbDoc.createElement("table");
//        tableNode.setAttribute("name", name);
//        for (SQLAttribute a : attrs) {
//            table.addAttribute(a);
//            Element attrNode = dbDoc.createElement("attribute");
//            attrNode.setAttribute("name", a.getName());
//            attrNode.setAttribute("type", a.getType().getName());
//            attrNode.setAttribute("length", String.valueOf(a.getLength()));
//            attrNode.setAttribute("unique", String.valueOf(a.isUnique()));
//            attrNode.setAttribute("indexed", String.valueOf(a.isIndexed()));
//            tableNode.appendChild(attrNode);
//        }
//
//        tables.put(name, table);
//        dbDoc.getDocumentElement().appendChild(tableNode);
        SQLTable table = new SQLTable(name);
        for (SQLAttribute a : attrs) {
            table.addAttribute(a);
        }
        tables.put(name, table);
        BufferManager.CreateNewFile(name);
    }

    /**
     * 删除一个表
     * @param name 要删除的表名
     * @throws TableNotExistsException 要删除的表不存在
     */
    public static void dropTable(String name) throws TableNotExistsException {
        if (tables.containsKey(name)) {
            SQLTable table = tables.get(name);
            for (SQLAttribute a : table.attributeCollection()) {
                if (!a.getIndex().equals("")) {
                    indexes.remove(a.getIndex());
                    BufferManager.RemoveAllNodeOfTable(a.getIndex() + ".ind");
                    BufferManager.DeleteFile(a.getIndex() + ".ind");
                }
            }
            tables.remove(name);
            BufferManager.RemoveAllNodeOfTable(name);
            BufferManager.DeleteFile(name);
            /*
            Element root = dbDoc.getDocumentElement();
            NodeList tableList = root.getChildNodes();
            if (tableList != null) {
                for (int i = 0;i < tableList.getLength();i++) {
                    Node tableNode = tableList.item(i);
                    if (tableNode != null && tableNode.getNodeType() == Node.ELEMENT_NODE &&
                        tableNode.getAttributes().getNamedItem("name").getNodeValue().equals(name)) {
                        root.removeChild(tableNode);
                        break;
                    }
                }
            } */
        } else {
            throw new TableNotExistsException();
        }
    }

    /**
     * 根据表名选择一个表返回
     * @param name 要选择的表名
     * @return 储存表信息的SQLTable类型变量
     */
    public static SQLTable getTable(String name) {
        if (tables.containsKey(name)) {
            return tables.get(name);
        } else {
            return null;
        }
    }

    /**
     * 根据表名和属性名选择一个属性返回
     * @param tableName 要选择的属性所在的表
     * @param attrName 要选择的属性名
     * @return 储存属性信息的SQLAttribute类型变量
     * @throws AttributeNotExistsException 属性不存在
     * @throws TableNotExistsException 表不存在
     */
    public static SQLAttribute getAttribute(String tableName, String attrName) throws AttributeNotExistsException, TableNotExistsException {
        if (tables.containsKey(tableName)) {
            SQLTable table = tables.get(tableName);
            if (table.getAttribute(attrName) == null) {
                throw new AttributeNotExistsException();
            } else {
                return table.getAttribute(attrName);
            }
        } else {
            throw new TableNotExistsException();
        }
    }

    /**
     * 获取一个表中的单条记录大小，如果大小不足4字节则返回4字节
     * @param tableName 表名
     * @return 单条记录大小
     * @throws TableNotExistsException 表不存在
     */
    public static int SizeOfRecord(String tableName) throws TableNotExistsException {
        SQLTable t;
        if (tables.containsKey(tableName)) {
            t = tables.get(tableName);
        } else {
            throw new TableNotExistsException();
        }
        return t.size();
    }

    /**
     * 获取一个属性在单条记录中的偏移位置
     * @param tableName 表名
     * @param attrName 属性名
     * @return 偏移位置
     * @throws TableNotExistsException 表不存在
     * @throws AttributeNotExistsException 属性不存在
     */
    public static int getOffsetOfAttr(String tableName, String attrName) throws TableNotExistsException, AttributeNotExistsException {
        SQLTable t;
        if (tables.containsKey(tableName)) {
            t = tables.get(tableName);
        } else {
            throw new TableNotExistsException();
        }
        return t.getOffsetOfAttribute(attrName);
    }

    /**
     * 获取一条属性的大小
     * @param tableName 属性所在表名
     * @param attrName 属性名
     * @return 属性大小
     * @throws TableNotExistsException 表不存在
     * @throws AttributeNotExistsException 属性不存在
     */
    public static int getLengthOfAttr(String tableName, String attrName) throws  TableNotExistsException, AttributeNotExistsException {
        SQLTable t;
        if (tables.containsKey(tableName)) {
            t = tables.get(tableName);
        } else {
            throw new TableNotExistsException();
        }
        return t.getAttribute(attrName).size();
    }

    /**
     * 获取一个索引
     * @param name 索引名
     * @return 保存索引信息的SQLIndex变量
     */
    public static SQLIndex getIndex(String name) {
        if (indexes.containsKey(name)) {
            return indexes.get(name);
        } else {
            return null;
        }
    }

    /**
     * 检查插入数据类型，如果匹配则变换为相应的字节数组
     * @param tableName 对应表名
     * @param values 对应各个数据
     * @return 表中每个数据根据类型转换为字节拼成字节数组, 类型不匹配返回null
     */
    public static byte[] ValueAttrCheck(String tableName, Vector<String> values) {
        SQLTable table = getTable(tableName);
        return table.valueAttrCheck(values);
    }

    /**
     * 检查一个数据的类型，如果匹配则返回相应的字节数组
     * @param value 数据
     * @param attr 属性
     * @return 转换的字节数组，类型不匹配则返回null
     */
    public static byte[] checkType(String value, SQLAttribute attr) {
        return attr.checkType(value);
    }

    /*
    public static Comparable convertFromRecord(String tableName, String attrName, byte[] src)
        throws TableNotExistsException, AttributeNotExistsException {
        if (tables.containsKey(tableName)) {
            SQLTable table = tables.get(tableName);
            SQLAttribute attr = table.getAttribute(attrName);
            if (attr != null) {
                byte[] tmp = new byte[attr.size()];
                System.arraycopy(src, attr.getOffset(), tmp, 0, tmp.length);
                return attr.convert(tmp);
            } else {
                throw new AttributeNotExistsException();
            }
        } else {
            throw new TableNotExistsException();
        }
    } */

    /**
     * 根据内存中信息重建db相应xml的doc树
     */
    public static void updateDBDoc() {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = dbf.newDocumentBuilder();
            Document doc = builder.newDocument();
            Element root = doc.createElement("database");
            root.setAttribute("name", "test");
            for (SQLTable t : tables.values()) {
                Element te = doc.createElement("table");
                te.setAttribute("name", t.getName());
                for (SQLAttribute a : t.attributeCollection()) {
                    Element ae = doc.createElement("attribute");
                    ae.setAttribute("name", a.getName());
                    ae.setAttribute("type", a.getType().getName());
                    ae.setAttribute("index", a.getIndex());
                    ae.setAttribute("unique", String.valueOf(a.isUnique()));
                    ae.setAttribute("length", String.valueOf(a.size()));
                    te.appendChild(ae);
                }
                root.appendChild(te);
            }
            doc.appendChild(root);
            dbDoc = doc;
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
    }

    /**
     * 根据内存中信息重建index对应xml的doc树
     */
    public static void updateIndexDoc() {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = dbf.newDocumentBuilder();
            Document doc = builder.newDocument();
            Element root = doc.createElement("database");
            root.setAttribute("name", "test");
            for (SQLIndex i : indexes.values()) {
                Element ie = doc.createElement("index");
                ie.setAttribute("name", i.getName());
                ie.setAttribute("base_table", i.getBaseTabel().getName());
                ie.setAttribute("base_attribute", i.getBaseAttribute().getName());
                ie.setAttribute("root_index", String.valueOf(i.getRootIndex()));
                root.appendChild(ie);
            }
            doc.appendChild(root);
            indexDoc = doc;
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
    }

    /**
     * 根据doc树重新储存到xml文件
     */
    public static void saveDB() {
        updateDBDoc();
        updateIndexDoc();
        String path = System.getProperty("user.dir");
        TransformerFactory tf = TransformerFactory.newInstance();
        try {
            /*-------------Save dbDoc----------------------*/
            /* Get output stream */
            File catalogFile = new File(path, "db_catalog.xml");
            if (!catalogFile.exists()) {
                catalogFile.createNewFile();
            }
            FileOutputStream out = new FileOutputStream(catalogFile);

            /* Get the xml output format */
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            DOMSource source = new DOMSource(dbDoc);
            StreamResult result = new StreamResult();
            result.setOutputStream(out);
            transformer.transform(source, result);

            /*--------------Save indexDoc------------------*/
            catalogFile = new File(path, "index_catalog.xml");
            if (!catalogFile.exists()) {
                catalogFile.createNewFile();
            }
            out = new FileOutputStream(catalogFile);

            source = new DOMSource(indexDoc);
            result.setOutputStream(out);
            transformer.transform(source, result);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        }
    }
}
