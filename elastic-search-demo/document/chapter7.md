[toc]



# ElasticSearch URI 查询

## 1.URI 查询格式

URI 查询的一般格式如下：

```shell
GET /index_name/_search?q=key:val&df=title&sort=year:desc&from=0&size=10
{
	"profile":"true"
}
```

其中的参数代表的含义如下：

- q：用于指定查询语句，它可以是一个键值对，也可以只有一个 val。
  - key 表示在哪个字段中查询。
  - val 表示查询的内容。
- df：查询中未定义字段前缀时使用的默认字段。如果不指定，默认会对所有字段进行查询。
- sort：指定排序规则。
- from：与 size 一起用于分页。
- profile：用于查看查询执行的详细过程，可参考[这里](https://www.elastic.co/guide/en/elasticsearch/reference/7.0/search-profile.html)。
- 更多的 URI 参数可参考[这里](https://www.elastic.co/guide/en/elasticsearch/reference/7.0/search-uri-request.html)。



## 2.范查询

如果查询时没有指定字段，就会在所有的字段中查询，这叫做**范查询**。例如 `q=2012`。



## 3.Term：单词查询

**Term 查询**不需要用引号引住。比如 `q=A B` 表示包含 `A` 或 `B`。



## 4.Phrase：词组查询

**Phrase 查询**需要用引号引住。

比如 `q="A B"`，表示包含 `"A B"`，并且要求**顺序一致**，实际上此时 `"A B"` 会被认为是**一个单词**。



## 5.查询分组

比如 `q=title:A B`，表示的是 `title:A` 或 `B`，会在 `title` 字段中查询 `A`，在所有的字段中查询 `B`。

而 `q=title:(A B)`，只会在 `title` 中查询 `A` 或 `B`。`(A B)` 用括号括住，表示一个分组。



## 6.布尔查询

在使用 `{"profile":"true"}` 查看执行过程时，会有两个概念 `must` 和 `must_not`：

- `must`：表示**必须存在**，用 `+` 表示。
- `must not`：表示**必须不存在**，用 `-` 表示。

布尔查询包含下面三种：

- **AND**：且，比如 `q=title:(A AND B)`，表示 `+title:A +title:B`，表示必须包含 `A`，也必须包含 `B`。
- **OR**：或，比如 `q=title:(A OR B)`，等同于 `q=title:(A B)`，表示 `title:A title:B`，表示 `title` 中包含 `A` 或包含 `B`。
- **NOT**：非，比如 `q=title:(A NOT B)`，表示 `title:A -title:B`，表示包含 `A`，但不能包含 `B`。

也可以在查询中直接使用 `+` 或 `-`， `+` 用 `%2B` （URL 编码）表示，比如：

- `q=title:(%2BA -B)`，表示必须包含 A，不能包含 B。
- `q=title:(-A -B)`，表示不能包含 A，也不能包含 B。
- `q=title:(-A %2BB)`，表示不能包含 A，但必须包含 B。
- `q=title:(A %2BB)`，其实等价于 `q=title:(%2BB)`，表示必须包含 B，A 无所谓。



## 7.范围查询

用于数字类型：

- `[]` 表示闭区间
- `{}` 表示开区间

使用的时候用**括号**括住，比如：

- `year:({2000 TO 2020])`，表示 `2000 < year <= 2020`。
- `year:([* TO 2018])`，表示 `year <= 2018`。
- `year:({2016 TO *})`，表示 `year > 2016`。



## 8.通配符与正则查询

在通配符查询中：

- `?` 代表 1 个字符
- `*` 代表 0 或多个字符

一般通配符查询效率较低，占内存大，所以不建议使用，特别是放在最前面。

通配符查询示例：

- `title:mi?d`
- `title:be*`

正则表达式查询示例：

- `title:[bt]oy`



## 9.模糊匹配与近似查询

示例：

- `title:beautifl~1`
- `title:"Lord Rings"~2`