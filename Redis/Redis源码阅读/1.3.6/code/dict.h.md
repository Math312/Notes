# dict.h

redis的哈希表实现类似于Java的HashMap。

```c
/* Hash Tables Implementation.
 *
 * This file implements in memory hash tables with insert/del/replace/find/
 * get-random-element operations. Hash tables will auto resize if needed
 * tables of power of two in size are used, collisions are handled by
 * chaining. See the source code for more information... :)
 *
 * Copyright (c) 2006-2010, Salvatore Sanfilippo <antirez at gmail dot com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *   * Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *   * Neither the name of Redis nor the names of its contributors may be used
 *     to endorse or promote products derived from this software without
 *     specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

#ifndef __DICT_H
#define __DICT_H

#define DICT_OK 0
#define DICT_ERR 1

/* Unused arguments generate annoying warnings... */
#define DICT_NOTUSED(V) ((void) V)

/* Hash表 Entry结构（链表） */
typedef struct dictEntry {
    void *key;
    void *val;
    struct dictEntry *next;
} dictEntry;

typedef struct dictType {
    unsigned int (*hashFunction)(const void *key);
    /* key复制 */
    void *(*keyDup)(void *privdata, const void *key);
    /* value复制 */
    void *(*valDup)(void *privdata, const void *obj);
    int (*keyCompare)(void *privdata, const void *key1, const void *key2);
    /* key的析构器 */
    void (*keyDestructor)(void *privdata, void *key);
    /* value的析构器 */
    void (*valDestructor)(void *privdata, void *obj);
} dictType;
/* Hash表外层实现 */
typedef struct dict {
    /* Entry链表数组 */
    dictEntry **table;
    /* Hash表类型信息（方法信息） */
    dictType *type;
    unsigned long size;
    unsigned long sizemask;
    unsigned long used;
    void *privdata;
} dict;

/* 哈希表迭代器 */
typedef struct dictIterator {
    dict *ht;
    int index;
    dictEntry *entry, *nextEntry;
} dictIterator;

/* This is the initial size of every hash table */
/* 哈希表的默认初始化大小 */
#define DICT_HT_INITIAL_SIZE     4

/* ------------------------------- Macros ------------------------------------*/
/* 释放Entry的值（调用valDestructor） */
#define dictFreeEntryVal(ht, entry) \
    if ((ht)->type->valDestructor) \
        (ht)->type->valDestructor((ht)->privdata, (entry)->val)
/* 为entry设置value */
#define dictSetHashVal(ht, entry, _val_) do { \
    if ((ht)->type->valDup) \
        entry->val = (ht)->type->valDup((ht)->privdata, _val_); \
    else \
        entry->val = (_val_); \
} while(0)

/* 释放key的值 */
#define dictFreeEntryKey(ht, entry) \
    if ((ht)->type->keyDestructor) \
        (ht)->type->keyDestructor((ht)->privdata, (entry)->key)

/* 设置Entry的key */
#define dictSetHashKey(ht, entry, _key_) do { \
    if ((ht)->type->keyDup) \
        entry->key = (ht)->type->keyDup((ht)->privdata, _key_); \
    else \
        entry->key = (_key_); \
} while(0)

/* 比较Entry的key */
#define dictCompareHashKeys(ht, key1, key2) \
    (((ht)->type->keyCompare) ? \
        (ht)->type->keyCompare((ht)->privdata, key1, key2) : \
        (key1) == (key2))

/* 调用hash函数 */
#define dictHashKey(ht, key) (ht)->type->hashFunction(key)

#define dictGetEntryKey(he) ((he)->key)
#define dictGetEntryVal(he) ((he)->val)
#define dictSlots(ht) ((ht)->size)
#define dictSize(ht) ((ht)->used)

/* API */
/* 创建hash表 */
dict *dictCreate(dictType *type, void *privDataPtr);
/* hash表扩容 */
int dictExpand(dict *ht, unsigned long size);
/* 添加一个hash节点 */
int dictAdd(dict *ht, void *key, void *val);
/* 替换一个hash节点 */
int dictReplace(dict *ht, void *key, void *val);
/* 根据key进行删除 */
int dictDelete(dict *ht, const void *key);
/* 删除即诶的那但是不释放空间 */
int dictDeleteNoFree(dict *ht, const void *key);
/* 清空整个hash结构 */
void dictRelease(dict *ht);
/* 根据key进行查找 */
dictEntry * dictFind(dict *ht, const void *key);
/* 缩容 */
int dictResize(dict *ht);
/* 获取迭代器 */
dictIterator *dictGetIterator(dict *ht);
/* 迭代器的下一个 */
dictEntry *dictNext(dictIterator *iter);
/* 释放迭代器 */
void dictReleaseIterator(dictIterator *iter);
/* 获取随机一个Entry */
dictEntry *dictGetRandomKey(dict *ht);
/* 打印哈希表状态 */
void dictPrintStats(dict *ht);
/* 生成哈希函数 */
unsigned int dictGenHashFunction(const unsigned char *buf, int len);
/* 清空hash表 */
void dictEmpty(dict *ht);

/* Hash table types */
/* 哈希表类型 */
extern dictType dictTypeHeapStringCopyKey;
extern dictType dictTypeHeapStrings;
extern dictType dictTypeHeapStringCopyKeyValue;

#endif /* __DICT_H */

```