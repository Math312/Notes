# Spring-mvc实现

## Spring-mvc使用方式

1. 实现AbstractAnnotationConfigDispatcherServletInitializer

		//在web容器启动的时候创建对象，调用方法来初始化容器以及前端控制器
		public class MyWebApplicationInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {

			//获取根容器的配置类（Spring的配置文件）
			@Override
			protected Class<?>[] getRootConfigClasses() {
				// TODO Auto-generated method stub
				return new Class[] {RootConfiguration.class};
			}
			// 获取Web容器的配置类（Spring MVC配置文件）
			@Override
			protected Class<?>[] getServletConfigClasses() {
				// TODO Auto-generated method stub
				return new Class[] {WebConfiguration.class};
			}

			//获取DispatcherServlet的映射信息
			// “/”拦截所有请求，包括静态资源，不包括*.jsp
			// "/*"连*.jsp页面都拦截
			@Override
			protected String[] getServletMappings() {
				// TODO Auto-generated method stub
				return new String[] {"/"};
			}

		}

根容器配置类：

    @Configuration
    @ComponentScan(excludeFilters= {
            @Filter(type=FilterType.ANNOTATION,value=Controller.class)
    })
    public class RootConfiguration {

    }

Web容器配置类：

    @Configuration
    @ComponentScan(includeFilters= {
            @Filter(type=FilterType.ANNOTATION,value=Controller.class)
    },useDefaultFilters=false)
    public class WebConfiguration {

    }

测试Controller:

    @Controller
    @RequestMapping("/v1")
    public class HelloController {

        @RequestMapping("/hello")
        @ResponseBody
        public String hello() {
            return "hello2";
        }
        
    }

## 实现原理

### 实现AbstractAnnotationConfigDispatcherServletInitializer

使用Spring mvc首先要创建自己的AbstractAnnotationConfigDispatcherServletInitializer的实现类。下面考察该类的结构：

<xmp>
AbstractAnnotationConfigDispatcherServletInitializer
->AbstractDispatcherServletInitializer
        ->AbstractContextLoaderInitializer
            -<>WebApplicationInitializer
</xmp>


考察Spring-mvc包中的META-INF/services/javax.servlet.ServletContainerInitializer文件，文件中内容如下：

    org.springframework.web.SpringServletContainerInitializer

根据Servlet 文档提供的原理我们知道，当servlet容器创建时会调用该文件中提供的类的onStartUp方法：

    @HandlesTypes(WebApplicationInitializer.class)
    public class SpringServletContainerInitializer implements ServletContainerInitializer {

        /**
        * Delegate the {@code ServletContext} to any {@link WebApplicationInitializer}
        * implementations present on the application classpath.
        * <p>Because this class declares @{@code HandlesTypes(WebApplicationInitializer.class)},
        * Servlet 3.0+ containers will automatically scan the classpath for implementations
        * of Spring's {@code WebApplicationInitializer} interface and provide the set of all
        * such types to the {@code webAppInitializerClasses} parameter of this method.
        * <p>If no {@code WebApplicationInitializer} implementations are found on the classpath,
        * this method is effectively a no-op. An INFO-level log message will be issued notifying
        * the user that the {@code ServletContainerInitializer} has indeed been invoked but that
        * no {@code WebApplicationInitializer} implementations were found.
        * <p>Assuming that one or more {@code WebApplicationInitializer} types are detected,
        * they will be instantiated (and <em>sorted</em> if the @{@link
        * org.springframework.core.annotation.Order @Order} annotation is present or
        * the {@link org.springframework.core.Ordered Ordered} interface has been
        * implemented). Then the {@link WebApplicationInitializer#onStartup(ServletContext)}
        * method will be invoked on each instance, delegating the {@code ServletContext} such
        * that each instance may register and configure servlets such as Spring's
        * {@code DispatcherServlet}, listeners such as Spring's {@code ContextLoaderListener},
        * or any other Servlet API componentry such as filters.
        * @param webAppInitializerClasses all implementations of
        * {@link WebApplicationInitializer} found on the application classpath
        * @param servletContext the servlet context to be initialized
        * @see WebApplicationInitializer#onStartup(ServletContext)
        * @see AnnotationAwareOrderComparator
        */
        @Override
        public void onStartup(Set<Class<?>> webAppInitializerClasses, ServletContext servletContext)
                throws ServletException {

            List<WebApplicationInitializer> initializers = new LinkedList<WebApplicationInitializer>();

            if (webAppInitializerClasses != null) {
                for (Class<?> waiClass : webAppInitializerClasses) {
                    // Be defensive: Some servlet containers provide us with invalid classes,
                    // no matter what @HandlesTypes says...
                    if (!waiClass.isInterface() && !Modifier.isAbstract(waiClass.getModifiers()) &&
                            WebApplicationInitializer.class.isAssignableFrom(waiClass)) {
                        try {
                            initializers.add((WebApplicationInitializer) waiClass.newInstance());
                        }
                        catch (Throwable ex) {
                            throw new ServletException("Failed to instantiate WebApplicationInitializer class", ex);
                        }
                    }
                }
            }

            if (initializers.isEmpty()) {
                servletContext.log("No Spring WebApplicationInitializer types detected on classpath");
                return;
            }

            servletContext.log(initializers.size() + " Spring WebApplicationInitializers detected on classpath");
            AnnotationAwareOrderComparator.sort(initializers);
            for (WebApplicationInitializer initializer : initializers) {
                initializer.onStartup(servletContext);
            }
        }

    }

考察该类的实现逻辑：
1. 该类被@HandlesTypes(WebApplicationInitializer.class)修饰，那么项目类结构目录中所有WebApplicationInitializer的子类都会被放到onStartup方法的webAppInitializerClasses属性中。
2. 调用onStartup方法：
   1. 获取所有WebApplicationInitializer类型的类的列表为webAppInitializerClasses
   2. 遍历webAppInitializerClasses中的所有类，如果不是接口不是抽象类就建立一个该类的实例，放到一个名为initializers的列表中。
   3. 遍历结束后对整个列表排序
   4. 遍历调用该列表中对象的onStartup方法

考察WebApplicationInitializer接口的源码：

    public interface WebApplicationInitializer {

        /**
        * Configure the given {@link ServletContext} with any servlets, filters, listeners
        * context-params and attributes necessary for initializing this web application. See
        * examples {@linkplain WebApplicationInitializer above}.
        * @param servletContext the {@code ServletContext} to initialize
        * @throws ServletException if any call against the given {@code ServletContext}
        * throws a {@code ServletException}
        */
        void onStartup(ServletContext servletContext) throws ServletException;

    }

该接口只要求实现onStartup方法，考察其具体实现AbstractContextLoaderInitializer，源码如下：

    @Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		registerContextLoaderListener(servletContext);
	}

    protected void registerContextLoaderListener(ServletContext servletContext) {
		WebApplicationContext rootAppContext = createRootApplicationContext();
		if (rootAppContext != null) {
			ContextLoaderListener listener = new ContextLoaderListener(rootAppContext);
			listener.setContextInitializers(getRootApplicationContextInitializers());
			servletContext.addListener(listener);
		}
		else {
			logger.debug("No ContextLoaderListener registered, as " +
					"createRootApplicationContext() did not return an application context");
		}
	}

该方法用于为rootContext创建ContextLoaderListener，创建逻辑如下：
1. 创建根容器
2. 为根容器创建ContextLoaderListener，并将其添加到servletContext中。

接下来考察AbstractContextLoaderInitializer的父类AbstractDispatcherServletInitializer的onStartup方法：

    public void onStartup(ServletContext servletContext) throws ServletException {
		super.onStartup(servletContext);
		registerDispatcherServlet(servletContext);
	}

    protected void registerDispatcherServlet(ServletContext servletContext) {
		String servletName = getServletName();
		Assert.hasLength(servletName, "getServletName() must not return empty or null");

		WebApplicationContext servletAppContext = createServletApplicationContext();
		Assert.notNull(servletAppContext,
				"createServletApplicationContext() did not return an application " +
				"context for servlet [" + servletName + "]");

		FrameworkServlet dispatcherServlet = createDispatcherServlet(servletAppContext);
		dispatcherServlet.setContextInitializers(getServletApplicationContextInitializers());

		ServletRegistration.Dynamic registration = servletContext.addServlet(servletName, dispatcherServlet);
		Assert.notNull(registration,
				"Failed to register servlet with name '" + servletName + "'." +
				"Check if there is another servlet registered under the same name.");

		registration.setLoadOnStartup(1);
		registration.addMapping(getServletMappings());
		registration.setAsyncSupported(isAsyncSupported());

		Filter[] filters = getServletFilters();
		if (!ObjectUtils.isEmpty(filters)) {
			for (Filter filter : filters) {
				registerServletFilter(servletContext, filter);
			}
		}

		customizeRegistration(registration);
	}

该类的onStartup方法逻辑如下：
1. 调用父类onStartup方法创建根容器
2. 调用registerDispatcherServlet方法创建Web容器
   1. 获取servlet名，默认为dispatcher
   2. 创建Web容器
   3. 创建DispatcherServlet并为其设置属性，并将其添加到servletContext中
   4. 注册Filter
   5. 执行自定义操作

考察该类父类AbstractAnnotationConfigDispatcherServletInitializer发现其只具体实现了两个方法：

    @Override
	protected WebApplicationContext createRootApplicationContext() {
		Class<?>[] configClasses = getRootConfigClasses();
		if (!ObjectUtils.isEmpty(configClasses)) {
			AnnotationConfigWebApplicationContext rootAppContext = new AnnotationConfigWebApplicationContext();
			rootAppContext.register(configClasses);
			return rootAppContext;
		}
		else {
			return null;
		}
	}

    @Override
	protected WebApplicationContext createServletApplicationContext() {
		AnnotationConfigWebApplicationContext servletAppContext = new AnnotationConfigWebApplicationContext();
		Class<?>[] configClasses = getServletConfigClasses();
		if (!ObjectUtils.isEmpty(configClasses)) {
			servletAppContext.register(configClasses);
		}
		return servletAppContext;
	}

这两个方法就是父类创建Root容器和Web容器的具体方法，方法逻辑比较简单，不过需要注意的是，这两个容器的配置文件分别是通过getRootConfigClasses方法和getServletConfigClasses方法获得的。

这也就是为什么我们使用spring-mvc需要实现AbstractAnnotationConfigDispatcherServletInitializer，并且需要完成那几个方法。因为需要创建容器。

