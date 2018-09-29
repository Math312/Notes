package cn.ynu.edu;

public class HotSwapClassLoader extends ClassLoader{

	public HotSwapClassLoader() 
	{
		super(HotSwapClassLoader.class.getClassLoader());
	}
	
	public Class loadByte(byte[] classByte) 
	{
		return defineClass(null,classByte,0,classByte.length);
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
