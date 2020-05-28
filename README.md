# About
提供一个 既可以 配置 core size, 也可限制 max pool size, 且可以无限提交 execute 的 Executor。

用于解决 App 启动阶段，并发执行太多 task，导致线程暴涨，性能下降问题; 或者 core size 配置不合理导致的 性能问题


# More
[Fast-Executor 原理](https://blog.icodes.xyz/2020/05/28/Fast-Executor-Thread-Pool/)
[AsyncTask](https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/os/AsyncTask.java;l=199?q=AsyncTask&sq=)
