# Spring-IOC

## 例子

基于下面的例子分析spring-ioc容器创建过程：

配置类：

    @Configuration
    public class TestConfiguration {
        
        public TestConfiguration() {
            System.out.println("TestConfiguration is creating");
        }
        
        @Bean
        public Car car() {
            return new Car("Big Car",10);
        }	
    }
测试类：

    public class Main {

        public static void main(String[] args) {
            ApplicationContext context = new AnnotationConfigApplicationContext(TestConfiguration.class);
            Car car = (Car) context.getBean("car");
            String[] names = context.getBeanDefinitionNames();
            System.out.println(context.getDisplayName());
            System.out.println(car);
            System.out.println(context.containsBean("car"));
            for(String str:names) {
                System.out.println(str);
            }
        }
    }

测试Bean:

    public class Car {

        private String name;
        
        private int age;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public Car() {
            super();
        }

        public Car(String name, int age) {
            super();
            this.name = name;
            this.age = age;
        }

        @Override
        public String toString() {
            return "Car [name=" + name + ", age=" + age + "]";
        }
        
    }

## spring-ioc容器创建过程分析

查看Main类中的main方法：

    public static void main(String[] args) {
            ApplicationContext context = new AnnotationConfigApplicationContext(TestConfiguration.class);
            Car car = (Car) context.getBean("car");
            String[] names = context.getBeanDefinitionNames();
            System.out.println(context.getDisplayName());
            System.out.println(car);
            System.out.println(context.containsBean("car"));
            for(String str:names) {
                System.out.println(str);
            }
        }

其中第一行代码：

    ApplicationContext context = new AnnotationConfigApplicationContext(TestConfiguration.class);

调用构造器，创建AnnotationConfigApplicationContext对象：

    public AnnotationConfigApplicationContext(Class<?>... annotatedClasses) {
		this();
		register(annotatedClasses);
		refresh();
	}

### this()

### register()

### refresh()

refresh()方法包含如下代码：

    public void refresh() throws BeansException, IllegalStateException {
		synchronized (this.startupShutdownMonitor) {
			// Prepare this context for refreshing.
			prepareRefresh();

			// Tell the subclass to refresh the internal bean factory.
			ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory();

			// Prepare the bean factory for use in this context.
			prepareBeanFactory(beanFactory);

			try {
				// Allows post-processing of the bean factory in context subclasses.
				postProcessBeanFactory(beanFactory);

				// Invoke factory processors registered as beans in the context.
				invokeBeanFactoryPostProcessors(beanFactory);

				// Register bean processors that intercept bean creation.
				registerBeanPostProcessors(beanFactory);

				// Initialize message source for this context.
				initMessageSource();

				// Initialize event multicaster for this context.
				initApplicationEventMulticaster();

				// Initialize other special beans in specific context subclasses.
				onRefresh();

				// Check for listener beans and register them.
				registerListeners();

				// Instantiate all remaining (non-lazy-init) singletons.
				finishBeanFactoryInitialization(beanFactory);

				// Last step: publish corresponding event.
				finishRefresh();
			}

			catch (BeansException ex) {
				if (logger.isWarnEnabled()) {
					logger.warn("Exception encountered during context initialization - " +
							"cancelling refresh attempt: " + ex);
				}

				// Destroy already created singletons to avoid dangling resources.
				destroyBeans();

				// Reset 'active' flag.
				cancelRefresh(ex);

				// Propagate exception to caller.
				throw ex;
			}

			finally {
				// Reset common introspection caches in Spring's core, since we
				// might not ever need metadata for singleton beans anymore...
				resetCommonCaches();
			}
		}
	}

#### prepareRefresh()

方法注释：

    /**
	 * Prepare this context for refreshing, setting its startup date and
	 * active flag as well as performing any initialization of property sources.
	 */
     //为刷新容器做准备，设置容器启动事件，以及启动标志并且初始化配置文件中的属性

