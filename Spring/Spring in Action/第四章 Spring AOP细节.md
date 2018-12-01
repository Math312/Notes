# Spring AOP的细节

本篇文章我们来考虑下一些细节方面的事情：

## 1.@After、@AfterThrowing、@AfterReturning的调用先后关系

我们仍使用学生、老师的例子，查看如下代码：

学生类：

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
    
教师切面：

    @Aspect
    @Component
    public class Teacher
    {
    	@Pointcut("execution(* ynu.edu.springInAction.AOP.Java.Student.doHomework(..))"+"&&args(label)")
	    public void doHomework(int label){}

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
    
配置类：

    @ComponentScan
    @Configuration
    @EnableAspectJAutoProxy
    public class TeacherConfig
    {
    }


测试类：

    @RunWith(SpringJUnit4ClassRunner.class)
    @ContextConfiguration(classes = TeacherConfig.class)
    public class TeacherConfigTest
    {
    	@Autowired
    	private  Student student;

	    @Test
    	public void  testDoHomework()
    	{
	    	try
	    	{
	    		student.doHomework(2);
	    	} catch(Exception e)
	    	{
    
	    	}
	    }
    }
    
测试结果：

    Submitting homeWork1!!
    Having a talk1!!!
    
从测试结果我们可以看出，如果在切面中同时声明出@After和@AfterThrowing时，被@After注解标记的函数会先调用，然后再调用被@AfterThrowing注解标记的函数。

那么@After和@AfterRunning的关系是否也是这样呢，我们现在将测试类改变成如下模样：

    @RunWith(SpringJUnit4ClassRunner.class)
    @ContextConfiguration(classes = TeacherConfig.class)
    public class TeacherConfigTest
    {
    	@Autowired
	    private  Student student;


	    @Test
	    public void  testDoHomework()
	    {
	    	try
	    	{
    			student.doHomework(1);
    		} catch(Exception e)
	    	{
    
	    	}
    	}
    }
    
新的测试类与原测试类区别很小，只是在第14行将doHomework函数的参数从2变成了1，使该函数不抛出异常。下面我们再看测试结果：

    Doing homework!!
    Submitting homeWork1!!
    HomeWork Correcting1!!
    
@After注解所标记的函数在@AfterReturning注解所标记的函数之前调用了，这证明我们的猜测是正确的。我们也可以看到Student类的doHomework函数是无返回的但是@After和@AfterReturning正常执行了，也就意味着无返回的函数正常运行也是算正常返回的。所以我们得出结论。

结论：被@After注解标注的函数会在被@AfterReturning注解标注的函数和被@AfterThrowing标注的函数之前运行，并且就算无返回，正常运行函数后被@After和@AfterReturning标注的函数也会正常运行。

## 2.@Around和@Before、@After、@AfterReturning、@AfterThrowing的调用顺序

继续采用上面的例子，我们更改教师切面如下：

    @Aspect
    @Component
    public class Teacher
    {
    	@Pointcut("execution(* ynu.edu.springInAction.AOP.Java.Student.doHomework(..))"+"&&args(label)")
    	public void doHomework(int label){}

    	@Before("doHomework(int)")
    	public void setHomework()
	    {
	    	System.out.println("Setting homeWork1!!");
	    }

    	@After("doHomework(int)")
    	public void submitHomework()
    	{
	    	System.out.println("Submitting     homeWork1!!");
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

    	@Around("doHomework(int)")
    	public void process(ProceedingJoinPoint jp)
    	{

	    	try
	    	{
	    		System.out.println("Setting homeWork2!!");
	    		jp.proceed();
	    		System.out.println("Submitting     homeWork2!!");
	    		System.out.println("HomeWork Correcting2!!");
	    	} catch(Throwable throwable)
	    	{
	    		System.out.println("Having a     talk2!!!");
	    	}
    	}

    }
    
测试类仍采用doHomework参数为1的测试类，测试结果如下：

    Setting homeWork2!!
    Setting homeWork1!!
    Doing homework!!
    Submitting homeWork2!!
    HomeWork Correcting2!!
    Submitting homeWork1!!
    HomeWork Correcting1!!
    
很容易看出，被@Around注解修饰的函数的运行顺序是优先于其他被@Before、@After、@AfterReturning注解修饰的函数的。

将doHomework函数的参数变为2，测试结果如下：

    Setting homeWork2!!
    Setting homeWork1!!
    Having a talk2!!!
    Submitting homeWork1!!
    HomeWork Correcting1!!
    
可以看出此处被@around注解的方法将错误拦截，导致被@After注解和@AfterReturning注解以及@AfterThrowing注解标记的函数根本无法意识到有错误。

综上所述，被@around标记的函数会在@After、@AfterReturning、@AfterThrowing之前调用。

#### 切面上的切面
    
在此我们在Teacher切面上添加一个新的切面，名为Teacher2。

Teacher2类:

    @Aspect
    @Component
    public class Teacher2
    {
	@Pointcut("execution(* ynu.edu.springInAction.AOP.Java.Teacher.setHomework())")
	public void setHomework()
	{
	}

	@Before("setHomework()")
	public void beforeSetHomework()
	{
		System.out.println("123");
	}
    }

我们知道切面本身也是一个bean,那么理论上给一个bean中的一个切点增加一个切面是很正常的，那么现在我们再用测试类测试下，看看Teacher2类会不会在Teacher类调用setHomework()方法前调用beforeSetHomework()方法呢。

测试结果：

    Setting homeWork1!!
    Submitting homeWork1!!
    Having a talk1!!!
    
事实证明，我们想在切面上添加切面是无法实现的，但是这里没有错误提示，这证明这种想法是符合语法的，至于为何此想法无法实现，笔者会在后面补上。