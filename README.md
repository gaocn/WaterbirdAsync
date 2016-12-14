Android 中的异步任务常用的一种方式是：Handler + Thread 组合来实现的。Thread 负责子线程的耗时操作，Handler 负责线程间的通信，用的最多的当属子线程和主线程通信。
Android 为了简化操作，提供了 AsyncTask 类来实现异步任务，并且轻松实现子线程和主线程间的通信。AsyncTask主要有二个部分：一个是与主线各的交互，另一个就是线程的管理调度。
AsyncTask 虽然提供了cancle( true ) 方法来停止任务，但是这个方法只是中断了这个线程，但是并不能真正意义上的停止任务，这也是很多人说 AsyncTask 的弊端。极容易造成内存溢出的。

###三种间接结束任务的方式：
1. 判断标志位，类似Java中无法停止一个正在运行的线程，Android中的AsyncTask也是一样，为此需要设置一个标志位，并且在doInBackground中的关键步骤决定是都停止该任务；
2. 使用Exception，从外部调用AsyncTask的cancel方法不能终止任务，同样调用一个线程的interrupt方法之后线程仍然运行，但是若该线程中调用过sleep或wait方法后，处于sleep或wait状态，则sleep或wait状态立即结束并且抛出InterruptException异常；因此若在AsyncTask的doInBackground方法中调用了sleep或wait方法，则在UI线程中调用任务实例的cancel方法后，sleep或wait立即结束并且抛出InterruptedException异常，但是如果捕获该异常的代码后面还有其他代码，则这些代码还会继续执行。
###AsyncTask串行处理任务和并行处理任务
THREAD_POOL_EXECUTOR 并行线程池(ThreadPoolExecutor实现线程池，然而对于多用户、高并发的应用来说，提交的任务数量非常巨大，一定会比允许的最大线程数多很多。为了解决这个问题，必须要引入排队机制，或者是在内存中，或者是在硬盘等容量很大的存储介质中。ThreadPoolExecutor只支持任务在内存中排队，通过BlockingQueue暂存还没有来得及执行的任务)

    ```
           ThreadPoolExecutor(int corePoolSize,
                              int maximumPoolSize,
                              long keepAliveTime,
                              TimeUnit unit,
                              BlockingQueue<Runnable> workQueue,
                              ThreadFactory threadFactory)
    ```
    1. corePoolSize
        线程池的基本大小，即在没有任务需要执行的时候线程池的大小，并且只有在工作队列满了的情况下才会创建超出这个数量的线程。这里需要注意的是：在刚刚创建ThreadPoolExecutor的时候，线程并不会立即启动，而是要等到有任务提交时才会启动，除非调用了prestartCoreThread/prestartAllCoreThreads事先启动核心线程。再考虑到keepAliveTime和allowCoreThreadTimeOut超时参数的影响，所以没有任务需要执行的时候，线程池的大小不一定是corePoolSize。
    2. maximumPoolSize
        线程池中允许的最大线程数，线程池中的当前线程数目不会超过该值。如果队列中任务已满，并且当前线程个数小于maximumPoolSize，那么会创建新的线程来执行任务。这里值得一提的是largestPoolSize，该变量记录了线程池在整个生命周期中曾经出现的最大线程个数。为什么说是曾经呢？因为线程池创建之后，可以调用setMaximumPoolSize()改变运行的最大线程的数目。
    3. poolSize
        线程池中当前线程的数量，当该值为0的时候，意味着没有任何线程，线程池会终止；同一时刻，poolSize不会超过maximumPoolSize。
    4. keepAliveTime
        当线程空闲时间达到keepAliveTime，该线程会退出，直到线程数量等于corePoolSize。如果allowCoreThreadTimeout设置为true，则所有线程均会退出直到线程数量为0。
SERIAL_EXECUTOR 串行线程池(例如：同一时刻有两个任务要处理，AsyncTask会先执行第一个任务，等第一个任务执行结束，然后才会执行第二个任务。)

###线程创建规则
    一个任务通过 execute(Runnable)方法被添加到线程池，任务就是一个 Runnable类型的对象，任务的执行方法就是 Runnable类型对象的run()方法。
    当一个任务通过execute(Runnable)方法欲添加到线程池时：
    1. 如果此时线程池中的数量小于corePoolSize，即使线程池中的线程都处于空闲状态，也要创建新的线程来处理被添加的任务。
    2. 如果此时线程池中的数量等于 corePoolSize，但是缓冲队列 workQueue未满，那么任务被放入缓冲队列。
    3. 如果此时线程池中的数量大于corePoolSize，缓冲队列workQueue满，并且线程池中的数量小于maximumPoolSize，建新的线程来处理被添加的任务。
    4. 如果此时线程池中的数量大于corePoolSize，缓冲队列workQueue满，并且线程池中的数量等于maximumPoolSize，那么通过 handler所指定的策略来处理此任务。也就是：处理任务的优先级为：核心线程corePoolSize、任务队列workQueue、最大线程maximumPoolSize，如果三者都满了，使用handler处理被拒绝的任务。
    5. 当线程池中的线程数量大于 corePoolSize时，如果某线程空闲时间超过keepAliveTime，线程将被终止。这样，线程池可以动态的调整池中的线程数。
###线程池按以下行为执行任务
    1. 当线程数小于核心线程数时，创建线程。
    2. 当线程数大于等于核心线程数，且任务队列未满时，将任务放入任务队列。
    3. 当线程数大于等于核心线程数，且任务队列已满
        3-1. 若线程数小于最大线程数，创建线程
        3-2. 若线程数等于最大线程数，抛出异常，拒绝任务
