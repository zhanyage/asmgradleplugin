# asmgradleplugin
一个通过 asm + gradle plugin 插桩的工程
目前插桩的内容为在全部方法中插入 Log.i
如果需要修改，可以通过修改 MethodCollector 修改插桩的内容

操作步骤：
1. 使用Android Studio打开工程 asmgradleplugin
2. 运行gradle task ":asm-gradle-plugin:buildAndPublishToLocalMaven" 编译plugin插件
3. 使用Android Studio单独打开自己的工程，在根 build.gradle 依赖 
   classpath ("com.zhanyage.asm:asm-gradle-plugin:1.3") { changing = true }
   注意：因为我们的脚本是发不到本地 maven ，所以我们需要在 repositories 中加入 mavenLocal()
5. 在 app 的 build.gradle 中使用如下配置

```
apply plugin: 'com.zhanyage.asm-plugin'

asm {
    enable = true
    baseMethodMapFile = "${project.buildDir}/systrace_output/Debug.methodmap"
    blackListFile = "${project.projectDir}/blacklist/blackMethodList.txt"
}
```
