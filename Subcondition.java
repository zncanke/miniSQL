import java.util.*;

class Subcondition
{
	String left;
	final int op;
	final byte[] right;
	final SQLAttribute attr;
	Subcondition(String tableName, String s1, String s2, String s3)
		throws SyntaxError, AttributeNotExistsException, TableNotExistsException
	{
		left = s1;
		String tmp = s2;
		if (tmp.equals("<"))
			op = 0;
		else
		if (tmp.equals("="))
			op = 1;
		else
		if (tmp.equals(">"))
			op = 2;
		else
		if (tmp.equals("<="))
			op = 3;
		else
		if (tmp.equals(">="))
			op = 4;
		else
		if (tmp.equals("<>"))
			op = 5;
		else
			throw new SyntaxError("Operators can only be \"< = > <= >= <>\".");
		attr = CatalogManager.getAttribute(tableName, left);
		right = CatalogManager.checkType(s3, attr);
	}
}