package cn.edu.whu.smartsensing.util;

import android.util.Log;

import java.io.File;

public class FileUtil {


    // 生成文件
    public static File makeFilePath(String filePath, String fileName) {
        File file = null;
        makeRootDirectory(filePath);
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
    public static File makeRootDirectory(String filePath) {
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
}
