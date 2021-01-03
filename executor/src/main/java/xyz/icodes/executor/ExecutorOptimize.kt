package com.yy.hiyo.startup.executor

import android.annotation.SuppressLint
import android.os.Process
import android.util.Log
import androidx.annotation.IntRange
import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

private const val CPU_CORE: Int = 8
private const val MAX_WAIT_TASK = 1
private const val MAX_CPU_PERCENT = 90 // MAX CPU USAGE PERCENT
private const val TAG = "ExecutorOptimize"

@SuppressLint("LogUsage")
class ExecutorOptimize(private val executor: ThreadPoolExecutor,
                       private val setting: OptimizeSetting = PERFORMANCE.setting) {
    private val monitor = OptimizeMonitor()
    private val originSetting = OriginSetting(executor.corePoolSize, executor.maximumPoolSize)

    fun start() {
        Log.d(TAG, "start")
        monitor.onChange = this::onChanged
        monitor.start()
    }

    private fun onChanged(from: CoreData, to: CoreData) {
        if (to.cpuPercent < setting.maxCpuPercent) {
            // up
            if (from.cpuPercent < setting.maxCpuPercent) {
                val corePoolSize = executor.corePoolSize
                val queueSize = executor.queue.size

                if (queueSize > setting.maxWaitTask && executor.corePoolSize < setting.maxPoolSize) {
                    executor.corePoolSize = corePoolSize + 1
                    Log.d(TAG, "corePoolSize increase ${executor.corePoolSize}, taskCount ${executor.queue.size}")
                } else if (queueSize < setting.maxWaitTask) {
                    if (corePoolSize > 1 && corePoolSize > setting.coreSize) {
                        executor.corePoolSize = corePoolSize - 1
                        Log.d(TAG, "corePoolSize decrease ${executor.corePoolSize},  taskCount ${executor.queue.size}")
                    }
                }
            }
        } else {
            // decrease
        }
    }
}

private data class OriginSetting(val coreSize: Int, val maxPoolSize: Int)

data class OptimizeSetting(
        val coreSizeMin: Int = 2,
        @IntRange(from = 0, to = 4L * CPU_CORE)
        val coreSize: Int = CPU_CORE  - 1,
        val minCpuPercent: Int = 20,
        @IntRange(from = 0, to = 100)
        val maxCpuPercent: Int = MAX_CPU_PERCENT,
        val maxWaitTask: Int = MAX_WAIT_TASK,
        val minPoolSize: Int = CPU_CORE,
        val maxPoolSize: Int = CPU_CORE + 1
)

sealed class OptimizeType(val setting: OptimizeSetting = OptimizeSetting())

object PERFORMANCE : OptimizeType(OptimizeSetting(
        coreSizeMin = 2,
        coreSize = CPU_CORE,
        minCpuPercent = 50,
        maxCpuPercent = 95,
        maxWaitTask = 2,
        minPoolSize = CPU_CORE + 1,
        maxPoolSize = CPU_CORE * 4
))

/**
 * monitor cpu , io usage
 */
@SuppressLint("ThreadUsage", "LogUsage")
private class OptimizeMonitor {
    lateinit var onChange: (from: CoreData, to: CoreData) -> Unit
    private val from = CoreData(0, 0, 0)
    private val to = CoreData(0, 0, 0)

    private val executor by lazy {
        Executors.newSingleThreadScheduledExecutor()
    }

    fun start() {
        Log.d(TAG, "monitor start")
        executor.scheduleAtFixedRate(Runnable {
            check()
        }, 100, 200, TimeUnit.MILLISECONDS)
    }

    fun stop() {

    }

    private fun check() {
        mock()
    }

    private fun mock() {
        onChange(from, to)
    }
}

data class CoreData(var cpuPercent: Int, var ioPercent: Int, var memoryPercent: Int)