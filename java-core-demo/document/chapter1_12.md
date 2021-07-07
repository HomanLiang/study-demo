[toc]



# Java 日期时间

## 1.日期时间API的七宗罪

### 1.1.罪状一：Date同时表示日期和时间

`java.util.Date` 被设计为日期 + 时间的结合体。也就是说如果只需要日期，或者只需要单纯的时间，用 `Date` 是做不到的。

```
@Test
public void test1() {
    System.out.println(new Date());
}

输出：
Fri Jan 22 00:25:06 CST 2021
```

这就导致语义非常的不清晰，比如说：

```
/**
 * 是否是假期
 */
private static boolean isHoliday(Date date){
    return  ...;
}
```

判断某一天是否是假期，只和日期有关，和具体时间没有关系。如果代码这样写语义只能靠注释解释，方法本身无法达到自描述的效果，也无法通过强类型去约束，因此容易出错。

> 说明：本文所有例子不考虑时区问题，下同

### 1.2.罪状二：坑爹的年月日

```
@Test
public void test2() {
    Date date = new Date();
    System.out.println("当前日期时间：" + date);
    System.out.println("年份：" + date.getYear());
    System.out.println("月份：" + date.getMonth());
}

输出：
当前日期时间：Fri Jan 22 00:25:16 CST 2021
年份：121
月份：0
```

what？年份是121年，这什么鬼？月份返回0，这又是什么鬼？

无奈，看看这两个方法的 `Javadoc`：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210505153752.png)

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210505153758.png)

尼玛，原来 `2021 - 1900 = 121` 是这么来的。那么问题来了，为何是 `1900` 这个数字呢？

月份，竟然从 `0` 开始，这是学的谁呢？简直打破了我认为的只有 `index` 索引值才是从 `0` 开始的认知啊，这种做法非常的不符合人类思维有木有。

> 索引值从 `0` 开始就算了，毕竟那是给计算机看的无所谓，但是你这月份主要是给人看的呀

### 1.3.罪状三：Date是可变的

`oh my god`，也就是说我把一个 `Date` 日期时间对象传给你，你竟然还能给我改掉，真是太没安全感可言了。

```
@Test
public void test() {
    Date currDate = new Date();
    System.out.println("当前日期是①：" + currDate);
    boolean holiday = isHoliday(currDate);
    System.out.println("是否是假期：" + holiday);

    System.out.println("当前日期是②：" + currDate);
}

/**
 * 是否是假期
 */
private static boolean isHoliday(Date date) {
    // 架设等于这一天才是假期，否则不是
    Date holiday = new Date(2021 - 1900, 10 - 1, 1);

    if (date.getTime() == holiday.getTime()) {
        return true;
    } else {
        // 模拟写代码时不注意，使坏
        date.setTime(holiday.getTime());
        return true;
    }
}

输出：
当前日期是①：Fri Jan 22 00:41:59 CST 2021
是否是假期：true
当前日期是②：Fri Oct 01 00:00:00 CST 2021
```

我就像让你帮我判断下遮天是否是假期，然后你竟然连我的日期都给我改了？过分了啊。这是多么可怕的事，存在重大安全隐患有木有。

针对这种 `case`，一般来说我们函数内部操作的参数只能是**副本**：要么调用者传进来的就是副本，要么内部自己生成一个副本。

在本利中提高程序健壮性只需在 `isHoliday` 首行加入这句代码即可：

```
private static boolean isHoliday(Date date) {
    date = (Date) date.clone();
    ...
}
```

再次运行程序，输出：

```
当前日期是①：Fri Jan 22 00:44:10 CST 2021
是否是假期：true
当前日期是②：Fri Jan 22 00:44:10 CST 2021
```

bingo。

但是呢，`Date` 作为高频使用的 `API`，并不能要求每个程序员都有这种安全意识，毕竟即使百密也会有一疏。所以说，把 `Date` 设计为一个可变的类是非常糟糕的设计。

### 1.4.罪状四：无法理喻的java.sql.Date

来，看看 `java.util.Date` 类的继承结构：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210505153936.png)

它的三个子类均处于 `java.sql` 包内。且先不谈这种垮包继承的合理性问题，直接看下面这个使用例子：

