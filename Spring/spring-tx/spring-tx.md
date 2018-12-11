# spring-tx

## 使用方法

1. 配置数据源
2. 配置事务管理器，默认可以配置DataSourceTransactionManager
3. 在配置类上添加@EnableTransactionManagement注解
4. 在事务类或者事务方法上添加@Transactional注解

## 分析Spring声明式事务所用的例子

mysql创建一个test数据库，其中只有一张User表，表结构如下：

id|name
---|---
Integer|varchar(64)

DAO(Data access object)类如下：

    @Repository
    public class UserDao {

        @Autowired
        private JdbcTemplate jdbcTemplate;
        
        public void insert() {
            String sql = "INSERT INTO user(name) values(?)";
            String username = UUID.randomUUID().toString().substring(0, 5);
            jdbcTemplate.update(sql,username);
        }
        
    }

Service类如下：

    @Service
    @Transactional
    public class UserService {

        @Autowired
        private UserDao userdao;
        
        public void insertUser() {
            userdao.insert();
            System.out.println("userdao.insert()执行完成");
            int i = 10/0;
        }
        
    }

配置类：

    @ComponentScan
    @EnableTransactionManagement
    @Configuration
    public class TxConfig {

        @Bean
        public DataSource dataSource() throws PropertyVetoException {
            ComboPooledDataSource dataSource = new ComboPooledDataSource();
            dataSource.setUser("root");
            dataSource.setPassword("fanyan521");
            dataSource.setDriverClass("com.mysql.jdbc.Driver");
            dataSource.setJdbcUrl("jdbc:mysql://localhost:3306/test");
            return dataSource;
        }
        
        @Bean
        public JdbcTemplate jdbcTemplate(DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }
        
        @Bean
        public PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }
    }

测试类：

    public class Main {

        public static void main(String[] args) {
            AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TxConfig.class);
            UserService userService = context.getBean(UserService.class);
            userService.insertUser();
            context.close();
        }
        
    }

## 原理分析

### @EnableTransactionManagement

@EnableTransactionManagement注解源码如下：

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @Import(TransactionManagementConfigurationSelector.class)
    public @interface EnableTransactionManagement {
        //使用cglib创建代理还是Java动态代理
        boolean proxyTargetClass() default false;
        AdviceMode mode() default AdviceMode.PROXY;
        int order() default Ordered.LOWEST_PRECEDENCE;
    }

`EnableTransactionManagement`注解使用了Import注解向spring-ioc容器中注册了`TransactionManagementConfigurationSelector`接口中标注的类型的bean。

    public class TransactionManagementConfigurationSelector extends AdviceModeImportSelector<EnableTransactionManagement> {

        /**
        * {@inheritDoc}
        * @return {@link ProxyTransactionManagementConfiguration} or
        * {@code AspectJTransactionManagementConfiguration} for {@code PROXY} and
        * {@code ASPECTJ} values of {@link EnableTransactionManagement#mode()}, respectively
        */
        @Override
        protected String[] selectImports(AdviceMode adviceMode) {
            switch (adviceMode) {
                //根据EnableTransactionManagement注解中的mode属性注册bean
                //注意默认情况下是PROXY
                case PROXY:
                    return new String[] {AutoProxyRegistrar.class.getName(), ProxyTransactionManagementConfiguration.class.getName()};
                case ASPECTJ:
                    return new String[] {TransactionManagementConfigUtils.TRANSACTION_ASPECT_CONFIGURATION_CLASS_NAME};
                default:
                    return null;
            }
        }

    }

`TransactionManagementConfigurationSelector`类继承了`AdviceModeImportSelector`，而`AdviceModeImportSelector`实现了`ImportSelector`接口，考察AdviceModeImportSelector源码：

    @Override
	public final String[] selectImports(AnnotationMetadata importingClassMetadata) {
        //获取注解中的属性
        //调用子类的selectImports(AdviceMode adviceMode)方法进行选择创建哪些bean
		Class<?> annType = GenericTypeResolver.resolveTypeArgument(getClass(), AdviceModeImportSelector.class);
		Assert.state(annType != null, "Unresolvable type argument for AdviceModeImportSelector");

		AnnotationAttributes attributes = AnnotationConfigUtils.attributesFor(importingClassMetadata, annType);
		if (attributes == null) {
			throw new IllegalArgumentException(String.format(
				"@%s is not present on importing class '%s' as expected",
				annType.getSimpleName(), importingClassMetadata.getClassName()));
		}

		AdviceMode adviceMode = attributes.getEnum(this.getAdviceModeAttributeName());
		String[] imports = selectImports(adviceMode);
		if (imports == null) {
			throw new IllegalArgumentException(String.format("Unknown AdviceMode: '%s'", adviceMode));
		}
		return imports;
	}