方法源码以及逻辑：

    protected void prepareRefresh() {
        //设置启动时间
		this.startupDate = System.currentTimeMillis();
		//设置active标志
        this.closed.set(false);
		this.active.set(true);

		if (logger.isInfoEnabled()) {
			logger.info("Refreshing " + this);
		}

		// Initialize any placeholder property sources in the context environment
        //初始化配置文件中的配置
        //这是一个空方法，用于子类初始化配置
		initPropertySources();

		// Validate that all properties marked as required are resolvable
		// see ConfigurablePropertyResolver#setRequiredProperties
		//取出环境变量并且验证必要的属性
        getEnvironment().validateRequiredProperties();

		// Allow for the collection of early ApplicationEvents,
		// to be published once the multicaster is available...
		//最后初始化应用程序事件序列，用于存储刷新容器期间发布的信息
        this.earlyApplicationEvents = new LinkedHashSet<>();
	}

这里由于initPropertySources()方法是空方法，我们仔细分析下

    getEnvironment().validateRequiredProperties();

getEnvironment()方法源码十分简单，如下：

    public ConfigurableEnvironment getEnvironment() {
		if (this.environment == null) {
			this.environment = createEnvironment();
		}
		return this.environment;
	}

逻辑就是如果this.environment是空就创建一个，否则就直接返回对象。

考察validateRequiredProperties()逻辑：

    @Override
	public void validateRequiredProperties() throws MissingRequiredPropertiesException {
		this.propertyResolver.validateRequiredProperties();
	}

这里调用了this.propertyResolver.validateRequiredProperties()。不过值得注意的是如果验证不通过会抛出MissingRequiredPropertiesException异常。考虑this.propertyResolver.validateRequiredProperties()的验证逻辑：

    public void validateRequiredProperties() {
		MissingRequiredPropertiesException ex = new MissingRequiredPropertiesException();
		for (String key : this.requiredProperties) {
			if (this.getProperty(key) == null) {
				ex.addMissingRequiredProperty(key);
			}
		}
		if (!ex.getMissingRequiredProperties().isEmpty()) {
			throw ex;
		}
	}

依次遍历this.requiredProperties，如果获取不到属性就将其添加到MissingRequiredPropertiesException 中，遍历完成后，如果MissingRequiredPropertiesException 包含缺少的属性，就抛出异常。但是这个验证逻辑比较简单，可以对其进行重写。

执行流程如图：

![](images/prepareRefresh.png)

#### ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory();

方法注释如下：

    /**
	 * Tell the subclass to refresh the internal bean factory.
	 * @return the fresh BeanFactory instance
	 * @see #refreshBeanFactory()
	 * @see #getBeanFactory()
	 */
     //告诉子类去刷新内部bean工厂

源码如下：

    protected ConfigurableListableBeanFactory obtainFreshBeanFactory() {
        //刷新bean工厂
		refreshBeanFactory();
        //重新获取bean工厂
		ConfigurableListableBeanFactory beanFactory = getBeanFactory();
		if (logger.isDebugEnabled()) {
			logger.debug("Bean factory for " + getDisplayName() + ": " + beanFactory);
		}
        //返回bean工厂
		return beanFactory;
	}

考察refreshBeanFactory()方法，注解如下：

    /**
	 * Do nothing: We hold a single internal BeanFactory and rely on callers
	 * to register beans through our public methods (or the BeanFactory's).
	 * @see #registerBeanDefinition
	 */
     //什么都不做，我们只有一个内部的BeanFactory单例，并且依赖调用者通过我们的public方法进行注册

源码如下：

    protected final void refreshBeanFactory() throws IllegalStateException {
		if (!this.refreshed.compareAndSet(false, true)) {
			throw new IllegalStateException(
					"GenericApplicationContext does not support multiple refresh attempts: just call 'refresh' once");
		}
		this.beanFactory.setSerializationId(getId());
	}

这个方法并没有做什么事情，只是刷新了下beanFactory而已

#### prepareBeanFactory(beanFactory);

方法注释：

    /**
	 * Configure the factory's standard context characteristics,
	 * such as the context's ClassLoader and post-processors.
	 * @param beanFactory the BeanFactory to configure
	 */
     //配置工厂的标准上下文信息，例如例如上下文的类加载器和post-processor

