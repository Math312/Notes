# 面向切面编程

## 1.面向切面编程是什么？

我们在软件开发中都存在这样一种情况，有许多操作是在各个类中的某些操作后都要执行的，例如日志记录。然而在每个类中都写一个日志输出是很繁琐的，最起码不算很好维护，面向切面编程将这类操作同其余操作分离开来，让代码更简洁，功能更集中。

## 2.面向切面编程的术语

通知：
    
    通知是指出切面是什么以及在什么时候使用？定义了该切面是在调用某方法之前、之后、返回后、抛出异常后以及环绕方式运行。
    
连接点：
    
    连接点是指切面可以插入的时间点。
    
切点：

    切点定义了切面在何处插入。
    
切面：

    切面是切点和切面的集合。
    
引入：

    引入允许我们向现有的类中插入新的方法或属性。

织入：
    
    织入是把切面应用到目标对象并创建新的代理对象的过程。

## 3.面向切面编程的Spring实现方法：

Spring提供了4种类型的AOP支持

    基于代理的经典SpringAOP
    纯POJO切面
    @AspectJ注解驱动的切面
    注入式AspectJ切面
    
前3中都是通过SpringAOP实现的变体，SpringAOP构建在动态代理的基础上，因此只能局限于对方法的拦截。

如果你的AOP需求超过了简单的方法调用，你就需要去考虑AspectJ来实现切面

Spring通过代理类包裹切面，Spring在运行期把切面织入到Spring管理的bean中，当通知方法调用时，首先被代理类拦截，之后执行切面规定的方法，然后由代理类将调用转发给目标对象执行方法。

直到应用需要被代理的bean时，Spring才会创建代理对象，如果使用的是ApplicationContext的话，在ApplicationContext从beanFactory加载所有的bean时，Spring才会创建被代理的对象。因为Spring运行时创建才创建代理对象，所以我们不需要特殊的编译器来织入SpringAOP切面。

Spring借助AspectJ的切点表达式语言定义Spring切面

AspectJ指示器 | 描述
---|---
arg() | 限制连接点匹配参数为指定类型的执行方法
@args | 限制连接点匹配参数由指定注解标注的执行方法
execution() | 用于匹配是连接点的执行方法
this() | 限制连接点匹配AOP代理的bean引用为指定类型的类
target | 限制连接点匹配目标对象为指定类型的类
@target | 限制连接点匹配指定的执行对象，这些对象对应的类要有指定类型的注解
within() | 限制匹配连接点匹配指定的类型
@within() | 限制匹配连接点匹配指定注解所标注的类型(当使用SpringAOP时，方法定义在由指定的注解所标注的类)
@annotation | 限定匹配带有指定注解的连接点
bean() | 使用bean的ID或bean的名称作为参数来限制切点只匹配特定的bean
在Spring中尝试使用其他指示器时，会抛出IllegalArgumeException异常。

然而上述的指示器怎么用呢？我们接下来做出解答：

让我们考虑以下场景：

一个同学要做作业，那么做作业之前要调用老师类布置作业函数，作业成功完成后要调用批改作业函数，作业未成功完成要调用请家长函数，做作业过程中出现问题调用辅导函数。那么现在我们用代码的方式模拟以上过程，然后今儿学习切面如何使用。

(1)注解实现

a.Spring使用AspectJ注解来声明通知方法


注解 | 通知
---|---
@After | 通知方法会在目标方法返回或抛出异常后调用
@AfterReturning | 通知方法会在目标方法返回后调用
@AfterThrowing | 通知方法会在目标方法抛出异常后调用
@Around |通知方法会将目标方法封装起来
@Before | 通知方法会在目标方法调用之前执行

例子：

Student类：

    @Component
    public class Student
    {
	    public void doHomework(int label) throws Exception
	    {

		    	if(label == 1)
			    System.out.println( "Doing homework!!");
			    else
				    throw  new Exception();
	    }
    }
    
