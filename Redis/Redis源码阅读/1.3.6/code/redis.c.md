# redis.c.md

这部分是Redis Server的主要代码，首先定义了一系列的基础配置：

```c
/* 规定的Redis处理状态码 */
#define REDIS_OK                0
#define REDIS_ERR               -1

/* 静态的服务端配置 */
#define REDIS_SERVERPORT        6379    /* TCP段口号 */
#define REDIS_MAXIDLETIME       (60*5)  /* 默认的客户端超时时间 */
#define REDIS_IOBUF_LEN         1024    /* IO缓存区长度 */
#define REDIS_LOADBUF_LEN       1024    /* 加载缓存区长度 */
#define REDIS_STATIC_ARGS       4   /* 静态的参数个数 */
#define REDIS_DEFAULT_DBNUM     16  /* 默认的数据库个数 */
#define REDIS_CONFIGLINE_MAX    1024    /* 最大的配置行数 */
#define REDIS_OBJFREELIST_MAX   1000000 /* 最大的缓存对象个数 */
#define REDIS_MAX_SYNC_TIME     60      /* Slave最长的同步时间 */
#define REDIS_EXPIRELOOKUPS_PER_CRON    100 /* try to expire 100 keys/second */
#define REDIS_MAX_WRITE_PER_EVENT (1024*64)
#define REDIS_REQUEST_MAX_SIZE (1024*1024*256) /* Redis命令行的最大长度 */

/* If more then REDIS_WRITEV_THRESHOLD write packets are pending use writev */
#define REDIS_WRITEV_THRESHOLD      3
/* Max number of iovecs used for each writev call */
/*每次writev调用使用的最大iovec数*/
#define REDIS_WRITEV_IOVEC_COUNT    256

/* Hash table parameters */
/* 哈希表参数 */
#define REDIS_HT_MINFILL        10      /* Minimal hash table fill 10% */ /*哈希表最小填充10%*/

/* Command flags */
/* 命令行标示 */
#define REDIS_CMD_BULK          1       /* Bulk write command */
#define REDIS_CMD_INLINE        2       /* Inline command */
/* REDIS_CMD_DENYOOM reserves a longer comment: all the commands marked with
   this flags will return an error when the 'maxmemory' option is set in the
   config file and the server is using more than maxmemory bytes of memory.
   In short this commands are denied on low memory conditions. */
   /* 如果命令被REDIS_CMD_DENYOOM标示时，如果在配置文件中设置了maxmemory ，那么当服务器使用高于maxmemory的内存时会返回错误。简而言之，在低内存条件下会被拒绝*/
#define REDIS_CMD_DENYOOM       4

/* 对象类型 */
#define REDIS_STRING 0
#define REDIS_LIST 1
#define REDIS_SET 2
#define REDIS_ZSET 3
#define REDIS_HASH 4

/* Objects encoding. Some kind of objects like Strings and Hashes can be
 * internally represented in multiple ways. The 'encoding' field of the object
 * is set to one of this fields for this object. */
 /* 对象编码。 像字符串和哈希这样的对象可以通过多种方式在内部表示。 对象的“编码”字段设置为此对象的其中一个字段。*/
#define REDIS_ENCODING_RAW 0    /* Raw representation */
#define REDIS_ENCODING_INT 1    /* Encoded as integer */
#define REDIS_ENCODING_ZIPMAP 2 /* Encoded as zipmap */
#define REDIS_ENCODING_HT 3     /* Encoded as an hash table */

static char* strencoding[] = {
    "raw", "int", "zipmap", "hashtable"
};

/* Object types only used for dumping to disk */
/* 仅用于转储到磁盘的对象类型 */
#define REDIS_EXPIRETIME 253
#define REDIS_SELECTDB 254
#define REDIS_EOF 255

/* Defines related to the dump file format. To store 32 bits lengths for short
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
 * values, will fit inside. */
 /* 定了了关于对象文件dump的格式。因为简短的key对于32位长度多余了很多空间，所以采用检测高两位的方式去标示长度
 *
 *00|000000 如果最高两位是00，那么长度使用6bit标示
 *01|000000 00000000 如果高两位是01，那么使用14bit标示长度
 *10|000000 [32 bit integer] 如果高两位是10，那么使用32位长度标示
 *11|000000 如果是11，那么标示特殊的编码对象，6位数字制定了对象种类，详情看*REDIS_RDB_ENC_*
 * 使用单个字节存储长度最多63个，大多数DB键和值可以放在内部。
 * */
#define REDIS_RDB_6BITLEN 0
#define REDIS_RDB_14BITLEN 1
#define REDIS_RDB_32BITLEN 2
#define REDIS_RDB_ENCVAL 3
#define REDIS_RDB_LENERR UINT_MAX

/* When a length of a string object stored on disk has the first two bits
 * set, the remaining two bits specify a special encoding for the object
 * accordingly to the following defines: */
 /* 当存储在磁盘上的字符串对象的长度设置了前两位时，其余两位根据以下定义为对象指定特殊编码 */
#define REDIS_RDB_ENC_INT8 0        /* 8 bit signed integer */
#define REDIS_RDB_ENC_INT16 1       /* 16 bit signed integer */
#define REDIS_RDB_ENC_INT32 2       /* 32 bit signed integer */
#define REDIS_RDB_ENC_LZF 3         /* string compressed with FASTLZ */

/* Virtual memory object->where field. */
#define REDIS_VM_MEMORY 0       /* The object is on memory */
#define REDIS_VM_SWAPPED 1      /* The object is on disk */
#define REDIS_VM_SWAPPING 2     /* Redis is swapping this object on disk */
#define REDIS_VM_LOADING 3      /* Redis is loading this object from disk */

/* Virtual memory static configuration stuff.
 * Check vmFindContiguousPages() to know more about this magic numbers. */
#define REDIS_VM_MAX_NEAR_PAGES 65536
#define REDIS_VM_MAX_RANDOM_JUMP 4096
#define REDIS_VM_MAX_THREADS 32
#define REDIS_THREAD_STACK_SIZE (1024*1024*4)
/* The following is the *percentage* of completed I/O jobs to process when the
 * handelr is called. While Virtual Memory I/O operations are performed by
 * threads, this operations must be processed by the main thread when completed
 * in order to take effect. */
#define REDIS_MAX_COMPLETED_JOBS_PROCESSED 1

/* Client flags */
#define REDIS_SLAVE 1       /* This client is a slave server */
#define REDIS_MASTER 2      /* This client is a master server */
#define REDIS_MONITOR 4     /* This client is a slave monitor, see MONITOR */
#define REDIS_MULTI 8       /* This client is in a MULTI context */
#define REDIS_BLOCKED 16    /* The client is waiting in a blocking operation */
#define REDIS_IO_WAIT 32    /* The client is waiting for Virtual Memory I/O */

/* Slave replication state - slave side */
#define REDIS_REPL_NONE 0   /* No active replication */
#define REDIS_REPL_CONNECT 1    /* Must connect to master */
#define REDIS_REPL_CONNECTED 2  /* Connected to master */

/* Slave replication state - from the point of view of master
 * Note that in SEND_BULK and ONLINE state the slave receives new updates
 * in its output queue. In the WAIT_BGSAVE state instead the server is waiting
 * to start the next background saving in order to send updates to it. */
#define REDIS_REPL_WAIT_BGSAVE_START 3 /* master waits bgsave to start feeding it */
#define REDIS_REPL_WAIT_BGSAVE_END 4 /* master waits bgsave to start bulk DB transmission */
#define REDIS_REPL_SEND_BULK 5 /* master is sending the bulk DB */
#define REDIS_REPL_ONLINE 6 /* bulk DB already transmitted, receive updates */

/* List related stuff */
#define REDIS_HEAD 0
#define REDIS_TAIL 1

/* Sort operations */
#define REDIS_SORT_GET 0
#define REDIS_SORT_ASC 1
#define REDIS_SORT_DESC 2
#define REDIS_SORTKEY_MAX 1024

/* Log levels */
#define REDIS_DEBUG 0
#define REDIS_VERBOSE 1
#define REDIS_NOTICE 2
#define REDIS_WARNING 3

/* Anti-warning macro... */
#define REDIS_NOTUSED(V) ((void) V)

/* 跳表的最大深度 */
#define ZSKIPLIST_MAXLEVEL 32 /* Should be enough for 2^32 elements */
/* 跳表的P值 */
#define ZSKIPLIST_P 0.25      /* Skiplist P = 1/4 */

/* Append only defines */
#define APPENDFSYNC_NO 0
#define APPENDFSYNC_ALWAYS 1
#define APPENDFSYNC_EVERYSEC 2

/* Hashes related defaults */
#define REDIS_HASH_MAX_ZIPMAP_ENTRIES 64
#define REDIS_HASH_MAX_ZIPMAP_VALUE 512

```
```c
/* A redis object, that is a type able to hold a string / list / set */
/* 持有string/list/set类型的Redis对象 */
/* The VM object structure */
/* 虚拟内存对象结构 */
struct redisObjectVM {
    /* the page at witch the object is stored on disk */
    /* 存储在硬盘上的offset页数 */
    off_t page;         
    /* number of pages used on disk */
    /* 共用的页数 */
    off_t usedpages;  
    /* 最后一次的访问时间 */
    time_t atime;       /* Last access time */
    
} vm;
/* The actual Redis Object */
/* 实际的Redis对象 */
typedef struct redisObject {
    /* 指针 */
    void *ptr;
    /* 对象类型 */
    unsigned char type;
    /* 对象编码 */
    unsigned char encoding;
    /* value存储位值 */
    /* If this object is a key, where is the value?
                             * REDIS_VM_MEMORY, REDIS_VM_SWAPPED, ... */
    unsigned char storage;  
    /* If this object is a key, and value is swapped out,
      * this is the type of the swapped out object. */
      /* 交换出的对象类型 */
    unsigned char vtype; 
    /* 引用计数 */
    int refcount;
    /* VM fields, this are only allocated if VM is active, otherwise the
     * object allocation function will just allocate
     * sizeof(redisObjct) minus sizeof(redisObjectVM), so using
     * Redis without VM active will not have any overhead. */
    /* 虚拟内存域，仅仅当虚拟内存启动时，该对象占用空间会被创建，否则，不创建该空间 */
    struct redisObjectVM vm;
} robj;

/* Macro used to initalize a Redis object allocated on the stack.
 * Note that this macro is taken near the structure definition to make sure
 * we'll update it when the structure is changed, to avoid bugs like
 * bug #85 introduced exactly in this way. */
 /* 初始化Redis object使用的宏。 */
#define initStaticStringObject(_var,_ptr) do { \
    _var.refcount = 1; \
    _var.type = REDIS_STRING; \
    _var.encoding = REDIS_ENCODING_RAW; \
    _var.ptr = _ptr; \
    if (server.vm_enabled) _var.storage = REDIS_VM_MEMORY; \
} while(0);

/* Redis 数据库结构 */
typedef struct redisDb {
    /* The keyspace for this DB */
    /* DB的key空间 */
    dict *dict;   
    /* Timeout of keys with a timeout set */
    /* key的超市时间集合 */              
    dict *expires;              
    /* Keys with clients waiting for data (BLPOP) */
    /* 等待数据的key（BLPOP） */
    dict *blockingkeys;    
    /* Keys with clients waiting for VM I/O */
    /* 等待VM I/O的客户端 */
    dict *io_keys;             
    int id;
} redisDb;

/* Client MULTI/EXEC state */
/* multi命令 */
typedef struct multiCmd {
    robj **argv;
    int argc;
    struct redisCommand *cmd;
} multiCmd;
/* multi状态 */
typedef struct multiState {
    multiCmd *commands;     /* Array of MULTI commands */
    int count;              /* Total number of MULTI commands */
} multiState;

/* With multiplexing we need to take per-clinet state.
 * Clients are taken in a liked list. */
 /* 因为使用多路复用，我们需要保存每个客户端的状态，客户端被保存在一个类list中 */
typedef struct redisClient {
    int fd;
    redisDb *db;
    int dictid;
    sds querybuf;
    robj **argv, **mbargv;
    int argc, mbargc;
    /* bulk read len. -1 if not in bulk read mode */
    /* 块读取长度，如果这个值是-1，那么就不是块读取模式 */
    int bulklen;            
    /* multi bulk command format active */
    /* 事务 */
    int multibulk;          
    list *reply;
    int sentlen;
    /* 最后一次的操作时间 */
    time_t lastinteraction; /* time of the last interaction, used for timeout */
    /* REDIS_SLAVE | REDIS_MONITOR | REDIS_MULTI ... */
    int flags;              
    /* slave selected db, if this client is a slave */
    /* 从库选择的库 */
    int slaveseldb;         
    /* when requirepass is non-NULL */
    /* 是否授权 */
    int authenticated;      
    /* replication state if this is a slave */
    /* slave的复制状态 */
    int replstate;         
     /* replication DB file descriptor */
     /* 复制DB文件描述符 */ 
    int repldbfd;          
    /* replication DB file offset */
    /* 复制DB文件offset */
    long repldboff;         
     /* replication DB file size */
     /* 复制DB文件size */
    off_t repldbsize; 
    /* 事务执行状态 */     
    multiState mstate;      /* MULTI/EXEC state */
    /* The key we are waiting to terminate a blocking
     * operation such as BLPOP. Otherwise NULL. */
     /* 等待终结阻塞操作的key，例如BLPOP*/
    robj **blockingkeys;    
    /* Number of blocking keys */
    /* 锁住的key的个数 */
    int blockingkeysnum;   
    /* Blocking operation timeout. If UNIX current time
     * is >= blockingto then the operation timed out. */
    /* 如果当前时间大于blockingto，就算做超时 */
    time_t blockingto;      
    /* Keys this client is waiting to be loaded from the
     * swap file in order to continue. */
    /* 交换文件中等待加载的key */
    list *io_keys;          
} redisClient;

/* 存储参数 */
struct saveparam {
    time_t seconds;
    int changes;
};

/* Global server state structure */
/* 全局Server状态结构 */
struct redisServer {
    int port;
    int fd;
    redisDb *db;
    /* Poll used for object sharing */
    /* 轮询用于对象共享 */
    dict *sharingpool;    
    /* 共享对象池大小 */      
    unsigned int sharingpoolsize;
     /* changes to DB from the last save */
     /* 最后一次DB更改的时间 */
    long long dirty;        
    /* 客户端list */   
    list *clients;
    /* slave和monitor */
    list *slaves, *monitors;
    /* neterr */
    char neterr[ANET_ERR_LEN];
    /* 多路复用事件监听循环 */
    aeEventLoop *el;
    /* number of times the cron function run */
    /* 计划任务运行个数 */
    int cronloops;     
     /* A list of freed objects to avoid malloc() */
    /* 避免malloc的列表 */         
    list *objfreelist;         
     /* Unix time of last save succeeede */
     /* 上一次存储成功的Unix时间 */
    time_t lastsave;           
    /* Fields used only for stats */
    /* 用来标示状态的域 */

    /* server start time */
    /* 服务器启动时间 */
    time_t stat_starttime;         
    /* number of processed commands */
    /* 处理命令个数 */
    long long stat_numcommands;   
     /* number of connections received */
     /* 接收连接数 */ 
    long long stat_numconnections;
    /* Configuration */
    /* 配置 */
    int verbosity;
    int glueoutputbuf;
    int maxidletime; /* 最大空闲时间 */
    int dbnum;  /* 数据库个数 */
    int daemonize;  /* 守护进程 */
    int appendonly; 
    int appendfsync;  
    time_t lastfsync;
    int appendfd;
    int appendseldb;
    char *pidfile;  /* pid文件 */
    pid_t bgsavechildpid;   /* bgsave子pid */
    pid_t bgrewritechildpid;/* bgwrite子pid */
    /* buffer taken by parent during oppend only rewrite */
    /* 父级在oppend期间采用的缓冲区仅重写 */
    sds bgrewritebuf; 
    struct saveparam *saveparams;
    int saveparamslen;
    /* 日志文件 */
    char *logfile;
    /* 绑定地址 */
    char *bindaddr;
    /* 数据库文件名 */
    char *dbfilename;
    char *appendfilename;
    char *requirepass;
    int shareobjects;
    int rdbcompression;
    /* Replication related */
    /* 主从相关 */
    /* 是否是slave */
    int isslave;
    /* master授权字段 */
    char *masterauth;
    /* master的host */
    char *masterhost;
    /* master的端口 */
    int masterport;
    /* client that is master for this slave */
    /* 对于slave来说，这个client是master */
    redisClient *master;    
    /* 复制状态 */
    int replstate;
    /* 最大client个数 */
    unsigned int maxclients;
    /* 最大内存 */
    unsigned long long maxmemory;
    unsigned int blpop_blocked_clients;
    unsigned int vm_blocked_clients;
    /* Sort parameters - qsort_r() is only available under BSD so we
     * have to take this state global, in order to pass it to sortCompare() */
    int sort_desc;
    int sort_alpha;
    int sort_bypattern;
    /* Virtual memory configuration */
    /* 虚拟内存配置 */
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

typedef void redisCommandProc(redisClient *c);
/* Redis命令 结构 */
struct redisCommand {
    /* 命令名 */
    char *name;
    /* 处理函数 */
    redisCommandProc *proc;
    /* 元数 */
    int arity;
    /* 标志位 */
    int flags;
    /* Use a function to determine which keys need to be loaded
     * in the background prior to executing this command. Takes precedence
     * over vm_firstkey and others, ignored when NULL */
    redisCommandProc *vm_preload_proc;
    /* What keys should be loaded in background when calling this command? */
    int vm_firstkey; /* The first argument that's a key (0 = no keys) */
    int vm_lastkey;  /* THe last argument that's a key */
    int vm_keystep;  /* The step between first and last key */
};

struct redisFunctionSym {
    char *name;
    unsigned long pointer;
};

typedef struct _redisSortObject {
    robj *obj;
    union {
        double score;
        robj *cmpobj;
    } u;
} redisSortObject;

typedef struct _redisSortOperation {
    int type;
    robj *pattern;
} redisSortOperation;

/* ZSETs use a specialized version of Skiplists */
/* ZSET 采用特殊的跳表实现 */
typedef struct zskiplistNode {
    /* 指向前面的指针列表 */
    struct zskiplistNode **forward;
    /* 自相后面节点的指针 */
    struct zskiplistNode *backward;
    /* 层数 */
    unsigned int *span;
    /* 分数 */
    double score;
    /*存储内容*/
    robj *obj;
} zskiplistNode;

typedef struct zskiplist {
    /* 头尾节点 */
    struct zskiplistNode *header, *tail;
    /* 长度 */
    unsigned long length;
    /* 层级数 */
    int level;
} zskiplist;

/* zset */
typedef struct zset {
    dict *dict;
    zskiplist *zsl;
} zset;

/* Our shared "common" objects */
/* 共享的common对象 */
struct sharedObjectsStruct {
    robj *crlf, *ok, *err, *emptybulk, *czero, *cone, *pong, *space,
    *colon, *nullbulk, *nullmultibulk, *queued,
    *emptymultibulk, *wrongtypeerr, *nokeyerr, *syntaxerr, *sameobjecterr,
    *outofrangeerr, *plus,
    *select0, *select1, *select2, *select3, *select4,
    *select5, *select6, *select7, *select8, *select9;
} shared;

/* Global vars that are actally used as constants. The following double
 * values are used for double on-disk serialization, and are initialized
 * at runtime to avoid strange compiler optimizations. */

static double R_Zero, R_PosInf, R_NegInf, R_Nan;

/* VM threaded I/O request message */
/* 虚拟内存县城I/O请求消息 */
#define REDIS_IOJOB_LOAD 0          /* Load from disk to memory */
#define REDIS_IOJOB_PREPARE_SWAP 1  /* Compute needed pages */
#define REDIS_IOJOB_DO_SWAP 2       /* Swap from memory to disk */
typedef struct iojob {
    int type;   /* Request type, REDIS_IOJOB_* */
    redisDb *db;/* Redis database */
    robj *key;  /* This I/O request is about swapping this key */
    robj *val;  /* the value to swap for REDIS_IOREQ_*_SWAP, otherwise this
                 * field is populated by the I/O thread for REDIS_IOREQ_LOAD. */
    off_t page; /* Swap page where to read/write the object */
    off_t pages; /* Swap pages needed to safe object. PREPARE_SWAP return val */
    int canceled; /* True if this command was canceled by blocking side of VM */
    pthread_t thread; /* ID of the thread processing this entry */
} iojob;

```
```c
/* Glob-style pattern matching. */
/* Glob类型的模版匹配 */
int stringmatchlen(const char *pattern, int patternLen,
        const char *string, int stringLen, int nocase)
{

    while(patternLen) {
        switch(pattern[0]) {
        case '*':
            while (pattern[1] == '*') {
                pattern++;
                patternLen--;
            }
            if (patternLen == 1)
                return 1; /* match */
            while(stringLen) {
                if (stringmatchlen(pattern+1, patternLen-1,
                            string, stringLen, nocase))
                    return 1; /* match */
                string++;
                stringLen--;
            }
            return 0; /* no match */
            break;
        case '?':
            if (stringLen == 0)
                return 0; /* no match */
            string++;
            stringLen--;
            break;
        case '[':
        {
            int not, match;

            pattern++;
            patternLen--;
            not = pattern[0] == '^';
            if (not) {
                pattern++;
                patternLen--;
            }
            match = 0;
            while(1) {
                if (pattern[0] == '\\') {
                    pattern++;
                    patternLen--;
                    if (pattern[0] == string[0])
                        match = 1;
                } else if (pattern[0] == ']') {
                    break;
                } else if (patternLen == 0) {
                    pattern--;
                    patternLen++;
                    break;
                } else if (pattern[1] == '-' && patternLen >= 3) {
                    int start = pattern[0];
                    int end = pattern[2];
                    int c = string[0];
                    if (start > end) {
                        int t = start;
                        start = end;
                        end = t;
                    }
                    if (nocase) {
                        start = tolower(start);
                        end = tolower(end);
                        c = tolower(c);
                    }
                    pattern += 2;
                    patternLen -= 2;
                    if (c >= start && c <= end)
                        match = 1;
                } else {
                    if (!nocase) {
                        if (pattern[0] == string[0])
                            match = 1;
                    } else {
                        if (tolower((int)pattern[0]) == tolower((int)string[0]))
                            match = 1;
                    }
                }
                pattern++;
                patternLen--;
            }
            if (not)
                match = !match;
            if (!match)
                return 0; /* no match */
            string++;
            stringLen--;
            break;
        }
        case '\\':
            if (patternLen >= 2) {
                pattern++;
                patternLen--;
            }
            /* fall through */
        default:
            if (!nocase) {
                if (pattern[0] != string[0])
                    return 0; /* no match */
            } else {
                if (tolower((int)pattern[0]) != tolower((int)string[0]))
                    return 0; /* no match */
            }
            string++;
            stringLen--;
            break;
        }
        pattern++;
        patternLen--;
        if (stringLen == 0) {
            while(*pattern == '*') {
                pattern++;
                patternLen--;
            }
            break;
        }
    }
    if (patternLen == 0 && stringLen == 0)
        return 1;
    return 0;
}

/*redisLog*/
/* redis log处理 */
static void redisLog(int level, const char *fmt, ...) {
    va_list ap;
    FILE *fp;

    fp = (server.logfile == NULL) ? stdout : fopen(server.logfile,"a");
    if (!fp) return;

    va_start(ap, fmt);
    if (level >= server.verbosity) {
        char *c = ".-*#";
        char buf[64];
        time_t now;

        now = time(NULL);
        strftime(buf,64,"%d %b %H:%M:%S",localtime(&now));
        fprintf(fp,"[%d] %s %c ",(int)getpid(),buf,c[level]);
        vfprintf(fp, fmt, ap);
        fprintf(fp,"\n");
        fflush(fp);
    }
    va_end(ap);

    if (server.logfile) fclose(fp);
}

```
```c
/*====================== Hash table type implementation  ==================== */

/* This is an hash table type that uses the SDS dynamic strings libary as
 * keys and radis objects as values (objects can hold SDS strings,
 * lists, sets). */
 /* 使用SDS动态String作为key，使用redis object作为value的哈希表 */

static void dictVanillaFree(void *privdata, void *val)
{
    DICT_NOTUSED(privdata);
    zfree(val);
}

static void dictListDestructor(void *privdata, void *val)
{
    DICT_NOTUSED(privdata);
    listRelease((list*)val);
}

static int sdsDictKeyCompare(void *privdata, const void *key1,
        const void *key2)
{
    int l1,l2;
    DICT_NOTUSED(privdata);

    l1 = sdslen((sds)key1);
    l2 = sdslen((sds)key2);
    if (l1 != l2) return 0;
    return memcmp(key1, key2, l1) == 0;
}

/* Redis对象析构器 */
static void dictRedisObjectDestructor(void *privdata, void *val)
{
    DICT_NOTUSED(privdata);

    if (val == NULL) return; /* Values of swapped out keys as set to NULL */
    /* 引用计数-1 */
    decrRefCount(val);
}

/* Redis 的 Key比较，由于Key都是SDS，因此采用SDS比较方法 */
static int dictObjKeyCompare(void *privdata, const void *key1,
        const void *key2)
{
    const robj *o1 = key1, *o2 = key2;
    return sdsDictKeyCompare(privdata,o1->ptr,o2->ptr);
}

/* 哈希表key哈希 */
static unsigned int dictObjHash(const void *key) {
    const robj *o = key;
    return dictGenHashFunction(o->ptr, sdslen((sds)o->ptr));
}

/* 哈希表编码key比较 */
static int dictEncObjKeyCompare(void *privdata, const void *key1,
        const void *key2)
{
    robj *o1 = (robj*) key1, *o2 = (robj*) key2;
    int cmp;

    if (o1->encoding == REDIS_ENCODING_INT &&
        o2->encoding == REDIS_ENCODING_INT &&
        o1->ptr == o2->ptr) return 1;
    /* 获取一个编码对象的解码版本 */
    o1 = getDecodedObject(o1);
    o2 = getDecodedObject(o2);
    cmp = sdsDictKeyCompare(privdata,o1->ptr,o2->ptr);
    decrRefCount(o1);
    decrRefCount(o2);
    return cmp;
}

static unsigned int dictEncObjHash(const void *key) {
    robj *o = (robj*) key;

    if (o->encoding == REDIS_ENCODING_RAW) {
        return dictGenHashFunction(o->ptr, sdslen((sds)o->ptr));
    } else {
        if (o->encoding == REDIS_ENCODING_INT) {
            char buf[32];
            int len;

            len = snprintf(buf,32,"%ld",(long)o->ptr);
            return dictGenHashFunction((unsigned char*)buf, len);
        } else {
            unsigned int hash;

            o = getDecodedObject(o);
            hash = dictGenHashFunction(o->ptr, sdslen((sds)o->ptr));
            decrRefCount(o);
            return hash;
        }
    }
}

/* Sets type and expires */
/* Set的类型 */
static dictType setDictType = {
    dictEncObjHash,            /* hash function */
    NULL,                      /* key dup */
    NULL,                      /* val dup */
    dictEncObjKeyCompare,      /* key compare */
    dictRedisObjectDestructor, /* key destructor */
    NULL                       /* val destructor */
};

/* Sorted sets hash (note: a skiplist is used in addition to the hash table) */
/* zset类型 */
static dictType zsetDictType = {
    dictEncObjHash,            /* hash function */
    NULL,                      /* key dup */
    NULL,                      /* val dup */
    dictEncObjKeyCompare,      /* key compare */
    dictRedisObjectDestructor, /* key destructor */
    dictVanillaFree            /* val destructor of malloc(sizeof(double)) */
};

/* Db->dict */
/* 数据库->哈希表 */
static dictType dbDictType = {
    dictObjHash,                /* hash function */
    NULL,                       /* key dup */
    NULL,                       /* val dup */
    dictObjKeyCompare,          /* key compare */
    dictRedisObjectDestructor,  /* key destructor */
    dictRedisObjectDestructor   /* val destructor */
};

/* Db->expires */
static dictType keyptrDictType = {
    dictObjHash,               /* hash function */
    NULL,                      /* key dup */
    NULL,                      /* val dup */
    dictObjKeyCompare,         /* key compare */
    dictRedisObjectDestructor, /* key destructor */
    NULL                       /* val destructor */
};

/* Hash type hash table (note that small hashes are represented with zimpaps) */
/* 哈希类型的哈希表（注意小的哈希表使用zimpaps表示） */
static dictType hashDictType = {
    dictEncObjHash,             /* hash function */
    NULL,                       /* key dup */
    NULL,                       /* val dup */
    dictEncObjKeyCompare,       /* key compare */
    dictRedisObjectDestructor,  /* key destructor */
    dictRedisObjectDestructor   /* val destructor */
};

/* Keylist hash table type has unencoded redis objects as keys and
 * lists as values. It's used for blocking operations (BLPOP) and to
 * map swapped keys to a list of clients waiting for this keys to be loaded. */
static dictType keylistDictType = {
    dictObjHash,                /* hash function */
    NULL,                       /* key dup */
    NULL,                       /* val dup */
    dictObjKeyCompare,          /* key compare */
    dictRedisObjectDestructor,  /* key destructor */
    dictListDestructor          /* val destructor */
};
```

