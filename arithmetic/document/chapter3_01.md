[toc]



# 原码，反码，补码

### 1.计算方式

原码：是计算机机器数中最简单的一种形式，数值位就是真值的绝对值，即二进制表示的格式；其中最高位是符号位，符号位中0表示正数，1表示负数。

反码：正数的反码和原码一样；负数的反码是它原码除符号位外，其他位按位取反。

补码：正数的补码和原码一样；负数的补码等于它反码+1，或者说是等于它的原码自低位向高位，尾数的第一个‘1’及其右边的‘0’保持不变，左边的各位按位取反，符号位不变。

### 2.为什么这样要计算呢？

由于计算机的硬件设计决定，其本质都是以二进制码来存储和运算的；根据冯~诺依曼提出的经典计算机体系结构框架。一台计算机由运算器，控制器，存储器，输入和输出设备组成。其中运算器，只有加法运算器，没有减法运算器（据说一开始是有的，后来由于减法器硬件开销太大，被废了 ）。

因此在计算机中是没有减法的，只有加法运算。即减一个数相当于加上一个负数。

所以需要设计一种新的计算规则来实现计算机的加法运算；注意我们需要的是一个新的规则来适配计算机的加法运算，使其最终结果和我们现有的计算规则的结果一样！！

下面我们以4位的二进制数为例做设计。

#### 2.1.原码

原码：是最简单的机器数表示法。用最高位表示符号位，‘1’表示负号，‘0’表示正号。其他位存放该数的二进制的绝对值。

下面这个以原码的规则计算机中存储的数据

| /    | 正数 | /    | 负数 |
| :--- | :--- | :--- | :--- |
| 0    | 0000 | -0   | 1000 |
| 1    | 0001 | -1   | 1001 |
| 2    | 0010 | -2   | 1010 |
| 3    | 0011 | -3   | 1011 |
| 4    | 0100 | -4   | 1100 |
| 5    | 0101 | -5   | 1101 |
| 6    | 0110 | -6   | 1110 |
| 7    | 0111 | -7   | 1111 |

这种设计方式很简单，虽然出现了-0和+0，但是还能接受；下面我们开始做运算：

```text
	0001+0010=0011  ==>> 1+2=3      没得问题
	0000+1000=1000  ==>> 0+(-0)=-0  可以接受
	0001+1001=1010  ==>> 1+(-1)=-2  哦，这个...
```

这种方式在正数之间进行没得问题，但是有负数的运算就不行了，看来原码干不了这个活啊；于是反码来了。

#### 2.2.反码

我们知道，在十进制中一个数和其相反数相加等于0，对应的减法也可定义为一个数加上另一个数的相反数；基于这一点反码的设计思路出来，那就是定义二进制的相反数求法。

直接按十进制的套用明显不行，那么让它的原码除符号位外，按位取反；由于正数使用原码进行计算没得问题，就暂时不动它，只让其适用于负数。得到如下的结果：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/arithmetic-design/20210630214049.png)

现在再来计算：

```text
	0001+1110=1111  ==>> 1+(-1)=-0  现在正确了
```

> 注意现在计算机中实际存储的是反码了。

```text
	1110+1101=1011  ==>> -1+(-2)=--4  哦，这个...
```

这种方式好像在计算负数+负数的时候不得行啊。

不过我们已经解决了相反数相加的问题了，对于负数我们直接让其符号位固定为1即可达到正确结果。

```text
0001+1110=1111 ==>> 1+(-1)=-0  这个看着怪别扭的
```

这种负0看着怪别扭的，同时需要在负数+负数的时候还要做个符号位强制位1的操作，太麻烦了，要想个办法偷懒（平时编程中也应当有个偷懒的思维 hhh）; 于是补码出现了。

#### 2.3.补码

由于正数是没得问题的，不做修改，所以正数的补码等于他的原码；负数的补码等于反码+1。（这只是一种算补码的方式，多数书对于补码就是这句话）

