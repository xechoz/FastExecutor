package xyz.icodes.executor

import java.util.*
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.math.max
import kotlin.math.min

/**
 *
 */
class FastExecutor private constructor(private val executor: ThreadPoolExecutor): ExecutorService by executor {
    companion object {
        private const val TAG = "FastExecutor"
        private val CPU_COUNT = Runtime.getRuntime().availableProcessors()
        private val CORE_SIZE = max(2, min(CPU_COUNT - 1, 4))
        private val MAX_SIZE = CPU_COUNT * 2 + 1
        val DEFAULT: ExecutorService by lazy {
            newExecutor()
        }
        
        fun newExecutor(name: String = "Fast",
                        corePoolSize: Int = CORE_SIZE,
                        maximumPoolSize: Int = MAX_SIZE,
                        keepAliveTime: Long = 30,
                        unit: TimeUnit = TimeUnit.SECONDS,
                        workQueue: BlockingQueue<Runnable> = LinkedBlockingDeque(128),
                        threadFactory: ThreadFactory = DefaultThreadFactory(
                            name
                        )
        ): ExecutorService {
            val executor = object : ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory) {
                lateinit var idleTask: (ExecutorService)->Unit

                override fun afterExecute(r: Runnable?, t: Throwable?) {
                    super.afterExecute(r, t)

                    if (queue.remainingCapacity() > 0) {
                        idleTask(this)
                    }
                }
            }

            val fastExecutor = FastExecutor(executor)
            executor.setRejectedExecutionHandler { r, _ ->
                r?.let { fastExecutor.fillSecondQueue(it) }
            }
            executor.idleTask = {
                fastExecutor.pollSecondQueue()
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
                    fastExecutor.pollSecondQueue()
                }
            }
        }
    }

    private val secondQueue = SecondQueue()

    /**
     * fill rejected tack into  second queue
     */
    private fun fillSecondQueue(task: Runnable) {
        println("$TAG, fillSecondQueue second queue size: ${secondQueue.size()}, first Queue size ${executor.queue.size}, ${executor.poolSize}, task: $task")
        secondQueue.offer(task)
    }

    /**
     * poll task from second queue when first queue is not full
     */
    private fun pollSecondQueue() {
        secondQueue.poll()?.let {
            println("$TAG, pollSecondQueue queue size: ${secondQueue.size()}, first Queue size ${executor.queue.size}, ${executor.poolSize}, task: $it")
            execute(it)
        }
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
}

private class DefaultThreadFactory(private val name: String) : ThreadFactory {
    private val index = AtomicInteger(0)

    override fun newThread(r: Runnable?): Thread {
        val thread = Thread(r)
        thread.name = "${name}-${index.getAndIncrement()}"
        return thread
    }
}
