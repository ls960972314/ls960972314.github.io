---
layout: default
title: quartz内存泄漏问题
---


下面这段话是网上摘抄的:
>spring中的提供了一个名为 org.springframework.web.util.IntrospectorCleanupListener的监听器.它主要负责处理由　 JavaBeans Introspector的使用而引起的缓冲泄露.
spring中对它的描述如下:它是一个在web应用关闭的时候,清除JavaBeans Introspector的监听器.web.xml中注册这个listener.可以保证在web 应用关闭的时候释放与掉这个web 应用相关的class loader 
和由它管理的类.
如果你使用了JavaBeans Introspector来分析应用中的类,Introspector 缓冲中会保留这些类的引用.结果在你的应用关闭的时候,这些类以及web 应用相关的class loader没有被垃圾回收.
不幸的是,清除Introspector的唯一方式是刷新整个缓冲.这是因为我们没法判断哪些是属于你的应用的引用.所以删除被缓冲的introspection会导致把这台电脑上的所有应用的introspection都删掉.
需要注意的是,spring 托管的bean不需要使用这个监听器.因为spring它自己的introspection所使用的缓冲在分析完一个类之后会被马上从javaBeans Introspector缓冲中清除掉.
应用程序中的类从来不直接使用JavaBeans Introspector.所以他们一般不会导致内部查看资源泄露.
但是一些类库和框架往往会产生这个问题.例如:Struts 和Quartz.单个的内部查看泄漏会导致整个的web应用的类加载器不能进行垃圾回收.在web应用关闭之后,你会看到此应用的所有静态类资源(例如单例).
这个错误当然不是由这个类自身引起的.

个人理解为,quartz中使用了Introspector来分析类,导致类的Class加载到缓存中,当前应用关闭且容器还在的时候,无法回收使用Introspector分析过的class及classLoader,进而导致内存泄漏,如果应用关闭时容器也关闭了,应该就不存在这个问题.
# quartz内存泄漏解决方案
就是在web.xml中加入:  
```xml
<listener>  
    <listener-class>org.springframework.web.util.IntrospectorCleanupListener</listener-class>  
</listener> 
```

只知道servlet标准不允许在web容器内自行做线程管理,quartz的问题确实存在。  
对于Web容器来说,最忌讳应用程序私自启动线程,自行进行线程调度,像Quartz这种在web容器内部默认就自己启动了10线程进行异步job调度的框架本身就是很危险的事情,很容易造成servlet线程资源回收不掉 
org.springframework.web.util.IntrospectorCleanupListener的代码如下
```java
public class IntrospectorCleanupListener implements ServletContextListener {

	@Override
	public void contextInitialized(ServletContextEvent event) {
		CachedIntrospectionResults.acceptClassLoader(Thread.currentThread().getContextClassLoader());
	}

	@Override
	public void contextDestroyed(ServletContextEvent event) {
		CachedIntrospectionResults.clearClassLoader(Thread.currentThread().getContextClassLoader());
		Introspector.flushCaches();
	}

}
```

CachedIntrospectionResults.clearClassLoader的代码如下,作用是如果判断各个已缓存的classLoader在当前classLoader之下,就remove掉
```java
	public static void clearClassLoader(ClassLoader classLoader) {
		for (Iterator<ClassLoader> it = acceptedClassLoaders.iterator(); it.hasNext();) {
			ClassLoader registeredLoader = it.next();
			if (isUnderneathClassLoader(registeredLoader, classLoader)) {
				it.remove();
			}
		}
		for (Iterator<Class<?>> it = strongClassCache.keySet().iterator(); it.hasNext();) {
			Class<?> beanClass = it.next();
			if (isUnderneathClassLoader(beanClass.getClassLoader(), classLoader)) {
				it.remove();
			}
		}
		for (Iterator<Class<?>> it = softClassCache.keySet().iterator(); it.hasNext();) {
			Class<?> beanClass = it.next();
			if (isUnderneathClassLoader(beanClass.getClassLoader(), classLoader)) {
				it.remove();
			}
		}
	}
```

Introspector.flushCaches的代码如下,作用是清空当前ThreadGroupContext中的beanInfoCache
```java
    public static void flushCaches() {
        synchronized (declaredMethodCache) {
            ThreadGroupContext.getContext().clearBeanInfoCache();
            declaredMethodCache.clear();
        }
    }
```


