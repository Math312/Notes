# Redis服务端初始化——初始化内存数据

Redis服务端代码主要放在redis.c文件中。主要负责服务端业务逻辑实现，由上到下分为头文件引用、宏定义、数据结构、函数声明、全局变量、业务实现，几部分组成。本文主要讨论Redis服务端的初始化过程。

Redis服务端的main函数如下：

```c
int main(int argc, char **argv) {
    time_t start;
    /* 初始化服务器配置 */
    initServerConfig();
    /* 加载配置 */
    if (argc == 2) {
        /* 重置服务端持久化参数 */
        resetServerSaveParams();
        /* 加载配置文件配置*/
        loadServerConfig(argv[1]);
    } else if (argc > 2) {
        fprintf(stderr,"Usage: ./redis-server [/path/to/redis.conf]\n");
        exit(1);
    } else {
        redisLog(REDIS_WARNING,"Warning: no config file specified, using the default config. In order to specify a config file use 'redis-server /path/to/redis.conf'");
    }
    /*设置守护进程方式运行 */
    if (server.daemonize) daemonize();
    /* 初始化服务器数据结构 */
    initServer();
    redisLog(REDIS_NOTICE,"Server started, Redis version " REDIS_VERSION);
#ifdef __linux__
/*
    该文件指定了内核针对内存分配的策略，其值可以是0、1、2。
    0，表示内核将检查是否有足够的可用内存供应用进程使用；如果有足够的可用内存，内存申请允许；否则，内存申请失败，并把错误返回给应用进程。
    1，表示内核允许分配所有的物理内存，而不管当前的内存状态如何。
    2，表示内核允许分配超过所有物理内存和交换空间总和的内存（参照overcommit_ratio）。
 */
    linuxOvercommitMemoryWarning();
#endif
    start = time(NULL);
    /* 加载持久化数据 */
    if (server.appendonly) {
        if (loadAppendOnlyFile(server.appendfilename) == REDIS_OK)
            redisLog(REDIS_NOTICE,"DB loaded from append only file: %ld seconds",time(NULL)-start);
    } else {
        if (rdbLoad(server.dbfilename) == REDIS_OK)
            redisLog(REDIS_NOTICE,"DB loaded from disk: %ld seconds",time(NULL)-start);
    }
    redisLog(REDIS_NOTICE,"The server is now ready to accept connections on port %d", server.port);
    /* Redis启动部分 */
    aeSetBeforeSleepProc(server.el,beforeSleep);
    /* Redis主程序部分 */
    aeMain(server.el);
    /* Redis程序结束，清理数据 */
    aeDeleteEventLoop(server.el);
    return 0;
}
```

Redis的main函数逻辑主要是如下部分：

1. 初始化Redis服务端基本配置。
2. 从配置文件或者其他地方加载配置
3. 按照配置，设置是否以守护方式进行
4. 初始化服务器数据结构
5. 判断Linux系统内存分配方式
6. 从持久化文件中加载原有数据
7. 使用多路复用API运行Redis主程序

## 持久化数据加载

由于Redis数据会根据saveparams进行数据持久化，因此需要在启动时将持久化的数据加载到内存中。实现逻辑代码如下：

```c
start = time(NULL);
    if (server.appendonly) {
        if (loadAppendOnlyFile(server.appendfilename) == REDIS_OK)
            redisLog(REDIS_NOTICE,"DB loaded from append only file: %ld seconds",time(NULL)-start);
    } else {
        if (rdbLoad(server.dbfilename) == REDIS_OK)
            redisLog(REDIS_NOTICE,"DB loaded from disk: %ld seconds",time(NULL)-start);
    }
```

如果开启了aof就从AOF日志中加载，否则从rdb中进行加载。从AOF日志中加载即

```c
 if (loadAppendOnlyFile(server.appendfilename) == REDIS_OK)
```

从rdb文件中加载即

```c
if (rdbLoad(server.dbfilename) == REDIS_OK)
```

接下来仔细讨论这两者。

### 从AOF日志中加载数据

AOF日志是记录用户操作的命令的日志。具体加载数据的代码如下：

