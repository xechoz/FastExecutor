# About
提供一个 既可以 配置 core size, 也可限制 max pool size, 且可以无限提交 execute 的 Executor。

用于解决 App 启动阶段，并发执行太多 task，导致线程暴涨，性能下降问题; 或者 core size 配置不合理导致的 性能问题

# 背景

# 使用

FastExecutor 返回的是 `ExecutorService` 接口，跟一般的线程池一样使用即可

```kotlin
// executor 建议全局保持一个实例
val executor = FastExecutor.newExecutor(name = "MyApp")

// 或者使用默认的
// executor = FastExecutor.DEFAULT

executor.execute { 
    println("Hello Demo")
}
```

# 参考
1. [Fast-Executor 原理](https://blog.icodes.xyz/2020/05/28/Fast-Executor-Thread-Pool/)
2. [AsyncTask](https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/os/AsyncTask.java;l=199?q=AsyncTask&sq=)
