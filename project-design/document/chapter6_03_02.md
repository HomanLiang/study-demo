[toc]



# Json库

## 1.废弃fastjson！大型项目迁移Gson保姆级实战

### 1.1.为何要放弃fastjson？

究其原因，**是fastjson漏洞频发**，导致了公司内部需要频繁的督促各业务线升级 `fastjson` 版本，来防止安全问题。

`fastjson` 在2020年频繁暴露安全漏洞，此漏洞可以绕过 `autoType` 开关来实现反序列化远程代码执行并获取服务器访问权限。

从2019年7月份发布的 `v1.2.59` 一直到2020年6月份发布的 `v1.2.71` ，每个版本的升级中都有关于 `AutoType` 的升级，涉及13个正式版本。

`fastjson` 中与 `AutoType` 相关的版本历史：

```
1.2.59发布，增强AutoType打开时的安全性 fastjson
1.2.60发布，增加了AutoType黑名单，修复拒绝服务安全问题 fastjson
1.2.61发布，增加AutoType安全黑名单 fastjson
1.2.62发布，增加AutoType黑名单、增强日期反序列化和JSONPath fastjson
1.2.66发布，Bug修复安全加固，并且做安全加固，补充了AutoType黑名单 fastjson
1.2.67发布，Bug修复安全加固，补充了AutoType黑名单 fastjson
1.2.68发布，支持GEOJSON，补充了AutoType黑名单
1.2.69发布，修复新发现高危AutoType开关绕过安全漏洞，补充了AutoType黑名单
1.2.70发布，提升兼容性，补充了AutoType黑名单
1.2.71发布，补充安全黑名单，无新增利用，预防性补充
```

相比之下，其他的json框架，如Gson和Jackson，漏洞数量少很多，**高危漏洞也比较少**，这是公司想要替换框架的主要原因。

### 1.2.fastjson替代方案

**本文主要讨论Gson替换fastjson框架的实战问题，所以在这里不展开详细讨论各种json框架的优劣，只给出结论。**

经过评估，主要有 `Jackson` 和 `Gson` 两种 `json` 框架放入考虑范围内，与 `fastjson` 进行对比。

#### 1.2.1.三种json框架的特点

**FastJson**

- 速度快

	`fastjson` 相对其他JSON库的特点是快，从2011年 `fastjson` 发布1.1.x版本之后，其性能从未被其他Java实现的JSON库超越。

- 使用广泛

	`fastjson` 在阿里巴巴大规模使用，在数万台服务器上部署，`fastjson` 在业界被广泛接受。在2012年被开源中国评选为最受欢迎的国产开源软件之一。

- 测试完备

	`fastjson` 有非常多的 `testcase` ，在1.2.11版本中，`testcase` 超过3321个。每次发布都会进行回归测试，保证质量稳定。

- 使用简单

	`fastjson` 的API十分简洁。

**Jackson**

- 容易使用 - jackson API提供了一个高层次外观，以简化常用的用例。

- 无需创建映射 - API提供了默认的映射大部分对象序列化。

- 性能高 - 快速，低内存占用，适合大型对象图表或系统。

- 干净的JSON - jackson创建一个干净和紧凑的JSON结果，这是让人很容易阅读。

- 不依赖 - 库不需要任何其他的库，除了JDK。

**Gson**

- 提供一种机制，使得将Java对象转换为JSON或相反如使用toString()以及构造器（工厂方法）一样简单。

- 允许预先存在的不可变的对象转换为JSON或与之相反。

- 允许自定义对象的表现形式

- 支持任意复杂的对象

- 输出轻量易读的JSON

#### 1.2.2.性能对比

同事撰写的性能对比源码：

https://github.com/zysrxx/json-comparison

本文不详细讨论性能的差异，毕竟这其中涉及了很多各个框架的实现思路和优化，所以只给出结论：

1. 序列化单对象性能 `Fastjson > Jackson > Gson`，其中Fastjson和Jackson性能差距很小，Gson性能较差
   