```
@Test
public void test3() {
    // 竟然还没有空构造器
    // java.util.Date date = new java.sql.Date();
    java.util.Date date = new java.sql.Date(System.currentTimeMillis());

    // 按到当前的时分秒
    System.out.println(date.getHours());
    System.out.println(date.getMinutes());
    System.out.println(date.getSeconds());
}
```

运行程序，暴雷了：

```
java.lang.IllegalArgumentException
	at java.sql.Date.getHours(Date.java:187)
	at com.yourbatman.formatter.DateTester.test3(DateTester.java:65)
	...
```

what？又是一打破认知的结果啊，第一句 `getHours()` 就报错啦。走进 `java.sql.Date` 的方法源码进去一看，握草重写了父类方法：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210505154010.png)

还有这么重写父类方法的？还有王法吗？这也算是JDK能干出来的事？赤裸裸的违背里氏替换原则等众多设计原则，子类能力竟然比父类小，使用起来简直让人云里雾里。

`java.util.Date` 的三个子类均位于 `java.sql` 包内，他们三是通过 `Javadoc` 描述来进行分工的：

- `java.sql.Date`：只表示日期
- `java.sql.Time`：只表示时间
- `java.sql.Timestamp`：表示日期 + 时间

这么一来，似乎可以“理解” `java.sql.Date` 为何重写父类的 `getHours()` 方法改为抛出 `IllegalArgumentException` 异常了，毕竟它只能表示日期嘛。但是这种通过继承再阉割的实现手法你们接受得了？反正我是不能的~

### 1.5.罪状五：无法处理时区

因为日期时间的特殊性，不同的国家地区在**同一时刻**显示的日期时间应该是不一样的，但 `Date` 做不到，因为它底层代码是这样的：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210505154036.png)

也就是说它表示的是一个具体时刻（时间戳），这个数值放在全球任何地方都是一模一样的，也就是说 `new Date()` 和`System.currentTimeMillis()` 没啥两样。

`JDK` 提供了 `TimeZone` 表示时区的概念，但它在 `Date` 里并无任何体现，只能使用在格式化器上，这种设计着实让我再一次看不懂了。

### 1.6.罪状六：线程不安全的格式化器

关于 `Date` 的格式化，站在架构设计的角度来看，首先不得不吐槽的是 `Date` 明明属于 `java.util` 包，那么它的格式化器 `DateFormat` 为毛却跑到 `java.text` 里去了呢？这种依赖管理的什么鬼？是不是有点太过于随意了呢？

另外，`JDK` 提供了一个 `DateFormat` 的子类实现 `SimpleDateFormat` 专门用于格式化日期时间。**但是**它却被设计为了线程不安全的，一个定位为模版组件的 `API` 竟然被设计为线程不安全的类，实属瞎整。

就因为这个坑的存在，让多少初中级工程师泪洒职场，算了说多了都是泪。另外，因为线程不安全问题并非必现问题，因此在黑盒/白盒测试、功能测试阶段都可能测不出来，留下潜在风险。

> 这就是“灵异事件”：测试环境测试得好好的，为何到线上就出问题了呢？

### 1.7.罪状七：Calendar难当大任

从 `JDK 1.1` 开始，`Java` 日期时间 `API` 似乎进步了些，引入了 `Calendar` 类，并且对职责进行了划分：

- `Calendar` 类：日期和时间字段之间转换
- `DateFormat` 类：格式化和解析字符串
- `Date` 类：**只**用来承载日期和时间

有了 `Calendar` 后，原有 `Date` 中的大部分方法均标记为废弃，交由 `Calendar` 代替。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210505154115.png)

`Date` 终于单纯了些：只需要展示日期时间而无需再顾及年月日操作、格式化操作等等了。值得注意的是，这些方法只是被标记为过期，并未删除。即便如此，请在实际开发中也**一定不要使用**它们。

引入了一个 `Calendar` 似乎分离了职责，但 `Calendar` 难当大任，设计上依旧存在很多问题。

