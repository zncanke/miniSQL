class Util
{
	public static int ByteArr2Int(final byte[] b)
	{
		int res;
		res = (b[0] & 0xff) | ((b[1] << 8) & 0xff00) | 
			  ((b[2] << 16) & 0xff0000) | (b[3] << 24);
		return res;
	}
	public static byte[] Int2ByteArr(final int x)
	{
		byte[] res = new byte[4];
		res[0] = (byte)(x & 0xff);
		res[1] = (byte)((x >> 8) & 0xff);
		res[2] = (byte)((x >> 16) & 0xff);
		res[3] = (byte)((x >> 24) & 0xff);
		return res; 
	}
	public static byte[] String2ByteArr(final String s, int len)
	{
		byte[] res = new byte[len];
		System.arraycopy(s.getBytes(), 0, res, 0, s.length());
		for (int i = s.length(); i < len; i++)
			res[i] = 0;
		return res;
	}
	public static String ByteArr2String(final byte[] b)
	{
		int t = 0;
		for (int i = 0; i < b.length; i++)
			if (b[i] == 0)
			{
				t = i;
				break;
			}
		byte[] bb = new byte[t];
		System.arraycopy(b, 0, bb, 0, t);
		return new String(bb);
	}
	public static byte[] Float2ByteArr(final float f)
	{
		int fbit = Float.floatToIntBits(f);
		byte[] b = Int2ByteArr(fbit);
		int len = b.length;
		byte tmp;
		for (int i = 0; i < len / 2; i++)
		{
			tmp = b[i];
			b[i] = b[len - i - 1];
			b[len - i - 1] = tmp;
		}
		return b;
	}
	public static float ByteArr2Float(final byte[] b)
	{
		int tmp;
		tmp = b[3];
		tmp &= 0xff;
		tmp |= ((int)b[2] << 8);
		tmp &= 0xffff;
		tmp |= ((int)b[1] << 16);
		tmp &= 0xffffff;
		tmp |= ((int)b[0] << 24);
		return Float.intBitsToFloat(tmp);
	}
}