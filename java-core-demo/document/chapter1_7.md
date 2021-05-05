[toc]



# Java 控制语句

## 1. 选择语句

### 1.1. if 语句

`if` 语句会判断括号中的条件是否成立，如果成立则执行 `if` 语句中的代码块，否则跳过代码块继续执行。

**语法**

```
if(布尔表达式) {
   //如果布尔表达式为true将执行的语句
}
```

**示例**

```
public class IfDemo {
    public static void main(String args[]) {
        int x = 10;
        if (x < 20) {
            System.out.print("这是 if 语句");
        }
    }
}
// output:
// 这是 if 语句
```

### 1.2. if...else 语句

`if` 语句后面可以跟 `else` 语句，当 `if` 语句的布尔表达式值为 `false` 时，`else` 语句块会被执行。

**语法**

```
if(布尔表达式) {
   //如果布尔表达式的值为true
} else {
   //如果布尔表达式的值为false
}
```

**示例**

```
public class IfElseDemo {
    public static void main(String args[]) {
        int x = 30;
        if (x < 20) {
            System.out.print("这是 if 语句");
        } else {
            System.out.print("这是 else 语句");
        }
    }
}
// output:
// 这是 else 语句
```

### 1.3. if...else if...else 语句

- `if` 语句至多有 1 个 `else` 语句，`else` 语句在所有的 `else if` 语句之后。
- `If` 语句可以有若干个 `else if` 语句，它们必须在 `else` 语句之前。
- 一旦其中一个 `else if` 语句检测为 `true`，其他的 `else if` 以及 `else` 语句都将跳过执行。

**语法**

```
if (布尔表达式 1) {
   //如果布尔表达式 1的值为true执行代码
} else if (布尔表达式 2) {
   //如果布尔表达式 2的值为true执行代码
} else if (布尔表达式 3) {
   //如果布尔表达式 3的值为true执行代码
} else {
   //如果以上布尔表达式都不为true执行代码
}
```

**示例**

```
public class IfElseifElseDemo {
    public static void main(String args[]) {
        int x = 3;

        if (x == 1) {
            System.out.print("Value of X is 1");
        } else if (x == 2) {
            System.out.print("Value of X is 2");
        } else if (x == 3) {
            System.out.print("Value of X is 3");
        } else {
            System.out.print("This is else statement");
        }
    }
}
// output:
// Value of X is 3
```

### 1.4. 嵌套的 if…else 语句

使用嵌套的 `if else` 语句是合法的。也就是说你可以在另一个 `if` 或者 `else if` 语句中使用 `if` 或者 `else if` 语句。

**语法**

```
if (布尔表达式 1) {
   ////如果布尔表达式 1的值为true执行代码
   if (布尔表达式 2) {
      ////如果布尔表达式 2的值为true执行代码
   }
}
```

**示例**

```
public class IfNestDemo {
    public static void main(String args[]) {
        int x = 30;
        int y = 10;

        if (x == 30) {
            if (y == 10) {
                System.out.print("X = 30 and Y = 10");
            }
        }
    }
}
// output:
// X = 30 and Y = 10
```

### 1.5. switch 语句

`switch` 语句判断一个变量与一系列值中某个值是否相等，每个值称为一个分支。

`switch` 语句有如下规则：

- `switch` 语句中的变量类型只能为 `byte`、`short`、`int`、`char` 或者 `String`。
- `switch` 语句可以拥有多个 `case` 语句。每个 `case` 后面跟一个要比较的值和冒号。
- `case` 语句中的值的数据类型必须与变量的数据类型相同，而且只能是常量或者字面常量。
- 当变量的值与 `case` 语句的值相等时，那么 `case` 语句之后的语句开始执行，直到 `break` 语句出现才会跳出 `switch` 语句。
- 当遇到 `break` 语句时，`switch` 语句终止。程序跳转到 `switch` 语句后面的语句执行。`case` 语句不必须要包含 `break` 语句。如果没有 `break` 语句出现，程序会继续执行下一条 `case` 语句，直到出现 `break` 语句。
- `switch` 语句可以包含一个 `default` 分支，该分支必须是 `switch` 语句的最后一个分支。`default` 在没有 `case` 语句的值和变量值相等的时候执行。`default` 分支不需要 `break` 语句。