这里已经将所有的配置全部设置好了，那什么时候对容器进行刷新呢？由于我们在创建容器的时候为每个容器建立了一个ContextLoaderListener，正是由这个Listener进行初始化操作，我们对其进行分析：

    public class ContextLoaderListener extends ContextLoader implements ServletContextListener {

        /**
        * Create a new {@code ContextLoaderListener} that will create a web application
        * context based on the "contextClass" and "contextConfigLocation" servlet
        * context-params. See {@link ContextLoader} superclass documentation for details on
        * default values for each.
        * <p>This constructor is typically used when declaring {@code ContextLoaderListener}
        * as a {@code <listener>} within {@code web.xml}, where a no-arg constructor is
        * required.
        * <p>The created application context will be registered into the ServletContext under
        * the attribute name {@link WebApplicationContext#ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE}
        * and the Spring application context will be closed when the {@link #contextDestroyed}
        * lifecycle method is invoked on this listener.
        * @see ContextLoader
        * @see #ContextLoaderListener(WebApplicationContext)
        * @see #contextInitialized(ServletContextEvent)
        * @see #contextDestroyed(ServletContextEvent)
        */
        public ContextLoaderListener() {
        }

        /**
        * Create a new {@code ContextLoaderListener} with the given application context. This
        * constructor is useful in Servlet 3.0+ environments where instance-based
        * registration of listeners is possible through the {@link javax.servlet.ServletContext#addListener}
        * API.
        * <p>The context may or may not yet be {@linkplain
        * org.springframework.context.ConfigurableApplicationContext#refresh() refreshed}. If it
        * (a) is an implementation of {@link ConfigurableWebApplicationContext} and
        * (b) has <strong>not</strong> already been refreshed (the recommended approach),
        * then the following will occur:
        * <ul>
        * <li>If the given context has not already been assigned an {@linkplain
        * org.springframework.context.ConfigurableApplicationContext#setId id}, one will be assigned to it</li>
        * <li>{@code ServletContext} and {@code ServletConfig} objects will be delegated to
        * the application context</li>
        * <li>{@link #customizeContext} will be called</li>
        * <li>Any {@link org.springframework.context.ApplicationContextInitializer ApplicationContextInitializer}s
        * specified through the "contextInitializerClasses" init-param will be applied.</li>
        * <li>{@link org.springframework.context.ConfigurableApplicationContext#refresh refresh()} will be called</li>
        * </ul>
        * If the context has already been refreshed or does not implement
        * {@code ConfigurableWebApplicationContext}, none of the above will occur under the
        * assumption that the user has performed these actions (or not) per his or her
        * specific needs.
        * <p>See {@link org.springframework.web.WebApplicationInitializer} for usage examples.
        * <p>In any case, the given application context will be registered into the
        * ServletContext under the attribute name {@link
        * WebApplicationContext#ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE} and the Spring
        * application context will be closed when the {@link #contextDestroyed} lifecycle
        * method is invoked on this listener.
        * @param context the application context to manage
        * @see #contextInitialized(ServletContextEvent)
        * @see #contextDestroyed(ServletContextEvent)
        */
        public ContextLoaderListener(WebApplicationContext context) {
            super(context);
        }


        /**
        * Initialize the root web application context.
        */
        @Override
        public void contextInitialized(ServletContextEvent event) {
            // 初始化并刷新容器
            initWebApplicationContext(event.getServletContext());
        }


        /**
        * Close the root web application context.
        */
        @Override
        public void contextDestroyed(ServletContextEvent event) {
            closeWebApplicationContext(event.getServletContext());
            ContextCleanupListener.cleanupAttributes(event.getServletContext());
        }

    }

    public WebApplicationContext initWebApplicationContext(ServletContext servletContext) {
        // 如果servletContext中存在org.springframework.web.context.WebApplicationContext.ROOT对象
        //这证明rootContext已经被初始化并刷新过了
        //不能重复初始化并刷新
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
            //初始化context
			if (this.context == null) {
				this.context = createWebApplicationContext(servletContext);
			}
			if (this.context instanceof ConfigurableWebApplicationContext) {
                //设置父context
				ConfigurableWebApplicationContext cwac = (ConfigurableWebApplicationContext) this.context;
				if (!cwac.isActive()) {
					// The context has not yet been refreshed -> provide services such as
					// setting the parent context, setting the application context id, etc
					if (cwac.getParent() == null) {
						// The context instance was injected without an explicit parent ->
						// determine parent for root web application context, if any.
						ApplicationContext parent = loadParentContext(servletContext);
						cwac.setParent(parent);
					}
                    //配置和刷新容器
					configureAndRefreshWebApplicationContext(cwac, servletContext);
				}
			}
            // 将容器加入到servletContext中
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

考虑这个函数的逻辑：
1. 首先判断根容器是否已经创建，如果创建则抛出异常，否则开始创建
2. 创建根容器并设置父容器
3. 配置并刷新容器
4. 将容器放入servletContext

创建容器代码如下：

    protected WebApplicationContext createWebApplicationContext(ServletContext sc) {
        //获取Context的Class类型
		Class<?> contextClass = determineContextClass(sc);
        //如果获取的类型是ConfigurableWebApplicationContext
		if (!ConfigurableWebApplicationContext.class.isAssignableFrom(contextClass)) {
			throw new ApplicationContextException("Custom context class [" + contextClass.getName() +
					"] is not of type [" + ConfigurableWebApplicationContext.class.getName() + "]");
		}
        //创建容器实例
		return (ConfigurableWebApplicationContext) BeanUtils.instantiateClass(contextClass);
	}

    protected Class<?> determineContextClass(ServletContext servletContext) {
        // 获取容器类型名字
		String contextClassName = servletContext.getInitParameter(CONTEXT_CLASS_PARAM);
        // 获取对应名称的class名称
		if (contextClassName != null) {
			try {
				return ClassUtils.forName(contextClassName, ClassUtils.getDefaultClassLoader());
			}
			catch (ClassNotFoundException ex) {
				throw new ApplicationContextException(
						"Failed to load custom context class [" + contextClassName + "]", ex);
			}
		}
		else {
            //获取默认方式的容器名
			contextClassName = defaultStrategies.getProperty(WebApplicationContext.class.getName());
			try {
				return ClassUtils.forName(contextClassName, ContextLoader.class.getClassLoader());
			}
			catch (ClassNotFoundException ex) {
				throw new ApplicationContextException(
						"Failed to load default context class [" + contextClassName + "]", ex);
			}
		}
	}

    static {
		// Load default strategy implementations from properties file.
		// This is currently strictly internal and not meant to be customized
		// by application developers.
		try {
            // 从ContextLoader.properties文件中加载属性
            // 从中获取的全类名是org.springframework.web.context.support.XmlWebApplicationContext
			ClassPathResource resource = new ClassPathResource(DEFAULT_STRATEGIES_PATH, ContextLoader.class);
			defaultStrategies = PropertiesLoaderUtils.loadProperties(resource);
		}
		catch (IOException ex) {
			throw new IllegalStateException("Could not load 'ContextLoader.properties': " + ex.getMessage());
		}
	}

刷新容器函数如下：

    protected void configureAndRefreshWebApplicationContext(ConfigurableWebApplicationContext wac, ServletContext sc) {
		if (ObjectUtils.identityToString(wac).equals(wac.getId())) {
			// The application context id is still set to its original default value
			// -> assign a more useful id based on available information
			String idParam = sc.getInitParameter(CONTEXT_ID_PARAM);
			if (idParam != null) {
				wac.setId(idParam);
			}
			else {
				// Generate default id...
				wac.setId(ConfigurableWebApplicationContext.APPLICATION_CONTEXT_ID_PREFIX +
						ObjectUtils.getDisplayString(sc.getContextPath()));
			}
		}

		wac.setServletContext(sc);
		String configLocationParam = sc.getInitParameter(CONFIG_LOCATION_PARAM);
		if (configLocationParam != null) {
			wac.setConfigLocation(configLocationParam);
		}

		// The wac environment's #initPropertySources will be called in any case when the context
		// is refreshed; do it eagerly here to ensure servlet property sources are in place for
		// use in any post-processing or initialization that occurs below prior to #refresh
		ConfigurableEnvironment env = wac.getEnvironment();
		if (env instanceof ConfigurableWebEnvironment) {
			((ConfigurableWebEnvironment) env).initPropertySources(sc, null);
		}

		customizeContext(sc, wac);
		wac.refresh();
	}

根容器的部分我们已经分析完毕，剩下的就是DispatcherServlet部分，首先需要分析一下每个Servlet的生命周期。

Servlet生命周期是由servlet容器来控制的，可以分为3个阶段：初始化、运行和销毁：
1. 初始化阶段：
   1. servlet容器加载servlet类，把servlet类的.class文件中的数据读到内存中。
   2. servlet容器创建一个ServletConfig对象。ServletConfig对象包含了servlet的初始化配置信息
   3. servlet容器创建一个servlet对象
   4. servlet容器调用servlet对象的init方法进行初始化。
2. 运行阶段：
   当servlet容器接收到一个请求时，servlet容器会针对这个请求创建servletRequest和servletResponse对象，然后调用service方法。并把这两个参数传递给service方法。service方法通过servletRequest对象获得请求的信息，并处理该请求。再通过servletResponse对象生成这个请求的响应结果。然后销毁servletRequest和servletResponse对象。我们不管这个请求时post提交的还是get提交的，最终这个请求都会由service方法来处理。
3. 销毁阶段：
   当web应用被终止时，servlet容器会先调用servlet对象的destroy方法，然后再销毁servlet对象，同时也会销毁与servlet对象相关联的servletConfig对象。我们可以再destroy方法的实现中，释放servlet所占用的资源。

下面考察DispatcherServlet类的源码，首先考察其类继承结构：
<xmp>
DispatcherServlet
->FrameworkServlet
    ->HttpServletBean
        ->HttpServlet
            ->GenericServlet
        -<>EnvironmentCapable
        -<>EnvironmentAware
    -<>ApplicationContextAware
</xmp>

我们首先考虑DispatcherServlet类的init方法，该方法由其父类HttpServletBean实现：

    public final void init() throws ServletException {
		if (logger.isDebugEnabled()) {
			logger.debug("Initializing servlet '" + getServletName() + "'");
		}

		// Set bean properties from init parameters.
        // 解析init-param并封装到pvs中
		PropertyValues pvs = new ServletConfigPropertyValues(getServletConfig(), this.requiredProperties);
		if (!pvs.isEmpty()) {
			try {
                //将当前这个Servlet类转化为一个BeanWrapper，从而能够以Spring的方式来对init-param的值进行注入
				BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(this);
				ResourceLoader resourceLoader = new ServletContextResourceLoader(getServletContext());
                //注册自定义属性编辑器，一旦遇到Resource类型的属性将会使用ResourceEditor进行解析。
				bw.registerCustomEditor(Resource.class, new ResourceEditor(resourceLoader, getEnvironment()));
                // 空实现，留给子类覆盖
				initBeanWrapper(bw);
                //属性注入
				bw.setPropertyValues(pvs, true);
			}
			catch (BeansException ex) {
				if (logger.isErrorEnabled()) {
					logger.error("Failed to set bean properties on servlet '" + getServletName() + "'", ex);
				}
				throw ex;
			}
		}

		// Let subclasses do whatever initialization they like.
        // 留给子类扩展
		initServletBean();

		if (logger.isDebugEnabled()) {
			logger.debug("Servlet '" + getServletName() + "' configured successfully");
		}
	}


上述函数逻辑如下：
1. 封装及验证初始化参数
2. 将当前servlet实例转化成BeanWrapper实例
3. 注册相对于Resource的属性编辑器
4. 属性注入
5. servletBean的初始化

接下来我们分别对其进行分析：

1. 封装及验证初始化参数
   ServletConfigPropertyValues除了封装属性外还有对属性验证的功能：

        public ServletConfigPropertyValues(ServletConfig config, Set<String> requiredProperties)
				throws ServletException {

			//对传入的属性进行一个复制
            Set<String> missingProps = (!CollectionUtils.isEmpty(requiredProperties) ?
					new HashSet<String>(requiredProperties) : null);
            //获取ServletConfig中的初始化参数
			Enumeration<String> paramNames = config.getInitParameterNames();
            // 对于每一个需要的属性进行检测，如果该属性存在将其添加，并从missingProps中移除
			while (paramNames.hasMoreElements()) {
				String property = paramNames.nextElement();
				Object value = config.getInitParameter(property);
				addPropertyValue(new PropertyValue(property, value));
				if (missingProps != null) {
					missingProps.remove(property);
				}
			}

			// Fail if we are still missing properties.
            // 如果存在属性没有配置则抛出异常
			if (!CollectionUtils.isEmpty(missingProps)) {
				throw new ServletException(
						"Initialization from ServletConfig for servlet '" + config.getServletName() +
						"' failed; the following required properties were missing: " +
						StringUtils.collectionToDelimitedString(missingProps, ", "));
			}
		}

2. 将当前servlet实例转化成BeanWrapper实例
   PropertyAccessorFactory.forBeanPropertyAccess是Spring种提供的工具方法，主要用于将指定实例转化为Spring种而可以处理的BeanWrapper类型的实例。
3. 注册相对于Resource的属性编辑器
   这里的属性编辑器的目的是在对当前实例（DispatcherServlet）属性注入过程中一旦遇到Resource类型的属性就会使用ResourceEditor去解析。
4. 属性注入
   BeanWrapper为Spring中的方法，支持Spring的自动注入。其实我们最常用的属性注入无非是contextAttribute、contextClass、nameSpace、contextConfigLocation等属性。
5. servletBean的初始化
   考察initServletBean()方法，父类FrameworkServlet覆盖了HttpServletBean中的initServletBean函数，源码如下：

        protected final void initServletBean() throws ServletException {
            getServletContext().log("Initializing Spring FrameworkServlet '" + getServletName() + "'");
            if (this.logger.isInfoEnabled()) {
                this.logger.info("FrameworkServlet '" + getServletName() + "': initialization started");
            }
            // 记录初始化开始时间
            long startTime = System.currentTimeMillis();

            try {
                //初始化Web容器
                this.webApplicationContext = initWebApplicationContext();
                //设计为子类覆盖
                initFrameworkServlet();
            }
            catch (ServletException ex) {
                this.logger.error("Context initialization failed", ex);
                throw ex;
            }
            catch (RuntimeException ex) {
                this.logger.error("Context initialization failed", ex);
                throw ex;
            }

            if (this.logger.isInfoEnabled()) {
                //打印初始化时间
                long elapsedTime = System.currentTimeMillis() - startTime;
                this.logger.info("FrameworkServlet '" + getServletName() + "': initialization completed in " +
                        elapsedTime + " ms");
            }
        }

    考察initWebApplicationContext方法：

        protected WebApplicationContext initWebApplicationContext() {
            WebApplicationContext rootContext =
                    WebApplicationContextUtils.getWebApplicationContext(getServletContext());
            WebApplicationContext wac = null;

            if (this.webApplicationContext != null) {
                // A context instance was injected at construction time -> use it
                // context实例在构造函数中被注入
                wac = this.webApplicationContext;
                if (wac instanceof ConfigurableWebApplicationContext) {
                    ConfigurableWebApplicationContext cwac = (ConfigurableWebApplicationContext) wac;
                    if (!cwac.isActive()) {
                        // The context has not yet been refreshed -> provide services such as
                        // setting the parent context, setting the application context id, etc
                        if (cwac.getParent() == null) {
                            // The context instance was injected without an explicit parent -> set
                            // the root application context (if any; may be null) as the parent
                            cwac.setParent(rootContext);
                        }
                        // 刷新上下文
                        configureAndRefreshWebApplicationContext(cwac);
                    }
                }
            }
            if (wac == null) {
                // No context instance was injected at construction time -> see if one
                // has been registered in the servlet context. If one exists, it is assumed
                // that the parent context (if any) has already been set and that the
                // user has performed any initialization such as setting the context id
                //根据contextAttribute属性加载WebApplicationContext
                wac = findWebApplicationContext();
            }
            if (wac == null) {
                // No context instance is defined for this servlet -> create a local one
                wac = createWebApplicationContext(rootContext);
            }

            if (!this.refreshEventReceived) {
                // Either the context is not a ConfigurableApplicationContext with refresh
                // support or the context injected at construction time had already been
                // refreshed -> trigger initial onRefresh manually here.
                onRefresh(wac);
            }

            if (this.publishContext) {
                // Publish the context as a servlet context attribute.
                String attrName = getServletContextAttributeName();
                getServletContext().setAttribute(attrName, wac);
                if (this.logger.isDebugEnabled()) {
                    this.logger.debug("Published WebApplicationContext of servlet '" + getServletName() +
                            "' as ServletContext attribute with name [" + attrName + "]");
                }
            }

            return wac;
        }

   1. 寻找或创建对应的WebApplicationContext实例
      1. 通过构造函数的注入进行初始化
         当进入initWebApplicationContext函数后通过判断this.webApplication!=null后，便可以确定this.webApplicationContext是否是通过构造函数来初始化的。
      2. 通过contextAttribute进行初始化
         通过在web.xml文件中配置的servlet参数contextAttribute来查找ServletContext中对应的属性，默认为WebApplicationContext.class.getName()+".ROOT"，也就是在ContextLoaderListener加载时会创建WebApplicationContext实例，并将实例以WebApplicationContext.class.getName()+".ROOT"为key放入ServletContext中，当然读者可以重写初始化逻辑使用自己创建的WebApplicationContext，并在servlet的配置中通过初始化参数contextAttribute指定key。

                protected WebApplicationContext findWebApplicationContext() {
                    String attrName = getContextAttribute();
                    if (attrName == null) {
                        return null;
                    }
                    WebApplicationContext wac =
                            WebApplicationContextUtils.getWebApplicationContext(getServletContext(), attrName);
                    if (wac == null) {
                        throw new IllegalStateException("No WebApplicationContext found: initializer not registered?");
                    }
                    return wac;
                }

        3. 重新创建WebApplicationContext实例。
           如果通过以上的两种方式都没有找到任何突破，那就只能重新创建新的实例了。

                protected WebApplicationContext createWebApplicationContext(WebApplicationContext parent) {
                    return createWebApplicationContext((ApplicationContext) parent);
                }

                protected WebApplicationContext createWebApplicationContext(ApplicationContext parent) {
                    // 获取servlet的初始化参数contextClass，如果没有配置默认为XmlWebApplicationContext.class
                    Class<?> contextClass = getContextClass();
                    if (this.logger.isDebugEnabled()) {
                        this.logger.debug("Servlet with name '" + getServletName() +
                                "' will try to create custom WebApplicationContext context of class '" +
                                contextClass.getName() + "'" + ", using parent context [" + parent + "]");
                    }
                    if (!ConfigurableWebApplicationContext.class.isAssignableFrom(contextClass)) {
                        throw new ApplicationContextException(
                                "Fatal initialization error in servlet with name '" + getServletName() +
                                "': custom WebApplicationContext class [" + contextClass.getName() +
                                "] is not of type ConfigurableWebApplicationContext");
                    }
                    // 通过反射方式实例化contextClass
                    ConfigurableWebApplicationContext wac =
                            (ConfigurableWebApplicationContext) BeanUtils.instantiateClass(contextClass);
                    //parent为在ContextLoaderListener中创建的实例
                    //在ContextLoaderListener加载的时候初始化的WebApplicationContext类型实例
                    wac.setEnvironment(getEnvironment());
                    wac.setParent(parent);
                    //获取contextConfigLocation属性，配置在servlet初始化参数中
                    wac.setConfigLocation(getContextConfigLocation());
                    //初始化Spring环境包括加载配置文件等
                    configureAndRefreshWebApplicationContext(wac);

                    return wac;
                }
     2. configureAndRefreshWebApplicationContext
        这个函数之前分析过，这里不多做解释。 
     3. 刷新
        onRefresh是FrameworkServlet类所提供的模板方法，在其子类DispatcherServlet中进行了重写，主要用于刷新Spring在Web功能实现中所必须使用的全局变量。源码如下：

            @Override
            protected void onRefresh(ApplicationContext context) {
                initStrategies(context);
            } 
            protected void initStrategies(ApplicationContext context) {
                // 初始化MultipartResolver
                initMultipartResolver(context);
                // 初始化LocaleResolver
                initLocaleResolver(context);
                // 初始化ThemeResolver
                initThemeResolver(context);
                // 初始化HandlerMappings
                initHandlerMappings(context);
                // 初始化HandlerAdapters
                initHandlerAdapters(context);
                // 初始化HandlerExceptionResolvers
                initHandlerExceptionResolvers(context);
                // 初始化RequestToViewNameTranslator
                initRequestToViewNameTranslator(context);
                // 初始化ViewResolvers
                initViewResolvers(context);
                // 初始化FlashMapManager
                initFlashMapManager(context);
            }

        1. 初始化MultipartResolver
           在Spring中，MultipartResolver主要用来处理文件上传。默认情况下，Spring是没有multipart处理的，因为一些开发者想要自己处理它们。如果想使用Spring的multipart，则需要在Web应用的上下文中添加multipart解析器。这样，这个请求就会被就检查是否包含multipart。然而，如果请求中包含multipart，那么上下文中定义的MultipartResolver就会解析它，这样请求中的multipart属性就会像其他属性一样被处理。 

                private void initMultipartResolver(ApplicationContext context) {
                    try {
                        this.multipartResolver = context.getBean(MULTIPART_RESOLVER_BEAN_NAME, MultipartResolver.class);
                        if (logger.isDebugEnabled()) {
                            logger.debug("Using MultipartResolver [" + this.multipartResolver + "]");
                        }
                    }
                    catch (NoSuchBeanDefinitionException ex) {
                        // Default is no multipart resolver.
                        this.multipartResolver = null;
                        if (logger.isDebugEnabled()) {
                            logger.debug("Unable to locate MultipartResolver with name '" + MULTIPART_RESOLVER_BEAN_NAME +
                                    "': no multipart request handling provided");
                        }
                    }
                }

            因为之前的步骤已经完成了Spring中配置文件的解析，所以只要在配置文件中注册过都可以通过通过ApplicationContext提供的getBean方法来直接获取对应bean，进而初始化DispatcherServlet中的multipareResolver变量。

        2. 初始化LocaleResolver
           在Spring的国际化配置中一共有3种使用方式。
           - 基于URL参数的配置
             使用这种方式需要注册org.Springframework.web.servlet.il8n.AcceptHeaderLocaleResolver类型的bean，id为localResolver 
           - 基于Session的配置
             使用这种方式需要注册org.Springframework.web.servlet.il8n.SessionLocaleResolver类型的bean，id为localResolver  
           - 基于Cookie的配置 
             使用这种方式需要注册org.Springframework.web.servlet.il8n.CookieLocaleResolver类型的bean，id为localResolver   

            初始化源码如下：

                private void initLocaleResolver(ApplicationContext context) {
                    try {
                        this.localeResolver = context.getBean(LOCALE_RESOLVER_BEAN_NAME, LocaleResolver.class);
                        if (logger.isDebugEnabled()) {
                            logger.debug("Using LocaleResolver [" + this.localeResolver + "]");
                        }
                    }
                    catch (NoSuchBeanDefinitionException ex) {
                        // We need to use the default.
                        this.localeResolver = getDefaultStrategy(context, LocaleResolver.class);
                        if (logger.isDebugEnabled()) {
                            logger.debug("Unable to locate LocaleResolver with name '" + LOCALE_RESOLVER_BEAN_NAME +
                                    "': using default [" + this.localeResolver + "]");
                        }
                    }
                }

            3. 初始化ThemeResolver
               Web开发中经常会遇到通过主题Theme来控制网页风格，这将进一步改善用户体验。简单地说，一个主题就是一组静态资源（比如样式表和图片），它们可以影响应用程序的视觉效果。Spring的主题功能和国际化功能非常类似。构成Spring主题功能主要包括以下内容。
               - 主题资源
                 org.Springframework.ui.context.ThemeSource是Spring中主题资源的接口，Spring的主题需要通过ThemeSource接口来实现存放主题信息的资源。
                 org.Springframework.ui.context.support.ResourceBundleThemeSource是ThemeSource接口默认是新啊类（也就是通过ResourceBundle资源的方式定义主题），在Spring需要配置该bean。 
               - 主题解析器
                 org.Springframework.web.servlet.ThemeResolver是主题解析器的接口，主题解析的工作便是由它的子类来完成。 
               - 拦截器
                 Spring提供了ThemeChangeInterceptor拦截器来改变主题。

               解析代码也很简单：

                    private void initThemeResolver(ApplicationContext context) {
                        try {
                            this.themeResolver = context.getBean(THEME_RESOLVER_BEAN_NAME, ThemeResolver.class);
                            if (logger.isDebugEnabled()) {
                                logger.debug("Using ThemeResolver [" + this.themeResolver + "]");
                            }
                        }
                        catch (NoSuchBeanDefinitionException ex) {
                            // We need to use the default.
                            this.themeResolver = getDefaultStrategy(context, ThemeResolver.class);
                            if (logger.isDebugEnabled()) {
                                logger.debug("Unable to locate ThemeResolver with name '" + THEME_RESOLVER_BEAN_NAME +
                                        "': using default [" + this.themeResolver + "]");
                            }
                        }
                    }
            
            4. 初始化HandlerMappings
                当客户端发出Request时DispatcherServlet会将Request提交给HandlerMapping，然后HandlerMapping根据WebApplication Context的配置来回传给DispatcherServlet相应的Controller。
                在基于SpringMVC的Web应用程序中，我们可以为DispatcherServlet提供多个HandlerMapping供其使用。DispatcherServlet在选用HandlerMapping的过程中，将根据物品们所指定的一系列HandlerMapping的优先级进行排序，然后优先使用优先级在前的HandlerMapping。如果当前的Handlermapping能够返回可用的Handler，DispatcherServlet则使用当前返回的Handler进行的Web请求的处理，而不再继续询问其他的HandlerMapping。否则，DispatcherServlet将继续按照各个HandlerMapping的优先级进行询问，直到获取一个可用的Handler为止，初始化配置如下：
                 
                    private void initHandlerMappings(ApplicationContext context) {
                        this.handlerMappings = null;
                        // 如果指定了detectAllHandlerMappings为true
                        if (this.detectAllHandlerMappings) {
                            // Find all HandlerMappings in the ApplicationContext, including ancestor contexts.
                            // 获取容器中所有的HandlerMapping bean
                            Map<String, HandlerMapping> matchingBeans =
                                    BeanFactoryUtils.beansOfTypeIncludingAncestors(context, HandlerMapping.class, true, false);
                            if (!matchingBeans.isEmpty()) {
                                this.handlerMappings = new ArrayList<HandlerMapping>(matchingBeans.values());
                                // We keep HandlerMappings in sorted order.
                                // 对其进行排序
                                AnnotationAwareOrderComparator.sort(this.handlerMappings);
                            }
                        }
                        else {
                            try {
                                // 获取HandlerMapping bean
                                HandlerMapping hm = context.getBean(HANDLER_MAPPING_BEAN_NAME, HandlerMapping.class);
                                
                                this.handlerMappings = Collections.singletonList(hm);
                            }
                            catch (NoSuchBeanDefinitionException ex) {
                                // Ignore, we'll add a default HandlerMapping later.
                            }
                        }

                        // Ensure we have at least one HandlerMapping, by registering
                        // a default HandlerMapping if no other mappings are found.
                        if (this.handlerMappings == null) {
                            this.handlerMappings = getDefaultStrategies(context, HandlerMapping.class);
                            if (logger.isDebugEnabled()) {
                                logger.debug("No HandlerMappings found in servlet '" + getServletName() + "': using default");
                            }
                        }
                    }
        5. 初始化HandlerAdapters
           首先分析初始化HandlerAdapter源码：

                private void initHandlerAdapters(ApplicationContext context) {
                    this.handlerAdapters = null;

                    if (this.detectAllHandlerAdapters) {
                        // Find all HandlerAdapters in the ApplicationContext, including ancestor contexts.
                        Map<String, HandlerAdapter> matchingBeans =
                                BeanFactoryUtils.beansOfTypeIncludingAncestors(context, HandlerAdapter.class, true, false);
                        if (!matchingBeans.isEmpty()) {
                            this.handlerAdapters = new ArrayList<HandlerAdapter>(matchingBeans.values());
                            // We keep HandlerAdapters in sorted order.
                            AnnotationAwareOrderComparator.sort(this.handlerAdapters);
                        }
                    }
                    else {
                        try {
                            HandlerAdapter ha = context.getBean(HANDLER_ADAPTER_BEAN_NAME, HandlerAdapter.class);
                            this.handlerAdapters = Collections.singletonList(ha);
                        }
                        catch (NoSuchBeanDefinitionException ex) {
                            // Ignore, we'll add a default HandlerAdapter later.
                        }
                    }

                    // Ensure we have at least some HandlerAdapters, by registering
                    // default HandlerAdapters if no other adapters are found.
                    if (this.handlerAdapters == null) {
                        this.handlerAdapters = getDefaultStrategies(context, HandlerAdapter.class);
                        if (logger.isDebugEnabled()) {
                            logger.debug("No HandlerAdapters found in servlet '" + getServletName() + "': using default");
                        }
                    }
                } 

            同样在初始化的过程中涉及了一个变量detectAllHandlerAdapters，detectAllHandlerAdapters作用和detectAllHandlerMappings类似，只不过作用对象是handlerAdapter。如果经过这两个没有获得handlerAdapters那么，就使用默认的创建逻辑，默认创建逻辑如下：

                protected <T> List<T> getDefaultStrategies(ApplicationContext context, Class<T> strategyInterface) {
                    String key = strategyInterface.getName();
                    String value = defaultStrategies.getProperty(key);
                    if (value != null) {
                        String[] classNames = StringUtils.commaDelimitedListToStringArray(value);
                        List<T> strategies = new ArrayList<T>(classNames.length);
                        for (String className : classNames) {
                            try {
                                Class<?> clazz = ClassUtils.forName(className, DispatcherServlet.class.getClassLoader());
                                Object strategy = createDefaultStrategy(context, clazz);
                                strategies.add((T) strategy);
                            }
                            catch (ClassNotFoundException ex) {
                                throw new BeanInitializationException(
                                        "Could not find DispatcherServlet's default strategy class [" + className +
                                                "] for interface [" + key + "]", ex);
                            }
                            catch (LinkageError err) {
                                throw new BeanInitializationException(
                                        "Error loading DispatcherServlet's default strategy class [" + className +
                                                "] for interface [" + key + "]: problem with class file or dependent class", err);
                            }
                        }
                        return strategies;
                    }
                    else {
                        return new LinkedList<T>();
                    }
                }

            在getDefaultStrategies函数中，Spring会尝试从defaultStrategies中加载对应的HandlerAdapter的属性。考察defaultStrategies属性：

                private static final Properties defaultStrategies;
                static {
                    // Load default strategy implementations from properties file.
                    // This is currently strictly internal and not meant to be customized
                    // by application developers.
                    try {
                        // DEFAULT_STRATEGIES_PATH = "DispatcherServlet.properties"
                        // 从DispatcherServlet.properties中加载属性
                        ClassPathResource resource = new ClassPathResource(DEFAULT_STRATEGIES_PATH, DispatcherServlet.class);
                        defaultStrategies = PropertiesLoaderUtils.loadProperties(resource);
                    }
                    catch (IOException ex) {
                        throw new IllegalStateException("Could not load '" + DEFAULT_STRATEGIES_PATH + "': " + ex.getMessage());
                    }
                }

            在系统加载的时候defaultStrategies根据当前路径DispatcherServlet.properties来初始化本身，查看该文件：

                org.springframework.web.servlet.HandlerAdapter=org.springframework.web.servlet.mvc.HttpRequestHandlerAdapter,\
                    org.springframework.web.servlet.mvc.SimpleControllerHandlerAdapter,\
                    org.springframework.web.servlet.mvc.annotation.AnnotationMethodHandlerAdapter

            由此得知，如果程序开发人员没有在配置文件中定义自己的适配器，那么Spring会默认加载配置文件中的3个适配器。

            作为总控制器的派遣器servlet通过处理器应红色到处理器后，会轮询处理器适配器模块，查找能够处理当前HTTP请求的处理器适配器的实现，处理器适配器模块根据处理器映射返回的处理器类型，例如简单的控制器类型、注解控制器类型或者远程调用控制器类型，来选择某一个适合的处理器适配器的实现，从而适配当前的HTTP请求。
            - HTTP请求处理器适配器（HttpRequestHandlerAdapter）
              HTTP请求处理器适配器仅仅支持对HTTP请求处理器的适配。它简单地将HTTP请求对象和相应对象传递给HTTP请求处理器的实现，它并不需要返回值。它主要应用在基于HTTP的远程调用的是线上。
            - 简单控制器处理器适配器（SimpleControllerHandlerAdapter）
              这个实现类将HTTP请求适配到一个控制器的实现进行处理。这里控制器的实现是一个简单的控制器接口的实现。简单控制器处理器适配器被设计成一个框架类的实现，不需要被改写，客户化的业务逻辑通常是在控制器接口的实现类中实现的。
            - 注解方法处理器适配器（AnnotationMethodHandlerAdapter）
              这个类的实现是基于注解的实现，它需要结合注解方法映射和注解方法处理器协同工作。它通过解析声明在注解控制器的请求映射信息来解析相应的处理器方法来处理当前的HTTP请求。在处理过程中，他通过反射来发现探测处理器方法的参数，调用处理器方法，并且映射返回值到模型和控制器对象，最后返回模型和控制器对象给作为主控制器的派遣器Servlet。

        6. 初始化HandlerExceptionResolvers。
           基于HandlerExceptionResolver接口的异常处理，使用这种方式只需要实现resolveException方法，该方法返回一个ModelAndView对象，在方法内部对异常类型进行判断，然后尝试生成对应的ModelAndView对象，如果该方法返回了null，则Spring会继续寻找其他的实现了HandlerExceptionResolver接口的bean。换句话说，Spring会搜索所有注册在其环境中的实现了HandlerExceptionResolver接口的bean，逐个执行，直到返回了一个ModelAndView对象。


                private void initHandlerExceptionResolvers(ApplicationContext context) {
                    this.handlerExceptionResolvers = null;

                    if (this.detectAllHandlerExceptionResolvers) {
                        // Find all HandlerExceptionResolvers in the ApplicationContext, including ancestor contexts.
                        Map<String, HandlerExceptionResolver> matchingBeans = BeanFactoryUtils
                                .beansOfTypeIncludingAncestors(context, HandlerExceptionResolver.class, true, false);
                        if (!matchingBeans.isEmpty()) {
                            this.handlerExceptionResolvers = new ArrayList<HandlerExceptionResolver>(matchingBeans.values());
                            // We keep HandlerExceptionResolvers in sorted order.
                            AnnotationAwareOrderComparator.sort(this.handlerExceptionResolvers);
                        }
                    }
                    else {
                        try {
                            HandlerExceptionResolver her =
                                    context.getBean(HANDLER_EXCEPTION_RESOLVER_BEAN_NAME, HandlerExceptionResolver.class);
                            this.handlerExceptionResolvers = Collections.singletonList(her);
                        }
                        catch (NoSuchBeanDefinitionException ex) {
                            // Ignore, no HandlerExceptionResolver is fine too.
                        }
                    }   

            7. 初始化RequestToViewNameTranslator
               当Controller处理器方法没有返回一个View对象或逻辑视图名称，并且在该方法中没有直接网reponse的输出流里面写数据的时候，Spring就会采用约定好的方式提供一个逻辑视图名称。这个逻辑视图名称是通过Spring定义的org.Springframework.web.servlet.RequestToViewNameTranslator接口的getViewname方法来实现的，我们可以实现自己的RequestToViewNameTranslator接口来约定好没有返回视图名称的时候如何确定视图名称。Spring已经给我们提供了一个它自己的实现，那就是org.Springframework.web.servlet.view.DisfaultRequestToViewNameTranslator。

                    private void initRequestToViewNameTranslator(ApplicationContext context) {
                        try {
                            this.viewNameTranslator =
                                    context.getBean(REQUEST_TO_VIEW_NAME_TRANSLATOR_BEAN_NAME, RequestToViewNameTranslator.class);
                            if (logger.isDebugEnabled()) {
                                logger.debug("Using RequestToViewNameTranslator [" + this.viewNameTranslator + "]");
                            }
                        }
                        catch (NoSuchBeanDefinitionException ex) {
                            // We need to use the default.
                            this.viewNameTranslator = getDefaultStrategy(context, RequestToViewNameTranslator.class);
                            if (logger.isDebugEnabled()) {
                                logger.debug("Unable to locate RequestToViewNameTranslator with name '" +
                                        REQUEST_TO_VIEW_NAME_TRANSLATOR_BEAN_NAME + "': using default [" + this.viewNameTranslator +
                                        "]");
                            }
                        }
                    }

            8. 初始化ViewResolvers
               在SpringMVC中，当Controller讲请求处理结果放入到ModelAndView中以后，DispatcherServlet会根据ModelAndView选择合适的视图及逆行渲染。那么在SpringMVC中是如何选择合适的View呢？View对象是如何创建的呢？答案就在ViewResolver中。ViewResolver接口定义了resolverViewName方法。根据viewName创建合适类型的View实现。其初始化方法如下：

                    private void initViewResolvers(ApplicationContext context) {
                        this.viewResolvers = null;

                        if (this.detectAllViewResolvers) {
                            // Find all ViewResolvers in the ApplicationContext, including ancestor contexts.
                            Map<String, ViewResolver> matchingBeans =
                                    BeanFactoryUtils.beansOfTypeIncludingAncestors(context, ViewResolver.class, true, false);
                            if (!matchingBeans.isEmpty()) {
                                this.viewResolvers = new ArrayList<ViewResolver>(matchingBeans.values());
                                // We keep ViewResolvers in sorted order.
                                AnnotationAwareOrderComparator.sort(this.viewResolvers);
                            }
                        }
                        else {
                            try {
                                ViewResolver vr = context.getBean(VIEW_RESOLVER_BEAN_NAME, ViewResolver.class);
                                this.viewResolvers = Collections.singletonList(vr);
                            }
                            catch (NoSuchBeanDefinitionException ex) {
                                // Ignore, we'll add a default ViewResolver later.
                            }
                        }
            9. 初始化FlashMapManager
               SpringMVC Flash attributes提供了一个请求存储属性，可供其他请求使用。在使用重定向时候非常必要，例如Post/Redirect/Get模式。Flash attributes在重定向之前暂存（就像存在session中）  以便重定向之后还能使用，并立即删除。
               SpringMVC 有两个主要的抽象来支持flash attributes。FlashMap用于保持flash attributes，而FlashMapManager用于存储、检索、管理FlashMap实例。
               flash attribute支持默认开启（"on"）而不需要显示启用，它永远不会导致HTTP Session的创建。这两个FlashMap实例都可以通过静态方法RequestContextUtils从Spring MVC的任何位置访问。
               flashMapManager的初始化在initFlashMapManager中完成。

                    private void initFlashMapManager(ApplicationContext context) {
                        try {
                            this.flashMapManager = context.getBean(FLASH_MAP_MANAGER_BEAN_NAME, FlashMapManager.class);
                            if (logger.isDebugEnabled()) {
                                logger.debug("Using FlashMapManager [" + this.flashMapManager + "]");
                            }
                        }
                        catch (NoSuchBeanDefinitionException ex) {
                            // We need to use the default.
                            this.flashMapManager = getDefaultStrategy(context, FlashMapManager.class);
                            if (logger.isDebugEnabled()) {
                                logger.debug("Unable to locate FlashMapManager with name '" +
                                        FLASH_MAP_MANAGER_BEAN_NAME + "': using default [" + this.flashMapManager + "]");
                            }
                        }
                    }

## DispatcherServlet的逻辑处理

HttpServlet类中分别提供了相应的服务方法，它们是doDelete()、doGet()、doOptions()、doPost()、doPut()和doTrace()。它会根据请求的不同形式讲程序引导至对应的函数进行处理。这几个函数中最常用的函数无非就是doGet()和doPost()，那么我们直接查看DisptacherServlet中对于这两个函数的逻辑实现。其实doPut()和doDelete()也是类似逻辑：

    protected final void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		processRequest(request, response);
	}

    protected final void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		processRequest(request, response);
	}

    protected final void doPut(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		processRequest(request, response);
	}

    protected final void doDelete(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		processRequest(request, response);
	}

这四个方法都是调用processRequest方法进行处理，查看该方法如下：

    protected final void processRequest(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

        //记录当前时间，用于计算web请求的处理时间
		long startTime = System.currentTimeMillis();
		Throwable failureCause = null;

        // Expose current LoacaleResolver and request as LocaleContext
		LocaleContext previousLocaleContext = LocaleContextHolder.getLocaleContext();
		LocaleContext localeContext = buildLocaleContext(request);

        // Expose current RequestAttributes to current thread.
		RequestAttributes previousAttributes = RequestContextHolder.getRequestAttributes();
		ServletRequestAttributes requestAttributes = buildRequestAttributes(request, response, previousAttributes);

		WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);
		asyncManager.registerCallableInterceptor(FrameworkServlet.class.getName(), new RequestBindingInterceptor());

		initContextHolders(request, localeContext, requestAttributes);

		try {
			doService(request, response);
		}
		catch (ServletException ex) {
			failureCause = ex;
			throw ex;
		}
		catch (IOException ex) {
			failureCause = ex;
			throw ex;
		}
		catch (Throwable ex) {
			failureCause = ex;
			throw new NestedServletException("Request processing failed", ex);
		}

		finally {
			resetContextHolders(request, previousLocaleContext, previousAttributes);
			if (requestAttributes != null) {
				requestAttributes.requestCompleted();
			}

			if (logger.isDebugEnabled()) {
				if (failureCause != null) {
					this.logger.debug("Could not complete request", failureCause);
				}
				else {
					if (asyncManager.isConcurrentHandlingStarted()) {
						logger.debug("Leaving response open for concurrent processing");
					}
					else {
						this.logger.debug("Successfully completed request");
					}
				}
			}

			publishRequestHandledEvent(request, response, startTime, failureCause);
		}
	}