###ThreadPoolExecutor默认实现
###CachedThreadPool
CachedThreadPool会创建一个缓存区，将初始化的线程缓存起来。会终止并且从缓存中移除已有60秒未被使用的线程。如果线程有可用的，就使用之前创建好的线程;如果线程没有可用的，就新创建线程。
1. 重用：缓存型池子，先查看池中有没有以前建立的线程，如果有，就reuse；如果没有，就建一个新的线程加入池中
2. 使用场景：缓存型池子通常用于执行一些生存期很短的异步型任务，因此在一些面向连接的daemon型SERVER中用得不多。
3. 超时：能reuse的线程，必须是timeout IDLE内的池中线程，缺省timeout是60s，超过这个IDLE时长，线程实例将被终止及移出池。
4. 结束：注意，放入CachedThreadPool的线程不必担心其结束，超过TIMEOUT不活动，其会自动被终止。

```
 public static ExecutorService newCachedThreadPool() {
     return new ThreadPoolExecutor(0, Integer.MAX_VALUE,
             60L, TimeUnit.SECONDS,
             new SynchronousQueue<Runnable>());
 }

 public static ExecutorService newCachedThreadPool(ThreadFactory threadFactory) {
     return new ThreadPoolExecutor(0, Integer.MAX_VALUE,
             60L, TimeUnit.SECONDS,
             new SynchronousQueue<Runnable>(),
             threadFactory);
 }

```
###FixedThreadPool
在FixedThreadPool中，有一个固定大小的池。如果当前需要执行的任务超过池大小，那么多出的任务处于等待状态，直到有空闲下来的线程执行任务，如果当前需要执行的任务小于池大小，空闲的线程也不会去销毁。
1. 重用：fixedThreadPool与cacheThreadPool差不多，也是能reuse就用，但不能随时建新的线程
2. 固定数目：其独特之处在于，任意时间点，最多只能有固定数目的活动线程存在，此时如果有新的线程要建立，只能放在另外的队列中等待，直到当前的线程中某个线程终止直接被移出池子
3. 超时：和cacheThreadPool不同，FixedThreadPool没有IDLE机制（可能也有，但既然文档没提，肯定非常长，类似依赖上层的TCP或UDP IDLE机制之类的），
4. 使用场景：所以FixedThreadPool多数针对一些很稳定很固定的正规并发线程，多用于服务器

```
public static ExecutorService newFixedThreadPool(int nThreads) {
     return new ThreadPoolExecutor(nThreads, nThreads,
             0L, TimeUnit.MILLISECONDS,
             new LinkedBlockingQueue<Runnable>());
 }

 public static ExecutorService newFixedThreadPool(int nThreads, ThreadFactory threadFactory) {
     return new ThreadPoolExecutor(nThreads, nThreads,
             0L, TimeUnit.MILLISECONDS,
             new LinkedBlockingQueue<Runnable>(),
             threadFactory);
 }

```
####源码分析：
    从方法的源代码看，cache池和fixed 池调用的是同一个底层池，只不过参数不同：
    1. fixed池线程数固定，并且是0秒IDLE（无IDLE）;
    2. cache池线程数支持0-Integer.MAX_VALUE(显然完全没考虑主机的资源承受能力），60秒IDLE;

####SingleThreadExecutor
SingleThreadExecutor得到的是一个单个的线程，这个线程会保证你的任务执行完成。<font color="red">如果当前线程意外终止，会创建一个新线程继续执行任务</font>，这和我们直接创建线程不同，也和newFixedThreadPool(1)不同。
```
public static ExecutorService newSingleThreadExecutor() {
     return new FinalizableDelegatedExecutorService
             (new ThreadPoolExecutor(1, 1,
                     0L, TimeUnit.MILLISECONDS,
                     new LinkedBlockingQueue<Runnable>()));
 }

 public static ExecutorService newWorkStealingPool(int parallelism) {
     return new ForkJoinPool
             (parallelism,
                     ForkJoinPool.defaultForkJoinWorkerThreadFactory,
                     null, true);
 }

 public static ScheduledExecutorService newSingleThreadScheduledExecutor() {
     return new DelegatedScheduledExecutorService
             (new ScheduledThreadPoolExecutor(1));
 }

 public static ExecutorService newSingleThreadExecutor(ThreadFactory threadFactory) {
     return new FinalizableDelegatedExecutorService
             (new ThreadPoolExecutor(1, 1,
                     0L, TimeUnit.MILLISECONDS,
                     new LinkedBlockingQueue<Runnable>(),
                     threadFactory));
 }

 public static ScheduledExecutorService newSingleThreadScheduledExecutor(ThreadFactory threadFactory) {
     return new DelegatedScheduledExecutorService
             (new ScheduledThreadPoolExecutor(1, threadFactory));
 }

```
###ScheduledThreadPool
ScheduledThreadPool是一个固定大小的线程池，与FixedThreadPool类似，执行的任务是定时执行。
```
public static ScheduledExecutorService newScheduledThreadPool(
         int corePoolSize, ThreadFactory threadFactory) {
     return new ScheduledThreadPoolExecutor(corePoolSize, threadFactory);
 }

 public static ScheduledExecutorService newScheduledThreadPool(int corePoolSize) {
     return new ScheduledThreadPoolExecutor(corePoolSize);
 }
```
