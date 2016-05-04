//import com.sun.tools.doclint.HtmlTag;

import java.awt.image.BufferedImage;
import java.util.*;
import java.io.*;

class API
{
	/**
	 * 数据库初始化
	 */
	public static void Initial()
	{
		BufferManager.Initial();
		CatalogManager.initDb("test");
	}

	/**
	 * 关闭数据库，执行写回
	 * @throws IOException
	 */
	public static void Close() throws IOException
	{
		BufferManager.WriteBackAll();
		CatalogManager.saveDB();
	}

	/**
	 * 检查表是否存在
	 * @param tableName 表名
	 * @return 是否存在
	 */
	public static boolean TableInDatabase(String tableName)
	{
        if (CatalogManager.getTable(tableName) == null) {
            return false;
        } else {
            return true;
        }
	}

	/**
	 * 检查索引是否存在
	 * @param indexName 索引名
	 * @return 是否存在
	 */
	public static boolean IndexInDatabase(String indexName)
	{
		return CatalogManager.getIndex(indexName) != null;
	}

	/**
	 * 检查表是否含有指定属性
	 * @param tableName 表名
	 * @param attrName 属性名
	 * @return 是否包含
	 */
	public static boolean AttrInTable(String tableName, String attrName)
	{
        if (CatalogManager.getTable(tableName).getAttribute(attrName) == null) {
            return false;
        } else {
            return true;
        }
	}

	/**
	 * 转换数据
	 * @param tableName 对应表
	 * @param value 各个数据的对应字符串
	 * @return 转换得到的字节数组
	 */
	public static byte[] ValueAttrCheck(String tableName, Vector<String> value)
	{
        return CatalogManager.ValueAttrCheck(tableName, value);
	}

	/**
	 * 删除一个表
	 * @param tableName 表名
	 * @throws TableNotExistsException
	 */
	public static void DropTable(String tableName) throws TableNotExistsException
	{
		CatalogManager.dropTable(tableName);
	}

	/**
	 * 删除一个索引
	 * @param indexName 索引名
	 * @throws IndexNotExistsException
	 */
	public static void DropIndex(String indexName) throws IndexNotExistsException
	{
		CatalogManager.dropIndex(indexName);
	}

	/**
	 * 创建表
	 * @param tableName 表名
	 * @param attrs 表所包含的属性
	 * @throws SyntaxError
	 * @throws IOException
	 */
	public static void CreateTable(String tableName, Vector<SQLAttribute> attrs) throws SyntaxError, IOException
	{
		if (TableInDatabase(tableName))
			throw new SyntaxError("This table has existed.");
		CatalogManager.createTable(tableName, attrs);
		/*System.out.println(tableName);
		Iterator iter = attr.entrySet().iterator();
		while (iter.hasNext())
		{
			Map.Entry entry = (Map.Entry) iter.next();
			System.out.print(entry.getKey() + ":");
			SQLAttribute tmp = (SQLAttribute)entry.getValue();
			System.out.println(tmp.getType());
		}*/
	}

	/**
	 * 创建索引
	 * @param indexName 索引名
	 * @param tableName 表名
	 * @param attrName 属性名
	 * @throws IOException
	 */
	public static void CreateIndex(String indexName, String tableName, String attrName)
		throws IOException, TableNotExistsException, AttributeNotExistsException, NotUniqueException, SyntaxError
	{
		//try {
			if (IndexInDatabase(indexName)) {
				throw new SyntaxError("Index has already been established.");
			}
			SQLAttribute attr = CatalogManager.getAttribute(tableName, attrName);
			if (!attr.getIndex().equals("")) {
				throw new SyntaxError("Attribute " + attrName + " in table " + tableName + " already has index: " + attr.getIndex());
			}
			CatalogManager.addIndex(indexName, tableName, attrName);
			Index index = CatalogManager.getIndex(indexName).getIndex();
			index.init();
			int[] num = new int[1];
			num[0] = 0;
			while (true)
			{
				HashMap<Comparable, Integer> res = RecordManager.Traverse(tableName, attr, num);
				if (res == null)
					break;
				for (Map.Entry<Comparable, Integer> e : res.entrySet()) {
					index.insertOrUpdate(e.getKey(), e.getValue());
				}
			}
		/*} catch (TableNotExistsException e) {
			System.err.println("Table not exists");
		} catch (AttributeNotExistsException e) {
			System.err.println("Attribute not exists");
		} catch (NotUniqueException e) {
			System.err.println("Attribute is not unique");
		}*/
	}
	public static void InsertRecord(String tableName, Vector<String> value)
		throws IOException, TableNotExistsException, SizeOfRecordIsTooLarge, SyntaxError
	{
		if (!TableInDatabase(tableName))
			throw new TableNotExistsException();
		byte[] b = ValueAttrCheck(tableName, value);
		if (b == null)
			throw new SyntaxError("Please check the format of record.");
		SQLTable table = CatalogManager.getTable(tableName);
		Vector<SQLAttribute> toCheck = table.checkIndex(b);
		if (toCheck == null) {
			throw new SyntaxError("Repeat values on unique attribute");
		} else {
			try {
				if (RecordManager.HaveRepeat(tableName, b, toCheck)) {
					throw new SyntaxError("Repeat values on unique attribute");
				}
			} catch (AttributeNotExistsException e) {
				e.printStackTrace();
			}
		}
		table.addRecordWithIndex(b, RecordManager.Insert(tableName, b));
	}
	public static int DeleteRecord(String tableName, Vector<Object> condition)
		throws TableNotExistsException, IOException, AttributeNotExistsException
	{
		if (!TableInDatabase(tableName))
			throw new TableNotExistsException();
		int tot = 0;
		int[] num = new int[1];
		num[0] = 0;
		SQLTable table = CatalogManager.getTable(tableName);
		while (true)
		{
			Vector<byte[]> res = RecordManager.Delete(tableName, condition, num);
			if (res == null)
				break;
			for (byte[] b : res) {
				table.delRecordWithIndex(b);
			}
			tot += res.size();
		}
		return tot;
	}
	public static void SelectRecord(String tableName, Vector<Object> condition)
		throws TableNotExistsException, IOException, AttributeNotExistsException
	{
		if (!TableInDatabase(tableName))
			throw new TableNotExistsException();
		Vector<String> res = showRecord(tableName, RecordManager.Select(tableName, condition));
		CatalogManager.getTable(tableName).showTitle();
		for (int i = 0; i < res.size(); i++)
			System.out.println(res.elementAt(i));
	}

	/**
	 * 格式化查询结果
	 * @param tableName 结果对应表名
	 * @param src 要显示的数据
	 * @return 格式化的字符串集
	 */
	public static Vector<String> showRecord(String tableName, Vector<byte[]> src) {
		Vector<String> res = new Vector<String>();
		SQLTable table = CatalogManager.getTable(tableName);
		for (byte[] t : src) {
			res.add(table.show(t));
		}
		return res;
	}
}