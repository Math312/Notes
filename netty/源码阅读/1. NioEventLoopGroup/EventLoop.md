# EventLoop

netty采用了注明的Reactor模式，简单的说，在服务端启动的时候，启动了两个线程组，我们分别将其称为`Acceptor Group`和`Worker Group`，他们的职责正如他们的名字一样：

1. Acceptor Group: 负责接收客户端TCP连接以及将链路状态变更通知到Worker Group
2. Worker Group: 负责进行业务操作

而这两个线程组里面的线程就是我们即将要介绍的`EventLoop`。这里我们通过`NioEventLoop`对`EventLoop`进行分析。

## NioEventLoop

首先查看`NioEventLoop`的类图：

![NioEventLoop](./NioEventLoop.png)

这里我们可以看到几个非常熟悉的类和接口：`Executor`、`ExecutorService`、`AbstractExecutorService`、`ScheduledExecutorService`、`AutoCloseable`这几个接口都是JDK本身的接口，这里不再赘述，我们接下来主要讨论其余的接口与实现类。首先讨论途中的接口，这样方便明确每个实现类提供的功能。

### 1. EventExecutorGroup和EventExecutor

刚才已经了解到服务端在启动时会创建两个线程组，业务操作由两个线程组（`EventExecutorGroup`）执行，但是实际上，线程组中真正执行业务操作的是各个线程（`EventExecutor`）。因此`EventExecutorGroup`实际上就是`EventExecutor`的代理，因此`EventExecutor`接口继承了`EventExecutorGroup`。

考察`EventExecutorGroup`源码，我们可以将该接口中要求实现的方法分为两类：

1. EventExecutorGroup生命周期相关的方法

    1. `boolean isShuttingDown()`：判断线程组是否被关闭
    2. `Future<?> shutdownGracefully()`：使用默认参数优雅的关闭线程组
    3. `Future<?> shutdownGracefully(long quietPeriod, long timeout, TimeUnit unit)`：明确指定参数优雅的关闭线程组

2. 管理线程组中线程的方法

    1. `EventExecutor next()`：获取线程组管理的一个线程
    2. `<E extends EventExecutor> Set<E> children()`：获取线程组管理的所有线程（不可修改）
    3. `Future<?> submit(...)`：提交任务到线程组
    4. `ScheduledFuture<?> scheduleXXX(XXX)`：提交定时任务到线程组

由于`EventExecutorGroup`是`EventExecutor`的代理，因此`EventExecutorGroup`接口`EventExecutor`必须实现，除此之外，`EventExecutor`还实现了一些本身的接口，这些接口可以分为两类：

1. 属性接口：
   1. `EventExecutorGroup parent()`：返回管理当前`EventExecutor`的线程组
   2. `boolean inEventLoop({Thread}?)`：判断指定线程是否被`EventExecutor`管理，如果Thread参数不传，则默认为当前线程
   3. `EventExecutor unwrap()`：将当前`EventExecutor`解包装
2. 结果返回接口：
    由于`EventExecutor`执行业务操作都是异步执行的，因此，所有的结果返回都是通过回调（即`Promise`）的方式，这部分接口就是通过`Promise`来指定返回结果的。

    1. `<V> Promise<V> newPromise()`
    2. `<V> ProgressivePromise<V> newProgressivePromise()`
    3. `<V> Future<V> newSucceededFuture(V result)`
    4. `<V> Future<V> newFailedFuture(Throwable cause)`

然而到现在为止，我们分析的仅仅是`EventExecutor`，而不是`EventLoop`，那么这两者到底有什么区别呢？我们接下来进行分析。

### 2. EventLoopGroup和EventLoop

`EventLoop`和`EventExecutor`从名字就可以看出，一个是循环的多次一直操作，一个只是单次操作。事实上对于Nio的开发模式来说`EventLoop`使用的更多，因为，我们更偏向于只要有一个事件发生就执行一系列操作，而不是我们需要自己监听某个集合是否有事件。除此之外，`EventLoop`将`EventExecutor`与`Channel`耦合起来了。但是`EventExecutor`为`EventLoop`提供基础，可能在不就的将来，就会有使用`EventExecutor`实现的`EventCircle`什么的呢。

