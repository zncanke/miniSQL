import java.util.Comparator;
import java.util.Vector;

class TestRun
{
    public static void main(String[] args)
    {
        Interpreter.OpenSQL();
		/*while (true)
		{
			int returnCode = Interpreter.Work();
			if (returnCode == 1)
			{
				Interpreter.CloseSQL();
				break;
			}
		}*/
        try {
            System.out.println(Class.forName("java.lang.Integer").getName());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        CatalogManager.initDb("test");
        /*
        try {
            CatalogManager.dropTable("score");
        } catch (TableNotExistsException e) {
            e.printStackTrace();
        }
        Vector<SQLAttribute> attrs = new Vector<SQLAttribute>();
        attrs.add(new SQLAttribute("ID", Integer.class, 4, true, false));
        attrs.add(new SQLAttribute("score", Integer.class, 4, false, false));
        CatalogManager.addTable("score", attrs);
        try {
            CatalogManager.addIndex("scoreIndex", "score", "ID");
        } catch (Exception e) {
            e.printStackTrace();
        }
        CatalogManager.saveDB();
        		*/
        byte[] src;
        SQLAttribute attr = new SQLAttribute("ID", String.class, 10, true, null);
        try {
            src = CatalogManager.checkType("\'9527\'", attr);
            Comparable a = (Comparable)attr.convert(src);
            byte[] tmp = attr.checkType("\'9527\'");
            Comparable b = attr.convert(tmp);
            System.out.println(a.compareTo(b));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}