```c
/* Replay the append log file. On error REDIS_OK is returned. On non fatal
 * error (the append only file is zero-length) REDIS_ERR is returned. On
 * fatal error an error message is logged and the program exists. */

int loadAppendOnlyFile(char *filename) {
    struct redisClient *fakeClient;
    FILE *fp = fopen(filename,"r");
    struct redis_stat sb;
    unsigned long long loadedkeys = 0;

    if (redis_fstat(fileno(fp),&sb) != -1 && sb.st_size == 0)
        return REDIS_ERR;

    if (fp == NULL) {
        redisLog(REDIS_WARNING,"Fatal error: can't open the append log file for reading: %s",strerror(errno));
        exit(1);
    }
    /* 创建Client */
    fakeClient = createFakeClient();
    while(1) {
        int argc, j;
        unsigned long len;
        robj **argv;
        char buf[128];
        sds argsds;
        struct redisCommand *cmd;

        if (fgets(buf,sizeof(buf),fp) == NULL) {
            if (feof(fp))
                break;
            else
                goto readerr;
        }
        /* 针对每条AOF日志 */
        /* 第一行数据的第一个字符必须是* */
        if (buf[0] != '*') goto fmterr;
        /* *后面给出了参数个数 */
        argc = atoi(buf+1);
        argv = zmalloc(sizeof(robj*)*argc);
        for (j = 0; j < argc; j++) {
            /* 分别获取参数 */
            if (fgets(buf,sizeof(buf),fp) == NULL) goto readerr;
            /* $后面的数字表示了参数的长度 */
            if (buf[0] != '$') goto fmterr;
            len = strtol(buf+1,NULL,10);
            argsds = sdsnewlen(NULL,len);
            /* 根据参数长度获取参数 */
            if (len && fread(argsds,len,1,fp) == 0) goto fmterr;
            argv[j] = createObject(REDIS_STRING,argsds);
            /* 行末是CR LF */
            if (fread(buf,2,1,fp) == 0) goto fmterr; /* discard CRLF */
        }

        /* Command lookup */
        /* 查找命令 */
        cmd = lookupCommand(argv[0]->ptr);
        if (!cmd) {
            redisLog(REDIS_WARNING,"Unknown command '%s' reading the append only file", argv[0]->ptr);
            exit(1);
        }
        /* Try object sharing and encoding */
        if (server.shareobjects) {
            int j;
            for(j = 1; j < argc; j++)
                argv[j] = tryObjectSharing(argv[j]);
        }
        if (cmd->flags & REDIS_CMD_BULK)
            tryObjectEncoding(argv[argc-1]);
        /* Run the command in the context of a fake client */
        fakeClient->argc = argc;
        fakeClient->argv = argv;
        cmd->proc(fakeClient);
        /* Discard the reply objects list from the fake client */
        /* 成功返回则删除cmd的argv并释放空间 */
        while(listLength(fakeClient->reply))
            listDelNode(fakeClient->reply,listFirst(fakeClient->reply));
        /* Clean up, ready for the next command */
        for (j = 0; j < argc; j++) decrRefCount(argv[j]);
        zfree(argv);
        /* Handle swapping while loading big datasets when VM is on */
        loadedkeys++;
        if (server.vm_enabled && (loadedkeys % 5000) == 0) {
            while (zmalloc_used_memory() > server.vm_max_memory) {
                if (vmSwapOneObjectBlocking() == REDIS_ERR) break;
            }
        }
    }
    /* 回收资源 */
    fclose(fp);
    freeFakeClient(fakeClient);
    return REDIS_OK;

/* 异常处理 */
readerr:
    if (feof(fp)) {
        redisLog(REDIS_WARNING,"Unexpected end of file reading the append only file");
    } else {
        redisLog(REDIS_WARNING,"Unrecoverable error reading the append only file: %s", strerror(errno));
    }
    exit(1);
fmterr:
    redisLog(REDIS_WARNING,"Bad file format reading the append only file");
    exit(1);
}
```
AOF日志加载逻辑如下：

1. 尝试打开AOF持久化文件
2. 创建FakeClient用于执行从AOF日志中解析出的命令
3. 循环解析AOF日志：
   1. 提取AOF日志中的命令cmd
   2. 使用FakeClient执行cmd
   3. 抹除cmd中的数据
4. 关闭持久化文件
5. 释放资源

解析AOF日志的逻辑相对较为简单，主要逻辑即是创建FakeClient，然后根据AOF日志中解析出的cmd，使用FakeClient进行执行。执行完毕后回收资源。需要注意的是这里是单线程的将数据加载到内存中，如果数据量太大就会很慢。

AOF日志的基本格式如下：

```
*参数数量CRLF
$参数长度CRLF                   //参数数量个
参数CRLF
...
```

### RDB持久化数据加载

