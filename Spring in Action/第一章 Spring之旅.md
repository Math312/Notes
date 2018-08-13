

## 1.1 简化Java开发

1.为了降低Java的开发复杂性，Spring采取的四种关键策略

（1）基于POJO的轻量级和最小侵入性编程

（2）通过依赖注入和面向接口实现松耦合

（3）基于切面和惯例进行声明式编程

（4）通过切面和模板减少样式代码。

1.1.1 激发POJO的潜能

1.Spring不会强迫你实现Spring规范的接口或继承Spring规范的类，在基于Spring的构建的应用中，它的类通常没有任何痕迹表明你使用了Spring。最坏的场景是使用Spring注解但是它仍是一个POJO。

2.Spring的非侵入式编程模型意味着这个类在Spring应用或非Spring应用中都可以发挥同样的作用。

1.1.2 依赖注入（DI）

1,耦合具有两面性，一方面，紧密耦合的代码难以测试，难以复用，难以理解；另一方面，一定程度的耦合有事必须的，没有耦合的代码什么都做不不了。

2,通过DI，对象的依赖关系将由系统中负责协调各对象的第三方组件在创建时进行设定，对象无须自行创建和管理他们的依赖关系。

3.注入方式：构造器注入，Setter方法注入

4.创建应用组件之间协作的行为成为装配。

5.那么什么是依赖注入，怎么实现依赖注入呢，

按我的理解来说，我们日常所写的代码通常都由Main函数等这类函数来进行依赖之间的装配,或者靠类中的硬编码进行装配。我们来看如下例子。

例子1：

    public class People
    {
        PrintStream stream;
        public People()
        {
            stream = System.out;
        }
        
        public void sayHello()
        {
            stream.println("Hello");
        }
        
        public static void main(String [] args)
        {
            People mySelf = new People();
            mySelf.sayHello();
        }
    }
    
上述代码中，stream属性是被硬编码进类中，这种方法会导致修改相当困难。

例子2：

    public class People
    {
        PrintStream stream;
        public People(PrintStream printStream)
        {
            stream = printStream;
        }
        
        public void sayHello()
        {
            stream.println("Hello");
        }
        
        public static void main(String [] args)
        {
            People mySelf = new People(System.out);
            mySelf.sayHello();
        }
    }
    
这样做就导致依赖关系稍微变弱了些，但是仍然需要依靠Main函数等进行装配。

现在我们看使用Spring进行装配的方式：

    <?xml version="1.0" encoding="UTF-8">
    <beans xmlns="http://www.springframework.org/schema/beans"
    xmlns:msi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="Http://www.springframework.org/schema/beans
        http://www.springframework.org/schema/beans/spring-beans.xsd"
    >
    <bean id="people" class="People">
        <constructor-args value="#{T(System).out}">
    </bean>
    </beans>
    
当然配置方式不仅仅只能用xml文件，同样可以用Java代码，配置文件如下：

    @Configuration
    public class PeopleConfig
    {
        @Bean
        public People getPeople()
        {
            return new People(System.out);
        }
    }

比较上述3种代码你会发现，Spring的配置方式将会导致一种有趣的现象，People对象调用sayHello()时，完全不知道到底是采用哪种PrintStream进行的，只是知道调用了该函数，达到了降低耦合的目的。

1.1.3 应用切面(AOP)

1.何为面向切面编程？如何实现呢？

我们来考虑这样一种情况，每天下课老师都会留作业让学生去做，学生做完老师就要查作业。那么这种情况下存在一种类似交互似的东西。学生做作业前调用老师留作业的方法，学生做作业后调用老师查作业的方法，那么怎么做呢？我们按照传统的方法实现一下：

    public class Teacher
    {
        private PrintStream stream;
        
        public Teacher(PrintStream stream)
        {
            this.stream = stream;
        }
        
        public void giveAssignments()
        {
            stream.println("The assignments are coming!!");
        }
        
        public void checkAssignments()
        {
            stream.println("Everything is OK!!");
        }
    }
    public class Student
    {
        private PrintStream stream;
        private Teacher teacher;
        
        
        public Student(PrintStream stream,Teacher teacher)
        {
            this.stream = stream;
            this.teacher = teacher;
        }
        
        public void doHomeWork()
        {
            teacher.giveAssignments();
            stream.println("Doing the homework!!");
            teacher.checkAssignments();
        }
    }
    
我们的实现方法中，每个学生都有一个老师类的属性，这样看起来总觉得有些问题，Spring解决了这个问题。不过首先我们还是先把Student类改正常了。
    
    public class Student
    {
        private PrintStream stream;
        
        public Student(PrintStream stream)
        {
            this.stream = stream;
        }
        
        public void doHomeWork()
        {
            stream.println("Doing the homework!!");
        }
    }
    
