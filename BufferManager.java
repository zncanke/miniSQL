import java.util.*;
import java.io.*;

class BlockIsTooSmall extends Exception{}

class BufferManager
{
	final static int sizeOfBlock = 8192;
	final static int limitOfBlock = 65536;
	final static int blockHeader = 8;
	final static int fileHeader = 4;
	final static int emptyPosStart = 4;
	final static int totRecordStart = 0;
	static HashMap<Key, Block> inqueue;
	static Block head, tail;
	static Block using;
	public static void Initial()
	{
		head = null;
		tail = null;
		using = null;
		inqueue = new HashMap<Key, Block>();
		inqueue.clear();
	}
	private static void InsertNode(Block a, Block b)
	{
		Block t = a.next;
		b.next = t;
		a.next = b;
		b.pre = a;
		if (t != null)
			t.pre = b;
	}
	private static void RemoveNode(Block a)
	{
		Block p, n;
		p = a.pre;
		n = a.next;
		if (a == p)
		{
			head = tail = null;
			return;
		}
		p.next = n;
		n.pre = p;
		if (head == a)
			head = n;
		if (tail == a)
			tail = p;
	}
	private static Block LRU()
	{
		Block tmp = head;
		while (true)
		{
			if (tmp.using)
				continue;
			if (!tmp.referenceBit)
				return tmp;
			else
				tmp.referenceBit = false;
			tmp = tmp.next;
		}
	}
	public static void WriteOneBlock(String fileName, int pblock, byte[] b) throws IOException
	{
		File f = new File(System.getProperty("user.dir"), fileName);
		RandomAccessFile fout = new RandomAccessFile(f, "rws");
		fout.seek((pblock-1)*sizeOfBlock + fileHeader);
		fout.write(b);
		fout.close();
	}
	public static Block LoadBlock(String tableName, int rankOfBlock) throws IOException
	{
		//System.out.println("enterLoadBlock");
		Block tmp;
		if ((tmp = inqueue.get(new Key(tableName, rankOfBlock))) != null)
		{
			if (using != null)
				using.using  = false;
			tmp.using = true;
			using = tmp;
			return tmp;
		}
		//System.out.println("flag1");
		File srcFile = new File(System.getProperty("user.dir"), tableName);
		RandomAccessFile in = new RandomAccessFile(srcFile, "r");
		//System.out.println(numOfRecordInBlock);
		in.seek((rankOfBlock - 1) * sizeOfBlock + fileHeader);
		byte[] tb = new byte[sizeOfBlock];
		in.read(tb);
		in.close();
		//System.out.println(inqueue.size());
		//System.out.println(tableName+" "+rankOfBlock);
		if (inqueue.size() == limitOfBlock)
		{
			Block out = LRU();
			//System.out.println(out.tableName+" "+out.num+" ");
			if (out.dirty || out.node != null)
			{
				out.beforeWriteBack();
				WriteOneBlock(out.tableName, out.num, out.getContent());
			}
			RemoveNode(out);
			//System.out.print(out.tableName+" "+out.num+" ");
			inqueue.remove(new Key(out.tableName, out.num));
			InsertNode(tail, new Block(tableName, rankOfBlock, tb));
			tail = tail.next;
		}else
		{
			//System.out.println("flag3");
			if (head == null)
			{
				head = tail = new Block(tableName, rankOfBlock, tb);
				head.next = head. pre = tail;
				tail.next = tail.pre = head;
			}
			else
			{
				InsertNode(tail, new Block(tableName, rankOfBlock, tb));
				tail = tail.next;
			}
		}
		inqueue.put(new Key(tableName, rankOfBlock), tail);
		if (using != null)
			using.using = false;
		tail.using = true;
		using = tail;
		return tail;
	}
	public static void WriteBackAll() throws IOException
	{
		inqueue.clear();
		if (head == null) return;
		while (true)
		{
			if (head.dirty || head.node != null)
			{
				head.beforeWriteBack();
				WriteOneBlock(head.tableName, head.num, head.getContent());
			}
			if (head == tail) break;
			head = head.next;
		}
	}
	public static byte[] GetAttr(String tableName, byte[] b, String attrName)
		throws TableNotExistsException, IOException, AttributeNotExistsException
	{
		int off = CatalogManager.getOffsetOfAttr(tableName, attrName);
		int len = CatalogManager.getLengthOfAttr(tableName, attrName);
		//System.out.println("offset: "+off);
		//System.out.println("length: "+len);
		byte[] res = new byte[len];
		System.arraycopy(b, off, res, 0, len);
		return res;
	}
	public static boolean CreateNewFile(String tableName)
		throws IOException
	{
		File srcFile = new File(System.getProperty("user.dir"), tableName);
		//System.out.println("1");
		RandomAccessFile out = new RandomAccessFile(srcFile, "rws");
		out.write(Util.Int2ByteArr(0));
		out.close();
		return true; 
	}
	public static boolean DeleteFile(String tableName)
	{
		File f = new File(System.getProperty("user.dir"), tableName);
		if (f.exists())
			f.delete();
		return true;
	}
	public static int CalcOffInFile(String tableName, int rankOfBlock, int k)
		throws TableNotExistsException
	{
		int sizeOfRecord = CatalogManager.SizeOfRecord(tableName);
		return fileHeader + (rankOfBlock - 1) * sizeOfBlock + blockHeader + (k - 1) * sizeOfRecord;
	}
	public static int GetTotBlock(String tableName) throws IOException
	{
		File srcFile = new File(System.getProperty("user.dir"), tableName);
		RandomAccessFile in = new RandomAccessFile(srcFile, "r");
		byte[] res = new byte[fileHeader];
		in.read(res);
		in.close();
		return Util.ByteArr2Int(res);
	}
	public static int GetTotRecord(Block b)
	{
		int res;
		res = Util.ByteArr2Int(b.getValue(0, 4));
		return res;
	}
	public static int GetFirstEmptyPos(Block b)
	{
		int res;
		res = Util.ByteArr2Int(b.getValue(4, 4));
		return res;
	}
	public static void SetEmptyPos(Block b, int p)
	{
		b.setValue(Util.Int2ByteArr(p), emptyPosStart, 4);
	}
	public static void SetTotRecord(Block b, int p)
	{
		b.setValue(Util.Int2ByteArr(p), totRecordStart, 4);
	}
	public static int GetOffRecord(String tableName, int p)
		throws TableNotExistsException
	{
		int sizeOfRecord = CatalogManager.SizeOfRecord(tableName);
		return (p - 1) * sizeOfRecord + blockHeader;
	}
	public static int GetNextEmptyPos(Block b, int off)
	{
		byte[] res = new byte[4];
		System.arraycopy(b.getValue(off, 4), 0, res, 0, 4);
		return Util.ByteArr2Int(res);
	}
	public static byte[] GetRecord(String tableName, Block b, int k)
		throws TableNotExistsException
	{
		int sizeOfRecord = CatalogManager.SizeOfRecord(tableName);
		int p = (k - 1) * sizeOfRecord + blockHeader;
		return b.getValue(p, sizeOfRecord);
	}
	public static void WriteIntoRecord(String tableName, Block b, int k, final byte[] rb)
		throws TableNotExistsException
	{
		int sizeOfRecord = CatalogManager.SizeOfRecord(tableName);
		int p = (k - 1) * sizeOfRecord + blockHeader;
		b.setValue(rb, p, rb.length);
	}
	public static void WriteIntoRecord(Block b, int off, final byte[] val)
	{
		b.setValue(val, off, val.length);
	}
	public static Block CreateNewBlock(String tableName) 
		throws IOException, TableNotExistsException
	{
		File srcFile = new File(System.getProperty("user.dir"), tableName);
		RandomAccessFile out = new RandomAccessFile(srcFile, "rws");

		int tot = GetTotBlock(tableName);
		out.seek(tot*sizeOfBlock + fileHeader);
		//out.seek(tot*10+fileHeader);
		out.write(Util.Int2ByteArr(0));
		out.write(Util.Int2ByteArr(blockHeader));
		tot++;
		out.seek(0);
		out.write(Util.Int2ByteArr(tot));
		out.close();
		//System.out.println("Enter_CreateNewBlock");
		return LoadBlock(tableName, tot);
	}
	public static void RemoveAllNodeOfTable(String tableName)
	{
		if (using != null && using.tableName.equals(tableName))
			using = null;
		Iterator iter = inqueue.entrySet().iterator();
		while (iter.hasNext())
		{
			Map.Entry entry = (Map.Entry)iter.next();
			Key key = (Key)entry.getKey();
			Block val = inqueue.get(key);
			if (key.s.equals(tableName))
			{
				iter.remove();
				RemoveNode(val);
			}
		}
	}
}