/**
 * Created by lenovo on 2015/10/27.
 */
public class SQLAttribute {
    String name;
    Class type;
    int length, offset;
    boolean unique;
    String  index;

    /**
     * 构建属性信息
     * @param name 属性名
     * @param type 属性类型
     * @param length 属性长度
     * @param unique 是否唯一
     * @param index 对应索引名，没有则为空字符串
     */
    public SQLAttribute(String name, Class type, int length, boolean unique, String index) {
        this.name = name;
        this.type = type;
        this.length = length;
        this.unique = unique;
        this.index = index;
    }

    /**
     * 默认构造函数
     */
    public SQLAttribute() {
        this.name = "";
        this.type = Integer.class;
        this.unique = false;
        this.index = "";
        this.length = 0;
    }

    /**
     * 设置属性在单条记录内的偏移位置
     * @param offset 偏移位置
     */
    public void setOffset(int offset) {
        this.offset = offset;
    }

    /**
     * 查询属性在单条记录内的偏移位置
     * @return 偏移位置
     */
    public int getOffset() {
        return offset;
    }

    /**
     * 检测是否唯一
     * @return 是否唯一
     */
    public boolean isUnique() {
        return unique;
    }

    /**
     * 设置是否唯一
     * @param unique 唯一标记
     */
    public void setUnique(boolean unique) {
        this.unique = unique;
    }

    /**
     * 检查是否有索引
     * @return 是否有索引
     */
    public boolean isIndexed() {
        return !index.equals("");
    }

    /**
     * 设置索引
     * @param indexName 索引名
     */
    public void setIndex(String indexName) {
        index = indexName;
    }

    /**
     * 获取索引名
     * @return 索引名
     */
    public String getIndex() {
        return index;
    }

    /**
     * 获取属性名
     * @return 属性名
     */
    public String getName() {
        return name;
    }

    /**
     * 获取属性类型
     * @return 属性类型
     */
    public Class getType() {
        return type;
    }

    /**
     * 获取属性长度
     * @return 属性长度（字节）
     */
    public int size() {
        return length;
    }

    /**
     * 将字符串数据根据类型转变为相应字节数组
     * @param value 字符串数据
     * @return 表达数据的字节数组
     */
    public byte[] checkType(String value) {
        byte[] res = null;
        boolean match = false;
        if (value.charAt(0) == '\'' && value.charAt(value.length() - 1) == '\''
                && type == String.class && length >= value.length() - 1) {
            match = true;
            res = Util.String2ByteArr(value.substring(1, value.length() - 1), length);
        } else if (type == Float.class) {
            /*
            if (value.equals("")) return null;
            match = true;
            int sign = 1, begin = 0;
            int flag = 0;
            float tmp = 0;
            if (value.charAt(0) == '-') {
                sign = -1;
                begin = 1;
            }
            for (int i = begin; i < value.length();i++) {
                if (value.charAt(i) == '.') {
                    flag = 10;
                } else if (value.charAt(i) >= '0' && value.charAt(i) <= '9') {
                    if (flag == 0) {
                        tmp = tmp * 10 + value.charAt(i) - '0';
                    } else {
                        tmp = tmp + (float)(value.charAt(i) - '0') / (float)flag;
                        flag *= 10;
                    }
                } else {
                    match = false;
                    break;
                }

            }
            if (match) {
                res = Util.Float2ByteArr(tmp * (float)sign);
            }*/
            float tmp;
            try {
                tmp = Float.parseFloat(value);
            } catch (NumberFormatException e) {
                return null;
            }
            res = Util.Float2ByteArr(tmp);
        } else {
            /*
            match = true;
            int sign = 1, begin = 0;
            int tmp = 0;
            if (value.charAt(0) == '-') {
                sign = -1;
                begin = 1;
            }
            for (int i = begin;i < value.length();i++) {
                if (value.charAt(i) >= '0' && value.charAt(i) <= '9') {
                    tmp = tmp * 10 + value.charAt(i) - '0';
                } else {
                    match = false;
                    break;
                }
            }
            if (match) {
            if (match ) {
                res = Util.Int2ByteArr(tmp * sign);
            } */
            int tmp;
            try {
                tmp = Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return null;
            }
            res = Util.Int2ByteArr(tmp);
        }
        return res;
    }

    /**
     * 根据属性类型格式化输出数据
     * @param src 数据内容
     * @return 表达数据的字符串
     */
    public String show(byte[] src) {
        if (type == Integer.class) {
            return String.format("%-,15d", Util.ByteArr2Int(src));
        } else if (type == Float.class) {
            return String.format("%-,15f", Util.ByteArr2Float(src));
        } else {
            return String.format("%-" + Math.max(length + 2, name.length()) + "s", Util.ByteArr2String(src));
        }
    }

    /**
     * 根据属性类型讲字节数据转换为可比较对象
     * @param src 字节数据
     * @return 可比较对象
     */
    public Comparable convert(byte[] src) {
        if (type == Integer.class) {
            return Util.ByteArr2Int(src);
        } else if (type == Float.class) {
            return Util.ByteArr2Float(src);
        } else {
            return Util.ByteArr2String(src);
        }
    }

    /**
     * 根据属性类型将一个可比较对象转换为字节数据
     * @param key 表达数据的对象
     * @return 表达数据的字节数组
     */
    public byte[] getByte(Comparable key) {
        if (type == Integer.class) {
            return Util.Int2ByteArr((Integer)key);
        } else if (type == Float.class) {
            return Util.Float2ByteArr((Float)key);
        } else {
            return Util.String2ByteArr((String)key, length);
        }
    }
}