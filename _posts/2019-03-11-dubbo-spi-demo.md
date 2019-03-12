---
layout: post
title: 编写可扩展化代码
date: 2019-03-11
categories: test
tags: spi 
---

- 这里是一个目录
{:toc}

# 痛点

​       拿差错系统来说，大体上有核查、差错提交、贷记调整、例外交易、例外复核、收付调整等差错交易类型，每个差错交易类型又分为很多原因码，比如核查有2001、2201、2301、2502、2102、2401、2402等原因码，每个原因码还可能分有不同的子原因码。在接收到差错交易请求时，由于每个差错交易的发起方与接收方的不同，再加上代理清算、原交易状态、清算状态等各个维度的判断，会导致整个差错业务的校验部分比较复杂，但是每个差错交易的整体逻辑是相同的。前期代码的开发都是由不同的人员开发，代码编写能力也都不相同，导致整体代码校验部分看起来非常复杂，虽然已经对各个方法进行了封装，但整体看起来仍然不太满意，所以需要在业务稳定后，对原有代码进行重构，加强模块化与可扩展性。

​	下面截图为差错核查的代码，虽然已经有很多注释了，但是并没有一个很清晰的思路在里面。

![img](https://raw.githubusercontent.com/ls960972314/ls960972314.github.io/master/_posts/dubbo-spi-demo/1079532119.jpg) 

# dubbo框架的SPI机制

​	在阅读dubbo源码的时候，会发现整体的结构非常清晰，而且对外提供了很多扩展点，它是怎么做到的呢？以下内容如果看的不太懂，直接看下一章节后再回头看。

​	dubbo扩展都会被放置在下面几个目录

1. **META-INF/dubbo/internal/** ：用于dubbo内部提供的拓展实现

2. **META-INF/dubbo/** ：用于用户自定义的拓展实现

3. **META-INF/service/** ：Java SPI的配置目录

   下图为dubbo的Compiler接口的内部实现配置

   ![img](https://raw.githubusercontent.com/ls960972314/ls960972314.github.io/master/_posts/dubbo-spi-demo/3798905611.jpg) 

   

​       另外需要知道三个注解及一个对象：@SPI，@Adaptive，@Activate，URL。

**@SPI**：定义一个接口是一个可被扩展的接口，@SPI注解中的值代表其默认的实现类，对应上面配置文件中的key。下图为Compile接口，默认实现为javassist。

![img](https://raw.githubusercontent.com/ls960972314/ls960972314.github.io/master/_posts/dubbo-spi-demo/1552302968617.jpg)

**@Adaptive**：分为两种

1. 标记在类上，代表**手工实现拓展实现类**的逻辑，比如下图Compile的拓展类实现AdaptiveCompiler，如果DEFAULT_COMPILE有值（就是对应的dubbo配置文件中的key），就使用其对应的实现类实现，如果没有配置就使用默认的实现（对应SPI注解中配置的值）。

   ![img](https://raw.githubusercontent.com/ls960972314/ls960972314.github.io/master/_posts/dubbo-spi-demo/3261779939.jpg) 

2. 标记在方法上时，代表**自动生成代码**实现该接口的拓展实现类（这个目前我们可以先不用，因为这块的动态生成的代码和dubbo内部的逻辑绑的很紧，有必要时可以自己重新实现这块功能）。

**@Activate**：代表自动激活，是什么意思呢？拿Filter接口相关的服务来说，在调用ExtensionLoader.getExtensionLoader(Filter.class).getActivateExtension相关的方法时，返回一个根据getActivateExtension参数过滤出来的List。比如下图中组装过滤器时先根据key和group过滤出对应的过滤器数组，在进行其他操作。

![img](https://raw.githubusercontent.com/ls960972314/ls960972314.github.io/master/_posts/dubbo-spi-demo/3731389863.jpg) 

**URL：**dubbo内部将服务相关的所有变量放到了一个URL对象中，这样很容易在各个环节进行处理，URL就相当于各个环节间通用的语言。



# 借鉴dubbo spi机制编写可扩展的代码

​	我是在研究dubbo源码的时候，感受到其spi机制在实现可扩展代码时的便利与强大。然后回想当前的业务场景，是不是也可以借助这种模式来编写出优美的可扩展性代码。我下面拿dubbo spi的这一套实现一个最简单的差错交易demo。

​	假如我们现在要实现一个最简单的需求：实现核查与差错提交两种交易，有2201和2301两个原因码。这在目前的代码中是核查与差错提交各用一个接口实现，然后在核查中使用一个Map保存了2201和2301的校验实现类，根据参数拿到原因码的实现类后在调用。下面开始借鉴dubbo spi机制实现上述需求。

## 运行结果

### 自动拓展运行结果

先看下主要代码及代码运行结果，注意看红线部分，我们当前实现的需求是核查的2301原因码：

![img](https://raw.githubusercontent.com/ls960972314/ls960972314.github.io/master/_posts/dubbo-spi-demo/1552310720159.jpg)

好，现在需求变了，我们要新增一个差错提交的2201的原因码，怎么办？改代码结构？不存在的，看下图，只需要将disputeType和disputeReasonCode的值调整一下即可：



![img](https://raw.githubusercontent.com/ls960972314/ls960972314.github.io/master/_posts/dubbo-spi-demo/2628384447.jpg) 

### 自动激活过滤器运行结果

@Activate的作用是自动激活，是什么意思先不要管，先看如下的执行结果，红线部分是获取groupName为dispute的Filter实现列表，再组装成一个Invoker执行链。先根据过滤器配置的order顺序执行各个过滤器，最后再执行业务实现，也就是i'm last invoker这部分的逻辑。

![img](https://raw.githubusercontent.com/ls960972314/ls960972314.github.io/master/_posts/dubbo-spi-demo/3011701304.jpg) 



## 代码结构介绍	

先看整体结构

![img](https://raw.githubusercontent.com/ls960972314/ls960972314.github.io/master/_posts/dubbo-spi-demo/2744583058.jpg) 

**META-INF/dubbo目录中放的是我们的扩展配置：**

com.epcc.risk.api.biz.base.spi.disputeReasonCode.DisputeReasonCode文件：差错原因码扩展配置

com.epcc.risk.api.biz.base.spi.disputeTran.DisputeTran文件：差错交易扩展

com.epcc.risk.api.biz.base.spi.Filter文件：过滤器扩展配置

**spi包下放的是几个公共的类：**

- DisputeTranTest：测试类入口
- Filter：过滤器接口
- Invoker：业务执行接口
- Context：差错业务类，类似于dubbo中的URL
- MyExtensionLoader：等同dubbo中的ExtensionLoader,将其复制了一份，新增了一个getActivateExtension方法用来做实验用，因为其他的getActivateExtension都带有各种参数，同dubbo框架绑的比较死。

**filter包下放的是过滤器实现：**

- LimitFilter：模拟限流过滤器
- LogFilter：模拟日志过滤器
- TokenFilter：模拟Token过滤器

**disputeTran包下放的差错交易实现：**

- AdaptiveDisputeTran：差错交易拓展实现类
- DisputeTran：差错交易接口
- InspectTran：核查接口实现
- SubmitTran：差错提交接口实现

**disputeReasonCode包下放的是差错原因码实现：**

- AdaptiveDisputeTran：差错原因码拓展实现类
- DisputeReasonCode：差错原因码接口
- ReasonCode2201：差错原因码2201的实现
- ReasonCode2301：差错原因码2301的实现

## 代码实现

### 自动拓展代码实现

拿差错交易的实现来说，获取DisputeTran实现的代码是

```
DisputeTran disputeTran = MyExtensionLoader.getExtensionLoader(DisputeTran.class).getAdaptiveExtension();
```



差错交易配置文件中配置了核查实现、差错提交实现及差错交易的拓展实现类：

```
inspectTran=com.epcc.risk.api.biz.base.spi.disputeTran.InspectTran
submitTran=com.epcc.risk.api.biz.base.spi.disputeTran.SubmitTran
adptiveDisputeTran=com.epcc.risk.api.biz.base.spi.disputeTran.AdaptiveDisputeTran
```



先看一下接口如何定义，接口类上有一个@SPI标记其是一个可扩展接口，默认拓展是inspectTran。

```java
@SPI("inspectTran")
public interface DisputeTran {

    /**
     *
     * @param context
     * @return
     */
    public Result process(Context context);

}
```



再看下DisputeTran的拓展实现类，类上有一个@Adaptive注解标记其是一个拓展实现类，实现是如果contextType中有值，就使用该值找到实现拓展，如果没有值，使用默认的拓展：

```java
@Adaptive
public class AdaptiveDisputeTran implements DisputeTran {

