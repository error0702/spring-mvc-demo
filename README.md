# spring-mvc-demo
springmvcdemo document url: https://my.oschina.net/u/2488220/blog/1604829

前言: springmvc是什么，能做什么，即使是新手，也应该有所了解，俺这里就不一一赘述了。

首先我们需要引入javaee的一些概念，为什么要引入它呢？不明觉厉，不管是什么样的web框架，那么这玩意是基础。不用怀疑。所以不管如何变化，我们要谨记，是跟javaee的东西打交道。

需要大家自行掌握的概念有： servlet容器，包括ServletContext，Listener，ServletConfig。 session，request这些就不说了。

工欲善其事，必先利其器。所以我们需要先折腾一个springmvc的工程。我这里使用maven模板去创建。



选择这一个模板
  ● org.jetbrains.idea.maven.model.MavenArchetype

工程创建完后我们需要引入对应的jar包。我这里按照最小化配置来。

<dependency>
  <groupId>org.springframework</groupId>
  <artifactId>spring-webmvc</artifactId>
  <version>4.3.2.RELEASE</version>
</dependency>

加入webmvc(ps: 这里使用了webmvc，它里面又内置了好几个。包括我们熟悉的servlet, spring-beans,context等等)




具体详见webmvc的pom。这里不再累述，我们继续配置



最后加完的jar包有这么多，实际上因为servlet-api是provided的  所以这里没有出现，但并不意味着他不存在。。


在web.xml中添加


<context-param>
  <param-name>contextConfigLocation</param-name>
  <param-value>classpath:applicationContext.xml</param-value>
</context-param>

<listener>
  <listener-class>org.springframework.web.context.ContextLoaderListener</listener-class>
</listener>

<servlet>
  <servlet-name>spring</servlet-name>
  <servlet-class>org.springframework.web.servlet.DispatcherServlet</servlet-class>
  <load-on-startup>1</load-on-startup>
</servlet>

