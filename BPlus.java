/**
 * Created by lenovo on 2015/11/2.
 */
public interface BPlus {
    public Object get(Comparable key);
    public void remove(Comparable key);
    public void insertOrUpdate(Comparable key, Object obj);
}