执行逻辑如下：

1. 为了保证当前线程的LocaleContext以及RequestAttributes可以在当前请求后还能恢复，提取当前线程的两个属性。
2. 根据当前request创建对应的LocaleContext和RequestAttributes，并绑定到当前线程。
3. 委托给doService方法进一步处理。
4. 请求处理结束后回复线程到原始状态。
5. 请求处理结束后无论成功与否发布事件通知。

    protected void doService(HttpServletRequest request, HttpServletResponse response) throws Exception {
		if (logger.isDebugEnabled()) {
			String resumed = WebAsyncUtils.getAsyncManager(request).hasConcurrentResult() ? " resumed" : "";
			logger.debug("DispatcherServlet with name '" + getServletName() + "'" + resumed +
					" processing " + request.getMethod() + " request for [" + getRequestUri(request) + "]");
		}

		// Keep a snapshot of the request attributes in case of an include,
		// to be able to restore the original attributes after the include.
		// 保存request attributes的快照
        Map<String, Object> attributesSnapshot = null;
		if (WebUtils.isIncludeRequest(request)) {
			attributesSnapshot = new HashMap<String, Object>();
			Enumeration<?> attrNames = request.getAttributeNames();
			while (attrNames.hasMoreElements()) {
				String attrName = (String) attrNames.nextElement();
				if (this.cleanupAfterInclude || attrName.startsWith(DEFAULT_STRATEGIES_PREFIX)) {
					attributesSnapshot.put(attrName, request.getAttribute(attrName));
				}
			}
		}

		// Make framework objects available to handlers and view objects.
		request.setAttribute(WEB_APPLICATION_CONTEXT_ATTRIBUTE, getWebApplicationContext());
		request.setAttribute(LOCALE_RESOLVER_ATTRIBUTE, this.localeResolver);
		request.setAttribute(THEME_RESOLVER_ATTRIBUTE, this.themeResolver);
		request.setAttribute(THEME_SOURCE_ATTRIBUTE, getThemeSource());

		FlashMap inputFlashMap = this.flashMapManager.retrieveAndUpdate(request, response);
		if (inputFlashMap != null) {
			request.setAttribute(INPUT_FLASH_MAP_ATTRIBUTE, Collections.unmodifiableMap(inputFlashMap));
		}
		request.setAttribute(OUTPUT_FLASH_MAP_ATTRIBUTE, new FlashMap());
		request.setAttribute(FLASH_MAP_MANAGER_ATTRIBUTE, this.flashMapManager);

		try {
			doDispatch(request, response);
		}
		finally {
			if (!WebAsyncUtils.getAsyncManager(request).isConcurrentHandlingStarted()) {
				// Restore the original attribute snapshot, in case of an include.
				if (attributesSnapshot != null) {
					restoreAttributesAfterInclude(request, attributesSnapshot);
				}
			}
		}
	}