2. 序列化大对象性能 `Jackson> Fastjson > Gson` ，序列化大Json对象时 `Jackson> Gson > Fastjson`，Jackson序列化大数据时性能优势明显
   
3. 反序列化单对象性能 `Fastjson > Jackson > Gson` , 性能差距较小
   
4. 反序列化大对象性能 `Fastjson > Jackson > Gson` , 性能差距较很小

#### 1.2.3.最终选择方案

- `Jackson`适用于高性能场景，`Gson` 适用于高安全性场景

- 对于新项目仓库，不再使用 `fastjson`。对于存量系统，考虑到 `Json` 更换成本，由以下几种方案可选：

  - 项目未使用 `autoType` 功能，建议直接切换为非 `fastjson`，如果切换成本较大，可以考虑继续使用 `fastjson`，关闭`safemode`。
  - 业务使用了 `autoType` 功能，建议推进废弃 `fastjson`。

### 1.3.替换依赖注意事项

企业项目或者说大型项目的特点：

- 代码结构复杂，团队多人维护。
- 承担重要线上业务，一旦出现严重bug会导致重大事故。
- 如果是老项目，可能缺少文档，不能随意修改，牵一发而动全身。
- 项目有很多开发分支，不断在迭代上线。

所以对于大型项目，想要做到将底层的fastjson迁移到gson是一件复杂且痛苦的事情，其实对于其他依赖的替换，也都一样。

**我总结了如下几个在替换项目依赖过程中要特别重视的问题。**

#### 1.3.1.谨慎，谨慎，再谨慎

再怎么谨慎都不为过，如果你要更改的项目是非常重要的业务，那么一旦犯下错误，代价是非常大的。并且，对于业务方和产品团队来说，没有新的功能上线，但是系统却炸了，是一件“无法忍受”的事情。尽管你可能觉得很委屈，因为只有你或者你的团队知道，虽然业务看上去没变化，但是代码底层已经发生了翻天覆地的变化。

所以，谨慎点！

#### 1.3.2.做好开发团队和测试团队的沟通

在依赖替换的过程中，需要做好项目的规划，比如分模块替换，严格细分排期。

把前期规划做好，开发和测试才能有条不紊的进行工作。

开发之间，需要提前沟通好开发注意事项，比如依赖版本问题，防止由多个开发同时修改代码，最后发现使用的版本不同，接口用法都不同这种很尴尬，并且要花额外时间处理的事情。

而对于测试，更要事先沟通好。一般来说，测试不会太在意这种对于业务没有变化的技术项目，因为既不是优化速度，也不是新功能。但其实迁移涉及到了底层，很容易就出现BUG。要让测试团队了解更换项目依赖，是需要大量的测试时间投入的，成本不亚于新功能，让他们尽量重视起来。

#### 1.3.3.做好回归/接口测试

上面说到测试团队需要投入大量工时，这些工时主要都用在项目功能的整体回归上，也就是回归测试。

当然，不只是业务回归测试，如果有条件的话，要做接口回归测试。

**如果公司有接口管理平台，那么可以极大提高这种项目测试的效率。**

打个比方，在一个模块修改完成后，在测试环境（或者沙箱环境），部署一个线上版本，部署一个修改后的版本，直接将接口返回数据进行对比。一般来说是Json对比，网上也有很多的Json对比工具：

https://www.sojson.com/

#### 1.3.4.考虑迁移前后的性能差异

正如上面描述的Gson和Fastjson性能对比，替换框架需要注意框架之间的性能差异，尤其是对于流量业务，也就是高并发项目，响应时间如果发生很大的变化会引起上下游的注意，导致一些额外的后果。

### 1.4.使用Gson替换Fastjson

**这里总结了两种json框架常用的方法，贴出详细的代码示例，帮助大家快速的上手Gson，无缝切换！**

#### 1.4.1.Json反序列化

