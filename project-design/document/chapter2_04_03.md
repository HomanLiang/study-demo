[toc]



# 组合模式

## 1.模式意图

使对象组合成树形的结构。使用户对单个对象和组合对象的使用具有一致性。

## 2.应用场景

- 表示对象的 部分-整体 层次结构
- 忽略组合对象与单个对象的不同，统一的使用组合结构中的所有对象。

## 3.模式结构

**【安全的组合模式】**

这种组合模式，叶子节点，也就是单个对象不具有对象的控制功能。仅仅有简单的业务操作。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210420224300.jpeg)

```
package com.xingoo.composite.safe;

import java.util.ArrayList;
import java.util.List;

interface Component{
    Composite getCmposite();
    void sampleComposite();
}

class Leaf implements Component{

    public Composite getCmposite() {
        return null;
    }

    public void sampleComposite() {
        System.out.println("Leaf operation");
    }

}

class Composite implements Component{

    private List<Component> list = new ArrayList();

    public void add(Component component){
        list.add(component);
    }

    public void remove(Component component){
        list.remove(component);
    }

    public Composite getCmposite() {
        return this;
    }

    public void sampleComposite() {
        System.out.println("Composite operation");
        for(Component com : list){
            com.sampleComposite();
        }
    }

}
public class Client {
    public static void main(String[] args) {
        Component leaf1 = new Leaf();
        Component leaf2 = new Leaf();
        Component composite = new Composite();
        composite.getCmposite().add(leaf1);
        composite.getCmposite().add(leaf2);
        composite.getCmposite().sampleComposite();
    }
}
```

执行结果

```
Composite operation
Leaf operation
Leaf operation
```

**【透明的组合模式】**

这种组合模式，叶子节点与组合对象具有相同的方法，外表看来毫无差异。不过叶子节点的处理方法默认为空。忽略叶子节点，与组合对象的差异性。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210420224336.jpeg)

```
package com.xingoo.composite.transparent;

import java.util.ArrayList;
import java.util.List;

interface Component{
    public void SampleOperation();
    public void add(Component component);
    public void remove(Component component);
    public Component getComponent();
}

class Leaf implements Component{
    public void SampleOperation() {
        System.out.println("leaf operation!");
    }

    public void add(Component component) {

    }

    public void remove(Component component) {

    }

    public Component getComponent(){
        return this;
    }
}

class Composite implements Component{

    private List<Component> list = new ArrayList();

    public void SampleOperation() {
        System.out.println("composite operation!");
        for(Component com : list){
            com.getComponent().SampleOperation();
        }
    }

    public void add(Component component) {
        list.add(component);
    }

    public void remove(Component component) {
        list.remove(component);
    }

    public Component getComponent(){
        return this;
    }
}
public class Client {
    public static void main(String[] args) {
        Component leaf1 = new Leaf();
        Component leaf2 = new Leaf();
        Component leaf3 = new Leaf();
        Component composite1 = new Composite();
        Component composite = new Composite();

        composite1.add(leaf3);

        composite.getComponent().add(leaf1);
        composite.getComponent().add(leaf2);
        composite.getComponent().add(composite1);

        composite.getComponent().SampleOperation();
    }
}
```

本例中的结构层次

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210420224403.jpeg)

执行结果

```
composite operation!
leaf operation!
leaf operation!
composite operation!
leaf operation!
```