Spring通过该方法将已经初始化的功能辅助工具变量，比如localeResolver、themeResolver等设置在request属性中，这些属性会在接下来的处理中排上用场。

    protected void doDispatch(HttpServletRequest request, HttpServletResponse response) throws Exception {
		HttpServletRequest processedRequest = request;
		HandlerExecutionChain mappedHandler = null;
		boolean multipartRequestParsed = false;

		WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);

		try {
			ModelAndView mv = null;
			Exception dispatchException = null;

			try {
				// 如果是MultipartContent类型的request则转换request为MultipartHttpServletRequest类型的request
                processedRequest = checkMultipart(request);

                // 根据request信息寻找对应的Handler
				multipartRequestParsed = (processedRequest != request);

				// Determine handler for the current request.
				// 根据request找到对应的Handler
                mappedHandler = getHandler(processedRequest);
				if (mappedHandler == null || mappedHandler.getHandler() == null) {
                    // 如果没有找到对应的handler则通过response反馈错误信息
					noHandlerFound(processedRequest, response);
					return;
				}

				// Determine handler adapter for the current request.
                // 根据当前的handler寻找对应的HandlerAdapter
				HandlerAdapter ha = getHandlerAdapter(mappedHandler.getHandler());

				// Process last-modified header, if supported by the handler.
                // 如果当前handler支持last-modified头处理
				String method = request.getMethod();
				boolean isGet = "GET".equals(method);
				if (isGet || "HEAD".equals(method)) {
					long lastModified = ha.getLastModified(request, mappedHandler.getHandler());
					if (logger.isDebugEnabled()) {
						logger.debug("Last-Modified value for [" + getRequestUri(request) + "] is: " + lastModified);
					}
					if (new ServletWebRequest(request, response).checkNotModified(lastModified) && isGet) {
						return;
					}
				}

                //拦截器的preHandler方法的调用
				if (!mappedHandler.applyPreHandle(processedRequest, response)) {
					return;
				}

				// Actually invoke the handler.
                // 真正的激活handler并返回视图
				mv = ha.handle(processedRequest, response, mappedHandler.getHandler());

				if (asyncManager.isConcurrentHandlingStarted()) {
					return;
				}
                // 视图转换应用于需要添加前缀后缀的情况。
				applyDefaultViewName(processedRequest, mv);
                // 应用所有拦截器的postHandle方法
				mappedHandler.applyPostHandle(processedRequest, response, mv);
			}
			catch (Exception ex) {
				dispatchException = ex;
			}
			catch (Throwable err) {
				// As of 4.3, we're processing Errors thrown from handler methods as well,
				// making them available for @ExceptionHandler methods and other scenarios.
				dispatchException = new NestedServletException("Handler dispatch failed", err);
			}
			processDispatchResult(processedRequest, response, mappedHandler, mv, dispatchException);
		}
		catch (Exception ex) {
			triggerAfterCompletion(processedRequest, response, mappedHandler, ex);
		}
		catch (Throwable err) {
			triggerAfterCompletion(processedRequest, response, mappedHandler,
					new NestedServletException("Handler processing failed", err));
		}
		finally {
			if (asyncManager.isConcurrentHandlingStarted()) {
				// Instead of postHandle and afterCompletion
				if (mappedHandler != null) {
					mappedHandler.applyAfterConcurrentHandlingStarted(processedRequest, response);
				}
			}
			else {
				// Clean up any resources used by a multipart request.
				if (multipartRequestParsed) {
					cleanupMultipart(processedRequest);
				}
			}
		}
	}

