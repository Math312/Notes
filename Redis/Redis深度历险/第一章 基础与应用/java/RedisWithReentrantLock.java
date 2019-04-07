/*************************************************************************
	> File Name: RedisWithReentrantLock.java
	> Author: Math312
	> Mail: 894688078@qq.com 
	> Created Time: 一  3/25 19:51:43 2019
 ************************************************************************/
public class RedisWithReentrantLock{
	private ThreadLocal<Map<String,Integer>> lockers = new ThreadLocal<>();

	private 