RDB文件是redis数据库的快照形式，保存的是真实数据，具体解析代码调用：

```c
rdbLoad(server.dbfilename) == REDIS_OK
```

考察代码如下：

```c
/*加载rdb文件 */
static int rdbLoad(char *filename) {
    FILE *fp;
    robj *keyobj = NULL;
    uint32_t dbid;
    int type, retval, rdbver;
    dict *d = server.db[0].dict;
    /*指定db指针 */
    redisDb *db = server.db+0;
    char buf[1024];
    /*默认无超时时间 */
    time_t expiretime = -1, now = time(NULL);
    long long loadedkeys = 0;
    /*打开rdb文件 */
    fp = fopen(filename,"r");
    /*打开失败则报错 */
    if (!fp) return REDIS_ERR;
    /*加载rdb文件中的数据 */
    if (fread(buf,9,1,fp) == 0) goto eoferr;
    buf[9] = '\0';
    /*数据库魔法值REDIS */
    if (memcmp(buf,"REDIS",5) != 0) {
        fclose(fp);
        redisLog(REDIS_WARNING,"Wrong signature trying to load DB from file");
        return REDIS_ERR;
    }
    /* redis版本号 */
    rdbver = atoi(buf+5);
    if (rdbver != 1) {
        fclose(fp);
        redisLog(REDIS_WARNING,"Can't handle RDB format version %d",rdbver);
        return REDIS_ERR;
    }
    /*加载数据到内存 */
    while(1) {
        robj *o;

        /* Read type. */
        /* 读取类型 */
        if ((type = rdbLoadType(fp)) == -1) goto eoferr;
        /*如果类型是过期时间 */
        if (type == REDIS_EXPIRETIME) {
            /*加载过期时间数据 */
            if ((expiretime = rdbLoadTime(fp)) == -1) goto eoferr;
            /* We read the time so we need to read the object type again */
            /* 因为我们读取了时间，所以我们需要再次读取对象类型 */
            if ((type = rdbLoadType(fp)) == -1) goto eoferr;
        }
        /*如果读取到结尾则结束 */
        if (type == REDIS_EOF) break;
        /* Handle SELECT DB opcode as a special case */
        /*处理SELECT DB操作码作为特殊情况 */
        if (type == REDIS_SELECTDB) {
            /*记载Redis长度错误 */
            if ((dbid = rdbLoadLen(fp,NULL)) == REDIS_RDB_LENERR)
                goto eoferr;
            if (dbid >= (unsigned)server.dbnum) {
                redisLog(REDIS_WARNING,"FATAL: Data file was created with a Redis server configured to handle more than %d databases. Exiting\n", server.dbnum);
                exit(1);
            }
            /* 否则，将db指向对应的db */
            db = server.db+dbid;
            d = db->dict;
            continue;
        }
        /* Read key */
        /*读取String的key */
        if ((keyobj = rdbLoadStringObject(fp)) == NULL) goto eoferr;
        /* Read value */
        /*读取value */
        if ((o = rdbLoadObject(type,fp)) == NULL) goto eoferr;
        /* Add the new object in the hash table */
        /* 将数据添加到dict中 */
        retval = dictAdd(d,keyobj,o);
        /*如果添加异常，则报错 */
        if (retval == DICT_ERR) {
            redisLog(REDIS_WARNING,"Loading DB, duplicated key (%s) found! Unrecoverable error, exiting now.", keyobj->ptr);
            exit(1);
        }
        /* Set the expire time if needed */
        /*设置超时时间 */
        if (expiretime != -1) {
            setExpire(db,keyobj,expiretime);
            /* Delete this key if already expired */
            /*如果过期，则删除（为啥不先判断过期呢？？） */
            if (expiretime < now) deleteKey(db,keyobj);
            expiretime = -1;
        }
        keyobj = o = NULL;
        /* Handle swapping while loading big datasets when VM is on */
        /* 在VM启动时加载大数据集时处理交换*/
        loadedkeys++;
        if (server.vm_enabled && (loadedkeys % 5000) == 0) {
            while (zmalloc_used_memory() > server.vm_max_memory) {
                if (vmSwapOneObjectBlocking() == REDIS_ERR) break;
            }
        }
    }
    fclose(fp);
    return REDIS_OK;

eoferr: /* unexpected end of file is handled here with a fatal exit */
    if (keyobj) decrRefCount(keyobj);
    redisLog(REDIS_WARNING,"Short read or OOM loading DB. Unrecoverable error, aborting now.");
    exit(1);
    return REDIS_ERR; /* Just to avoid warning */
}
```
rdb文件的加载逻辑如下：

