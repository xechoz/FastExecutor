package xyz.icodes.executor

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import java.util.concurrent.Executor
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
}

interface IAppExecutor {
    fun io(): IExecutor

    fun looper(): ILooperExecutor

    fun work(): IExecutor

    fun ui(): ILooperExecutor
    fun postUi(runnable: () -> Unit)
    fun postWork(runnable: () -> Unit)
}

interface IExecutor : Executor {
    fun remove(runnable: Runnable?)
}

interface ILooperExecutor : IExecutor {
    fun post(runnable: () -> Unit)

    fun postDelay(runnable: () -> Unit, delayMillis: Long)
}

private class AndroidExecutor : IAppExecutor {
    private val io = FastExecutors.newIOExecutor()
    private val work = FastExecutors.newWork()
    private val looper = LooperExecutor()
    private val ui = UiExecutor()

    override fun io(): IExecutor = io

    override fun looper(): ILooperExecutor = looper

    override fun work(): IExecutor = work

    override fun ui(): ILooperExecutor = ui

    override
    fun postUi(runnable: () -> Unit) {
        ui.post(runnable)
    }

    override
    fun postWork(runnable: () -> Unit) {
        work.execute(runnable)
    }
}

object FastExecutors {
    fun newIOExecutor(corePoolSize: Int = 2 * ExecutorConfig.MAX_SIZE): IExecutor {
        return FastExecutor.newExecutor(
            name = "${ExecutorConfig.TAG}.IO",
            corePoolSize = corePoolSize,
            maximumPoolSize = corePoolSize,
            allowCoreThreadTimeOut = false
        )
    }

    fun newWork(): IExecutor {
        return FastExecutor.newExecutor()
    }

    fun newAppExecutor(): IAppExecutor {
        return AndroidExecutor()
    }
}

private abstract class BaseLooperExecutor : ILooperExecutor {
    protected abstract val handler: Handler

    override fun post(runnable: () -> Unit) {
        handler.post(runnable)
    }

    override fun postDelay(runnable: () -> Unit, delayMillis: Long) {
        handler.postDelayed(runnable, delayMillis)
    }

    override fun remove(runnable: Runnable?) {
        runnable?.let {
            handler.removeCallbacks(it)
        }
    }

    override fun execute(command: Runnable) {
        handler.post(command)
    }
}

private class LooperExecutor : BaseLooperExecutor() {
    private val thread: HandlerThread by lazy {
        HandlerThread("")
    }

    override
    val handler: Handler by lazy {
        thread.start()
        Handler(thread.looper)
    }
}

private class UiExecutor : BaseLooperExecutor() {
    override val handler: Handler by lazy {
        Handler(Looper.getMainLooper())
    }
}