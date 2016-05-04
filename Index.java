import java.io.IOException;
import java.util.*;

/**
 * Created by lenovo on 2015/11/4.
 */
public class Index {
    String fileName;
    SQLAttribute attr;
    SQLIndex indexInfo;
    HashMap<Integer, Node> nodes = new HashMap<Integer, Node>();
    BPlusTree tree;

    /**
     * 根据索引信息创建索引数据
     * @param index 索引信息
     */
    public Index(SQLIndex index) {
        fileName = index.name + ".ind";
        attr = index.baseAttribute;
        tree = new BPlusTree((Block.sizeOfBlock - 21) / (attr.size() + 4), index.rootIndex);
        indexInfo = index;
    }

    /**
     * 初始化，读入并设置树根
     */
    public void init() {
        Node node = createNode(true, true);
        tree.setRoot(node.ind);
    }

    /**
     * 插入一个数据
     * @param key 键值
     * @param obj 记录在文件中的偏移
     */
    public void insertOrUpdate(Comparable key, Object obj) {
        tree.insertOrUpdate(key, obj);
    }

    /**
     * 移除一个数据
     * @param key 键值
     */
    public void remove(Comparable key) {
        tree.remove(key);
    }

    /**
     * 根据键值获取一个数据
     * @param key 键值
     * @return 数据
     */
    public Object get(Comparable key) {
        return tree.get(key);
    }

