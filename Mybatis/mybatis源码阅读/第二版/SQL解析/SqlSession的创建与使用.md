# SQL解析

回想一下使用Mybatis的操作过程：

1. 根据配置文件使用`SqlSessionFactoryBuilder`创建`SqlSessionFactory`
2. 使用`SqlSessionFactory`获取一个`SqlSession`
3. 通过`SqlSession`获取`XXXMapper`对象
4. 最后通过获取到的`XXXMapper`对象调用我们在配置文件中声明的方法。

上一章我们已经将结果了第1步，即根据配置文件创建`SqlSessionFactory`，通过上一章我们知道Mybatis会将配置文件解析成为一个Configuration对象，将该对象保存在`SqlSessionFactory`中，默认情况下使用的是`DefaultSqlSessionFactory`。那么接下来让我们考虑如何剩下三步是怎么完成的。

根Mybatis文档，我们可以看到如果你已经配置完了配置文件，需要进行业务操作，那么你需要按照如下例子写出业务代码：

```java
try (SqlSession session = sqlSessionFactory.openSession()) {
  Blog blog = (Blog) session.selectOne("org.mybatis.example.BlogMapper.selectBlog", 101);
}
```

这里调用的就是BlogMapper的selectBlog(int)方法。可以看到，这种方式是很容易出错的，因为没有编译提示。所以，Mybatis还提供了如下方式帮助你消除这种错误。

```java
try (SqlSession session = sqlSessionFactory.openSession()) {
  BlogMapper mapper = session.getMapper(BlogMapper.class);
  Blog blog = mapper.selectBlog(101);
}
```

无论使用上面的哪种方式，都无法逃脱如下的步骤：

1. 使用SqlSessionFactory获取SqlSession
2. 通过获取到的SqlSession进行数据库持久化操作

这里笔者打算优先介绍第一种方式的执行逻辑，再介绍完毕这部分之后，再介绍Mybatis是如何将第一种方式转换为第二种方式的。但是无论顺序如何，都需要先介绍其通用步骤：根据SqlSessionFactory获取SqlSession。

## 根据SqlSessionFactory获取SqlSession

对于Mybatis来说，一个SqlSession就相当于对于数据库的一次会话。例如对于Mysql来说，就相当于你在`Mysql-shell`中打开的一个连接，或者`Mysql-workbench`中打开的一个窗口，对比于JDBC来说就相当于一个`Connection`，不过Mybatis对`Connection`进行了封装，这让这个`Connection`使用起来更加方便。

为了使读者更方便理解，这里给出一个使用JDBC进行数据库操作的例子：

```java
public class Main {

    public static void main(String[] args) throws SQLException {
        // 加载JDBC驱动
        try{
            Class.forName("com.mysql.jdbc.Driver") ;
        }catch(ClassNotFoundException e){
            System.out.println("找不到驱动程序类 ，加载驱动失败！");
            e.printStackTrace() ;
        }
        String url = "jdbc:mysql://XXX:3306/mybatis" ;
        String username = "root" ;
        String password = "XXXXXX" ;
        Connection con = null;
        try{
            // 获取Connection
            con = DriverManager.getConnection(url , username , password ) ;
            String sql = "select username, password from t_user where username = ?";
            con.setAutoCommit(false);
            // 构建持久化操作SQL
            PreparedStatement statement = con.prepareStatement(sql);
            statement.setString(1,"andre");
            // 执行SQL
            ResultSet rs = statement.executeQuery();
            con.commit();
            // 处理结果
            while (rs.next()) {
                String name = rs.getString("username");
                System.out.println(name);
            }
            rs.close();
        }catch(SQLException se){
            System.out.println("数据库连接失败！");
            se.printStackTrace() ;
        }finally {
            if (con != null) {
                con.close();
            }
        }
    }
}
```

正常情况下，jdbc进行持久化操作步骤如下：

