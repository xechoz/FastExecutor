package xyz.icodes.executor

import kotlin.math.max
import kotlin.math.min

/**
 * @author ZhengJianHui
 * Date：2020/6/7
 * Description:
 */

internal object ExecutorConfig {
    const val TAG = "Fast"
    val CPU_COUNT = Runtime.getRuntime().availableProcessors()
    val CORE_SIZE = max(2, min(CPU_COUNT - 1, 4))
    val MAX_SIZE = CPU_COUNT * 2 + 1
    val QUEUE_CAPACITY = CORE_SIZE * 2
}