源码如下：

    protected void prepareBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		// Tell the internal bean factory to use the context's class loader etc.
        //设置类加载器
		beanFactory.setBeanClassLoader(getClassLoader());
		//设置SpEL解析器
        beanFactory.setBeanExpressionResolver(new StandardBeanExpressionResolver(beanFactory.getBeanClassLoader()));
		//添加属性编辑器
        beanFactory.addPropertyEditorRegistrar(new ResourceEditorRegistrar(this, getEnvironment()));

		// Configure the bean factory with context callbacks.
		//添加ApplicationContextAwareProcessor
        beanFactory.addBeanPostProcessor(new ApplicationContextAwareProcessor(this));
        //忽略一系列东西的自动装配
		beanFactory.ignoreDependencyInterface(EnvironmentAware.class);
		beanFactory.ignoreDependencyInterface(EmbeddedValueResolverAware.class);
		beanFactory.ignoreDependencyInterface(ResourceLoaderAware.class);
		beanFactory.ignoreDependencyInterface(ApplicationEventPublisherAware.class);
		beanFactory.ignoreDependencyInterface(MessageSourceAware.class);
		beanFactory.ignoreDependencyInterface(ApplicationContextAware.class);

		// BeanFactory interface not registered as resolvable type in a plain factory.
		// MessageSource registered (and found for autowiring) as a bean.
        //注册依赖
		beanFactory.registerResolvableDependency(BeanFactory.class, beanFactory);
		beanFactory.registerResolvableDependency(ResourceLoader.class, this);
		beanFactory.registerResolvableDependency(ApplicationEventPublisher.class, this);
		beanFactory.registerResolvableDependency(ApplicationContext.class, this);

		// Register early post-processor for detecting inner beans as ApplicationListeners.
        //添加ListenerDetector
		beanFactory.addBeanPostProcessor(new ApplicationListenerDetector(this));

		// Detect a LoadTimeWeaver and prepare for weaving, if found.
        //注册几个基本的bean
		if (beanFactory.containsBean(LOAD_TIME_WEAVER_BEAN_NAME)) {
			beanFactory.addBeanPostProcessor(new LoadTimeWeaverAwareProcessor(beanFactory));
			// Set a temporary ClassLoader for type matching.
			beanFactory.setTempClassLoader(new ContextTypeMatchClassLoader(beanFactory.getBeanClassLoader()));
		}

		// Register default environment beans.
		if (!beanFactory.containsLocalBean(ENVIRONMENT_BEAN_NAME)) {
			beanFactory.registerSingleton(ENVIRONMENT_BEAN_NAME, getEnvironment());
		}
		if (!beanFactory.containsLocalBean(SYSTEM_PROPERTIES_BEAN_NAME)) {
			beanFactory.registerSingleton(SYSTEM_PROPERTIES_BEAN_NAME, getEnvironment().getSystemProperties());
		}
		if (!beanFactory.containsLocalBean(SYSTEM_ENVIRONMENT_BEAN_NAME)) {
			beanFactory.registerSingleton(SYSTEM_ENVIRONMENT_BEAN_NAME, getEnvironment().getSystemEnvironment());
		}
	}

#### invokeBeanFactoryPostProcessors()

方法注释如下：

    /**
	 * Instantiate and invoke all registered BeanFactoryPostProcessor beans,
	 * respecting explicit order if given.
	 * <p>Must be called before singleton instantiation.
	 */
     遵循特定顺序实例化并执行所有的被注册的BeanFactoryPostProcessor，一定要在单例实例化之前调用

