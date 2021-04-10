package cn.edu.whu.smartsensing.util;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FileUtil {

    private static final String tag = "File Util";

    public static void generateUUID(String UUIDPath) {
        String id = UUID.randomUUID().toString();
        File file = new File(UUIDPath + "info");
        if (!file.exists()) {
            writeTxtToFile("mc-" + id, UUIDPath, "info");
            Log.i("File Util", "新生成了UUID：" + id);
            return;
        }
        makeFile(UUIDPath + "/mc/", "data.txt");
    }

    /**
     * 一次性读取全部文件数据
     */
    public static String readFile(String filePath, String fileName){
        try {
            InputStream is = new FileInputStream(filePath + fileName);
            int iAvail = is.available();
            byte[] bytes = new byte[iAvail];
            is.read(bytes);
            String result = new String(bytes);
            Log.i(tag, "文件内容:" + result);
            is.close();
            return result;
        }
        catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 覆盖写入data
     */
    public static void writeFile(String filePath, String fileName, List<String> data) {
        try {
            File dir = new File(filePath);
            File dest = new File(filePath + fileName);
            if(!dir.exists()) {
                dir.mkdirs(); dest.createNewFile();
            }
            RandomAccessFile raf = new RandomAccessFile(dest, "rwd");
            BufferedWriter bw = new BufferedWriter(new FileWriter(dest));
//            raf.seek(dest.length());
//            raf.write(date.getBytes());
//            raf.close();
            data.forEach(
                    s -> {
                        try {
                            bw.write(s);
                            bw.newLine();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
            );
            bw.flush();bw.close();
        } catch (IOException e) {
        }
    }

    /**
     * 按行读取文件
     */
    public static List<String> readFileByLine(String filePath, String fileName){
        try {
            File file = new File(filePath + fileName);
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            String strLine = null;
            int lineCount = 1;
            ArrayList<String> result = new ArrayList<String>();
            while(null != (strLine = bufferedReader.readLine())){
                Log.i(tag, "第[" + lineCount + "]行数据:[" + strLine + "]");
                result.add(strLine);
                lineCount++;
            }
            return result;
        }
        catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 生成文件
     */
    public static File makeFile(String filePath, String fileName) {
        File file = null;
        makeFileDirectory(filePath);
        try {
            file = new File(filePath + fileName);
            if (!file.exists()) {
                boolean createResult = file.createNewFile();
                if (createResult) {
                    System.out.println("创建文件成功");
                    return file;
                }
                else {
                    System.out.println("创建文件失败");
                    return null;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // 生成文件夹
    public static File makeFileDirectory(String filePath) {
        File file = null;
        try {
            file = new File(filePath);
            if (!file.exists()) {
                boolean mkdir = file.mkdir();
                if (mkdir) {
                    System.out.println("创建目录成功");

                }
                else {
                    System.out.println("创建目录失败");
                }
            }

        } catch (Exception e) {
            Log.i("error:", e + "");
        }
        return file;
    }

    // 将字符串写入到文本文件中
    public static void writeTxtToFile(String content, String filePath, String fileName) {


        String strFilePath = filePath + fileName;
        // 每次写入时，都换行写
        String strContent = content + "\r\n";
        try {
            File file = new File(strFilePath);
            if (!file.exists()) {
                Log.d("TestFile", "Create the file:" + strFilePath);
//                boolean mkdirs = file.getParentFile().mkdirs();
//                file.createNewFile();
                file = FileUtil.makeFile(filePath, fileName);
            }
            RandomAccessFile raf = new RandomAccessFile(file, "rwd");
            raf.seek(file.length());
            raf.write(strContent.getBytes());
            raf.close();
        } catch (Exception e) {
            Log.e("TestFile", "Error on write File:" + e);
        }
    }


}
