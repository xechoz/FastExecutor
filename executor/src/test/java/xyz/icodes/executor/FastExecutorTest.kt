package xyz.icodes.executor

import org.junit.Test
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class FastExecutorTest {
    @Test
    fun run() {
        val executor = FastExecutor.newExecutor(
            name = "Demo",
            corePoolSize = 1,
            maximumPoolSize = 2,
            keepAliveTimeSecond = 30,
            workQueue =  LinkedBlockingDeque(2)
        )

        val counter = AtomicInteger(0)

        (0..100).forEach {
            executor.execute(object : Runnable {
                override fun run() {
                    val time = System.nanoTime() % 20
                    println("${Thread.currentThread()} sleep $time ms, ${toString()}")
                    Thread.sleep(time)
                    println("${Thread.currentThread()}, ${toString()}, finished")


                    if (counter.incrementAndGet() == 11) {
                        println("executor: $executor")

                        val handler = Executors.newScheduledThreadPool(1)
                        handler.schedule({
                            println("executor: $executor")
                        }, 0, TimeUnit.MILLISECONDS)

                        handler.schedule({
                            println("executor: $executor")
                        }, 2000, TimeUnit.MILLISECONDS)
                    }
                }

                override fun toString(): String {
                    return "task-$it"
                }
            })
        }
    }
}