<servlet-mapping>
  <servlet-name>spring</servlet-name>
  <url-pattern>/*</url-pattern>
</servlet-mapping>

在resources目录中创建 applicationContext， 添加扫包(这里不扫描Controller注解的)

<!-- configure auto scan -->
<context:component-scan base-package="org.spring">
    <context:exclude-filter type="annotation"
                            expression="org.springframework.stereotype.Controller" />
</context:component-scan>

在WEB-INF中创建spring-servlet.xml，加入扫描Controller的扫包配置和激活RequestMappingHanlder适配器

<context:component-scan base-package="org.spring.*.controller" />
<mvc:annotation-driven  />

到此我们的工程已经可以在容器中跑起来了。当然，上面的并不是重点，接下来才是重头戏。跟着我们的代码飞吧。

启动工程，进入 ContextLoaderListener ， 我们会发现他是实现了继承自ContextLoader，实现的是ServletContextListener接口。

这里多说一句。

有好奇的小伙伴就会问，继承ContextLoader 的作用是什么呀？

且听我一一道来。
ContextLoaderListener他的主要作用是在web应用程序启动时载入ioc容器。具体详见下面会分解。而ContextLoader正是对ioc容器初始化进行了资源加载等操作，属于灵魂级的类

ok, 我们继续说监听器。

我们打开ServletContextListener接口。发现如下方法， 见名知意。init开头的就是初始化执行的动作，另外一个则是销毁


销毁暂且不论，初始化才是我们的重头戏。我们看到他最后执行了这样的操作。其实就是调用了ContextLoader.initWebApplicationContext(ServletContext servletContext) 方法。具体源码如下


废话不多说，我们先看initWebApplicationContext(event.getServletContext()); 做了什么

根源：


首先创建一个抽象的ClassPathResource，这玩意有点类似于java的File对象，是一个抽象的东东。
然后将其load进来。我们看看这个properties文件中的内容。

# Default WebApplicationContext implementation class for ContextLoader.
# Used as fallback when no explicit context implementation has been specified as context-param.
# Not meant to be customized by application developers.

org.springframework.web.context.WebApplicationContext=org.springframework.web.context.support.XmlWebApplicationContext

可以看得出来，这是一个ApplicationContext的实现类。这就是spring真正的上下文类。我们看看他的继承体系结构图


进入正题。开始创建它

进入createWebApplicationContext。

这个时候从servletContext中去取contextClass 是空。那么他从配置文件中根据这个类名去获取对应的value值。也就是org.springframework.web.context.support.XmlWebApplicationContext 然后使用Class.forName的方式将其加载

然后拿到加载完毕的类，使用BeanUtils.instantiateClass(contextClass);   初始化
等同于Object object = new Object()这种方式。因为它使用的是反射。


这里把上面的流程屡一下。。

public WebApplicationContext initWebApplicationContext(ServletContext servletContext) {
		// 如果上下文中存在 WebApplicationContext 则抛异常。说明WebApplicationContext已经被初始化过了。
		if (servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE) != null) {
			throw new IllegalStateException(
					"Cannot initialize context because there is already a root application context present - " +
					"check whether you have multiple ContextLoader* definitions in your web.xml!");
		}

		Log logger = LogFactory.getLog(ContextLoader.class);
		servletContext.log("Initializing Spring root WebApplicationContext");
		if (logger.isInfoEnabled()) {
			logger.info("Root WebApplicationContext: initialization started");
		}
		long startTime = System.currentTimeMillis();

		try {
			// Store context in local instance variable, to guarantee that
			// it is available on ServletContext shutdown.
			if (this.context == null) {
				// 这里去初始化上下文。具体流程上面已经有讲到
				this.context = createWebApplicationContext(servletContext);
			}
			// 我们已知 从properties中读取到的类为XmlWebApplicationContext 且 XmlWebApplicationContext 是 ConfigurableWebApplicationContext的子类，所以这个判断会进去
			if (this.context instanceof ConfigurableWebApplicationContext) {
				// 强转
				ConfigurableWebApplicationContext cwac = (ConfigurableWebApplicationContext) this.context;
		        // cwac尚未被激活，目前还没有进行配置文件加载
				if (!cwac.isActive()) {
					// The context has not yet been refreshed -> provide services such as
					// setting the parent context, setting the application context id, etc
					if (cwac.getParent() == null) {
						// The context instance was injected without an explicit parent ->
						// determine parent for root web application context, if any.
						ApplicationContext parent = loadParentContext(servletContext);
						cwac.setParent(parent);
					}
					// 加载配置文件。
					configureAndRefreshWebApplicationContext(cwac, servletContext);
				}
			}
			servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, this.context);

			ClassLoader ccl = Thread.currentThread().getContextClassLoader();
			if (ccl == ContextLoader.class.getClassLoader()) {
				currentContext = this.context;
			}
			else if (ccl != null) {
				currentContextPerThread.put(ccl, this.context);
			}

			if (logger.isDebugEnabled()) {
				logger.debug("Published root WebApplicationContext as ServletContext attribute with name [" +
						WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE + "]");
			}
			if (logger.isInfoEnabled()) {
				long elapsedTime = System.currentTimeMillis() - startTime;
				logger.info("Root WebApplicationContext: initialization completed in " + elapsedTime + " ms");
			}

			return this.context;
		}
		catch (RuntimeException ex) {
			logger.error("Context initialization failed", ex);
			servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, ex);
			throw ex;
		}
		catch (Error err) {
			logger.error("Context initialization failed", err);
			servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, err);
			throw err;
		}
	}

然后我们进入加载配置文件那一段



有IOC经验的朋友应该知道，只有真正刷新IOC容器的时候才是IOC容器真正初始化并注册各种bean的时候。这里不再赘述。后续文章会对ioc容器进行覆盖式轰炸。。这里只需要知道概念就OK。

总结: 通过讲解ContextLoaderListener，我们会发现这个东西其实是将javaEE的容器和spring的IOC整合到了一起，之后会把创建完成的上下文对象塞入ServletContext上下文中，注意这里的ServletContext是JavaEE的上下文。也就是说spring自己的上下文在javaee的上下文中只是一个属性。。。至于为什么是这样呢。大家一想就明白了。因为javaee是最外层的规范，而我们的web容器(tomcat, jetty等)实现了这一规范。而spring呢，是需要依托于javaee容器来运行。那么, 就意味着, Spring的IOC容器对于javaEE来讲只是一个属性。。没错。所以javaEE的容器就是spring容器的一个属性。。我们常说子父容器。那么对于spring来讲，他的父容器就是javaEE，子容器就是spring的IOC咯。

本文就写到了这里，希望能帮到大家，谢谢。如果对本文有疑问的请大家留言给我，我会及时回复。
最后打个小广告，欢迎大家加入qq群：77174608 一起讨论学习。。