```c
/* ========================= Random utility functions ======================= */

/* Redis generally does not try to recover from out of memory conditions
 * when allocating objects or strings, it is not clear if it will be possible
 * to report this condition to the client since the networking layer itself
 * is based on heap allocation for send buffers, so we simply abort.
 * At least the code will be simpler to read... */
 /* 当分配对象或者字符串空间时，如果出现内存空间不足的情况，不会尝试进行恢复，因为网络层的发送缓存是基于堆分配的，因此我们放弃了报告，至少代码将会更易读。 */
static void oom(const char *msg) {
    redisLog(REDIS_WARNING, "%s: Out of memory\n",msg);
    sleep(1);
    abort();
}

```

接下来查看Redis 的启动函数，即main函数：


```c
int main(int argc, char **argv) {
    time_t start;
    /* 初始化服务配置 */
    initServerConfig();
    if (argc == 2) {
        /* 如果指定了配置，则重置配置，将指定配置加载到其中 */
        resetServerSaveParams();
        loadServerConfig(argv[1]);
    } else if (argc > 2) {
        /* 参数过多 */
        fprintf(stderr,"Usage: ./redis-server [/path/to/redis.conf]\n");
        exit(1);
    } else {
        redisLog(REDIS_WARNING,"Warning: no config file specified, using the default config. In order to specify a config file use 'redis-server /path/to/redis.conf'");
    }
    /*设置守护进程方式运行 */
    if (server.daemonize) daemonize();
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
    /* 服务器启动时间 */
    start = time(NULL);
    if (server.appendonly) {
        if (loadAppendOnlyFile(server.appendfilename) == REDIS_OK)
            redisLog(REDIS_NOTICE,"DB loaded from append only file: %ld seconds",time(NULL)-start);
    } else {
        /* 加载数据 */
        if (rdbLoad(server.dbfilename) == REDIS_OK)
            redisLog(REDIS_NOTICE,"DB loaded from disk: %ld seconds",time(NULL)-start);
    }
    redisLog(REDIS_NOTICE,"The server is now ready to accept connections on port %d", server.port);
    /* 启动主程序 */
    aeSetBeforeSleepProc(server.el,beforeSleep);
    aeMain(server.el);
    aeDeleteEventLoop(server.el);
    return 0;
}

```

