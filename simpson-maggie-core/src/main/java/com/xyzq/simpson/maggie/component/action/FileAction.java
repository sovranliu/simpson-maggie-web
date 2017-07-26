package com.xyzq.simpson.maggie.component.action;

import com.xyzq.simpson.maggie.framework.action.core.IAction;
import com.xyzq.simpson.maggie.framework.Context;
import com.xyzq.simpson.maggie.framework.Visitor;

import java.io.*;

/**
 * 文件下载动作
 */
public class FileAction implements IAction {
    /**
     * 待下载地址键
     */
    public final static String CONTEXT_KEY_FILE = "file";
    /**
     * 缓存尺寸
     */
    public final static int BUFFER_SIZE = 1024;


    /**
     * 调用
     *
     * @param visitor 访问者
     * @param context 上下文
     * @return 下一步动作，不包括后缀名称，null表示结束
     */
    @Override
    public String execute(Visitor visitor, Context context) throws Exception {
        visitor.response().reset();
        if(null == context.get(CONTEXT_KEY_FILE)) {
            return null;
        }
        File file = null;
        if(context.get(CONTEXT_KEY_FILE) instanceof File) {
            file = (File) context.get(CONTEXT_KEY_FILE);
        }
        else {
            file = new File((String) context.get(CONTEXT_KEY_FILE));
        }
        if(!file.exists()) {
            return null;
        }
        visitor.response().setHeader("Content-Disposition", "attachment; filename=" + file.getName());
        FileInputStream fileInputStream = new FileInputStream(file);
        try {
            transfer(fileInputStream, visitor.response().getOutputStream());
        }
        catch (IOException ex) {
            try {
                fileInputStream.close();
            }
            catch (IOException e) { }
            fileInputStream = null;
            throw ex;
        }
        return null;
    }

    /**
     * 输入转换输出
     *
     * @param source 源
     * @param target 目标
     * @return 传输字节数
     */
    public static long transfer(InputStream source, OutputStream target) throws IOException {
        long result = 0L;
        byte[] buffer = new byte[BUFFER_SIZE];
        int bytes = 0;
        while((bytes = source.read(buffer)) != -1) {
            if(0 == bytes) {
                continue;
            }
            target.write(buffer, 0, bytes);
            target.flush();
            result += bytes;
        }
        return result;
    }
}
