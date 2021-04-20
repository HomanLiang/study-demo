[toc]



# 解释器模式

## 1.模式意图

自定义某种语言后，给定一种文法标准，定义解释器，进行解析。

做过搜索的朋友们可能更了解一些，平时我们搜索所需要的词库，通常就需要用这种方式来实现。

## 2.应用场景

- 有复杂的语法分析场景
- 需要高效的解释，胜过快速的效率（即看中解释的结果，而放弃效率）

## 3.模式结构

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210420231647.jpeg)

**Expression** 语法解释器的抽线模型

```
/**
 * 指定抽象表达式，具体表达式必须实现的方法
 * @author xingoo
 *
 */
abstract class Expression{
    /**
     * 以上下文环境为准，解释指定的表达式
     * @param ctx
     * @return
     */
    public abstract boolean interpret(Context ctx);
    /**
     * 检验两个表达式，是否相同
     */
    public abstract boolean equals(Object o);
    /**
     * 返回表达式代表的hashCode
     */
    public abstract int hashCode();
    /**
     * 转换成字符串
     */
    public abstract String toString();
}
```

以下是具体的解释器实现过程，这里主要模仿JAVA模式中的例子

```
/**
 * 常量表达式
 * @author xingoo
 *
 */
class Constant extends Expression{
    private boolean value;
    public Constant(boolean value){
        this.value = value;
    }
    public boolean interpret(Context ctx){
        return value;
    }
    public boolean equals(Object o){
        if(o!=null && o instanceof Constant){
            return this.value == ((Constant)o).value;
        }
        return false;
    }
    public int hashCode() {
        return (this.toString()).hashCode();
    }
    public String toString() {
        return new Boolean(value).toString();
    }
}
/**
 * 变量表达式
 * @author xingoo
 *
 */
class Variable extends Expression{
    private String name;
    
    public Variable(String name){
        this.name = name;
    }
    
    public boolean interpret(Context ctx) {
        return ctx.lookup(this);
    }
    
    public boolean equals(Object o) {
        if(o!=null && o instanceof Variable){
            return this.name.equals(((Variable)o).name);
        }
        return false;
    }

    public int hashCode() {
        return (this.toString()).hashCode();
    }
    public String toString() {
        return name;
    }
    
}
/**
 * 与 表达式
 * @author xingoo
 *
 */
class And extends Expression{
    private Expression left,right;
    
    public And(Expression left,Expression right){
        this.left = left;
        this.right = right;
    }
    
    public boolean interpret(Context ctx) {
        return left.interpret(ctx) && right.interpret(ctx);
    }

    @Override
    public boolean equals(Object o) {
        if(o!=null && o instanceof And){
            return this.left.equals(((And)o).left) && this.right.equals(((And)o).right);
        }
        return false;
    }

    public int hashCode() {
        return (this.toString()).hashCode();
    }

    public String toString() {
        return "("+left.toString()+" AND "+right.toString()+")";
    }
}
/**
 * 或 表达式
 * @author xingoo
 *
 */
class Or extends Expression{
    private Expression left,right;
    
    public Or(Expression left,Expression right){
        this.left = left;
        this.right = right;
    }
    
    public boolean interpret(Context ctx) {
        return left.interpret(ctx) || right.interpret(ctx);
    }

    public boolean equals(Object o) {
        if(o!=null && o instanceof Or){
            return this.left.equals(((Or)o).left) && this.right.equals(((Or)o).right);
        }
        return false;
    }

    public int hashCode() {
        return (this.toString()).hashCode();
    }

    public String toString() {
        return "("+left.toString()+" Or "+right.toString()+")";
    }
}
/**
 * 非 表达式
 * @author xingoo
 *
 */
class Not extends Expression{
    private Expression exp;
    
    public Not(Expression exp){
        this.exp = exp;
    }
    
    public boolean interpret(Context ctx) {
        return !exp.interpret(ctx);
    }

    public boolean equals(Object o) {
        if(o!=null && o instanceof Not){
            return this.exp.equals(((Not)o).exp);
        }
        return false;
    }

    public int hashCode() {
        return (this.toString()).hashCode();
    }

    public String toString() {
        return "(Not "+exp.toString()+")";
    }
    
}
```

**Context** 上下文环境，存储一些表达式的内容

```
/**
 * 上下文环境
 * @author xingoo
 *
 */
class Context{
    
    private HashMap map = new HashMap();
    
    public void assign(Variable var,boolean value){
        map.put(var, new Boolean(value));
    }
    
    public boolean lookup(Variable var) throws IllegalArgumentException{
        Boolean value = (Boolean)map.get(var);
        if(value == null){
            throw new IllegalArgumentException();
        }
        return value.booleanValue();
    }
}
```