**语法**

```
switch(expression){
    case value :
       //语句
       break; //可选
    case value :
       //语句
       break; //可选
    //你可以有任意数量的case语句
    default : //可选
       //语句
       break; //可选，但一般建议加上
}
```

**示例**

```
public class SwitchDemo {
    public static void main(String args[]) {
        char grade = 'C';

        switch (grade) {
        case 'A':
            System.out.println("Excellent!");
            break;
        case 'B':
        case 'C':
            System.out.println("Well done");
            break;
        case 'D':
            System.out.println("You passed");
        case 'F':
            System.out.println("Better try again");
            break;
        default:
            System.out.println("Invalid grade");
            break;
        }
        System.out.println("Your grade is " + grade);
    }
}
// output:
// Well done
// Your grade is C
```

## 2. 循环语句

### 2.1. while 循环

只要布尔表达式为 `true`，`while` 循环体会一直执行下去。

**语法**

```
while( 布尔表达式 ) {
    //循环内容
}
```

**示例**

```
public class WhileDemo {
    public static void main(String args[]) {
        int x = 10;
        while (x < 20) {
            System.out.print("value of x : " + x);
            x++;
            System.out.print("\n");
        }
    }
}
// output:
// value of x : 10
// value of x : 11
// value of x : 12
// value of x : 13
// value of x : 14
// value of x : 15
// value of x : 16
// value of x : 17
// value of x : 18
// value of x : 19
```

### 2.2. do while 循环

对于 `while` 语句而言，如果不满足条件，则不能进入循环。但有时候我们需要即使不满足条件，也至少执行一次。

`do while` 循环和 `while` 循环相似，不同的是，`do while` 循环至少会执行一次。

**语法**

```
do {
    //代码语句
} while (布尔表达式);
```

布尔表达式在循环体的后面，所以语句块在检测布尔表达式之前已经执行了。 如果布尔表达式的值为 true，则语句块一直执行，直到布尔表达式的值为 false。

**示例**

```
public class DoWhileDemo {
    public static void main(String args[]) {
        int x = 10;

        do {
            System.out.print("value of x : " + x);
            x++;
            System.out.print("\n");
        } while (x < 20);
    }
}
// output:
// value of x:10
// value of x:11
// value of x:12
// value of x:13
// value of x:14
// value of x:15
// value of x:16
// value of x:17
// value of x:18
// value of x:19
```

### 2.3. for 循环

虽然所有循环结构都可以用 `while` 或者 `do while` 表示，但 Java 提供了另一种语句 —— `for` 循环，使一些循环结构变得更加简单。 `for` 循环执行的次数是在执行前就确定的。

**语法**

```
for (初始化; 布尔表达式; 更新) {
    //代码语句
}
```

- 最先执行初始化步骤。可以声明一种类型，但可初始化一个或多个循环控制变量，也可以是空语句。
- 然后，检测布尔表达式的值。如果为 true，循环体被执行。如果为 false，循环终止，开始执行循环体后面的语句。
- 执行一次循环后，更新循环控制变量。
- 再次检测布尔表达式。循环执行上面的过程。

**示例**

```
public class ForDemo {
    public static void main(String args[]) {
        for (int x = 10; x < 20; x = x + 1) {
            System.out.print("value of x : " + x);
            System.out.print("\n");
        }
    }
}
// output:
// value of x : 10
// value of x : 11
// value of x : 12
// value of x : 13
// value of x : 14
// value of x : 15
// value of x : 16
// value of x : 17
// value of x : 18
// value of x : 19
```

### 2.4. foreach 循环

Java5 引入了一种主要用于数组的增强型 for 循环。

**语法**

```
for (声明语句 : 表达式) {
    //代码句子
}
```