方法源码如下：

    protected void invokeBeanFactoryPostProcessors(ConfigurableListableBeanFactory beanFactory) {
        //这里执行实例化并执行所有BeanFactoryPostProcessor的实际逻辑
		PostProcessorRegistrationDelegate.invokeBeanFactoryPostProcessors(beanFactory, getBeanFactoryPostProcessors());

		// Detect a LoadTimeWeaver and prepare for weaving, if found in the meantime
        //检测LoadTimeWeaver并准备编织（如果在此期间找到）
        //通过一个被@Bean修饰的ConfigurationClassPostProcessor
		// (e.g. through an @Bean method registered by ConfigurationClassPostProcessor)
		if (beanFactory.getTempClassLoader() == null && beanFactory.containsBean(LOAD_TIME_WEAVER_BEAN_NAME)) {
			beanFactory.addBeanPostProcessor(new LoadTimeWeaverAwareProcessor(beanFactory));
			beanFactory.setTempClassLoader(new ContextTypeMatchClassLoader(beanFactory.getBeanClassLoader()));
		}
	}

我们仔细查看执行所有BeanFactoryPostProcessor的主逻辑，即：

    PostProcessorRegistrationDelegate.invokeBeanFactoryPostProcessors(beanFactory, getBeanFactoryPostProcessors());

