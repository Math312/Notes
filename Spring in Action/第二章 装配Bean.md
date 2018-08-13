2.1 Spring配置的可选方案

1.Spring容器负责创建应用程序中的bean并调用DI来协调这些对象的关系，但是，作为开发人员，你需要告诉Spring要创建哪些bean并且如何将其装配在一起。

2.Spring的装配机制

(1)在XML中进行显示配置

(2)在Java中进行显示配置

(3)隐式的bean发现机制和自动装配机制

3.尽可能使用自动配置的机制，显示配置越少越好。自动配置优先度高于JavaConfig优先于XML配置

2.2 自动化装配bean

1.Spring从两个角度实现自动化配置：

(1)组件扫描：Spring会自动发现应用上下文中所创建的bean

(2)自动装配：自动满足bean之间的依赖。

2.自动装配Bean是如何实现的

自动装配技术在Spring中是靠注解实现的，其中主要有@Component、@ComponentScan、@Autowired、@Configuration、@Named、@Injected实现。

@Component声明该类是一个组件可以被扫描到，具有属性value用于设置值。

@ComponentScan打开自动扫描服务，无参数表示自动扫描当前包以及其子包的类，basePackage参数获取一个String数组，String值即包名，Spring会按照包名进行扫描装配。basePackageClasses参数接收一个String数组，该String数组用于存储类名或接口名，按照类名和接口名进行装配。

@Awtowired用于指定需要装配的函数或者属性。被该标签标记的函数会自动从Spring上下文容器中寻找符合条件的对象进行装配，如果没有符合条件的会产生异常，但是可以通过将required值设为false进行避免。被改标签标记的属性同样会被装配。

@Configuration用于指定类为配置文件

@Named用于命名组件名称

@Injected 除非一些微小的区别外约等于@AutoWired

考虑如下例子，用代码模拟CD机

    public interface CD
    {
        public String play();   
    }
    @Component
    public class SegPeppers implements CD
    {
        private String title = "123";
        private String player = "456";
        public String play()
        {
            return "Play "+title+" by "+player;
        }
    }
    public interface CDPlayer
    {
        public String play();
    }
    @Component
    public class MediaPlayer implements CDPlayer
    {
        private CD cd;
        @Autowired
        public MediaPlayer(CD cd)
        {
            this.cd = cd;
        }
        public String play()
        {
            return cd.play();
        }
    }
    @Configuration
    @ComponentScan
    public class CDConfig{}
    
以下是上述代码的测试文件：

    @RunWith(SpringJUnit4ClassRunner.class)
    @ContextConfiguration(classes=CDPlayerConfig.class)
    public class CDPlayerTest
    {
        @Autowired
        private CD cd;
        @AutoWired
        private CDPlayer player;
        
        @Test
        public void TestPlay()
        {
            assertEqual(player.play(),"Play 123 by 456");
        }
        
    }
    
@SpringJUnitClassRunner注解保证测试开始时自动创建Spring应用上下文，@ContextConfiguration注解指定要加载的配置类。

组件ID默认为类名首字母小写。

2.3 通过Java代码进行显示配置

1.为何要通过Java进行显示配置？

有些情况下自动配置是无法满足需要的，如果你要引入别人的代码，别人的代码是无法让你去增添注解的，因此就要通过显示配置，当然显示配置有2种，Java代码和XML，首选Java代码进行配置。

2.如何通过Java代码进行显示配置？

Spring通过注解@Configuration、@Bean。

@Configuration注解指出该类是配置类。

@Bean注解指出该方法会返回一个bean

我们同样考虑刚才的例子：

    public interface CD
    {
        public String play();   
    }
    public class SegPeppers implements CD
    {
        private String title = "123";
        private String player = "456";
        public String play()
        {
            return "Play "+title+" by "+player;
        }
    }
    public interface CDPlayer
    {
        public String play();
    }
    public class MediaPlayer implements CDPlayer
    {
        private CD cd;
        public MediaPlayer(CD cd)
        {
            this.cd = cd;
        }
        public String play()
        {
            return cd.play();
        }
    }
    @Configuration
    public class CDConfig
    {
        @Bean
        public CD segPeppers()
        {
            return new SegPeppers();
        }
        
        @Bean
        public CDPlayer getMediaPlayer()
        {
            return new MediaPlayer(segPePPers());
        }
    }
    
我们删除了SegPePPers类和MediaPlayer类中的注解以及ComponentScan注解，并扩充了CDConfig类，这样上述的测试文件仍能运行。

2.4 通过XML文件显示配置

1.为何要采用XML文件进行显示配置？

理由同为何用Java进行显示配置

2.如何用XML文件进行显示配置？

用XML文件进行配置是一件相当繁琐的工作（真的恶心）。
我们先介绍一下配置文件的基本格式，配置文件是一个以beans为主标签的文件，形如：

    <?version="1.0" encoding="UTF-8>
    <beans xmlns="http://www.springframework.org/schema/beans"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.springframwwork.org/schema/beans
    http://www.springframwwork.org/schema/beans/spring-beans.xsd
    http://www.springframework.org/schema/context">
    <!-- Configuration details go here -->
    </beans>
    