    @Override
    public Result process(Context context) {
        DisputeTran disputeTran;
        ExtensionLoader<DisputeTran> loader = ExtensionLoader.getExtensionLoader(DisputeTran.class);
        if (context.getDisputeType() != null && context.getDisputeType() != "") {
            disputeTran = loader.getExtension(context.getDisputeType());
        } else {
            disputeTran = loader.getDefaultExtension();
        }
        return disputeTran.process(context);
    }
}
```

再看下核查交易的实现，打印了两条日志来代表其要做的事情。另外，差错交易中用同样的方式获取了差错原因码的实现，此处就不做讲解了，原理相同。

```java
@Slf4j
public class InspectTran implements DisputeTran {

    DisputeReasonCode disputeReasonCode = ExtensionLoader.getExtensionLoader(DisputeReasonCode.class).getAdaptiveExtension();

    @Override
    public Result process(Context context) {
        log.info("deal inspect");
        disputeReasonCode.deal(context);
        log.info("insert db");
        return null;
    }
}
```



再回过头看主流程执行的代码，其disputeType的值为inspectTran时，便会根据拓展实现类中的逻辑，取配置文件中对应的com.epcc.risk.api.biz.base.spi.disputeTran.InspectTran为实现类进行处理，disputeReasonCode值为reasonCode2201时，同样根据差错原因码的拓展实现类逻辑，取com.epcc.risk.api.biz.base.spi.disputeReasonCode.inspect.ReasonCode2201为实现类进行处理。当新增了差错类型或差错原因码时，无需改动主逻辑代码，直接在配置文件中指定新增的差错类型和差错原因码的实现类即可。

```java
    @Test
    public void test1() {
        Context context = new Context();
        context.setDisputeType("submitTran");
        context.setDisputeReasonCode("reasonCode2201");
        disputeTran.process(context);
    }
