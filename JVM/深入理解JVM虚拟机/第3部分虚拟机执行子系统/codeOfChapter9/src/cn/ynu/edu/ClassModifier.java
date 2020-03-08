package cn.ynu.edu;

public class ClassModifier {

	private static final int CONSTANT_POOL_COUNT_INDEX = 8;
	
	private static final int CONSTANT_Utf8_info = 1;
	
	private static final int[] CONSTANT_ITEM_LENGTH = {-1,-1,-1,5,5,9,9,3,3,5,5,5,5};
	
	private static final int u1 = 1;
	
	private static final int u2 = 2;
	
	private byte[] classByte;
	
	public ClassModifier(byte[] classByte) {
		this.classByte = classByte;
	}
	
	public byte[] modifyUTF8Constant(String oldStr, String newStr) {
		int cpc = getConstantPoolCount();
		int offset = CONSTANT_POOL_COUNT_INDEX + u2;
		for (int i = 0;i < cpc;i ++) {
			int tag = ByteUtils.bytes2Int(classByte, offset, u1);
			if (tag == CONSTANT_Utf8_info) {
				int len = ByteUtils.bytes2Int(classByte, offset + u1, u2 );
				offset += (u1 + u2);
				String str = ByteUtils.bytes2String(classByte, offset, len);
				if (str.equalsIgnoreCase(oldStr)) {
					byte[] strBytes = ByteUtils.string2Bytes(newStr);
					byte[] strLen = ByteUtils.int2Bytes(newStr.length(), u2);
					classByte = ByteUtils.bytesReplace(classByte, offset - u2, u2, strLen);
					classByte = ByteUtils.bytesReplace(classByte, offset, len, strBytes);
					return classByte;
				}
				else {
					offset += len;
				}
			}else {
				offset += CONSTANT_ITEM_LENGTH[tag];
			}
		}
		return classByte;
	}
	
	private int getConstantPoolCount() {
		// TODO Auto-generated method stub
		return ByteUtils.bytes2Int(classByte, CONSTANT_POOL_COUNT_INDEX, u2);
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