你可能会问，这东西不会让我自己敲吧，这不就点XML文件的根命名空间么？

我可以负责任的告诉你，除非IDE帮你，否则你还真得自己敲，开不开心？

言归正传，这样基本的架子已经搭好了，我们往里填写配置，否则上述的配置完全没有内容。

    <beans id="bean的id" class="全限定类名">
    
现在你已经学会如何声明一个简单的bean了，这里有一些特征你务必要了解：

(1) 你不需要直接负责创建SgtPeppers实例，然而在基于JavaConfig的配置中，我们是要这样做的(自己写的return new SgtPeppers();)，当Spring发现这个<bean>元素时，它将会调用SgtPeppers的默认构造器来创建bean。在XML配置中，bean的创建显得更加被动，不过，它没有JavaConfig那么强大，在JavaConfig配置方式中，你可以采用任何方法进行创建bean实例（毕竟创建方法是自己写的，可以根据自己的想法定义）。

(2) 我们将bean的类型以字符创的形式设置在了class属性中。这种方法会导致一些错误，毕竟没人能保证类名的正确性。

了解完这些特性，我们继续进一步学习，我们已经知道，装配方式有很多，其中我们主要提到的就是构造器注入和setter方法注入。那么下面我们来看这两个问题。

2.4.1 通过构造器注入bean

Spring为构造器注入提供了两种方法：

(1)<constructor-arg>元素

(2)c-命名空间

首先我们先介绍第一种<constructor-arg>元素,事实上，这并不是什么难事，配置文件如下：

    <bean id="A" class="B">
        <constructor-arg ref=“C”/>
    </bean>
    
在这里ref传入的是引用值，上述配置文件的意思为，请生成一个类型为B的bean，其ID为A，B的构造器传入参数为ID为C的bean。

当然，有时你传入的参数可能并不是某个已存在于Spring上下文容器中的bean，而只是单纯的字面量。这种时候采用如下方式进行装配：

    <bean id="A" class="B">
        <constructor-arg value=“C”/>
    </bean>
    
这里C值便是要传入的字面量。当然并不是一个bean标签内只能包含一个<constructor-arg>标签，这点你大可放心。

单个数据的部分我们已经介绍完了，那么我们要传入的如果不是单个数据而是数据集合怎么办。事实上Spring给我们提供了如下的方式:

    <bean id="A" class="B">
        <constructor-arg>
            <list>
                <value></value>
                <value></value>
            </list>
        </constructor-arg>
    </bean>
    
value处也可换为ref不过此时就要如下写法：

    <bean id="A" class="B">
        <constructor-arg>
            <list>
                <ref bean=""/>
                <ref bean=""/>
            </list>
        </constructor-arg>
    </bean>

我们也可以按照同样的方式使用set元素，只需把list换为set。

下面我们来介绍c-命名空间。首先要使用C-命名空间时，必须在XML文件的首部声明如下模式：

    <?version="1.0" encoding="UTF-8"?>
    <beans xmlns="http://www.springframework.org/schema/beans"
    xmlns:c="http://www.springframework.org/schema/c"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.springframework.org/schema/beans
    http://www.springframework.org/schema/beans/spring-beans.xsd">
    ...
    </beans>
    
C-命名空间的配置方式有很多种，首先最普通的，配置方式如下：
    
    <bean id="A" class="B"
        c:C-ref="D">
        
上述配置表示，调用B类的构造器创建一个ID为A的bean，其构造器参数名为C，传入到C的引用为ID为D的bean。

第二种方式配置方式如下：

    <bean id="A" class="B"
        c:_0-ref="D">
        
这种方式与上一种表达的意义类似，意义为调用B类的构造器创建一个ID为A的bean，传入到B的构造器的第一个参数是ID为D的bean的引用。

第三种配置方式如下：

    <bean id="A" class="B"
        c:_-ref="D">

这种方式仅用_表示参数，因此不适用于构造器参数多余1的构造器。

2.4.2 设置属性

对于强依赖采用构造器注入，对于可选性依赖通过属性注入。

属性注入的方式也有两种，我们一一介绍。

普通方式形式如下：

    <bean id="cdPlayer" class="soundsystem.CDPlayer">
    <property name="compactDisc" ref="compactDisc" />
    </bean>
    
<property>元素为属性的Setter方法所提供的功能与<constructor-arg>元素为构造器所提供的功能。

第二种方式为p-命名空间方式，XML文件需要声明的模式：
    
    <?xml version="1.0" encoding="UTF-8"?>
    <beans xmlns="http://www.springframework.org/schema/beans"
    xmlns:p="http://www.springframework.org/schma/p"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-nstance"
    xsi:schemaLocation="http://www.springframework/schema/beans
    http://www.springframework.org/schema/beans/spring-beans.xsd">
    ...
    </beans>
    
配置方式也分为3种：

第一种：
    
    <bean id="A" class="B" p:C-ref="D"/>
    
第二种：
    
    <bean id="A" class="B" p:_0-ref="D"/>
    
第三种：

    <bean id="A" class="B" p:_-ref="D"/>
    
2.5 导入和混合配置

1.在JavaConfig中导入XML配置

2.在XML中导入