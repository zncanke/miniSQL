import java.util.*;

/**
 * Created by lenovo on 2015/10/27.
 */
public class SQLTable {
    int size;
    private String name; //表名
    private HashMap<String, SQLAttribute> attributes; // 表内属性的哈希表
    private Vector<SQLAttribute> order; // 表内属性的顺序排列

    /**
     *
     * @param name 表名
     * */
    public SQLTable(String name) {
        this.name = name;
        attributes = new HashMap<String, SQLAttribute>();
        order = new Vector<SQLAttribute>();
    }

    /**
     * 添加一个属性
     * @param attr
     */
    public void addAttribute(SQLAttribute attr) {
        int pos;
        if (order.isEmpty()) {
            pos = 0;
        } else {
            SQLAttribute last = order.lastElement();
            pos = last.getOffset() + last.size();
        }
        attributes.put(attr.getName(), attr);
        attr.setOffset(pos);
        order.add(attr);
    }

    /**
     * 根据索引检查一条插入数据是否与唯一标记冲突，如果存在不可判断的属性，则返回不可判断的属性列表
     * @param src 插入数据
     * @return 不可确定的属性
     */
    public Vector<SQLAttribute> checkIndex(byte[] src) {
        Vector<SQLAttribute> toCheck = new Vector<SQLAttribute>();
        for (int i = 0;i < order.size();i++) {
            SQLAttribute attr = order.get(i);
            if (attr.isUnique()) toCheck.add(attr);
        }
        for (SQLAttribute a : toCheck) {
            if (a.getIndex().equals("")) return toCheck;
        }
        byte[] tmp;
        for (SQLAttribute a : toCheck) {
            tmp = new byte[a.size()];
            System.arraycopy(src, a.getOffset(), tmp, 0, a.size());
            Comparable key = a.convert(tmp);
            if (CatalogManager.getIndex(a.getIndex()).getIndex().get(key) != null) {
                return null;
            }
        }
        return new Vector<SQLAttribute>();
    }

    /**
     * 根据插入的数据更新索引
     * @param src
     * @param displacement
     */
    public void addRecordWithIndex(byte[] src, int displacement) {
        byte[] tmp;
        for (int i = 0;i < order.size();i++) {
            SQLAttribute attr = order.get(i);
            if (!attr.getIndex().equals("")) {
                tmp = new byte[attr.size()];
                System.arraycopy(src, attr.getOffset(), tmp, 0, tmp.length);
                Comparable key = attr.convert(tmp);
                CatalogManager.getIndex(attr.getIndex()).getIndex().insertOrUpdate(key , displacement);
            }
        }

    }

    /**
     * 根据删除记录更新索引
     * @param src
     */
    public void delRecordWithIndex(byte[] src) {
        byte[] tmp;
        for (int i = 0;i < order.size(); i++ ){
            SQLAttribute attr = order.get(i);
            if (!attr.getIndex().equals("")) {
                tmp = new byte[attr.size()];
                System.arraycopy(src, attr.getOffset(), tmp, 0, tmp.length);
                Comparable key = attr.convert(tmp);
                CatalogManager.getIndex(attr.getIndex()).getIndex().remove(key);
            }
        }
    }

    /**
     * 获取属性
     * @param name 属性名
     * @return 属性
     */
    public SQLAttribute getAttribute(String name) {
        if (attributes.containsKey(name)) {
            return attributes.get(name);
        } else {
            return null;
        }
    }

    /**
     * 根据各个数据的类型讲字符串数据转换为字节数据，如果有类型不匹配的属性存在，则返回null
     * @param values 对应各个属性的字符串集合
     * @return 字节数据
     */
    public byte[] valueAttrCheck(Vector<String> values) {
        byte[] tmp;
        if (values.size() != order.size()) return null;
        byte[] res = new byte[size()];
        if (values.size() != order.size()) return null;
        for (int i = 0;i < values.size();i++) {
            SQLAttribute attr = order.get(i);
            tmp = attr.checkType(values.get(i));
            if (tmp != null) {
                for (int j = 0;j < attr.size();j++) {
                    res[attr.getOffset() + j] = tmp[j];
                }
            } else {
                return null;
            }
        }
        return res;
    }

    /**
     * 根据表信息打印输出用的表头
     */
    public void showTitle() {
        System.out.print("+");
        for (SQLAttribute a : order) {
            int length;
            if (a.getType() == Integer.class) {
                length = 15;
            } else if (a.getType() == Float.class) {
                length = 15;
            } else {
                length = Math.max(a.length + 2, a.getName().length());
            }
            for (int j = 0;j < length;j++)
                System.out.print("-");
            System.out.print("+");
        }
        System.out.println();
        System.out.print("|");
        for (SQLAttribute a : order) {
            String tmp;
            if (a.getType() == Integer.class) {
                System.out.print(String.format("%-15s", a.getName()));
            } else if (a.getType() == Float.class) {
                System.out.print(String.format("%-15s", a.getName()));
            } else {
                System.out.print(String.format("%-" + Math.max(a.length + 2, a.getName().length()) + "s", a.getName()));
            }
            System.out.print("|");
        }
        System.out.println();
        System.out.print("+");
        for (SQLAttribute a : order) {
            int length;
            if (a.getType() == Integer.class) {
                length = 15;
            } else if (a.getType() == Float.class) {
                length = 15;
            } else {
                length = Math.max(a.length + 2, a.getName().length());
            }
            for (int j = 0;j < length;j++)
                System.out.print("-");
            System.out.print("+");
        }
        System.out.println();
    }

    /**
     * 将字节数据转换为格式化字符串返回
     * @param src
     * @return
     */
    public String show(byte[] src) {
        StringBuilder builder = new StringBuilder();
        builder.append(" ");
        for (int i = 0;i < order.size();i++) {
            SQLAttribute attr = order.get(i);
            byte[] slice = new byte[attr.size()];
            System.arraycopy(src,attr.getOffset(), slice, 0, attr.size());
            builder.append(order.get(i).show(slice));
            builder.append(" ");
        }
        return builder.toString();
    }

    /**
     * 根据属性编号查询属性
     * @param index 编号
     * @return 属性
     */
    public SQLAttribute getAttributeByIndex(int index) {
        return order.get(index);
    }

    /**
     * 获取一个属性的偏移位置
     * @param attrName 属性名
     * @return 偏移位置
     */
    public int getOffsetOfAttribute(String attrName) {
        return getAttribute(attrName).getOffset();
    }

    /**
     * 计算一个表中单条记录的大小，小于4个字节的统一按4字节计算
     * @return 记录大小
     */
    public int size() {
        int sum = 0;
        for (SQLAttribute a : attributes.values()) {
            sum += a.size();
        }
        if (sum < 4) sum = 4;
        return sum + 1;
    }

    /**
     * 获取属性集合
     * @return 属性集合
     */
    public Vector<SQLAttribute> attributeCollection() {
        return order;
    }

    /**
     * 获取表名
     * @return 表名
     */
    public String getName() {
        return name;
    }

}