它将执行逻辑交给PostProcessorRegistrationDelegate类的invokeBeanFactoryPostProcessors()方法，查看该方法源码：

    public static void invokeBeanFactoryPostProcessors(
			ConfigurableListableBeanFactory beanFactory, List<BeanFactoryPostProcessor> beanFactoryPostProcessors) {

		// Invoke BeanDefinitionRegistryPostProcessors first, if any.
        //用于记录已经处理过的bean
		Set<String> processedBeans = new HashSet<>();
        //这里是处理已经存在于beanFactoryPostProcessors属性中的BeanFactoryPostProcessor实例，由于传入的参数是由getBeanFactoryPostProcessors()可能有些BeanFactoryPostProcessors还未初始化，因此这里是拿不到的
		if (beanFactory instanceof BeanDefinitionRegistry) {
			BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
			List<BeanFactoryPostProcessor> regularPostProcessors = new LinkedList<>();
			List<BeanDefinitionRegistryPostProcessor> registryProcessors = new LinkedList<>();

			for (BeanFactoryPostProcessor postProcessor : beanFactoryPostProcessors) {
				if (postProcessor instanceof BeanDefinitionRegistryPostProcessor) {
					BeanDefinitionRegistryPostProcessor registryProcessor =
							(BeanDefinitionRegistryPostProcessor) postProcessor;
					registryProcessor.postProcessBeanDefinitionRegistry(registry);
					registryProcessors.add(registryProcessor);
				}
				else {
					regularPostProcessors.add(postProcessor);
				}
			}

			// Do not initialize FactoryBeans here: We need to leave all regular beans
			// uninitialized to let the bean factory post-processors apply to them!
			// Separate between BeanDefinitionRegistryPostProcessors that implement
			// PriorityOrdered, Ordered, and the rest.
			//接下来是按照实现了PriorityOrdered, Ordered, and the rest的顺序进行实例化并且调用对应接口方法的工作，对于下面的BeanFactoryPostProcessor也一样

			List<BeanDefinitionRegistryPostProcessor> currentRegistryProcessors = new ArrayList<>();

			// First, invoke the BeanDefinitionRegistryPostProcessors that implement PriorityOrdered.
			//通过对beanFactory的beanDefinitionNames中的元素进行遍历，获取每个BeanDefinition，然后通过BeanDefinition与输入属性进行对照，
			//这里三个输入的变量分别代表
			// - Bean Class类型
			// - 是否可以不是单例
			// - 是否allowEagerInit
			//将满足条件的beanDefinition的名称全部返回
			//注意，对于被@Configuration注解修饰的类，其解析工作就是在这里通过
			//org.springframework.context.annotation.internalConfigurationAnnotationProcessor
			//完成的
			String[] postProcessorNames =
					beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
			for (String ppName : postProcessorNames) {
				if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					processedBeans.add(ppName);
				}
			}
			sortPostProcessors(currentRegistryProcessors, beanFactory);
			registryProcessors.addAll(currentRegistryProcessors);
			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
			currentRegistryProcessors.clear();

			// Next, invoke the BeanDefinitionRegistryPostProcessors that implement Ordered.
			postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
			for (String ppName : postProcessorNames) {
				if (!processedBeans.contains(ppName) && beanFactory.isTypeMatch(ppName, Ordered.class)) {
					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					processedBeans.add(ppName);
				}
			}
			sortPostProcessors(currentRegistryProcessors, beanFactory);
			registryProcessors.addAll(currentRegistryProcessors);
			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
			currentRegistryProcessors.clear();

			// Finally, invoke all other BeanDefinitionRegistryPostProcessors until no further ones appear.
			boolean reiterate = true;
			while (reiterate) {
				reiterate = false;
				postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
				for (String ppName : postProcessorNames) {
					if (!processedBeans.contains(ppName)) {
						currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
						processedBeans.add(ppName);
						reiterate = true;
					}
				}
				sortPostProcessors(currentRegistryProcessors, beanFactory);
				registryProcessors.addAll(currentRegistryProcessors);
				invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
				currentRegistryProcessors.clear();
			}

			// Now, invoke the postProcessBeanFactory callback of all processors handled so far.
			//这里是按顺序调用所有的BeanDefinitionRegistry的postProcessBeanFactory()方法
			invokeBeanFactoryPostProcessors(registryProcessors, beanFactory);
			//这里就是调用原本已经存在于beanFactoryPostProcessors属性中的BeanFactoryPostProcessor实例的postProcessBeanFactory()方法
			invokeBeanFactoryPostProcessors(regularPostProcessors, beanFactory);
		}

		else {
			// Invoke factory processors registered with the context instance.
			invokeBeanFactoryPostProcessors(beanFactoryPostProcessors, beanFactory);
		}

		// Do not initialize FactoryBeans here: We need to leave all regular beans
		// uninitialized to let the bean factory post-processors apply to them!
		String[] postProcessorNames =
				beanFactory.getBeanNamesForType(BeanFactoryPostProcessor.class, true, false);

		// Separate between BeanFactoryPostProcessors that implement PriorityOrdered,
		// Ordered, and the rest.
		List<BeanFactoryPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
		List<String> orderedPostProcessorNames = new ArrayList<>();
		List<String> nonOrderedPostProcessorNames = new ArrayList<>();
		for (String ppName : postProcessorNames) {
			if (processedBeans.contains(ppName)) {
				// skip - already processed in first phase above
			}
			else if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
				priorityOrderedPostProcessors.add(beanFactory.getBean(ppName, BeanFactoryPostProcessor.class));
			}
			else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
				orderedPostProcessorNames.add(ppName);
			}
			else {
				nonOrderedPostProcessorNames.add(ppName);
			}
		}

		// First, invoke the BeanFactoryPostProcessors that implement PriorityOrdered.
		sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
		invokeBeanFactoryPostProcessors(priorityOrderedPostProcessors, beanFactory);

		// Next, invoke the BeanFactoryPostProcessors that implement Ordered.
		List<BeanFactoryPostProcessor> orderedPostProcessors = new ArrayList<>();
		for (String postProcessorName : orderedPostProcessorNames) {
			orderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
		sortPostProcessors(orderedPostProcessors, beanFactory);
		invokeBeanFactoryPostProcessors(orderedPostProcessors, beanFactory);

		// Finally, invoke all other BeanFactoryPostProcessors.
		List<BeanFactoryPostProcessor> nonOrderedPostProcessors = new ArrayList<>();
		for (String postProcessorName : nonOrderedPostProcessorNames) {
			nonOrderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
		invokeBeanFactoryPostProcessors(nonOrderedPostProcessors, beanFactory);

		// Clear cached merged bean definitions since the post-processors might have
		// modified the original metadata, e.g. replacing placeholders in values...
		beanFactory.clearMetadataCache();
	}

### initMessageSource()

这里不太熟悉。

方法注释如下：

	/**
	 * Initialize the MessageSource.
	 * Use parent's if none defined in this context.
	 */
	 //初始化一个MessageSource
	 //如果上下文中没定义的话就用父类的

源码如下：

	protected void initMessageSource() {
		//获取BeanFactory
		ConfigurableListableBeanFactory beanFactory = getBeanFactory();
		//判断是否有id为messageSource的bean
		if (beanFactory.containsLocalBean(MESSAGE_SOURCE_BEAN_NAME)) {
			this.messageSource = beanFactory.getBean(MESSAGE_SOURCE_BEAN_NAME, MessageSource.class);
			// Make MessageSource aware of parent MessageSource.
			if (this.parent != null && this.messageSource instanceof HierarchicalMessageSource) {
				HierarchicalMessageSource hms = (HierarchicalMessageSource) this.messageSource;
				if (hms.getParentMessageSource() == null) {
					// Only set parent context as parent MessageSource if no parent MessageSource
					// registered already.
					hms.setParentMessageSource(getInternalParentMessageSource());
				}
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Using MessageSource [" + this.messageSource + "]");
			}
		}
		else {
			// Use empty MessageSource to be able to accept getMessage calls.
			// 创建新的MessageSouce，并设置ParentMessageSource
			DelegatingMessageSource dms = new DelegatingMessageSource();
			dms.setParentMessageSource(getInternalParentMessageSource());
			this.messageSource = dms;
			beanFactory.registerSingleton(MESSAGE_SOURCE_BEAN_NAME, this.messageSource);
			if (logger.isDebugEnabled()) {
				logger.debug("Unable to locate MessageSource with name '" + MESSAGE_SOURCE_BEAN_NAME +
						"': using default [" + this.messageSource + "]");
			}
		}
	}

### initApplicationEventMulticaster()

方法注释如下：

	/**
	 * Initialize the ApplicationEventMulticaster.
	 * Uses SimpleApplicationEventMulticaster if none defined in the context.
	 * @see org.springframework.context.event.SimpleApplicationEventMulticaster
	 */
	 //这个方法用来初始化一个ApplicationEventMulticaster
	 //如果上下文中没有的话就创建一个SimpleApplicationEventMulticaster
	 //ApplicationEventMulticaster是用来完成Sprin
	 g Context上下文Event监听的具体请查看[](The Usage Of Spring-IOC.md)

源码如下：

	protected void initApplicationEventMulticaster() {
		//获取BeanFactory
		ConfigurableListableBeanFactory beanFactory = getBeanFactory();
		//如果beanFactory包含applicationEventMulticaster名字的bean就获取
		if (beanFactory.containsLocalBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME)) {
			this.applicationEventMulticaster =
					beanFactory.getBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME, ApplicationEventMulticaster.class);
			if (logger.isDebugEnabled()) {
				logger.debug("Using ApplicationEventMulticaster [" + this.applicationEventMulticaster + "]");
			}
		}
		else {
			//否则创建一个SimpleApplicationEventMulticaster对象并赋值
			this.applicationEventMulticaster = new SimpleApplicationEventMulticaster(beanFactory);
			beanFactory.registerSingleton(APPLICATION_EVENT_MULTICASTER_BEAN_NAME, this.applicationEventMulticaster);
			if (logger.isDebugEnabled()) {
				logger.debug("Unable to locate ApplicationEventMulticaster with name '" +
						APPLICATION_EVENT_MULTICASTER_BEAN_NAME +
						"': using default [" + this.applicationEventMulticaster + "]");
			}
		}
	}

