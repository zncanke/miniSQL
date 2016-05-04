class Key
{
	public final String s;
	public final int x;
	Key(String ss, int xx)
	{
		s = new String(ss);
		x = xx;
	}

	@Override
	public boolean equals(Object o) 
    {
        if (this == o) return true;
        if (!(o instanceof Key)) return false;
        Key key = (Key) o;
        return s.equals(key.s) && x == key.x;
    }

    @Override
    public int hashCode()
    {
    	return s.hashCode() * 31 + x;
    }
}