1. 初始化Redis 数据库数据结构
2. 打开rdb文件
3. 根据魔法值REDIS以及版本号，确认文件正确性
4. 循环加载数据
   1. 读取类型
   2. 根据类型向内存中放入数据。
      1. 类型为 超时时间：读取超时时间，并再次读取类型（因为超时时间与数据共存）
      2. 类型为 EOF：读取数据结束
      3. 类型为 SELECTDB：读取DB索引，并获取DB指针
   3. 获取key
   4. 获取value
   5. 将数据放入到内存中并设置超时时间
   6. loadedkeys ++
5. 关闭rdb文件

### 1. 初始化Redis数据库数据结构

```c
FILE *fp;
    robj *keyobj = NULL;
    uint32_t dbid;
    int type, retval, rdbver;
    dict *d = server.db[0].dict;
    /*指定db指针 */
    redisDb *db = server.db+0;
    char buf[1024];
    /*默认无超时时间 */
    time_t expiretime = -1, now = time(NULL);
    long long loadedkeys = 0;
```

这里只是初始化了基础的Redis db数据。

### 2. 打开并验证rdb文件

```c
/*打开rdb文件 */
    fp = fopen(filename,"r");
    /*打开失败则报错 */
    if (!fp) return REDIS_ERR;
```

### 3. 验证魔法值和版本号

```c
    /*加载rdb文件中的数据 */
    if (fread(buf,9,1,fp) == 0) goto eoferr;
    buf[9] = '\0';
    /*数据库魔法值REDIS */
    if (memcmp(buf,"REDIS",5) != 0) {
        fclose(fp);
        redisLog(REDIS_WARNING,"Wrong signature trying to load DB from file");
        return REDIS_ERR;
    }
    rdbver = atoi(buf+5);
    if (rdbver != 1) {
        fclose(fp);
        redisLog(REDIS_WARNING,"Can't handle RDB format version %d",rdbver);
        return REDIS_ERR;
    }
```

rdb文件中读取前9个字节，其中前五个是REDIS，后面的是版本号，而且是1，用于判断版本号是否正确。

### 4. 加载数据到内存

```c
    /*加载数据到内存 */
    while(1) {
        robj *o;

        /* Read type. */
        /* 读取类型 */
        if ((type = rdbLoadType(fp)) == -1) goto eoferr;
        /*如果类型是过期时间 */
        if (type == REDIS_EXPIRETIME) {
            /*加载过期时间数据 */
            if ((expiretime = rdbLoadTime(fp)) == -1) goto eoferr;
            /* We read the time so we need to read the object type again */
            /* 因为我们读取了时间，所以我们需要再次读取对象类型 */
            if ((type = rdbLoadType(fp)) == -1) goto eoferr;
        }
        /*如果读取到结尾则结束 */
        if (type == REDIS_EOF) break;
        /* Handle SELECT DB opcode as a special case */
        /*处理SELECT DB操作码作为特殊情况 */
        if (type == REDIS_SELECTDB) {
            /*记载Redis长度错误 */
            if ((dbid = rdbLoadLen(fp,NULL)) == REDIS_RDB_LENERR)
                goto eoferr;
            if (dbid >= (unsigned)server.dbnum) {
                redisLog(REDIS_WARNING,"FATAL: Data file was created with a Redis server configured to handle more than %d databases. Exiting\n", server.dbnum);
                exit(1);
            }
            /* 否则，将db指向对应的db */
            db = server.db+dbid;
            d = db->dict;
            continue;
        }
        /* Read key */
        /*读取String的key */
        if ((keyobj = rdbLoadStringObject(fp)) == NULL) goto eoferr;
        /* Read value */
        /*读取value */
        if ((o = rdbLoadObject(type,fp)) == NULL) goto eoferr;
        /* Add the new object in the hash table */
        /* 将数据添加到dict中 */
        retval = dictAdd(d,keyobj,o);
        /*如果添加异常，则报错 */
        if (retval == DICT_ERR) {
            redisLog(REDIS_WARNING,"Loading DB, duplicated key (%s) found! Unrecoverable error, exiting now.", keyobj->ptr);
            exit(1);
        }
        /* Set the expire time if needed */
        /*设置超时时间 */
        if (expiretime != -1) {
            setExpire(db,keyobj,expiretime);
            /* Delete this key if already expired */
            /*如果过期，则删除（为啥不先判断过期呢？？） */
            if (expiretime < now) deleteKey(db,keyobj);
            expiretime = -1;
        }
        keyobj = o = NULL;
        /* Handle swapping while loading big datasets when VM is on */
        /* 在VM启动时加载大数据集时处理交换*/
        loadedkeys++;
        if (server.vm_enabled && (loadedkeys % 5000) == 0) {
            while (zmalloc_used_memory() > server.vm_max_memory) {
                if (vmSwapOneObjectBlocking() == REDIS_ERR) break;
            }
        }
    }
```