```
String jsonCase = "[{\"id\":10001,\"date\":1609316794600,\"name\":\"小明\"},{\"id\":10002,\"date\":1609316794600,\"name\":\"小李\"}]";

// fastjson
JSONArray jsonArray = JSON.parseArray(jsonCase);
System.out.println(jsonArray);
System.out.println(jsonArray.getJSONObject(0).getString("name"));
System.out.println(jsonArray.getJSONObject(1).getString("name"));
// 输出：
// [{"date":1609316794600,"name":"小明","id":10001},{"date":1609316794600,"name":"小李","id":10002}]
// 小明
// 小李

// Gson
JsonArray jsonArrayGson = gson.fromJson(jsonCase, JsonArray.class);
System.out.println(jsonArrayGson);
System.out.println(jsonArrayGson.get(0).getAsJsonObject().get("name").getAsString());
System.out.println(jsonArrayGson.get(1).getAsJsonObject().get("name").getAsString());
// 输出：
// [{"id":10001,"date":1609316794600,"name":"小明"},{"id":10002,"date":1609316794600,"name":"小李"}]
// 小明
// 小李
```

看得出，两者区别主要在get各种类型上，Gson调用方法有所改变，但是变化不大。

那么，来看下空对象反序列化会不会出现异常：

```
String jsonObjectEmptyCase = "{}";

// fastjson
JSONObject jsonObjectEmpty = JSON.parseObject(jsonObjectEmptyCase);
System.out.println(jsonObjectEmpty);
System.out.println(jsonObjectEmpty.size());
// 输出：
// {}
// 0

// Gson
JsonObject jsonObjectGsonEmpty = gson.fromJson(jsonObjectEmptyCase, JsonObject.class);
System.out.println(jsonObjectGsonEmpty);
System.out.println(jsonObjectGsonEmpty.size());
// 输出：
// {}
// 0
```

没有异常，开心。

看看空数组呢，毕竟[]感觉比{}更加容易出错。

```
String jsonArrayEmptyCase = "[]";

// fastjson
JSONArray jsonArrayEmpty = JSON.parseArray(jsonArrayEmptyCase);
System.out.println(jsonArrayEmpty);
System.out.println(jsonArrayEmpty.size());
// 输出：
// []
// 0

// Gson
JsonArray jsonArrayGsonEmpty = gson.fromJson(jsonArrayEmptyCase, JsonArray.class);
System.out.println(jsonArrayGsonEmpty);
System.out.println(jsonArrayGsonEmpty.size());
// 输出：
// []
// 0
```

两个框架也都没有问题，完美解析。

#### 1.4.2.范型处理

解析泛型是一个非常常用的功能，我们项目中大部分fastjson代码就是在解析json和Java Bean。

```
// 实体类
User user = new User();
user.setId(1L);
user.setUserName("马云");

// fastjson
List<User> userListResultFastjson = JSONArray.parseArray(JSON.toJSONString(userList), User.class);
List<User> userListResultFastjson2 = JSON.parseObject(JSON.toJSONString(userList), new TypeReference<List<User>>(){});
System.out.println(userListResultFastjson);
System.out.println("userListResultFastjson2" + userListResultFastjson2);
// 输出：
// userListResultFastjson[User [Hash = 483422889, id=1, userName=马云], null]
// userListResultFastjson2[User [Hash = 488970385, id=1, userName=马云], null]

// Gson
List<User> userListResultTrue = gson.fromJson(gson.toJson(userList), new TypeToken<List<User>>(){}.getType());
System.out.println("userListResultGson" + userListResultGson);
// 输出：
// userListResultGson[User [Hash = 1435804085, id=1, userName=马云], null]
```

可以看出，Gson也能支持泛型。

#### 1.4.3.List/Map写入

这一点fastjson和Gson有区别，Gson不支持直接将List写入value，而fastjson支持。

所以Gson只能将List解析后，写入value中，详见如下代码：

