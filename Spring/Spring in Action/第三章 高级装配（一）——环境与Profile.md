## 一、环境与profile

1.为何要采用这种技术？这种技术有什么用途？

事实上，我们都会遇到如下的情况，同一个对象可能在不同的时候需要不同的配置，而在这种情况下，强行的硬编码将会带来很大的问题，这时Spring给我们提供了良好的方法将改变的与不变的相解耦。

2.如何采用这门技术？

让我们考虑一下这种情况，假设你需要做如下一个系统：

一个系统开发时、运行时采用不同的数据库，你需要对其进行一个良好的设计。平常情况下你会怎么做？

方法一、设计一个接口，然后让其子类实现这个接口，子类中包含了数据库的一个属性，可以通过构造器注入动态改变。

方法二、类似方法一，不过此时使用的不是接口，而是抽象类，但实质大同小异。

方法三、采用Spring的profile技术

这是我能想到的三种方法，下面我们将一一列举并比较他们的优缺点。

由于方法一、二极其类似，因此我们将其归并为一项。如下：

首先是抽象类：

    package ynu.edu.springInAction.profile.common;
    import ynu.edu.springInAction.profile.DataBase;
    public abstract class DataBaseAbstract
    {
	    protected DataBase database;

	    public void setDatabase(DataBase db)
	    {
		    this.database = db;
	    }
	    public DataBase getDatabase()
	    {
		    return database;
	    }

	    public abstract  void check();
	    public abstract  void delete();
	    public abstract  void insert();
	}
	
接下来是子类2：

    package ynu.edu.springInAction.profile.common;
    import ynu.edu.springInAction.profile.MengoDb;
    public class DataBase2 extends DataBaseAbstract
    {
	    public DataBase2()
	    {
		    super.database = new MengoDb();
	    }
	    @Override
	    public void check()
	    {
		    System.out.println("MengoDB is checking!!");
	    }

	    @Override
	    public void delete()
	    {
		    System.out.println("MengoDB is deleting an item!!");
	    }

	    @Override
	    public void insert()
	    {
		    System.out.println("MengoDB is inserting an item!!");
	    }
    }

子类1类似:

    package ynu.edu.springInAction.profile.common;
    import ynu.edu.springInAction.profile.DataBase;
    import ynu.edu.springInAction.profile.SQLServerDb;

    public class DataBase1 extends  DataBaseAbstract
    {
	    public DataBase1()
	    {
		    super.setDatabase(new SQLServerDb());
	    }
	    @Override
	    public void check()
	    {
		    System.out.println("SQLServer is checking!!");
	    }

	    @Override
	    public void delete()
	    {
		    System.out.println("SQLServer is deleting an item!!");
	    }
    
	    @Override
	    public void insert()
	    {
		    System.out.println("SQLServer is inserting an item!!");
	    }
    }

运用DataBase的类如下：

    package ynu.edu.springInAction.profile.common;
    public class Main
    {
	    private DataBaseAbstract db;

	    public Main(DataBaseAbstract db)
	    {
		    this.db = db;
	    }

	    public void  check()
	    {
		    db.check();
	    }

	    public void delete()
	    {
		    db.delete();
	    }

	    public  void insert()
	    {
		    db.insert();;
	    }
    }
    
上述代码确确实实地完成了我们所提供的需求，但是仔细看来缺陷也是很大，当我们在进行配置时，需要手动更改代码，这种方式并不是一种良好的设计，当然这里的Main类可以采用前面讲到的依赖注入来解决这个麻烦。而且由于上述例子采用的是抽象类的方法，导致每个类无法实现单例，若是想实现，可以采用接口的实现方法，但是这种方法又导致了代码的冗余。而且还有一个更大的缺点，便是，上述方法只能实现一个属性的动态改变，多个属性的捆绑动态改变需要人为进行控制，或者硬编码。

那么下面我们来看一下Spring所提供的profile的方法：

数据库1的代码:

    package ynu.edu.springInAction.profile.spring;

    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.stereotype.Component;
    import ynu.edu.springInAction.profile.SQLServerDb;

    @Component
    public class DataBase1 extends DataBaseAbstract
    {

	    public DataBase1()
	    {
		    super.setDatabase(new SQLServerDb());
	    }

	    @Override
	    public void check()
	    {
		    System.out.println("SQLServer is checking!!");
	    }

	    @Override
	    public void delete()
	    {
		    System.out.println("SQLServer is deleting an item!!");
	    }

	    @Override
	    public void insert()
	    {
		    System.out.println("SQLServer is inserting an item!!");
	    }
    }
    
