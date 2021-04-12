[toc]



# Linux 特殊符号

## ~ 主目录
这个波浪号 ~ 指的是主目录，也就是我们用户的个人目录，无论你身在何方，输入 cd ~ 它将带你回家！
```
cd ~
```
![Image](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/linux-demo/20210411180541.png)

更高端的玩法就是在它后面加上具体的路径，直接定位到家目录中的指定位置，是不是很方便呢？

```
cd ~/work/archive
```
![Image [2]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/linux-demo/20210411180552.png)

## . 当前目录
英文句号 . 代表当前目录，我们来看一下当前目录下的全部文件：
```
ls -al
```
![Image [3]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/linux-demo/20210411180613.png)

红圈里的 . 就是指当前目录，不过这没什么意义，我们更多的是在命令中使用它，如下：

```
./script.sh
```
这样做是在告诉 bash 只要在当前目录中查找并执行 script.sh 文件就好了，不用在路径中找了。

![Image [4]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/linux-demo/20210411180625.png)

## .. 父目录
两个英文句号 .. 代表父目录，也就是当前目录的上一级目录。假设我们要回到上一级目录：
```
cd ..
```
![Image [5]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/linux-demo/20210411180636.png)

跟前面一样，你可以在它后面加具体的目录，这里的意思就是定位到与当前目录同级的其它目录：

```
cd ../projects/
```
![Image [6]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/linux-demo/20210411180646.png)

## / 路径目录分隔符
斜杠 "/" 指的是路径目录分隔符，这里没什么好说的。
```
ls ~/work/tests/
```
![Image [7]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/linux-demo/20210411180655.png)

但是，有意思的是，如果 / 路径目录分隔符前面没有东西的话，是不是就是意味着这是最上级的目录了？由于 Linux 系统的目录树均始于 / ，所以仅仅一个 / 代表了我们常说的系统根目录。

```
cd /
```
![Image [8]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/linux-demo/20210411180711.png)

## # 注释
以 # 开头，代表这句话是注释。
```
# This will be ignored by the Bash shell
```
![Image [9]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/linux-demo/20210411180724.png)

虽然上面那段话就被忽略了，但它还是会添加到您的命令历史记录中。

更厉害的做法如下：

先定义一个变量并给它赋值字符串 “amazing alvin”

