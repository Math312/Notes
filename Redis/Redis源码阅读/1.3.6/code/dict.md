# dict

本部分主要介绍哈希结构的具体知识，代码分析请看[dict.h.md](./dict.h.md)和[dict.c.md](./dict.c.md)

## 1. 数据结构

Redis的哈希表结构与Java的HashMap结构类似，都是类似于如下的结构：

```
 ---------- ---------- ---------- ----------
|  Entry   |  Entry   |  Entry   |  Entry   |
 ---------- ---------- ---------- ----------
                |
            ----------
           |  Entry   |
            ----------
                |
            ----------
           |  Entry   |
            ----------    
```

数据结构代码如下：

```c
/* Hash表结构 */
typedef struct dict {
    /* Entry链表数组 */
    dictEntry **table;
    /* Hash表类型信息（方法信息） */
    dictType *type;
    unsigned long size;
    /* 哈希表的掩码 */
    unsigned long sizemask;
    unsigned long used;
    void *privdata;
} dict;

/* Hash表 Entry结构（链表） */
typedef struct dictEntry {
    void *key;
    void *val;
    struct dictEntry *next;
} dictEntry;
```

其中table是上面图中的数组，是一个指针数组，而每个指针指向的就是每个链表。其中type属性如下：

```c
/* Hash table types */
/* 哈希表类型 */
extern dictType dictTypeHeapStringCopyKey;
extern dictType dictTypeHeapStrings;
extern dictType dictTypeHeapStringCopyKeyValue;

dictType dictTypeHeapStringCopyKey = {
    _dictStringCopyHTHashFunction,        /* hash function */
    _dictStringCopyHTKeyDup,              /* key dup */
    NULL,                               /* val dup */
    _dictStringCopyHTKeyCompare,          /* key compare */
    _dictStringCopyHTKeyDestructor,       /* key destructor */
    NULL                                /* val destructor */
};

/* This is like StringCopy but does not auto-duplicate the key.
 * It's used for intepreter's shared strings. */
dictType dictTypeHeapStrings = {
    _dictStringCopyHTHashFunction,        /* hash function */
    NULL,                               /* key dup */
    NULL,                               /* val dup */
    _dictStringCopyHTKeyCompare,          /* key compare */
    _dictStringCopyHTKeyDestructor,       /* key destructor */
    NULL                                /* val destructor */
};

/* This is like StringCopy but also automatically handle dynamic
 * allocated C strings as values. */
dictType dictTypeHeapStringCopyKeyValue = {
    _dictStringCopyHTHashFunction,        /* hash function */
    _dictStringCopyHTKeyDup,              /* key dup */
    _dictStringKeyValCopyHTValDup,        /* val dup */
    _dictStringCopyHTKeyCompare,          /* key compare */
    _dictStringCopyHTKeyDestructor,       /* key destructor */
    _dictStringKeyValCopyHTValDestructor, /* val destructor */
};
```

每个type都声明了一系列函数，用于不通类型的数据处理。size属性表示当前数组的大小，而used则标示当前哈希表使用了的空间。

Redis的1.3.6版本中Hash结构的实现是一个数组，数组中的每一项都是一个链表。至于哈希算法，Redis采用Thomas Wang 的 32 Bit / 64 Bit Mix Function ，这是一
种基于位移运算的散列方法。基于移位的散列是使用Key值进行移位操作。通常是结合左
移和右移。每个移位过程的结果进行累加，最后移位的结果作为最终结果。这种方法的
好处是避免了乘法运算，从而提高Hash函数本身的性能。

代码如下：

```c
unsigned int dictIntHashFunction(unsigned int key)
{
    key += ~(key << 15);
    key ^=  (key >> 10);
    key +=  (key << 3);
    key ^=  (key >> 6);
    key += ~(key << 11);
    key ^=  (key >> 16);
    return key;
}
```

除此之外，还有通用的散列函数：

```c
unsigned int dictGenHashFunction(const unsigned char *buf, int len) {
    unsigned int hash = 5381;

    while (len--)
        hash = ((hash << 5) + hash) + (*buf++); /* hash * 33 + c */
    return hash;
}
```

需要注意的是，哈希表的初始大小最小是默认大小，即

```c
#define DICT_HT_INITIAL_SIZE     4
```

如果小于4，则会默认设置为4，如果大于4，则设置为大于等于该值的第一个2的幂。这样做也使计算方便快速很多，所有的计算均可以采用位运算而不是四则运算。

# 2. 基本操作

## 2.1 增加节点

Redis的哈希结构，节点增加操作如下：

