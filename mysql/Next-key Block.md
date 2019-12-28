# Next-key Block

众所周知，Mysql的事务隔离级别分为4个，分别是READ-UNCOMMITED，READ-COMMITED，REPEATABLE-READ，SERIALIZABLE，在常规数据库概论中，前三种事务隔离级别会带来脏读、不可重复读、幻读的问题，对应关系如下：

||脏读|不可重复读|幻读
|---|---|---|---|
|READ-UNCOMMITED|√|√|√|
|READ-COMMITED|×|√|√|
|REPEATABLE-READ|×|×|√|
|SERIALIZABLE|×|×|×|

但是在Mysql中使用了Next-key Block解决了幻读问题,下面我们通过讨论该问题来详细讨论Next-key Block，这里考虑一个常见的幻读情况，首先创建示例表：

```sql
create database test;
use test;
CREATE TABLE `t` (
  `t1` int(11) NOT NULL,
  `t2` int(11) DEFAULT NULL,
  PRIMARY KEY (`t1`),
  KEY `t2` (`t2`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
```

将其中加入几条示例数据：

```sql
insert into t values(1,0),(2,10),(3,20),(4,30),(5,40);
```

接下来考虑一个常见的幻读情况，我们可以先将mysql的Next-key Block关闭，可以采用如下两种方式对其进行关闭：

1. 将事务隔离级别设置为READ-COMMITTED
2. 将参数`innodb_locks_unsafe_for_binlog`设置为1，注意这里设置为1是关闭Next-key Block

由于`innodb_locks_unsafe_for_binlog`参数需要重启服务器才能进行配置，因此我们采用第一种方式，将session的事务隔离级别设置为READ-COMMITTED。下面考察一般的幻读情况，我们的实验方式如下：

|事务1|事务2|
|---|---|
|begin;||
|select * from t where t2=20;（查到一条记录，（3，20））||
||begin;|
||insert into t value(6,20);|
||commit;|
|select * from t where t2=20;（查到两条记录（3，20），（6，20））||
|commit;||

事务1实验过程如下：

```shell
mysql> set session transaction isolation level read committed; # 设置当前session的事务隔离级别为READ-COMMITED
Query OK, 0 rows affected (0.00 sec)

mysql> set autocommit = 0; # 取消自动Commit
Query OK, 0 rows affected (0.00 sec)

mysql> begin; # 开始一个新事务
Query OK, 0 rows affected (0.00 sec)

mysql> select * from t where t2=20;  # 首次查询t2为20的数据，查询点1
+----+------+
| t1 | t2   |
+----+------+
|  3 |   20 |
+----+------+
1 row in set (0.00 sec)

mysql> select * from t where t2=20; # 事务2未提交时查询t2为20的数据，查询点2
+----+------+
| t1 | t2   |
+----+------+
|  3 |   20 |
+----+------+
1 row in set (0.00 sec)

mysql> select * from t where t2=20; # 事务2提交后查询t2为20的数据，查询点3（出现幻读）
+----+------+
| t1 | t2   |
+----+------+
|  3 |   20 |
|  6 |   20 |
+----+------+
2 rows in set (0.00 sec)

mysql> commit; # 提交事务1
Query OK, 0 rows affected (0.00 sec)
```

事务2执行过程如下：

```shell
mysql> set session transaction isolation level read committed; # 设置当前session的事务隔离级别为READ-COMMITED
Query OK, 0 rows affected (0.00 sec)

mysql> set autocommit = 0; # 取消自动Commit
Query OK, 0 rows affected (0.00 sec)

mysql> begin; # 开始一个事务
Query OK, 0 rows affected (0.00 sec)

mysql> insert into t value(6,20); # 调用点1、调用点2之间进行插入新数据  这里同时也是为了营造t2列的索引是非唯一索引的情况，否则会简化为Record Lock，为下一步的讨论做准备
Query OK, 1 row affected (0.00 sec)

mysql> commit; # 调用点2、调用点3之间进行提交
Query OK, 0 rows affected (0.00 sec)
```

可以看到，这种情况下幻读正常发生。

接下来，考察使用Next-key Block防止出现幻读的情况时，会发生的情况。这里我们再次强调一下我对幻读的理解，考虑当前有事务A、B，事务A中具有两条一模一样的查询语句执行（例如上述例子的调用点1和3，注意，我们不考虑调用点2），在两条查询语句执行的中间，事务B提交了会影响到事务A两条查询语句结果的`插入请求`（事务2的插入语句），这时，事务A的查询语句的执行结果会和第一条的查询结果不同，就好似出现了幻觉。那么接下来真正开始讨论Next-key Block。

## Next key Block

讨论Next-key Block之前，我们需要对一些基本概念进行解释，Mysql的锁算法有3种：

1. 记录锁（Record Lock），该锁锁的是一条索引记录（注意是索引记录）
2. 间隙锁（GAP Lock），该锁锁的是一个范围，但是该范围是(X,Y)类型的，注意是两边都是开区间
3. Next-key Block，有人将其称之为后码锁，不过我还是感觉英文名更贴近其意思，他将记录锁和间隙锁组合应用，幻读就是通过它解决的。

介绍完基础概念之后我们继续开始探究，基本的查询语句显而易见有3种，大于、小于、等于、不等于，这里我们主要讨论这四种情况，接下来对其进行一一讨论，不过首先要都把事务隔离级别设置为REPEATABLE-READ。

### 1. 大于的情况

考虑查询语句更改为如下语句：

```sql
select * from t where t2>20 for update;
```

