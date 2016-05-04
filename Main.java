class Main
{
	public static void main(String[] args)
	{
		Interpreter.OpenSQL();
		while (true)
		{
			int returnCode = Interpreter.Work(null);
			if (returnCode == 1)
			{
				Interpreter.CloseSQL();
				break;
			}
		}
	}
}