```
@Test
public void test4() {
    Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
    calendar.set(2021, 10, 1); // -> 依旧是可变的

    System.out.println(calendar.get(Calendar.YEAR));
    System.out.println(calendar.get(Calendar.MONTH));
    System.out.println(calendar.get(Calendar.DAY_OF_MONTH));
}

输出：
2021
10
1
```

年月日的处理上似乎可以接受没有问题了。从结果中可以发现，`Calendar` 年份的传值不用再减去 `1900` 了，这和 `Date` 是不一样的，不知道这种行为不一致会不会让有些人抓狂。

> 说明：`Calendar` 相关的 `API` 是由 `IBM` 捐过来的，所以和 `Date` 不一样貌似也“情有可原”

另外，还有个重点是 `Calendar` 依旧是可变的，所以存在不安全因素，参与计算改变值时请使用其副本变量。

总的来说，`Calendar` 在 `Date` 的基础上做了改善，但仅限于修修补补，**并未从根本上解决问题**。最重要的是 `Calendar` 的 `API` 使用起来真的很不方便，而且该类在语义上也完全不符合日期/时间的含义，使用起来更显尴尬。

### 1.8.自我救赎：JSR 310

因为原生的 `Date` 日期时间体系存在“**七宗罪**”，催生了第三方 `Java` 日期时间库的诞生，如大名鼎鼎的 `Joda-Time` 的流行甚至一度成为标配。

对于 `Java` 来说，如此重要的 `API` 模块岂能被第三方库给占据，开发者本就想简单的处理个日期时间还得导入第三方库，使用也太不方便了吧。当时的 `Java` 如日中天，因此就开启了“收编” `Joda-Time` 之旅。

2013年9月份，具有划时代意义的 `Java 8` 大版本正式发布，该版本带来了非常多的新特性，其中最引入瞩目之一便是全新的日期时间`API`：`JSR 310`。

`JSR 310` 规范的领导者是 `Stephen Colebourne`，此人也是 `Joda-Time` 的缔造者。不客气的说 `JSR 310` 是在 `Joda-Time` 的基础上建立的，参考了其绝大部分的 `API` 实现，因此若你之前是 `Joda-Time` 的重度使用者，现在迁移到 `Java 8` 原生的 `JSR 310` 日期时间上来几乎无缝。

即便这样，也并不能说 `JSR 310` 就完全等于 `Joda-Time` 的官方版本，还是有些许诧异的，例举如下：

1. 首先当然是包名的差别，`org.joda.time -> java.time` 标准日期时间包
2. **JSR 310不接受null值，Joda-Time把Null值当0处理**
3. `JSR 310` 所有抛出的异常是 `DateTimeException`，它是个 `RuntimeException`，而 `Joda-Time` 都是 `checked exception`

简单感受下 `JSR 310 API`：

```
@Test
public void test5() {
    System.out.println(LocalDate.now(ZoneId.systemDefault()));
    System.out.println(LocalTime.now(ZoneId.systemDefault()));
    System.out.println(LocalDateTime.now(ZoneId.systemDefault()));

    System.out.println(OffsetTime.now(ZoneId.systemDefault()));
    System.out.println(OffsetDateTime.now(ZoneId.systemDefault()));
    System.out.println(ZonedDateTime.now(ZoneId.systemDefault()));

    System.out.println(DateTimeFormatter.ISO_LOCAL_DATE.format(LocalDate.now()));
    System.out.println(DateTimeFormatter.ISO_LOCAL_TIME.format(LocalTime.now()));
    System.out.println(DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now()));
}
```

`JSR 310` 的所有对象都是**不可变**的，所以线程安全。和老的日期时间 `API` 相比，**最主要**的特征对比如下：

| **JSR 310** | **Date/Calendar** | **说明**                                                     |
| ----------- | ----------------- | ------------------------------------------------------------ |
| 流畅的API   | 难用的API         | API设计的好坏最直接影响编程体验，前者大大大大优于后者        |
| 实例不可变  | 实例可变          | 对于日期时间实例，设计为可变确实不合理也不安全。都不敢放心的传递给其它函数使用 |
| 线程安全    | 线程不安全        | 此特性直接决定了编码方式和健壮性                             |

