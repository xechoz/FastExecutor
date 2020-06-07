package xyz.icodes.executor

import xyz.icodes.executor.ExecutorConfig.MAX_SIZE
import xyz.icodes.executor.ExecutorConfig.TAG
import java.util.concurrent.Executor

/**
 * @author ZhengJianHui
 * Date：2020/6/6
 * Description: 适用于 io 操作的线程池
 * 因为 io 是重读写，轻cpu 的操作，所以线程数可以尽量多
 */

class IOExecutor(executor: Executor): Executor by executor {
    companion object {
        fun newInstance(corePoolSize: Int = 2*MAX_SIZE): Executor {
            val executor = FastExecutor.newExecutor(
                name = "$TAG.IO",
                corePoolSize = corePoolSize,
                maximumPoolSize = corePoolSize,
                allowCoreThreadTimeOut = false
            )

            return IOExecutor(executor = executor)
        }
    }
}

