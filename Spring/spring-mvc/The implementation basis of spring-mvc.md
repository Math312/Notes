# Spring mvc 实现基础

传统的方式实现servlet、filter、listener需要在web.xml中进行注册，包括spring-mvc的前端控制器DispatcherServlet。然而servlet 3.0标准之后，提供了使用注解的方式完成上述功能。

PS：servlet 3.0标准需要tomcat 7 版本及以上的支持的，属于JSR 315系列的规范。

## Shared libraries（共享库）/runtimes pluggability（运行时插件能力）

1. Servlet容器启动会扫描当前应用里面每一个jar包的ServletContainerInitializer的实现
2. 提供ServletContainerInitializer的实现类：
   - 必须绑定在META-INF/services/javax.servlet.ServletContainerInitializer这个文件中
   - 文件的内容就是ServletContainerInitializer的实现类的全类名

总结：Servlet容器在启动应用时会扫描当前应用META-INF/services/javax.servlet.ServletContianerInitializer所制定的实现类。启动并运行这个实现类的方法。

使用该方法注册Servlet,Listener,Filter的方法如下：

    @HandlesTypes(value= {HttpServlet.class})
    public class MyServletContainerInitializer implements ServletContainerInitializer{

	/**
	 * 
	  *  应用启动时运行onStartup()方法
	 * 
	 * @param Set<Class<?>> arg0:感兴趣的类型的所有子类型
	 * @param ServletContext arg1:代表当前web应用的ServletContext:一个web对应一个servletContext
	 * */
	@Override
	public void onStartup(Set<Class<?>> arg0, ServletContext arg1) throws ServletException {
            // TODO Auto-generated method stub
            System.out.println("感兴趣的类型：");
            for(Class<?> t:arg0) {
                System.out.println(t.getName());
            }
            
            ServletRegistration.Dynamic servlet = arg1.addServlet("userServlet", new UserServlet());
            servlet.addMapping("/user");
            
            arg1.addListener(UserListener.class);
            
            FilterRegistration.Dynamic filter= arg1.addFilter("userFilter", UserFilter.class);
            filter.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/*");
        }

    }