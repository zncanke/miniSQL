import java.io.*;
import java.util.*;

class SizeOfRecordIsTooLarge extends Exception{}

class RecordManager
{
	public static HashMap<Comparable, Integer> Traverse(String tableName, SQLAttribute attr, int[] num)
		throws IOException, TableNotExistsException, AttributeNotExistsException
	{
		num[0]++;
		int totBlock = BufferManager.GetTotBlock(tableName);
		if (num[0] > totBlock)
			return null;
		HashMap<Comparable, Integer> res = new HashMap<Comparable, Integer>();
		res.clear();
		Block b = BufferManager.LoadBlock(tableName, num[0]);
		int tot = BufferManager.GetTotRecord(b);
		for (int i = 0, now = 1; i < tot; now++)
		{
			byte[] rb;
			rb = BufferManager.GetRecord(tableName, b, now);
			if (rb[rb.length-1] == 0)
				continue;
			i++;
			byte[] tb = BufferManager.GetAttr(tableName, rb, attr.name);
			Comparable tmp = (Comparable)attr.convert(tb);
			int off = BufferManager.CalcOffInFile(tableName, num[0], now);
			res.put(tmp, off);
		}
		return res;
	}

	public static boolean HaveRepeat(String tableName, byte[] val, Vector<SQLAttribute> vattr)
		throws IOException, TableNotExistsException, AttributeNotExistsException
	{
		if (vattr.size() == 0)
			return false;
		Vector<byte[]> st = new Vector<byte[]>();
		st.clear();
		for (int i = 0; i < vattr.size(); i++)
		{
			st.add(BufferManager.GetAttr(tableName, val, vattr.elementAt(i).name));
		}
		int totBlock = BufferManager.GetTotBlock(tableName);
		for (int i = 0 ; i < totBlock; i++)
		{
			Block b = BufferManager.LoadBlock(tableName, i+1);
			int tot = BufferManager.GetTotRecord(b);
			for (int j = 0, now = 1; j < tot; now++)
			{
				byte[] rb;
				rb = BufferManager.GetRecord(tableName, b, now);
				if (rb[rb.length-1] == 0)
					continue;
				j++;
				for (int k = 0; k < vattr.size(); k++)
				{
					byte[] tmp = BufferManager.GetAttr(tableName, rb, vattr.elementAt(k).name);
					if (Arrays.equals(tmp, st.elementAt(k)))
						return true;
				}
			}
		}
		return false;
	}

	public static int Insert(String tableName, byte[] val)
		throws IOException, SizeOfRecordIsTooLarge, TableNotExistsException
	{
		int res;
		val[val.length-1] = 1;
		int totBlock = BufferManager.GetTotBlock(tableName);
		/*判重
		*/
		for (int i = 0; i < totBlock; i++)
		{
			Block b = BufferManager.LoadBlock(tableName, i + 1);
			int off = BufferManager.GetFirstEmptyPos(b);
			int tot = BufferManager.GetTotRecord(b);
			if (off == BufferManager.sizeOfBlock)
				continue;
			if (BufferManager.sizeOfBlock - off < val.length)
				continue;

			res = BufferManager.fileHeader + i * BufferManager.sizeOfBlock + off;

			tot++;
			int tmp = BufferManager.GetNextEmptyPos(b, off);
			
			if (tmp == 0)
				tmp = off + val.length;
			BufferManager.SetEmptyPos(b, tmp);
			BufferManager.SetTotRecord(b, tot);
			BufferManager.WriteIntoRecord(b, off, val);
			return res;
		}
		//System.out.println("New_Block");
		Block b = BufferManager.CreateNewBlock(tableName);
		int off = BufferManager.GetFirstEmptyPos(b);

		//System.out.println(b + " " + off);

		if (val.length > BufferManager.sizeOfBlock - off)
			throw new SizeOfRecordIsTooLarge();

		res = BufferManager.fileHeader + b.num * BufferManager.sizeOfBlock + off;

		//System.out.println("After_SizeOfRecordIsTooLarge");		
		byte[] tmp = Util.Int2ByteArr(val.length + BufferManager.blockHeader);
		/*for (int i = 0; i < 4; i++)
			System.out.println(tmp[i]);*/
		b.setValue(Util.Int2ByteArr(1), BufferManager.totRecordStart, 4);
		b.setValue(tmp, BufferManager.emptyPosStart, tmp.length);
		b.setValue(val, off, val.length);

		return res;
	}

