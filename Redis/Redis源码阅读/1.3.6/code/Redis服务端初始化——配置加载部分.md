# Redis服务端（1.3.6版本）初始化——配置加载部分

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

## Redis服务端配置数据结构

```c
* Global server state structure */
struct redisServer {
    /* 端口号 */
    int port;
    /* file description 文件描述符  多路复用API使用 */
    int fd;
    /* db数组，数据存储位置 */
    redisDb *db;
    dict *sharingpool;          /* Poll used for object sharing */
    unsigned int sharingpoolsize;
    /* 距离上一次save，数据更改次数 */
    long long dirty;            /* changes to DB from the last save */
    /* 连接的客户端数 */
    list *clients;
    /* 从节点和监听节点 */
    list *slaves, *monitors;
    char neterr[ANET_ERR_LEN];
    /* 事件循环，多路复用API使用 */
    aeEventLoop *el;
    /* 定时任务循环 */
    int cronloops;              /* number of times the cron function run */
    /* Redis的key-value内存释放不是立即释放，这样避免了频繁释放空间导致消耗资源过多 */
    list *objfreelist;          /* A list of freed objects to avoid malloc() */
    /* 上一次save的时间 */
    time_t lastsave;            /* Unix time of last save succeeede */
    /* Fields used only for stats */
    /* 服务器启动时间 */
    time_t stat_starttime;         /* server start time */
    /* 处理命令数 */
    long long stat_numcommands;    /* number of processed commands */
    /* 连接数 */
    long long stat_numconnections; /* number of connections received */
    /* Configuration */
    /* 配置部分 */
    /* 日志级别 */
    int verbosity;
    /* Redis支持小数据打包成打数据进行打包发送，使用该配置开启 */
    int glueoutputbuf;
    /* 客户端最大空闲时间 */
    int maxidletime;
    /* 数据库个数 */
    int dbnum;
    /* 是否按照守护进程运行 */
    int daemonize;
    /* 是否每条修改都进行日志记录 */
    int appendonly;
    /* 更新日志策略
    指定更新日志条件，共有 3 个可选值：
        no：表示等操作系统进行数据缓存同步到磁盘（快）
        always：表示每次更新操作后手动调用 fsync() 将数据写到磁盘（慢，安全）
        everysec：表示每秒同步一次（折中，默认值）
     */
    int appendfsync;
    /* 上一次更新时间 */
    time_t lastfsync;
    int appendfd;
    int appendseldb;
    /* pid文件位置 */
    char *pidfile;
    /* bgsave子进程pid */
    pid_t bgsavechildpid;
    /* bgwrite子进程pid */
    pid_t bgrewritechildpid;
    /* bgwrite buffer */
    sds bgrewritebuf; /* buffer taken by parent during oppend only rewrite */
    /* save参数 */
    struct saveparam *saveparams;
    int saveparamslen;
    char *logfile;
    char *bindaddr;
    char *dbfilename;
    /* aof文件名 */
    char *appendfilename;
    char *requirepass;
    int shareobjects;
    int rdbcompression;
    /* Replication related */
    int isslave;
    char *masterauth;
    char *masterhost;
    int masterport;
    redisClient *master;    /* client that is master for this slave */
    int replstate;
    unsigned int maxclients;
    unsigned long long maxmemory;
    unsigned int blpop_blocked_clients;
    unsigned int vm_blocked_clients;
    /* Sort parameters - qsort_r() is only available under BSD so we
     * have to take this state global, in order to pass it to sortCompare() */
    int sort_desc;
    int sort_alpha;
    int sort_bypattern;
    /* 以下部分先不考虑 */
    /* Virtual memory configuration */
    int vm_enabled;
    char *vm_swap_file;
    off_t vm_page_size;
    off_t vm_pages;
    unsigned long long vm_max_memory;
    /* Hashes config */
    size_t hash_max_zipmap_entries;
    size_t hash_max_zipmap_value;
    /* Virtual memory state */
    FILE *vm_fp;
    int vm_fd;
    off_t vm_next_page; /* Next probably empty page */
    off_t vm_near_pages; /* Number of pages allocated sequentially */
    unsigned char *vm_bitmap; /* Bitmap of free/used pages */
    time_t unixtime;    /* Unix time sampled every second. */
    /* Virtual memory I/O threads stuff */
    /* An I/O thread process an element taken from the io_jobs queue and
     * put the result of the operation in the io_done list. While the
     * job is being processed, it's put on io_processing queue. */
    list *io_newjobs; /* List of VM I/O jobs yet to be processed */
    list *io_processing; /* List of VM I/O jobs being processed */
    list *io_processed; /* List of VM I/O jobs already processed */
    list *io_ready_clients; /* Clients ready to be unblocked. All keys loaded */
    pthread_mutex_t io_mutex; /* lock to access io_jobs/io_done/io_thread_job */
    pthread_mutex_t obj_freelist_mutex; /* safe redis objects creation/free */
    pthread_mutex_t io_swapfile_mutex; /* So we can lseek + write */
    pthread_attr_t io_threads_attr; /* attributes for threads creation */
    int io_active_threads; /* Number of running I/O threads */
    int vm_max_threads; /* Max number of I/O threads running at the same time */
    /* Our main thread is blocked on the event loop, locking for sockets ready
     * to be read or written, so when a threaded I/O operation is ready to be
     * processed by the main thread, the I/O thread will use a unix pipe to
     * awake the main thread. The followings are the two pipe FDs. */
    int io_ready_pipe_read;
    int io_ready_pipe_write;
    /* Virtual memory stats */
    unsigned long long vm_stats_used_pages;
    unsigned long long vm_stats_swapped_objects;
    unsigned long long vm_stats_swapouts;
    unsigned long long vm_stats_swapins;
    FILE *devnull;
};

```

