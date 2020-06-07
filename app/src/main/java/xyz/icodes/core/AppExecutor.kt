package xyz.icodes.core

import xyz.icodes.executor.FastExecutors

/**
 * @author ZhengJianHui
 * Date：2020/6/8
 * Description:
 */

object App {
    val executor = FastExecutors.newAppExecutor()
}