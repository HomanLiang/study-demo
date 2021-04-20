[toc]



# 备忘录模式

## 1.模式意图

这个模式主要是想通过一个对象来记录对象的某种状态，这样有利于在其他需要的场合进行恢复。

该模式还有跟多可以扩展的地方，比如可以记录多个时间的状态，每个角色都有可以扩展的空间，完全看业务场景而定。

## 2.应用场景

- 保存对象某一时刻的状态
- 避免直接暴露接口，破坏封装性

## 3.模式结构

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210420234143.jpeg)

**Originator** 是备忘录的发起者，记录状态的对象

```
class Originator{
    private String state;
    public Memento ceateMemento() {
        return new Memento(state);
    }
    public void restoreMemento(Memento memento) {
        this.state = memento.getState();
    }
    public String getState(){
        return this.state;
    }
    public void setState(String state){
        this.state = state;
        System.out.println("Current state = "+this.state);
    }
}
```

**Memento** 备忘录角色，通常用于保存某种状态

```
class Memento{
    private String state;
    public Memento(String state) {
        this.state = state;
    }
    public String getState() {
        return state;
    }
    public void setState(String state) {
        this.state = state;
    }
}
```

**Caretaker** 备忘录的负责人，负责在恰当的时机，进行状态的恢复

```
class Caretaker{
    private Memento memento;
    public Memento retrieveMemento(){
        return this.memento;
    }
    public void saveMemento(Memento memento){
        this.memento = memento;
    }
}
```

全部代码

```
package com.xingoo.test.design.memento;
class Originator{
    private String state;
    public Memento ceateMemento() {
        return new Memento(state);
    }
    public void restoreMemento(Memento memento) {
        this.state = memento.getState();
    }
    public String getState(){
        return this.state;
    }
    public void setState(String state){
        this.state = state;
        System.out.println("Current state = "+this.state);
    }
}
class Memento{
    private String state;
    public Memento(String state) {
        this.state = state;
    }
    public String getState() {
        return state;
    }
    public void setState(String state) {
        this.state = state;
    }
}
class Caretaker{
    private Memento memento;
    public Memento retrieveMemento(){
        return this.memento;
    }
    public void saveMemento(Memento memento){
        this.memento = memento;
    }
}
public class Client {
    private static Originator o = new Originator();
    private static Caretaker c = new Caretaker();
    public static void main(String[] args) {
        o.setState("On");
        //记录状态
        c.saveMemento(o.ceateMemento());
        //更改状态
        o.setState("Off");
        //更新状态
        o.restoreMemento(c.retrieveMemento());
    }
}
```

运行结果

```
Current state = On
Current state = Off
```

## 4.生活中的设计模式

最近看了会 恶魔奶爸，挺扯淡的漫画。不过看到其中的女仆，让我想起了这种备忘录模式。

主人在有什么重要的事情时，都会交给女仆记着，规定的时间在提醒自己。

下面的主人就有一件很重要的事情，就是陪亲爱的小丽去看电影，于是他弄了一个笔记本，记录下了这个信息。女仆拿到笔记本，并在预先商量好的时间提醒主人。这里的笔记本就是上面的备忘录对象Memento，而这个模式中，主人就是备忘录的发起者，女仆是负责人。

这里涉及到的备忘录是属于【白箱】的，也就是说，**备忘录中的信息，可以被发起人和负责人看到**。还有一种是【黑箱】的，主要是用了一种内部类继承这个备忘录对象，这样**外部的负责人就得不到真正备忘录中的具体信息**。

下面看下具体的实现，主人的代码如下：

```
class Master{
    private String info;
    public String getInfo() {
        return info;
    }
    public void setInfo(String info) {
        this.info = info;
    }
    public Note createNote(String info){
        return new Note(info);
    }
    public void action(Note note){
        this.info = note.getInfo();
        System.out.println("主人看到笔记，记起了 "+ this.info);
    }
    public void toDo(){
        System.out.println("****主人正在..."+info);
    }
}
```

女仆的代码如下：

```
class Maid{
    private Note note;
    public Note readNote(){
        System.out.println("女仆拿到笔记本");
        return this.note;
    }
    public void writeNote(Note note){
        System.out.println("女仆写笔记");
        this.note = note;
    }
}
```

备忘录的代码如下：

```
class Note{
    private String info;
    public Note(String info) {
        this.info = info;
    }
    public void setInfo(String info){
        this.info = info;
        System.out.println("写笔记！");
    }
    public String getInfo(){
        System.out.println("读笔记！");
        return info;
    }
}
```

全部代码：

```
package com.xingoo.test.design.memento;
class Note{
    private String info;
    public Note(String info) {
        this.info = info;
    }
    public void setInfo(String info){
        this.info = info;
        System.out.println("写笔记！");
    }
    public String getInfo(){
        System.out.println("读笔记！");
        return info;
    }
}
class Master{
    private String info;
    public String getInfo() {
        return info;
    }
    public void setInfo(String info) {
        this.info = info;
    }
    public Note createNote(String info){
        return new Note(info);
    }
    public void action(Note note){
        this.info = note.getInfo();
        System.out.println("主人看到笔记，记起了 "+ this.info);
    }
    public void toDo(){
        System.out.println("****主人正在..."+info);
    }
}
class Maid{
    private Note note;
    public Note readNote(){
        System.out.println("女仆拿到笔记本");
        return this.note;
    }
    public void writeNote(Note note){
        System.out.println("女仆写笔记");
        this.note = note;
    }
}
public class LifeWithMaid {
    public static void main(String[] args) {
        Master master = new Master();
        Maid maid = new Maid();
        //主人想起了要做的事情
        maid.writeNote(master.createNote("晚上6点，配小丽看电影"));
        //主人忙其他的事情
        master.setInfo("睡觉吃饭打豆豆！");
        master.toDo();//主人正在做什么？
        //时间到了，女仆提醒主人
        master.action(maid.readNote());
        master.toDo();//主人正在做什么？
    }
}
```

运行结果

```
女仆写笔记
****主人正在...睡觉吃饭打豆豆！
女仆拿到笔记本
读笔记！
主人看到笔记，记起了 晚上6点，配小丽看电影
****主人正在...晚上6点，配小丽看电影
```