    /**
     * 根据块编号读入一个节点
     * @param rankOfBlock 块编号
     * @return 节点对象
     */
    public Node loadNode(int rankOfBlock){
        Node node = null;
        if (rankOfBlock == 0) return null;
        try {
            if (nodes.containsKey(rankOfBlock)) {
                node = nodes.get(rankOfBlock);
                if (node.getBlock() == null) {
                    Block block = BufferManager.LoadBlock(fileName, node.ind);
                    block.setNode(node);
                    node.setBlock(BufferManager.LoadBlock(fileName, node.ind));
                }
                return nodes.get(rankOfBlock);
            }
            Block block = BufferManager.LoadBlock(fileName, rankOfBlock);
            node = new Node(block, attr);
            block.setNode(node);
            nodes.put(node.ind, node);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return node;
    }

    /**
     * 新建一个节点
     * @param isLeaf 是否为叶子
     * @param isRoot 是否为根
     * @return 节点对象
     */
    public Node createNode(boolean isLeaf, boolean isRoot) {
        Node node = new Node(isLeaf, isRoot);
        try {
            Block block = BufferManager.CreateNewBlock(fileName);
            block.setNode(node);
            node.setBlock(block);
            node.ind = block.num;
            nodes.put(node.ind, node);
        } catch (TableNotExistsException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return node;
    }

    public class Node {
        boolean isLeaf, isRoot;
        protected int parent;
        protected int ind;
        protected int previous;
        protected int next;
        protected Block block;
        protected List<Map.Entry<Comparable, Object>> entries;
        protected List<Integer> children;

        public List<Map.Entry<Comparable, Object>> getEntries() {
            return entries;
        }

        public Node(boolean isLeaf) {
            this.isLeaf = isLeaf;
            entries = new ArrayList<Map.Entry<Comparable, Object>>();
            if (!isLeaf) {
                children = new ArrayList<Integer>();
            }
        }

        public Node(boolean isLeaf, boolean isRoot) {
            this(isLeaf);
            this.isRoot = isRoot;
        }

        /**
         * 根据对应属性信息从块内读取数据建立一个节点
         * @param block 节点对应块
         * @param attr 节点对应属性信息
         */
        public Node(Block block, SQLAttribute attr) {
            byte[] tmp;
            int step = attr.size();
            tmp = block.getValue(0, 4);
            this.block = block;
            this.ind = block.num;
            int num = Util.ByteArr2Int(tmp);
            // get parent
            tmp = block.getValue(4, 4);
            parent = Util.ByteArr2Int(tmp);
            // get previous
            tmp = block.getValue(8, 4);
            previous = Util.ByteArr2Int(tmp);
            // get next
            tmp = block.getValue(12, 4);
            next = Util.ByteArr2Int(tmp);
            // get isLeaf nad isRoot
            tmp = block.getValue(16, 1);
            isLeaf = (tmp[0] & 2) != 0;
            isRoot = (tmp[0] & 1) != 0;
            entries = new ArrayList<Map.Entry<Comparable, Object>>();
            if (num != 0) {
                if (!isLeaf) {
                    children = new ArrayList<Integer>();
                    tmp = block.getValue(17, 4);
                    children.add(Util.ByteArr2Int(tmp));
                    for (int i = 0; i < num; i++) {
                        tmp = block.getValue(21 + (step + 4) * i, step);
                        entries.add(new AbstractMap.SimpleEntry<Comparable, Object>(attr.convert(tmp), null));
                        tmp = block.getValue(21 + (step + 4) * i + step, 4);
                        children.add(Util.ByteArr2Int(tmp));
                    }
                } else {
                    int value;
                    Comparable key;
                    for (int i = 0;i < num;i++) {
                        tmp = block.getValue(21 + (step + 4) * i, step);
                        key = attr.convert(tmp);
                        tmp = block.getValue(21 + (step + 4) * i + step, 4);
                        value = Util.ByteArr2Int(tmp);
                        entries.add(new AbstractMap.SimpleEntry<Comparable, Object>(key, value));
                    }
                }
            }
        }

        public void setBlock(Block block) {
            this.block = block;
            this.ind = block.num;
        }

        public Block getBlock() {
            return block;
        }

        /**
         * 写回节点信息到缓存块
         */
        public void sync() {
            byte[] tmp;
            int step = attr.size();
            tmp = Util.Int2ByteArr(entries.size());
            block.setValue(tmp, 0, 4);
            tmp = Util.Int2ByteArr(parent);
            block.setValue(tmp ,4, 4);
            tmp = Util.Int2ByteArr(previous);
            block.setValue(tmp ,8, 4);
            tmp = Util.Int2ByteArr(next);
            block.setValue(tmp, 12, 4);
            tmp = new byte[1];
            tmp[0] = 0;
            if (isLeaf) tmp[0] |= 2;
            if (isRoot) tmp[0] |= 1;
            block.setValue(tmp, 16, 1);
            if (isLeaf) {
                for (int i = 0;i < entries.size();i++) {
                    tmp = attr.getByte(entries.get(i).getKey());
                    block.setValue(tmp, 21 + (step + 4) * i, step);
                    tmp = Util.Int2ByteArr((Integer)entries.get(i).getValue());
                    block.setValue(tmp, 21 + (step + 4) * i + step, 4);

                }
            } else {
                tmp = Util.Int2ByteArr(children.get(0));
                block.setValue(tmp, 17, 4);
                for (int i = 0;i < entries.size();i++) {
                    tmp = attr.getByte(entries.get(i).getKey());
                    block.setValue(tmp, 21 + (step + 4) * i, step);
                    tmp = Util.Int2ByteArr(children.get(i + 1));
                    block.setValue(tmp, 21 + (step + 4) * i + step, 4);
                }
            }
        }

        public List<Integer> getChildren() {
            return children;
        }

        public int getParent() {
            return parent;
        }

        public void setParent(int parent) {
            this.parent = parent;
        }

        public int getPrevious() {
            return previous;
        }

        public void setPrevious(int previous) {
            this.previous = previous;
        }

        public int getNext() {
            return next;
        }

        public void setNext(int next) {
            this.next = next;
        }

        /**
         * 获取当前节点子树中最小的键
         * @return 子树中最小的键
         */
        public Comparable getLeastKey() {
            Node tmp = this;
            while (!tmp.isLeaf) {
                tmp = loadNode(tmp.children.get(0));
            }
            try {
                return tmp.entries.get(0).getKey();
            }catch (IndexOutOfBoundsException e) {
                e.printStackTrace();
            }
            return null;
        }

        /**
         * 根据键查询一个数据
         * @param key 查询的键
         * @return 数据
         */
        public Object get(Comparable key) {
            if (isLeaf) {
                // 叶子节点
                for (Map.Entry<Comparable, Object> e : entries) {
                    if (e.getKey().compareTo(key) == 0) {
                        return e.getValue();
                    }
                }
                return null;
            } else {
                // 非叶子节点
                if (entries.get(0).getKey().compareTo(key) > 0) {
                    return loadNode(children.get(0)).get(key);
                } else if (entries.get(entries.size() - 1).getKey().compareTo(key) <= 0) {
                    return loadNode(children.get(children.size() - 1)).get(key);
                } else {
                    for (int i = 0; i < entries.size(); i++) {
                        if (entries.get(i).getKey().compareTo(key) <= 0 && entries.get(i + 1).getKey().compareTo(key) > 0) {
                            return loadNode(children.get(i + 1)).get(key);
                        }
                    }
                }
                return null;
            }
        }

        /**
         * 插入或更新一个键值对
         * @param key 键
         * @param obj 值
         * @param tree 所在B+树
         */
        public void insertOrUpdate(Comparable key, Object obj, BPlusTree tree) {
            if (isLeaf) {
                // 无需拆分
                insertOrUpdate(key, obj);
                if (entries.size() >= tree.getOrder()) {
                    Node tmp = createNode(isLeaf, false);
                    insertOrUpdate(key, obj);
                    int rightStart = tree.getOrder() / 2 + tree.getOrder() % 2;
                    for (int i = rightStart;i < tree.getOrder();i++) {
                        tmp.getEntries().add(i - rightStart, entries.get(rightStart));
                        entries.remove(rightStart);
                    }
                    if (this.next != 0) {
                        loadNode(this.next).previous = tmp.block.num;
                    }
                    tmp.next = this.next;
                    this.next = tmp.ind;
                    tmp.previous = this.ind;
                    if (parent != 0) {
                        // 不为根
                        int index = loadNode(parent).getChildren().indexOf(this.ind);
                        loadNode(parent).getChildren().add(index + 1, tmp.ind);
                        tmp.setParent(parent);
                        loadNode(parent).updateInsert(tree);
                    } else {
                        // 为根
                        isRoot = false;
                        Node parent = createNode(false, true);
                        tree.setRoot(parent.ind);
                        this.setParent(parent.ind);
                        tmp.setParent(parent.ind);
                        parent.getChildren().add(this.ind);
                        parent.getChildren().add(tmp.ind);
                        parent.updateInsert(tree);
                    }
                }
            } else {
                if (entries.get(0).getKey().compareTo(key) > 0 ) {
                    loadNode(children.get(0)).insertOrUpdate(key, obj, tree);
                } else if (entries.get(entries.size() - 1).getKey().compareTo(key) <= 0) {
                    loadNode(children.get(children.size() - 1)).insertOrUpdate(key, obj, tree);
                } else {
                    for (int i = 0;i < entries.size();i++) {
                        if (entries.get(i).getKey().compareTo(key) <= 0 && entries.get(i + 1).getKey().compareTo(key) > 0) {
                            loadNode(children.get(i + 1)).insertOrUpdate(key, obj, tree);
                            break;
                        }
                    }
                }
            }
            //Todo
        }

        /**
         * 插入后B+树结构调整
         * @param tree 所在B+树
         */
        protected void updateInsert(BPlusTree tree) {
            validate(tree);
            if (children.size() > tree.getOrder()) {
                //需要分裂
                Node tmp = createNode(false, false);
                int rightStart = (tree.getOrder() + 1) / 2 + (tree.getOrder() + 1) % 2;
                tmp.getChildren().add(0, children.get(rightStart));
                loadNode(children.get(rightStart)).setParent(tmp.ind);
                children.remove(rightStart);
                entries.remove(rightStart - 1);
                //从原节点转移数据和子节点
                for (int i = rightStart;i < tree.getOrder();i++) {
                    tmp.getChildren().add(i - rightStart + 1, children.get(rightStart));
                    tmp.getEntries().add(i - rightStart, entries.get(rightStart - 1));
                    loadNode(children.get(rightStart)).setParent(tmp.ind);
                    children.remove(rightStart);
                    entries.remove(rightStart - 1);
                }
                //调整上层结构
                if (parent != 0) {
                    Node node = loadNode(parent);
                    int index = node.getChildren().indexOf(this.ind);
                    node.getChildren().add(index + 1, tmp.ind);
                    tmp.setParent(parent);
                    node.updateInsert(tree);
                } else {
                    isRoot = false;
                    Node parent = createNode(false ,true);
                    tree.setRoot(parent.ind);
                    parent.getChildren().add(this.ind);
                    parent.getChildren().add(tmp.ind);
                    setParent(parent.ind);
                    tmp.setParent(parent.ind);
                    parent.updateInsert(tree);
                }
            }

        }

        /**
         * 根据子节点信息更新非叶子节点的键信息
         * @param tree 所在B+树
         */
        protected void validate(BPlusTree tree) {
            if (entries.size() == children.size() - 1) {
                for (int i = 0;i < entries.size();i++) {
                    Comparable key = loadNode(children.get(i + 1)).getLeastKey();
                    if (entries.get(i).getKey().compareTo(key) != 0) {
                        entries.remove(i);
                        entries.add(i, new AbstractMap.SimpleEntry<Comparable, Object>(key, null));
                        if (!isRoot) {
                            loadNode(parent).validate(tree);
                        }
                        return;
                    }
                }
            } else if (isRoot && children.size() >= 2 ||
                    children.size() >= (tree.getOrder() / 2 + tree.getOrder() & 1) &&
                            children.size() <= tree.getOrder() + 1 && children.size() >= 2) {
                entries.clear();
                loadNode(children.get(0)).setParent(this.ind);
                for (int i = 1;i < children.size();i++) {
                    Comparable key = loadNode(children.get(i)).getLeastKey();
                    entries.add(i - 1, new AbstractMap.SimpleEntry<Comparable, Object>(key, null));
                    loadNode(children.get(i)).setParent(this.ind);
                }
                if (!isRoot)
                    loadNode(parent).validate(tree);
                return;
            }
        }

        /**
         * 移除一个数据
         * @param key 数据对应键
         * @param tree 所在B+树
         */
        public void remove(Comparable key, BPlusTree tree) {
            if (isLeaf) {
                if (remove(key)) {
                    if (isRoot) return;
                    int minNum = tree.getOrder() / 2 + tree.getOrder() % 2;
                    if (entries.size() < minNum) {
                        Node preNode = loadNode(previous), nextNode = loadNode(next);
                        if (preNode != null && preNode.getEntries().size() > minNum &&
                                preNode.getEntries().size() > 2 && preNode.getParent() == parent) {
                            // 向左借
                            int size  = preNode.getEntries().size();
                            Map.Entry<Comparable, Object> e = preNode.getEntries().get(size - 1);
                            preNode.getEntries().remove(size - 1);
                            entries.add(0, e);
                        } else if (nextNode != null && nextNode.getEntries().size() > minNum &&
                                nextNode.getEntries().size() > 2 && nextNode.getParent() == parent) {
                            // 向右借
                            Map.Entry<Comparable, Object> e = nextNode.getEntries().get(0);
                            nextNode.getEntries().remove(0);
                            entries.add(e);
                        } else {
                            // 需要合并
                            if (preNode != null && preNode.getParent() == parent) {
                                // 向左合并
                                for (int i = preNode.getEntries().size() - 1;i >= 0;i--) {
                                    entries.add(0, preNode.getEntries().get(i));
                                }
                                loadNode(parent).getChildren().remove((Integer)previous);
                                preNode.getBlock().setNode(null);
                                if (preNode.getPrevious() != 0) {
                                    loadNode(preNode.getPrevious()).setNext(this.ind);
                                    previous = preNode.getPrevious();
                                } else {
                                    tree.setHead(this.ind);
                                    previous = 0;
                                }
                            } else if (nextNode != null && nextNode.getParent() == parent) {
                                for (int i = 0;i < nextNode.getEntries().size();i++) {
                                    entries.add(nextNode.getEntries().get(i));
                                }
                                loadNode(parent).getChildren().remove((Integer)next);
                                nextNode.getBlock().setNode(null);
                                if (nextNode.getNext() != 0) {
                                    loadNode(nextNode.getNext()).setPrevious(this.ind);
                                    next = nextNode.getNext();
                                } else {
                                    next = 0;
                                }
                            }
                        }
                    }
                    loadNode(parent).updateRemove(tree);
                }
            } else {
                if (entries.get(0).getKey().compareTo(key) > 0) {
                    loadNode(children.get(0)).remove(key, tree);
                } else if (entries.get(entries.size() - 1).getKey().compareTo(key) <= 0) {
                    loadNode(children.get(children.size() - 1)).remove(key, tree);
                } else {
                    for (int i = 0;i < entries.size();i++) {
                        if (entries.get(i).getKey().compareTo(key) <= 0 && entries.get(i + 1).getKey().compareTo(key) > 0) {
                            loadNode(children.get(i + 1)).remove(key, tree);
                            break;
                        }
                    }
                }
            }
        }

        /**
         * 移除数据后更新树结构
         * @param tree 所在B+树
         */
        public void updateRemove(BPlusTree tree) {
            validate(tree);
            int minNum = tree.getOrder() / 2 + tree.getOrder() % 2;
            if (children.size() < minNum) {
                if (isRoot) {
                    if (children.size() >= 2) {
                        return;
                    } else {
                        Node root = loadNode(children.get(0));
                        tree.setRoot(root.ind);
                        root.setParent(0);
                        root.isRoot = true;
                        block.setNode(null);
                    }
                } else {
                    Node parentNode = loadNode(parent);
                    Node preNode = null, nextNode = null;
                    int currId = parentNode.getChildren().indexOf(this.ind);
                    int prevId = currId - 1;
                    int nextId = currId + 1;
                    if (prevId >= 0) {
                        previous = parentNode.getChildren().get(prevId);
                        preNode = loadNode(previous);
                    }
                    if (nextId < parentNode.getChildren().size()) {
                        next = parentNode.getChildren().get(nextId);
                        nextNode = loadNode(next);
                    }
                    if (preNode != null && preNode.getChildren().size() > minNum) {
                        int size = preNode.getChildren().size();
                        Node tmp = loadNode(preNode.getChildren().get(size - 1));
                        tmp.setParent(this.ind);
                        children.add(0, tmp.ind);
                        preNode.getChildren().remove(size - 1);
                        preNode.getEntries().remove(size - 2);
                        validate(tree);
                        parentNode.updateRemove(tree);
                    } else if (nextNode != null && nextNode.getChildren().size() > minNum) {
                        Node tmp = loadNode(nextNode.getChildren().get(0));
                        tmp.setParent(this.ind);
                        children.add(tmp.ind);
                        nextNode.getChildren().remove(0);
                        nextNode.getEntries().remove(0);
                        validate(tree);
                        parentNode.updateRemove(tree);
                    } else {
                        if (preNode != null) {
                            for (int i = preNode.getChildren().size() - 1;i >= 0;i--) {
                                children.add(0, preNode.getChildren().get(i));
                                loadNode(children.get(0)).setParent(this.ind);
                            }
                            parentNode.getChildren().remove(prevId);
                            preNode.getBlock().setNode(null);
                            validate(tree);
                            parentNode.updateRemove(tree);
                        } else if (nextNode != null) {
                            for (int i = 0;i < nextNode.getChildren().size();i++) {
                                Node child = loadNode(nextNode.getChildren().get(i));
                                child.setParent(this.ind);
                                children.add(child.ind);
                            }
                            parentNode.getChildren().remove(nextId);
                            nextNode.getBlock().setNode(null);
                            validate(tree);
                            parentNode.updateRemove(tree);
                        }
                    }
                }
            }
        }

        /**
         * 检查当前节点是否含有某个键
         * @param key 键
         * @return 是否存在
         */
        public boolean containKey(Comparable key) {
            for (Map.Entry<Comparable, Object> e : entries) {
                if (e.getKey().compareTo(key) == 0) {
                    return true;
                }
            }
            return false;
        }

        /**
         * 在当前节点插入或更新一个键值对
         * @param key 键
         * @param obj 值
         */
        public void insertOrUpdate(Comparable key, Object obj) {
            Map.Entry<Comparable, Object> tmp = new AbstractMap.SimpleEntry<Comparable, Object>(key, obj);
            if (entries.size() == 0) {
                entries.add(tmp);
                return;
            }
            for (int i = 0;i < entries.size();i++) {
                if (entries.get(i).getKey().compareTo(key) == 0) {
                    entries.get(i).setValue(obj);
                    return;
                } else if (entries.get(i).getKey().compareTo(key) > 0) {
                    entries.add(i, tmp);
                    return;
                }
            }
            entries.add(entries.size(), tmp);
        }

        /**
         * 从当前节点删除一个键值对
         * @param key 键
         * @return 是否存在这个键
         */
        public boolean remove(Comparable key) {
            for (int i = 0;i < entries.size();i++) {
                if (entries.get(i).getKey().compareTo(key) == 0) {
                    entries.remove(i);
                    return true;
                }
            }
            return false;
        }
    }
    public class BPlusTree implements BPlus{
        protected int root;
        protected int order;
        protected int head;

        public BPlusTree(int order, int rootIndex) {
            if (order < 3) {
                System.err.println("order must be greater than 2");
                System.exit(0);
            }
            this.order = order;
            this.root = rootIndex;
            head = root;
        }

        public int getHead() {
            return head;
        }
        public void setHead(int head) {
            this.head = head;
        }
        public int getRoot() {
            return root;
        }
        public void setRoot(int root) {
            this.root = root;
            indexInfo.setRootIndex(root);
        }
        public int getOrder() {
            return order;
        }
        public void setOrder(int order) {
            this.order = order;
        }

        @Override
        public Object get(Comparable key) {
            return loadNode(root).get(key);
        }

        @Override
        public void remove(Comparable key) {
            loadNode(root).remove(key, this);
        }

        @Override
        public void insertOrUpdate(Comparable key, Object obj) {
            loadNode(root).insertOrUpdate(key, obj, this);
        }
    }
}