**声明语句**：声明新的局部变量，该变量的类型必须和数组元素的类型匹配。其作用域限定在循环语句块，其值与此时数组元素的值相等。

**表达式**：表达式是要访问的数组名，或者是返回值为数组的方法。

**示例**

```
public class ForeachDemo {
    public static void main(String args[]) {
        int[] numbers = { 10, 20, 30, 40, 50 };

        for (int x : numbers) {
            System.out.print(x);
            System.out.print(",");
        }

        System.out.print("\n");
        String[] names = { "James", "Larry", "Tom", "Lacy" };

        for (String name : names) {
            System.out.print(name);
            System.out.print(",");
        }
    }
}
// output:
// 10,20,30,40,50,
// James,Larry,Tom,Lacy,
```

## 3. 中断语句

### 3.1. break 关键字

`break` 主要用在循环语句或者 `switch` 语句中，用来跳出整个语句块。

`break` 跳出最里层的循环，并且继续执行该循环下面的语句。

**示例**

```
public class BreakDemo {
    public static void main(String args[]) {
        int[] numbers = { 10, 20, 30, 40, 50 };

        for (int x : numbers) {
            if (x == 30) {
                break;
            }
            System.out.print(x);
            System.out.print("\n");
        }

        System.out.println("break 示例结束");
    }
}
// output:
// 10
// 20
// break 示例结束
```

### 3.2. continue 关键字

`continue` 适用于任何循环控制结构中。作用是让程序立刻跳转到下一次循环的迭代。在 `for` 循环中，`continue` 语句使程序立即跳转到更新语句。在 `while` 或者 `do while` 循环中，程序立即跳转到布尔表达式的判断语句。

**示例**

```
public class ContinueDemo {
    public static void main(String args[]) {
        int[] numbers = { 10, 20, 30, 40, 50 };

        for (int x : numbers) {
            if (x == 30) {
                continue;
            }
            System.out.print(x);
            System.out.print("\n");
        }
    }
}
// output:
// 10
// 20
// 40
// 50
```

### 3.3. return 关键字

跳出整个函数体，函数体后面的部分不再执行。

示例

```
public class ReturnDemo {
    public static void main(String args[]) {
        int[] numbers = { 10, 20, 30, 40, 50 };

        for (int x : numbers) {
            if (x == 30) {
                return;
            }
            System.out.print(x);
            System.out.print("\n");
        }

        System.out.println("return 示例结束");
    }
}
// output:
// 10
// 20
```

> 🔔 注意：请仔细体会一下 `return` 和 `break` 的区别。

## 4. 最佳实践

- 选择分支特别多的情况下，`switch` 语句优于 `if...else if...else` 语句。
- `switch` 语句不要吝啬使用 `default`。
- `switch` 语句中的 `default` 要放在最后。
- `foreach` 循环优先于传统的 `for` 循环
- 不要循环遍历容器元素，然后删除特定元素。正确姿势应该是遍历容器的迭代器（`Iterator`），删除元素。

## X.常见问题

### X.1.Java 中的 Switch 都支持 String 了，为什么不支持 long？

我们知道 Java Switch 支持byte、short、int 类型，在 JDK 1.5 时，支持了枚举类型，在 JDK 1.7 时，又支持了 String类型。那么它为什么就不能支持 long 类型呢，明明它跟 byte、short、int 一样都是数值型，它又是咋支持 String 类型的呢？

#### X.1.1.结论

不卖关子，先说结论：

**switch 底层是使用 int 型 来进行判断的，即使是枚举、String类型，最终也是转变成 int 型。由于 long 型表示范围大于 int 型，因此不支持 long 类型。**

下面详细介绍下各个类型是如何被转变成 int 类型的，使用的编译命令为 javac，反编译网站为：http://javare.cn

#### X.1.2.枚举类型是咋变成 int 类型的？

在没有实验之前，我想当然的认为它是不是根据枚举的 int 型字段来计算的（因为一般枚举都是一个int型，一个string型），但是转念一想，万一枚举没有 int 型字段呢，万一有多个 int 型字段呢，所以肯定不是这样的，下面看实验吧。

