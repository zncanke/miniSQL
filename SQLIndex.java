/**
 * Created by lenovo on 2015/10/27.
 */
public class SQLIndex {
    String name;
    SQLAttribute baseAttribute;
    SQLTable baseTable;
    Index index;
    int rootIndex;

    /**
     *
     * @param name 索引名
     * @param baseTable 所在表
     * @param baseAttribute 所在属性
     * @param rootIndex 索引树的根在索引文件中的块编号
     */
    public SQLIndex(String name, SQLTable baseTable, SQLAttribute baseAttribute, int rootIndex) {
        this.name = name;
        this.baseTable = baseTable;
        this.baseAttribute = baseAttribute;
        this.rootIndex = rootIndex;
    }

    /**
     * 关联索引数据
     * @param index 实际索引数据
     */
    public void setIndex(Index index) {
        this.index = index;
    }

    /**
     * 获取索引数据
     * @return 实际索引数据
     */
    public Index getIndex() {
        if (index == null) {
            index = new Index(this);
        }
        return index;
    }
    public String getName() {
        return name;
    }
    public SQLTable getBaseTabel() {
        return baseTable;
    }
    public SQLAttribute getBaseAttribute() {
        return baseAttribute;
    }
    public int getRootIndex() {
        return rootIndex;
    }
    public void setRootIndex(int rootIndex) {
        this.rootIndex = rootIndex;
    }
}