与`EventExecutorGroup和EventExecutor`的关系一样，`EventLoopGroup`是`EventLoop`的管理者，同时也是`EventLoop`的代理。所以我们还是首先分析`EventLoopGroup`接口。`EventLoopGroup`仅仅提供了一个新的注册接口，还有一个旧接口的变体。

由于Nio大量使用了`Channel`和`基于事件监听的方式`，因此`EventLoop`需要将两者与`EventExecutor`整合起来，这就需要一个`register(XXX)`方法将`Channel`注册到`EventLoop`中进行监听，所以就有了如下接口：

1. ChannelFuture register(Channel channel);
2. ChannelFuture register(Channel channel, ChannelPromise promise);

既然`EventLoopGroup`管理的都是`EventLoop`了，所以获取`EventExecutor`的方法肯定要变成获取`EventLoop`，所以`next()`方法的返回值就被修改了：

1. EventLoop next();

之前已经说了`EventLoopGroup`是`EventLoop`的代理，`EventLoop`有很多自己实现的方法，事实上，`EventLoop`仅仅要求多实现如下方法：

```java
ChannelHandlerInvoker asInvoker();
```

无论是`EventLoop`还是`EventExecutor`，都要进行业务逻辑的处理，而`ChannelHandlerInvoker`就是`EventLoop`进行业务处理的具体执行者。那么为什么`EventExecutor`没有对应的业务执行者呢？因为只有`Channel`才能明确监听哪些事件，`EventExecutor`是无法控制到底要监听多少事件的，所以`EventExecutor`没有对应的执行者对象。

至此，`EventLoop`相关的所有接口都分析完了，但是仅仅了解接口定义是不够的，我们需要进一步了解接口的具体实现。接下来让我们来考察上面四个接口的具体实现。

### 3. AbstractEventExecutor

`AbstractEventExecutor`是`EventExecutor`接口的一个简单实现，事实上`AbstractEventExecutor`几乎所有的管理线程组中线程的方法都是`AbstractExecutorService`实现的，自己仅仅实现了一些模板方法罢了，例如：

```java
    public Future<?> shutdownGracefully() {
        return shutdownGracefully(DEFAULT_SHUTDOWN_QUIET_PERIOD, DEFAULT_SHUTDOWN_TIMEOUT, TimeUnit.SECONDS);
    }
```

该类实现了`shutdownGracefully()`方法，但是对于具体实现`uture<?> shutdownGracefully(long quietPeriod, long timeout, TimeUnit unit)`则交给了子类。

不过这里实现了所有的结果返回接口：

```java
    public <V> Promise<V> newPromise() {
        return new DefaultPromise<V>(this);
    }

    @Override
    public <V> ProgressivePromise<V> newProgressivePromise() {
        return new DefaultProgressivePromise<V>(this);
    }

    @Override
    public <V> Future<V> newSucceededFuture(V result) {
        return new SucceededFuture<V>(this, result);
    }

    @Override
    public <V> Future<V> newFailedFuture(Throwable cause) {
        return new FailedFuture<V>(this, cause);
    }
```

虽然`AbstractEventExecutor`像一个混子接口，但是这个接口实际上做了很多事。我们知道，`AbstractEventExecutor`的所有管理线程的业务都交给了`AbstractExecutorService`，那为什么还要用`AbstractEventExecutor`呢？事实上，该类最重要的就是最后这一部分结果返回接口。

因为基于事件监听的编码方式中业务都是异步完成的，无法像常规的编码那样同步处理，所以结果返回就是回调，因此该类的主要作用是为全新的编码方式提供基础。

### 4. AbstractScheduledEventExecutor

正如类名所示，`AbstractScheduledEventExecutor`为`EventExecutor`提供了定时任务支持。我们都知道，`ExecutorService`虽然好，但是有个非常不方便的地方，就是没办法执行定时任务。Jdk的定时任务都需要使用其他类执行。所以`AbstractScheduledEventExecutor`实现了自己的定时任务执行器。