由于Redis之中都是key-value形式，因此，两者分别进行加载，key加载使用rdbLoadStringObject()方法，value加载使用rdbLoadObject()方法，然后放入到db中。

```c
/* Load an encoded length from the DB, see the REDIS_RDB_* defines on the top
 * of this file for a description of how this are stored on disk.
 *
 * isencoded is set to 1 if the readed length is not actually a length but
 * an "encoding type", check the above comments for more info */
 /* 根据REDIS_RDB_*加载长度，即
* Defines related to the dump file format. To store 32 bits lengths for short
 * keys requires a lot of space, so we check the most significant 2 bits of
 * the first byte to interpreter the length:
 *
 * 00|000000 => if the two MSB are 00 the len is the 6 bits of this byte
 * 01|000000 00000000 =>  01, the len is 14 byes, 6 bits + 8 bits of next byte
 * 10|000000 [32 bit integer] => if it's 01, a full 32 bit len will follow
 * 11|000000 this means: specially encoded object will follow. The six bits
 *           number specify the kind of object that follows.
 *           See the REDIS_RDB_ENC_* defines.
 *
 * Lenghts up to 63 are stored using a single byte, most DB keys, and may
 * values, will fit inside. 
 * #define REDIS_RDB_6BITLEN 0
 * #define REDIS_RDB_14BITLEN 1
 * #define REDIS_RDB_32BITLEN 2
 * #define REDIS_RDB_ENCVAL 3
 * #define REDIS_RDB_LENERR UINT_MAX
 */
static uint32_t rdbLoadLen(FILE *fp, int *isencoded) {
    unsigned char buf[2];
    uint32_t len;
    int type;
    if (isencoded) *isencoded = 0;
    /* 读取类型 */
    if (fread(buf,1,1,fp) == 0) return REDIS_RDB_LENERR;
    /* buf[0] & 1100 0000 */
    /* 取头两位 */
    type = (buf[0]&0xC0)>>6;
    /* 6位长的数据 */
    if (type == REDIS_RDB_6BITLEN) {
        /* Read a 6 bit len */
        /* buf[0] &  0011 1111 */
        return buf[0]&0x3F;
        /* 指定位数的数据 */
    } else if (type == REDIS_RDB_ENCVAL) {
        /* Read a 6 bit len encoding type */
        if (isencoded) *isencoded = 1;
        return buf[0]&0x3F;
        /* 14位长的数据 */
    } else if (type == REDIS_RDB_14BITLEN) {
        /* Read a 14 bit len */
        if (fread(buf+1,1,1,fp) == 0) return REDIS_RDB_LENERR;
        return ((buf[0]&0x3F)<<8)|buf[1];
    } else {
        /* 32位长的数据 */
        /* Read a 32 bit len */
        if (fread(&len,4,1,fp) == 0) return REDIS_RDB_LENERR;
        return ntohl(len);
    }
}

static robj *rdbLoadStringObject(FILE*fp) {
    int isencoded;
    uint32_t len;
    sds val;

    len = rdbLoadLen(fp,&isencoded);
    if (isencoded) {
        switch(len) {
        case REDIS_RDB_ENC_INT8:
        case REDIS_RDB_ENC_INT16:
        case REDIS_RDB_ENC_INT32:
            return tryObjectSharing(rdbLoadIntegerObject(fp,len));
        case REDIS_RDB_ENC_LZF:
            return tryObjectSharing(rdbLoadLzfStringObject(fp));
        default:
            redisAssert(0);
        }
    }

    if (len == REDIS_RDB_LENERR) return NULL;
    val = sdsnewlen(NULL,len);
    if (len && fread(val,len,1,fp) == 0) {
        sdsfree(val);
        return NULL;
    }
    return tryObjectSharing(createObject(REDIS_STRING,val));
}
```

### 5. 关闭文件

```c
fclose(fp);
    return REDIS_OK;
```