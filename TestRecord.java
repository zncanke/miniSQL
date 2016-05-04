class TestRecord
{
	public static void main(String[] args)
	{
		try
		{
			BufferManager.Initial();
			byte[] b = new byte[4];
			b[0] = 1;
			b[1] = 0;
			b[2] = 0;
			b[3] = 0;
			BufferManager.CreateNewFile("in");
			RecordManager.Insert("in", b);
			BufferManager.WriteBackAll();
			//RecordManager.DropTable("in");
		}catch(Exception e)
		{
			System.out.println("<-------Exception------->");
		}
	}
}