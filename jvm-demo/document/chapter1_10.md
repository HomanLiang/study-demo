# 对象的实例化、内存布局与访问定位

## 对象的实例化

### 大厂面试题

- 美团：

  对象在 JVM 中是怎么存储的？

  对象头信息里面有哪里东西？

- 蚂蚁金服：

  二面：Java 对象头里有什么？

### 对象的实例化

![image-20210118214721589](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210118214721589.png)

### 创建对象的步骤

前面所述是从字节码角度看待对象的创建过程，现在从执行步骤的角度来分析：

![image-20210118214803774](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210118214803774.png)



## 对象的内存布局

![image-20210118215025610](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210118215025610.png)

![image-20210118215049879](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210118215049879.png)





## 对象的访问定位

![image-20210118215142504](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210118215142504.png)

JVM 是如何通过栈帧中的对象引用访问到其内部对象实例的呢？

![image-20210118215156031](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210118215156031.png)

![image-20210118215240475](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210118215240475.png)

![image-20210118215252385](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210118215252385.png)









