1. 加载数据库驱动
2. 使用数据库驱动获得连接
3. 创建执行SQL
4. 执行SQL
5. 接收并执行结果
6. 关闭连接

SqlSessionFactory帮我们封装第`2`步，SqlSession就封装了第`3,4`这四步。让我们首先查看`SqlSessionFactory`接口源码，查看他是怎么对第2步操作进行封装的。

```java
public interface SqlSessionFactory {
  SqlSession openSession(XXX);

  Configuration getConfiguration();
}
```

`SqlSessionFactory`仅仅要求实现两族方法：

1. `SqlSession openSession(XXX)`：该方法代表了一组获取SqlSession的方法，该方法重载了很多版本。
2. `Configuration getConfiguration()`：该方法仅仅是Configuration的getter方法。

这里我们主要讨论第一族方法，由于默认情况下，我们使用的都是`DefaultSqlSessionFactory`，这里我们考察`DefaultSqlSessionFactory.SqlSession openSession(XXX)`方法，这族方法的具体实现都交给`openSessionFromDataSource(ExecutorType execType, TransactionIsolationLevel level, boolean autoCommit)`方法实现：

```java
public SqlSession openSession() {
    return openSessionFromDataSource(configuration.getDefaultExecutorType(), null, false);
}

public SqlSession openSession(boolean autoCommit) {
    return openSessionFromDataSource(configuration.getDefaultExecutorType(), null, autoCommit);
}
```

这里只拿两个方法举例，如果读者感兴趣可以去`org.apache.ibatis.session.defaults.DefaultSqlSessionFactory`查看，这里我们主要分析真正处理业务逻辑的`openSessionFromDataSource(ExecutorType execType, TransactionIsolationLevel level, boolean autoCommit)`方法，源码如下：

```java
private SqlSession openSessionFromDataSource(ExecutorType execType, TransactionIsolationLevel level, boolean autoCommit) {
    Transaction tx = null;
    try {
      // 获取Mybatis中配置的Environment属性
      // Environment中包含了两个对象的设置：
      // 1. 数据源
      // 2. 事务管理器
      final Environment environment = configuration.getEnvironment();
      final TransactionFactory transactionFactory = getTransactionFactoryFromEnvironment(environment);
      // 通过事务管理器开启一个新的事务
      tx = transactionFactory.newTransaction(environment.getDataSource(), level, autoCommit);
      // 通过ExecutorType创建一个Executor，该Executor负责执行真正的数据库查询操作
      final Executor executor = configuration.newExecutor(tx, execType);
      // 返回SqlSession
      return new DefaultSqlSession(configuration, executor, autoCommit);
    } catch (Exception e) {
      closeTransaction(tx); // may have fetched a connection so lets call close()
      throw ExceptionFactory.wrapException("Error opening session.  Cause: " + e, e);
    } finally {
      ErrorContext.instance().reset();
    }
}
```

可以看到，Mybatis中`DefaultSqlSessionFactory`的SqlSession都是通过事务管理器获取的。获取SqlSession需要经过如下几步：

1. 获取Configuration中配置的事务管理器
2. 通过事务管理器创建一个新的事务
3. 为事务对象创建一个新的Executor
4. 将创建好的Executor放到SqlSession中

所以可以看出，对于Mybatis来说事务的生命周期是交给Executor来接管的。这里我们不对Executor进行详细分析，因为这部分的内容将在SQL执行的部分进行解析。

不过这里可以考虑一个问题，很多时候，我们在用Mybatis写demo时，由于用不到事务，所以是没有配置事务管理器的，那么如下代码为什么没有出现空指针异常呢？

```java
final TransactionFactory transactionFactory = getTransactionFactoryFromEnvironment(environment);
// 通过事务管理器开启一个新的事务
tx = transactionFactory.newTransaction(environment.getDataSource(), level, autoCommit);
```