```c
/* Add an element to the target hash table */
/* 添加操作 */
int dictAdd(dict *ht, void *key, void *val)
{
    int index;
    dictEntry *entry;

    /* Get the index of the new element, or -1 if
     * the element already exists. */
    /* 查看key是否存在，如果存在则抛出异常 */
    if ((index = _dictKeyIndex(ht, key)) == -1)
        return DICT_ERR;

    /* Allocates the memory and stores key */
    /* 分配空间 */
    entry = _dictAlloc(sizeof(*entry));
    /* 将节点信息添加到链表头 */
    entry->next = ht->table[index];
    ht->table[index] = entry;

    /* Set the hash entry fields. */
    dictSetHashKey(ht, entry, key);
    dictSetHashVal(ht, entry, val);
    ht->used++;
    return DICT_OK;
}
```

大致增加步骤如下：

1. 调用_dictKeyIndex()函数考察要增加的节点是否存在
2. 如果存在，则返回添加失败
3. 如果不存在：
   1. 为新节点分配空间
   2. 将新节点添加到链表头
   3. 更新used(已使用节点)

_dictKeyIndex()函数如下：

```c
/* Returns the index of a free slot that can be populated with
 * an hash entry for the given 'key'.
 * If the key already exists, -1 is returned. */
 /* 根据给定的key，返回该key在hash表中的index，如果key已经存在则返回-1 */
static int _dictKeyIndex(dict *ht, const void *key)
{
    unsigned int h;
    dictEntry *he;

    /* Expand the hashtable if needed */
    /* 如果需要扩展则返回-1 */
    if (_dictExpandIfNeeded(ht) == DICT_ERR)
        return -1;
    /* Compute the key hash value */
    h = dictHashKey(ht, key) & ht->sizemask;
    /* Search if this slot does not already contain the given key */
    he = ht->table[h];
    while(he) {
        if (dictCompareHashKeys(ht, key, he->key))
            return -1;
        he = he->next;
    }
    return h;
}
```
_dictKeyIndex()函数操作流程如下：

1. 首先判断哈希表是否需要扩展
2. 根据哈希函数生成哈希值
3. 循环遍历哈希表查找是否具有与输入的key相同的key。
4. 如果有，则返回-1
5. 否则返回对应的哈希值

考察_dictExpandIfNeeded()函数：

```c
/* Expand the hash table if needed */
/* 哈希表扩容 */
static int _dictExpandIfNeeded(dict *ht)
{
    /* If the hash table is empty expand it to the intial size,
     * if the table is "full" dobule its size. */
    /* 如果是空表扩展到初始大小，否则以二倍扩展 */
    if (ht->size == 0)
        return dictExpand(ht, DICT_HT_INITIAL_SIZE);
    if (ht->used == ht->size)
        return dictExpand(ht, ht->size*2);
    return DICT_OK;
}
```

如果是空列表扩展到默认大小，如果不是，则扩展到原来的二倍。

```c
/* Expand or create the hashtable */
/* 扩张或者创建hash表 */
int dictExpand(dict *ht, unsigned long size)
{
    dict n; /* the new hashtable */
    /* 由于redis的哈希表大小始终都为2的n次方，这里取大于等于size的最小的2的n次方，如果size超过最大限制，则取最大限制 */
    unsigned long realsize = _dictNextPower(size), i;

    /* the size is invalid if it is smaller than the number of
     * elements already inside the hashtable */
    /* 如果size小于已经使用的空间，则报错 */
    if (ht->used > size)
        return DICT_ERR;
    /* 将原有非节点数据放入到新的哈希表 */
    _dictInit(&n, ht->type, ht->privdata);
    /* 并设置属性值 */
    n.size = realsize;
    n.sizemask = realsize-1;
    /*为哈希表的第一行分配当前节点数的空间*/
    n.table = _dictAlloc(realsize*sizeof(dictEntry*));

    /* Initialize all the pointers to NULL */
    /* 将哈希表中所有的数据设置为NULL */
    memset(n.table, 0, realsize*sizeof(dictEntry*));

    /* Copy all the elements from the old to the new table:
     * note that if the old hash table is empty ht->size is zero,
     * so dictExpand just creates an hash table. */
    /* 新哈希表的使用节点数与原来相同 */
    n.used = ht->used;
    /* 遍历所有节点 */
    for (i = 0; i < ht->size && ht->used > 0; i++) {
        dictEntry *he, *nextHe;
        /* 因为hash结构是一个类二维数组，先遍历第一层 */
        if (ht->table[i] == NULL) continue;
        
        /* For each hash entry on this slot... */
        /* 然后遍历该层的每个节点 */
        he = ht->table[i];
        while(he) {
            unsigned int h;

            nextHe = he->next;
            /* Get the new element index */
            /* 获取新的hash表中的index */
            h = dictHashKey(ht, he->key) & n.sizemask;
            /* 然后将链表中的节点赋值给table[h] */
            he->next = n.table[h];
            n.table[h] = he;
            ht->used--;
            /* Pass to the next element */
            he = nextHe;
        }
    }
    // 判断原有的used是否为0
    assert(ht->used == 0);
    // 释放空间
    _dictFree(ht->table);

    /* Remap the new hashtable in the old */
    // 将新的hash表指向旧的指针
    *ht = n;
    return DICT_OK;
}
```