现在我们知道@EnableTransactionManagement注解为我们创建了两个bean，类型分别为：

- AutoProxyRegistrar
- ProxyTransactionManagementConfiguration
  
那么这两个bean到底是做什么呢？我们分别查看其源码。

### AutoProxyRegistrar

    public class AutoProxyRegistrar implements ImportBeanDefinitionRegistrar {

        private final Log logger = LogFactory.getLog(getClass());

        /**
        * Register, escalate, and configure the standard auto proxy creator (APC) against the
        * given registry. Works by finding the nearest annotation declared on the importing
        * {@code @Configuration} class that has both {@code mode} and {@code proxyTargetClass}
        * attributes. If {@code mode} is set to {@code PROXY}, the APC is registered; if
        * {@code proxyTargetClass} is set to {@code true}, then the APC is forced to use
        * subclass (CGLIB) proxying.
        * <p>Several {@code @Enable*} annotations expose both {@code mode} and
        * {@code proxyTargetClass} attributes. It is important to note that most of these
        * capabilities end up sharing a {@linkplain AopConfigUtils#AUTO_PROXY_CREATOR_BEAN_NAME
        * single APC}. For this reason, this implementation doesn't "care" exactly which
        * annotation it finds -- as long as it exposes the right {@code mode} and
        * {@code proxyTargetClass} attributes, the APC can be registered and configured all
        * the same.
        */
        @Override
        public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
            boolean candidateFound = false;
            //获取@EnableTransactionManagement注解的属性
            Set<String> annoTypes = importingClassMetadata.getAnnotationTypes();
            for (String annoType : annoTypes) {
                AnnotationAttributes candidate = AnnotationConfigUtils.attributesFor(importingClassMetadata, annoType);
                if (candidate == null) {
                    continue;
                }
                Object mode = candidate.get("mode");
                Object proxyTargetClass = candidate.get("proxyTargetClass");
                if (mode != null && proxyTargetClass != null && AdviceMode.class == mode.getClass() &&
                        Boolean.class == proxyTargetClass.getClass()) {
                    candidateFound = true;
                    //根据注解属性创建bean
                    if (mode == AdviceMode.PROXY) {
                        //如果是PROXY模式，调用该方法注册bean
                        AopConfigUtils.registerAutoProxyCreatorIfNecessary(registry);
                        //如果proxyTargetClass是true的话，继续调用方法创建bean
                        if ((Boolean) proxyTargetClass) {
                            AopConfigUtils.forceAutoProxyCreatorToUseClassProxying(registry);
                            return;
                        }
                    }
                }
            }
            if (!candidateFound) {
                String name = getClass().getSimpleName();
                logger.warn(String.format("%s was imported but no annotations were found " +
                        "having both 'mode' and 'proxyTargetClass' attributes of type " +
                        "AdviceMode and boolean respectively. This means that auto proxy " +
                        "creator registration and configuration may not have occurred as " +
                        "intended, and components may not be proxied as expected. Check to " +
                        "ensure that %s has been @Import'ed on the same class where these " +
                        "annotations are declared; otherwise remove the import of %s " +
                        "altogether.", name, name, name));
            }
        }

    }

接下来考察AopUtils类的`registerAutoProxyCreatorIfNecessary(BeanDefinitionRegistry registry)`方法和`forceAutoProxyCreatorToUseClassProxying(BeanDefinitionRegistry registry)`方法。

首先查看`registerAutoProxyCreatorIfNecessary(BeanDefinitionRegistry registry)`方法:

    @Nullable
	public static BeanDefinition registerAutoProxyCreatorIfNecessary(BeanDefinitionRegistry registry) {
		return registerAutoProxyCreatorIfNecessary(registry, null);
	}

	@Nullable
	public static BeanDefinition registerAutoProxyCreatorIfNecessary(BeanDefinitionRegistry registry,
			@Nullable Object source) {

		return registerOrEscalateApcAsRequired(InfrastructureAdvisorAutoProxyCreator.class, registry, source);
	}

这里的代码注册了一个`InfrastructureAdvisorAutoProxyCreator`，不管怎样都是一个`通知自动代理创建器`，根据spring-aop的源码分析，我们知道spring-aop的`AnnotationAspectJAutoProxyCreator`也是在这里创建的。我们接下来再讨论`InfrastructureAdvisorAutoProxyCreator`类的细节。