## 初始化Redis服务端基本配置

初始化Redis服务端基本配置的代码为：

```c
initServerConfig();
```

由于Redis配置文件的配置并不能满足将所有配置都包含，因此，需要在从配置文件中加载配置之前首先进行一次初始化。用户不使用配置文件时，也可以保证配置的完整性。相比于先判断是否有配置文件，再根据配置文件加载，这种类似于覆盖的实现方式优雅的多，不过也是一种很正常的实现方式啦。

考察initServerConfig()函数如下：

```c
/* 初始化Redis状态 */
static void initServerConfig() {
    /*Redis默认数据库个数为16 */
    server.dbnum = REDIS_DEFAULT_DBNUM;
    /*Redis的服务端端口号是6379 */
    server.port = REDIS_SERVERPORT;
    /*Redis的日志级别默认为Verbose */
    /* 
    ##指定日志记录级别，Redis总共支持四个级别：debug、verbose、notice、warning，默认为verbose
    ## debug （大量信息，对开发/测试有用）
    ## verbose （很多精简的有用信息，但是不像debug等级那么多）
    ## notice （适量的信息，基本上是你生产环境中需要的）
    ## warning （只有很重要/严重的信息会记录下来）*/
    server.verbosity = REDIS_VERBOSE;
    /*Redis的最大空闲时间 */
    server.maxidletime = REDIS_MAXIDLETIME;
    /*Redis持久化规则的配置参数*/
    server.saveparams = NULL;
    /*Log存储文件 */
    server.logfile = NULL; /* NULL = log on standard output */
    /*绑定地址 */
    server.bindaddr = NULL;
    server.glueoutputbuf = 1;
    /* 是否按照守护进程的方式执行 */
    server.daemonize = 0;
    /* 是否在每次操作后都进行日志记录 */
    server.appendonly = 0;
    /* 日志同步策略 */
    server.appendfsync = APPENDFSYNC_ALWAYS;
    /*最后一次的同步时间 */
    server.lastfsync = time(NULL);
    server.appendfd = -1;
    server.appendseldb = -1; /* Make sure the first time will not match */
    /*pid文件 */
    server.pidfile = "/var/run/redis.pid";
    /*持久化RDB文件 */
    server.dbfilename = "dump.rdb";
    /* aof文件名称 */
    server.appendfilename = "appendonly.aof";
    /* Redis服务端密码验证 */
    server.requirepass = NULL;
    /* 共享对象 */
    server.shareobjects = 0;
    /* 是否对存储到本地的数据进行压缩，默认压缩 */
    server.rdbcompression = 1;
    server.sharingpoolsize = 1024;
    /* 设置同一时间的客户端最大连接数，如果是0，则表示不限制 */
    server.maxclients = 0;
    /* blpop阻塞客户端 */
    server.blpop_blocked_clients = 0;
    /* 占用最大内存 */
    server.maxmemory = 0;
    server.vm_enabled = 0;
    server.vm_swap_file = zstrdup("/tmp/redis-%p.vm");
    server.vm_page_size = 256;          /* 256 bytes per page */
    server.vm_pages = 1024*1024*100;    /* 104 millions of pages */
    server.vm_max_memory = 1024LL*1024*1024*1; /* 1 GB of RAM */
    server.vm_max_threads = 4;
    server.vm_blocked_clients = 0;
    /* zipmap配置 */
    server.hash_max_zipmap_entries = REDIS_HASH_MAX_ZIPMAP_ENTRIES;
    server.hash_max_zipmap_value = REDIS_HASH_MAX_ZIPMAP_VALUE;
    /*重置服务器存储参数 */
    resetServerSaveParams();
    /* 默认一小时如果更改了一次就进行持久化 */
    appendServerSaveParams(60*60,1);  /* save after 1 hour and 1 change */
    /* 默认5分钟如果更改了100次就进行持久化 */
    appendServerSaveParams(300,100);  /* save after 5 minutes and 100 changes */
    /* 默认1分钟如果更改了10000次数据就进行持久化 */
    appendServerSaveParams(60,10000); /* save after 1 minute and 10000 changes */
    /* Replication related */
    /* 复制相关 */
    server.isslave = 0;
    server.masterauth = NULL;
    server.masterhost = NULL;
    server.masterport = 6379;
    server.master = NULL;
    server.replstate = REDIS_REPL_NONE;

    /* Double constants initialization */
    R_Zero = 0.0;
    R_PosInf = 1.0/R_Zero;
    R_NegInf = -1.0/R_Zero;
    R_Nan = R_Zero/R_Zero;
}

```