关于 `JSR 310` 日期时间更多介绍此处就不展开了，毕竟前面文章啰嗦过好多次了。总之它是 `Java` 的新一代日期时间 `API`，设计得非常好，**几乎没有缺点可言**，可用于 `100%` 替代老的日期时间 `API`。

## 2.Java8日期处理

`Java 8` 推出了全新的日期时间 `API`，在教程中我们将通过一些简单的实例来学习如何使用新API。

`Java` 处理日期、日历和时间的方式一直为社区所诟病，将 `java.util.Date` 设定为可变类型，以及 `SimpleDateFormat` 的非线程安全使其应用非常受限。

新 `API` 基于 `ISO` 标准日历系统，`java.time` 包下的所有类都是不可变类型而且线程安全。

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210505154419.webp)

### 2.1.示例1:Java 8中获取今天的日期

`Java 8` 中的 `LocalDate` 用于表示当天日期。和 `java.util.Date` 不同，它只有日期，不包含时间。当你仅需要表示日期时就用这个类。

```
package com.shxt.demo02;

import java.time.LocalDate;

public class Demo01 {
    public static void main(String[] args) {
        LocalDate today = LocalDate.now();
        System.out.println("今天的日期:"+today);
    }
}
```

### 2.2.示例2:Java 8中获取年、月、日信息

```
package com.shxt.demo02;

import java.time.LocalDate;

public class Demo02 {
    public static void main(String[] args) {
        LocalDate today = LocalDate.now();
        int year = today.getYear();
        int month = today.getMonthValue();
        int day = today.getDayOfMonth();

        System.out.println("year:"+year);
        System.out.println("month:"+month);
        System.out.println("day:"+day);

    }
}
```

### 2.3.示例3:Java 8中处理特定日期

我们通过静态工厂方法 `now()` 非常容易地创建了当天日期，你还可以调用另一个有用的工厂方法 `LocalDate.of()` 创建任意日期， 该方法需要传入年、月、日做参数，返回对应的 `LocalDate` 实例。这个方法的好处是没再犯老API的设计错误，比如年度起始于1900，月份是从0开 始等等。

```
package com.shxt.demo02;

import java.time.LocalDate;

public class Demo03 {
    public static void main(String[] args) {
        LocalDate date = LocalDate.of(2018,2,6);
        System.out.println("自定义日期:"+date);
    }
}
```

### 2.4.示例4:Java 8中判断两个日期是否相等

```
package com.shxt.demo02;

import java.time.LocalDate;

public class Demo04 {
    public static void main(String[] args) {
        LocalDate date1 = LocalDate.now();

        LocalDate date2 = LocalDate.of(2018,2,5);

        if(date1.equals(date2)){
            System.out.println("时间相等");
        }else{
            System.out.println("时间不等");
        }

    }
}
```

### 2.5.示例5:Java 8中检查像生日这种周期性事件

```
package com.shxt.demo02;

import java.time.LocalDate;
import java.time.MonthDay;

public class Demo05 {
    public static void main(String[] args) {
        LocalDate date1 = LocalDate.now();

        LocalDate date2 = LocalDate.of(2018,2,6);
        MonthDay birthday = MonthDay.of(date2.getMonth(),date2.getDayOfMonth());
        MonthDay currentMonthDay = MonthDay.from(date1);

        if(currentMonthDay.equals(birthday)){
            System.out.println("是你的生日");
        }else{
            System.out.println("你的生日还没有到");
        }

    }
}
```

只要当天的日期和生日匹配，无论是哪一年都会打印出祝贺信息。你可以把程序整合进系统时钟，看看生日时是否会受到提醒，或者写一个单元测试来检测代码是否运行正确。

### 2.6.示例6:Java 8中获取当前时间

```
package com.shxt.demo02;

import java.time.LocalTime;

public class Demo06 {
    public static void main(String[] args) {
        LocalTime time = LocalTime.now();
        System.out.println("获取当前的时间,不含有日期:"+time);

    }
}
```

可以看到当前时间就只包含时间信息，没有日期

### 2.7.示例7:Java 8中获取当前时

通过增加小时、分、秒来计算将来的时间很常见。`Java 8` 除了不变类型和线程安全的好处之外，还提供了更好的 `plusHours()` 方法替换 `add()`，并且是兼容的。注意，这些方法返回一个全新的 `LocalTime` 实例，由于其不可变性，返回后一定要用变量赋值。