### MultipartContent类型的request处理

对于请求的处理，Spring首先考虑的是对于Multipart的处理，如果是MultipartContent类型的request，则转换request为MultiHttpServletRequest类型的request。

    protected HttpServletRequest checkMultipart(HttpServletRequest request) throws MultipartException {
		if (this.multipartResolver != null && this.multipartResolver.isMultipart(request)) {
			if (WebUtils.getNativeRequest(request, MultipartHttpServletRequest.class) != null) {
				logger.debug("Request is already a MultipartHttpServletRequest - if not in a forward, " +
						"this typically results from an additional MultipartFilter in web.xml");
			}
			else if (hasMultipartException(request) ) {
				logger.debug("Multipart resolution failed for current request before - " +
						"skipping re-resolution for undisturbed error rendering");
			}
			else {
				try {
					return this.multipartResolver.resolveMultipart(request);
				}
				catch (MultipartException ex) {
					if (request.getAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE) != null) {
						logger.debug("Multipart resolution failed for error dispatch", ex);
						// Keep processing error dispatch with regular request handle below
					}
					else {
						throw ex;
					}
				}
			}
		}
		// If not returned before: return original request.
		return request;
	}

### 根据request信息寻找对应的Handler

在Spring中最简单的映射处理器配置如下：

    <bean id="simpleUrlMapping" class="org.Springframework.web.servlet.handler.SimpleUrlHandlerMapping">
        <property name="mappings">
            <props>
                <prop key="/userlist.htm">UserController</prop>
            </props>
        </property>
    </bean>

