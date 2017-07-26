package com.xyzq.simpson.maggie.component.service;

import com.xyzq.simpson.base.character.Encoding;
import com.xyzq.simpson.base.json.JSONArray;
import com.xyzq.simpson.base.json.JSONObject;
import com.xyzq.simpson.base.json.JSONString;
import com.xyzq.simpson.base.text.Text;
import com.xyzq.simpson.maggie.Maggie;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 控制台服务
 */
public class ConsoleService {
    /**
     * 日志对象
     */
    protected static Logger logger = LoggerFactory.getLogger(ConsoleService.class);


    /**
     * 添加文件
     *
     * @param path 路径
     * @param content 文件内容
     * @return 执行结果
     */
    public boolean add(String path, String content) {
        path = path.replace("/", File.separator);
        path = path.replace("\\", File.separator);
        if (path.startsWith(File.separator)) {
            path = Maggie.root() + path.substring(1);
        }
        else {
            path = Maggie.root() + path;
        }
        File file = new File(path);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        if (file.exists()) {
            file.delete();
        }
        try {
            Text.saveFile(content, 3, file);
        } catch (IOException e) {
            logger.error(String.format("file save failed:\npath = %s\ncontent=%s", new Object[]{path, content}), e);
            return false;
        }
        try {
            Maggie.dispatcher().loadScript(file);
        }
        catch (IOException e) {
            logger.error("add action failed, file = " + path, e);
            return false;
        }
        return true;
    }

    /**
     * 删除文件
     *
     * @param path 路径
     * @return 执行结果
     */
    public boolean delete(String path) {
        path = path.replace("/", File.separator);
        path = path.replace("\\", File.separator);
        if (path.startsWith(File.separator)) {
            path = Maggie.root() + path.substring(1);
        }
        else {
            path = Maggie.root() + path;
        }
        File file = new File(path);
        if (file.exists()) {
            file.delete();
        }
        try {
            Maggie.dispatcher().loadScript(file);
        }
        catch (IOException e) {
            logger.error("delete action failed, file = " + path, e);
            return false;
        }
        return true;
    }

    /**
     * 查看文件
     *
     * @param path 路径
     * @return 文件内容
     */
    public String view(String path) {
        File file = new File(Maggie.root() + path);
        if ((!file.exists()) || (file.isDirectory())) {
            return null;
        }
        try {
            return Text.loadFile(file, Encoding.ENCODING_UTF8);
        } catch (Exception e) {
            logger.error(String.format("view file failed:\npath = %s", new Object[]{path}), e);
        }
        return null;
    }

    /**
     * 枚举路径
     *
     * @param path 路径
     * @return 路径下文件集合
     */
    public JSONArray list(String path) {
        File dictionary = new File(Maggie.root() + path);
        JSONArray result = new JSONArray();
        if ((!dictionary.exists()) || (!dictionary.isDirectory())) {
            return result;
        }
        for (File item : dictionary.listFiles()) {
            JSONObject json = new JSONObject();
            json.put("name", new JSONString(item.getName()));
            if (item.isDirectory()) {
                json.put("type", new JSONString("folder"));
                result.insert(0, json);
            }
            else {
                json.put("type", new JSONString("file"));
                result.add(json);
            }
        }
        return result;
    }

    /**
     * 刷新文件
     *
     * @param path 路径
     * @return 执行结果
     */
    public boolean reload(String path) {
        try {
            if(Text.isBlank(path)) {
                return Maggie.dispatcher().loadAction();
            }
            else {
                File file = new File(path);
                return Maggie.dispatcher().loadScript(file);
            }
        }
        catch (IOException e) {
            logger.error("reload action failed, file = " + path, e);
            return false;
        }
    }
}