> 负数的补码等于他的原码自低位向高位，尾数的第一个‘1’及其右边的‘0’保持不变，左边的各位按位取反，符号位不变。

想想当年那些计算机学家（高级专业偷懒户），并不会心血来潮的把反码+1就定义为补码。下面来看看其设计原因。

由于使用十进制的计算方式已经不能满足二进制的需求了，因此我们需要跳出来，重新找灵感。

> 生活中的时钟有12个刻度，如果时针现在在10点的位置，那么什么时候会停止在8点钟的位置呢？

这个很简单再过10个小时，或者2个小时前，那么得到如下公式：

```text
10-2=8=10+10 时间超过12就会重新开始，这种称为模。
```

在时钟运算中，减去一个数，其实就相当于加上另外一个数（这个数与减数相加正好等于12，也称为同余数）

通过时钟的例子可以发现最终转换后的计算表达式是2个正数相加，而从前面的结论中2个正数进行运算其符号位并不是必须的，那么现在设计补码时我们暂时就将符号位去掉。

> 这也是为什么正数的符号位是0，负数的符号位是1的原因；因为如果正数的符号位是1的话，由于其符号位被忽略，当其参与运算时就会发生进位的情况，而使用0就不会有这种情况。

- 同余数

现在就是需要求这个同余数的问题，根据数学中对同余数的定义：

```text
    两个整数a，b，若它们除以整数m所得的余数相等，则称a，b对于模m同余。
```

例如，当m=12时，3跟15是同余的，因为3mod12=3=15mod12,对于同余，有如下结论:

```text
    a，b是关于m同余的，当且仅当，二者相差m的整数倍，
    a−b=k×m, with k=……−2,−1,0,1,2,……
    即，
    a=b+k×m, with k=……−2,−1,0,1,2,……
    一个数x加a对m取余，等于x加a的同余b对m取余，即，
    (x+a) mod m=(x+b) mod m.
    由1.易知2.是成立的。
```

将参数带入：

```text
    3-15=-1*12  === 3=15+（-1）*12
    (x+3)%12=(x+15)%12
```

将上面的表达式在简化下：

```text
    若：a=b+m，则 (x+a) mod m = (x+b) mod m
```

那么现在该如何来求这个同余数呢？也就是找到负数补码的求法。

若b为一个负数，表达式可以转为类似如下：

```text
a-b=m  ===>> 设c=-b，则：a+c=m
```

现在要 求a；通过上面的推导，我们在运算时可以减法转换为加法，相当于将负数转换为了正数，那么符合位也就没有用了；因此我们新设计的补码就可以不要符合位(因为都是正数的运算了)。 

参考时钟的案例，我们最终其实只关注二进制位数能够表示的数(因为多余的位数也没地方存储)，即时钟刻度的最大值就是m，也就是二进制位数表达的最大值，例如4位的最大值就是16。 

所以求a即计算m-c就是求其二进制的另一半。而c的另一半就是把二进制位上的0变1、1变0即取反，它们相加后全是1，而m是其最大值+1,因此还需要加1才行。而这就是补码的求法。这个计算的方法还可以参考时钟的情况。下面来实际计算验证一下：

示例1：

```text
    3-5=-2=(3+11)%16
    
    0011(3的补码) + 1011(-5的补码，11的原码) = 1110
    1110是一个补码(此时最高位就是符号位)转为原码：1010 即为-2
```

示例2：

```text
    5-3=2=(5+13)%16
    
    0101(5的补码) + 1101(-3的补码，13的原码) = 1 0010
    由于总共只有4位，产生的进位会被丢掉，因此最终的补码为 0010 转为原码：0010即为2
```

示例3：

```text
0001+1111=1 0000 ==>> 1+(-1)=0  这样之前的-0问题也解决了
```

最终使用补码满足了我们的要求，因此在计算机中实际存储的是补码，进行操作的时候也是通过补码来进行操作。