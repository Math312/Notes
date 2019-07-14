import java.util.*;
import java.io.*;
public class BuildUtil{
    
    public static void processFile(String parentName,File file) {
        if(file.isDirectory()) {
            String[] innerFiles = file.list();
            for(String innerFileName: innerFiles) {
                String absolutePath = parentName+"/"+innerFileName;
                processFile(absolutePath,new File(absolutePath));
            }
        }else {
            processMd(new File(file.getAbsolutePath()));
        }
    }

    private static void appendFileHeader(byte[] header,String srcPath) throws Exception{
		RandomAccessFile src = new RandomAccessFile(srcPath, "rw");
		int srcLength = (int)src.length() ;
		byte[] buff = new byte[srcLength];
			src.read(buff , 0, srcLength);
			src.seek(0);
			src.write(header);
			src.seek(header.length);
			src.write(buff);
			src.close();
	}

    public static void processMd(File file) {
        if(file.getName().endsWith(".md")) {
            String header = "---\n";
            String title = "title: \""+file.getName().substring(0,file.getName().lastIndexOf(".md"))+"\"\n";
            String date = "date: "+new Date()+"\n";
            String draft = "draft: true\n";
            String tail = "---\n";
            String all = header+title+date+draft+tail;
            try {
                appendFileHeader(all.getBytes(), file.getAbsolutePath());
            }catch(Exception e){

            }
        }
    }
    
    public static void main(String[] args) {
        File file = new File("Notes");
        processFile(file.getName(),file);

    }
}