```
package com.shxt.demo02;

import java.time.LocalTime;

public class Demo07 {
    public static void main(String[] args) {
        LocalTime time = LocalTime.now();
        LocalTime newTime = time.plusHours(3);
        System.out.println("三个小时后的时间为:"+newTime);

    }
}
```

### 2.8.示例8:Java 8如何计算一周后的日期

和上个例子计算 `3` 小时以后的时间类似，这个例子会计算一周后的日期。`LocalDate` 日期不包含时间信息，它的 `plus()` 方法用来增加天、周、月，`ChronoUnit` 类声明了这些时间单位。由于 `LocalDate` 也是不变类型，返回后一定要用变量赋值。

```
package com.shxt.demo02;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class Demo08 {
    public static void main(String[] args) {
        LocalDate today = LocalDate.now();
        System.out.println("今天的日期为:"+today);
        LocalDate nextWeek = today.plus(1, ChronoUnit.WEEKS);
        System.out.println("一周后的日期为:"+nextWeek);

    }
}
```

可以看到新日期离当天日期是 `7` 天，也就是一周。你可以用同样的方法增加1个月、1年、1小时、1分钟甚至一个世纪，更多选项可以查看 `Java 8 API` 中的 `ChronoUnit` 类

### 2.9.示例9:Java 8计算一年前或一年后的日期

利用 `minus()` 方法计算一年前的日期

```
package com.shxt.demo02;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class Demo09 {
    public static void main(String[] args) {
        LocalDate today = LocalDate.now();

        LocalDate previousYear = today.minus(1, ChronoUnit.YEARS);
        System.out.println("一年前的日期 : " + previousYear);

        LocalDate nextYear = today.plus(1, ChronoUnit.YEARS);
        System.out.println("一年后的日期:"+nextYear);

    }
}
```

### 2.10.示例10:Java 8的Clock时钟

`Java 8` 增加了一个 `Clock` 时钟类用于获取当时的时间戳，或当前时区下的日期时间信息。以前用到 `System.currentTimeInMillis()` 和 `TimeZone.getDefault()` 的地方都可用 `Clock` 替换。

```
package com.shxt.demo02;

import java.time.Clock;

public class Demo10 {
    public static void main(String[] args) {
        // Returns the current time based on your system clock and set to UTC.
        Clock clock = Clock.systemUTC();
        System.out.println("Clock : " + clock.millis());

        // Returns time based on system clock zone
        Clock defaultClock = Clock.systemDefaultZone();
        System.out.println("Clock : " + defaultClock.millis());

    }
}
```

### 2.11.示例11:如何用Java判断日期是早于还是晚于另一个日期

另一个工作中常见的操作就是如何判断给定的一个日期是大于某天还是小于某天？在 `Java 8` 中，`LocalDate` 类有两类方法 `isBefore()` 和 `isAfter()` 用于比较日期。调用 `isBefore()` 方法时，如果给定日期小于当前日期则返回 `true`。

```
package com.shxt.demo02;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;


public class Demo11 {
    public static void main(String[] args) {
        LocalDate today = LocalDate.now();

        LocalDate tomorrow = LocalDate.of(2018,2,6);
        if(tomorrow.isAfter(today)){
            System.out.println("之后的日期:"+tomorrow);
        }

        LocalDate yesterday = today.minus(1, ChronoUnit.DAYS);
        if(yesterday.isBefore(today)){
            System.out.println("之前的日期:"+yesterday);
        }
    }
}
```

### 2.12.示例12:Java 8中处理时区

`Java 8` 不仅分离了日期和时间，也把时区分离出来了。现在有一系列单独的类如 `ZoneId` 来处理特定时区，`ZoneDateTime` 类来表示某时区下的时间。这在 `Java 8` 以前都是 `GregorianCalendar` 类来做的。下面这个例子展示了如何把本时区的时间转换成另一个时区的时间。