全部代码

```
package com.xingoo.interpreter;

import java.util.HashMap;
/**
 * 指定抽象表达式，具体表达式必须实现的方法
 * @author xingoo
 *
 */
abstract class Expression{
    /**
     * 以上下文环境为准，解释指定的表达式
     * @param ctx
     * @return
     */
    public abstract boolean interpret(Context ctx);
    /**
     * 检验两个表达式，是否相同
     */
    public abstract boolean equals(Object o);
    /**
     * 返回表达式代表的hashCode
     */
    public abstract int hashCode();
    /**
     * 转换成字符串
     */
    public abstract String toString();
}
/**
 * 常量表达式
 * @author xingoo
 *
 */
class Constant extends Expression{
    private boolean value;
    public Constant(boolean value){
        this.value = value;
    }
    public boolean interpret(Context ctx){
        return value;
    }
    public boolean equals(Object o){
        if(o!=null && o instanceof Constant){
            return this.value == ((Constant)o).value;
        }
        return false;
    }
    public int hashCode() {
        return (this.toString()).hashCode();
    }
    public String toString() {
        return new Boolean(value).toString();
    }
}
/**
 * 变量表达式
 * @author xingoo
 *
 */
class Variable extends Expression{
    private String name;

    public Variable(String name){
        this.name = name;
    }

    public boolean interpret(Context ctx) {
        return ctx.lookup(this);
    }

    public boolean equals(Object o) {
        if(o!=null && o instanceof Variable){
            return this.name.equals(((Variable)o).name);
        }
        return false;
    }

    public int hashCode() {
        return (this.toString()).hashCode();
    }
    public String toString() {
        return name;
    }

}
/**
 * 与 表达式
 * @author xingoo
 *
 */
class And extends Expression{
    private Expression left,right;

    public And(Expression left,Expression right){
        this.left = left;
        this.right = right;
    }

    public boolean interpret(Context ctx) {
        return left.interpret(ctx) && right.interpret(ctx);
    }

    @Override
    public boolean equals(Object o) {
        if(o!=null && o instanceof And){
            return this.left.equals(((And)o).left) && this.right.equals(((And)o).right);
        }
        return false;
    }

    public int hashCode() {
        return (this.toString()).hashCode();
    }

    public String toString() {
        return "("+left.toString()+" AND "+right.toString()+")";
    }
}
/**
 * 或 表达式
 * @author xingoo
 *
 */
class Or extends Expression{
    private Expression left,right;

    public Or(Expression left,Expression right){
        this.left = left;
        this.right = right;
    }

    public boolean interpret(Context ctx) {
        return left.interpret(ctx) || right.interpret(ctx);
    }

    public boolean equals(Object o) {
        if(o!=null && o instanceof Or){
            return this.left.equals(((Or)o).left) && this.right.equals(((Or)o).right);
        }
        return false;
    }

    public int hashCode() {
        return (this.toString()).hashCode();
    }

    public String toString() {
        return "("+left.toString()+" Or "+right.toString()+")";
    }
}
/**
 * 非 表达式
 * @author xingoo
 *
 */
class Not extends Expression{
    private Expression exp;

    public Not(Expression exp){
        this.exp = exp;
    }

    public boolean interpret(Context ctx) {
        return !exp.interpret(ctx);
    }

    public boolean equals(Object o) {
        if(o!=null && o instanceof Not){
            return this.exp.equals(((Not)o).exp);
        }
        return false;
    }

    public int hashCode() {
        return (this.toString()).hashCode();
    }

    public String toString() {
        return "(Not "+exp.toString()+")";
    }

}
/**
 * 上下文环境
 * @author xingoo
 *
 */
class Context{

    private HashMap map = new HashMap();

    public void assign(Variable var,boolean value){
        map.put(var, new Boolean(value));
    }

    public boolean lookup(Variable var) throws IllegalArgumentException{
        Boolean value = (Boolean)map.get(var);
        if(value == null){
            throw new IllegalArgumentException();
        }
        return value.booleanValue();
    }
}
public class Client {
    private static Context ctx;
    private static Expression exp;
    public static void main(String[] args) {
        ctx = new Context();
        Variable x = new Variable("x");
        Variable y = new Variable("y");

        Constant c = new Constant(true);

        //放入上下文中
        ctx.assign(x, false);
        ctx.assign(y, true);

        exp = new Or(new And(c,x),new And(y,new Not(x)));
        System.out.println("x = "+x.interpret(ctx));
        System.out.println("y = "+y.interpret(ctx));
        System.out.println(exp.toString() +" = "+exp.interpret(ctx));
    }
}
```

运行结果

```
x = false
y = true
((true AND x) Or (y AND (Not x))) = true
```