Redis的服务端启动过程如下：

1.  初始化服务配置
2.  如果配置了配置文件，则加载配置文件配置
3.  判断守护进程方式配置
4.  初始化Server基本配置
5.  如果是linux系统，则检测内存分配策略
6.  判断appendonly参数，并进行检测
7.  开启多路复用处理问题


 初始化服务配置。由于Redis的配置并不是每一个都可以在配置文件中读取的，因此，需要初始化服务配置，防止部分配置未被初始化。

```c
    /* 初始化Redis数据结构基本状态 */
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
        server.saveparams = NULL;
        /*Log存储文件 */
        server.logfile = NULL; /* NULL = log on standard output */
        /*绑定地址 */
        server.bindaddr = NULL;
        server.glueoutputbuf = 1;
        server.daemonize = 0;
        server.appendonly = 0;
        server.appendfsync = APPENDFSYNC_ALWAYS;
        /*最后一次的同步时间 */
        server.lastfsync = time(NULL);
        server.appendfd = -1;
        server.appendseldb = -1; /* Make sure the first time will not match */
        /*pid文件 */
        server.pidfile = "/var/run/redis.pid";
        /*持久化RDB文件 */
        server.dbfilename = "dump.rdb";
        server.appendfilename = "appendonly.aof";
        server.requirepass = NULL;
        server.shareobjects = 0;
        server.rdbcompression = 1;
        server.sharingpoolsize = 1024;
        server.maxclients = 0;
        server.blpop_blocked_clients = 0;
        server.maxmemory = 0;
        server.vm_enabled = 0;
        server.vm_swap_file = zstrdup("/tmp/redis-%p.vm");
        server.vm_page_size = 256;          /* 256 bytes per page */
        server.vm_pages = 1024*1024*100;    /* 104 millions of pages */
        server.vm_max_memory = 1024LL*1024*1024*1; /* 1 GB of RAM */
        server.vm_max_threads = 4;
        server.vm_blocked_clients = 0;
        server.hash_max_zipmap_entries = REDIS_HASH_MAX_ZIPMAP_ENTRIES;
        server.hash_max_zipmap_value = REDIS_HASH_MAX_ZIPMAP_VALUE;
        /*重置服务器存储参数 */
        resetServerSaveParams();

        appendServerSaveParams(60*60,1);  /* save after 1 hour and 1 change */
        appendServerSaveParams(300,100);  /* save after 5 minutes and 100 changes */
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

重置saveparams记录：

```c
static void resetServerSaveParams() {
    /*重置服务器存储参数 */
    zfree(server.saveparams);
    server.saveparams = NULL;
    server.saveparamslen = 0;
}
```
加载配置文件配置：

```c
/* I agree, this is a very rudimental way to load a configuration...
   will improve later if the config gets more complex */
   /*加载服务端配置 */
static void loadServerConfig(char *filename) {
    FILE *fp;
    /*配置文件放入内存的区域 */
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
            redisLog(REDIS_WARNING,"Fatal error, can't open config file");
            exit(1);
        }
    }

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
            /*save参数表示如果XXX秒内XX个key发生变化则重写rdb文件 */
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

判断是否按照守护进程运行，如果是，则处理：

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

初始化服务端数据结构：

```c
static void initServer() {
    int j;

    signal(SIGHUP, SIG_IGN);
    signal(SIGPIPE, SIG_IGN);
    setupSigSegvAction();
    /*初始化devnull */
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