现在我们引入面向切面编程的思想，事实上，这种老师和同学之间的关系，其实相当于一种管理模式，就像你编程过程中，你的日志、安全模块一样，我们处理完一个操作可能需要调用日志记录，但是按照日常的方式，就像上述一样，每个类可能都要包含日志模块、安全模块这类的对象，这样并不是一种良好的设计，而且也使代码的耦合度增加，那么有没有一种方法让一个别的东西帮我管理这些呢，这就是面向切面编程。那么这种方法是怎么处理的呢？事实上，你可以把你的程序看成一个盒子，盒子里放置的就是类似于Teacher和Student这样的类，而那些需要各种地方都有参与的类即日志、安全模块就是盒子的面，盒子中的操作交给盒子面去完成，这也就是面向切面编程。

可能文字的方式有些繁琐，那么我们采用代码的方式进行表达。同样是上述的例子：

    <?xml version="1.0" encoding="UTF-8">
    <beans xmlns="http://www.springframework.org/schema/beans"
    xmlns:msi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="Http://www.springframework.org/schema/aop
        http://www.springframework.org/schema/aop/spring-aop-3.2.xsd
        http://www.springframework.org/schema/beans
        http://www.springframework.org/schema/beans/spring-beans.xsd"
    >
    <bean id="teacher" class="Teacher">
        <constructor-args value="#{T(System).out}">
    </bean>
    <bean id="student" class="Student">
        <constructor-args value="#{T(System).out}">
    </bean>
    <bean id="teach" class="Teach">
        <constructor-args value="#{T(System).out}">
    </bean>
    <aop:config>
        <aop:aspect ref="teach">
            <aop:pointcut id="doHomework"
            expression="execution(* *.embarkOnQuest(..))"/>
            <aop:before pointcut-ref="doHomework"
            method="giveAssignments"/>
            <aop:before pointcut-ref="doHomework"
            method="checkAssignments"/>
            
        </aop:respect>
    </aop:config>
    </beans>
    
其中teach被声明为一个切面，同时他也是一个POJO。但务必要注意的是尽管teach是一个切面，但我们仍要将其声明为一个bean。

1.1.4 使用模板消除板式代码

Spring运用自己的API库，使一些繁琐的操作变得简单，例如JDBC中的操作。

1.2 容纳你的Bean

1.在基于Spring的应用中，你的应用对象生存于Spring容器中，并且由Spring负责创建、装配、管理生命周期。

2.Spring自带了多个容器实现：bean工厂和上下文工厂。

1.2.1 使用上下文

1.Spring自带了多种上下文，这里只列举几个最容易遇到的：

- AnnotationConfigApplicationContext:从一个或多个基于Java的配置类中加载Spring上下文
- AnnotationConfigWebApplicationContext：从一个或多个基于Java的配置类中加载Spring Web应用上下文
- ClassPathXmlApplicationContext：从类路径下的一个或多个XML配置文件中加载上下文定义，把应用上下文的定义文件作为类资源。
- FileSystemXmlapplicationContext：从文件系统下的一个或多个XML配置文件中加载上下文定义
- XmlWebApplicationContext:从web应用下的一个或多个XML配置文件中加载上下文定义

2.加载方法：

现在我们仅简单地使用FileSystemXmlapplicationContext和ClassPathXmlApplicationContext，以及AnnotationConfigApplicationContext。

加载一个FileSystemApplicationContext：

    ApplicationContext context= newFileSystemApplicationContext("c:/teacher.xml");

加载一个ClassPathXmlApplicationContext：

    ApplicationContext context = new ClassPathXmlApplicationContext("teacher.xml");
    
从Java配置中加载应用上下文，使用AnnotationConfigApplicationContext：
    
    ApplicationContext context =  new ApplicationConfigApplicationContext(TeacherConfig.class);
    
应用上下文准备就绪后，就可以调用上下文的getBean()方法从Spring容器中获取bean。

1.2.2 bean的生命周期

[SpringBean的生命周期图](http://note.youdao.com/noteshare?id=5560c5db4a16238194cba7478f412918&sub=B593DE697523481A96EF1B579EF5D08D)

1. Spring对bean进行实例化
2. Spring将值和Bean的引用注入到bean对应属性中
3. 如果bean实现了BeanNameAware接口，Spring将Bean的ID传给setBeanName()方法
4. 如果bean实现了BeanFactoryAware接口，Spring将调用setBeanFactory()方法，将BeanFactory容器实例传入。
5. 如果bean实现了ApplicationContextAware接口，Spring将调用setApplicationContext()方法，将在bean所在的应用上下文的引用传入进来。
6. 如果bean实现了BeanPostProcessor接口，Spring将调用他们的postProcessBeforeInitialization()方法。
7. 如果bean实现了InitializingBean接口，Spring将调用它们的afterPropertiesSet()方法。类似的，如果bean使用init-method声明了初始化方法，该方法也会被调用。
8. 如果bean实现了BeanPostProcessor接口，Spring将调用它们的postProcessAfterInitialization()方法.
9. 此时，bean已经准备就绪，可以被应用程序使用了，它们一直主流在应用上下文中，直到上下文被销毁。
10. 如果bean实现了DisposableBean接口，Spring将调用它的destroy()接口方法。同样，如果bean使用destroy-method声明了销毁方法，该方法也会被调用。