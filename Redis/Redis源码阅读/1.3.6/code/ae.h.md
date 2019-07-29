# ae.h

AE事件库的数据结构与接口定义，代码极其简短。

```c
/* A simple event-driven programming library. Originally I wrote this code
 * for the Jim's event-loop (Jim is a Tcl interpreter) but later translated
 * it in form of a library for easy reuse.
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

#ifndef __AE_H__
#define __AE_H__

#define AE_SETSIZE (1024*10)    /* Max number of fd supported */

/* 规定的返回码 */
#define AE_OK 0
#define AE_ERR -1

/* 规定的fd事件状态 */

#define AE_NONE 0
#define AE_READABLE 1
#define AE_WRITABLE 2

/* fd事件类型 */

#define AE_FILE_EVENTS 1
#define AE_TIME_EVENTS 2
#define AE_ALL_EVENTS (AE_FILE_EVENTS|AE_TIME_EVENTS)
#define AE_DONT_WAIT 4

#define AE_NOMORE -1

/* 宏 */
#define AE_NOTUSED(V) ((void) V)

struct aeEventLoop;

/* Types and data structures */
/* 文件事件处理逻辑 */
typedef void aeFileProc(struct aeEventLoop *eventLoop, int fd, void *clientData, int mask);
/*时间事件处理逻辑*/
typedef int aeTimeProc(struct aeEventLoop *eventLoop, long long id, void *clientData);
/* 事件终结时逻辑 */
typedef void aeEventFinalizerProc(struct aeEventLoop *eventLoop, void *clientData);
/* 睡眠前处理逻辑 */
typedef void aeBeforeSleepProc(struct aeEventLoop *eventLoop);

/* 文件事件数据结构 */
typedef struct aeFileEvent {
    int mask; /* one of AE_(READABLE|WRITABLE) */
    /* 读事件处理逻辑 */
    aeFileProc *rfileProc;
    /* 写事件处理逻辑 */
    aeFileProc *wfileProc;
    /* 处理内容 */
    void *clientData;
} aeFileEvent;

/* 定时器事件结构 （链表）*/
typedef struct aeTimeEvent {
    long long id; /* 定时器事件的唯一标示 */
    long when_sec; /* 秒 */
    long when_ms; /* 毫秒 */
    /* 定时器处理程序 */
    aeTimeProc *timeProc;
    /* 定时器终结时处理逻辑 */
    aeEventFinalizerProc *finalizerProc;
    /* 处理内容 */
    void *clientData;
    /* 下一个定时器事件 */
    struct aeTimeEvent *next;
} aeTimeEvent;

/* 表示即将触发的事件 */
typedef struct aeFiredEvent {
    int fd;
    int mask;
} aeFiredEvent;

/* 事件Container */
typedef struct aeEventLoop {
    int maxfd;
    long long timeEventNextId;
    aeFileEvent events[AE_SETSIZE]; /* Registered events */
    aeFiredEvent fired[AE_SETSIZE]; /* Fired events */
    aeTimeEvent *timeEventHead;
    int stop;
    void *apidata; /* This is used for polling API specific data */
    aeBeforeSleepProc *beforesleep;
} aeEventLoop;

/* Prototypes */
/* aeEventLoop管理接口定义 */
aeEventLoop *aeCreateEventLoop(void);
void aeDeleteEventLoop(aeEventLoop *eventLoop);
void aeStop(aeEventLoop *eventLoop);
int aeCreateFileEvent(aeEventLoop *eventLoop, int fd, int mask,
        aeFileProc *proc, void *clientData);
void aeDeleteFileEvent(aeEventLoop *eventLoop, int fd, int mask);
long long aeCreateTimeEvent(aeEventLoop *eventLoop, long long milliseconds,
        aeTimeProc *proc, void *clientData,
        aeEventFinalizerProc *finalizerProc);
int aeDeleteTimeEvent(aeEventLoop *eventLoop, long long id);
int aeProcessEvents(aeEventLoop *eventLoop, int flags);
int aeWait(int fd, int mask, long long milliseconds);
void aeMain(aeEventLoop *eventLoop);
char *aeGetApiName(void);
void aeSetBeforeSleepProc(aeEventLoop *eventLoop, aeBeforeSleepProc *beforesleep);

#endif

```