# quartz中对象属性的设置
quartz中用到的线程池为org.quartz.simpl.SimpleThreadPool
执行的线程为org.quartz.simpl.SimpleThreadPool$WorkerThread

SimpleThreadPool是通过org.quartz.impl.StdSchedulerFactory类中instantiate()方法初始化的,该方法中对各个对象的属性设置是通过StdSchedulerFactory.setBeanProps(Object obj, Properties props)完成的.
代码如下,可以看到使用到了Introspector.getBeanInfo方法来得到这个类的BeanInfo,再利用反射循环设置该类的各个属性值.
```java
    private void setBeanProps(Object obj, Properties props)
        throws NoSuchMethodException, IllegalAccessException,
            java.lang.reflect.InvocationTargetException,
            IntrospectionException, SchedulerConfigException {
        props.remove("class");

        BeanInfo bi = Introspector.getBeanInfo(obj.getClass());
        PropertyDescriptor[] propDescs = bi.getPropertyDescriptors();
        PropertiesParser pp = new PropertiesParser(props);

        java.util.Enumeration<Object> keys = props.keys();
        while (keys.hasMoreElements()) {
            String name = (String) keys.nextElement();
            String c = name.substring(0, 1).toUpperCase(Locale.US);
            String methName = "set" + c + name.substring(1);

            java.lang.reflect.Method setMeth = getSetMethod(methName, propDescs);

            try {
                if (setMeth == null) {
                    throw new NoSuchMethodException(
                            "No setter for property '" + name + "'");
                }

                Class<?>[] params = setMeth.getParameterTypes();
                if (params.length != 1) {
                    throw new NoSuchMethodException(
                        "No 1-argument setter for property '" + name + "'");
                }
                
                // does the property value reference another property's value? If so, swap to look at its value
                PropertiesParser refProps = pp;
                String refName = pp.getStringProperty(name);
                if(refName != null && refName.startsWith("$@")) {
                    refName =  refName.substring(2);
                    refProps = cfg;
                }
                else
                    refName = name;
                
                if (params[0].equals(int.class)) {
                    setMeth.invoke(obj, new Object[]{Integer.valueOf(refProps.getIntProperty(refName))});
                } else if (params[0].equals(long.class)) {
                    setMeth.invoke(obj, new Object[]{Long.valueOf(refProps.getLongProperty(refName))});
                } else if (params[0].equals(float.class)) {
                    setMeth.invoke(obj, new Object[]{Float.valueOf(refProps.getFloatProperty(refName))});
                } else if (params[0].equals(double.class)) {
                    setMeth.invoke(obj, new Object[]{Double.valueOf(refProps.getDoubleProperty(refName))});
                } else if (params[0].equals(boolean.class)) {
                    setMeth.invoke(obj, new Object[]{Boolean.valueOf(refProps.getBooleanProperty(refName))});
                } else if (params[0].equals(String.class)) {
                    setMeth.invoke(obj, new Object[]{refProps.getStringProperty(refName)});
                } else {
                    throw new NoSuchMethodException(
                            "No primitive-type setter for property '" + name
                                    + "'");
                }
            } catch (NumberFormatException nfe) {
                throw new SchedulerConfigException("Could not parse property '"
                        + name + "' into correct data type: " + nfe.toString());
            }
        }
    }
```

# Introspector.getBeanInfo方法实现

Introspector.getBeanInfo方法用Class为key从当前ThreadGroup中获取BeanInfo缓存,如果不存在,就初始化然后存放进ThreadGroupContext的缓存中.

```java
    public static BeanInfo getBeanInfo(Class<?> beanClass)
        throws IntrospectionException
    {
        if (!ReflectUtil.isPackageAccessible(beanClass)) {
            return (new Introspector(beanClass, null, USE_ALL_BEANINFO)).getBeanInfo();
        }
        ThreadGroupContext context = ThreadGroupContext.getContext();
        BeanInfo beanInfo;
        synchronized (declaredMethodCache) {
            beanInfo = context.getBeanInfo(beanClass);
        }
        if (beanInfo == null) {
            beanInfo = new Introspector(beanClass, null, USE_ALL_BEANINFO).getBeanInfo();
            synchronized (declaredMethodCache) {
                context.putBeanInfo(beanClass, beanInfo);
            }
        }
        return beanInfo;
    }
```