事实上`AbstractScheduledEventExecutor`仅仅实现了一个优先级队列，用于存储需要执行的定时任务，真正的监听与执行操作都是子类完成的。`AbstractScheduledEventExecutor`维护的就是如下对象：

```java
PriorityQueue<ScheduledFutureTask<?>> scheduledTaskQueue;
```

获取`scheduledTaskQueue`的方法是如下方法：

```java
    PriorityQueue<ScheduledFutureTask<?>> scheduledTaskQueue() {
        if (scheduledTaskQueue == null) {
            scheduledTaskQueue = new DefaultPriorityQueue<ScheduledFutureTask<?>>(
                    SCHEDULED_FUTURE_TASK_COMPARATOR,
                    // Use same initial capacity as java.util.PriorityQueue
                    11);
        }
        return scheduledTaskQueue;
    }
```

剩下的仅仅是维护队列的一系列方法。至于具体是如何执行定时任务的，我们会在讨论到`NioEventLoop`时讨论到。

### 5. SingleThreadEventExecutor

之前我们已经知道，`AbstractEventExecutor`默认实现的`submit(XXX)`这些操作都是交给`AbstractExecutorService`实现的。这样其实在普通的业务操作执行时，通常还是同步操作，这些业务操作并没有被自己管理起来基于事件异步执行。因此就需要使用`SingleThreadEventExecutor`进行封装。

所以，`SingleThreadEventExecutor`存在的目的就是让`执行的业务操作变成基于事件的形式，并且异步执行，但是实际上使用的是同一线程`。这里我们首先考察一下`SingleThreadEventExecutor`中声明的属性：

1. `SingleThreadEventExecutor`状态：

    ```java
    private volatile int state = ST_NOT_STARTED;
    private static final int ST_NOT_STARTED = 1;
    private static final int ST_STARTED = 2;
    private static final int ST_SHUTTING_DOWN = 3;
    private static final int ST_SHUTDOWN = 4;
    private static final int ST_TERMINATED = 5;
    private static final AtomicIntegerFieldUpdater<SingleThreadEventExecutor> STATE_UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(SingleThreadEventExecutor.class, "state");
    ```

    要达到上面的目的，事实上我们相当于重新管理了一下线程，因此要给线程一系列状态，方便进行管理。`SingleThreadEventExecutor`声明了五个状态，为了保证状态更新的原子性，使用了`AtomicIntegerFieldUpdater`。

2. 保存执行线程的属性

    ```java
    private volatile ThreadProperties threadProperties;
    private static final AtomicReferenceFieldUpdater<SingleThreadEventExecutor, ThreadProperties> PROPERTIES_UPDATER =
                AtomicReferenceFieldUpdater.newUpdater(
                        SingleThreadEventExecutor.class, ThreadProperties.class, "threadProperties");
    private static final class DefaultThreadProperties implements ThreadProperties {
        private final Thread t;

        ...
    }
    ```

    由于`SingleThreadEventExecutor`使用单个线程处理多个业务事件，因此需要保存被使用的线程的状态。而保存该状态的就是`DefaultThreadProperties`类型的对象。需要注意，实际上`DefaultThreadProperties`中仅仅存了`Thread`对象。为了保证原子性更新同样使用了`AtomicReferenceFieldUpdater`。

    不过需要注意，事实上在`SingleThreadEventExecutor`是没有使用到这个属性的。

3. 存储执行业务操作的队列

    ```java
    private final Queue<Runnable> taskQueue;
    ```

    通过上面的分析，我们已经知道了定时任务存储在`scheduledTaskQueue`中，那么非定时任务的业务操作呢？实际上就放在了`taskQueue`中。默认情况下，`taskQueue`是一个`LinkedBlockingQueue`：

    ```java
    taskQueue = newTaskQueue(this.maxPendingTasks);

    protected Queue<Runnable> newTaskQueue(int maxPendingTasks) {
        return new LinkedBlockingQueue<Runnable>(maxPendingTasks);
    }
    ```

    阻塞队列默认长度是16，长度可以在创建`SingleThreadEventExecutor`时自己调节。

