[toc]



# Java Fork Join

**对于简单的并行任务，你可以通过“线程池 +Future”的方案来解决；如果任务之间有聚合关系，无论是 AND 聚合还是 OR 聚合，都可以通过 CompletableFuture 来解决；而批量的并行任务，则可以通过 CompletionService 来解决。**

## 1. CompletableFuture

### 1.1.什么是CompletableFuture？

Java 8 新特性

在Java中CompletableFuture用于异步编程，异步编程是编写非阻塞的代码，运行的任务在一个单独的线程，与主线程隔离，并且会通知主线程它的进度，成功或者失败。

在这种方式中，主线程不会被阻塞，不需要一直等到子线程完成。主线程可以并行的执行其他任务。

使用这种并行方式，可以极大的提高程序的性能。

### 1.2.Future vs CompletableFuture

CompletableFuture 是 [Future API](https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/Future.html) 的扩展。

Future 被用于作为一个异步计算结果的引用。提供一个 `isDone()` 方法来检查计算任务是否完成。当任务完成时，`get()` 方法用来接收计算任务的结果。

从 [Callbale和 Future 教程](https://www.callicoder.com/java-callable-and-future-tutorial/) 可以学习更多关于 Future 知识.

Future API 是非常好的 Java 异步编程进阶，但是它缺乏一些非常重要和有用的特性。

### 1.3.Future 的局限性

1. 不能手动完成 当你写了一个函数，用于通过一个远程API获取一个电子商务产品最新价格。因为这个 API 太耗时，你把它允许在一个独立的线程中，并且从你的函数中返回一个 Future。现在假设这个API服务宕机了，这时你想通过该产品的最新缓存价格手工完成这个Future 。你会发现无法这样做。
2. Future 的结果在非阻塞的情况下，不能执行更进一步的操作 Future 不会通知你它已经完成了，它提供了一个阻塞的 `get()` 方法通知你结果。你无法给 Future 植入一个回调函数，当 Future 结果可用的时候，用该回调函数自动的调用 Future 的结果。
3. 多个 Future 不能串联在一起组成链式调用 有时候你需要执行一个长时间运行的计算任务，并且当计算任务完成的时候，你需要把它的计算结果发送给另外一个长时间运行的计算任务等等。你会发现你无法使用 Future 创建这样的一个工作流。
4. 不能组合多个 Future 的结果 假设你有10个不同的Future，你想并行的运行，然后在它们运行未完成后运行一些函数。你会发现你也无法使用 Future 这样做。
5. 没有异常处理 Future API 没有任务的异常处理结构居然有如此多的限制，幸好我们有CompletableFuture，你可以使用 CompletableFuture 达到以上所有目的。

CompletableFuture 实现了 `Future`  和 `CompletionStage`接口，并且提供了许多关于创建，链式调用和组合多个 Future 的便利方法集，而且有广泛的异常处理支持。

### 1.4.创建 CompletableFuture

**1. 简单的例子** 可以使用如下无参构造函数简单的创建 CompletableFuture：

```
CompletableFuture<String> completableFuture = new CompletableFuture<String>();
```

这是一个最简单的 CompletableFuture，想获取CompletableFuture 的结果可以使用 `CompletableFuture.get()` 方法：

```
String result = completableFuture.get()
```

`get()` 方法会一直阻塞直到 Future 完成。因此，以上的调用将被永远阻塞，因为该Future一直不会完成。

你可以使用 `CompletableFuture.complete()` 手工的完成一个 Future：

```
completableFuture.complete("Future's Result")
```

所有等待这个 Future 的客户端都将得到一个指定的结果，并且 `completableFuture.complete()` 之后的调用将被忽略。

**2. 使用 `runAsync()` 运行异步计算** 如果你想异步的运行一个后台任务并且不想改任务返回任务东西，这时候可以使用 `CompletableFuture.runAsync()`方法，它持有一个[Runnable ](https://docs.oracle.com/javase/7/docs/api/java/lang/Runnable.html)对象，并返回 `CompletableFuture<Void>`。

```
// Run a task specified by a Runnable Object asynchronously.
CompletableFuture<Void> future = CompletableFuture.runAsync(new Runnable() {
    @Override
    public void run() {
        // Simulate a long-running Job
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
        System.out.println("I'll run in a separate thread than the main thread.");
    }
});

// Block and wait for the future to complete
future.get()
```

你也可以以 lambda 表达式的形式传入 Runnable 对象：

```
// Using Lambda Expression
CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
    // Simulate a long-running Job   
    try {
        TimeUnit.SECONDS.sleep(1);
    } catch (InterruptedException e) {
        throw new IllegalStateException(e);
    }
    System.out.println("I'll run in a separate thread than the main thread.");
});
```

在本文中，我使用lambda表达式会比较频繁，如果以前你没有使用过，建议你也多使用lambda 表达式。

**3. 使用 `supplyAsync()` 运行一个异步任务并且返回结果** 当任务不需要返回任何东西的时候， `CompletableFuture.runAsync()` 非常有用。但是如果你的后台任务需要返回一些结果应该要怎么样？

`CompletableFuture.supplyAsync()` 就是你的选择。它持有`supplier<T>` 并且返回`CompletableFuture<T>`，`T` 是通过调用 传入的supplier取得的值的类型。

```
// Run a task specified by a Supplier object asynchronously
CompletableFuture<String> future = CompletableFuture.supplyAsync(new Supplier<String>() {
    @Override
    public String get() {
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
        return "Result of the asynchronous computation";
    }
});

// Block and get the result of the Future
String result = future.get();
System.out.println(result);
```

`Supplier<T>` 是一个简单的函数式接口，表示supplier的结果。它有一个`get()`方法，该方法可以写入你的后台任务中，并且返回结果。

你可以使用lambda表达式使得上面的示例更加简明：

```
// Using Lambda Expression
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
    try {
        TimeUnit.SECONDS.sleep(1);
    } catch (InterruptedException e) {
        throw new IllegalStateException(e);
    }
    return "Result of the asynchronous computation";
});
```

> **一个关于Executor 和Thread Pool笔记** 你可能想知道，我们知道`runAsync()`和`supplyAsync()`方法在单独的线程中执行他们的任务。但是我们不会永远只创建一个线程。 CompletableFuture可以从全局的 [ForkJoinPool.commonPool()](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ForkJoinPool.html#commonPool--)获得一个线程中执行这些任务。 但是你也可以创建一个线程池并传给`runAsync()`和`supplyAsync()`方法来让他们从线程池中获取一个线程执行它们的任务。 CompletableFuture API 的所有方法都有两个变体-一个接受`Executor`作为参数，另一个不这样：

```
// Variations of runAsync() and supplyAsync() methods
static CompletableFuture<Void>  runAsync(Runnable runnable)
static CompletableFuture<Void>  runAsync(Runnable runnable, Executor executor)
static <U> CompletableFuture<U> supplyAsync(Supplier<U> supplier)
static <U> CompletableFuture<U> supplyAsync(Supplier<U> supplier, Executor executor)
```

创建一个线程池，并传递给其中一个方法：

```
Executor executor = Executors.newFixedThreadPool(10);
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
    try {
        TimeUnit.SECONDS.sleep(1);
    } catch (InterruptedException e) {
        throw new IllegalStateException(e);
    }
    return "Result of the asynchronous computation";
}, executor);
```

### 1.5.在 CompletableFuture 转换和运行

`CompletableFuture.get()` 方法是阻塞的。它会一直等到Future完成并且在完成后返回结果。 但是，这是我们想要的吗？对于构建异步系统，我们应该附上一个回调给CompletableFuture，当Future完成的时候，自动的获取结果。 如果我们不想等待结果返回，我们可以把需要等待Future完成执行的逻辑写入到回调函数中。

可以使用 `thenApply()`, `thenAccept()` 和`thenRun()`方法附上一个回调给CompletableFuture。

**1. thenApply()** 可以使用 `thenApply()` 处理和改变CompletableFuture的结果。持有一个`Function<R,T>`作为参数。`Function<R,T>`是一个简单的函数式接口，接受一个T类型的参数，产出一个R类型的结果。

```
// Create a CompletableFuture
CompletableFuture<String> whatsYourNameFuture = CompletableFuture.supplyAsync(() -> {
   try {
       TimeUnit.SECONDS.sleep(1);
   } catch (InterruptedException e) {
       throw new IllegalStateException(e);
   }
   return "Rajeev";
});

// Attach a callback to the Future using thenApply()
CompletableFuture<String> greetingFuture = whatsYourNameFuture.thenApply(name -> {
   return "Hello " + name;
});

// Block and get the result of the future.
System.out.println(greetingFuture.get()); // Hello Rajeev
```

你也可以通过附加一系列的`thenApply()`在回调方法 在CompletableFuture写一个连续的转换。这样的话，结果中的一个 `thenApply`方法就会传递给该系列的另外一个 `thenApply`方法。

```
CompletableFuture<String> welcomeText = CompletableFuture.supplyAsync(() -> {
    try {
        TimeUnit.SECONDS.sleep(1);
    } catch (InterruptedException e) {
       throw new IllegalStateException(e);
    }
    return "Rajeev";
}).thenApply(name -> {
    return "Hello " + name;
}).thenApply(greeting -> {
    return greeting + ", Welcome to the CalliCoder Blog";
});

System.out.println(welcomeText.get());
// Prints - Hello Rajeev, Welcome to the CalliCoder Blog
```

**2. thenAccept() 和 thenRun()** 如果你不想从你的回调函数中返回任何东西，仅仅想在Future完成后运行一些代码片段，你可以使用`thenAccept()`和 `thenRun()`方法，这些方法经常在调用链的最末端的最后一个回调函数中使用。 `CompletableFuture.thenAccept()`持有一个`Consumer<T>`，返回一个`CompletableFuture<Void>`。它可以访问`CompletableFuture`的结果：

```
// thenAccept() example
CompletableFuture.supplyAsync(() -> {
	return ProductService.getProductDetail(productId);
}).thenAccept(product -> {
	System.out.println("Got product detail from remote service " + product.getName())
});
```

虽然`thenAccept()`可以访问CompletableFuture的结果，但`thenRun()`不能访Future的结果，它持有一个Runnable返回CompletableFuture：

```
// thenRun() example
CompletableFuture.supplyAsync(() -> {
    // Run some computation  
}).thenRun(() -> {
    // Computation Finished.
});
```

> **异步回调方法的笔记** CompletableFuture提供的所有回调方法都有两个变体： `// thenApply() variants <U> CompletableFuture<U> thenApply(Function<? super T,? extends U> fn) <U> CompletableFuture<U> thenApplyAsync(Function<? super T,? extends U> fn) <U> CompletableFuture<U> thenApplyAsync(Function<? super T,? extends U> fn, Executor executor)` 这些异步回调变体通过在独立的线程中执行回调任务帮助你进一步执行并行计算。 以下示例：

```
CompletableFuture.supplyAsync(() -> {
    try {
       TimeUnit.SECONDS.sleep(1);
    } catch (InterruptedException e) {
      throw new IllegalStateException(e);
    }
    return "Some Result"
}).thenApply(result -> {
    /* 
      Executed in the same thread where the supplyAsync() task is executed
      or in the main thread If the supplyAsync() task completes immediately (Remove sleep() call to verify)
    */
    return "Processed Result"
})
```

在以上示例中，在`thenApply()`中的任务和在`supplyAsync()`中的任务执行在相同的线程中。任何`supplyAsync()`立即执行完成,那就是执行在主线程中（尝试删除sleep测试下）。 为了控制执行回调任务的线程，你可以使用异步回调。如果你使用`thenApplyAsync()`回调，将从`ForkJoinPool.commonPool()`获取不同的线程执行。

```
CompletableFuture.supplyAsync(() -> {
    return "Some Result"
}).thenApplyAsync(result -> {
    // Executed in a different thread from ForkJoinPool.commonPool()
    return "Processed Result"
})
```

此外，如果你传入一个`Executor`到`thenApplyAsync()`回调中，，任务将从Executor线程池获取一个线程执行。

```
Executor executor = Executors.newFixedThreadPool(2);
CompletableFuture.supplyAsync(() -> {
    return "Some result"
}).thenApplyAsync(result -> {
    // Executed in a thread obtained from the executor
    return "Processed Result"
}, executor);
```

### 1.6.组合两个CompletableFuture

**1. 使用 `thenCompose()`组合两个独立的future** 假设你想从一个远程API中获取一个用户的详细信息，一旦用户信息可用，你想从另外一个服务中获取他的贷方。 考虑下以下两个方法`getUserDetail()`和`getCreditRating()`的实现：

```
CompletableFuture<User> getUsersDetail(String userId) {
	return CompletableFuture.supplyAsync(() -> {
		UserService.getUserDetails(userId);
	});	
}

CompletableFuture<Double> getCreditRating(User user) {
	return CompletableFuture.supplyAsync(() -> {
		CreditRatingService.getCreditRating(user);
	});
}
```

现在让我们弄明白当使用了`thenApply()`后是否会达到我们期望的结果-

```
CompletableFuture<CompletableFuture<Double>> result = getUserDetail(userId)
.thenApply(user -> getCreditRating(user));
```

在更早的示例中，`Supplier`函数传入`thenApply`将返回一个简单的值，但是在本例中，将返回一个CompletableFuture。以上示例的最终结果是一个嵌套的CompletableFuture。 如果你想获取最终的结果给最顶层future，使用 `thenCompose()`方法代替-

```
CompletableFuture<Double> result = getUserDetail(userId)
.thenCompose(user -> getCreditRating(user));
```

因此，规则就是-如果你的回调函数返回一个CompletableFuture，但是你想从CompletableFuture链中获取一个直接合并后的结果，这时候你可以使用`thenCompose()`。

**2. 使用`thenCombine()`组合两个独立的 future** 虽然`thenCompose()`被用于当一个future依赖另外一个future的时候用来组合两个future。`thenCombine()`被用来当两个独立的`Future`都完成的时候，用来做一些事情。

```
System.out.println("Retrieving weight.");
CompletableFuture<Double> weightInKgFuture = CompletableFuture.supplyAsync(() -> {
    try {
        TimeUnit.SECONDS.sleep(1);
    } catch (InterruptedException e) {
       throw new IllegalStateException(e);
    }
    return 65.0;
});

System.out.println("Retrieving height.");
CompletableFuture<Double> heightInCmFuture = CompletableFuture.supplyAsync(() -> {
    try {
        TimeUnit.SECONDS.sleep(1);
    } catch (InterruptedException e) {
       throw new IllegalStateException(e);
    }
    return 177.8;
});

System.out.println("Calculating BMI.");
CompletableFuture<Double> combinedFuture = weightInKgFuture
        .thenCombine(heightInCmFuture, (weightInKg, heightInCm) -> {
    Double heightInMeter = heightInCm/100;
    return weightInKg/(heightInMeter*heightInMeter);
});

System.out.println("Your BMI is - " + combinedFuture.get());
```

当两个Future都完成的时候，传给``thenCombine()的回调函数将被调用。

### 1.7.组合多个CompletableFuture

我们使用`thenCompose()`和 `thenCombine()`把两个CompletableFuture组合在一起。现在如果你想组合任意数量的CompletableFuture，应该怎么做？我们可以使用以下两个方法组合任意数量的CompletableFuture。

```
static CompletableFuture<Void> allOf(CompletableFuture<?>... cfs)
static CompletableFuture<Object> anyOf(CompletableFuture<?>... cfs)
```

**1. CompletableFuture.allOf()** `CompletableFuture.allOf`的使用场景是当你一个列表的独立future，并且你想在它们都完成后并行的做一些事情。

假设你想下载一个网站的100个不同的页面。你可以串行的做这个操作，但是这非常消耗时间。因此你想写一个函数，传入一个页面链接，返回一个CompletableFuture，异步的下载页面内容。

```
CompletableFuture<String> downloadWebPage(String pageLink) {
	return CompletableFuture.supplyAsync(() -> {
		// Code to download and return the web page's content
	});
} 
```

现在，当所有的页面已经下载完毕，你想计算包含关键字`CompletableFuture`页面的数量。可以使用`CompletableFuture.allOf()`达成目的。

```
List<String> webPageLinks = Arrays.asList(...)	// A list of 100 web page links

// Download contents of all the web pages asynchronously
List<CompletableFuture<String>> pageContentFutures = webPageLinks.stream()
        .map(webPageLink -> downloadWebPage(webPageLink))
        .collect(Collectors.toList());


// Create a combined Future using allOf()
CompletableFuture<Void> allFutures = CompletableFuture.allOf(
        pageContentFutures.toArray(new CompletableFuture[pageContentFutures.size()])
);
```

使用`CompletableFuture.allOf()`的问题是它返回CompletableFuture。但是我们可以通过写一些额外的代码来获取所有封装的CompletableFuture结果。

```
// When all the Futures are completed, call `future.join()` to get their results and collect the results in a list -
CompletableFuture<List<String>> allPageContentsFuture = allFutures.thenApply(v -> {
   return pageContentFutures.stream()
           .map(pageContentFuture -> pageContentFuture.join())
           .collect(Collectors.toList());
});
```

花一些时间理解下以上代码片段。当所有future完成的时候，我们调用了`future.join()`，因此我们不会在任何地方阻塞。

`join()`方法和`get()`方法非常类似，这唯一不同的地方是如果最顶层的CompletableFuture完成的时候发生了异常，它会抛出一个未经检查的异常。

现在让我们计算包含关键字页面的数量。

```
// Count the number of web pages having the "CompletableFuture" keyword.
CompletableFuture<Long> countFuture = allPageContentsFuture.thenApply(pageContents -> {
    return pageContents.stream()
            .filter(pageContent -> pageContent.contains("CompletableFuture"))
            .count();
});

System.out.println("Number of Web Pages having CompletableFuture keyword - " + 
        countFuture.get());
```

**2. CompletableFuture.anyOf()**

`CompletableFuture.anyOf()`和其名字介绍的一样，当任何一个CompletableFuture完成的时候【相同的结果类型】，返回一个新的CompletableFuture。以下示例：

```
CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> {
    try {
        TimeUnit.SECONDS.sleep(2);
    } catch (InterruptedException e) {
       throw new IllegalStateException(e);
    }
    return "Result of Future 1";
});

CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
    try {
        TimeUnit.SECONDS.sleep(1);
    } catch (InterruptedException e) {
       throw new IllegalStateException(e);
    }
    return "Result of Future 2";
});

CompletableFuture<String> future3 = CompletableFuture.supplyAsync(() -> {
    try {
        TimeUnit.SECONDS.sleep(3);
    } catch (InterruptedException e) {
       throw new IllegalStateException(e);
    }
    return "Result of Future 3";
});

CompletableFuture<Object> anyOfFuture = CompletableFuture.anyOf(future1, future2, future3);

System.out.println(anyOfFuture.get()); // Result of Future 2
```

在以上示例中，当三个中的任何一个CompletableFuture完成， `anyOfFuture`就会完成。因为`future2`的休眠时间最少，因此她最先完成，最终的结果将是`future2`的结果。

`CompletableFuture.anyOf()`传入一个Future可变参数，返回CompletableFuture。`CompletableFuture.anyOf()`的问题是如果你的CompletableFuture返回的结果是不同类型的，这时候你讲会不知道你最终CompletableFuture是什么类型。

### 1.8.CompletableFuture 异常处理

我们探寻了怎样创建CompletableFuture，转换它们，并组合多个CompletableFuture。现在让我们弄明白当发生错误的时候我们应该怎么做。

首先让我们明白在一个回调链中错误是怎么传递的。思考下以下回调链：

```
CompletableFuture.supplyAsync(() -> {
	// Code which might throw an exception
	return "Some result";
}).thenApply(result -> {
	return "processed result";
}).thenApply(result -> {
	return "result after further processing";
}).thenAccept(result -> {
	// do something with the final result
});
```

如果在原始的`supplyAsync()`任务中发生一个错误，这时候没有任何`thenApply`会被调用并且future将以一个异常结束。如果在第一个`thenApply`发生错误，这时候第二个和第三个将不会被调用，同样的，future将以异常结束。

**1. 使用 exceptionally() 回调处理异常** `exceptionally()`回调给你一个从原始Future中生成的错误恢复的机会。你可以在这里记录这个异常并返回一个默认值。

```
Integer age = -1;

CompletableFuture<String> maturityFuture = CompletableFuture.supplyAsync(() -> {
    if(age < 0) {
        throw new IllegalArgumentException("Age can not be negative");
    }
    if(age > 18) {
        return "Adult";
    } else {
        return "Child";
    }
}).exceptionally(ex -> {
    System.out.println("Oops! We have an exception - " + ex.getMessage());
    return "Unknown!";
});

System.out.println("Maturity : " + maturityFuture.get()); 
```

**2. 使用 handle() 方法处理异常** API提供了一个更通用的方法 - `handle()`从异常恢复，无论一个异常是否发生它都会被调用。

```
Integer age = -1;

CompletableFuture<String> maturityFuture = CompletableFuture.supplyAsync(() -> {
    if(age < 0) {
        throw new IllegalArgumentException("Age can not be negative");
    }
    if(age > 18) {
        return "Adult";
    } else {
        return "Child";
    }
}).handle((res, ex) -> {
    if(ex != null) {
        System.out.println("Oops! We have an exception - " + ex.getMessage());
        return "Unknown!";
    }
    return res;
});

System.out.println("Maturity : " + maturityFuture.get());
```

如果异常发生，`res`参数将是 null，否则，`ex`将是 null。

## 2. CompletionStage

CompletionStage 接口可以清晰地描述任务之间的时序关系，如**串行关系、并行关系、汇聚关系**等。

### 2.1. 串行关系

CompletionStage 接口里面描述串行关系，主要是 thenApply、thenAccept、thenRun 和 thenCompose 这四个系列的接口。

thenApply 系列函数里参数 fn 的类型是接口 `Function<T, R>`，这个接口里与 CompletionStage 相关的方法是 `R apply(T t)`，这个方法既能接收参数也支持返回值，所以 thenApply 系列方法返回的是`CompletionStage`。

而 thenAccept 系列方法里参数 consumer 的类型是接口 `Consumer<T>`，这个接口里与 CompletionStage 相关的方法是 `void accept(T t)`，这个方法虽然支持参数，但却不支持回值，所以 thenAccept 系列方法返回的是`CompletionStage<Void>`。

thenRun 系列方法里 action 的参数是 Runnable，所以 action 既不能接收参数也不支持返回值，所以 thenRun 系列方法返回的也是`CompletionStage<Void>`。

这些方法里面 Async 代表的是异步执行 fn、consumer 或者 action。其中，需要你注意的是 thenCompose 系列方法，这个系列的方法会新创建出一个子流程，最终结果和 thenApply 系列是相同的。

### 2.2. 描述 AND 汇聚关系

CompletionStage 接口里面描述 AND 汇聚关系，主要是 thenCombine、thenAcceptBoth 和 runAfterBoth 系列的接口，这些接口的区别也是源自 fn、consumer、action 这三个核心参数不同。

```
CompletionStage<R> thenCombine(other, fn);
CompletionStage<R> thenCombineAsync(other, fn);
CompletionStage<Void> thenAcceptBoth(other, consumer);
CompletionStage<Void> thenAcceptBothAsync(other, consumer);
CompletionStage<Void> runAfterBoth(other, action);
CompletionStage<Void> runAfterBothAsync(other, action);
```

### 2.3. 描述 OR 汇聚关系

CompletionStage 接口里面描述 OR 汇聚关系，主要是 applyToEither、acceptEither 和 runAfterEither 系列的接口，这些接口的区别也是源自 fn、consumer、action 这三个核心参数不同。

```
CompletionStage applyToEither(other, fn);
CompletionStage applyToEitherAsync(other, fn);
CompletionStage acceptEither(other, consumer);
CompletionStage acceptEitherAsync(other, consumer);
CompletionStage runAfterEither(other, action);
CompletionStage runAfterEitherAsync(other, action);
```

下面的示例代码展示了如何使用 applyToEither() 方法来描述一个 OR 汇聚关系。

```
CompletableFuture<String> f1 =
  CompletableFuture.supplyAsync(()->{
    int t = getRandom(5, 10);
    sleep(t, TimeUnit.SECONDS);
    return String.valueOf(t);
});

CompletableFuture<String> f2 =
  CompletableFuture.supplyAsync(()->{
    int t = getRandom(5, 10);
    sleep(t, TimeUnit.SECONDS);
    return String.valueOf(t);
});

CompletableFuture<String> f3 =
  f1.applyToEither(f2,s -> s);

System.out.println(f3.join());
```

### 2.4. 异常处理

虽然上面我们提到的 fn、consumer、action 它们的核心方法都**不允许抛出可检查异常，但是却无法限制它们抛出运行时异常**，例如下面的代码，执行 `7/0` 就会出现除零错误这个运行时异常。非异步编程里面，我们可以使用 try{}catch{} 来捕获并处理异常，那在异步编程里面，异常该如何处理呢？

```
CompletableFuture<Integer>
  f0 = CompletableFuture.
    .supplyAsync(()->(7/0))
    .thenApply(r->r*10);
System.out.println(f0.join());
```

CompletionStage 接口给我们提供的方案非常简单，比 try{}catch{}还要简单，下面是相关的方法，使用这些方法进行异常处理和串行操作是一样的，都支持链式编程方式。

```
CompletionStage exceptionally(fn);
CompletionStage<R> whenComplete(consumer);
CompletionStage<R> whenCompleteAsync(consumer);
CompletionStage<R> handle(fn);
CompletionStage<R> handleAsync(fn);
```

下面的示例代码展示了如何使用 exceptionally() 方法来处理异常，exceptionally() 的使用非常类似于 try{}catch{}中的 catch{}，但是由于支持链式编程方式，所以相对更简单。既然有 try{}catch{}，那就一定还有 try{}finally{}，whenComplete() 和 handle() 系列方法就类似于 try{}finally{}中的 finally{}，无论是否发生异常都会执行 whenComplete() 中的回调函数 consumer 和 handle() 中的回调函数 fn。whenComplete() 和 handle() 的区别在于 whenComplete() 不支持返回结果，而 handle() 是支持返回结果的。

```
CompletableFuture<Integer>
  f0 = CompletableFuture
    .supplyAsync(()->7/0))
    .thenApply(r->r*10)
    .exceptionally(e->0);
System.out.println(f0.join());
```

## 3. Fork/Join

ForkJoin是由JDK1.7之后提供的多线程并发处理框架。ForkJoin框架的基本思想是分而治之。什么是分而治之？分而治之就是将一个复杂的计算，按照设定的阈值分解成多个计算，然后将各个计算结果进行汇总。相应的，ForkJoin将复杂的计算当做一个任务，而分解的多个计算则是当做一个个子任务来并行执行。

Fork/Join 是一个并行计算的框架，主要就是用来支持分治任务模型的，这个计算框架里的**Fork 对应的是分治任务模型里的任务分解，Join 对应的是结果合并**。Fork/Join 计算框架主要包含两部分，一部分是**分治任务的线程池 ForkJoinPool**，另一部分是**分治任务 ForkJoinTask**。这两部分的关系类似于 ThreadPoolExecutor 和 Runnable 的关系，都可以理解为提交任务到线程池，只不过分治任务有自己独特类型 ForkJoinTask。

ForkJoinTask 是一个抽象类，它的方法有很多，最核心的是 fork() 方法和 join() 方法，其中 fork() 方法会异步地执行一个子任务，而 join() 方法则会阻塞当前线程来等待子任务的执行结果。ForkJoinTask 有两个子类——RecursiveAction 和 RecursiveTask，通过名字你就应该能知道，它们都是用递归的方式来处理分治任务的。这两个子类都定义了抽象方法 compute()，不过区别是 RecursiveAction 定义的 compute() 没有返回值，而 RecursiveTask 定义的 compute() 方法是有返回值的。这两个子类也是抽象类，在使用的时候，需要你定义子类去扩展。

### 3.1. ForkJoinPool 工作原理

Fork/Join 并行计算的核心组件是 ForkJoinPool，所以下面我们就来简单介绍一下 ForkJoinPool 的工作原理。

通过专栏前面文章的学习，你应该已经知道 ThreadPoolExecutor 本质上是一个生产者 - 消费者模式的实现，内部有一个任务队列，这个任务队列是生产者和消费者通信的媒介；ThreadPoolExecutor 可以有多个工作线程，但是这些工作线程都共享一个任务队列。

ForkJoinPool 本质上也是一个生产者 - 消费者的实现，但是更加智能，你可以参考下面的 ForkJoinPool 工作原理图来理解其原理。ThreadPoolExecutor 内部只有一个任务队列，而 ForkJoinPool 内部有多个任务队列，当我们通过 ForkJoinPool 的 invoke() 或者 submit() 方法提交任务时，ForkJoinPool 根据一定的路由规则把任务提交到一个任务队列中，如果任务在执行过程中会创建出子任务，那么子任务会提交到工作线程对应的任务队列中。

如果工作线程对应的任务队列空了，是不是就没活儿干了呢？不是的，ForkJoinPool 支持一种叫做“**任务窃取**”的机制，如果工作线程空闲了，那它可以“窃取”其他工作任务队列里的任务，例如下图中，线程 T2 对应的任务队列已经空了，它可以“窃取”线程 T1 对应的任务队列的任务。如此一来，所有的工作线程都不会闲下来了。

ForkJoinPool 中的任务队列采用的是双端队列，工作线程正常获取任务和“窃取任务”分别是从任务队列不同的端消费，这样能避免很多不必要的数据竞争。我们这里介绍的仅仅是简化后的原理，ForkJoinPool 的实现远比我们这里介绍的复杂，如果你感兴趣，建议去看它的源码。

![687474703a2f2f64756e77752e746573742e757063646e2e6e65742f736e61702f32303230303730333134313332362e706e67](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210322225703.png)

### 3.2. ForkJoin框架的实现

ForkJoin框架中一些重要的类如下所示。

![20200411235333318](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210323235725.jpg)

ForkJoinPool 框架中涉及的主要类如下所示。

**1.ForkJoinPool类**

实现了ForkJoin框架中的线程池，由类图可以看出，ForkJoinPool类实现了线程池的Executor接口。

我们也可以从下图中看出ForkJoinPool的类图关系。

![20200411235346149](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210323235738.jpg)

其中，可以使用Executors.newWorkStealPool()方法创建ForkJoinPool。

ForkJoinPool中提供了如下提交任务的方法。

```java
public void execute(ForkJoinTask<?> task)
public void execute(Runnable task)
public <T> T invoke(ForkJoinTask<T> task)
public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) 
public <T> ForkJoinTask<T> submit(ForkJoinTask<T> task)
public <T> ForkJoinTask<T> submit(Callable<T> task)
public <T> ForkJoinTask<T> submit(Runnable task, T result)
public ForkJoinTask<?> submit(Runnable task)
```

**2.ForkJoinWorkerThread类**

实现ForkJoin框架中的线程。

**3.ForkJoinTask类**

ForkJoinTask封装了数据及其相应的计算，并且支持细粒度的数据并行。ForkJoinTask比线程要轻量，ForkJoinPool中少量工作线程能够运行大量的ForkJoinTask。

ForkJoinTask类中主要包括两个方法fork()和join()，分别实现任务的分拆与合并。

fork()方法类似于Thread.start()，但是它并不立即执行任务，而是将任务放入工作队列中。跟Thread.join()方法不同，ForkJoinTask的join()方法并不简单的阻塞线程，而是利用工作线程运行其他任务，当一个工作线程中调用join()，它将处理其他任务，直到注意到目标子任务已经完成。

我们可以使用下图来表示这个过程。

![20200411235408792](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210323235750.jpg)

ForkJoinTask有3个子类：

![20200411235448186](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210323235758.jpg)

- RecursiveAction：无返回值的任务。
- RecursiveTask：有返回值的任务。
- CountedCompleter：完成任务后将触发其他任务。

**4.RecursiveTask 类**

有返回结果的ForkJoinTask实现Callable。

**5.RecursiveAction类**

无返回结果的ForkJoinTask实现Runnable。

**6.CountedCompleter 类**

在任务完成执行后会触发执行一个自定义的钩子函数。

### 3.3.ForkJoin示例程序

```java
package io.binghe.concurrency.example.aqs;
 
import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.RecursiveTask;
@Slf4j
public class ForkJoinTaskExample extends RecursiveTask<Integer> {
    public static final int threshold = 2;
    private int start;
    private int end;
    public ForkJoinTaskExample(int start, int end) {
        this.start = start;
        this.end = end;
    }
    @Override
    protected Integer compute() {
        int sum = 0;
        //如果任务足够小就计算任务
        boolean canCompute = (end - start) <= threshold;
        if (canCompute) {
            for (int i = start; i <= end; i++) {
                sum += i;
            }
        } else {
            // 如果任务大于阈值，就分裂成两个子任务计算
            int middle = (start + end) / 2;
            ForkJoinTaskExample leftTask = new ForkJoinTaskExample(start, middle);
            ForkJoinTaskExample rightTask = new ForkJoinTaskExample(middle + 1, end);
 
            // 执行子任务
            leftTask.fork();
            rightTask.fork();
 
            // 等待任务执行结束合并其结果
            int leftResult = leftTask.join();
            int rightResult = rightTask.join();
 
            // 合并子任务
            sum = leftResult + rightResult;
        }
        return sum;
    }
    public static void main(String[] args) {
        ForkJoinPool forkjoinPool = new ForkJoinPool();
 
        //生成一个计算任务，计算1+2+3+4
        ForkJoinTaskExample task = new ForkJoinTaskExample(1, 100);
 
        //执行一个任务
        Future<Integer> result = forkjoinPool.submit(task);
 
        try {
            log.info("result:{}", result.get());
        } catch (Exception e) {
            log.error("exception", e);
        }
    }
}
```