这个问题很好回答，肯定是Mybatis实现了一个默认的事务管理器，但是这个事务管理器什么事情都没有做。我们考察`getTransactionFactoryFromEnvironment(Environment environment)`方法源码：

```java
  private TransactionFactory getTransactionFactoryFromEnvironment(Environment environment) {
    if (environment == null || environment.getTransactionFactory() == null) {
      return new ManagedTransactionFactory();
    }
    return environment.getTransactionFactory();
  }
```

可以看到，Mybatis默认使用的是`ManagedTransactionFactory`，我们进一步查看这个事务管理器创建的事务是什么样子的，查看`ManagedTransactionFactory`的`newTransaction(Connection conn)`方法：

```java
public Transaction newTransaction(Connection conn) {
  return new ManagedTransaction(conn, closeConnection);
}
```

`ManagedTransactionFactory`创建的事务是`ManagedTransaction`，该事务类有一个特点，它的`commit()`和`rollback()`方法都是空的。它仅仅是一个`Connection`持有者。所以这就是Mybatis如何处理默认配置的。

分析到了这里，我们已经了解了一个SqlSession是如何产生的。其实SqlSession就是一个开启了事务的`Connection`，并且封装了很多持久化用的方法，而无需我们再去输入SQL，并且傻傻的拼接各种各样的参数。

## 通过SqlSession进行持久化操作

上一节已经讨论了SqlSession的创建过程，当SqlSession创建完成后，我们就可以使用它的API去进行持久化操作。在介绍具体的操作步骤之前，我们首先查看一下`SqlSession`到底提供了一套哪些持久化操作API。`SqlSession`接口源码如下：

```java
public interface SqlSession extends Closeable {

  <T> T selectOne(String statement);

  <T> T selectOne(String statement, Object parameter);

  <E> List<E> selectList(String statement);

  <E> List<E> selectList(String statement, Object parameter);

  <E> List<E> selectList(String statement, Object parameter, RowBounds rowBounds);

  <K, V> Map<K, V> selectMap(String statement, String mapKey);

  <K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey);

  <K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey, RowBounds rowBounds);

  <T> Cursor<T> selectCursor(String statement);

  <T> Cursor<T> selectCursor(String statement, Object parameter);

  <T> Cursor<T> selectCursor(String statement, Object parameter, RowBounds rowBounds);

  void select(String statement, Object parameter, ResultHandler handler);

  void select(String statement, ResultHandler handler);

  void select(String statement, Object parameter, RowBounds rowBounds, ResultHandler handler);

  int insert(String statement);

  int insert(String statement, Object parameter);

  int update(String statement);

  int update(String statement, Object parameter);

  int delete(String statement);

  int delete(String statement, Object parameter);

  void commit();

  void commit(boolean force);

  void rollback();

  void rollback(boolean force);

}
```

通过方法名其实就可以看出，使用SqlSession和使用基本的JDBC差距就在是否要自己拼装Sql以及是否要自己处理结果，不过这已经够香了。由于上一节创建的`SqlSession`是`DefaultSqlSession`，所以我们还是分析该类的源码。首先分析用于查询的方法，由于`DefaultSqlSession`中执行`select`操作的方法实现都十分相似，这里只分析具有代表性的`selectMap(XXX)`：