在Spring加载的过程中，Spring会将类型为SimpleUrlHandlerMapping的实例加载到this.handlerMappings中，按照常理推断，根据request提取对那个的Handler，无非就是提取当前实例中的userController，但是userController为继承自AbstractController类型实例，与HandlerExecutionChain并无任何关联，那么这一步是如何实现的呢？

    protected HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {
		for (HandlerMapping hm : this.handlerMappings) {
			if (logger.isTraceEnabled()) {
				logger.trace(
						"Testing handler map [" + hm + "] in DispatcherServlet with name '" + getServletName() + "'");
			}
			HandlerExecutionChain handler = hm.getHandler(request);
			if (handler != null) {
				return handler;
			}
		}
		return null;
	}

在系统启动时Spring会将所有的映射类型的bean注册到this.handlerMappings变量中，所以此函数的目的就是遍历所有的HandlerMapping，并调用其getHandler方法进行封装处理。以SimpleUrlHandlerMapping为例查看器getHandler方法如下：

    public final HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {
        // 根据request获取对应的Handler
		Object handler = getHandlerInternal(request);
		if (handler == null) {
            // 如果没有对应request的handler则使用默认的handler
			handler = getDefaultHandler();
		}
        // 如果也没有提供默认的handler则无法继续处理返回null。
		if (handler == null) {
			return null;
		}
		// Bean name or resolved handler?
		if (handler instanceof String) {
			String handlerName = (String) handler;
			handler = getApplicationContext().getBean(handlerName);
		}

		HandlerExecutionChain executionChain = getHandlerExecutionChain(handler, request);
		if (CorsUtils.isCorsRequest(request)) {
			CorsConfiguration globalConfig = this.corsConfigSource.getCorsConfiguration(request);
			CorsConfiguration handlerConfig = getCorsConfiguration(handler, request);
			CorsConfiguration config = (globalConfig != null ? globalConfig.combine(handlerConfig) : handlerConfig);
			executionChain = getCorsHandlerExecutionChain(request, executionChain, config);
		}
		return executionChain;
	}

函数中首先会使用getHandlerInternal方法根据request获取对应的Handler，如果以SimpleUrlHandlerMapping为例分析，那么我们推断此步骤提供的功能很可能就是根据url找到匹配的Controller并返回，当然如果没有找到对应的Controller处理器那么程序会尝试去查找配置中的默认处理器，当然当查找的controller为String类型时，那意味着返回的是配置的bean名称，需要根据bean名称查找对应的bean，最后还要通过getHandlerExecutionChain方法对返回的Handler进行封装，以保证满足返回类型的匹配。

#### 1. 根据request查找对应的Handler

