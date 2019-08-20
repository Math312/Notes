# Redis服务端初始化——初始化数据结构

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

## 按照配置设置是否以守护方式进行

服务器的server.daemonize属性控制着服务端是否以守护进程方式运行。如果被设置为该配置值为0，则不按照守护进程方式运行，否则按照守护进程方式运行。代码如下：

```c
    /*设置守护进程方式运行 */
    if (server.daemonize) daemonize();
```

守护进程的设置交由daemonize()方法处理：

```c
static void daemonize(void) {
    int fd;
    FILE *fp;
    /* 无法fork，退出 */
    if (fork() != 0) exit(0); /* parent exits */
    setsid(); /* create a new session */

    /* Every output goes to /dev/null. If Redis is daemonized but
     * the 'logfile' is set to 'stdout' in the configuration file
     * it will not log at all. */
    if ((fd = open("/dev/null", O_RDWR, 0)) != -1) {
        dup2(fd, STDIN_FILENO);
        dup2(fd, STDOUT_FILENO);
        dup2(fd, STDERR_FILENO);
        if (fd > STDERR_FILENO) close(fd);
    }
    /* Try to write the pid file */
    // 尝试写入到pidFile
    fp = fopen(server.pidfile,"w");
    if (fp) {
        fprintf(fp,"%d\n",getpid());
        fclose(fp);
    }
}
```

