# executor 的执行策略

当有无限多的  task 的时候，线程池是这样执行的:

1. core thread start, 直到 thread 数量达到 core size 
2. enque BlockQueue 直到 full 
3. non-core thread start, 直到 thread 数量达到 max pool size 
4. 执行拒绝 task 策略

默认的拒绝策略只有 4 种
1. AbortPolicy。 默认行为。抛异常
2. CallerRunsPolicy 直接在调用线程执行
3. DiscardPolicy 丢掉新的
4. DiscardOldestPolicy 丢掉最旧的

如果是 后端的线程池，丢弃，或者直接超时 是可以的，因为这个是预期的一个结果，比如 server 5xx 错误。但是客户端不一样，
客户端的 task 是不能丢的，丢了就影响流程，
比如启动App，执行很多task, 丢了任意一个都存在风险。
似乎可以使用 CallerRunsPolicy，但是这个会导致 调用线程很忙，或者
先提交的 task 没有机会执行。

# 问题是什么？选择合适的拒绝策略

目标
1. 不丢 task
2. 公平执行 task 
3. 等待合适的时机执行

Android AsynTask 也做了这个优化，其策略是，启动一个 back up 线程执行 这些task。
但是出现这种的时候，意味着task 超出了线程池的负载，
如果开启 back up 线程，这个会导致 back up 线程需要执行很多任务，
很可能抢占了本来就紧张的 cpu 资源

为了避免上面的情况，我的策略是，保存这些task，等到线程池空闲之后，线程池从这个队列获取task执行
这样就可以继续复用线程池的并发能力.

# 目标

1. core pool size 按需配置
2. max pool size 按需配置
3. 不能丢弃 task

因为 “不能丢弃 task”, 所以 queue 需要可以无限添加 task 的。但是 默认的 executor 策略，如果使用了 不限大小的 queue, 
就不会启动 core pool size 之外的 thread，或者配置为 core pool size 为 0， 全部改为 non-core pool, 但是这样就可能出现 thread 被回收，频繁创建 的情况

因为 默认的 executor 策略 达不到这个要求。

# 实现一个高效的 Executor

需要满足以下需求

1. 执行效率。
这个就需要 core size, max pool size 都需要生效，否则线程过少效率不行，或者闲置的线程太多都耗费资源。
根据 executor 的执行策略，
2. 可靠性
这个意味着 超出 queue size 的 task，需要保存起来，等待线程池空闲之后执行.
因此 需要 second queue 保存超出来的 task

# 流程

1. execute begin
2. core thread start, run task; until match core pool size, goto step 2
3. first queue enque, until queue full, goto step 3
4. non-core thread start, poll from first queue, until match max pool size
5. first queue is full, enque second queue 
6. after execute each task,  check if first queue not full, poll and execute second queue task 

a) first queue has a limit size 
b) second queue is not limit size, up to Int.MAX_VALUE


## Implemantation