教师切面:

    @Aspect
    public class Teacher
    {
    	@Pointcut("execution(* ynu.edu.springInAction.AOP.Java.Student.doHomework(..))"+"&&args(label)")
    	public void doHomework(int label){}

    	@Before("doHomework(int)")
    	public void setHomework()
    	{
    		System.out.println("Setting     homeWork1!!");
    	}

    	@After("doHomework(int)")
    	public void submitHomework()
    	{
    		System.out.println("Submitting homeWork1!!");
    	}

    	@AfterReturning("doHomework(int)")
    	public void homeworkCorrecting()
    	{
    		System.out.println("HomeWork Correcting1!!");
    	}

    	@AfterThrowing("doHomework(int)")
    	public  void  haveATalk()
    	{
	    	System.out.println("Having a talk1!!!");
    	}
	}
	
我们在Teacher切面中将目标方法设置为Student类的doHomework(int label)方法。设置步骤如下：

首先声明一个@PointCut，我们为该注解提供了如下字符串：

    "execution(* ynu.edu.springInAction.AOP.Java.Student.doHomework(..))"+"&&args(label)"
    
该字符串的含义如下

    execution(返回值(即上述的*) 包名.类名.函数名(参数列表(即上述的..)))
    
截止到现在，上述代码声明的意义如下：

目标函数为 （包名.类名）.函数，该函数的参数列表任意

后面的字符串
    
    args(label)
    
表明这个函数传入的一个参数参数名为label。当然我们前面的列表内也给出了可以使用的AspectJ指示器。

值得一提的是，采用AspectJ指示器指定切点可以采用 “&&”、“||”、“!”三种符号表示“与”、“或”、“非”的逻辑关系。在使用XML文件进行配置时甚至可以用and、or、not来指定这三种关系。

配置类:

    @ComponentScan
    @Configuration
    @EnableAspectJAutoProxy
    public class TeacherConfig
    {
    	@Bean
    	public  Teacher getTeacher()
    	{
    		return  new Teacher();
    	}
    }
    
测试类：

    @RunWith(SpringJUnit4ClassRunner.class)
    @ContextConfiguration(classes = TeacherConfig.class)
    public class TeacherConfigTest
    {
    	@Autowired
    	private  Student student;
    	@Autowired
	        private Teacher teacher;
            @Test
    	public void  testDoHomework()
    	{
    		try
    		{
    			student.doHomework(2);
    		} catch(Exception e)
    		{
			e.printStackTrace();
	    	}
    	}
    }

事实上AOP还提供了一个更强大的方法，可以让我们为被代理包装的bean提供功能扩展。

仔细想想这并不是什么不可能的事，下面我们对SpringAOP的原理进行简单分析，请看下图：

