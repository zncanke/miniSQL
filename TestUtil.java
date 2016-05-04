import java.io.*;
import java.util.*;

class TestUtil
{
	public static void main(String[] args)
	{
		//try
		//{
			/*File srcFile = new File("in");
			RandomAccessFile out = new RandomAccessFile(srcFile, "rws");
			out.writeFloat(f);
			out.close();*/
			//float f = (float)0.1;
			//byte[] b = Util.Float2ByteArr(f);
			/*for (int i = 0; i < b.length; i++)
				System.out.println(Integer.toHexString((int)b[i]));*/
			//System.out.println(Util.ByteArr2Float(b));
			byte[] bb = new byte[4];
			bb[0] = (byte)0x3d;
			bb[1] = (byte)0xcc;
			bb[2] = (byte)0xcc;
			bb[3] = (byte)0xcd;
			System.out.println(Util.ByteArr2Float(bb));
		//}catch(Exception e)
		//{
		//	System.out.println("<---Exception--->");
		//}
	}
}