首先从根据request查找对应的Handler开始分析：

    protected Object getHandlerInternal(HttpServletRequest request) throws Exception {
        // 截取用于匹配的url有效路径
		String lookupPath = getUrlPathHelper().getLookupPathForRequest(request);
        // 根据路径寻找handler
		Object handler = lookupHandler(lookupPath, request);
		if (handler == null) {
			// We need to care for the default handler directly, since we need to
			// expose the PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE for it as well.
			Object rawHandler = null;
			if ("/".equals(lookupPath)) {
                // 如果请求的路径仅仅是"/"，那么使用RootHandler进行处理
				rawHandler = getRootHandler();
			}
			if (rawHandler == null) {
                // 如果无法找到Handler则使用默认handler
				rawHandler = getDefaultHandler();
			}
			if (rawHandler != null) {
                // 根据bean名称获取对应bean
				// Bean name or resolved handler?
				if (rawHandler instanceof String) {
					String handlerName = (String) rawHandler;
					rawHandler = getApplicationContext().getBean(handlerName);
				}
                // 模板方法
				validateHandler(rawHandler, request);
				handler = buildPathExposingHandler(rawHandler, lookupPath, lookupPath, null);
			}
		}
		if (handler != null && logger.isDebugEnabled()) {
			logger.debug("Mapping [" + lookupPath + "] to " + handler);
		}
		else if (handler == null && logger.isTraceEnabled()) {
			logger.trace("No handler mapping found for [" + lookupPath + "]");
		}
		return handler;
	}

    protected Object lookupHandler(String urlPath, HttpServletRequest request) throws Exception {
		// Direct match?
        // 直接匹配的处理
		Object handler = this.handlerMap.get(urlPath);
		if (handler != null) {
			// Bean name or resolved handler?
			if (handler instanceof String) {
				String handlerName = (String) handler;
				handler = getApplicationContext().getBean(handlerName);
			}
			validateHandler(handler, request);
			return buildPathExposingHandler(handler, urlPath, urlPath, null);
		}

		// Pattern match?
        // 通配符匹配的处理
		List<String> matchingPatterns = new ArrayList<String>();
		for (String registeredPattern : this.handlerMap.keySet()) {
			if (getPathMatcher().match(registeredPattern, urlPath)) {
				matchingPatterns.add(registeredPattern);
			}
			else if (useTrailingSlashMatch()) {
				if (!registeredPattern.endsWith("/") && getPathMatcher().match(registeredPattern + "/", urlPath)) {
					matchingPatterns.add(registeredPattern +"/");
				}
			}
		}

		String bestMatch = null;
		Comparator<String> patternComparator = getPathMatcher().getPatternComparator(urlPath);
		if (!matchingPatterns.isEmpty()) {
			Collections.sort(matchingPatterns, patternComparator);
			if (logger.isDebugEnabled()) {
				logger.debug("Matching patterns for request [" + urlPath + "] are " + matchingPatterns);
			}
			bestMatch = matchingPatterns.get(0);
		}
		if (bestMatch != null) {
			handler = this.handlerMap.get(bestMatch);
			if (handler == null) {
				if (bestMatch.endsWith("/")) {
					handler = this.handlerMap.get(bestMatch.substring(0, bestMatch.length() - 1));
				}
				if (handler == null) {
					throw new IllegalStateException(
							"Could not find handler for best pattern match [" + bestMatch + "]");
				}
			}
			// Bean name or resolved handler?
			if (handler instanceof String) {
				String handlerName = (String) handler;
				handler = getApplicationContext().getBean(handlerName);
			}
			validateHandler(handler, request);
			String pathWithinMapping = getPathMatcher().extractPathWithinPattern(bestMatch, urlPath);

			// There might be multiple 'best patterns', let's make sure we have the correct URI template variables
			// for all of them
			Map<String, String> uriTemplateVariables = new LinkedHashMap<String, String>();
			for (String matchingPattern : matchingPatterns) {
				if (patternComparator.compare(bestMatch, matchingPattern) == 0) {
					Map<String, String> vars = getPathMatcher().extractUriTemplateVariables(matchingPattern, urlPath);
					Map<String, String> decodedVars = getUrlPathHelper().decodePathVariables(request, vars);
					uriTemplateVariables.putAll(decodedVars);
				}
			}
			if (logger.isDebugEnabled()) {
				logger.debug("URI Template variables for request [" + urlPath + "] are " + uriTemplateVariables);
			}
			return buildPathExposingHandler(handler, bestMatch, pathWithinMapping, uriTemplateVariables);
		}

		// No handler found...
		return null;
	}

根据URL获取对应Handler的匹配规则代码实现起来虽然很长，但是并不难理解，考虑了直接匹配与通配符两种情况。其中要提及的是buildPathExposingHandler函数，它将Handler封装成了HandlerExecutionChain类型。

    protected Object buildPathExposingHandler(Object rawHandler, String bestMatchingPattern,
			String pathWithinMapping, Map<String, String> uriTemplateVariables) {

		HandlerExecutionChain chain = new HandlerExecutionChain(rawHandler);
		chain.addInterceptor(new PathExposingHandlerInterceptor(bestMatchingPattern, pathWithinMapping));
		if (!CollectionUtils.isEmpty(uriTemplateVariables)) {
			chain.addInterceptor(new UriTemplateVariablesHandlerInterceptor(uriTemplateVariables));
		}
		return chain;
	}

在函数中我们看到了通过将Handler以参数形式传入，并构建HandlerExecutionChain类型实例，加入了两个拦截器。

#### 2. 加入拦截器到执行链

getHandlerExecutionChain函数最主要的目的是将配置中的拦截器加入到执行链中，以保证这些拦截器可以有效地作用于目标对象。

    protected HandlerExecutionChain getHandlerExecutionChain(Object handler, HttpServletRequest request) {
		HandlerExecutionChain chain = (handler instanceof HandlerExecutionChain ?
				(HandlerExecutionChain) handler : new HandlerExecutionChain(handler));

		String lookupPath = this.urlPathHelper.getLookupPathForRequest(request);
		for (HandlerInterceptor interceptor : this.adaptedInterceptors) {
			if (interceptor instanceof MappedInterceptor) {
				MappedInterceptor mappedInterceptor = (MappedInterceptor) interceptor;
				if (mappedInterceptor.matches(lookupPath, this.pathMatcher)) {
					chain.addInterceptor(mappedInterceptor.getInterceptor());
				}
			}
			else {
				chain.addInterceptor(interceptor);
			}
		}
		return chain;
	}

### 没有找到对应的Handler的错误处理

每个请求都对应着一个Handler，因为每个请求都会在后台有相应的逻辑对应，而逻辑的实现就是在Handler中，所以一旦没有遇到找到Handler的情况，开发者可以设计默认的Handler进行处理，如果没有默认的Handler，就只能通过response向用户返回错误信息。

    protected void noHandlerFound(HttpServletRequest request, HttpServletResponse response) throws Exception {
		if (pageNotFoundLogger.isWarnEnabled()) {
			pageNotFoundLogger.warn("No mapping found for HTTP request with URI [" + getRequestUri(request) +
					"] in DispatcherServlet with name '" + getServletName() + "'");
		}
		if (this.throwExceptionIfNoHandlerFound) {
			throw new NoHandlerFoundException(request.getMethod(), getRequestUri(request),
					new ServletServerHttpRequest(request).getHeaders());
		}
		else {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
		}
	}

### 根据当前Handler寻找对应的HandlerAdapter

在WebApplicationContext的初始化过程中我们讨论了HandlerAdapters的初始化，了解了在默认情况下普通的Web请求会交给SimpleControllerHandlerAdapter去处理，下面我们以SimpleControllerHandlerAdapter为例来分析获取适配器的逻辑：

	protected HandlerAdapter getHandlerAdapter(Object handler) throws ServletException {
		for (HandlerAdapter ha : this.handlerAdapters) {
			if (logger.isTraceEnabled()) {
				logger.trace("Testing handler adapter [" + ha + "]");
			}
			if (ha.supports(handler)) {
				return ha;
			}
		}
		throw new ServletException("No adapter for handler [" + handler +
				"]: The DispatcherServlet configuration needs to include a HandlerAdapter that supports this handler");
	}

通过上面的函数我们了解到，对于获取适配器的逻辑无非就是遍历所有适配器来选择合适的适配器并返回它，而某个适配器是否使用于当前的Handler逻辑被封装在具体的适配器中。进一步查看SimpleControllerHandlerAdapter中的supports方法。

    public boolean supports(Object handler) {
		return (handler instanceof Controller);
	}

分析道这里，一切已经明了，SimpleControllerHandlerAdapter就是处于普通的Web请求的，而且对于SpringMVC来说，我们会把逻辑封装至Controller的子类中。

### 缓存处理

在研究Spring对缓存处理的功能支持前，首先要了解Last-Modified缓存机制。

1. 在客户端第一次输入URL时，服务器端会返回内容和状态码200，表示请求成功，表示请求成功，同时会添加一个"Last-Modified:Wed,14 Mar 2012 10:22:42 GMT"表示最后更新时间为(2012-03-14 10:22).
2. 客户端第二次请求此URL时，客户端会向服务器发送请求头"If-Modified-Since"，询问服务器该时间之后当前请求内容是否有被修改过，如果服务器端的内容没有变化，则自动返回HTTP 304状态码。

Spring提供的对Last-Modified机制的支持，只需要实现LastModified接口。

### HandlerInterceptor处理

Servlet API定义的servlet过滤器可以在servlet处理每个Web请求的前后分别对它进行前置处理和后置处理。此外，有些时候，你可能只想处理由某些SpringMVC处理程序处理的Web请求，并在这些处理程序返回的模型属性呗传递到视图之前，对它们进行一些操作。

SpringMVC允许你通过处理拦截Web请求，进行前置处理和后置处理。处理拦截事在Spring的Web应用程序上下文中配置的，因此它们可以利用各种容器特性，并引用容器中声明的任何Bean。处理拦截是针对特殊的处理程序映射进行注册的，因此它只拦截通过这些处理程序映射的请求，每个处理拦截都必须实现HandlerInterceptor接口，它包含三个需要你实现的回调方法：preHandle()、postHandle()和afterCompletion()。第一个和第二个方法分别是在处理程序处理请求之前和之后被调用的。第二个方法还允许访问返回的ModelAndView对象，因此可以在它里面操作模型属性。最后一个方法是在所有请求处理完成之后呗调用的。

### 逻辑处理

对于逻辑处理其实是通过适配器中转调用Handler并返回视图的，对应代码：

    mv = ha.handle(processedRequest, response, mappedHandler.getHandler());

同样，还是以引导示例为基础进行处理逻辑分析，之前分析过，对于普通的Web请求，Spring默认使用SImpleControllerHandlerAdapter类进行处理，我们进入SimpleControllerHandlerAdapter类进行处理，进入SimpleControllerHandlerAdapter类的handle方法如下：

    public ModelAndView handle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {

		return ((Controller) handler).handleRequest(request, response);
	}

### 异常视图处理