### ProxyTransactionManagementConfiguration

    @Configuration
    public class ProxyTransactionManagementConfiguration extends AbstractTransactionManagementConfiguration {
        
        //创建BeanFactoryTransactionAttributeSourceAdvisor
        //反正是个通知器
        //根据spring-aop的原理我们知道，这个通知器是cglib用来创建代理用的
        @Bean(name = TransactionManagementConfigUtils.TRANSACTION_ADVISOR_BEAN_NAME)
        @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
        public BeanFactoryTransactionAttributeSourceAdvisor transactionAdvisor() {
            BeanFactoryTransactionAttributeSourceAdvisor advisor = new BeanFactoryTransactionAttributeSourceAdvisor();
            //需要注意的是这里需要transactionAttributeSource()方法声明的bean
            advisor.setTransactionAttributeSource(transactionAttributeSource());
            advisor.setAdvice(transactionInterceptor());
            if (this.enableTx != null) {
                advisor.setOrder(this.enableTx.<Integer>getNumber("order"));
            }
            return advisor;
        }

        //声明了一个TransactionAttributeSource类型的bean，
        //TransactionAttributeSource注释如下：
        //{@link TransactionInterceptor}用于元数据检索的策略接口。
        //用来获取事务属性、元数据、配置来自哪里
        @Bean
        @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
        public TransactionAttributeSource transactionAttributeSource() {
            //AnnotationTransactionAttributeSource类的注解如下
            //该类读取Spring的JDK 1.5+ {@link Transactional}注释，
            //并向Spring的事务基础结构公开相应的事务属性。 
            //还支持JTA 1.2的{@link javax.transaction.Transactional}和EJB3的{@link javax.ejb.TransactionAttribute}注释（如果存在）。 
            //此类还可以作为自定义TransactionAttributeSource的基类，
            //或通过{@link TransactionAnnotationParser}策略进行自定义。
            return new AnnotationTransactionAttributeSource();
        }
        //拦截器
        //该拦截器用于帮助实现实现事务功能的方法调用
        @Bean
        @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
        public TransactionInterceptor transactionInterceptor() {
            TransactionInterceptor interceptor = new TransactionInterceptor();
            //这里要传入事务管理器，所以事务管理器不能为空
            interceptor.setTransactionAttributeSource(transactionAttributeSource());
            if (this.txManager != null) {
                interceptor.setTransactionManager(this.txManager);
            }
            return interceptor;
        }

    }

### InfrastructureAdvisorAutoProxyCreator

首先查看其类继承结构：

<xmp>
    InfrastructureAdvisorAutoProxyCreator
      -> AbstractAdvisorAutoProxyCreator
        -> AbstractAutoProxyCreator
          -> ProxyProcessorSupport
          -<> SmartInstantiationAwareBeanPostProcessor
            -> InstantiationAwareBeanPostProcessor
              -> BeanPostProcessor
          -<> BeanFactoryAware
</xmp>

这里我们和Spring-aop中配置的AnnotationAwareAspectJAutoProxyCreator的类结构进行对比

<xmp>
AnnotationAwareAspectJAutoProxyCreator
  ->AspectJAwareAdvisorAutoProxyCreator
   ->AbstractAdvisorAutoProxyCreator    
     ->AbstractAutoProxyCreator
       ->ProxyProcessorSupport
       -<>SmartInstantiationAwareBeanPostProcessor
         ->InstantiationAwareBeanPostProcessor
           ->BeanPostProcessor
       -<>BeanFactoryAware
</xmp>

只有顶层的类结构不同，而底层的类结构都是相同的。因此spring-tx的逻辑与spring-aop其实是类似的。

### 创建InfrastructureAdvisorAutoProxyCreator

由于InfrastructureAdvisorAutoProxyCreator是一个BeanPostProcessor，因此我们要去查看AnnotationConfigApplicationContext类的构造器：

    /**
	 * Create a new AnnotationConfigApplicationContext, deriving bean definitions
	 * from the given annotated classes and automatically refreshing the context.
	 * @param annotatedClasses one or more annotated classes,
	 * e.g. {@link Configuration @Configuration} classes
	 */
	public AnnotationConfigApplicationContext(Class<?>... annotatedClasses) {
		this();
		register(annotatedClasses);
        //这里去初始化各种bean实例
		refresh();
	}

而初始化InstantiationAwareBeanPostProcessor类型的BeanProcessor是在AnnotationConfigApplicationContext类的registerBeanPostProcessors(ConfigurableListableBeanFactory beanFactory)方法中。由于这个创建逻辑已经分析过很多次，我们直接分析创建了InfrastructureAdvisorAutoProxyCreator类型bean之后的过程，这个类型的bean的id也是`org.springframework.aop.config.internalAutoProxyCreator`，这个id和AnnotationAwareAspectJAutoProxyCreator类型的bean id是相同的，那么，两者是如何同时存在的呢？AopConfigUtils给出了一个方法：

    private static BeanDefinition registerOrEscalateApcAsRequired(Class<?> cls, BeanDefinitionRegistry registry,
			@Nullable Object source) {

		Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
        //如果bean定义中已经包含了一个id为
        //org.springframework.aop.config.internalAutoProxyCreator
        //的bean
        //选取优先级高的进行实现。
		if (registry.containsBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME)) {
			BeanDefinition apcDefinition = registry.getBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME);
			if (!cls.getName().equals(apcDefinition.getBeanClassName())) {
				int currentPriority = findPriorityForClass(apcDefinition.getBeanClassName());
				int requiredPriority = findPriorityForClass(cls);
				if (currentPriority < requiredPriority) {
					apcDefinition.setBeanClassName(cls.getName());
				}
			}
			return null;
		}

		RootBeanDefinition beanDefinition = new RootBeanDefinition(cls);
		beanDefinition.setSource(source);
		beanDefinition.getPropertyValues().add("order", Ordered.HIGHEST_PRECEDENCE);
		beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		registry.registerBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME, beanDefinition);
		return beanDefinition;
	}