4. 真正执行业务操作的线程池

    ```java
    private final Executor executor;
    ```

    真正执行业务操作的线程池。由于`SingleThreadEventExecutor`的工作线程可能是从一个线程池中取出的一个，因此这里存放的就是提供线程的线程池。

5. 执行线程是否中断

    ```java
    private volatile boolean interrupted;
    ```

6. 线程锁

    ```java
    private final Semaphore threadLock = new Semaphore(0);
    ```

    明明是单线程，为什么需要线程锁呢？`ExecutorService`接口要求实现`awaitTermination(long timeout, TimeUnit unit)`，要求规定时间内，无法新添加任务，只有已经添加了的任务执行。为了做到这一点netty使用了信号量，注意信号量初始值为0。就意味着调用了就锁死。但是注意，只有调用该方法的线程会锁死，不会影响到其他线程。

7. shutdown钩子

    当调用了`shutdown()`停止EventExecutor时，当所有已经添加的任务处理完毕之后会调用如下变量存储的钩子函数:

    ```java
    private final Set<Runnable> shutdownHooks = new LinkedHashSet<Runnable>();
    ```

    SingleThreadEventExecutor提供了三个管理该属性的函数：

    ```java
    public void addShutdownHook(final Runnable task) {}
    public void removeShutdownHook(final Runnable task) {}
    private boolean runShutdownHooks() {}
    ```

8. 终止钩子

    当`EventExecutor`完全终止时返回的Future:

    ```java
    private final Promise<?> terminationFuture = new DefaultPromise<Void>(GlobalEventExecutor.INSTANCE);
    ```

9. 添加任务是否唤醒EventExecutor的标志

    ```java
    private final boolean addTaskWakesUp;
    ```

    如果该标志为true，则每次添加任务都会主动唤醒线程，否则不会。

了解了`SingleThreadEventExecutor`的属性，我们已经知道了，所有的新发生需要处理的业务操作都被存储在`taskQueue`中，定时任务则存储在`scheduledTaskQueue`中。这里我们首先分析如何将任务存储到`taskQueue`中的。考察诸多public 方法中的`execute(Runnable task)`，该方法用于将task交给`SingleThreadEventExecutor`执行。

```java
    public void execute(Runnable task) {
        if (task == null) {
            throw new NullPointerException("task");
        }

        boolean inEventLoop = inEventLoop();
        if (inEventLoop) {
            // 代码点1
            addTask(task);
        } else {
            // 代码点2
            startExecution();
            // 代码点3
            addTask(task);
            // 代码点4
            if (isShutdown() && removeTask(task)) {
                reject();
            }
        }
        // 代码点5
        if (!addTaskWakesUp && wakesUpForTask(task)) {
            wakeup(inEventLoop);
        }
    }
```

该方法主要做了三件事：

1. 首先向该`SingleThreadEventExecutor`添加任务的线程是否是EventLoop自身的线程，如果是，直接添加就好了，如果不是，需要进行三部分处理：
    1. startExecution():开启定时任务，定时清除定时任务队列中被取消的任务，启动`SingleThreadEventExecutor`
    2. addTask(task)：将新的task加入到任务队列中
       该函数向阻塞队列中添加了一条记录，不过除此之外，还判断了`SingleThreadEventExecutor`是否是shutdown的。如果`SingleThreadEventExecutor`已经被shutdown，那么会拒绝将新的任务添加到阻塞队列中。

        ```java
        protected void addTask(Runnable task) {
            if (task == null) {
                throw new NullPointerException("task");
            }
            if (isShutdown()) {
                reject();
            }
            taskQueue.add(task);
        }
        ```

    3. 检验是否有人关闭该`SingleThreadEventExecutor`，如果有，则删除新增加的任务，并使用拒绝策略。

