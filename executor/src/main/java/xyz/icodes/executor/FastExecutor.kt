package xyz.icodes.executor

import xyz.icodes.executor.ExecutorConfig.CORE_SIZE
import xyz.icodes.executor.ExecutorConfig.MAX_SIZE
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock

/**
 *
 */
class FastExecutor private constructor(private val executor: ThreadPoolExecutor):
    Executor by executor {
    companion object {
        private const val TAG = "${ExecutorConfig.TAG}.Executor"
        private val QUEUE_CAPACITY = CORE_SIZE * 2

        val DEFAULT: Executor by lazy {
            newExecutor()
        }

        /**
         * @param name thread prefix name
         * @param corePoolSize
         * @param maximumPoolSize
         * @param keepAliveTimeSecond
         * @param capacity 第一个队列最大数量，达到数量限制后，就会启动 非核心线程，之后超出的 task 会被放到 第二个队列
         * @param workQueue 第一个队列
         * @param threadFactory
         */
        fun newExecutor(name: String = "Fast",
                        corePoolSize: Int = CORE_SIZE,
                        maximumPoolSize: Int = MAX_SIZE,
                        keepAliveTimeSecond: Long = 30,
                        capacity: Int = QUEUE_CAPACITY,
                        allowCoreThreadTimeOut: Boolean = false,
                        workQueue: BlockingQueue<Runnable> = LinkedBlockingDeque(capacity),
                        threadFactory: ThreadFactory = DefaultThreadFactory(name)
        ): Executor {
            val executor = object : ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTimeSecond, TimeUnit.SECONDS, workQueue, threadFactory) {
                lateinit var idleTask: (ExecutorService)->Unit

                override fun afterExecute(r: Runnable?, t: Throwable?) {
                    super.afterExecute(r, t)

                    if (queue.remainingCapacity() > 0) {
                        idleTask(this)
                    }
                }
            }

            executor.allowCoreThreadTimeOut(allowCoreThreadTimeOut)
            val fastExecutor = FastExecutor(executor)
            executor.setRejectedExecutionHandler { r, _ ->
                r?.let { fastExecutor.fillSecondQueue(it) }
            }
            executor.idleTask = {
                fastExecutor.consumeOne()
            }

            println("$TAG, corePoolSize: $corePoolSize, maximumPoolSize： $maximumPoolSize, Capacity: ${workQueue.remainingCapacity()}")
            return fastExecutor
        }

        /**
         * @return a function, call it when task idle, etc, afterExecute
         */
        fun patch(executor: ThreadPoolExecutor): (queue: BlockingDeque<Runnable>) -> Unit {
            val fastExecutor = FastExecutor(executor)
            executor.setRejectedExecutionHandler { r, _ ->
                r?.let { fastExecutor.fillSecondQueue(it) }
            }

            return { queue ->
                if (queue.remainingCapacity() > 0) {
                    fastExecutor.consumeOne()
                }
            }
        }
    }

    private val secondQueue = SecondQueue()

    /**
     * fill rejected tack into  second queue
     */
    private fun fillSecondQueue(task: Runnable) {
//        println("$TAG, fillSecondQueue, " +
//                "first Queue: ${executor.queue.size}, " +
//                "second queue: ${secondQueue.size()}, " +
//                "poolSize ${executor.poolSize}, " +
//                "task: $task")
        secondQueue.offer(task)
    }

    private fun consumeOne() {
        if (executor.queue.isNotEmpty() || secondQueue.isNotEmpty()) {
            pollOne()?.let {
//                println("consumeOne $it")
                execute(it)
            }
        }
    }

    /**
     * poll task from first queue is not empty,
     * else from second queue
     */
    private fun pollOne(): Runnable? {
        executor.queue.poll()?.let {
//            println("$TAG, pollOne, from first queue, \n " +
//                    "first Queue: ${executor.queue.size}, " +
//                    "second queue: ${secondQueue.size()}, " +
//                    "poolSize ${executor.poolSize}, " +
//                    "task: $it")
            return it
        }

        secondQueue.poll()?.let {
//            println("$TAG, pollOne, from second queue，\n " +
//                    "first Queue: ${executor.queue.size}, " +
//                    "second queue: ${secondQueue.size()}, " +
//                    "poolSize ${executor.poolSize}, " +
//                    "task: $it")
            return it
        }

        return null
    }

    override fun toString(): String {
        return executor.toString()
    }
}

class SecondQueue {
    private val queue = LinkedList<Runnable>()
    private val lock = ReentrantLock()

    fun poll(): Runnable? {
        return if (queue.size > 0 && lock.tryLock()) {
            try {
                queue.poll()
            } finally {
                lock.unlock()
            }
        } else {
            // how to fix tryLock fail?
            null
        }
    }

    fun offer(task: Runnable) {
        lock.lock()
        try {
            queue.offer(task)
        } finally {
            lock.unlock()
        }
    }

    fun size() = queue.size

    fun isNotEmpty() = queue.size > 0
}

private class DefaultThreadFactory(private val name: String) : ThreadFactory {
    private val index = AtomicInteger(0)

    override fun newThread(r: Runnable?): Thread {
        val thread = Thread(r)
        thread.name = "${name}-${index.getAndIncrement()}"
        return thread
    }
}