	/*public static void DropTable(String tableName)
	{
		BufferManager.RemoveAllNodeOfTable(tableName);
		File f = new File(tableName);
		if (f.exists())
			f.delete();
	}*/

	public static boolean CheckCondition(String tableName, final byte[] b, Vector<Object> condition)
		throws TableNotExistsException, IOException, AttributeNotExistsException
	{
		if (condition == null)
			return true;
		Stack<Boolean> s = new Stack<Boolean>();
		while (!s.empty())
			s.pop();
		for (int i = 0; i < condition.size(); i++)
		{
			if (condition.elementAt(i) instanceof Integer)
			{
				if ((Integer)condition.elementAt(i) == 1)
				{
					s.push(s.pop() & s.pop());
				}else
				{
					s.push(s.pop() | s.pop());
				}
			}
			else
			{
				Subcondition c;
				c = (Subcondition)condition.elementAt(i);
				byte[] tb = BufferManager.GetAttr(tableName, b, c.left);
				Comparable tmp1 = (Comparable)c.attr.convert(c.right);
				Comparable tmp0 = (Comparable)c.attr.convert(tb);
				int res = tmp0.compareTo(tmp1);
				switch (c.op)
				{
					case 0:
						s.push(res < 0);
					break;
					case 1:
						s.push(res == 0);
					break;
					case 2: 
						s.push(res > 0);
					break;
					case 3: 
						s.push(res <= 0);
					break;
					case 4:
						s.push(res >= 0);
					break;
					case 5:
						s.push(res != 0);
					break;
				}
			}
		}
		return s.pop();
	}

	public static Vector<byte[]> Select(String tableName, Vector<Object> condition)
		throws TableNotExistsException, IOException, AttributeNotExistsException
	{
		Vector<byte[]> res = new Vector<byte[]>();
		res.clear();
		int totBlock = BufferManager.GetTotBlock(tableName);
		for (int i = 0; i < totBlock; i++)
		{
			Block b = BufferManager.LoadBlock(tableName, i+1);
			int totRecord = BufferManager.GetTotRecord(b);
			for (int j = 0, now = 1; j < totRecord; now++)
			{
				byte[] rb;
				rb = BufferManager.GetRecord(tableName, b, now);
				if (rb[rb.length-1] == 0)
					continue;
				j++;
				if (CheckCondition(tableName, rb, condition))
				{
					res.add(rb);
				}
			}
		}
		return res;
	}

	public static Vector<byte[]> Delete(String tableName, Vector<Object> condition, int[] num)
		throws IOException, TableNotExistsException, AttributeNotExistsException
	{
		num[0]++;
		Vector<byte[]> res = new Vector<byte[]>();
		int totBlock = BufferManager.GetTotBlock(tableName);
		if (num[0] > totBlock)
			return null;
		int i = num[0];
		//for (int i = 0; i < totBlock; i++)
		//{
			Block b = BufferManager.LoadBlock(tableName, i);
			int totRecord = BufferManager.GetTotRecord(b);
			int emptyPos = BufferManager.GetFirstEmptyPos(b);
			int tot = totRecord;
			for (int j = 0, now = 1; j < totRecord; now++)
			{
				byte[] rb;
				rb = BufferManager.GetRecord(tableName, b, now);
				if (rb[rb.length-1] == 0)
					continue;
				j++;
				if (CheckCondition(tableName, rb, condition))
				{
					rb[rb.length-1] = 0;

					byte[] tt = new byte[rb.length];
					System.arraycopy(rb, 0, tt, 0, rb.length);
					res.add(tt);
					
					System.arraycopy(Util.Int2ByteArr(emptyPos), 0, rb, 0, 4);
					BufferManager.WriteIntoRecord(tableName, b, now, rb);
					int p = BufferManager.GetOffRecord(tableName, now);
					p = BufferManager.GetNextEmptyPos(b, p);
					if (p == 0)
						p = p + rb.length;
					emptyPos = p;
					tot--;
				}

			}
			BufferManager.SetEmptyPos(b, emptyPos);
			BufferManager.SetTotRecord(b, tot);
		//}
		return res;
	}
}