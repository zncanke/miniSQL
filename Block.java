import java.util.*;

class Block
{
	final static int sizeOfBlock = 8192;
	public boolean referenceBit;
	public boolean dirty;
	public boolean using;
	public Index.Node node;
	public Block next, pre;
	public String tableName;
	public int num;
	private byte[] content = new byte[sizeOfBlock];
	Block()
	{
		referenceBit = false;
		next = pre = null;
		dirty = false;
		using = false;
		tableName = "";
		num = 0;
		node = null;
		Arrays.fill(content, (byte)0);
	}
	Block(String tableName, int num, byte[] src)
	{
		referenceBit = false;
		next = pre = null;
		dirty = false;
		using = false;
		this.tableName = tableName;
		this.num = num;
		System.arraycopy(src, 0, content, 0, sizeOfBlock);
	}
	public byte[] getValue(int fromIndex, int len)
	{
		byte[] res = new byte[len];
		System.arraycopy(content, fromIndex, res, 0, len);
		return res;
	}
	public void setValue(byte[] src, int beginIndex, int len)
	{
		dirty = true;
		System.arraycopy(src, 0, content, beginIndex, len);
	}
	public byte[] getContent()
	{
		byte[] res;
		res = new byte[sizeOfBlock];
		System.arraycopy(content, 0, res, 0, sizeOfBlock);
		return res;
	}
	public void setNode(Index.Node node) {
		this.node = node;
	}
	public void beforeWriteBack()
	{
		if (node != null) {
			node.sync();
		}
	}
}