2. 如果`SingleThreadEventExecutor`被shutdown了，那么唤醒该阻塞队列。

    ```java
    if (!addTaskWakesUp && wakesUpForTask(task)) {
        wakeup(inEventLoop);
    }
    ```

虽然该方法做的事情仅仅是将新的任务保存到`taskQueue`中，但是实际上在细节方面进行了很多类似于乐观锁的操作。

可以看到，每个task添加时至少进行了两次`shutdown`状态的检查，如果添加新线程的操作并非`SingleThreadEventExecutor`的工作线程所做的操作，则要判断3次。实际上netty就是希望不使用锁，以提高效率。简单实现自然是同步，对这段代码加一个悲观锁就好了，下面我们对比一下悲观锁和当前这种方式的区别。

使用悲观锁，自然就是，添加时加锁，shutdown时也加锁。两个操作之间同步。

那么考虑使用当前方式处理。考虑代码点1、4，考虑如下情况：

1. SingleThreadEventExecutor在执行execute(task)之前就shutdown了。不可能出现这种情况，因为如果出现这种情况，inEventLoop()方法必然返回false，因为工作线程已经shutdown了
2. SingleThreadEventExecutor在执行完inEventLoop()到代码点1之间shudown了。此时`addTask(task)`会保证新任务不插入。
3. SingleThreadEventExecutor在执行完代码点1到代码点4之间shutdown了。此时由于新的任务已经添加到队列了，所以必须要执行完毕才能shutdown。但是有一种特殊情况，由于SingleThreadEventExecutor中的任务都是异步执行，因此可能执行者不断的从`SingleThreadEventExecutor`的阻塞队列中获取任务，但是此时`SingleThreadEventExecutor`已经shutdown了，但是`阻塞队列并不会在这种情况下通知执行者`，因此在重新启动之前不会有新的任务了，此时，执行者可能就一直阻塞了。如果`SingleThreadEventExecutor`支持`addTaskWakesUp`可以向阻塞队列中添加空任务唤醒执行者，并通知执行者不再监听，但是如果不支持那就一直锁死了。为了防止这种情况，这里主动调用一下`wakeup(XX)`。

考虑代码点2、3、4、5，这道流程处理的是当`SingleThreadEventExecutor`处于`shutdown状态或者添加任务到SingleThreadEventExecutor的线程是其余线程时的情况`。为了保证`SingleThreadEventExecutor`在插入时是start状态，首先调用代码点2，开启`SingleThreadEventExecutor`。

