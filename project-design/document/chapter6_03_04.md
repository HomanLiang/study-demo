# EasyExcel

[toc]

## 1.相关网址
 [EasyExcel GitHub](https://github.com/alibaba/easyexcel)
 [语雀知识库--EasyExcel官方文档](https://www.yuque.com/easyexcel)

## 2.POI 和 EasyExcel 比较
### 2.1.实现方式
#### 2.1.1.POI
1. 要区分不同Excel版本单独处理

2. 要自己实现读写逻辑

3. 要自己判断单元格格式，考虑不完善会出错

  ![Image](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505210020.png)

  ![Image [2]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505210040.png)

  ![Image [3]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505210045.png)

  ![Image [4]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505210049.png)

  ![Image [5]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505210054.png)
#### 2.1.2.EasyExcel
1. 定义好实体类就可以接收Excel数据了

2. 业务逻辑可以写在监听器，EasyExcel时读取一行，处理一行的

3. 封装得比较好，不需要考虑Excel文件类型之类的问题

  Excel文件：

  ![Image [6]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505210104.png)

  EasyExcel读写操作实体类：

  ![Image [7]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505210111.png)

  监听器实现类：

  ![Image [8]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505210117.png)

  调用方法：

  ![Image [9]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505210124.png)

### 2.2.性能比较
读取10W行，5.5Mb数据(.xslx)
#### 2.2.1.POI
开发者们大部分使用 POI，都是使用其 userModel 模式。而 userModel 的好处是上手容易使用简单，随便拷贝个代码跑一下，剩下就是写业务转换了，虽然转换也要写上百行代码，但是还是可控的。
然而 userModel 模式最大的问题是在于，对内存消耗非常大，一个几兆的文件解析甚至要用掉上百兆的内存。现实情况是，很多应用现在都在采用这种模式，之所以还正常在跑是因为并发不大，并发上来后，一定会OOM或者频繁的 full gc

内存使用：

![Image [10]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505210135.png)

执行时间：

![Image [11]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505210141.png)

#### 2.2.2.EasyExcel
内存使用：

![Image [12]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505210147.png)

执行时间：

![Image [13]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505210154.png)

## 3.关于EasyExcel

easyexcel核心功能:
- 读任意大小的03、07版Excel不会OO]
- 读Excel自动通过注解，把结果映射为java模型
- 读Excel支持多sheet
- 读Excel时候是否对Excel内容做trim()增加容错
- 写小量数据的03版Excel（不要超过2000行）
- 写任意大07版Excel不会OOM
- 写Excel通过注解将表头自动写入Excel
- 写Excel可以自定义Excel样式 如：字体，加粗，表头颜色，数据内容颜色
- 写Excel到多个不同sheet
- 写Excel时一个sheet可以写多个Table
- 写Excel时候自定义是否需要写表头


## 4.常用技巧
### 4.1.读取技巧
#### 4.1.1.Excel读取多页
```
    /**
     * 读多个或者全部sheet,这里注意一个sheet不能读取多次，多次读取需要重新读取文件
     * <p>
     * 1. 创建excel对应的实体对象 参照{@link DemoData}
     * <p>
     * 2. 由于默认异步读取excel，所以需要创建excel一行一行的回调监听器，参照{@link DemoDataListener}
     * <p>
     * 3. 直接读即可
     */
    @Test
    public void repeatedRead() {
        String fileName = TestFileUtil.getPath() + "demo" + File.separator + "demo.xlsx";
        // 读取全部sheet
        // 这里需要注意 DemoDataListener的doAfterAllAnalysed 会在每个sheet读取完毕后调用一次。然后所有sheet都会往同一个DemoDataListener里面写
        EasyExcel.read(fileName, DemoData.class, new DemoDataListener()).doReadAll();
 
        // 读取部分sheet
        fileName = TestFileUtil.getPath() + "demo" + File.separator + "demo.xlsx";
        ExcelReader excelReader = EasyExcel.read(fileName).build();
        // 这里为了简单 所以注册了 同样的head 和Listener 自己使用功能必须不同的Listener
        ReadSheet readSheet1 =
            EasyExcel.readSheet(0).head(DemoData.class).registerReadListener(new DemoDataListener()).build();
        ReadSheet readSheet2 =
            EasyExcel.readSheet(1).head(DemoData.class).registerReadListener(new DemoDataListener()).build();
        // 这里注意 一定要把sheet1 sheet2 一起传进去，不然有个问题就是03版的excel 会读取多次，浪费性能
        excelReader.read(readSheet1, readSheet2);
        // 这里千万别忘记关闭，读的时候会创建临时文件，到时磁盘会崩的
        excelReader.finish();
    }
```
可以看到doReadAll方法可以读取所有sheet页面

若要读取单独的页面，用第二种方式readSheet(index)，index为页面位置，从0开始计数

### 4.2.自定义字段转换
在读取写入的时候，我们可能会有这样的需求：比如日期格式转换，字符串添加固定前缀后缀等等，此时我们可以进行自定义编写
```
@Data
public class ConverterData {
    /**
     * 我自定义 转换器，不管数据库传过来什么 。我给他加上“自定义：”
     */
    @ExcelProperty(converter = CustomStringStringConverter.class)
    private String string;
    /**
     * 这里用string 去接日期才能格式化。我想接收年月日格式
     */
    @DateTimeFormat("yyyy年MM月dd日HH时mm分ss秒")
    private String date;
    /**
     * 我想接收百分比的数字
     */
    @NumberFormat("#.##%")
    private String doubleData;
}
```
如上面的CustomStringStringConverter类为自定义转换器，可以对字符串进行一定修改，而日期数字的格式化，它已经有提供注解了DateTimeFormat和NumberFormat

转换器如下，实现Converter接口后即可使用supportExcelTypeKey这是判断单元格类型，convertToJavaData这是读取转换，convertToExcelData这是写入转换

```
import com.alibaba.excel.converters.Converter;
import com.alibaba.excel.enums.CellDataTypeEnum;
import com.alibaba.excel.metadata.CellData;
import com.alibaba.excel.metadata.GlobalConfiguration;
import com.alibaba.excel.metadata.property.ExcelContentProperty;
 
public class CustomStringStringConverter implements Converter<String> {
    @Override
    public Class supportJavaTypeKey() {
        return String.class;
    }
 
    @Override
    public CellDataTypeEnum supportExcelTypeKey() {
        return CellDataTypeEnum.STRING;
    }
 
    /**
     * 这里读的时候会调用
     */
    @Override
    public String convertToJavaData(CellData cellData, ExcelContentProperty contentProperty,
        GlobalConfiguration globalConfiguration) {
        return "自定义：" + cellData.getStringValue();
    }
 
    /**
     * 这里是写的时候会调用 不用管
     */
    @Override
    public CellData convertToExcelData(String value, ExcelContentProperty contentProperty,
        GlobalConfiguration globalConfiguration) {
        return new CellData(value);
    }
 
}
```
这里解析结果截取部分如下，原数据是字符串0 2020/1/1 1:01 1

解析到一条数据:{"date":"2020年01月01日01时01分01秒","doubleData":"100%","string":"自定义：字符串0"}

#### 4.2.1.指定表头行数
```
        EasyExcel.read(fileName, DemoData.class, new DemoDataListener()).sheet()
            // 这里可以设置1，因为头就是一行。如果多行头，可以设置其他值。不传入也可以，因为默认会根据DemoData 来解析，他没有指定头，也就是默认1行
            .headRowNumber(1).doRead();
```

#### 4.2.2.读取表头数据
只要在实现了AnalysisEventListener接口的监听器中，重写invokeHeadMap方法即可
```
    /**
     * 这里会一行行的返回头
     *
     * @param headMap
     * @param context
     */
    @Override
    public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
        LOGGER.info("解析到一条头数据:{}", JSON.toJSONString(headMap));
    }
```
#### 4.2.3.转换异常处理
只要在实现了AnalysisEventListener接口的监听器中，重写onException方法即可
```
    @Override
    public void onException(Exception exception, AnalysisContext context) {
        LOGGER.error("解析失败，但是继续解析下一行:{}", exception.getMessage());
        if (exception instanceof ExcelDataConvertException) {
            ExcelDataConvertException excelDataConvertException = (ExcelDataConvertException)exception;
            LOGGER.error("第{}行，第{}列解析异常", excelDataConvertException.getRowIndex(),
                excelDataConvertException.getColumnIndex());
        }
    }
```
#### 4.2.4.读取单元格参数和类型
将类属性用CellData封装起来
```
@Data
public class CellDataReadDemoData {
    private CellData<String> string;
    // 这里注意 虽然是日期 但是 类型 存储的是number 因为excel 存储的就是number
    private CellData<Date> date;
    private CellData<Double> doubleData;
    // 这里并不一定能完美的获取 有些公式是依赖性的 可能会读不到 这个问题后续会修复
    private CellData<String> formulaValue;
}
```
这样读取到的数据如下，会包含单元格数据类型
```
解析到一条数据:{"date":{"data":1577811661000,"dataFormat":22,"dataFormatString":"m/d/yy h:mm","formula":false,"numberValue":43831.0423726852,"type":"NUMBER"},"doubleData":{"data":1.0,"formula":false,"numberValue":1,"type":"NUMBER"},"formulaValue":{"data":"字符串01","formula":true,"formulaValue":"_xlfn.CONCAT(A2,C2)","stringValue":"字符串01","type":"STRING"},"string":{"data":"字符串0","dataFormat":0,"dataFormatString":"General","formula":false,"stringValue":"字符串0","type":"STRING"}}
```
#### 4.2.5.同步返回
不推荐使用，但如果特定情况一定要用，可以如下，主要为doReadSync方法，直接返回List
```
    /**
     * 同步的返回，不推荐使用，如果数据量大会把数据放到内存里面
     */
    @Test
    public void synchronousRead() {
        String fileName = TestFileUtil.getPath() + "demo" + File.separator + "demo.xlsx";
        // 这里 需要指定读用哪个class去读，然后读取第一个sheet 同步读取会自动finish
        List<Object> list = EasyExcel.read(fileName).head(DemoData.class).sheet().doReadSync();
        for (Object obj : list) {
            DemoData data = (DemoData)obj;
            LOGGER.info("读取到数据:{}", JSON.toJSONString(data));
        }
 
        // 这里 也可以不指定class，返回一个list，然后读取第一个sheet 同步读取会自动finish
        list = EasyExcel.read(fileName).sheet().doReadSync();
        for (Object obj : list) {
            // 返回每条数据的键值对 表示所在的列 和所在列的值
            Map<Integer, String> data = (Map<Integer, String>)obj;
            LOGGER.info("读取到数据:{}", JSON.toJSONString(data));
        }
    }
```
#### 4.2.6.无对象的读
顾名思义，不创建实体对象来读取Excel数据，那么我们就用Map接收，但这种对日期不友好，对于简单字段的读取可以使用

其它都一样，监听器的继承中泛型参数变为Map即可
```
public class NoModleDataListener extends AnalysisEventListener<Map<Integer, String>> {
    ...
}
```
结果截取如下
```
解析到一条数据:{0:"字符串0",1:"2020-01-01 01:01:01",2:"1"}
```
### 4.3.写入技巧
#### 4.3.1.排除特定字段和只写入特定字段
使用excludeColumnFiledNames来排除特定字段写入，用includeColumnFiledNames表示只写入特定字段
```
    /**
     * 根据参数只导出指定列
     * <p>
     * 1. 创建excel对应的实体对象 参照{@link DemoData}
     * <p>
     * 2. 根据自己或者排除自己需要的列
     * <p>
     * 3. 直接写即可
     */
    @Test
    public void excludeOrIncludeWrite() {
        String fileName = TestFileUtil.getPath() + "excludeOrIncludeWrite" + System.currentTimeMillis() + ".xlsx";
 
        // 根据用户传入字段 假设我们要忽略 date
        Set<String> excludeColumnFiledNames = new HashSet<String>();
        excludeColumnFiledNames.add("date");
        // 这里 需要指定写用哪个class去读，然后写到第一个sheet，名字为模板 然后文件流会自动关闭
        EasyExcel.write(fileName, DemoData.class).excludeColumnFiledNames(excludeColumnFiledNames).sheet("模板")
            .doWrite(data());
 
        fileName = TestFileUtil.getPath() + "excludeOrIncludeWrite" + System.currentTimeMillis() + ".xlsx";
        // 根据用户传入字段 假设我们只要导出 date
        Set<String> includeColumnFiledNames = new HashSet<String>();
        includeColumnFiledNames.add("date");
        // 这里 需要指定写用哪个class去读，然后写到第一个sheet，名字为模板 然后文件流会自动关闭
        EasyExcel.write(fileName, DemoData.class).includeColumnFiledNames(includeColumnFiledNames).sheet("模板")
            .doWrite(data());
    }
```
#### 4.3.2.指定写入列
写入列的顺序可以进行指定，在实体类注解上指定index，从小到大，从左到右排列
```
@Data
public class IndexData {
    @ExcelProperty(value = "字符串标题", index = 0)
    private String string;
    @ExcelProperty(value = "日期标题", index = 1)
    private Date date;
    /**
     * 这里设置3 会导致第二列空的
     */
    @ExcelProperty(value = "数字标题", index = 3)
    private Double doubleData;
}
```
#### 4.3.3.复杂头写入
```
@Data
public class ComplexHeadData {
    @ExcelProperty({"主标题", "字符串标题"})
    private String string;
    @ExcelProperty({"主标题", "日期标题"})
    private Date date;
    @ExcelProperty({"主标题", "数字标题"})
    private Double doubleData;
}
```
#### 4.3.4.重复多次写入
分为三种：1. 重复写入同一个sheet；2. 同一个对象写入不同sheet；3. 不同的对象写入不同的sheet
```
    /**
     * 重复多次写入
     * <p>
     * 1. 创建excel对应的实体对象 参照{@link ComplexHeadData}
     * <p>
     * 2. 使用{@link ExcelProperty}注解指定复杂的头
     * <p>
     * 3. 直接调用二次写入即可
     */
    @Test
    public void repeatedWrite() {
        // 方法1 如果写到同一个sheet
        String fileName = TestFileUtil.getPath() + "repeatedWrite" + System.currentTimeMillis() + ".xlsx";
        // 这里 需要指定写用哪个class去读
        ExcelWriter excelWriter = EasyExcel.write(fileName, DemoData.class).build();
        // 这里注意 如果同一个sheet只要创建一次
        WriteSheet writeSheet = EasyExcel.writerSheet("模板").build();
        // 去调用写入,这里我调用了五次，实际使用时根据数据库分页的总的页数来
        for (int i = 0; i < 5; i++) {
            // 分页去数据库查询数据 这里可以去数据库查询每一页的数据
            List<DemoData> data = data();
            writeSheet.setSheetName("模板");
            excelWriter.write(data, writeSheet);
        }
        /// 千万别忘记finish 会帮忙关闭流
        excelWriter.finish();
 
        // 方法2 如果写到不同的sheet 同一个对象
        fileName = TestFileUtil.getPath() + "repeatedWrite" + System.currentTimeMillis() + ".xlsx";
        // 这里 指定文件
        excelWriter = EasyExcel.write(fileName, DemoData.class).build();
        // 去调用写入,这里我调用了五次，实际使用时根据数据库分页的总的页数来。这里最终会写到5个sheet里面
        for (int i = 0; i < 5; i++) {
            // 每次都要创建writeSheet 这里注意必须指定sheetNo
            writeSheet = EasyExcel.writerSheet(i, "模板"+i).build();
            // 分页去数据库查询数据 这里可以去数据库查询每一页的数据
            List<DemoData> data = data();
            excelWriter.write(data, writeSheet);
        }
        /// 千万别忘记finish 会帮忙关闭流
        excelWriter.finish();
 
        // 方法3 如果写到不同的sheet 不同的对象
        fileName = TestFileUtil.getPath() + "repeatedWrite" + System.currentTimeMillis() + ".xlsx";
        // 这里 指定文件
        excelWriter = EasyExcel.write(fileName).build();
        // 去调用写入,这里我调用了五次，实际使用时根据数据库分页的总的页数来。这里最终会写到5个sheet里面
        for (int i = 0; i < 5; i++) {
            // 每次都要创建writeSheet 这里注意必须指定sheetNo。这里注意DemoData.class 可以每次都变，我这里为了方便 所以用的同一个class 实际上可以一直变
            writeSheet = EasyExcel.writerSheet(i, "模板"+i).head(DemoData.class).build();
            // 分页去数据库查询数据 这里可以去数据库查询每一页的数据
            List<DemoData> data = data();
            excelWriter.write(data, writeSheet);
        }
        /// 千万别忘记finish 会帮忙关闭流
        excelWriter.finish();
    }
```
#### 4.3.5.字段宽高设置
设置实体类注解属性即可
```
@Data
@ContentRowHeight(10)
@HeadRowHeight(20)
@ColumnWidth(25)
public class WidthAndHeightData {
    @ExcelProperty("字符串标题")
    private String string;
    @ExcelProperty("日期标题")
    private Date date;
    /**
     * 宽度为50
     */
    @ColumnWidth(50)
    @ExcelProperty("数字标题")
    private Double doubleData;
}
```
#### 4.3.6.自定义样式
```
    @Test
    public void styleWrite() {
        String fileName = TestFileUtil.getPath() + "styleWrite" + System.currentTimeMillis() + ".xlsx";
        // 头的策略
        WriteCellStyle headWriteCellStyle = new WriteCellStyle();
        // 背景设置为红色
        headWriteCellStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
        WriteFont headWriteFont = new WriteFont();
        headWriteFont.setFontHeightInPoints((short)20);
        headWriteCellStyle.setWriteFont(headWriteFont);
        // 内容的策略
        WriteCellStyle contentWriteCellStyle = new WriteCellStyle();
        // 这里需要指定 FillPatternType 为FillPatternType.SOLID_FOREGROUND 不然无法显示背景颜色.头默认了 FillPatternType所以可以不指定
        contentWriteCellStyle.setFillPatternType(FillPatternType.SOLID_FOREGROUND);
        // 背景绿色
        contentWriteCellStyle.setFillForegroundColor(IndexedColors.GREEN.getIndex());
        WriteFont contentWriteFont = new WriteFont();
        // 字体大小
        contentWriteFont.setFontHeightInPoints((short)20);
        contentWriteCellStyle.setWriteFont(contentWriteFont);
        // 这个策略是 头是头的样式 内容是内容的样式 其他的策略可以自己实现
        HorizontalCellStyleStrategy horizontalCellStyleStrategy =
            new HorizontalCellStyleStrategy(headWriteCellStyle, contentWriteCellStyle);
 
        // 这里 需要指定写用哪个class去读，然后写到第一个sheet，名字为模板 然后文件流会自动关闭
        EasyExcel.write(fileName, DemoData.class).registerWriteHandler(horizontalCellStyleStrategy).sheet("模板")
            .doWrite(data());
    }
```
#### 4.3.7.自动列宽
根据作者描述，POI对中文的自动列宽适配不友好，easyexcel对数字也不能准确适配列宽，他提供的适配策略可以用，但不能精确适配，可以自己重写

想用就注册处理器LongestMatchColumnWidthStyleStrategy
```

    @Test
    public void longestMatchColumnWidthWrite() {
        String fileName =
            TestFileUtil.getPath() + "longestMatchColumnWidthWrite" + System.currentTimeMillis() + ".xlsx";
        // 这里 需要指定写用哪个class去读，然后写到第一个sheet，名字为模板 然后文件流会自动关闭
        EasyExcel.write(fileName, LongestMatchColumnWidthData.class)
            .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy()).sheet("模板").doWrite(dataLong());
    }
```
#### 4.3.8.不创建对象的写
在设置write的时候不设置对象类，在head里添加List<list></list的对象头




## 5.使用注意点
### 5.1.poi 冲突问题
理论上当前 easyexcel兼容支持 poi 的3.17,4.0.1,4.1.0所有较新版本，但是如果项目之前使用较老版本的 poi，由于 poi 内部代码调整，某些类已被删除，这样直接运行时很大可能会抛出以下异常：
- NoSuchMethodException
- ClassNotFoundException
- NoClassDefFoundError

所以使用过程中一定要注意统一项目中的 poi 的版本。

### 5.2.非注解方式自定义行高列宽
非注解方式自定义行高以及列宽比较麻烦，暂时没有找到直接设置的入口。查了一遍 github 相关 issue，开发人员回复需要实现 WriteHandler 接口，自定义表格样式。