```java
  // 方法1
  @Override
  public <K, V> Map<K, V> selectMap(String statement, String mapKey) {
    return this.selectMap(statement, null, mapKey, RowBounds.DEFAULT);
  }
  // 方法2
  @Override
  public <K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey) {
    return this.selectMap(statement, parameter, mapKey, RowBounds.DEFAULT);
  }
  // 方法3
  @Override
  public <K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey, RowBounds rowBounds) {
    // 调用selectList获取数据
    final List<? extends V> list = selectList(statement, parameter, rowBounds);
    // 使用DefaultMapResultHandler将List数据转化为Map数据
    final DefaultMapResultHandler<K, V> mapResultHandler = new DefaultMapResultHandler<K, V>(mapKey,
        configuration.getObjectFactory(), configuration.getObjectWrapperFactory(), configuration.getReflectorFactory());
    final DefaultResultContext<V> context = new DefaultResultContext<V>();
    for (V o : list) {
      context.nextResultObject(o);
      mapResultHandler.handleResult(context);
    }
    return mapResultHandler.getMappedResults();
  }

  // 方法4
  @Override
  public <E> List<E> selectList(String statement, Object parameter, RowBounds rowBounds) {
    try {
      // 使用Executor进行查询
      MappedStatement ms = configuration.getMappedStatement(statement);
      return executor.query(ms, wrapCollection(parameter), rowBounds, Executor.NO_RESULT_HANDLER);
    } catch (Exception e) {
      throw ExceptionFactory.wrapException("Error querying database.  Cause: " + e, e);
    } finally {
      ErrorContext.instance().reset();
    }
  }
```

可以看到，`DefaultSqlSession`将真正的查询操作交给了`Executor`。事实上Mybatis的所有持久化操作都是交给`Executor`处理的，可以查看一下剩下的`update`、`insert`、`delete`方法，其实都是这样。

但是这里有三点需要注意：

1. Mybatis如果接收结果是Map，那么转化工作是在SqlSession做的

    想必刚才的例子已经充分说明了这一点，Mybatis中如果持久化的结果是Map，那么最后结果的转化工作由SqlSession创建的`DefaultMapResultHandler`完成。实际上，`DefaultMapResultHandler`也是一次处理一条数据，而不是一次处理多条。通过下面代码就可以看出：

    ```java
    for (V o : list) {
      context.nextResultObject(o);
      mapResultHandler.handleResult(context);
    }
    ```

    注意，很多时候，我们有这种需求，有一张数据表，数据表中每行数据代表一个用户，每个用户有个id，我希望查询出返回的数据是一个id为key，User对象为value的HashMap。但是Mybatis的XML映射配置没有提供这种配置啊。实际上，Mybatis是支持这种操作的，可以看到，`DefaultMapResultHandler`的构造代码如下：

    ```java
    final DefaultMapResultHandler<K, V> mapResultHandler = new DefaultMapResultHandler<K, V>(mapKey,
        configuration.getObjectFactory(), configuration.getObjectWrapperFactory(), configuration.getReflectorFactory());
    ```

    第一个参数就是Map中key的属性名。那么这个mapKey参数到底是怎么获取到的呢？事实上是对应的`Mapper`对象的对应方法上加上`@MapKey`注解就可以了，但是现在还没有江街道Mapper对象与SqlSession整合，所以这里先提一下，之后在详细讲解。

2. 在Mybatis中`update`、`insert`、`delete`对应的都是`Executor`的`update`方法
3. Mybatis的`selectOne`是通过`selectList`实现的。

源码分析到这里，有些人可能会问，SqlSession也就封装了这些方法嘛，而且最后执行还不都是交给Executor执行，那Mybatis仅仅做的不就是封装加复用Sql么。实际上如果Executor接口仅仅只有一种实现，那么这个说法确实有一定道理，但是事实并不是这样，实际上Executor接口有多种实现。我们可以很轻易的看出，SqlSession的设计使用了策略模式，这使得所有的Executor可以按照自己的方式进行持久化操作。

## 总结

到这里SqlSession的创建与基本使用都已经介绍完毕了。

通过这部分分析我们了解了如下几点：

1. 对于Mybatis来说，事务其实是由SqlSession进行管理的，当SqlSession被创建时会给他开启一个事务，由SqlSession进行管理
2. SqlSession的所有持久化操作实际上都是通过`Executor`完成的，这里使用了策略模式
3. Executor实际上只提供了select和update操作，对于`insert`、`update`、`delete`操作都是update操作

下一节将介绍Mapper对象是如何使用SqlSession完成Java对象与XML文件的映射的。