定义两个枚举类，一个枚举类有一个int型属性，一个string型属性，另外一个枚举类只有一个string属性：

```
public enum SexEnum {  
    MALE(1, "男"),  
    FEMALE(0, "女");  
  
    private int type;  
  
    private String name;  
  
    SexEnum(int type, String name) {  
        this.type = type;  
        this.name = name;  
    }  
}  
public enum Sex1Enum {  
    MALE("男"),  
    FEMALE("女");  
    private String name;  
  
    Sex1Enum(String name) {  
        this.name = name;  
    }  
}  
```

然后编写一个测试类，并且让两个枚举 switch 的 FEMALE 和 MALE 对应的返回值不同：

```
public class SwitchTest {  
    public int enumSwitch(SexEnum sex) {  
        switch (sex) {  
            case MALE:  
                return 1;  
            case FEMALE:  
                return 2;  
            default:  
                return 3;  
        }  
    }  
  
    public int enum1Switch(Sex1Enum sex) {  
        switch (sex) {  
            case FEMALE:  
                return 1;  
            case MALE:  
                return 2;  
            default:  
                return 3;  
        }  
    }  
}  
```

将这几个类反编译下：

```
// SexEnum.class  
public enum SexEnum {  
  
   MALE(1, "鐢�"),  
   FEMALE(0, "濂�");  
   private int type;  
   private String name;  
   // $FF: synthetic field  
   private static final SexEnum[] $VALUES = new SexEnum[]{MALE, FEMALE};  
  
  
   private SexEnum(int var3, String var4) {  
      this.type = var3;  
      this.name = var4;  
   }  
  
}  
  
// Sex1Enum.class  
public enum Sex1Enum {  
  
   MALE("鐢�"),  
   FEMALE("濂�");  
   private String name;  
   // $FF: synthetic field  
   private static final Sex1Enum[] $VALUES = new Sex1Enum[]{MALE, FEMALE};  
  
  
   private Sex1Enum(String var3) {  
      this.name = var3;  
   }  
  
}  
```

反编译这两个枚举类，发现其中多了一个 $VALUES 数组，内部包含了所有的枚举值。继续反编译测试类：

```
// SwitchTest$1.class  
import com.example.express.test.Sex1Enum;  
import com.example.express.test.SexEnum;  
  
// $FF: synthetic class  
class SwitchTest$1 {  
  
   // $FF: synthetic field  
   static final int[] $SwitchMap$com$example$express$test$SexEnum;  
   // $FF: synthetic field  
   static final int[] $SwitchMap$com$example$express$test$Sex1Enum = new int[Sex1Enum.values().length];  
  
  
   static {  
      try {  
         $SwitchMap$com$example$express$test$Sex1Enum[Sex1Enum.FEMALE.ordinal()] = 1;  
      } catch (NoSuchFieldError var4) {  
         ;  
      }  
  
      try {  
         $SwitchMap$com$example$express$test$Sex1Enum[Sex1Enum.MALE.ordinal()] = 2;  
      } catch (NoSuchFieldError var3) {  
         ;  
      }  
  
      $SwitchMap$com$example$express$test$SexEnum = new int[SexEnum.values().length];  
  
      try {  
         $SwitchMap$com$example$express$test$SexEnum[SexEnum.MALE.ordinal()] = 1;  
      } catch (NoSuchFieldError var2) {  
         ;  
      }  
  
      try {  
         $SwitchMap$com$example$express$test$SexEnum[SexEnum.FEMALE.ordinal()] = 2;  
      } catch (NoSuchFieldError var1) {  
         ;  
      }  
  
   }  
}  
```

首先生成了一个名为 SwitchTest$1.java 的链接类，里面定义了两个枚举数组，这两个数组元素添加的顺序完全和测试类中 switch 类调用的顺序一致。

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210505151357.webp)