```
// 实体类
User user = new User();
user.setId(1L);
user.setUserName("马云");

// fastjson
JSONObject jsonObject1 = new JSONObject();
jsonObject1.put("user", user);
jsonObject1.put("userList", userList);
System.out.println(jsonObject1);
// 输出：
// {"userList":[{"id":1,"userName":"马云"},null],"user":{"id":1,"userName":"马云"}}

// Gson
JsonObject jsonObject = new JsonObject();
jsonObject.add("user", gson.toJsonTree(user));
System.out.println(jsonObject);
// 输出：
// {"user":{"id":1,"userName":"马云"},"userList":[{"id":1,"userName":"马云"},null]}
```

如此一来，Gson看起来就没有fastjson方便，因为放入List是以`gson.toJsonTree(user)`的形式放入的。这样就不能先入对象，在后面修改该对象了。（有些同学比较习惯先放入对象，再修改对象，这样的代码就得改动）

#### 1.4.4.驼峰与下划线转换

驼峰转换下划线依靠的是修改Gson的序列化模式，修改为`LOWER_CASE_WITH_UNDERSCORES`

```
GsonBuilder gsonBuilder = new GsonBuilder();
gsonBuilder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
Gson gsonUnderScore = gsonBuilder.create();
System.out.println(gsonUnderScore.toJson(user));
// 输出：
// {"id":1,"user_name":"马云"}
```

### 1.5.常见问题排雷

下面整理了我们在公司项目迁移Gson过程中，踩过的坑，这些坑现在写起来感觉没什么技术含量。但是这才是我写这篇文章的初衷，帮助大家把这些很难发现的坑避开。

这些问题有的是在测试进行回归测试的时候发现的，有的是在自测的时候发现的，有的是在上线后发现的，比如Swagger挂了这种不会去测到的问题。

#### 1.5.1.Date序列化方式不同

**不知道大家想过一个问题没有，如果你的项目里有缓存系统，使用fastjson写入的缓存，在你切换Gson后，需要用Gson解析出来。所以就一定要保证两个框架解析逻辑是相同的，但是，显然这个愿望是美好的。**

在测试过程中，发现了Date类型，在两个框架里解析是不同的方式。

- fastjson：Date直接解析为Unix
- Gson：直接序列化为标准格式Date