这里选择优先级高的进行实现，仔细查看两者源码，发现AnnotationAwareAspectJAutoProxyCreator是可以胜任InfrastructureAdvisorAutoProxyCreator工作的。考察InfrastructureAdvisorAutoProxyCreator源码：

    public class InfrastructureAdvisorAutoProxyCreator extends AbstractAdvisorAutoProxyCreator {

        @Nullable
        private ConfigurableListableBeanFactory beanFactory;


        @Override
        protected void initBeanFactory(ConfigurableListableBeanFactory beanFactory) {
            super.initBeanFactory(beanFactory);
            this.beanFactory = beanFactory;
        }

        @Override
        protected boolean isEligibleAdvisorBean(String beanName) {
            return (this.beanFactory != null && this.beanFactory.containsBeanDefinition(beanName) &&
                    this.beanFactory.getBeanDefinition(beanName).getRole() == BeanDefinition.ROLE_INFRASTRUCTURE);
        }

    }

我们知道与创建PostProcessor有关的只有这个initBeanFactory(ConfigurableListableBeanFactory beanFactory)方法，而这个方法的实现看着就比较敷衍，我们查看其父类的setBeanFactory()方法：

    @Override
	public void setBeanFactory(BeanFactory beanFactory) {
		super.setBeanFactory(beanFactory);
		if (!(beanFactory instanceof ConfigurableListableBeanFactory)) {
			throw new IllegalArgumentException(
					"AdvisorAutoProxyCreator requires a ConfigurableListableBeanFactory: " + beanFactory);
		}
		initBeanFactory((ConfigurableListableBeanFactory) beanFactory);
	}

	protected void initBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		this.advisorRetrievalHelper = new BeanFactoryAdvisorRetrievalHelperAdapter(beanFactory);
	}

也就意味着两者的不同只有以下这段代码：

    if (!(beanFactory instanceof ConfigurableListableBeanFactory)) {
			throw new IllegalArgumentException(
					"AdvisorAutoProxyCreator requires a ConfigurableListableBeanFactory: " + beanFactory);
		}

区别前者传入的beanFactory类型可以不是ConfigurableListableBeanFactory类型，而后者的传入类型必须是，除此之外没有区别。除了这一检测外，我们知道AnnotationAwareAspectJAutoProxyCreator初始化了三个属性，而这里只初始化了一个属性：advisorRetrievalHelper，主要是剩下的那两个属性都是AnnotationAwareAspectJAutoProxyCreator自己单独实现的，只提供给它自身使用，这样也就不会有啥影响了。

### 创建需要代理的类UserService

这里我们只描述InfrastructureAdvisorAutoProxyCreator处理的过程，UserService的初始化过程满足如下顺序：

1. 调用invokeAwareMethods()方法用来调用各种XXXAware接口中设置的方法。
2. 调用applyBeanPostProcessorsBeforeInitialization()方法用来调用各个已经在容器中的BeanPostProcessor中的postProcessBeforeInitialization()方法。
3. 调用invokeInitMethods()方法对创建好的bean进行初始化。
4. 调用applyBeanPostProcessorsAfterInitialization()方法用来调用各个已经在容器中的BeanPostProcessor中的postProcessAfterInitialization()方法。

由于这里与InfrastructureAdvisorAutoProxyCreator有关的只有第2步和第4步，我们依次查看，首先查看第2步：

    @Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) {
		return bean;
	}

可以发现，这里没有具体的处理逻辑，所以这部可以忽视，下面我们查看第四部：

    @Override
	public Object postProcessAfterInitialization(@Nullable Object bean, String beanName) throws BeansException {
		if (bean != null) {
			Object cacheKey = getCacheKey(bean.getClass(), beanName);
			if (!this.earlyProxyReferences.contains(cacheKey)) {
				return wrapIfNecessary(bean, beanName, cacheKey);
			}
		}
		return bean;
	}