枚举元素在数组中的下标由 ordinal() 函数决定，该方法就是返回枚举元素在枚举类中的序号。

这里我们其实就已经知道了，在 switch 语句中，是根据枚举元素在枚举中的序号来转变成 int 型的。最后再看下测试类的反编译结果验证下：

```
// SwitchTest.class  
import com.example.express.test.Sex1Enum;  
import com.example.express.test.SexEnum;  
import com.example.express.test.SwitchTest.1;  
  
public class SwitchTest {  
   public int enumSwitch(SexEnum var1) {  
      switch(1.$SwitchMap$com$example$express$test$SexEnum[var1.ordinal()]) {  
      case 1:  
         return 1;  
      case 2:  
         return 2;  
      default:  
         return 3;  
      }  
   }  
  
   public int enum1Switch(Sex1Enum var1) {  
      switch(1.$SwitchMap$com$example$express$test$Sex1Enum[var1.ordinal()]) {  
      case 1:  
         return 1;  
      case 2:  
         return 2;  
      default:  
         return 3;  
      }  
   }  
}  
```

#### X.1.3.String 类型是咋变成 int 类型的？

首先我们先知道 char 类型是如何变成 int 类型的，很简单，是 ASCII 码，例如存在 switch 语句：

```
public int charSwitch(char c) {  
    switch (c) {  
        case 'a':  
            return 1;  
        case 'b':  
            return 2;  
        default:  
            return Integer.MAX_VALUE;  
    }  
}  
```

反编译结果：

```
public int charSwitch(char var1) {  
    switch(var1) {  
        case 97:  
            return 1;  
        case 98:  
            return 2;  
        default:  
            return Integer.MAX_VALUE;  
    }  
}  
```

那么对于 String 来说，利用的就是 hashCode() 函数了，但是 两个不同的字符串 hashCode() 是有可能相等的，这时候就得靠 equals() 函数了，例如存在 switch 语句：

```
public int stringSwitch(String ss) {  
    switch (ss) {  
        case "ABCDEa123abc":  
            return 1;  
        case "ABCDFB123abc":  
            return 2;  
        case "helloWorld":  
            return 3;  
        default:  
            return Integer.MAX_VALUE;  
    }  
}  
```

其中字符串 ABCDEa123abc 和 ABCDFB123abc 的 hashCode 是相等的，反编译结果为：

```
public int stringSwitch(String var1) {  
   byte var3 = -1;  
   switch(var1.hashCode()) {  
       case -1554135584:  
          if(var1.equals("helloWorld")) {  
             var3 = 2;  
          }  
          break;  
       case 165374702:  
          if(var1.equals("ABCDFB123abc")) {  
             var3 = 1;  
          } else if(var1.equals("ABCDEa123abc")) {  
             var3 = 0;  
          }  
   }  
  
   switch(var3) {  
       case 0:  
          return 1;  
       case 1:  
          return 2;  
       case 2:  
          return 3;  
       default:  
          return Integer.MAX_VALUE;  
   }  
}  
```

可以看到它引入了局部变量 var3，对于 hashCode 相等情况通过 equals() 方法判断，最后再判断 var3 的值。

#### X.1.4.它们的包装类型支持吗？

这里以 Integer 类型为例，Character 和 Byte 同理，例如存在 switch 语句：

```
public int integerSwitch(Integer c) {  
    switch (c) {  
        case 1:  
            return 1;  
        case 2:  
            return 2;  
    }  
    return -1;  
}  
```

反编译结果为：

```
public int integerSwitch(Integer var1) {  
    switch(var1.intValue()) {  
        case 1:  
            return 1;  
        case 2:  
            return 2;  
        default:  
            return -1;  
    }  
}  
```

可以看到，是支持包装类型的，通过自动拆箱解决。

那万一包装类型是 NULL 咋办，首先我们知道 swtich 的 case 是不给加 null 的，编译都通不过，那如果传 null 呢？

答案是 NPE，毕竟实际还是包装类型的拆箱，自然就报空指针了。

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210505151348.webp)











