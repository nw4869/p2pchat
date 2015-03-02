package com.nightwind.p2pchat;

import java.io.UnsupportedEncodingException;

public class Utils {

    public static int utf8Len(byte b) {
        int num = b & 255;
        if(num < 0x80) return 1;
        if(num < 0xe0) return 2;
        if(num < 0xf0) return 3;
        if(num < 0xf8) return 4;
        return 5;
    }
    
    public static String substringByByteCount(String str, int byteCount) {
    	byte[] bytes = str.getBytes();
    	if (bytes.length <= byteCount) {
    		return str;
    	}
    	int strIndex = 0;
    	for (int i = 0; i < bytes.length; ) {
    		int len = utf8Len(bytes[i]);
    		if (i + len <= byteCount) {
    			i += len;
        		strIndex++;	
    		} else {
    			break;
    		}
    	}
    	return str.substring(0, strIndex);
    }
    
	public static void main(String[] args) throws UnsupportedEncodingException {
//		String str = "中a中 ";
//		byte[] bytes = str.getBytes();
//		int charCount = 0;
//		int i = 0;
//		while (i < bytes.length) {
//			i += utf8An invocation of this method of the form str.replaceFirst(regex, repl) yields exactly the same result as the expressionLen(bytes[i]);
//			charCount++;
//		}
//		System.out.println(charCount + " " + i);
//		
//		String str1 = substringByByteCount(str, 9);
//		System.out.println(str1);
		
		String str2 = "online client:\n192.168.1.100:48690 (you) \n";
		String str3 = str2.replaceFirst(str2, "");
		System.out.println(str3);
	}
}

class UTF8Processor {
    private byte[] buffer = new byte[6];
    private int count = 0;

    public String processByte(byte nextByte) throws UnsupportedEncodingException {
        buffer[count++] = nextByte;
        if(count == expectedBytes())
        {
            String result = new String(buffer, 0, count, "UTF-8");
            count = 0;
            return result;
        }
        return null;
    }

    private int expectedBytes() {
        int num = buffer[0] & 255;
        if(num < 0x80) return 1;
        if(num < 0xe0) return 2;
        if(num < 0xf0) return 3;
        if(num < 0xf8) return 4;
        return 5;
    }
}