上述代码保存了Redis的基本配置，由于vm即虚拟内存部分在后续版本删除，这里不对其进行更多的描述。

对于Redis数据的持久化，配置代码如下：

```c
/*重置服务器存储参数 */
    resetServerSaveParams();
    /* 默认一小时如果更改了一次就进行持久化 */
    appendServerSaveParams(60*60,1);  /* save after 1 hour and 1 change */
    /* 默认5分钟如果更改了100次就进行持久化 */
    appendServerSaveParams(300,100);  /* save after 5 minutes and 100 changes */
    /* 默认1分钟如果更改了10000次数据就进行持久化 */
    appendServerSaveParams(60,10000); /* save after 1 minute and 10000 changes */
```

考察resetServerSaveParams()函数：

```c
static void resetServerSaveParams() {
    /*重置服务器存储参数 */
    zfree(server.saveparams);
    server.saveparams = NULL;
    server.saveparamslen = 0;
}

/*在seconds秒内进行了changes改变就进行持久化*/
struct saveparam {
    time_t seconds;
    int changes;
};
/*saveparams数组*/
struct saveparam *saveparams;
int saveparamslen;
```

该函数只是简单清空saveparam数组并释放空间。具体的saveparam意义上面的注释也描述的相当清楚了。

考虑appendServerSaveParams(60*60,1)函数：

```c
static void appendServerSaveParams(time_t seconds, int changes) {
    /* 分配空间 */
    server.saveparams = zrealloc(server.saveparams,sizeof(struct saveparam)*(server.saveparamslen+1));
    /* 设置数据 */
    server.saveparams[server.saveparamslen].seconds = seconds;
    server.saveparams[server.saveparamslen].changes = changes;
    server.saveparamslen++;
}
```

至此，Redis服务端启动的最基本默认配置配置完成。

## 根据配置文件覆盖Redis配置

根据Redis 配置文件覆盖Redis配置的主要代码如下：