这里和spring-aop逻辑相同，就是为指定bean添加通知器创建代理，具体逻辑在wrapIfNecessary()方法中。由于这个方法是AbstractAutoProxyCreator类型的，所以和AnnotationAwareAspectJAutoProxyCreator是一样的，查看方法：

    protected Object wrapIfNecessary(Object bean, String beanName, Object cacheKey) {
        //判断beanName是否是空的并且是否这个beanName的bean已经被处理过，如果被处理过，那么就返回这个bean
		if (StringUtils.hasLength(beanName) && this.targetSourcedBeans.contains(beanName)) {
			return bean;
		}
        //判断这个bean是否不应该被处理
		if (Boolean.FALSE.equals(this.advisedBeans.get(cacheKey))) {
			return bean;
		}
        //判断这个bean是否符合被处理的条件
		if (isInfrastructureClass(bean.getClass()) || shouldSkip(bean.getClass(), beanName)) {
			this.advisedBeans.put(cacheKey, Boolean.FALSE);
			return bean;
		}

		// Create proxy if we have advice.
        //获取所有可用的Advisor
		Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(bean.getClass(), beanName, null);
		if (specificInterceptors != DO_NOT_PROXY) {
			this.advisedBeans.put(cacheKey, Boolean.TRUE);
			//创建代理
            Object proxy = createProxy(
					bean.getClass(), beanName, specificInterceptors, new SingletonTargetSource(bean));
			this.proxyTypes.put(cacheKey, proxy.getClass());
			//返回代理
            return proxy;
		}

		this.advisedBeans.put(cacheKey, Boolean.FALSE);
		return bean;
	}

注意这里有两个函数比较重要，分别是：

    Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(bean.getClass(), beanName, null);

和

    Object proxy = createProxy(
					bean.getClass(), beanName, specificInterceptors, new SingletonTargetSource(bean));

首先查看第一个函数，这个函数是用来选择出所有可用的Advisor：

    Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(bean.getClass(), beanName, null);

仔细查看类结构中的每个方法我们发现该方法是由AbstractAdvisorAutoProxyCreator类型实现的，它的默认实现为：

    protected Object[] getAdvicesAndAdvisorsForBean(
			Class<?> beanClass, String beanName, @Nullable TargetSource targetSource) {
        
		List<Advisor> advisors = findEligibleAdvisors(beanClass, beanName);
		if (advisors.isEmpty()) {
			return DO_NOT_PROXY;
		}
		return advisors.toArray();
	}
而且这个方法在AnnotationAwareAspectJAutoProxyCreator中和InfrastructureAdvisorAutoProxyCreator中也并没有重写。查看其逻辑：

- 其实就是调用了findEligibleAdvisors(Class<T> clazz,String beanName)方法，这个方法也是用来选择出所有可用的Advisor。

继续考察这个方法，这个方法也是在AbstractAdvisorAutoProxyCreator类中，它的源码如下：

    protected List<Advisor> findEligibleAdvisors(Class<?> beanClass, String beanName) {
		List<Advisor> candidateAdvisors = findCandidateAdvisors();
		List<Advisor> eligibleAdvisors = findAdvisorsThatCanApply(candidateAdvisors, beanClass, beanName);
		extendAdvisors(eligibleAdvisors);
		if (!eligibleAdvisors.isEmpty()) {
			eligibleAdvisors = sortAdvisors(eligibleAdvisors);
		}
		return eligibleAdvisors;
	}

这个逻辑也比较简单：

- 选择出所有候选的Advisor存入candidateAdvisors集合中
- 通过beanClass、beanName来从candidateAdvisors集合筛选出适合这个bean的Advisor并存入eligibleAdvisors
- 通过extendAdvisors()方法做一些后置处理，默认为空
- 通过sortAdvisors()方法对所有选出的Advisor及进行排序
- 返回结果

下面首先分析findEligibleAdvisors()方法，这个方法是由AbstractAdvisorAutoProxyCreator类进行实现的，源码如下：

    protected List<Advisor> findCandidateAdvisors() {
		Assert.state(this.advisorRetrievalHelper != null, "No BeanFactoryAdvisorRetrievalHelper available");
		return this.advisorRetrievalHelper.findAdvisorBeans();
	}

我们看到它将查找逻辑委托给了advisorRetrievalHelper属性进行，而advisorRetrievalHelper属性是我们创建BeanPostProcessor时调用initBeanFactory()进行初始化的，代码如下：

    protected void initBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		this.advisorRetrievalHelper = new BeanFactoryAdvisorRetrievalHelperAdapter(beanFactory);
	}