数据库2的代码：

    package ynu.edu.springInAction.profile.spring;
    import org.springframework.stereotype.Component;
    import ynu.edu.springInAction.profile.MengoDb;

    @Component
    public class DataBase2 extends DataBaseAbstract
    {

	    public DataBase2()
    	{
    		super.database = new MengoDb();
	    }

	    @Override
	    public void check()
	    {
    		System.out.println("MengoDB is checking!!");
    	}

	    @Override
	    public void delete()
	    {
		    System.out.println("MengoDB is deleting an item!!");
	    }

	    @Override
	    public void insert()
	    {
		    System.out.println("MengoDB is inserting an item!!");
	    }
    }
    
数据库抽象类代码：

    package ynu.edu.springInAction.profile.spring;

    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.stereotype.Component;
    import ynu.edu.springInAction.profile.DataBase;


    public abstract class DataBaseAbstract
    {
	    protected DataBase database;

	    public void setDatabase(DataBase db)
	    {
    		this.database = db;
	    }

	    public DataBase getDatabase()
	    {
		    return database;
	    }

	    public abstract  void check();
	    public abstract  void delete();
	    public abstract  void insert();
    }
    
主类：

    package ynu.edu.springInAction.profile.spring;

    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.stereotype.Component;

    @Component
    public class Main
    {

	    private DataBaseAbstract db;

	    @Autowired
	    public Main(DataBaseAbstract db)
	    {
		    this.db = db;
	    }

	    public void  check()
	    {
		    db.check();
	    }

	    public void delete()
	    {
		    db.delete();
	    }   

	    public  void insert()
	    {
		    db.insert();;
	    }
    }
    
配置类：

    package ynu.edu.springInAction.profile.spring;

    import org.springframework.context.annotation.Bean;
    import org.springframework.context.annotation.Configuration;
    import org.springframework.context.annotation.Profile;


    @Configuration
    public class MainConfig
    {
	    @Bean
	    public  Main getMain(DataBaseAbstract db)
	    {
		    return  new Main(db);
	    }

	    @Profile("way1")
	    @Bean
	    public DataBaseAbstract getDataBase1()
	    {
		    return  new DataBase1();
	    }
	    @Profile("way2")
	    @Bean
	    public DataBaseAbstract getDataBase2()
	    {
		    return  new DataBase2();
	    }
    }
    
@Profile注解是一个类级注解也是一个方法级注解，通过该注解我们可以将配置进行分类，可以达到一类配置捆绑加载的目的。标注了@Profile只有在其被激活的情况下才会被加载，未标注的则会正常加载。而且由于Spring容器中的Bean默认情况下为单例，因此解决了对象应为单例的问题。

在此我们查看一下上述例子的测试类：

    package ynu.edu.springInAction.profile.spring;

    import org.junit.Test;
    import org.junit.runner.RunWith;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.test.context.ActiveProfiles;
    import org.springframework.test.context.ContextConfiguration;
    import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

    import static org.junit.Assert.*;

    @RunWith(SpringJUnit4ClassRunner.class)
    @ContextConfiguration(classes = {MainConfig.class})
    @ActiveProfiles("way1")
    public class MainTest
    {

	    @Autowired
    	private Main main;
    	@Test
    	public void check() throws Exception
    	{
    		main.check();
    	}
    
    	@Test
    	public void delete() throws Exception
    	{
    		main.delete();
    	}

    	@Test
    	public void insert() throws Exception
    	{
    		main.insert();
      	}

    }
    
注解@ActiveProfiles指定了需要激活的Profile，当然可以是多个。

spring.profiles.active以及spring.profiles.default两个属性确定了哪个Profiles被激活，如果设置了Spring.profiles.active被设置了，那么将按照该值进行激活，否则按照spring.profiles.default进行激活。

对上述两个属性进行设置的方式：

1. 作为DispatcherServlet的初始化参数
2. 作为Web项目的上下文参数
3. 作为JDNI条目
4. 作为环境变量
5. 作为JVM系统属性
6. 在集成测试类上，使用@ActiveProfiles注释进行设置
7. 




    