扩展哈希表逻辑如下：

1. 取大于等于size的最小的2的幂
2. 将原始哈希表的非Entry数据放入到新的哈希表中
3. 初始化哈希表
4. 为哈希表的数组部分分配新的空间（即realize）
5. 依次遍历，并将其依次放入新哈希表中
6. 新指针指向旧指针

根据扩展逻辑可以看出，哈希表扩展是在哈希表数据达到数组数据长度的时候，并且扩展长度是二倍扩增。

综上所述，添加节点逻辑如下：

1. 考察当前哈希表是否需要初始化或者扩容，如果需要，先执行上述操作。
2. 考察添加的节点是否在原哈希表中存在。
   1. 如果存在则报错
   2. 如果不存在，则添加

## 2.2 删除节点

节点删除操作如下：

```c
/* 删除并释放空间 */
int dictDelete(dict *ht, const void *key) {
    return dictGenericDelete(ht,key,0);
}
/* 删除不释放空间 */
int dictDeleteNoFree(dict *ht, const void *key) {
    return dictGenericDelete(ht,key,1);
}
```

两者都调用了dictGenericDelete函数，唯一区别是传入的最后一个参数不同，考察dictGenericDelete函数：

```c
/* Search and remove an element */
/* 搜索并删除节点 */
static int dictGenericDelete(dict *ht, const void *key, int nofree)
{
    unsigned int h;
    dictEntry *he, *prevHe;
    /* 如果hash表长度为0，那么报错 */
    if (ht->size == 0)
        return DICT_ERR;
    h = dictHashKey(ht, key) & ht->sizemask;
    he = ht->table[h];

    prevHe = NULL;
    while(he) {
        if (dictCompareHashKeys(ht, key, he->key)) {
            /* Unlink the element from the list */
            /* 处理头结点 */
            if (prevHe)
                prevHe->next = he->next;
            else
                /* 处理一般节点 */
                ht->table[h] = he->next;
            if (!nofree) {
                /* 是否释放空间 */
                dictFreeEntryKey(ht, he);
                dictFreeEntryVal(ht, he);
            }
            _dictFree(he);
            ht->used--;
            return DICT_OK;
        }
        prevHe = he;
        he = he->next;
    }
    return DICT_ERR; /* not found */
}
```

删除节点的逻辑相对简单，仅仅是找到节点并且删除，并未考虑到缩容的情况。而之前所说的dictDelete函数和dictDeleteNoFree函数，两者只有标志位不同，该标志位如果是1，则不释放空间，如果是0，则释放空间。虽然C语言要求尽量释放掉使用的空间，但是仍然可以选择不释放，带来的好处是可以提高效率，如果一个Entry失效了就释放其空间，那么新增一个Entry就又需要再申请，频繁的释放空间对于单线程的Redis消耗很大。

Redis的内存回收策略主要体现在两个方面：
- 删除到达过期时间的键对象 
- 内存达到 maxmemory 后的淘汰机制

删除过期键对象

由于Redis进程内保存了大量的键，维护每个键的过期时间去删除键会消耗大量的CPU资源，对于单线程的Redis来说成本很高。所以Redis采用惰性删除 + 定时任务删除机制来实现过期键的内存回收。

惰性删除：当客户端读取键时，如果键带有过期时间并且已经过期，那么会执行删除操作并且查询命令返回空。这种机制是为了节约CPU成本，不需要单独维护一个TTL链表来处理过期的键。但是这种删除机制会导致内存不能及时得到释放，所以将结合下面的定时任务删除机制一起使用。

定时任务删除：Redis内部维护一个定时任务，用于随机获取一些带有过期属性的键，并将其中过期的键删除。来删除一些过期的冷数据。

## 2.3 查找节点

对于哈希表中数据的查找，操作方式与基本的哈希表查找相同，仅仅是先算出hashKey，然后通过hashKey获取到链表，再对链表进行遍历。代码如下：

```c
/* 查找节点 */
dictEntry *dictFind(dict *ht, const void *key)
{
    dictEntry *he;
    unsigned int h;

    if (ht->size == 0) return NULL;
    h = dictHashKey(ht, key) & ht->sizemask;
    he = ht->table[h];
    /* 遍历链表进行查找 */
    while(he) {
        if (dictCompareHashKeys(ht, key, he->key))
            return he;
        he = he->next;
    }
    return NULL;
}
```