它是一个BeanFactoryAdvisorRetrievalHelperAdapter类型的对象，而这个类是BeanFactoryAdvisorRetrievalHelper的子类，代码如下，这是AbstractAdvisorAutoProxyCreator一个内部类，可以看出这个类的主要目的就是使BeanFactoryAdvisorRetrievalHelper类型的对象调用isEligibleBean()方法时调用AbstractAdvisorAutoProxyCreator的方法，所以这就是一个适配器，只不过实现方式比较特殊，是采用内部类可以看到其外部类的方法的特点实现的。这样做的目的就是可以通过让AbstractAdvisorAutoProxyCreator的子类覆盖isEligibleBean(String beanName)方法来实现对该方法的定制化。

    private class BeanFactoryAdvisorRetrievalHelperAdapter extends BeanFactoryAdvisorRetrievalHelper {

		public BeanFactoryAdvisorRetrievalHelperAdapter(ConfigurableListableBeanFactory beanFactory) {
			super(beanFactory);
		}

		@Override
		protected boolean isEligibleBean(String beanName) {
			return AbstractAdvisorAutoProxyCreator.this.isEligibleAdvisorBean(beanName);
		}
	}

继续查看findAdvisorBeans()方法，他就是

    return this.advisorRetrievalHelper.findAdvisorBeans();

它的实现类是BeanFactoryAdvisorRetrievalHelper类型，源码如下：

    public List<Advisor> findAdvisorBeans() {
		// Determine list of advisor bean names, if not cached already.
		String[] advisorNames = null;
		synchronized (this) {
			advisorNames = this.cachedAdvisorBeanNames;
			if (advisorNames == null) {
				// Do not initialize FactoryBeans here: We need to leave all regular beans
				// uninitialized to let the auto-proxy creator apply to them!
				advisorNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
						this.beanFactory, Advisor.class, true, false);
				this.cachedAdvisorBeanNames = advisorNames;
			}
		}
		if (advisorNames.length == 0) {
			return new LinkedList<>();
		}

		List<Advisor> advisors = new LinkedList<>();
		for (String name : advisorNames) {
			if (isEligibleBean(name)) {
				if (this.beanFactory.isCurrentlyInCreation(name)) {
					if (logger.isDebugEnabled()) {
						logger.debug("Skipping currently created advisor '" + name + "'");
					}
				}
				else {
					try {
						advisors.add(this.beanFactory.getBean(name, Advisor.class));
					}
					catch (BeanCreationException ex) {
						Throwable rootCause = ex.getMostSpecificCause();
						if (rootCause instanceof BeanCurrentlyInCreationException) {
							BeanCreationException bce = (BeanCreationException) rootCause;
							String bceBeanName = bce.getBeanName();
							if (bceBeanName != null && this.beanFactory.isCurrentlyInCreation(bceBeanName)) {
								if (logger.isDebugEnabled()) {
									logger.debug("Skipping advisor '" + name +
											"' with dependency on currently created bean: " + ex.getMessage());
								}
								// Ignore: indicates a reference back to the bean we're trying to advise.
								// We want to find advisors other than the currently created bean itself.
								continue;
							}
						}
						throw ex;
					}
				}
			}
		}
		return advisors;
	}

该类中存储了一个数组用于缓存所有Advisor的名字，先从缓存中获取Advisor，然后调用isEligibleBean()依次判断是否是可用的，如果是则继续，如果不是则从beanFactory里查找Advisor并添加进来，最后返回一个Advisor列表。到这里findCandidateAdvisors()函数就结束了。接下来是findAdvisorsThatCanApply()函数，被AbstractAdvisorAutoProxyCreator实现，源码如下：

    protected List<Advisor> findAdvisorsThatCanApply(
			List<Advisor> candidateAdvisors, Class<?> beanClass, String beanName) {

		ProxyCreationContext.setCurrentProxiedBeanName(beanName);
		try {
            //核心
			return AopUtils.findAdvisorsThatCanApply(candidateAdvisors, beanClass);
		}
		finally {
			ProxyCreationContext.setCurrentProxiedBeanName(null);
		}
	}
它的核心是

    return AopUtils.findAdvisorsThatCanApply(candidateAdvisors, beanClass);

我们考察这个函数，实现类是AopUtils：

    public static List<Advisor> findAdvisorsThatCanApply(List<Advisor> candidateAdvisors, Class<?> clazz) {
		if (candidateAdvisors.isEmpty()) {
			return candidateAdvisors;
		}
		List<Advisor> eligibleAdvisors = new LinkedList<>();
		for (Advisor candidate : candidateAdvisors) {
			if (candidate instanceof IntroductionAdvisor && canApply(candidate, clazz)) {
				eligibleAdvisors.add(candidate);
			}
		}
		boolean hasIntroductions = !eligibleAdvisors.isEmpty();
		for (Advisor candidate : candidateAdvisors) {
			if (candidate instanceof IntroductionAdvisor) {
				// already processed
				continue;
			}
			if (canApply(candidate, clazz, hasIntroductions)) {
				eligibleAdvisors.add(candidate);
			}
		}
		return eligibleAdvisors;
	}

函数逻辑也很简单：

