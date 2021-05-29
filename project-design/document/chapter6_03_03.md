[toc]



# Activiti

## 1.相关网站
 [Activiti User Guide](https://activiti.gitbook.io/activiti-7-developers-guide/getting-started)
 [github](https://github.com/Activiti/Activiti)
[springboot2.0+activiti 7 整合（一）--初识activiti和创建数据库](https://yq.aliyun.com/articles/727708)
[Activiti7工作流+SpringBoot](https://www.cnblogs.com/jpfss/p/11076242.html)
[Activiti 7.x 与Spring-Boot 整合](https://www.jianshu.com/p/60551e3f4ec3)

 

## 2.插件安装
### 2.1.eclipse安装activiti工作流插件
 [参考链接](https://www.cnblogs.com/mingforyou/p/5347561.html)

**安装步骤：**

1. 点击eclipse上方工具栏的Help，选择Install New Software

   ![Image](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505183111.png)

2. 弹出如下窗口，然后填写插件名称和安装地址

   Name: Activiti BPMN 2.0 designer

   Location: http://activiti.org/designer/update/

   然后便是不停的next和finish了，组图如下：

   ![Image [2]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505183124.png)

   ![Image [3]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505183134.png)

     ![Image [4]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505183143.png)

     ![Image [5]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505183150.png)

3. 安装完成后，我们在new的时候，操作面板中便有activiti的相关文件了。

     ![Image [6]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505183210.png)

### 2.2.IDEA 安装插件
#### 2.2.1.安装Activiti插件
1. 搜索插件

   点击菜单【File】-->【Settings...】打开【Settings】窗口。

   点击左侧【Plugins】按钮，在右侧输出＂actiBPM＂，点击下面的【Search in repositories】链接会打开【Browse Repositories】窗口。

   ![Image [7]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505183225.png)

2. 开始安装

   进入【Browse Repositories】窗口，选中左侧的【actiBPM】，点击右侧的【Install】按钮，开始安装。

   ![Image [8]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505183245.png)

3. 安装进度

   ![Image [9]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505183257.png)

4. 安装完成

    安装完成后，会提示【Restart IntelliJ IDEA】，重启IDEA即可完成安装。

   ![Image [10]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505183312.png)

5. 查看结果

   打开【Settings】窗口，在【Plugins】中可看到安装的【actiBPM】插件，表示安装成功。

   ![Image [11]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505183325.png)

#### 2.2.2.使用Activiti
1. 创建BPMN文件

   点击菜单【File】-->【New】-->【BpmnFile】

   ![Image [12]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505183339.png)

   输入文件名称，点击【OK】按钮

     ![Image [13]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505183352.png)

   会出现如下绘制界面

   ![Image [14]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505183400.png)

2. 绘制流程图

     鼠标左键拖拽右侧图标，将其拖下左侧界面上，同样的方式再拖拽其他图标

   ![Image [15]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505183433.png)

   鼠标移至图标的中心会变成黑白色扇形，拖拽到另一图标，即可连接

   ![Image [16]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505183440.png)

   双击图标，可修改名称

   ![Image [17]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505183445.png)

3. 导出图片

   右击bpmn文件，选择【Refactor】-->【Rename】，修改其扩展名为.xml，点击【Refactor】

   ![Image [18]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505183453.png)

   接着右击此xml文件，选择【Diagrams】-->【Show BPMN 2.0 Diagrams...】，打开如下界面

   ![Image [19]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505183459.png)

   点击上图中【Export to file】图标，弹出【Save as image】窗口，点击【OK】即可导出png图片

   ![Image [20]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505183507.png)

4. 解决中文乱码问题

   在IDEA的安装目录，在下面两个文件中加上-Dfile.encoding=UTF-8

   ![Image [21]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505183515.png)

   ![Image [22]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505183524.png)

   重启IDEA，乱码问题解决

   ![Image [23]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505183529.png)

## 3.为什么选择Activiti
![Image [24]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505183537.png)

## 4.核心7大接口、28张表
![Image [25]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505183547.png)
- RepositoryService：提供一系列管理流程部署和流程定义的API。
- RuntimeService：在流程运行时对流程实例进行管理与控制。
- TaskService：对流程任务进行管理，例如任务提醒、任务完成和创建任务等。
- IdentityService：提供对流程角色数据进行管理的API，这些角色数据包括用户组、用户及它们之间的关系。
- ManagementService：提供对流程引擎进行管理和维护的服务。
- HistoryService：对流程的历史数据进行操作，包括查询、删除这些历史数据。
- FormService：表单服务。



## 5.核心数据库表
> 由Activiti启动时自动生成

![Image [26]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505183606.png)

Activiti的后台是有数据库的支持，所有的表都以ACT_开头。

**ACT_RE_*:** 'RE'表示repository。 这个前缀的表包含了流程定义和流程静态资源 （图片，规则，等等）。

**ACT_RU_*:** 'RU'表示runtime。 这些运行时的表，包含流程实例，任务，变量，异步任务，等运行中的数据。 Activiti只在流程实例执行过程中保存这些数据， 在流程结束时就会删除这些记录。 这样运行时表可以一直很小速度很快。

**ACT_ID_*:**' ID'表示identity。 这些表包含身份信息，比如用户，组等等。

**ACT_HI_*:** 'HI'表示history。 这些表包含历史数据，比如历史流程实例， 变量，任务等等。

**ACT_GE_*:** 通用数据， 用于不同场景下，如存放资源文件。


## 6.简单例子
### 6.1.Activiti版本
Activiti 6.0

![Image [27]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505183615.png)

### 6.2.额外的表
![Image [28]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505183623.png)

**core_process_config**：流程配置表，流程的自定义设置

![Image [29]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505183632.png)

**sys_error_log**：系统错误日志

**sys_message**：系统信息

### 6.3.新建流程
#### 6.3.1.新建流程图“George_three.bpmn”
注：流程图文件放在“Processes”文件夹

![Image [30]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505183652.png)

![Image [31]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505183701.png)

![Image [32]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505183706.png)

![Image [33]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505183712.png)

#### 6.3.2.设置流程图信息
Id:流程图id，key

Name:流程图名称

Namespace:流程图命名空间

![Image [34]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505183728.png)

#### 6.3.3.画流程图
1. 拖出开始事件节点

  ![Image [35]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505183807.png)

2. 拖出用户任务节点

  ![Image [36]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505183814.png)

3. 拖出网关节点

  ![Image [37]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505183819.png)

4. 重复步骤2和3后，拖出结束事件节点

  ![Image [38]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505183826.png)

5. 补上连接线

  ![Image [39]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505183833.png)

  ![Image [40]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505183846.png)

6. 重复步骤5，并完成流程控制

  ![Image [41]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505183854.png)

  ![Image [42]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505183900.png)

7. 设置用户任务节点的id和名称

  ![Image [43]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505183906.png)

8. 设置用户任务节点的候选人和候选组

   候选人= id+"User"

   候选组= id+"Group"a

   ![Image [44]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505183912.png)

9. 设置连接线的Id和Name
    ![Image [45]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505183919.png)

10. 设置连接线的条件

    ![Image [46]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505183924.png)

11. 重复步骤9和步骤10， 最后完成流程图配置

    ![Image [47]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505183932.png)

    ![Image [48]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505183937.png)
#### 6.3.4.流程图额外配置
![Image [49]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505183945.png)
#### 6.3.5.测试流程图
![Image [50]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505183952.png)

## 7.Activiti的特点
1. 使用mybatis

2. 原生支持spring，可以轻松集成spring

3. 引擎的Service接口

Activiti引擎提供了7大Service接口，均通过processEngine获取，支持链式API风格。
|    Service接口    |                           作用                           |
| :---------------: | :------------------------------------------------------: |
| RepositoryService | 流程仓库服务，管理流程仓库，比如部署、删除、读取流程资源 |
|  IdentityService  |              身份服务，管理用户、组及其关系              |
|  RuntimeService   |     运行服务，处理所有正在运行态的流程实例、任务等。     |
|    TaskService    |     任务服务，管理（签收、办理、指派等）、查询任务。     |
|    FormService    |         表单服务，读取和流程、任务相关的表单数据         |
|  HistoryService   |                历史服务，管理所有历史数据                |
| ManagementService |         引擎管理服务，和具体业务无关，管理引擎。         |
![Image [51]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505184001.png)

```
ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();

RuntimeService runtimeService = processEngine.getRuntimeService();
RepositoryService repositoryService = processEngine.getRepositoryService();
TaskService taskService = processEngine.getTaskService();
ManagementService managementService = processEngine.getManagementService();
IdentityService identityService = processEngine.getIdentityService();
HistoryService historyService = processEngine.getHistoryService();
FormService formService = processEngine.getFormService();
```
4. 流程设计器

  Eclipse插件：Eclipse Designer

  web：Activiti Modeler

5. 分离runtime和history数据


## 8.Activiti架构与组件
![Image [52]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505184007.png)

## 9.用户与组
### 9.1.用户
```
public class IdentityServiceTest{
    @Rule
    public ActivitiRule ar = new ActivitiRule();//使用默认的acitiviti.cfg.xml作为参数，当创建activitirule时，会自动创建引擎对象processEngine。
    
    @Test
    public void testUser() throws Exception(){
        IdentityService is = ar.getIdentityService();

        User user = is.newUser("henryyan");
        user.setFirstName("Henry");
        user.setLastName("yan");
        user.setEmail("yanhonglei@gamil.com");
   
        is.saveUser(user);
        
        User userInDb = is.createUserQuery().userId("henryyan").singleResult();
        assertNotNull(userInDb);
        
        is.deleteUser("henryyan");
        userInDb = is.createUserQuery().userId("henryyan").singleResult();
        assertNull(userInDb);
    }   
}
```
### 9.2.组
```
public class IdentityServiceTest{
    @Rule
    public ActivitiRule ar = new ActivitiRule();//使用默认的acitiviti.cfg.xml作为参数，当创建activitirule时，会自动创建引擎对象processEngine。
    
    @Test
    public void testGroup() throws Exception(){
        IdentityService is = ar.getIdentityService();

        Group group = is.newGroup("deptLeader");
        group .setName("部门领导");
        group .setType("assignment");
   
        is.saveGroup(group);
        
        List<Group> groupList = is.createGroupQuery().groupId("deptLeader").list();
        assertEquals(1,groupList.size());
        
        is.deleteGroup("deptLeader");
        groupList = is.createGroupQuery().groupId("deptLeader").list();
        assertEquals(0,groupList.size());
    }   
}
```
### 9.3.用户与组的关系
```
public class IdentityServiceTest{
    @Rule
    public ActivitiRule ar = new ActivitiRule();//使用默认的acitiviti.cfg.xml作为参数
      
    @Test
    public void testUserAndGroupMembership() throws Exception(){
        IdentityService is = ar.getIdentityService();

        Group group = is.newGroup("deptLeader");
        group .setName("部门领导");
        group .setType("assignment");
        is.saveGroup(group);
        
        User user = is.newUser("henryyan");
        user.setFirstName("Henry");
        user.setLastName("yan");
        user.setEmail("yanhonglei@gamil.com");
        is.saveUser(user);

        is.createMembership("henryyan","deptLeader");

        //查询组deptLeader所拥有的用户
        User userInGroup = is.createUserQuery().memberOfGroup("deptLeader").singleResult();
        assertNotNull(userInGroup);
        assertEquals("henryyan",userInGroup.getId());

        //查询用户henryyan所属的组
        Group groupContainsHenryyan = is.createGroupQuery().groupMember("henryyan").singleResult();
        assertNotNull(groupContainsHenryyan);
        assertEquals("deptLeader",groupContainsHenryyan.getId());
    }   
 }
```
### 9.4.UserTask中的用户与组
1. 测试场景及测试模板

   假设一个流程图示意图为：开始--->用户任务--->结束

   共用的xml文件模板如下：

    ```
    <process id="userAndGroupInUserTask" name="userAndGroupInUserTask">
    <startEvent id="startEvent1" name="Start"></startEvent>
    <userTask id="studyUserAndGroupInUserTask" name="学习用户与组在用户任务中的应用"></userTask>
    <sequenceFlow id="flow1" name="" sourceRef="startEvent1" targetRef="studyUserAndGroupInUserTask"></sequenceFlow>
    <endEvent id="endEvent1" name="End"></endEvent>
    <sequenceFlow id="flow2" name="" sourceRef="studyUserAndGroupInUserTask" targetRef="endEvent1"></sequenceFlow>
    </process>
    ```
   
	共用的测试代码模板如下：即此时有两个用户，位于组deptLeader里。
   
    ```
    public class UserAndGroupInUserTaskTest{
        @Before
        public void setup() throws Exception(){
            //初始化7个Service
            super.setUp();
    
            Group group = identityService.newGroup("deptLeader");
            group .setName("部门领导");
            group .setType("assignment");
            identityService.saveGroup(group);
            //henryyan示例，jackchen的略
            User user = identityService.newUser("henryyan");
            user.setFirstName("Henry");
            user.setLastName("yan");
            user.setEmail("yanhonglei@gmail.com");
            identityService.saveUser(user);
            //henryyan示例，jackchen的略
            identityService.createMembership("henryyan","deptLeader");
        }   
    
        @After
        public void afterInvokeTestMethod() throws Exception(){
            identityService.deleteMembership("henryyan","deptLeader");
            identityService.deleteGroup("deptLeader");
            identityService.deleteUser("henryyan");
        }
        ...
     }
    ```

2. 候选组

	修改上面的UserTask如下：属于这个组的用户都可以签收任务。一旦有一个人签收，其他人的用户任务里就没有这个任务了。
  
    ```
    <userTask id="studyUserAndGroupInUserTask" 
         name="学习用户与组在用户任务中的应用"
        activiti:candidateGroups="deptLeader">
    </userTask>
    ```

3. 候选人
	

修改上面的UserTask如下：列出来的用户都可以签收任务。一旦有一个人签收，其他人的用户任务里就没有这个任务了。

   ```
    <userTask id="studyUserAndGroupInUserTask" 
          name="学习用户与组在用户任务中的应用"
         activiti:candidateUsers="jackchen,henryyan">
    </userTask>
   ```

4. 共用测试代码

  在类userAndGroupTestInUserTask添加方法如下：

   ```
   public class UserAndGroupInUserTaskTest{
       @Test
       @Deployment(resources={"chapter5/userAndGroupInUserTask.bpmn"})
       public void testUserAndGroupInUserTask() throws Exception{
           ProcessInstance pi = runtimeService.startProcessInstanceByKey("userAndGroupInUserTask");
           assertNotNull(pi);
  
           //该任务属于所有位于候选组的用户/ 该任务属于所有列出来的候选人
           Task jackchenTask = taskService.createTaskQuery().taskCandidateUser("jackchen").singleResult();
           assertNotNull(jackchenTask);
           Task henryyanTask = taskService.createTaskQuery().taskCandidateUser("henryyan").singleResult();
           assertNotNull(henryyanTask);
  
           taskService.claim(jackchenTask.getId(),"jackchen");//jackchen签收任务
           //taskService.complete(jackchenTask.getId());//jackchen完成任务
  
           //被jackchen签收后，henryyan不再拥有该任务
           henryyanTask = taskService.createTaskQuery().taskCandidateUser("henryyan").singleResult();
           assertNull(henryyanTask);
       }
   }
   ```

## 10.部署流程资源及资源读取
### 10.1.流程资源
流程资源常用的有以下几种：
1. 流程定义文件：拓展名为bpmn20.xml和bpmn
2. 流程定义的图片：拓展名为PNG
3. 表单文件：拓展名为form
4. 规则文件：拓展名为drl

部署流程资源的时候，要注意一点：
引擎会根据不同的拓展名进行不同的处理。bpmn或bpmn20.xml类型的文件，会在ACT_RU_PROCDEF（流程定义表）、ACT_GE_BYTEARRAY（字节流表）两个表中都插入一条数据。而png类型的文件，只会在ACT_GE_BYTEARRAY表中插入一条数据。

所以可以看到，下文中所有部署，assertEquals（int，processDefinitionQuery）中的int都没有包含png的数目在内。
### 10.2.部署流程资源
启动一个流程实例processInstance，需要首先部署流程定义processDefinition，流程定义由许多活动Activity组成。

部署流程定义的方法有很多种，包括：
1. classpath方式：addClasspathResource（）
2. InputStream方式：addInputStream（）
3. 字符串方式：addString（）
4. zip/bar方式：addZipInputStream（）

#### 10.2.1.classpath方式
一般用在测试环节，真实的产品环境中很少用到这种方式。一般是管理页面手动部署或者设计完流程后直接部署到engine中。
项目中资源文件存放的位置：

![Image [53]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505184317.png)

示例代码（为了方便说明，没有使用链式编程，而是把每一步都分开写了）：

```
public class ClasspathDeploymentTest extends AbstractTest{
    @Test
    public void testClasspathDeployment throws Exception(){
        String bpmnClasspath = "chapter5/candidateUserInUserTask.bpmn";
        String pngClasspath = "chapter5/candidateUserInUserTask.bng";

        DeploymentBuilder db = repositoryService.createDeployment();
        db.addClasspathResource(bpmnClasspath);
        db.addClasspathResource(pngClasspath);
        db.deploy();

        //验证部署是否成功
        ProcessDefinitionQuery pdq = repositoryService.createProcessDefinitionQuery();
        long count = pdq.processDefinitionKey("candidateUserInUserTask").count;
        assertEquals(1,count);

        //读取图片文件
        ProcessDefinition pd = ProcessDefinitionQuery.singleResult();
        String diagramResourceName = pd.getDiagramResourceName();
        assertEquals(pngClasspath,diagramResourceName);
    }    
}
```
#### 10.2.2.InputStream方式
InputStream方式在产品环境中用的比较多，比如从web客户端接受一个文件对象，或者从URL中获取文件流，最后部署到engine中。

InputStream方式需要传入一个输入流及资源的名称。输入流的来源不限，可以是绝对路径，可以是classpath，可以是网络获取。

（从绝对路径获取）示例代码：
```
public class InputStreamDeploymentTest extends AbstractTest{
    @Test
    public void testInputStreamFromAbsoluteFilePath() throws Exception{
        String filePath = "/Users/henryyan/work/books/aia-books/aia-codes/bpmn20-example/src/test/
        resources/chapter5/userAndGroupInUserTask.bpmn";

        FileInputStream fis = new FileInputStream(filePath);
        repositoryService.createDeployment()
            .addInputStream("userAndGroupInUserTask.bpmn",fis)
            .deploy();//用userAndGroupInUserTask.bpmn作为资源名称

        //验证部署是否成功
        ProcessDefinitionQuery pdq = repositoryService.createProcessDefinitionQuery();
        long count = pdq.processDefinitionKey("userAndGroupInUserTask").count;
        assertEquals(1,count);
    }
}
```
#### 10.2.3.字符串方式
字符串方式是直接传入纯文本作为资源的来源。
示例代码：
```
public class StringDeploymentTest extends AbstractTest{
    //完整的text内容略
    private String text = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><definitions>...</definitions>";
    
    @Test
    public void testCharsDeployment(){//用candidateUserInUserTask.bpmn作为资源名称
        repositoryService.createDeployment().addString("candidateUserInUserTask.bpmn",text).deploy();

        //验证部署是否成功
        ProcessDefinitionQuery pdq = repositoryService.createProcessDefinitionQuery();
        long count = pdq.processDefinitionKey("candidateUserInUserTask").count;
        assertEquals(1,count);
    }
}
```
#### 10.2.4.zip/bar格式压缩包方式
前面三种方式一次都只能部署一个资源。除非执行多次deployment.addXXX()方法。

zip/bar方式，允许用户将资源打包，一次性部署多个资源。这几个资源关联的部署ID相同，因为属于同一次部署。

将文件打包为bar或者zip均可：

![Image [54]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505184331.png)

示例代码：

这里的addZipInputStream（）相当于for循环了n次addInputStream（）。

且这四个资源关联的是同一个deploymentID。

```
public class ZipStreamDeploymentTest extends AbstractTest{
    @Test
    public class testZipStreamFromAbsoluteFilePath(){
        InputStream zipStream = getClass().getClassLoader().getResourceAsStream("chapter5/chapter5-deployment.bar");
        repositoryService.createDeployment().addZipInputStream(new ZipInputStream(zipStream)).deploy();
        
        //验证部署是否成功
        long count = repositoryService.createProcessDefinitionQuery().count();
        assertEquals(2,count);//注意，不是4！！！

        //查询部署记录
        Deployment deployment = repositoryService.createDeploymentQuery().singleResult();
        assertNotNull(deployment);

        //验证四个文件均部署成功，且属于同一个部署ID
        String deploymentID = deployment.getId();
        assertNotNull(repositoryService.getResourceAsStream(deploymentID,"candidateUserInUserTask.bpmn"));
        assertNotNull(repositoryService.getResourceAsStream(deploymentID,"candidateUserInUserTask.png"));
        assertNotNull(repositoryService.getResourceAsStream(deploymentID,"userAndGroupInUserTask.bpmn"));
        assertNotNull(repositoryService.getResourceAsStream(deploymentID,"userAndGroupInUserTask.png"));
    }
}
```
### 10.3.流程资源读取
#### 10.3.1.读取已部署的processdefinition
假设现已有了一个页面，用于浏览及管理processDefinition。ID是流程定义id，DID是部署ID，名称是流程定义名称，KEY是流程定义KEY。XML和图片是流程定义的资源文件。

这个页面的访问路径是/chapter5-oa-manager/chapter5/process-list。

对应的jsp文件是webapp/WEB-INF/views/chapter5/process-list.jsp。

![Image [55]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505184346.png)

1. process-list.jsp文件

	完整代码与重点代码如下：

    ```
    <%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
    <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

    <html xmlns="http://www.w3.org/1999/xhtml">
    <head>
        <%@ include file="/common/global.jsp"%>
        <%@ include file="/common/meta.jsp" %>
        <%@ include file="/common/include-base-styles.jsp" %>
        <title>已部署流程定义列表--chapter5</title>
    </head>
    <body>
        <table width="100%" class="table table-bordered table-hover table-condensed">
            <thead>
                <tr>
                    <th>流程定义ID</th>
                    <th>部署ID</th>
                    <th>流程定义名称</th>
                    <th>流程定义KEY</th>
                    <th>版本号</th>
                    <th>XML资源名称</th>
                    <th>图片资源名称</th>
                    <th width="80">操作</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach items="${processDefinitionList }" var="pd">
                    <tr>
                        <td>${pd.id }</td>
                        <td>${pd.deploymentId }</td>
                        <td>${pd.name }</td>
                        <td>${pd.key }</td>
                        <td>${pd.version }</td>
                        <td>${pd.resourceName }</td>
                        <td>${pd.diagramResourceName }</td>
                </c:forEach>
            </tbody>
        </table>
    </body>
    </html>
    ```

2. DeploymentController

	完整代码和重点代码如下：

    ```
    @Controller
    @RequestMapping(value = "/chapter5")
    public class DeploymentController extends AbstractController {

        RepositoryService repositoryService = processEngine.getRepositoryService();

        /**
         * 流程定义列表
         */
        @RequestMapping(value = "/process-list")
        public ModelAndView processList() {

            // 对应WEB-INF/views/chapter5/process-list.jsp
            ModelAndView mav = new ModelAndView("chapter5/process-list");
            List<ProcessDefinition> processDefinitionList = repositoryService.createProcessDefinitionQuery().list();
            mav.addObject("processDefinitionList", processDefinitionList);
            return mav;
        }
    }
    ```

#### 10.3.2.从客户端部署流程
上节中可以获取到已经部署的流程，展示在列表里。这一节，通过web界面或其他客户端来部署processDefinition。这里采用上传文件的方式。
1. process-list.jsp

	添加代码如下：

    ```
    <body>
        <fieldset id="deployFieldset">
            <legend>部署流程资源</legend>
            <span class="alert alert-info"><b>支持文件格式：</b>zip、bar、bpmn、bpmn20.xml</span>
            <form action="${ctx }/chapter5/deploy" method="post" enctype="multipart/form-data" style="margin-top:1em;">
                <input type="file" name="file" />
                <input type="submit" value="Submit" class="btn" />
            </form>
            <hr class="soften" />
        </fieldset>
        ...
    </body>
    ```

2. DeploymentController

	添加方法如下：（类的路径是/chapter5，方法的路径是/deploy）

    ```
    /**
         * 部署流程资源
         */
        @RequestMapping(value = "/deploy")
        public String deploy(@RequestParam(value = "file", required = true) MultipartFile file) {
            // 获取上传的文件名
            String fileName = file.getOriginalFilename();

            try {
                // 得到输入流（字节流）对象
                InputStream fileInputStream = file.getInputStream();
                // 文件的扩展名
                String extension = FilenameUtils.getExtension(fileName);
                // zip或者bar类型的文件用ZipInputStream方式部署
                DeploymentBuilder deployment = repositoryService.createDeployment();
                if (extension.equals("zip") || extension.equals("bar")) {
                    ZipInputStream zip = new ZipInputStream(fileInputStream);
                    deployment.addZipInputStream(zip);
                } else {
                    // 其他类型的文件直接部署
                    deployment.addInputStream(fileName, fileInputStream);
                }
                deployment.deploy();
            } catch (Exception e) {
                logger.error("error on deploy process, because of file input stream");
            }

            return "redirect:process-list";// 回到列表页
        }
    ```

#### 10.3.3.读取流程定义的XML
现在增加一个功能，为列表中的"XML资源名称"添加一个链接，单击时，可以查看流程定义的XML文件内容。
1. process-list.jsp

	修改列的设置。

    ```
    <!--原来的代码
    <td>${pd.resourceName }</td>
    -->

    <!--修改如下-->
    <td>
        <a target="_blank" href='${ctx }/chapter5/read-resource?pdid=${pd.id }&resourceName=${pd.resourceName }'>${pd.resourceName }
        </a>
    </td>
    ```

2. DeploymentController

    添加方法如下：（类的路径是/chapter5，方法的路径是/read-resource）

    ```
    /**
         * 读取流程资源
         *
         * @param processDefinitionId 流程定义ID
         * @param resourceName        资源名称
         */
        @RequestMapping(value = "/read-resource")
        public void readResource(@RequestParam("pdid") String processDefinitionId, 
                                 @RequestParam("resourceName") String resourceName,      
                                 HttpServletResponse response)throws Exception {
            ProcessDefinitionQuery pdq = repositoryService.createProcessDefinitionQuery();
            ProcessDefinition pd = pdq.processDefinitionId(processDefinitionId).singleResult();

            // 通过接口读取
            InputStream resourceAsStream = repositoryService.getResourceAsStream(pd.getDeploymentId(), resourceName);

            // 输出资源内容到相应对象
            byte[] b = new byte[1024];
            int len = -1;
            while ((len = resourceAsStream.read(b, 0, 1024)) != -1) {
                response.getOutputStream().write(b, 0, len);
            }
        }
    ```
    
    运行效果
    
    点击&nbsp;![Image [56]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505184555.png)，跳转到页面：
    `localhost:8080/chapter5-oa-manager/chapter5/read-resource?pdid=candidateUserInUserTask:1:4&resourceName=candidateUserInUserTask.bpmn`
    
    xml文件的内容被完整显示。
    
    ![Image [57]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505184628.png)

#### 10.3.4.读取流程定义的图片文件
不管是bpmn还是png，对engine来说，都是资源文件。所以这里和前面的读取XML文件，所完成的功能类似。因此，二者在Controller里的方法是共用的。

所以只需修改jsp中的这句话为：注意，图片文件是diagramResource
```
<td>
    <a target="_blank" href='${ctx }/chapter5/read-resource?pdid=${pd.id }&resourceName=${pd.diagramResourceName }'>${pd.diagramResourceName }
    </a>
</td>
```
#### 10.3.5.读取流程定义图片时出现的中文乱码问题
读取XML文件和图片文件的区别：部署一个processDefinition时，bpmn文件是一定存在的，但是png文件可以没有。此时，engine会进行判定，如果没有部署时读取的资源里不包含png文件，它会自动生成同名文件。

因此会造成两个问题：坐标遗失及中文乱码。

上面读取xml内容时，显然内容中是没有坐标信息的，即engine帮我们自动生成的图片，布局的位置无法与bpmn中一模一样。同时，activiti默认的字体是Arial，在windows下默认自带宋体。所以转换时，会出现乱码。

1. 坐标遗失

  解决办法：部署时，bpmn文件和图片文件打包为zip/bar同时部署。不要让activiti为我们在部署的时候才自动生成。（Eclipse插件activiti designer中进行流程绘制时，可以设置为点击保存bpmn的同时立即生成png图片。在这个插件中绘制，查看bpmn文件的xml内容时 ，可以看到坐标信息。）

2. 中文乱码

    乱码解决办法1：

    修改类org.activiti.engine.impl.bpmn.diagram.ProcessDiagramCanvas的其中一句代码如下：

    ```
    Font font = new Font("simsun",Font.BOLD,11);//宋体是simsun
    ```

    缺点：动了源代码，导致使用activiti的升级版本时，又要做同样修改。

    乱码解决办法2：

    在processConfiguration的配置中增加这段：

    ```
    <bean id="processEngineConfiguration" class="...">
         <!--修改字体-->
         <property name="activityFontName" value="宋体">
         <property name="labelFontName" value="宋体">
    </bean>
    ```

    乱码解决办法3：

    部署时，bpmn文件和图片文件打包为zip/bar同时部署。不要让activiti为我们在部署的时候才自动生成。

综上可以看到：部署时，bpmn文件和图片文件打包为zip/bar同时部署，可以同时解决这两个问题。其他一些更细微的问题，比如显示不全，也可以解决。所以，最后的结论就是，将bpmn和png一起打包部署吧。

#### 10.3.6.删除部署
1. process-list.jsp

    添加一列，用来执行删除操作。

    ```
    <td><a target="_blank" href='${ctx }/chapter5/delete-deployment?deploymentId=${pd.deploymentId }'>删除</a></td>
    ```

2. DeploymentController

    添加方法如下：（类的路径是/chapter5，方法的路径是/delete-deployment）

    ```
    /**
         * 删除部署的流程，级联删除流程实例
         *
         * @param deploymentId 流程部署ID
         */
        @RequestMapping(value = "/delete-deployment")
        public String deleteProcessDefinition(@RequestParam("deploymentId") String deploymentId) {
            repositoryService.deleteDeployment(deploymentId, true);//true表示同时把与流程相关的数据也一并删除
            return "redirect:process-list";
        }
    ```

## 11.任务表单
### 11.1.请假流程图
![Image [58]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505184854.png)
### 11.2.动态表单
```
<activiti:formProperty>
```
### 11.3.外置表单
外置表单的特点：
1. 页面的原样显示
2. 字段值的自动填充
#### 11.3.1.流程定义
1. form文件

	![Image [59]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505184907.png)

	leave-start.form作为示例展示（字段要和后面代码中variables变量的key互相对应）：
	
    ```
    <div class="control-group">
        <label class="control-label" for="startDate">开始时间：</label>
        <div class="controls">
            <input type="text" id="startDate" name="startDate" class="datepicker" data-date-format="yyyy-mm-dd" required />
        </div>
    </div>
    <div class="control-group">
        <label class="control-label" for="endDate">结束时间：</label>
        <div class="controls">
            <input type="text" id="endDate" name="endDate" class="datepicker" data-date-format="yyyy-mm-dd" required />
        </div>
    </div>
    <div class="control-group">
        <label class="control-label" for="reason">请假原因：</label>
        <div class="controls">
            <textarea id="reason" name="reason" required></textarea>
        </div>
    </div>
    ```

2. 流程文件

    这里只显示部分xml内容，其他的一些见上节动态表单。这里的xml文件只是为了展示外置表单的使用方法。

    基本使用方式就是：activiti:formkey="chapter6/leave-formkey/approve.form" 

    form的值支持动态设置：activiti:formkey="${fooFormName}.form"

    ```
    <process id="leave-formkey" name="请假流程-外置表单">
        <startEvent id="startevent1" name="Start" 
                    activiti:initiator="applyUserId"
                    activiti:formkey="chapter6/leave-formkey/leave-start.form">
        </startEvent>
        <userTask id="deptLeaderVerify" name="部门经理审批" 
                    activiti:candidateGroups="deptLeader"
                    activiti:formkey="chapter6/leave-formkey/approve.form">
        </userTask>
        <userTask id="hrVerify" name="人事审批" 
                    activiti:candidateGroups="hr"
                    activiti:formkey="chapter6/leave-formkey/approve.form">
        </userTask>
        <userTask id="reportBack" name="销假" 
                    activiti:assignee="${applyUserId}"
                    activiti:formkey="chapter6/leave-formkey/report-back.form">
        </userTask>
        <userTask id="modifyApply" name="调整申请内容" 
                    activiti:assignee="${applyUserId}"
                    activiti:formkey="chapter6/leave-formkey/modify-apply.form">
        </userTask>
        <endEvent id="endevent1" name="End" 
        </endEvent>
    </process>
    ```

#### 11.3.2.单元测试
部署表单流程时需要把bpmn文件和form文件同时打包部署。这样部署了同名的form文件时多个流程定义，或相同流程不同版本之间，都不会有冲突。
```
public class LeaveFormKeyTest extends AbstractTest {

    @Test
    @Deployment(resources = {"chapter6/leave-formkey/leave-formkey.bpmn", 
            "chapter6/leave-formkey/leave-start.form",
            "chapter6/leave-formkey/approve-deptLeader.form", 
            "chapter6/leave-formkey/approve-hr.form", 
            "chapter6/leave-formkey/report-back.form",
            "chapter6/leave-formkey/modify-apply.form"})

    public void allPass() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Map<String, String> variables = new HashMap<String, String>();
        Calendar ca = Calendar.getInstance();
        String startDate = sdf.format(ca.getTime());
        ca.add(Calendar.DAY_OF_MONTH, 2); // 当前日期加2天
        String endDate = sdf.format(ca.getTime());

        // 启动流程
        variables.put("startDate", startDate);
        variables.put("endDate", endDate);
        variables.put("reason", "公休");

        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

        // 读取启动表单
        Object renderedStartForm = formService.getRenderedStartForm(processDefinition.getId());
        assertNotNull(renderedStartForm);

        // 启动流程
        // 设置当前用户
        String currentUserId = "henryyan";
        identityService.setAuthenticatedUserId(currentUserId);
        ProcessInstance processInstance = formService.submitStartFormData(processDefinition.getId(), variables);
        assertNotNull(processInstance);

        // 部门领导审批通过
        Task deptLeaderTask = taskService.createTaskQuery().taskCandidateGroup("deptLeader").singleResult();
        assertNotNull(formService.getRenderedTaskForm(deptLeaderTask.getId()));
        variables = new HashMap<String, String>();
        variables.put("deptLeaderApproved", "true");
        formService.submitTaskFormData(deptLeaderTask.getId(), variables);

        // 人事审批通过
        Task hrTask = taskService.createTaskQuery().taskCandidateGroup("hr").singleResult();
        assertNotNull(formService.getRenderedTaskForm(hrTask.getId()));// 读取任务表单
        variables = new HashMap<String, String>();
        variables.put("hrApproved", "true");
        formService.submitTaskFormData(hrTask.getId(), variables);

        // 销假（根据申请人的用户ID读取）
        Task reportBackTask = taskService.createTaskQuery().taskAssignee(currentUserId).singleResult();
        assertNotNull(formService.getRenderedTaskForm(reportBackTask.getId()));
        variables = new HashMap<String, String>();
        variables.put("reportBackDate", sdf.format(ca.getTime()));
        formService.submitTaskFormData(reportBackTask.getId(), variables);

        // 验证流程是否已经结束
        HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().finished().singleResult();
        assertNotNull(historicProcessInstance);

        // 读取历史变量
        Map<String, Object> historyVariables = packageVariables(processInstance);

        // 验证执行结果
        assertEquals("ok", historyVariables.get("result"));

    }
...
}
```
#### 11.3.3.自定义表单引擎
activiti既可以可以支持B/S结构的应用，也可以支持C/S结构的应用。getRenderd***Form()返回的内容是经过activiti的默认Form引擎处理过的，返回的值可以让B/S结构的应用直接使用，但是却不能直接支持C/S结构的应用。所以如果要生成C/S程序需要的java控件，需要事先自定义的form引擎。

####  11.3.4.读取流程启动表单
Activiti Explorer支持动态表单，却不支持外置表单。所以需要为Activiti Explorer增加外置表单支持。

## 12.activiti modeler



## 13.activiti-explorer



## 14.相关概念
### 14.1.委派和转办
#### 14.1.1.委派
委派：是将任务节点分给其他人处理，等其他人处理好之后，委派任务会自动回到委派人的任务中 

![Image [60]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505185012.png)

将hr的任务进行委派：

```
// taskId 任务id; userId:被委派人id
taskService.delegateTask(taskId, userId);
```
![Image [61]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505185021.png)

被委派人办理任务后：

```
taskService.resolveTask(taskId,variables);
```
正在运行的任务表中被委派人办理任务后hr的任务会回到委派人xxhr ，历史任务表中也一样

![Image [62]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505185031.png)

查询委派任务：

根据字段owner查询

```
//正在运行的委派任务：
public ListmytaskChangeOtherList1(String userId){

    List<Task>list = taskService.createTaskQuery().taskOwner(userId)

            .orderByTaskCreateTime().desc().list();

    returnlist;
}
```
#### 14.1.2.转办
直接将办理人assignee 换成别人，这时任务的拥有着不再是转办人，而是为空，相当与将任务转出。

直接将assignee =” zhuanban”       taskService.setAssignee(taskId, userId); 

![Image [63]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505185040.png)

```
//转他人处理
public void  mytaskChangeOther(String taskId,String userId){
    taskService.setAssignee(taskId,userId); 
}
```

如果要查询转给他人处理的任务：
可以同时将OWNER进行设置（正在运行的任务表和历史任务表都要进行设置），这样在查询的时候方便根据字段进行查询：以下是正在进行的任务表例子
```
taskService.setOwner(taskId, userId);
```
![Image [64]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505185049.png)

![Image [65]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505185054.png)

## X.问题
### X.1.Springboot2.0 项目中TKmybatis和Activiti集成的问题
版本信息：springboot 2.0，mybatis 3.4.6 , tkmybatis 2.0 ,activiti 6.0

今天在集成项目的时候，发现mybatis和activiti集成时不会报错，但是mapper和activiti集成到一起时启动就会报错，错误信息如下：
```
Parameter 1 of method springProcessEngineConfiguration in org.activiti.spring.boot.JpaProcessEngineAutoConfiguration$JpaConfiguration required a bean of type 'javax.persistence.EntityManagerFactory' that could not be found.
```
于是查看了一下JpaProcessEngineAutoConfiguration类，部分代码如下：
```
public class JpaProcessEngineAutoConfiguration {
    public JpaProcessEngineAutoConfiguration() {
    }
 
    @Configuration
    @ConditionalOnClass(
        name = {"javax.persistence.EntityManagerFactory"}
    )
    @EnableConfigurationProperties({ActivitiProperties.class})
    public static class JpaConfiguration extends AbstractProcessEngineAutoConfiguration {
        public JpaConfiguration() {
        }
    }
}
```
可以看到EntityManagerFactory是被@ConditionlOnClass所注解的。而EntityManagerFactory是来自于JPA相关的接口。其实这里是Activiti所做的判断，如果项目使用了JPA，那走JPA，如果没有，则走Mybatis。所以只引入Mybatis和Activiti的话项目不会报错，那为什么引入了Mapper就会报错呢？

继续看mapper的源码就能知道原因，其实mapper并没有实现EntityManagerFactory接口，而是自己写了一套，而在Activiti中则认为当前项目使用的是JPA，找不到EntityManagerFactory的实现类。所以报错。解决方法就是在mapper中移除对persistence-api依赖，在activiti中加上jpa的依赖。这样的话，项目启动不会报错，并且能正常使用tkmybatis，省去了公共的增删改查代码。

修改后的pom.xml如下：
```
        <!-- 集成tk-mapper -->
        <dependency>
            <groupId>tk.mybatis</groupId>
            <artifactId>mapper-spring-boot-starter</artifactId>
            <version>${tk.mapper.version}</version>
            <exclusions>
                <exclusion>
                    <artifactId>persistence-api</artifactId>
                    <groupId>javax.persistence</groupId>
                </exclusion>
            </exclusions>
        </dependency>
        <!-- activiti -->
        <dependency>
            <groupId>org.activiti</groupId>
            <artifactId>activiti-spring-boot-starter-basic</artifactId>
            <version>${activiti.version}</version>
        </dependency>
        <dependency>
            <groupId>org.activiti</groupId>
            <artifactId>activiti-spring-boot-starter-jpa</artifactId>
            <version>${activiti.version}</version>
        </dependency>
```
其他的配置按照mapper和activiti官网上的来就OK了。

### X.2.Spring Boot 2 与Activiti 7 整合出现打开URL出现signin问题
pring Boot 2 与Activiti 7 整合出现，打开URL出现signin问题，界面如下：

![Image [66]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505185105.png)

感觉有点莫名其妙，在pom.xml中没有引用spring-boot-starter-security。通过查看maven项目依赖jar中，确实有security相关的包。
后来查看，项目的dependencies关系，发现是activiti-spring-boot-starter包的依赖项中存在。先记录一下，还没有找到解决方法。
希望有高手能花指点一下。主要暂时还不想学security。