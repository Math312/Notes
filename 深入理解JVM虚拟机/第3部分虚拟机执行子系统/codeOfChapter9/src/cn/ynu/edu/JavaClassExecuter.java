package cn.ynu.edu;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;

public class JavaClassExecuter {

	public static String execute(byte[] classByte) {
		HackSystem.clearBuffer();
		ClassModifier cm = new ClassModifier(classByte);
		byte[] modiBytes = cm.modifyUTF8Constant("java/lang/System", "cn/ynu/edu/HackSystem");
		HotSwapClassLoader loader = new HotSwapClassLoader();
		try {
			System.out.println(new String(modiBytes,"UTF-8"));
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		Class clazz = loader.loadByte(modiBytes);
		try {
			Method method = clazz.getMethod("main", new Class[] {String[].class});
			method.invoke(null, new String[] {null});
		}
		catch(Throwable e) 
		{
			e.printStackTrace(HackSystem.out);
		}
		return HackSystem.getBufferString();
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