```
package com.shxt.demo02;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class Demo12 {
    public static void main(String[] args) {
        // Date and time with timezone in Java 8
        ZoneId america = ZoneId.of("America/New_York");
        LocalDateTime localtDateAndTime = LocalDateTime.now();
        ZonedDateTime dateAndTimeInNewYork  = ZonedDateTime.of(localtDateAndTime, america );
        System.out.println("Current date and time in a particular timezone : " + dateAndTimeInNewYork);
    }
}
```

### 2.13.示例13:如何表示信用卡到期这类固定日期，答案就在YearMont

与 `MonthDay` 检查重复事件的例子相似，`YearMonth` 是另一个组合类，用于表示信用卡到期日、FD到期日、期货期权到期日等。还可以用这个类得到 当月共有多少天，`YearMonth` 实例的 `lengthOfMonth()` 方法可以返回当月的天数，在判断2月有28天还是29天时非常有用。

```
package com.shxt.demo02;

import java.time.*;

public class Demo13 {
    public static void main(String[] args) {
        YearMonth currentYearMonth = YearMonth.now();
        System.out.printf("Days in month year %s: %d%n", currentYearMonth, currentYearMonth.lengthOfMonth());
        YearMonth creditCardExpiry = YearMonth.of(2019, Month.FEBRUARY);
        System.out.printf("Your credit card expires on %s %n", creditCardExpiry);
    }
}
```

### 2.14.示例14:如何在Java 8中检查闰年

```
package com.shxt.demo02;

import java.time.LocalDate;

public class Demo14 {
    public static void main(String[] args) {
        LocalDate today = LocalDate.now();
        if(today.isLeapYear()){
            System.out.println("This year is Leap year");
        }else {
            System.out.println("2018 is not a Leap year");
        }

    }
}
```

### 2.15.示例15:计算两个日期之间的天数和月数

有一个常见日期操作是计算两个日期之间的天数、周数或月数。在 `Java 8` 中可以用 `java.time.Period` 类来做计算。

下面这个例子中，我们计算了当天和将来某一天之间的月数。

```
package com.shxt.demo02;

import java.time.LocalDate;
import java.time.Period;

public class Demo15 {
    public static void main(String[] args) {
        LocalDate today = LocalDate.now();

        LocalDate java8Release = LocalDate.of(2018, 12, 14);

        Period periodToNextJavaRelease = Period.between(today, java8Release);
        System.out.println("Months left between today and Java 8 release : "
                + periodToNextJavaRelease.getMonths() );


    }
}
```

### 2.16.示例16:在Java 8中获取当前的时间戳

`Instant` 类有一个静态工厂方法 `now()` 会返回当前的时间戳，如下所示：

```
package com.shxt.demo02;

import java.time.Instant;

public class Demo16 {
    public static void main(String[] args) {
        Instant timestamp = Instant.now();
        System.out.println("What is value of this instant " + timestamp.toEpochMilli());
    }
}
```

时间戳信息里同时包含了日期和时间，这和 `java.util.Date` 很像。实际上 `Instant` 类确实等同于 `Java 8` 之前的 `Date` 类，你可以使用 `Date` 类和 `Instant` 类各自的转换方法互相转换，例如：`Date.from(Instant)` 将 `Instant` 转换成 `java.util.Date`，`Date.toInstant()` 则是将 `Date` 类转换成 `Instant` 类。

### 2.17.示例17:Java 8中如何使用预定义的格式化工具去解析或格式化日期

```
package com.shxt.demo02;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class Demo17 {
    public static void main(String[] args) {
        String dayAfterTommorrow = "20180205";
        LocalDate formatted = LocalDate.parse(dayAfterTommorrow,
                DateTimeFormatter.BASIC_ISO_DATE);
        System.out.println(dayAfterTommorrow+"  格式化后的日期为:  "+formatted);
    }
}
```

### 2.18.示例18:字符串互转日期类型

```
package com.shxt.demo02;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Demo18 {
    public static void main(String[] args) {
        LocalDateTime date = LocalDateTime.now();

        DateTimeFormatter format1 = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        //日期转字符串
        String str = date.format(format1);

        System.out.println("日期转换为字符串:"+str);

        DateTimeFormatter format2 = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        //字符串转日期
        LocalDate date2 = LocalDate.parse(str,format2);
        System.out.println("日期类型:"+date2);

    }
}
```