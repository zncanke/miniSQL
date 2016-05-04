import java.util.regex.*;
import java.util.*;
import java.io.*;

class ExitSQL extends Throwable{}
class SyntaxError extends Throwable
{
	public String msg;
	SyntaxError(String s)
	{
		msg = s;
	}
}

class Interpreter
{
	static boolean state = false;
	public static void OpenSQL()
	{
		System.out.println("*******************Welcome to our MiniSQL**********************");
		state = true;
		API.Initial();
	}
	public static void CloseSQL()
	{
		state = false;
		try
		{
			API.Close();
		}catch (IOException e)
		{
			System.err.println("Close files error.");
		}
		System.out.println("**********************Thanks for using*************************");
	}
	public static String GetInst()
	{
		StringBuffer inst = new StringBuffer("");
		boolean flag = false;
		do
		{
			if (flag)
				System.out.print("     ->");
			else
				System.out.print("minSQL>");
			Scanner scan = new Scanner(System.in);
			String s = scan.nextLine();
			if (s.trim().equals(""))
				continue;
			//System.err.println(":"+s);
			if (!flag)
				flag = true;
			inst.append(" " + s);
		}while (inst.toString().equals("") || inst.toString().trim().charAt(inst.toString().trim().length() - 1) != ';');
		//System.out.println();
		//System.out.println("GetInstEnd");
		return inst.toString().trim().toLowerCase();
	}
	public static Vector<String> CheckCreate(String inst) throws SyntaxError
	{
		Vector<String> res = new Vector<String>();
		res.clear();
		if (Pattern.matches("^create\\s+index\\s+[a-zA-Z][a-zA-Z0-9_]*\\s+on\\s+[a-zA-Z][a-zA-Z0-9_]*\\s*\\(\\s*[a-zA-Z][a-zA-Z0-9_]*\\s*\\)\\s*;", inst))
		{
			//System.out.println("CheckCreate_Index");
			res.add("create");
			res.add("index");
			int p0 = inst.indexOf("index") + 5;
			int p1 = inst.indexOf(" on ");
			String indexName = inst.substring(p0, p1).trim();
			if (indexName.indexOf(' ') >= 0 || indexName.equals(""))
				throw new SyntaxError("The name of index cannot be empty.");
			res.add(indexName);
			res.add("on");
			p0 = inst.indexOf('(', p1);
			//System.err.println(p1);
			String tableName = inst.substring(p1+4, p0).trim();
			//System.err.println(tableName);
			if (tableName.equals(""))
				throw new SyntaxError("The name of table cannot be empty.");
			res.add(tableName);
			p1 = inst.indexOf(')', p0);
			res.add(inst.substring(p0 + 1, p1).trim());
		}else
		if (Pattern.matches("^create\\s+table\\s+[a-zA-Z][a-zA-Z0-9_]*\\s*\\(.*\\)\\s*;", inst))
		{
			//System.out.println("CheckCreate_Table");
			res.add("create");
			res.add("table");
			int pp = inst.indexOf("table") + 5;
			int p0 = inst.indexOf('(');
			String tableName = inst.substring(pp, p0).trim();
			if (tableName.equals(""))
				throw new SyntaxError("Remember input the name of table.");
			res.add(tableName);
			int p1 = inst.lastIndexOf(')', inst.length() - 1);
			if (inst.substring(p0 + 1, p1).trim().equals(""))
				throw new SyntaxError("The definition of attributes cannot be empty.");
			Pattern pa = Pattern.compile(",");
			String[] s = pa.split(inst.substring(p0 + 1, p1).trim());
			boolean pkey = false;
			for (String t : s)
			{
				//System.out.println(t);
				if (t.trim().equals(""))
					throw new SyntaxError("The definition of attributes cannot be empty.");
				if (Pattern.matches("primary\\s+key\\s*\\(\\s*[a-zA-Z][a-zA-Z0-9_]*\\s*\\)", t.trim()))
				{
					if (!pkey)
					{
						pkey = true;
						res.add(t.trim());
						continue;
					}else
					throw new SyntaxError("One table can only have one primary key at most.");
				}
				if (!Pattern.matches("^[a-zA-Z][a-zA-Z0-9_]*\\s+(int|float|char\\s*\\(\\s*.+\\s*\\))(\\s+unique)?", t.trim()))
					throw new SyntaxError("Please check the definition of attributes.");
				p0 = t.indexOf('(');
				if (p0 < 0)
				{
					res.add(t.trim());
					continue;
				}
				p1 = t.lastIndexOf(')', t.length());
				String tt = t.substring(p0 + 1, p1).trim();
				try
				{
					int x = Integer.parseInt(tt);
					if (!(x <= 255 && x >= 1))
						throw new SyntaxError("The length of char(x) must be between 1 and 255.");
				}catch (Exception e)
				{
					throw new SyntaxError("The definition of char(x), x must be a number between 1 and 255.");
				}
				res.add(t.trim());
			}
		}else
			throw new SyntaxError("Please check this instruction.");
		return res;
	}
	public static Vector<String> CheckDrop(String inst) throws SyntaxError
	{
		Vector<String> res = new Vector<String>();
		res.clear();
		if (!Pattern.matches("^drop\\s+(table|index)\\s+[a-zA-Z][a-zA-Z0-9_]*\\s*;", inst))
			throw new SyntaxError("Please check this instruction.");
		Pattern p = Pattern.compile("[\\s]");
		String[] s = p.split(inst.substring(0, inst.length()-1));
		for (String t : s)
		{
			if (t.trim().equals(""))
				continue;
			switch (res.size())
			{
				case 0: case 1: case 2: 
					res.add(t); 
				break; 
				default: 
					throw new SyntaxError("You input too many parts.");
			}
		}
		return res;
	}
	private static Vector<String> ResolveWhere(String inst) throws SyntaxError
	{
		Vector<String> res = new Vector<String>();
		res.clear();
		int p = inst.indexOf(' ');
		if (!inst.substring(0, p).equals("where"))
			throw new SyntaxError("Make sure the position of \'where\'.");
		//System.out.println("ResolveWhere_AfterFindWhere");
		res.add("where");

		StringBuffer temp = new StringBuffer("");
		boolean inString = false;
		int tot = 0;
		for (int i = p; i < inst.length(); i++)
		{
			//System.out.println(temp);
			char c = inst.charAt(i);
			if (c != ' ' && c != '\'' && c != '<' && c != '=' && c != '>' && c != ';')
			{
				temp.append(c);
				continue;
			}
			if (c != '\'' && inString)
			{
				temp.append(c);
				continue;
			}
			if (c == '\'')
				if (!inString)
					inString = true;
				else
				{
					inString = false;
					temp.append(c);
					if (inst.charAt(i+1) != ' ' && inst.charAt(i+1) != ';')
						throw new SyntaxError("You should input spaces in proper position.");
				}
			if (c == ';' && i != inst.length() - 1)
				throw new SyntaxError("\';\' should be input last.");
			if (!temp.toString().equals(""))
			{
				//System.out.println("ResolveWhere_Judge:" + c);
				switch (tot % 4)
				{
					case 0:
						if (!Pattern.matches("^[a-zA-Z][a-zA-Z0-9_]*", temp))
							throw new SyntaxError("Pleast check the name of attributes.");
						res.add(temp.toString());
					break;
					/*case 1:
						if (!Pattern.matches("(<|>|=|<=|>=)", temp))
							throw new SyntaxError();
						res.add(temp.toString());
					break;*/
					case 2:
						res.add(temp.toString());
					break;
					case 3:
						if (!Pattern.matches("(and|or)", temp))
							throw new SyntaxError("You should input \'and\' or \'or\' to link two conditions.");
						res.add(temp.toString());
					break;
				}
				tot++;
			}
			temp.delete(0, temp.length());
			if (c == '\'' && inString == true)
				temp = temp.append(c);
			if (c == '<' || c == '>' || c == '=')
			{
				char cn = inst.charAt(i+1);
				if (tot % 4 == 1)
				{
					temp.append(c);
					if (c == '<')
					{
						if (cn == '>' || cn == '=')
						{
							temp.append(cn);
							i++;
						}
					}
					else
					if (c == '>')
						if (cn == '=')
						{
							temp.append(cn);
							i++;
						}
					res.add(temp.toString());
				}else
					throw new SyntaxError("Comparison operators should input between two operands.");
				tot++;
				temp.delete(0, temp.length());
			}
		}
		//System.out.println(tot);
		if (inString)
			throw new SyntaxError("Please check the uses of \'.");
		if (tot % 4 != 3)
			throw new SyntaxError("Please check the completeness of each subcondition.");
		return res;
	}
	public static Vector<String> CheckSelect(String inst) throws SyntaxError
	{
		Vector<String> res = new Vector<String>();
		res.clear();
		if (!Pattern.matches("^select\\s+\\*\\s+from\\s+[a-zA-Z][a-zA-Z0-9_]*.*;", inst))
			throw new SyntaxError("Please check this instruction.");
		else
		{
			//System.out.println("CheckSelect_AfterBasicMatch");
			int p = inst.indexOf(" where ");
			if (p < 0)
				p = inst.length() - 1;
			String s1 = inst.substring(0, p);
			Pattern pa = Pattern.compile("[\\s]");
			String[] s = pa.split(s1);
			for (String t : s)
			{
				if (t.trim().equals(""))
					continue;
				switch (res.size())
				{
					case 0: case 1: case 2: case 3:
						res.add(t); 
						//System.out.println("CheckSelect_AddBasic" + res.size());
					break;
					default:
						throw new SyntaxError("You input too many parts.");
				}
			}
			if (res.size() < 4)
				throw new SyntaxError("Please make sure the completeness of this instruction.");
			if (p == inst.length() - 1)
			{

				return res;
			}
			String s2 = inst.substring(p + 1, inst.length());
			//System.out.println("CheckSelect_BeforeEnterResolveWhere");
			res.addAll(ResolveWhere(s2));
		}
		return res;
	}
	public static Vector<String> CheckInsert(String inst) throws SyntaxError
	{
		//System.out.println("EnterCheckInsert");
		Vector<String> res = new Vector<String>();
		res.clear();

		int p0 = inst.indexOf('(');
		int p1 = inst.lastIndexOf(')', inst.length() - 1);

		if (p0 < 0 || p1 < 0)
			throw new SyntaxError("Please check this instruction.");
		//System.out.println("CheckInsert0:"+inst.substring(p1 + 1, inst.length()).trim());
		if (!inst.substring(p1 + 1, inst.length() - 1).trim().equals(""))
			throw new SyntaxError("The value of the record should not be empty.");

		String[] compare0 = {"", "into", "", "values"};
		String s1 = inst.substring(0, p0);
		String s2 = inst.substring(p0 + 1, p1);

		Pattern pa  = Pattern.compile("[\\s+]");
		String[] s = pa.split(s1);

		//System.out.println("CheckInsert1");

		for (String t : s)
		{
			if (t.trim().equals(""))
				continue;
			switch (res.size())
			{
				case 0: res.add(t); break;
				case 1: case 3:
					if (compare0[res.size()].equals(t))
						res.add(t);
					else
						throw new SyntaxError("Please check the instruction.");
				break;
				case 2:
					//if (t.equals(""))
						//throw new SyntaxError("The name of table cannot be empty.");
					if (Pattern.matches("^[a-zA-Z][a-zA-Z0-9_]*", t))
						res.add(t);
					else
						throw new SyntaxError("Please check the name of table.");
				break;
				default:
					throw new SyntaxError("You input too many parts.");
			}
		}

		if (res.size() < 4)
			throw new SyntaxError("Please check the instruction.");

		//System.out.println("CheckInsert2");

		/*Pattern pb = Pattern.compile(",");
		String[] ss = pb.split(s2);
		for (String t : ss)
		{
			if (t.trim().equals(""))
				throw new SyntaxError();
			res.add(t.trim());
		}*/
		StringBuffer temp = new StringBuffer("");
		for (int i = 0; i < s2.length(); i++)
		{
			char c = s2.charAt(i);
			if (c != ',')
			{
				temp.append(c);
				continue;
			}
			if (temp.toString().trim().equals(""))
				throw new SyntaxError("The value of attributes should not be empty.");
			res.add(temp.toString().trim());
			temp.delete(0, temp.length());
		}
		if (temp.toString().trim().equals(""))
			throw new SyntaxError("The value of attributes should not be empty.");
		res.add(temp.toString().trim());
		return res;
	}
	public static Vector<String> CheckDelete(String inst) throws SyntaxError
	{
		Vector<String> res = new Vector<String>();
		res.clear();
		if (!Pattern.matches("^delete\\s+from\\s+[a-zA-Z][a-zA-Z0-9_]*.*;", inst))
			throw new SyntaxError("Please check the instruction.");
		else
		{
			res.add("delete");
			res.add("from");
			int p0 = inst.indexOf("from") + 4;
			int p1 = inst.indexOf(" where ");
			if (p1 < 0)
				p1 = inst.length() - 1;
			String name = inst.substring(p0, p1).trim();
			if (name.indexOf(' ') >= 0 || name.equals(""))
				throw new SyntaxError("Please check the name of table.");
			else
				res.add(name);
			if (p1 != inst.length() - 1)
				res.addAll(ResolveWhere(inst.substring(p1 + 1, inst.length())));
		}
		return res;
	}
	public static Vector<String> CheckExecfile(String inst) 
		throws IOException, SyntaxError
	{
		Vector<String> res = new Vector<String>();
		res.clear();
		if (!Pattern.matches("^execfile\\s+[a-zA-Z][a-zA-Z0-9_]*.txt\\s*;", inst))
			throw new SyntaxError("Please check the instruction.");
		else
		{
			res.add("execfile");
			int p0 = inst.indexOf(' ');
			int p1 = inst.lastIndexOf(';');
			if (inst.substring(p0, p1).trim().equals(""))
				throw new SyntaxError("Please check the name of file.");
			res.add(inst.substring(p0, p1).trim());
		}
		return res;
	}
	public static Vector<String> Analyse(String inst) 
		throws ExitSQL, SyntaxError, IOException
	{
		if (Pattern.matches("^exit[\\s]*;", inst))
			throw new ExitSQL();
		int p = inst.lastIndexOf(';', inst.length());
		/*if (p != inst.length() - 1)
			throw new SyntaxError("Please check the position of ");*/
		p = inst.indexOf(' ');
		if (p < 0)
			throw new SyntaxError("Please check the instruciton.");
		String s = inst.substring(0, p);
		if (s.equals("create"))
			return CheckCreate(inst);
		else
		if (s.equals("drop"))
			return CheckDrop(inst);
		else
		if (s.equals("select"))
			return CheckSelect(inst);
		else
		if (s.equals("insert"))
			return CheckInsert(inst);
		else
		if (s.equals("delete"))
			return CheckDelete(inst);
		else
		if (s.equals("execfile"))
			return CheckExecfile(inst);
		else
			throw new SyntaxError("Please check the instruction.");
	}
	public static Vector<SQLAttribute> CreateDefAnalyse(Vector<String> inst, int beginIndex, StringBuffer pkey) throws SyntaxError
	{
		Vector<SQLAttribute> rr = new Vector<SQLAttribute>();
		rr.clear();
		HashMap<String, SQLAttribute> res = new HashMap<String, SQLAttribute>();
		res.clear();
		SQLAttribute tmp;
		for (int i = beginIndex; i < inst.size(); i++)
		{
			String s = inst.elementAt(i);
			int p = s.indexOf(' ');
			String attrName = s.substring(0, p);
			if (attrName.equals("unique"))
				throw new SyntaxError("The name of attribute should not be \'unique\'.");
			if (attrName.equals("primary"))
			{
				int l0 = s.indexOf('(');
				int l1 = s.indexOf(')');
				pkey.append(s.substring(l0+1, l1).trim());
				tmp = res.get(pkey.toString());
				if (tmp == null)
					throw new SyntaxError("Please check the definition of primary key.");
				tmp.setUnique(true);
				continue;
			}
			if (res.get(attrName) != null)
				throw new SyntaxError(String.format("Redefined the attribute %s.", attrName));
			int p1 = s.indexOf("unique");
			String attr;
			if (p1 >= 0)
				attr = s.substring(p, p1).trim();
			else
				attr = s.substring(p, s.length()).trim();
			if (attr.equals("int"))
			{
				res.put(attrName, tmp = new SQLAttribute(attrName, Integer.class, 4, (p1>=0), ""));
				rr.add(tmp);
			}
			else
			if (attr.equals("float"))
			{
				res.put(attrName, tmp = new SQLAttribute(attrName, Float.class, 4, (p1>=0), ""));
				rr.add(tmp);
			}
			else
			{
				int l1, l2;
				l1 = attr.indexOf('(');
				l2 = attr.indexOf(')');
				int len = Integer.parseInt(attr.substring(l1+1, l2).trim());
				res.put(attrName, tmp = new SQLAttribute(attrName, String.class, len+1, (p1>=0), ""));
				rr.add(tmp);
			}
		}
		return rr;
	}
	public static Vector<Object> ConvertWhere(String tableName, Vector<String> inst, int fromIndex, int toIndex)
		throws SyntaxError, AttributeNotExistsException, TableNotExistsException
	{
		Vector<Object> res = new Vector<Object>();
		res.clear();

		Stack<Integer> s = new Stack<Integer>();
		while (!s.empty()) s.pop();

		for (int i = fromIndex; i < toIndex; i++)
		{
			int t = i - fromIndex;
			if (t % 4 != 0 && t % 4 != 3) continue;
			if (t % 4 == 0)
			{
				Subcondition p;
				p = new Subcondition(tableName, inst.elementAt(i), inst.elementAt(i+1), inst.elementAt(i+2));
				res.add(p);
			}else
			{
				int op;
				if (inst.elementAt(i).equals("and"))
					op = 1;
				else
				if (inst.elementAt(i).equals("or"))
					op = 0;
				else
					throw new SyntaxError("You should use \'and\' or \'or\' to link two subconditons.");
				if (s.empty() || s.peek() <= op)
					s.push(op);
				else
				{
					while (!s.empty() && s.peek() > op)
						res.add(s.pop());
					s.push(op);
				}
			}
		}
		while (!s.empty())
			res.add(s.pop());
		return res;
	}
	public static void Execfile(Vector<String> inst)
		throws IOException, SyntaxError
	{
		File f = new File(inst.elementAt(1));
		if (!f.exists())
			throw new SyntaxError("Cannot find the file.");
		BufferedReader in = 
				new BufferedReader(
				new FileReader(f));
		String s;
		while ((s = in.readLine()) != null)
		{
			Work(s);
		}
	}
	public static int Work(String fs)
	{
		String instString;
		if (fs == null)
			instString = GetInst();
		else
			instString = fs;
		if (state)
		{
			try
			{
				Vector<String> inst = new Vector<String>(Analyse(instString));
				for (int i = 0; i < inst.size(); i++)
					System.out.print(inst.elementAt(i) + ' ');
				System.out.println();
				if (inst.elementAt(0).equals("create"))
				{
					if (inst.elementAt(1).equals("index"))
					{
						API.CreateIndex(inst.elementAt(2), inst.elementAt(4), inst.elementAt(5));
						System.out.println("Createindex Successed.");
					}else
					if (inst.elementAt(1).equals("table"))
					{
						StringBuffer pkey = new StringBuffer("");
						Vector<SQLAttribute> res = CreateDefAnalyse(inst, 3, pkey);
						//System.err.println(pkey);
						API.CreateTable(inst.elementAt(2), res);
						if (!pkey.toString().equals(""))
						{
							//System.err.println(pkey);
							API.CreateIndex("primary_key_of_"+inst.elementAt(2), inst.elementAt(2), pkey.toString());
						}
						System.out.println("Createtable Successed.");
					}else
					throw new SyntaxError("Please check the instruction.");
				}else
				if (inst.elementAt(0).equals("drop"))
				{
					if (inst.elementAt(1).equals("table"))
					{
						API.DropTable(inst.elementAt(2));
						System.out.println("Droptable Successed.");
					}
					else
					if (inst.elementAt(1).equals("index"))
					{
						API.DropIndex(inst.elementAt(2));
						System.out.println("DropIndex Successed.");
					}
					else
						throw new SyntaxError("Please check the instruction.");
				}else
				if (inst.elementAt(0).equals("insert"))
				{
					Vector<String> tmp = new Vector<String>();
					tmp.clear();
					for (int j = 4; j < inst.size(); j++)
						tmp.add(inst.elementAt(j));
					API.InsertRecord(inst.elementAt(2), tmp);
					System.out.println("InsertRecord Successed.");
				}else
				if (inst.elementAt(0).equals("select"))
				{
					if (inst.size() > 5)
					{
						Vector<Object> v = ConvertWhere(inst.elementAt(3), inst, 5, inst.size());
						API.SelectRecord(inst.elementAt(3), v);
					}
					else
						API.SelectRecord(inst.elementAt(3), null);
				}else
				if (inst.elementAt(0).equals("delete"))
				{
					if (inst.size() > 4)
					{
						Vector<Object> v = ConvertWhere(inst.elementAt(2), inst, 4, inst.size());
						int tot = API.DeleteRecord(inst.elementAt(2), v);
						System.out.println(tot + " record(s) have been deleted from table " + inst.elementAt(2));
					}
					else
					{
						int tot = API.DeleteRecord(inst.elementAt(2), null);
						System.out.println(tot + " record(s) have been deleted from table " + inst.elementAt(2));
					}
				}else
				if (inst.elementAt(0).equals("execfile"))
				{
					Execfile(inst);
				}
			}
			catch (ExitSQL e)
			{
				return 1;
			}
			catch (SyntaxError e)
			{
				System.err.println(e.msg);
			}
			catch (IOException e)
			{
				System.err.println("Met some file operating problems.");
			}
			catch (TableNotExistsException e)
			{
				System.err.println("The table does not exist in the database.");
			}
			catch (IndexNotExistsException e)
			{
				System.err.println("The index does not exist in the database.");
			}
			catch (SizeOfRecordIsTooLarge e)
			{
				System.err.println("The size of record is too large.");
			}
			catch (AttributeNotExistsException e)
			{
				System.err.println("Some attribute(s) cannot be found in corresponding table.");
			}
			catch (NotUniqueException e)
			{
				System.err.println("Attribute is not unique.");
			}
		}
		return 0;
	}
}