![图片](https://mmbiz.qpic.cn/mmbiz_png/qm3R3LeH8rah6NyVqibeEmpzkkiakeHLicXHIuniaNoUOgTn7bibsrJSHubLbv2Msa3ciaPLAzOt6CL8w4hdlf9gEnEw/640?wx_fmt=png&tp=webp&wxfrom=5&wx_lazy=1&wx_co=1)

导致了Gson在反序列化这个json的时候，直接报错，无法转换为Date。

**解决方案：**

新建一个专门用于解析Date类型的类：

```
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.Date;

public class MyDateTypeAdapter extends TypeAdapter<Date> {
    @Override
    public void write(JsonWriter out, Date value) throws IOException {
        if (value == null) {
            out.nullValue();
        } else {
            out.value(value.getTime());
        }
    }

    @Override
    public Date read(JsonReader in) throws IOException {
        if (in != null) {
            return new Date(in.nextLong());
        } else {
            return null;
        }
    }
}
```

接着，在创建Gson时，把他放入作为Date的专用处理类：

```
Gson gson = new GsonBuilder().registerTypeAdapter(Date.class,new MyDateTypeAdapter()).create();
```

这样就可以让Gson将Date处理为Unix。

**当然，这只是为了兼容老的缓存，如果你觉得你的仓库没有这方面的顾虑，可以忽略这个问题。**

#### 1.5.2.SpringBoot异常

切换到Gson后，使用SpringBoot搭建的Web项目的接口直接请求不了了。报错类似：

```
org.springframework.http.converter.HttpMessageNotWritableException
```

因为SpringBoot默认的Mapper是Jackson解析，我们切换为了Gson作为返回对象后，Jackson解析不了了。

**解决方案：**

`application.properties` 里面添加：

```
#Preferred JSON mapper to use for HTTP message conversion
spring.mvc.converters.preferred-json-mapper=gson
```

#### 1.5.3.Swagger异常

这个问题和上面的SpringBoot异常类似，是因为在SpringBoot中引入了Gson，导致 swagger 无法解析 json。

![图片](https://mmbiz.qpic.cn/mmbiz_png/qm3R3LeH8rah6NyVqibeEmpzkkiakeHLicXC6eyYdRlIHfzL2gzHibNw86gOhSg8RPKeiaO8KGDruwGv8YKqZkrOrCg/640?wx_fmt=png&tp=webp&wxfrom=5&wx_lazy=1&wx_co=1)

采用类似下文的解决方案（添加Gson适配器）：

GsonSwaggerConfig.java

```
@Configuration
public class GsonSwaggerConfig {
    //设置swagger支持gson
    @Bean
    public IGsonHttpMessageConverter IGsonHttpMessageConverter() {
        return new IGsonHttpMessageConverter();
    }
}
```

IGsonHttpMessageConverter.java

```
public class IGsonHttpMessageConverter extends GsonHttpMessageConverter {
    public IGsonHttpMessageConverter() {
        //自定义Gson适配器
        super.setGson(new GsonBuilder()
                .registerTypeAdapter(Json.class, new SpringfoxJsonToGsonAdapter())
                .serializeNulls()//空值也参与序列化
                .create());
    }
}
```

SpringfoxJsonToGsonAdapter.java

```
public class SpringfoxJsonToGsonAdapter implements JsonSerializer<Json> {
    @Override
    public JsonElement serialize(Json json, Type type, JsonSerializationContext jsonSerializationContext) {
        return new JsonParser().parse(json.value());
    }
}
```

#### 1.5.4.@Mapping JsonObject作为入参异常

有时候，我们会在入参使用类似：

```
public ResponseResult<String> submitAudit(@RequestBody JsonObject jsonObject) {}
```

如果使用这种代码，其实就是使用Gson来解析json字符串。**但是这种写法的风险是很高的**，平常请大家尽量避免使用JsonObject直接接受参数。

在Gson中，JsonObject若是有数字字段，会统一序列化为double，也就是会把`count = 0`这种序列化成`count = 0.0`。

为何会有这种情况？**简单的来说就是Gson在将json解析为Object类型时，会默认将数字类型使用double转换。**

> 如果Json对应的是Object类型，最终会解析为Map<String, Object>类型；其中Object类型跟Json中具体的值有关，比如双引号的""值翻译为STRING。我们可以看下数值类型（NUMBER）全部转换为了Double类型，所以就有了我们之前的问题，整型数据被翻译为了Double类型，比如30变为了30.0。

可以看下Gson的ObjectTypeAdaptor类，它继承了Gson的TypeAdaptor抽象类：

![图片](https://mmbiz.qpic.cn/mmbiz_png/qm3R3LeH8rah6NyVqibeEmpzkkiakeHLicXhKMcC6w8aJQSPzAEneYbl06sCIul1cua1wibR6IIDbFKtch1WOI1ibJg/640?wx_fmt=png&tp=webp&wxfrom=5&wx_lazy=1&wx_co=1)

具体的源码分析和原理阐述，大家可以看这篇拓展阅读：

https://www.jianshu.com/p/eafce9689e7d

**解决方案：**

第一个方案：把入参用实体类接收，不要使用JsonObject

第二个方案：与上面的解决Date类型问题类似，自己定义一个Adaptor，来接受数字，并且处理。这种想法我觉得可行但是难度较大，可能会影响到别的类型的解析，需要在设计适配器的时候格外注意。

### 1.6.总结

这篇文章主要是为了那些需要将项目迁移到Gson框架的同学们准备的。

一般来说，个人小项目，是不需要费这么大精力去做迁移，所以这篇文章可能目标人群比较狭窄。

但文章中也提到了不少**通用问题**的解决思路，比如怎么评估迁移框架的必要性。其中需要考虑到框架兼容性，两者性能差异，迁移耗费的工时等很多问题。