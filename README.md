# About
提供一个 既可以 配置 core size, 也可限制 max pool size, 且可以无限提交 execute 的 Executor。

用于解决 App 启动阶段，并发执行太多 task，导致线程暴涨，性能下降问题; 或者 core size 配置不合理导致的 性能问题

# 背景

# 使用

FastExecutor 返回的是 `ExecutorService` 接口，跟一般的线程池一样使用即可

```kotlin
// executor 建议全局保持一个实例
// 第一种：使用封装的方法:
val executor = FastExecutors.newWork()

// 第二种: 按需配置参数
// executor = FastExecutor.newExecutor(name = "MyApp")

executor.execute { 
    println("Hello Demo")
}
```

# IAppExecutor

根据 Android 的使用，封装了 work， io， ui， looper 四个常用的使用场景

```kotlin
val executor = FastExecutors.newAppExecutor()

executor.apply {
    // 等价于 ui().post
    postUi {
        println("post ui")
    }

    // 等价于 work().execute 
    postWork {
        println("post work")
    }

    io().execute {
        println("post io")
    }

    looper().post {
        println("post looper")
    }
}
```

# 建议

线程池至少需要分两个类型，一个是用于 io，一个用于 cpu。  
因为 io 更多的时候 thread 是处于 waiting 的状态，可以尽量多线程；
cpu 密集型，应该避免线程太多，频繁切换线程，容易导致 thread context 频繁切换出现性能问题.  
cpu 密集型的线程池，可能使用 looper, 或者 单线程的模型也可以很高效。

# 参考
1. [Fast-Executor 原理](https://blog.icodes.xyz/2020/05/28/Fast-Executor-Thread-Pool/)
2. [AsyncTask](https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/os/AsyncTask.java;l=199?q=AsyncTask&sq=)