有时候系统运行过程中出现异常，而我们并不希望就此中断对客户的服务，而是至少告知客户当前系统在处理逻辑的过程中出现了的异常，甚至告知它们因为什么原因导致的。Spring的异常处理机制会帮我们完成这个工作。其实，这里Spring主要的工作就是将逻辑引导至HandlerExceptionResolver类的resolveException方法。

    protected ModelAndView processHandlerException(HttpServletRequest request, HttpServletResponse response,
			Object handler, Exception ex) throws Exception {

		// Check registered HandlerExceptionResolvers...
		ModelAndView exMv = null;
		for (HandlerExceptionResolver handlerExceptionResolver : this.handlerExceptionResolvers) {
			exMv = handlerExceptionResolver.resolveException(request, response, handler, ex);
			if (exMv != null) {
				break;
			}
		}
		if (exMv != null) {
			if (exMv.isEmpty()) {
				request.setAttribute(EXCEPTION_ATTRIBUTE, ex);
				return null;
			}
			// We might still need view name translation for a plain error model...
			if (!exMv.hasView()) {
				exMv.setViewName(getDefaultViewName(request));
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Handler execution resulted in exception - forwarding to resolved error view: " + exMv, ex);
			}
			WebUtils.exposeErrorRequestAttributes(request, ex, getServletName());
			return exMv;
		}

		throw ex;
	}

### 根据视图跳转页面

无论是一个系统还是一个站点，最重要的工作都是与用户进行交互，用户操作系统后无论下发的命令成功与否都要给用户一个反馈，以便于用户进行下一步的判断。所以，在逻辑处理的最后一定会涉及一个页面跳转的问题。

    protected void render(ModelAndView mv, HttpServletRequest request, HttpServletResponse response) throws Exception {
		// Determine locale for request and apply it to the response.
		Locale locale = this.localeResolver.resolveLocale(request);
		response.setLocale(locale);

		View view;
		if (mv.isReference()) {
			// We need to resolve the view name.
			view = resolveViewName(mv.getViewName(), mv.getModelInternal(), locale, request);
			if (view == null) {
				throw new ServletException("Could not resolve view with name '" + mv.getViewName() +
						"' in servlet with name '" + getServletName() + "'");
			}
		}
		else {
			// No need to lookup: the ModelAndView object contains the actual View object.
			view = mv.getView();
			if (view == null) {
				throw new ServletException("ModelAndView [" + mv + "] neither contains a view name nor a " +
						"View object in servlet with name '" + getServletName() + "'");
			}
		}

		// Delegate to the View object for rendering.
		if (logger.isDebugEnabled()) {
			logger.debug("Rendering view [" + view + "] in DispatcherServlet with name '" + getServletName() + "'");
		}
		try {
			if (mv.getStatus() != null) {
				response.setStatus(mv.getStatus().value());
			}
			view.render(mv.getModelInternal(), request, response);
		}
		catch (Exception ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("Error rendering view [" + view + "] in DispatcherServlet with name '" +
						getServletName() + "'", ex);
			}
			throw ex;
		}
	}

#### 1. 解析视图名称

在上文中我们提到DispathcerServlet会根据ModelAndView选择合适的视图来进行渲染，而这一功能就是在resolveViewName函数中完成的。

    protected View resolveViewName(String viewName, Map<String, Object> model, Locale locale,
			HttpServletRequest request) throws Exception {

		for (ViewResolver viewResolver : this.viewResolvers) {
			View view = viewResolver.resolveViewName(viewName, locale);
			if (view != null) {
				return view;
			}
		}
		return null;
	}

我们以org.Springframework.web.servlet.view.InternalResourceViewResolver为例来分析ViewResolver逻辑的解析过程，其中resolveViewName函数的实现是在其父类AbstractCachingViewResolver中完成的。

    public View resolveViewName(String viewName, Locale locale) throws Exception {
		if (!isCache()) {
            // 不存在缓存的情况下直接创建视图
			return createView(viewName, locale);
		}
		else {
            // 直接从缓存中提取
			Object cacheKey = getCacheKey(viewName, locale);
			View view = this.viewAccessCache.get(cacheKey);
			if (view == null) {
				synchronized (this.viewCreationCache) {
					view = this.viewCreationCache.get(cacheKey);
					if (view == null) {
						// Ask the subclass to create the View object.
						view = createView(viewName, locale);
						if (view == null && this.cacheUnresolved) {
							view = UNRESOLVED_VIEW;
						}
						if (view != null) {
							this.viewAccessCache.put(cacheKey, view);
							this.viewCreationCache.put(cacheKey, view);
							if (logger.isTraceEnabled()) {
								logger.trace("Cached view [" + cacheKey + "]");
							}
						}
					}
				}
			}
			return (view != UNRESOLVED_VIEW ? view : null);
		}
	}

    protected View createView(String viewName, Locale locale) throws Exception {
		// 如果当前解析器不支持当前解析器如viewName为空等情况
        // If this resolver is not supposed to handle the given view,
		// return null to pass on to the next resolver in the chain.
		if (!canHandle(viewName, locale)) {
			return null;
		}
        // 处理前缀是redirect:XX的情况
		// Check for special "redirect:" prefix.
		if (viewName.startsWith(REDIRECT_URL_PREFIX)) {
			String redirectUrl = viewName.substring(REDIRECT_URL_PREFIX.length());
			RedirectView view = new RedirectView(redirectUrl, isRedirectContextRelative(), isRedirectHttp10Compatible());
			view.setHosts(getRedirectHosts());
			return applyLifecycleMethods(viewName, view);
		}
        // 处理前缀是forward:XX的情况
		// Check for special "forward:" prefix.
		if (viewName.startsWith(FORWARD_URL_PREFIX)) {
			String forwardUrl = viewName.substring(FORWARD_URL_PREFIX.length());
			return new InternalResourceView(forwardUrl);
		}
		// Else fall back to superclass implementation: calling loadView.
		return super.createView(viewName, locale);
	}

    protected View createView(String viewName, Locale locale) throws Exception {
		return loadView(viewName, locale);
	}

    protected View loadView(String viewName, Locale locale) throws Exception {
		AbstractUrlBasedView view = buildView(viewName);
		View result = applyLifecycleMethods(viewName, view);
		return (view.checkResource(locale) ? result : null);
	}

    protected AbstractUrlBasedView buildView(String viewName) throws Exception {
		AbstractUrlBasedView view = (AbstractUrlBasedView) BeanUtils.instantiateClass(getViewClass());
        // 添加前缀以及后缀
		view.setUrl(getPrefix() + viewName + getSuffix());

		String contentType = getContentType();
		if (contentType != null) {
            // 设置contentType
			view.setContentType(contentType);
		}

		view.setRequestContextAttribute(getRequestContextAttribute());
		view.setAttributesMap(getAttributesMap());

		Boolean exposePathVariables = getExposePathVariables();
		if (exposePathVariables != null) {
			view.setExposePathVariables(exposePathVariables);
		}
		Boolean exposeContextBeansAsAttributes = getExposeContextBeansAsAttributes();
		if (exposeContextBeansAsAttributes != null) {
			view.setExposeContextBeansAsAttributes(exposeContextBeansAsAttributes);
		}
		String[] exposedContextBeanNames = getExposedContextBeanNames();
		if (exposedContextBeanNames != null) {
			view.setExposedContextBeanNames(exposedContextBeanNames);
		}
		return view;
	}

通过阅读以上代码，我们发现对于InternalResouceViewResolver所提供的解析功能主要考虑到了几个方面的处理：
- 基于效率的考虑，提供了缓存的支持
- 提供了对redirect:XX和forward:XX前缀的支持
- 添加了前缀及后缀，并向View中加入了必须的属性设置。

#### 2. 页面跳转

当通过viewName解析到对应的View后，就可以进一步地处理跳转逻辑了。

    public void render(Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) throws Exception {
		if (logger.isTraceEnabled()) {
			logger.trace("Rendering view with name '" + this.beanName + "' with model " + model +
				" and static attributes " + this.staticAttributes);
		}

		Map<String, Object> mergedModel = createMergedOutputModel(model, request, response);
		prepareResponse(request, response);
		renderMergedOutputModel(mergedModel, getRequestToExpose(request), response);
	}

在引导示例中，我们了解到对于ModelView的使用，可以将一些属性直接放入其中，然后在页面上直接通过JSTL语法或者原始的request获取。这是一个很方便也很神奇的功能，这些解析属性的工作就是在createMergedOutputModel函数中完成的。

    protected Map<String, Object> createMergedOutputModel(Map<String, ?> model, HttpServletRequest request,
			HttpServletResponse response) {

		@SuppressWarnings("unchecked")
		Map<String, Object> pathVars = (this.exposePathVariables ?
				(Map<String, Object>) request.getAttribute(View.PATH_VARIABLES) : null);

		// Consolidate static and dynamic model attributes.
		int size = this.staticAttributes.size();
		size += (model != null ? model.size() : 0);
		size += (pathVars != null ? pathVars.size() : 0);

		Map<String, Object> mergedModel = new LinkedHashMap<String, Object>(size);
		mergedModel.putAll(this.staticAttributes);
		if (pathVars != null) {
			mergedModel.putAll(pathVars);
		}
		if (model != null) {
			mergedModel.putAll(model);
		}

		// Expose RequestContext?
		if (this.requestContextAttribute != null) {
			mergedModel.put(this.requestContextAttribute, createRequestContext(request, response, mergedModel));
		}

		return mergedModel;
	}

    // 处理页面跳转
    protected void renderMergedOutputModel(
			Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception {
        // 将model中的数据以属性的方式设置到request中
		// Expose the model object as request attributes.
		exposeModelAsRequestAttributes(model, request);

		// Expose helpers as request attributes, if any.
		exposeHelpers(request);

		// Determine the path for the request dispatcher.
		String dispatcherPath = prepareForRendering(request, response);

		// Obtain a RequestDispatcher for the target resource (typically a JSP).
		RequestDispatcher rd = getRequestDispatcher(request, dispatcherPath);
		if (rd == null) {
			throw new ServletException("Could not get RequestDispatcher for [" + getUrl() +
					"]: Check that the corresponding file exists within your web application archive!");
		}

		// If already included or response already committed, perform include, else forward.
		if (useInclude(request, response)) {
			response.setContentType(getContentType());
			if (logger.isDebugEnabled()) {
				logger.debug("Including resource [" + getUrl() + "] in InternalResourceView '" + getBeanName() + "'");
			}
			rd.include(request, response);
		}

		else {
			// Note: The forwarded resource is supposed to determine the content type itself.
			if (logger.isDebugEnabled()) {
				logger.debug("Forwarding to resource [" + getUrl() + "] in InternalResourceView '" + getBeanName() + "'");
			}
			rd.forward(request, response);
		}
	}