```java
    private void startExecution() {
        // 如果SingleThreadEventExecutor处于尚未开启的状态
        if (STATE_UPDATER.get(this) == ST_NOT_STARTED) {
            // 则将状态设置为开启（ST_STARTED）
            if (STATE_UPDATER.compareAndSet(this, ST_NOT_STARTED, ST_STARTED)) {
                // 然后添加定时任务PurgeTask
                // 每秒执行一次
                // purgeTask负责清除scheduledTaskQueue中已经被取消的任务
                schedule(new ScheduledFutureTask<Void>(
                        this, Executors.<Void>callable(new PurgeTask(), null),
                        ScheduledFutureTask.deadlineNanos(SCHEDULE_PURGE_INTERVAL), -SCHEDULE_PURGE_INTERVAL));
                // 执行主程序
                scheduleExecution();
            }
        }
    }

    protected final void scheduleExecution() {
        updateThread(null);
        executor.execute(asRunnable);
    }

    private void updateThread(Thread t) {
        // 将线程状态设置为当前线程
        THREAD_UPDATER.lazySet(this, t);
    }

    private final Runnable asRunnable = new Runnable() {
        @Override
        public void run() {
            // 将线程状态设置为当前线程
            updateThread(Thread.currentThread());

            // lastExecutionTime must be set on the first run
            // in order for shutdown to work correctly for the
            // rare case that the eventloop did not execute
            // a single task during its lifetime.
            if (firstRun) {
                firstRun = false;
                updateLastExecutionTime();
            }

            try {
                // 执行SingleThreadEventExecutor的run()方法
                SingleThreadEventExecutor.this.run();
            } catch (Throwable t) {
                logger.warn("Unexpected exception from an event executor: ", t);
                // 出现异常就清理并停止
                cleanupAndTerminate(false);
            }
        }
    };

    protected void cleanupAndTerminate(boolean success) {
        for (;;) {
            // CAS 将SingleThreadEventExecutor状态设置为ST_SHUTTING_DOWN
            int oldState = STATE_UPDATER.get(this);
            if (oldState >= ST_SHUTTING_DOWN || STATE_UPDATER.compareAndSet(
                    this, oldState, ST_SHUTTING_DOWN)) {
                break;
            }
        }

        // Check if confirmShutdown() was called at the end of the loop.
        // success表示confirmShutdown()是否调用成功，如果没有调用成功，打error日志
        if (success && gracefulShutdownStartTime == 0) {
            logger.error("Buggy " + EventExecutor.class.getSimpleName() + " implementation; " +
                    SingleThreadEventExecutor.class.getSimpleName() + ".confirmShutdown() must be called " +
                    "before run() implementation terminates.");
        }

        try {
            // Run all remaining tasks and shutdown hooks.
            // 运行confirmShutdown()直到调用成功
            for (;;) {
                if (confirmShutdown()) {
                    break;
                }
            }
        } finally {
            try {
                // 模板方法
                // 清除资源
                cleanup();
            } finally {
                // 将SingleThreadEventExecutor状态设置为终止
                STATE_UPDATER.set(this, ST_TERMINATED);
                // 释放信号量
                // 如果在confirmShutdown()执行之后到当前位置，仍然有其他线程提交任务到了任务队列
                // 就打个警告级别的日志
                // 不过这个有点极限，但是确实有很大可能发生
                // 毕竟整个SingleThreadEventExecutor几乎都没有用锁
                threadLock.release();
                if (!taskQueue.isEmpty()) {
                    logger.warn(
                            "An event executor terminated with " +
                                    "non-empty task queue (" + taskQueue.size() + ')');
                }
                // 初始化状态
                firstRun = true;
                // 终止成功
                terminationFuture.setSuccess(null);
            }
        }
    }
    // 该方法用于确认`SingleThreadEventExecutor`在shutdown之前实例中的任务是否全部执行完了
    // 如果如果没有就将当前时间点所有的任务执行完毕
    protected boolean confirmShutdown() {
        // 如果没有shutdown返回false
        if (!isShuttingDown()) {
            return false;
        }

        if (!inEventLoop()) {
            throw new IllegalStateException("must be invoked from an event loop");
        }
        // 取消所有定时任务
        cancelScheduledTasks();
        // 更新推荐Shutdown时间
        if (gracefulShutdownStartTime == 0) {
            gracefulShutdownStartTime = ScheduledFutureTask.nanoTime();
        }
        // 运行所有的任务和shutdown钩子
        if (runAllTasks() || runShutdownHooks()) {
            if (isShutdown()) {
                // Executor shut down - no new tasks anymore.
                return true;
            }

            // There were tasks in the queue. Wait a little bit more until no tasks are queued for the quiet period.
            wakeup(true);
            return false;
        }

        final long nanoTime = ScheduledFutureTask.nanoTime();
        // 如果运行剩余任务超时了，则直接返回
        if (isShutdown() || nanoTime - gracefulShutdownStartTime > gracefulShutdownTimeout) {
            return true;
        }
        // 如果最后一次执行任务的时间到当前时间低于QuietPeriod规定时间
        // 那么重新激活阻塞队列
        // 线程阻塞100ms
        // 返回false，表示还没有完全shutdown
        if (nanoTime - lastExecutionTime <= gracefulShutdownQuietPeriod) {
            // Check if any tasks were added to the queue every 100ms.
            // TODO: Change the behavior of takeTask() so that it returns on timeout.
            wakeup(true);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // Ignore
            }

            return false;
        }

        // No tasks were added for last quiet period - hopefully safe to shut down.
        // (Hopefully because we really cannot make a guarantee that there will be no execute() calls by a user.)
        return true;
    }
```