```

### 自动激活代码实现

Filter配置文件中配置了各个过滤器对应的过滤器实现：

```
logFilter=com.epcc.risk.api.biz.base.spi.filter.LogFilter
limitFilter=com.epcc.risk.api.biz.base.spi.filter.LimitFilter
tokenFilter=com.epcc.risk.api.biz.base.spi.filter.TokenFilter
```



先看Filter的实现，接口类上有一个@SPI标记其是一个可扩展接口。

```java
@SPI
public interface Filter {

    Result invoke(Invoker invoker, Context context);

    default Result onResponse(Result result, Context invocation) {
        return result;
    }

}
```



再看其各个实现类的实现，这里只列出LogFilter和LimitFilter，类上有@Activate注解表名其是一个自动激活的类，其groupName都是dispute（这里的groupName是用来给获取自动激活的列表提供过滤条件）。

```java
@Slf4j
@Activate(group = "dispute", order = 999)
public class LogFilter implements Filter {

    @Override
    public Result invoke(Invoker invoker, Context context) {
        log.info("i'm log filter, order is 999");
        return invoker.invoke(context);
    }
}
```

```java
@Slf4j
@Activate(group = "dispute", order = 1000)
public class LimitFilter implements Filter {

    @Override
    public Result invoke(Invoker invoker, Context context) {
        log.info("i'm limit filter, order is 1000");
        return invoker.invoke(context);
    }
}
```



# 总结

这部分内容是完全模拟dubbo spi的机制写的一个简单的可扩展程序的小demo，dubbo内部实现可扩展程序主要靠的是com.alibaba.dubbo.common.extension.ExtensionLoader，我们完全可以按照其实现一个自己的可扩展程序。也可以利用spring等框架实现一个类似的可扩展框架，目标是将代码模块化管理，插件式开发，尽力编写出可扩展、可阅读、可测试的高质量代码。