### onRefresh()

方法注释如下：

	/**
	 * Template method which can be overridden to add context-specific refresh work.
	 * Called on initialization of special beans, before instantiation of singletons.
	 * <p>This implementation is empty.
	 * @throws BeansException in case of errors
	 * @see #refresh()
	 */
	 //这是一个模板方法，可以用来完成特定的上下文刷新工作
	 //它用于实例化特定的bean，会在单例bean实例化之前调用，默认为空

源码如下：

	protected void onRefresh() throws BeansException {
		// For subclasses: do nothing by default.
	}

### registerListeners()

方法注释如下：

	/**
	 * Add beans that implement ApplicationListener as listeners.
	 * Doesn't affect other listeners, which can be added without being beans.
	 */
	 //添加实现了ApplicationListener接口的类作为监听器bean
	 //如果不是bean将不能被添加，监听器之间不会相互影响

源码如下：

	protected void registerListeners() {
		//注册指定监听器，这些监听器是在applicationListeners属性中指定的，将其添加到ApplicationEventMulticaster中
		// Register statically specified listeners first.
		for (ApplicationListener<?> listener : getApplicationListeners()) {
			getApplicationEventMulticaster().addApplicationListener(listener);
		}

		// Do not initialize FactoryBeans here: We need to leave all regular beans
		// uninitialized to let post-processors apply to them!
		//获取beanDefinition中存在的bean，然后将其添加到ApplicationEventMulticaster中
		String[] listenerBeanNames = getBeanNamesForType(ApplicationListener.class, true, false);
		for (String listenerBeanName : listenerBeanNames) {
			getApplicationEventMulticaster().addApplicationListenerBean(listenerBeanName);
		}

		// Publish early application events now that we finally have a multicaster...
		// 第一步中设置的earlyApplicationEvents的处理
		Set<ApplicationEvent> earlyEventsToProcess = this.earlyApplicationEvents;
		this.earlyApplicationEvents = null;
		if (earlyEventsToProcess != null) {
			for (ApplicationEvent earlyEvent : earlyEventsToProcess) {
				getApplicationEventMulticaster().multicastEvent(earlyEvent);
			}
		}
	}