通过上面的分析我们知道`SingleThreadEventExecutor`阻塞队列中生产者方法是`execute(Runnable)`方法，消费者方法则是`run()`方法。前者向阻塞队列中添加任务，后者则从队列中取出任务执行。

`SingleThreadEventExecutor`提供了两个从阻塞队列中取出任务的方法，以及两个直接执行任务的方法：

1. protected Runnable pollTask()
2. protected Runnable takeTask()
3. protected boolean runAllTasks()
4. protected boolean runAllTasks(long timeoutNanos)

这里主要介绍`protected boolean runAllTasks(long timeoutNanos)`方法：

```java
    protected boolean runAllTasks(long timeoutNanos) {
        // 从定时队列中获取当前时间为止需要执行的所有任务，放入到taskQueue中
        fetchFromScheduledTaskQueue();
        // 从taskQueue中
        Runnable task = pollTask();
        if (task == null) {
            afterRunningAllTasks();
            return false;
        }
        // 获取超时时间时间点
        final long deadline = ScheduledFutureTask.nanoTime() + timeoutNanos;
        long runTasks = 0;
        long lastExecutionTime;
        for (;;) {
            try {
                // 运行任务
                task.run();
            } catch (Throwable t) {
                logger.warn("A task raised an exception.", t);
            }

            runTasks ++;

            // 每执行64次任务检查一次是否超时
            if ((runTasks & 0x3F) == 0) {
                lastExecutionTime = ScheduledFutureTask.nanoTime();
                if (lastExecutionTime >= deadline) {
                    break;
                }
            }
            // 获取新的任务
            task = pollTask();
            // 如果没有任务执行了，就填写最后执行时间
            if (task == null) {
                lastExecutionTime = ScheduledFutureTask.nanoTime();
                break;
            }
        }
        afterRunningAllTasks();
        this.lastExecutionTime = lastExecutionTime;
        return true;
    }
```

至此，我们就了解了`SingleThreadEventExecutor`提供的功能，该类提供了一个阻塞队列管理提交的任务，并且提供了一系列的管理方法，以便子类调用处理队列中的任务。

### SingleThreadEventLoop

`SingleThreadEventLoop`是`EventLoop`接口的简单实现，具体是使用`SingleThreadEventExecutor`进行实现的。该接口为`EventLoop`接口提供了默认实现。事实上`EventLoop`接口仅仅要求实现`Channel`注册的相关方法。因此我们重点查看如下方法：

```java
    @Override
    public ChannelFuture register(Channel channel) {
        return register(new DefaultChannelPromise(channel, this));
    }

    @Override
    public ChannelFuture register(final ChannelPromise promise) {
        ObjectUtil.checkNotNull(promise, "promise");
        promise.channel().unsafe().register(this, promise);
        return promise;
    }
```

可以看到，注册一个channel到`EventLoop`中也是一个异步操作，因为，在注册过程中传入了一个`Promise`类型的对象，注册过程仅仅是调用了`Channel`中Unsafe对象的`register(EventLoop，Promise)`方法，这个方法在介绍Channel中再详细讲解。

除了上面提到的为`EventLoop`提供了接口实现以外，`SingleThreadEventLoop`还提供了一个功能：由于`EventLoop`不断监听`taskQueue`，一旦有任务就执行，没有任务就空轮询，因此，`SingleThreadEventLoop`提供了每次循环执行完毕后，执行某些特定任务，上述功能的支持。该功能的实现主要依赖如下属性：

```java
private final Queue<Runnable> tailTasks;
```

在`runAllTasks(long timeoutNanos)`方法中执行了如下方法：

```java
afterRunningAllTasks();
this.lastExecutionTime = lastExecutionTime;
return true;
```

`afterRunningAllTasks()`该方法负责执行`tailTasks`中保存的所有任务。

### NIOEventLoop