- 如果传入的列表为空列表，则返回原列表
- 否则Advisor中是IntroductionAdvisor并且满足canApply()方法的加入到结果列表中
- 剩下的不是IntroductionAdvisor的Advisor中，满足canApply()方法的加入到结果列表中
- 返回结果列表

那么也就只用查看canApply()方法了：

    public static boolean canApply(Advisor advisor, Class<?> targetClass, boolean hasIntroductions) {
		if (advisor instanceof IntroductionAdvisor) {
			return ((IntroductionAdvisor) advisor).getClassFilter().matches(targetClass);
		}
		else if (advisor instanceof PointcutAdvisor) {
			PointcutAdvisor pca = (PointcutAdvisor) advisor;
			return canApply(pca.getPointcut(), targetClass, hasIntroductions);
		}
		else {
			// It doesn't have a pointcut so we assume it applies.
			return true;
		}
	}

函数逻辑如下：

- 如果是IntroductionAdvisor类型，那么有一个getClassFilter()方法用于筛选类的，返回该方法返回值即可
- 如果是PointcutAdvisor类型，那么直接返回canApply(Pointcut,Class<?>,boolean)的结果
- 两者都不是，返回true。

继续分析canApply(Pointcut,Class<?>,boolean)方法，同样是AopUtils实现的:

    public static boolean canApply(Pointcut pc, Class<?> targetClass, boolean hasIntroductions) {
		Assert.notNull(pc, "Pointcut must not be null");
		if (!pc.getClassFilter().matches(targetClass)) {
			return false;
		}

		MethodMatcher methodMatcher = pc.getMethodMatcher();
		if (methodMatcher == MethodMatcher.TRUE) {
			// No need to iterate the methods if we're matching any method anyway...
			return true;
		}

		IntroductionAwareMethodMatcher introductionAwareMethodMatcher = null;
		if (methodMatcher instanceof IntroductionAwareMethodMatcher) {
			introductionAwareMethodMatcher = (IntroductionAwareMethodMatcher) methodMatcher;
		}

		Set<Class<?>> classes = new LinkedHashSet<>();
		if (!Proxy.isProxyClass(targetClass)) {
			classes.add(ClassUtils.getUserClass(targetClass));
		}
		classes.addAll(ClassUtils.getAllInterfacesForClassAsSet(targetClass));

		for (Class<?> clazz : classes) {
			Method[] methods = ReflectionUtils.getAllDeclaredMethods(clazz);
			for (Method method : methods) {
				if (introductionAwareMethodMatcher != null ?
						introductionAwareMethodMatcher.matches(method, targetClass, hasIntroductions) :
						methodMatcher.matches(method, targetClass)) {
					return true;
				}
			}
		}

		return false;
	}

实现逻辑：

- 切点参数为pc
- 先使用pc.getClassFilter()进行匹配，匹配失败的返回false;
- 然后获取pc.getMethodMatcher()进行匹配，如果实现了IntroductionAwareMethodMatcher强转成IntroductionAwareMethodMatcher进行匹配，否则就作为普通的MethodMatcher()进行匹配，匹配通过一次即成功

至于后面的MehtodMatcher进行匹配的过程就是spring-tx自己实现的一个匹配器，具体的逻辑就不研究了，有兴趣可以自己看下。

到这里findAdvisorsThatCanApply()函数也就完成了，剩下两个意义不大，我们就不再详述了。

那么剩下就是createProxy()由于这部分已经在spring-aop中讲解了，这里就略过。

### 调用方法（BeanFactoryTransactionAttributeSourceAdvisor）

当我们执行Main类中的main()方法时：

    public class Main {

        public static void main(String[] args) {
            AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TxConfig.class);
            UserService userService = context.getBean(UserService.class);
            userService.insertUser();
            context.close();
        }
        
    }

当执行到userService.insertUser()方法时，会调用CglibAopProxy$DynamicAdvisedInterceptor的intercept()方法，这个方法我们在spring-aop中讲得十分详细，这里就略过，我们直接看调用TransactionInterceptor的部分：

    public Object invoke(final MethodInvocation invocation) throws Throwable {
		// Work out the target class: may be {@code null}.
		// The TransactionAttributeSource should be passed the target class
		// as well as the method, which may be from an interface.
		Class<?> targetClass = (invocation.getThis() != null ? AopUtils.getTargetClass(invocation.getThis()) : null);

		// Adapt to TransactionAspectSupport's invokeWithinTransaction...
		return invokeWithinTransaction(invocation.getMethod(), targetClass, invocation::proceed);
	}