```c
/* 如果提供了redis配置 即两个参数 redis-server config.conf */
if (argc == 2) {
        /* 重置持久化参数配置 */
        resetServerSaveParams();
        /* 从配置文件加载配置 */
        loadServerConfig(argv[1]);
        /* 如果输入参数超过2，则报错 */
    } else if (argc > 2) {
        fprintf(stderr,"Usage: ./redis-server [/path/to/redis.conf]\n");
        exit(1);
    } else {
        /* 未提供配置文件则，打出提醒，提示使用默认配置启动 */
        redisLog(REDIS_WARNING,"Warning: no config file specified, using the default config. In order to specify a config file use 'redis-server /path/to/redis.conf'");
    }
```

如果提供了配置文件，则会根据loadServerConfig()方法解析并设置配置：

```c
/* I agree, this is a very rudimental way to load a configuration...
   will improve later if the config gets more complex */
   /*加载服务端配置 */
static void loadServerConfig(char *filename) {
    FILE *fp;
    /*REDIS_CONFIGLINE_MAX+1 最后一行判断结尾 */
    char buf[REDIS_CONFIGLINE_MAX+1], *err = NULL;
    int linenum = 0;
    sds line = NULL;

    if (filename[0] == '-' && filename[1] == '\0')
        /*如果输入参数是-，那么就从标准输入流读取参数 */
        fp = stdin;
    else {
        /*否则直接打开文件 */
        if ((fp = fopen(filename,"r")) == NULL) {
            /* 打开文件失败，则报错 */
            redisLog(REDIS_WARNING,"Fatal error, can't open config file");
            exit(1);
        }
    }

    /* 读取配置文件并修改默认配置 */
    while(fgets(buf,REDIS_CONFIGLINE_MAX+1,fp) != NULL) {
        sds *argv;
        int argc, j;

        linenum++;
        line = sdsnew(buf);
        line = sdstrim(line," \t\r\n");

        /* Skip comments and blank lines*/
        /* 判断是否是注释 */
        if (line[0] == '#' || line[0] == '\0') {
            sdsfree(line);
            continue;
        }

        /* Split into arguments */
        /*拆分参数 */
        argv = sdssplitlen(line,sdslen(line)," ",1,&argc);
        /*字符串全部转为小写 */
        sdstolower(argv[0]);

        /* Execute config directives */
        /* 配置Redis */
        if (!strcasecmp(argv[0],"timeout") && argc == 2) {
            /*配置timeout属性到maxidletime */
            server.maxidletime = atoi(argv[1]);
            if (server.maxidletime < 0) {
                err = "Invalid timeout value"; goto loaderr;
            }
        } else if (!strcasecmp(argv[0],"port") && argc == 2) {
            /*配置端口 */
            server.port = atoi(argv[1]);
            if (server.port < 1 || server.port > 65535) {
                /*配置出错跳到loaderr */
                err = "Invalid port"; goto loaderr;
            }
        } else if (!strcasecmp(argv[0],"bind") && argc == 2) {
            /*配置绑定addr */
            server.bindaddr = zstrdup(argv[1]);
        } else if (!strcasecmp(argv[0],"save") && argc == 3) {
            /*save参数表示如果XXX秒内XX个key发生变化则进行持久化 */
            int seconds = atoi(argv[1]);
            int changes = atoi(argv[2]);
            if (seconds < 1 || changes < 0) {
                err = "Invalid save parameters"; goto loaderr;
            }
            appendServerSaveParams(seconds,changes);
        } else if (!strcasecmp(argv[0],"dir") && argc == 2) {
            /*更改进程的工作目录 */
            if (chdir(argv[1]) == -1) {
                redisLog(REDIS_WARNING,"Can't chdir to '%s': %s",
                    argv[1], strerror(errno));
                exit(1);
            }
        } else if (!strcasecmp(argv[0],"loglevel") && argc == 2) {
            /*更改log等级 */
            if (!strcasecmp(argv[1],"debug")) server.verbosity = REDIS_DEBUG;
            else if (!strcasecmp(argv[1],"verbose")) server.verbosity = REDIS_VERBOSE;
            else if (!strcasecmp(argv[1],"notice")) server.verbosity = REDIS_NOTICE;
            else if (!strcasecmp(argv[1],"warning")) server.verbosity = REDIS_WARNING;
            else {
                err = "Invalid log level. Must be one of debug, notice, warning";
                goto loaderr;
            }
        } else if (!strcasecmp(argv[0],"logfile") && argc == 2) {
            /*配置log文件 */
            FILE *logfp;
            /*如果logFile配置为stdout，则直接设置 */
            server.logfile = zstrdup(argv[1]);
            if (!strcasecmp(server.logfile,"stdout")) {
                zfree(server.logfile);
                server.logfile = NULL;
            }
            if (server.logfile) {
                /* Test if we are able to open the file. The server will not
                 * be able to abort just for this problem later... */
                /*否则尝试打开文件，进行测试 */
                logfp = fopen(server.logfile,"a");
                if (logfp == NULL) {
                    err = sdscatprintf(sdsempty(),
                        "Can't open the log file: %s", strerror(errno));
                    goto loaderr;
                }
                fclose(logfp);
            }
        } else if (!strcasecmp(argv[0],"databases") && argc == 2) {
            /*如果是数据库的数据库的话，则获取存储的数据库索引 */
            server.dbnum = atoi(argv[1]);
            if (server.dbnum < 1) {
                err = "Invalid number of databases"; goto loaderr;
            }
        } else if (!strcasecmp(argv[0],"maxclients") && argc == 2) {
            /*客户端最大链接数 */
            server.maxclients = atoi(argv[1]);
        } else if (!strcasecmp(argv[0],"maxmemory") && argc == 2) {
            /*最大内存数 */
            server.maxmemory = strtoll(argv[1], NULL, 10);
        } else if (!strcasecmp(argv[0],"slaveof") && argc == 3) {
            /*主从配置 */
            server.masterhost = sdsnew(argv[1]);
            server.masterport = atoi(argv[2]);
            server.replstate = REDIS_REPL_CONNECT;
        } else if (!strcasecmp(argv[0],"masterauth") && argc == 2) {
            /*主节点验证信息 */
        	server.masterauth = zstrdup(argv[1]);
        } else if (!strcasecmp(argv[0],"glueoutputbuf") && argc == 2) {
            /*是否将较小的输出打包为一个包发送 */
            if ((server.glueoutputbuf = yesnotoi(argv[1])) == -1) {
                err = "argument must be 'yes' or 'no'"; goto loaderr;
            }
        } else if (!strcasecmp(argv[0],"shareobjects") && argc == 2) {
            if ((server.shareobjects = yesnotoi(argv[1])) == -1) {
                err = "argument must be 'yes' or 'no'"; goto loaderr;
            }
        } else if (!strcasecmp(argv[0],"rdbcompression") && argc == 2) {
            /*指定存储至本地数据库时是否压缩数据，默认为yes，Redis采用LZF压缩，如果为了节省CPU时间，可以关闭该选项，但会导致数据库文件变的巨大 */
            if ((server.rdbcompression = yesnotoi(argv[1])) == -1) {
                err = "argument must be 'yes' or 'no'"; goto loaderr;
            }
        } else if (!strcasecmp(argv[0],"shareobjectspoolsize") && argc == 2) {
            server.sharingpoolsize = atoi(argv[1]);
            if (server.sharingpoolsize < 1) {
                err = "invalid object sharing pool size"; goto loaderr;
            }
        } else if (!strcasecmp(argv[0],"daemonize") && argc == 2) {
            /*Redis是否按照守护进程的方式运行 */
            if ((server.daemonize = yesnotoi(argv[1])) == -1) {
                err = "argument must be 'yes' or 'no'"; goto loaderr;
            }
        } else if (!strcasecmp(argv[0],"appendonly") && argc == 2) {
            /*是否在每次更新操作后都进行日志记录 */
            if ((server.appendonly = yesnotoi(argv[1])) == -1) {
                err = "argument must be 'yes' or 'no'"; goto loaderr;
            }
        } else if (!strcasecmp(argv[0],"appendfsync") && argc == 2) {
            /*
            指定更新日志条件，共有3个可选值： 
            no：表示等操作系统进行数据缓存同步到磁盘（快） 
            always：表示每次更新操作后手动调用fsync()将数据写到磁盘（慢，安全） 
            everysec：表示每秒同步一次（折衷，默认值）
             */
            if (!strcasecmp(argv[1],"no")) {
                server.appendfsync = APPENDFSYNC_NO;
            } else if (!strcasecmp(argv[1],"always")) {
                server.appendfsync = APPENDFSYNC_ALWAYS;
            } else if (!strcasecmp(argv[1],"everysec")) {
                server.appendfsync = APPENDFSYNC_EVERYSEC;
            } else {
                err = "argument must be 'no', 'always' or 'everysec'";
                goto loaderr;
            }
        } else if (!strcasecmp(argv[0],"requirepass") && argc == 2) {
            /*设置Redis的连接密码 */
            server.requirepass = zstrdup(argv[1]);
        } else if (!strcasecmp(argv[0],"pidfile") && argc == 2) {
            /* 当Redis以守护进程方式运行时，Redis默认会把pid写入/var/run/redis.pid文件，可以通过pidfile指定 */
            server.pidfile = zstrdup(argv[1]);
        } else if (!strcasecmp(argv[0],"dbfilename") && argc == 2) {
            /*指定redis的rdb文件名字 */
            server.dbfilename = zstrdup(argv[1]);
        } else if (!strcasecmp(argv[0],"vm-enabled") && argc == 2) {
            /*指定是否启动虚拟内存 */
            if ((server.vm_enabled = yesnotoi(argv[1])) == -1) {
                err = "argument must be 'yes' or 'no'"; goto loaderr;
            }
        } else if (!strcasecmp(argv[0],"vm-swap-file") && argc == 2) {
            /*虚拟内存交换区文件 */
            zfree(server.vm_swap_file);
            server.vm_swap_file = zstrdup(argv[1]);
        } else if (!strcasecmp(argv[0],"vm-max-memory") && argc == 2) {
            /*交换区的最大内存 */
            server.vm_max_memory = strtoll(argv[1], NULL, 10);
        } else if (!strcasecmp(argv[0],"vm-page-size") && argc == 2) {
            /*交换区的页大小 */
            server.vm_page_size = strtoll(argv[1], NULL, 10);
        } else if (!strcasecmp(argv[0],"vm-pages") && argc == 2) {
            /*交换区中页的数量 */
            server.vm_pages = strtoll(argv[1], NULL, 10);
        } else if (!strcasecmp(argv[0],"vm-max-threads") && argc == 2) {
            /*设置访问swap文件的线程数,最好不要超过机器的核数,如果设置为0,
            那么所有对swap文件的操作都是串行的，可能会造成比较长时间的延迟。默认值为4 */
            server.vm_max_threads = strtoll(argv[1], NULL, 10);
        } else if (!strcasecmp(argv[0],"hash-max-zipmap-entries") && argc == 2){
            /*指定在超过一定的数量或者最大的元素超过某一临界值时，采用一种特殊的哈希算法*/
            server.hash_max_zipmap_entries = strtol(argv[1], NULL, 10);
        } else if (!strcasecmp(argv[0],"hash-max-zipmap-value") && argc == 2){
            server.hash_max_zipmap_value = strtol(argv[1], NULL, 10);
        } else if (!strcasecmp(argv[0],"vm-max-threads") && argc == 2) {
            server.vm_max_threads = strtoll(argv[1], NULL, 10);
        } else {
            err = "Bad directive or wrong number of arguments"; goto loaderr;
        }
        for (j = 0; j < argc; j++)
            sdsfree(argv[j]);
        zfree(argv);
        sdsfree(line);
    }
    if (fp != stdin) fclose(fp);
    return;

loaderr:
    /*异常处理 */
    fprintf(stderr, "\n*** FATAL CONFIG FILE ERROR ***\n");
    fprintf(stderr, "Reading the configuration file, at line %d\n", linenum);
    fprintf(stderr, ">>> '%s'\n", line);
    fprintf(stderr, "%s\n", err);
    exit(1);
}
```

至此，Redis的所有配置均读入到内存中。接下来所有的操作均依赖于内存中的配置。