# ClassScanner

一个使用asm扫描class的gradle插件，用于扫描class中使用反射获取资源的代码，给出可能使用反射获取的资源id，方便进一步将R文件inline内联以减少包大小和dex数量。

你可以使用[ByteX](https://github.com/bytedance/ByteX)提供的插件将R资源inline，ByteX提供了使用资源名称配置白名单的能力。

## 使用方法

添加工程最外层gradle文件中下面的依赖：

```
classpath "io.github.yuweiguocn:scanner:1.0.2"
```

在application module的gradle文件中应用插件：

```
apply plugin: 'com.android.application'
apply plugin: 'class-scanner'
scannerConfig{
    debug = false // 是否打印日志
    needGetIdentifierResult = false // 是否需要扫描使用getResources().getIdentifier这种方式获取id的class
    needReflectResult = false // 是否需要扫描使用反射class的结果
    packageName = "io/github/yuweiguo/classscaner" //高优先级
    ignorePackageName = "android/,androidx/"
}
```

扫描结果可以在build/outputs目录下的scanner_result.json文件中找到：

```
{
  "possibleRids": [
    "io.github.yuweiguo.classscaner.R.mipmap.ic_launcher",
    "io.github.yuweiguo.classscaner.R.mipmap.testcall",
    "io.github.yuweiguo.classscaner.R.mipmap.testcall2",
    "io.github.yuweiguo.classscaner.R.mipmap.weather_detail_icon_"
  ],
  "getResource": [
    "directCall io.github.yuweiguo.classscaner.L.getId(): 18 { java.lang.reflect.Field.getInt }",
    "directCall io.github.yuweiguo.classscaner.MainActivity.onCreate(): 18 { java.lang.reflect.Field.getInt }",
    "indirectCall io.github.yuweiguo.classscaner.MainActivity.test(): 34 { io.github.yuweiguo.classscaner.L.getId }",
    "indirectCall io.github.yuweiguo.classscaner.MainActivity.test2(): 30 { io.github.yuweiguo.classscaner.L.getId }"
  ],
  "useReflect": [],
  "getResourceDetail": [
    {
      "type": "directCall",
      "lineNum": "18",
      "className": "io.github.yuweiguo.classscaner.MainActivity",
      "methodName": "onCreate",
      "ids": "ic_launcher",
      "rClass": "io.github.yuweiguo.classscaner.R.mipmap",
      "reflect": "java.lang.reflect.Field.getInt"
    },
    {
      "type": "indirectCall",
      "lineNum": "30",
      "className": "io.github.yuweiguo.classscaner.MainActivity",
      "methodName": "test2",
      "ids": "testcall2",
      "rClass": "io.github.yuweiguo.classscaner.R.mipmap",
      "reflect": "io.github.yuweiguo.classscaner.L.getId"
    },
    {
      "type": "indirectCall",
      "lineNum": "34",
      "className": "io.github.yuweiguo.classscaner.MainActivity",
      "methodName": "test",
      "ids": "testcall",
      "rClass": "io.github.yuweiguo.classscaner.R.mipmap",
      "reflect": "io.github.yuweiguo.classscaner.L.getId"
    },
    {
      "type": "directCall",
      "lineNum": "18",
      "className": "io.github.yuweiguo.classscaner.L",
      "methodName": "getId",
      "ids": "weather_detail_icon_",
      "rClass": "io.github.yuweiguo.classscaner.R.mipmap",
      "reflect": "java.lang.reflect.Field.getInt"
    }
  ],
  "useReflectDetail": []
}
```

## 原理

插件会进行两次扫描，第一次扫描收集所有使用反射的集合，包含class名称为method名称，还会收集当前R文件中包含的所有资源类型；第二次扫描收集所有反射和间接调用包含反射的集合，间接调用就是调用了第一次集合中的方法。


### 获取资源

首先是使用getIdentifier获取资源id的情况，如果调用了`android.content.res.Resources.getIdentifier`方法，则认为是获取了资源，然后收集当前方法调用获取资源之前的所有字符串常量，也就是所有可能的资源名称，如果常量中包含R资源类型，则认为是获取的该类型的资源id，下面的代码，字符串常量有两个，我们认为testident的资源类型是id：
```
getResources().getIdentifier("testident", "id", getPackageName());
```
这里需要注意的是不需要考虑使用getResources().getIdentifier这种方式获取id的代码，因为这种方式并没有用到R资源索引文件，而是直接使用native从resources.arsc资源映射表获取的资源id。

第二种情况是使用反射`java.lang.reflect.Field.getInt`方法获取资源值，当参数值传入了null我们认为是获取了资源值，然后收集当前方法调用获取资源之前的所有字符串常量，也就是所有可能的资源名称，比如下面的代码，会被误判使用反射获取了资源，但最后扫描信息产出可能的id集合时，由于没有获取到R文件信息所以不会产出到可能的id集合中。

```
try {
    Field field = MainActivity.class.getField("mConfigChangeFlags");
    int mConfigChangeFlags = field.getInt(null);
    L.d("mConfigChangeFlags" + mConfigChangeFlags);
} catch (Exception e) {
    e.printStackTrace();
}
```