这里，invoke()方法将逻辑委托给invokeWithinTransaction()方法来处理，我们查看该方法：

    protected Object invokeWithinTransaction(Method method, @Nullable Class<?> targetClass,
			final InvocationCallback invocation) throws Throwable {

		// If the transaction attribute is null, the method is non-transactional.
		TransactionAttributeSource tas = getTransactionAttributeSource();
		final TransactionAttribute txAttr = (tas != null ? tas.getTransactionAttribute(method, targetClass) : null);
		final PlatformTransactionManager tm = determineTransactionManager(txAttr);
		final String joinpointIdentification = methodIdentification(method, targetClass, txAttr);

		if (txAttr == null || !(tm instanceof CallbackPreferringPlatformTransactionManager)) {
			// Standard transaction demarcation with getTransaction and commit/rollback calls.
			TransactionInfo txInfo = createTransactionIfNecessary(tm, txAttr, joinpointIdentification);
			Object retVal = null;
			try {
				// This is an around advice: Invoke the next interceptor in the chain.
				// This will normally result in a target object being invoked.
				retVal = invocation.proceedWithInvocation();
			}
			catch (Throwable ex) {
				// target invocation exception
				completeTransactionAfterThrowing(txInfo, ex);
				throw ex;
			}
			finally {
				cleanupTransactionInfo(txInfo);
			}
			commitTransactionAfterReturning(txInfo);
			return retVal;
		}

		else {
			final ThrowableHolder throwableHolder = new ThrowableHolder();

			// It's a CallbackPreferringPlatformTransactionManager: pass a TransactionCallback in.
			try {
				Object result = ((CallbackPreferringPlatformTransactionManager) tm).execute(txAttr, status -> {
					TransactionInfo txInfo = prepareTransactionInfo(tm, txAttr, joinpointIdentification, status);
					try {
						return invocation.proceedWithInvocation();
					}
					catch (Throwable ex) {
						if (txAttr.rollbackOn(ex)) {
							// A RuntimeException: will lead to a rollback.
							if (ex instanceof RuntimeException) {
								throw (RuntimeException) ex;
							}
							else {
								throw new ThrowableHolderException(ex);
							}
						}
						else {
							// A normal return value: will lead to a commit.
							throwableHolder.throwable = ex;
							return null;
						}
					}
					finally {
						cleanupTransactionInfo(txInfo);
					}
				});

				// Check result state: It might indicate a Throwable to rethrow.
				if (throwableHolder.throwable != null) {
					throw throwableHolder.throwable;
				}
				return result;
			}
			catch (ThrowableHolderException ex) {
				throw ex.getCause();
			}
			catch (TransactionSystemException ex2) {
				if (throwableHolder.throwable != null) {
					logger.error("Application exception overridden by commit exception", throwableHolder.throwable);
					ex2.initApplicationException(throwableHolder.throwable);
				}
				throw ex2;
			}
			catch (Throwable ex2) {
				if (throwableHolder.throwable != null) {
					logger.error("Application exception overridden by commit exception", throwableHolder.throwable);
				}
				throw ex2;
			}
		}
	}

方法很长，我们一部分一部分分析，首先：

    TransactionAttributeSource tas = getTransactionAttributeSource();
	final TransactionAttribute txAttr = (tas != null ? tas.getTransactionAttribute(method, targetClass) : null);
	final PlatformTransactionManager tm = determineTransactionManager(txAttr);
	final String joinpointIdentification = methodIdentification(method, targetClass, txAttr);

这里是初始化数据，拿到TransactionAttributeSource、TransactionAttribute、PlatformTransactionManager和joinpointIdentification。接下来是一个判断：

    if (txAttr == null || !(tm instanceof CallbackPreferringPlatformTransactionManager)) 

判断TransactionAttribute是否为空和PlatformTransactionManager 的类型信息，默认情况下当然是满足txAttr不为空并且事务管理器不是CallbackPreferringPlatformTransactionManager类型的，进入下面的操作：

        TransactionInfo txInfo = createTransactionIfNecessary(tm, txAttr, joinpointIdentification);
		Object retVal = null;
		try {
			// This is an around advice: Invoke the next interceptor in the chain.
			// This will normally result in a target object being invoked.
			retVal = invocation.proceedWithInvocation();
		}
		catch (Throwable ex) {
			// target invocation exception
			completeTransactionAfterThrowing(txInfo, ex);
			throw ex;
		}
		finally {
			cleanupTransactionInfo(txInfo);
		}
		commitTransactionAfterReturning(txInfo);
		return retVal;

我们发现所谓的事务就是，先通过事务管理器创建事务：

    TransactionInfo txInfo = createTransactionIfNecessary(tm, txAttr, joinpointIdentification);

然后调用我们自己的事务逻辑：

    retVal = invocation.proceedWithInvocation();

如果调用过程中出现了异常，就执行回滚操作：

    completeTransactionAfterThrowing(txInfo, ex);

否则执行commit操作：

    commitTransactionAfterReturning(txInfo);

当然每次都要清理缓存：

    cleanupTransactionInfo(txInfo);

至于剩下的一个分支，由于我也没用过，等到遇到再进行补充。