![](http://note.youdao.com/yws/api/personal/file/4F0A3B150A984BF485D5C6695665F0D2?method=download&shareKey=5077f777611cb541007250c18c577c28)

我们在调用目标对象的函数时，首先请求会被代理类拦截，然而用户是不知道这件事的，因此，我们可以在代理类上做一些手脚，这样，调用与目标对象绑定的代理类的函数就和给目标对象动态增加功能没有区别了。

因此上图就变成了这样：

![image](http://note.youdao.com/yws/api/personal/file/C4991D4EBA0846F59D19B7774CF1BA6B?method=download&shareKey=17de5310e7ad654fd5bf74b4b008f90a)

下面我们给出例子：

我们要为学生类添加讨论方法：

Discuss接口：

    public interface Discuss
    {
    	public String discuss();
    }
    
默认实现：

    public class DefaultDiscuss implements Discuss
    {

    	@Override
    	public String discuss()
    	{
    		return "hello";
    	}
    }
    
另外声明的切面：

    @Aspect
    @Component
    public class ExpandAspect
    {
    	@DeclareParents(value = "ynu.edu.springInAction.AOP.Java.Student+",defaultImpl = DefaultDiscuss.class)
	    public static Discuss discuss;
    }
    
测试类：

    @RunWith(SpringJUnit4ClassRunner.class)
    @ContextConfiguration(classes = TeacherConfig.class)
    public class TeacherConfigTest
    {
    	@Autowired
    	private  Student student;

    	@Autowired
    	private Discuss discuss;


    	@Test
    	public void  testDoHomework()
    	{
    		try
    		{
			//student.doHomework(2);
	    		System.out.println(discuss.discuss());
	    	} catch(Exception e)
	    	{
	    		e.printStackTrace();
	    	}
	    }
    }
    
我们可以看到在新声明的切面中运用了一个新的注解：@DeclareParents，该注解由3部分组成：

a.value属性制定了哪种类型的bean要引用该接口。上述例子值得是Student类及其所有子类，后面的加号表示Student类的所有子类。

b.defaultImpl属性指定了为引入功能提供实现的类。

c.@DeclareParents注解所标注的静态属性指明了要引入的接口。


(2)XML文件配置


AOP配置元素 | 用途
---|---
<aop:advisor> | 定义通知器
<aop:after> | 定义后置通知（不管被通知方法是否执行完成）
<aop:after-returning> | 定义AOP返回通知
<aop:after-throwing> | 定义AOP异常通知
<aop:around> | 定义AOP环绕通知
<aop:aspect> | 定义一个切面
<aop:aspectj-autoproxy> | 启动@AspectJ注解驱动的切面
<aop:before> | 定义一个AOP前置通知
<aop:config> | 顶层的AOP配置元素。大多数的<aop:*>元素必须包含在<aop:config>元素内
<aop:declare-paents> | 以透明的方式为通知的对象引入额外的接口
<aop:pointcut> | 定义一个切点

下面给出几个例子用来完成同java配置相同的操作。

例1：

前置通知和后置通知演示：

Student类：

    public class Student
    {
	    public void doHomework(int Label) throws Exception
	    {

		if(Label == 1)
			System.out.println( "Doing homework!!");
		else

				throw  new Exception();

	    }
    }
    
Teacher类：

    public class Teacher
    {

	    	public void setHomework()
	    	{
	    		System.out.println("Setting homeWork1!!");
	    	}

	    	public void submitHomework()
	    	{
	    		System.out.println("Submitting homeWork1!!");
	    	}

		public void homeworkCorrecting()
		{
			System.out.println("HomeWork Correcting1!!");
		}

		public  void  haveATalk()
		{
			System.out.println("Having a talk1!!!");
		}

		public void process(ProceedingJoinPoint jp)
		{

			try
			{
				System.out.println("Setting homeWork2!!");
				jp.proceed();
				System.out.println("Submitting homeWork2!!");
				System.out.println("HomeWork Correcting2!!");
			} catch(Throwable throwable)
			{
				System.out.println("Having a talk2!!!");
			}
		}
    }
    
配置文件：

    <?xml version="1.0" encoding="UTF-8" ?>
    <http:beans xmlns:http="http://www.springframework.org/schema/beans"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns:aop="http://www.springframework.org/schema/aop"
        xsi:schemaLocation="http://www.springframework.org/schema/aop
                                        http://www.springframework.org/schema/aop/spring-aop.xsd
                                        http://www.springframework.org/schema/beans
                                        http://www.springframework.org/schema/beans/spring-beans.xsd">

    <http:bean id="student"
                class="ynu.edu.springInAction.AOP.XML.Student">
    </http:bean>
    <http:bean id="teacher"
                class="ynu.edu.springInAction.AOP.XML.Teacher">
    </http:bean>

    <aop:config>
        <aop:aspect ref="teacher">
                <aop:pointcut id="doHomework" expression="execution(* ynu.edu.springInAction.AOP.XML.Student.doHomework(..) )"  />
                <aop:before pointcut-ref="doHomework" method="setHomework"></aop:before>
                <aop:after pointcut-ref="doHomework" method="submitHomework"/>
                <aop:after-returning pointcut-ref="doHomework" method="homeworkCorrecting"/>
                <aop:after-throwing pointcut-ref="doHomework" method="haveATalk"/>
        </aop:aspect>
    </aop:config>
    </http:beans> 
    
测试类：

    @RunWith(SpringJUnit4ClassRunner.class)
    @ContextConfiguration

    public class TeacherConfig
    {
	@Autowired
	private Student student;

	@Test
	public void doHomework()
	{
		try
		{
			student.doHomework(2);
		} catch(Exception e)
		{
			e.printStackTrace();
		}
	}
    }

测试结果：

    Setting homeWork1!!
    Submitting homeWork1!!
    Having a talk1!!!
    
将测试类中doHomework()参数改为1，测试结果为：

    Setting homeWork1!!
    Doing homework!!
    Submitting homeWork1!!
    HomeWork Correcting1!!
    
环绕通知演示：
由于环绕通知中仍然使用了上述的Student、Teacher类和测试类，所以在此我们只给出变化的配置文件部分：

    <?xml version="1.0" encoding="UTF-8" ?>
    <http:beans xmlns:http="http://www.springframework.org/schema/beans"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns:aop="http://www.springframework.org/schema/aop"
        xsi:schemaLocation="http://www.springframework.org/schema/aop
                                        http://www.springframework.org/schema/aop/spring-aop.xsd
                                        http://www.springframework.org/schema/beans
                                        http://www.springframework.org/schema/beans/spring-beans.xsd">

    <http:bean id="student"
                class="ynu.edu.springInAction.AOP.XML.Student">
    </http:bean>
    <http:bean id="teacher"
                class="ynu.edu.springInAction.AOP.XML.Teacher">
    </http:bean>

    <aop:config>
        <aop:aspect ref="teacher">
                <aop:pointcut id="doHomework" expression="execution(* ynu.edu.springInAction.AOP.XML.Student.doHomework(..) )"  />
                <!--<aop:before pointcut-ref="doHomework" method="setHomework"></aop:before>-->
                <!--<aop:after pointcut-ref="doHomework" method="submitHomework"/>-->
                <!--<aop:after-returning pointcut-ref="doHomework" method="homeworkCorrecting"/>-->
                <!--<aop:after-throwing pointcut-ref="doHomework" method="haveATalk"/>-->
                <aop:around method="process" pointcut-ref="doHomework"/>
        </aop:aspect>
    </aop:config>
    </http:beans>
    
这里只是简单的将上个例子的四个配置标签变成了around标签。

引入新的功能：

配置文件：

    <?xml version="1.0" encoding="UTF-8" ?>
    <http:beans xmlns:http="http://www.springframework.org/schema/beans"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns:aop="http://www.springframework.org/schema/aop"
        xsi:schemaLocation="http://www.springframework.org/schema/aop
                                        http://www.springframework.org/schema/aop/spring-aop.xsd
                                        http://www.springframework.org/schema/beans
                                        http://www.springframework.org/schema/beans/spring-beans.xsd">

    <http:bean id="student"
                class="ynu.edu.springInAction.AOP.XML.Student">
    </http:bean>
    <http:bean id="teacher"
                class="ynu.edu.springInAction.AOP.XML.Teacher">
    </http:bean>

    <http:bean id="defaultDiscuss"
               class="ynu.edu.springInAction.AOP.XML.DefaultDiscuss">
    </http:bean>
    <aop:config>
        <aop:aspect ref="teacher">
                <aop:pointcut id="doHomework" expression="execution(* ynu.edu.springInAction.AOP.XML.Student.doHomework(..) )"  />
                <!--<aop:before pointcut-ref="doHomework" method="setHomework"></aop:before>-->
                <!--<aop:after pointcut-ref="doHomework" method="submitHomework"/>-->
                <!--<aop:after-returning pointcut-ref="doHomework" method="homeworkCorrecting"/>-->
                <!--<aop:after-throwing pointcut-ref="doHomework" method="haveATalk"/>-->
                <!--<aop:around method="process" pointcut-ref="doHomework"/>-->
                <aop:declare-parents types-matching="ynu.edu.springInAction.AOP.XML.Student+"
                                     implement-interface="ynu.edu.springInAction.AOP.XML.Discuss" delegate-ref="defaultDiscuss"/>
        </aop:aspect>
    </aop:config>
    </http:beans>
    
测试类：

    @RunWith(SpringJUnit4ClassRunner.class)
    @ContextConfiguration

    public class TeacherConfig
    {
	@Autowired
	private Student student;

	@Autowired
	private DefaultDiscuss discuss;

	@Test
	public void doHomework()
	{
		try
		{
			student.doHomework(1);
			discuss.discuss();
		} catch(Exception e)
		{
			e.printStackTrace();
		}
	}
    }
    
测试结果：

    Doing homework!!
    Hello!

    