### finishBeanFactoryInitialization()

方法注释：

	/**
	 * Finish the initialization of this context's bean factory,
	 * initializing all remaining singleton beans.
	 */
	 //通过上下文中的bean工厂完成所有剩下的单例的实例化

方法源码：

	protected void finishBeanFactoryInitialization(ConfigurableListableBeanFactory beanFactory) {
		// Initialize conversion service for this context.
		if (beanFactory.containsBean(CONVERSION_SERVICE_BEAN_NAME) &&
				beanFactory.isTypeMatch(CONVERSION_SERVICE_BEAN_NAME, ConversionService.class)) {
			beanFactory.setConversionService(
					beanFactory.getBean(CONVERSION_SERVICE_BEAN_NAME, ConversionService.class));
		}

		// Register a default embedded value resolver if no bean post-processor
		// (such as a PropertyPlaceholderConfigurer bean) registered any before:
		// at this point, primarily for resolution in annotation attribute values.
		if (!beanFactory.hasEmbeddedValueResolver()) {
			beanFactory.addEmbeddedValueResolver(strVal -> getEnvironment().resolvePlaceholders(strVal));
		}

		// Initialize LoadTimeWeaverAware beans early to allow for registering their transformers early.
		String[] weaverAwareNames = beanFactory.getBeanNamesForType(LoadTimeWeaverAware.class, false, false);
		for (String weaverAwareName : weaverAwareNames) {
			getBean(weaverAwareName);
		}

		// Stop using the temporary ClassLoader for type matching.
		beanFactory.setTempClassLoader(null);

		// Allow for caching all bean definition metadata, not expecting further changes.
		//冻结配置，避免配置发生变化
		beanFactory.freezeConfiguration();

		// Instantiate all remaining (non-lazy-init) singletons.
		//真正的实例化剩下bean的逻辑
		beanFactory.preInstantiateSingletons();
	}
### finishRefresh()

方法注释：

	/**
	 * Finish the refresh of this context, invoking the LifecycleProcessor's
	 * onRefresh() method and publishing the
	 * {@link org.springframework.context.event.ContextRefreshedEvent}.
	 */
	 //完成容器刷新，执行LifecycleProcessor的onRefresh()方法，并且发布ContextRefreshedEvent事件

方法源码：

	protected void finishRefresh() {
		// Clear context-level resource caches (such as ASM metadata from scanning).
		clearResourceCaches();

		// Initialize lifecycle processor for this context.
		initLifecycleProcessor();

		// Propagate refresh to lifecycle processor first.
		getLifecycleProcessor().onRefresh();

		// Publish the final event.
		publishEvent(new ContextRefreshedEvent(this));

		// Participate in LiveBeansView MBean, if active.
		LiveBeansView.registerApplicationContext(this);
	}