在这种情况下，我们猜想应该给大于20的t2列的索引全部加锁，而对于插入的方面又可以分为3类：

1. 插入b列小于20的数据

    ```sql
    insert into t value(7,19);
    ```

    胡乱猜想也可以知道，这种情况并不会导致插入语句锁住的情况，因为上述的锁并没有涉及到t2列为19的情况，事实证明也是如此。

    这里给出实验结果

    |事务1|事务2|
    |---|---|
    |begin;||
    |select * from t where t2>20 for update;（查到两条记录，（4，30），（5，40））||
    ||begin;|
    ||insert into t value(7,19);|
    ||commit;|
    |select * from t where t2=20 for update;（查到两条记录，（4，30），（5，40））||
    |commit;||

    为了下面的实验，我们将数据库还原，即删除t1=7的数据。

2. 插入b列等于20的数据

    ```sql
    insert into t value(7,20);
    ```

    首先，我们猜想，如此情况插入数据不会被事务1中的查询语句锁住，因为没有涉及到会更改查询结果的部分，接下来进行实验；

    |事务1|事务2|
    |---|---|
    |begin;||
    |select * from t where t2>20;（查到两条记录，（4，30），（5，40））||
    ||begin;|
    ||insert into t value(7,20); # 阻塞了|
    
    这时我们考虑是哪个锁阻塞掉了该插入操作，查询`information_schema`.`innodb_locks`表。结果如下：

    lock_id| lock_trx_id| lock_mode| lock_type| lock_table| lock_index| lock_space| lock_page| lock_rec| lock_data
    ---|---|---|---|---|---|---|---|---|---
    '1371:23:4:5'| '1371'| 'X,GAP'| 'RECORD'| '`test`.`t`'| 't2'| '23'| '4'| '5'| '30, 4'
    '1370:23:4:5'| '1370'| 'X'| 'RECORD'| '`test`.`t`'| 't2'| '23'| '4'| '5'| '30, 4'

    其中第一行是事务2导致的，第二行是事务1导致的。可以看到事务1的查询语句还对t2为30的索引列加了写锁。而事务2请求的也是t2为30的写锁，我明明插入的是20为什么是请求t2为30的写锁呢？

    根据我们的猜想，我们了解对于t2>20的索引列都被加上了锁，那么为什么插入的是20，却锁的是30呢？考虑之前的数据，我们发现30是20后面的一个索引值。这里我们先给标记起来（mark 1）。

    这里我们直接rollback就好了，还是恢复数据库。

3. 插入b列大于20的数据

    ```sql
    insert into t value(7,20);
    ```

    该情况与第二种插入等于20的数据加锁一致，此处不再赘述。

### 2.小于的情况

考虑查询语句更改为如下语句：

```sql
select * from t where t2<20 for update;
```

1. 插入b列大于20的数据

    ```sql
    insert into t value(7,21);
    ```

    这种情况其实和1.1情况类似，我们猜想插入数据与查询数据无关，必定不会锁住，实际上也是这样。

2. 插入b列等于20的数据

    ```sql
    insert into t value(7,20);
    ```

    这里我们猜想，应该也和1.2情况类似，会直接锁住，但是实际上你错了，这里直接插入成功了，查看实验结果：

    |事务1|事务2|
    |---|---|
    |begin;||
    |select * from t where t2<20 for update;||
    ||begin;|
    ||insert into t value(7,20);# 注意没有阻塞|
    ||commit;|
    |select * from t where t2<20 for update;||
    |commit;||

    这是为什么呢？明明上一个加锁了啊，为什么这个没有加锁，直接就添加上了，我们考察上一个加的锁是大于20的间隙锁，我们插入20时，锁住的是t2为30的索引，而30正是20的下一个索引，这是否意味着：

    `索引的下一个值其实是用来锁住上一个值到下一个值的区间的。`简单来讲就是t2=30这个索引的锁会锁住[20,30)这个范围。

    这里我们继续考察，恢复数据库。

3. 插入b列小于20的数据

    ```sql
    insert into t value(7,19);
    ```

    这种情况下执行结果与1.3的情况类似，插入操作也被阻塞了，这里列出加锁情况。

    lock_id| lock_trx_id| lock_mode| lock_type| lock_table| lock_index| lock_space| lock_page| lock_rec| lock_data
    ---|---|---|---|---|---|---|---|---|---
    '1373:23:4:4'| '1373'| 'X，GAP'| 'RECORD'| '`test`.`t`'| 't2'| '23'| '4'| '4'| '20， 3'
    '1372:23:4:4'| '1372'| 'X'| 'RECORD'| '`test`.`t`'| 't2'| '23'| '4'| '4'| '20， 3'

    这里刚刚符合我们说的`索引的下一个值其实是用来锁住上一个值到下一个值的区间的。`结论，这里应该锁住的就是[10,20)的区间，所以该区间内的插入都不会成功。那么此时我如果把他变为插入`(7,9)`这条数据呢？我猜想会锁住`10,2`吧，这里试验一下。

    lock_id| lock_trx_id| lock_mode| lock_type| lock_table| lock_index| lock_space| lock_page| lock_rec| lock_data
    ---|---|---|---|---|---|---|---|---|---
    '1373:23:4:3'| '1373'| 'X，GAP'| 'RECORD'| '`test`.`t`'| 't2'| '23'| '4'| '4'| '10， 2'
    '1372:23:4:3'| '1372'| 'X'| 'RECORD'| '`test`.`t`'| 't2'| '23'| '4'| '4'| '10， 2'

    事实证明这里我蒙对了。

有事，晚上继续补完等于和不等于的讨论情况。