关于守护进程部分的知识，请参考[https://www.cnblogs.com/bhlsheji/p/4591484.html](https://www.cnblogs.com/bhlsheji/p/4591484.html)。

## 初始化服务器

这部分代码逻辑主要初始化server的基本数据结构，代码如下：

```c
initServer();
```
```c
static void initServer() {
    int j;

    signal(SIGHUP, SIG_IGN);
    signal(SIGPIPE, SIG_IGN);
    setupSigSegvAction();
    /*初始化devnull */
    /* 存储空设备 */
    server.devnull = fopen("/dev/null","w");
    if (server.devnull == NULL) {
        redisLog(REDIS_WARNING, "Can't open /dev/null: %s", server.neterr);
        exit(1);
    }
    /*初始化客户端链表 */
    server.clients = listCreate();
    /*初始化从节点端链表 */
    server.slaves = listCreate();
    /*初始化监听器节点链表 */
    server.monitors = listCreate();
    /*初始化保存需要释放的对象的链表 */
    server.objfreelist = listCreate();
    /* 创建共享对象，共用数据 */
    createSharedObjects();
    /*创建EventLoop */
    server.el = aeCreateEventLoop();
    /*为Redis分配数据库空间 */
    server.db = zmalloc(sizeof(redisDb)*server.dbnum);
    server.sharingpool = dictCreate(&setDictType,NULL);
    /*创建用于监听TCP连接的FD */
    server.fd = anetTcpServer(server.neterr, server.port, server.bindaddr);
    if (server.fd == -1) {
        redisLog(REDIS_WARNING, "Opening TCP port: %s", server.neterr);
        exit(1);
    }
    /*初始化各个数据库 */
    for (j = 0; j < server.dbnum; j++) {
        /*数据库中的哈希数据表 */
        server.db[j].dict = dictCreate(&dbDictType,NULL);
        /*数据库中的过期时间记录哈希表 */
        server.db[j].expires = dictCreate(&keyptrDictType,NULL);
        /*数据库中的阻塞表 */
        server.db[j].blockingkeys = dictCreate(&keylistDictType,NULL);
        /*如果开启了VM则需要使用io_keys */
        if (server.vm_enabled)
            server.db[j].io_keys = dictCreate(&keylistDictType,NULL);
        server.db[j].id = j;
    }
    server.cronloops = 0;
    /*bgsave子进程 */
    server.bgsavechildpid = -1;
    /*bgwrite子进程 */
    server.bgrewritechildpid = -1;
    /*bgwrite buffer */
    server.bgrewritebuf = sdsempty();
    server.lastsave = time(NULL);
    server.dirty = 0;
    server.stat_numcommands = 0;
    server.stat_numconnections = 0;
    server.stat_starttime = time(NULL);
    server.unixtime = time(NULL);
      /* 一毫秒执行一次serverCron */
    aeCreateTimeEvent(server.el, 1, serverCron, NULL, NULL);
    if (aeCreateFileEvent(server.el, server.fd, AE_READABLE,
        acceptHandler, NULL) == AE_ERR) oom("creating file event");

    if (server.appendonly) {
        server.appendfd = open(server.appendfilename,O_WRONLY|O_APPEND|O_CREAT,0644);
        if (server.appendfd == -1) {
            redisLog(REDIS_WARNING, "Can't open the append-only file: %s",
                strerror(errno));
            exit(1);
        }
    }

    if (server.vm_enabled) vmInit();
}

```

server使用三个链表分别存储与之相连接的client、slave、monitor。需要删除的key-value值不及时释放，而是记录在一个链表中，为了省去频繁分配空间的开销。除此之外，由于Redis采用的是多路复用API，因此需要创建对应的fd。并初始化基本的轮询事件，用于处理Redis的主要逻辑。这里需要注意如下两行代码：

```c
    aeCreateTimeEvent(server.el, 1, serverCron, NULL, NULL);
    if (aeCreateFileEvent(server.el, server.fd, AE_READABLE,
        acceptHandler, NULL) == AE_ERR) oom("creating file event");
```

其中第一行：

```c
    aeCreateTimeEvent(server.el, 1, serverCron, NULL, NULL);
```

创建了一个1ms运行一次的轮询事件，代码如下：

```c
long long aeCreateTimeEvent(aeEventLoop *eventLoop, long long milliseconds,
        aeTimeProc *proc, void *clientData,
        aeEventFinalizerProc *finalizerProc)
{
    /* 时间事件ID */
    long long id = eventLoop->timeEventNextId++;
    /* 创建事件 */
    aeTimeEvent *te;

    te = zmalloc(sizeof(*te));
    if (te == NULL) return AE_ERR;
    te->id = id;
    / * 获取对应的执行时间 */
    aeAddMillisecondsToNow(milliseconds,&te->when_sec,&te->when_ms);
    / * 指定时间执行的程序*/
    te->timeProc = proc;
    /* 析构程序 */
    te->finalizerProc = finalizerProc;
    /* 客户端数据 */
    te->clientData = clientData;
    /* 将事件添加到eventLoop */
    te->next = eventLoop->timeEventHead;
    eventLoop->timeEventHead = te;
    return id;
}
```

由于阅读上面源码可知，

```c
    aeCreateTimeEvent(server.el, 1, serverCron, NULL, NULL);
```

serverCron即是定时任务的主体程序，考察该部分程序：

```c
/* 方法名就说了是服务器的定时任务 */
static int serverCron(struct aeEventLoop *eventLoop, long long id, void *clientData) {
	int j, loops = server.cronloops++;
    REDIS_NOTUSED(eventLoop);
    REDIS_NOTUSED(id);
    REDIS_NOTUSED(clientData);

    /* We take a cached value of the unix time in the global state because
     * with virtual memory and aging there is to store the current time
     * in objects at every object access, and accuracy is not needed.
     * To access a global var is faster than calling time(NULL) */
     /* 在全局变量中获取unix的时间，因为在很多地方会存储time，并不需要准确性，而调用time(NULL)比访问全局变量更耗时。 */
    server.unixtime = time(NULL);

    /* Show some info about non-empty databases */
    for (j = 0; j < server.dbnum; j++) {
        long long size, used, vkeys;
        /* 打印redis各个db的占用信息以及过期信息 */
        size = dictSlots(server.db[j].dict);
        used = dictSize(server.db[j].dict);
        vkeys = dictSize(server.db[j].expires);
        if (!(loops % 5) && (used || vkeys)) {
            redisLog(REDIS_VERBOSE,"DB %d: %lld keys (%lld volatile) in %lld slots HT.",j,used,vkeys,size);
            /* dictPrintStats(server.dict); */
        }
    }

    /* We don't want to resize the hash tables while a bacground saving
     * is in progress: the saving child is created using fork() that is
     * implemented with a copy-on-write semantic in most modern systems, so
     * if we resize the HT while there is the saving child at work actually
     * a lot of memory movements in the parent will cause a lot of pages
     * copied. */
     /*bgsave的过程中不进行hash表的resize，因为进行Redis的bgsave使用的是COW的形式，这意味着父子线程使用同一块内存，一旦进行了
      * bgsave会导致内存移动。*/
    if (server.bgsavechildpid == -1) tryResizeHashTables();

    /* Show information about connected clients */
    /* 每循环5次，则打印Redis连接的客户端信息 */
    if (!(loops % 5)) {
        redisLog(REDIS_VERBOSE,"%d clients connected (%d slaves), %zu bytes in use, %d shared objects",
            listLength(server.clients)-listLength(server.slaves),
            listLength(server.slaves),
            zmalloc_used_memory(),
            dictSize(server.sharingpool));
    }

    /* Close connections of timedout clients */

     /* 如果最大超时时间被设置了，并且进行了10次该事件或者有客户端被阻塞 */
     /* 关闭空闲超时阻塞客户端*/
    if ((server.maxidletime && !(loops % 10)) || server.blpop_blocked_clients)
        closeTimedoutClients();

    /* Check if a background saving or AOF rewrite in progress terminated */
    /* 检查正在进行后台保存或AOF重写是否已终止 */
    if (server.bgsavechildpid != -1 || server.bgrewritechildpid != -1) {
        int statloc;
        pid_t pid;
        	/* wait3函数获取子进程状态与子进程资源使用情况 */
        if ((pid = wait3(&statloc,WNOHANG,NULL)) != 0) {
        		/* 分别处理bgsave和bgwrite */
            if (pid == server.bgsavechildpid) {
                backgroundSaveDoneHandler(statloc);
            } else {
                backgroundRewriteDoneHandler(statloc);
            }
        }
    } else {
        /* If there is not a background saving in progress check if
         * we have to save now */
         /* 如果现在没有bgsave正在执行，检测是否需要bgsave */
         time_t now = time(NULL);
         for (j = 0; j < server.saveparamslen; j++) {
            struct saveparam *sp = server.saveparams+j;
            	/* 如果截止到上次bgsave，现在的数据更改次数大于changes的值，并且上一次更新事件距离现在超过配置的更新时间，则执行bgsave */
            if (server.dirty >= sp->changes &&
                now-server.lastsave > sp->seconds) {
                redisLog(REDIS_NOTICE,"%d changes in %d seconds. Saving...",
                    sp->changes, sp->seconds);
                /* 执行bgsave */
                rdbSaveBackground(server.dbfilename);
                break;
            }
         }
    }

    /* Try to expire a few timed out keys. The algorithm used is adaptive and
     * will use few CPU cycles if there are few expiring keys, otherwise
     * it will get more aggressive to avoid that too much memory is used by
     * keys that can be removed from the keyspace. */
    /*尝试处理超时的key
     处理流程：
		轮询所有的数据库，查看expires数组中是否具有超时的key，如果超时的key大于等于REDIS_EXPIRELOOKUPS_PER_CRON，
		则处理REDIS_EXPIRELOOKUPS_PER_CRON个key，否则处理所有的key。
		如果回收了超过1/4，则重复上述过程。
     * */
    // 为了保证程序的流畅运行，处理expire时，不处理完全
    // todo  为什么要判断expired > REDIS_EXPIRELOOKUPS_PER_CRON/4
    for (j = 0; j < server.dbnum; j++) {
        int expired;
        redisDb *db = server.db+j;

        /* Continue to expire if at the end of the cycle more than 25%
         * of the keys were expired. */
        do {
            /* 获取数据库中过期的key-value */
            long num = dictSize(db->expires);
            time_t now = time(NULL);

            expired = 0;
            /* 每次最大过期的key数量 */
            if (num > REDIS_EXPIRELOOKUPS_PER_CRON)
                num = REDIS_EXPIRELOOKUPS_PER_CRON;
            while (num--) {
                dictEntry *de;
                time_t t;
            /* 清除过期key */
             if ((de = dictGetRandomKey(db->expires)) == NULL) break;
                t = (time_t) dictGetEntryVal(de);
                if (now > t) {
                    deleteKey(db,dictGetEntryKey(de));
                    expired++;
                }
            }
            /*清除过期key到达总数的1/4的情况下，重复清除操作*/
        } while (expired > REDIS_EXPIRELOOKUPS_PER_CRON/4);
    }

    /* Swap a few keys on disk if we are over the memory limit and VM
     * is enbled. Try to free objects from the free list first. */
    if (vmCanSwapOut()) {
        while (server.vm_enabled && zmalloc_used_memory() >
                server.vm_max_memory)
        {
            int retval;

            if (tryFreeOneObjectFromFreelist() == REDIS_OK) continue;
            retval = (server.vm_max_threads == 0) ?
                        vmSwapOneObjectBlocking() :
                        vmSwapOneObjectThreaded();
            if (retval == REDIS_ERR && (loops % 30) == 0 &&
                zmalloc_used_memory() >
                (server.vm_max_memory+server.vm_max_memory/10))
            {
                redisLog(REDIS_WARNING,"WARNING: vm-max-memory limit exceeded by more than 10%% but unable to swap more objects out!");
            }
            /* Note that when using threade I/O we free just one object,
             * because anyway when the I/O thread in charge to swap this
             * object out will finish, the handler of completed jobs
             * will try to swap more objects if we are still out of memory. */
            if (retval == REDIS_ERR || server.vm_max_threads > 0) break;
        }
    }

    /* Check if we should connect to a MASTER */
    /* 检测是否需要连接到主服务器 */
    if (server.replstate == REDIS_REPL_CONNECT) {
        redisLog(REDIS_NOTICE,"Connecting to MASTER...");
        /* 主从同步 */
        if (syncWithMaster() == REDIS_OK) {
            redisLog(REDIS_NOTICE,"MASTER <-> SLAVE sync succeeded");
        }
    }
    return 1000;
}
```

Redis Server的主业务循环:

1. 更新unixtime时间，用于缓存时间，因为调用函数查询与从内存中查询相比性能差距过大。
2. 显示数据库信息。
3. 如果正在执行bgsave则不进行resize操作，否则尝试resize
4. 打印客户端连接信息。
5. 检测客户端超时情况，关闭超时客户端
6. 处理bgsave和bgwrite
7. 处理过期的key-value
8. 处理主从同步问题

这一部分具体逻辑将会在下一篇文章进行分析。

## 检测Linux的内存分配策略

由于Redis需要动态进行内存分配，需要考虑Linux的内存分配策略，具体查询代码如下：

```c
void linuxOvercommitMemoryWarning(void) {
    /*考察Linux内存分配策略 */
    if (linuxOvercommitMemoryValue() == 0) {
        redisLog(REDIS_WARNING,"WARNING overcommit_memory is set to 0! Background save may fail under low condition memory. To fix this issue add 'vm.overcommit_memory = 1' to /etc/sysctl.conf and then reboot or run the command 'sysctl vm.overcommit_memory=1' for this to take effect.");
    }
}

#ifdef __linux__
int linuxOvercommitMemoryValue(void) {
    FILE *fp = fopen("/proc/sys/vm/overcommit_memory","r");
    char buf[64];

    if (!fp) return -1;
    if (fgets(buf,64,fp) == NULL) {
        fclose(fp);
        return -1;
    }
    fclose(fp);

    return atoi(buf);
}
```

主要逻辑就是考察`/proc/sys/vm/overcommit_memory`文件，该文件指定了内核针对内存分配的策略，其值可以是0、1、2。
- 0，表示内核将检查是否有足够的可用内存供应用进程使用；如果有足够的可用内存，内存申请允许；否则，内存申请失败，并把错误返回给应用进程。
- 1，表示内核允许分配所有的物理内存，而不管当前的内存状态如何。
- 2，表示内核允许分配超过所有物理内存和交换空间总和的内存（参照overcommit_ratio）。