```
this_string="amazing alvin"
```
${this_string#amazing} 返回的是被注释掉 amazing 的 this_string 字符串变量，可以 echo 输出看下结果：
```
echo awsome ${this_string#amazing}
```
![Image [10]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/linux-demo/20210411180734.png)

amazing 只是被注释掉而已，它并未被删除，去掉注释它就回来了：

```
echo $this_string
```
![Image [11]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/linux-demo/20210411180743.png)

## ? 单字符通配符
问号 "?"，指的是单字符通配符。Bash Shell 支持三种通配符。

它代表文件名中任意一个字符的匹配，例如：
```
ls badge?.txt
```
![Image [12]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/linux-demo/20210411180755.png)

注意，它与 badge.txt 是不匹配滴，因为 badge 后面没有字符。

正因为 "?" 匹配单个字符，所以这里有个看似很厉害的玩法，就是你想要的找的文件的文件名有多少个字符，你就输入多少个 "?" 。
```
ls ?????.txt
```
![Image [13]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/linux-demo/20210411180804.png)

## * 字符序列通配符
星号 * 代表的是任意字符序列，匹配任意字符，包括空字符，以刚才的 badge 为例：
```
ls badge*
```
![Image [14]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/linux-demo/20210411180813.png)

可以看到，badge.txt 都匹配到了。

匹配任意类型的文件：
```
ls source.*
```

## [] 字符集通配符
方括号 "[]" 指的是字符集通配符，文件名中的相关字符必须与字符集中的至少一个字符匹配。通过例子来体会一下它的作用吧：
```
ls badge_0[246].txt
```
![Image [15]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/linux-demo/20210411180823.png)
```
ls badge_[01][789].txt
```
![Image [16]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/linux-demo/20210411180834.png)
```
ls badge_[23][1-5].txt
```
![Image [17]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/linux-demo/20210411180842.png)

## ; 命令分隔符
这跟我们日常使用的 ";" 差不多，就不细说了，主要是用来分隔命令的。
```
ls > count.txt; wc -l count.txt; rm count.txt
```
![Image [18]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/linux-demo/20210411180853.png)

这里注意，用 ; 分隔命令时，即使第一个命令失败，第二个命令也会运行，即使第二个命令失败，第三个命令也会运行，依此类推。

如果要在一个命令失败的情况下就停止，请使用 "&&" ，如下：
```
cd ./doesntexist && cp ~/Documents/reports/* .
```
![Image [19]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/linux-demo/20210411180903.png)

## & 后台处理
有时候在终端正在运行一个命令时，例如 vim，你想运行另外一个命令怎么办？这里有个小技巧就是在命令后面加一个 "&" 符号，将这个程序放在后台启动，这样你就能在终端实现后台多任务的效果了。
```
vim command_address.page &
```
![Image [20]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/linux-demo/20210411180916.png)

上面显示的是这个后台进程的 ID 

## < 输入重定向
许多 Linux 命令接受一个文件作为参数，并从该文件中获取数据。这些命令中的大多数还可以从流中获取输入。要创建一个流，可以使用左尖括号 "<" ，如下将文件重定向到命令中:
```
sort < words.txt
```
![Image [21]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/linux-demo/20210411180930.png)

上面将 words.txt 文件的内容并进行了排序。

注意：它是不显示数据来源文件的文件名的。

```
wc words.txt
wc < words.txt
```
![Image [22]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/linux-demo/20210411180938.png)

## > 输出重定向
输入和输出是相反的，很好理解。用右尖括号 ">" 将命令的输出重定向，通常是重定向到文件中。
```
ls > files.txt
cat files.txt
```
![Image [23]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/linux-demo/20210411180948.png)

高端玩家还可以和数字一同使用：

```
wc doesntexist.txt 2> errors.txt
cat errors.txt
```
![Image [24]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/linux-demo/20210411180956.png)

这里的 2 是一个文件描述符，表示标准错误(stderr)

## | 连接命令
我们可以将 " | " 看成将命令链接在一起的管道。它从一个命令获取输出，并将其作为输入送入下一个命令。管道命令的数量是任意的。
```
cat words.txt | grep [cC] | sort -r
```
![Image [25]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/linux-demo/20210411181003.png)

解释下，上面先使用 cat 将 words.txt 文件的内容输入 grep , 然后 grep 提取包含小写或大写（C/c）的任何行，接着 grep 将这些行传递给 sort ，最后 sort 进行 -r 反向排序。

## ! 逻辑非
这跟编程语言中的 " ! " 差不多，我们直接拿个例子来说吧：
```
[ ! -d ./backup ] && mkdir ./backup
```
第一个命令 -d 判断当前目录是否存在 backup 的目录文件，外面加个逻辑非 ! 判断，第二个命令是创建 backup 目录文件，中间的 && 上面说过。

总的来说就是当 backup 目录不存在时，创建 backup 目录；当不存在时则不执行第二条命令。

不妨看下文件夹的备份状态：
```
ls -l -d backup
```
![Image [26]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/linux-demo/20210411181011.png)

" ! " 的另一个用法就是重新运行历史命令：

```
!24
!!
```
![Image [27]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/linux-demo/20210411181023.png)

" !! " 是重新运行上一条命令的意思。

## $ 变量表达式
"$" 开头通常表示变量，下面是一些系统变量：
```
echo $USER
echo $HOME
echo $PATH
```
![Image [28]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/linux-demo/20210411181031.png)

当然，你也可以自己定义变量然后输出：

```
ThisDistro=Ubuntu
MyNumber=2001
echo $ThisDistro
echo $MyNumber
```
![Image [29]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/linux-demo/20210411181039.png)

我们还可以通过 "{}" 解锁更高级的玩法：

先定义一个变量 MyString 并给它赋值 123456qwerty
```
MyString=123456qwerty
```
正常输出
```
echo ${MyString}
```
加个 ":6" 返回从索引位置 6 开始的一直到最后的字符串
```
echo ${MyString:6}
```
显示从索引位置从 0 开始往后 6 个字符的字符串
```
echo ${MyString:0:6}
```
显示从索引位置从 4 开始往后 4 个字符的字符串
```
echo ${MyString:4:4}
```
![Image [30]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/linux-demo/20210411181049.png)

## 引用特殊字符
说了那么多，那么就有个问题了，就是我只想在命令里面将这些特殊字符作为一般的符号显示怎么办？这种我们称之为引用，Linux 中有三种引用方法。

用双引号 "" 括起来，不过这对 "$" 无效。
```
echo "Today is $(date)"
```
用单引号 '' 括起来，停止所有特殊字符的功能。
```
echo 'Today is $(date)'
```
反斜杠 \ 转义，这在很多场合都有通用的。
```
echo "Today is \$(date)"
```
![Image [31]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/linux-demo/20210411181058.png)