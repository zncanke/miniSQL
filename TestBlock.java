class TestBlock
{
	public static void main(String[] args)
	{
		try
		{
			BufferManager.Initial();
			//byte[] a = BufferManager.GetAttr("in", 1, "a");
			//System.out.println(Integer.toHexString(a[0])+" "+Integer.toHexString(a[1]));
			BufferManager.CreateNewFile("in1");
			BufferManager.CreateNewBlock("in1");
		}catch(Exception e)
		{
			System.out.println("<-------Exception------->");
		}
	}
}