package com.xyzq.simpson.maggie.utility.template;

import com.xyzq.simpson.base.model.core.IFilter;
import com.xyzq.simpson.base.type.List;
import com.xyzq.simpson.base.type.safe.Table;
import org.apache.commons.collections.ExtendedProperties;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.resource.Resource;
import org.apache.velocity.runtime.resource.loader.ResourceLoader;

import java.io.*;
import java.util.Vector;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * Maggie模板加载器
 */
public class MaggieTemplateLoader extends ResourceLoader {
    /**
     * 根目录集合
     */
    private File[] roots = null;
    /**
     * 缓存映射
     */
    private Table<String, File> cache = null;


    /**
     * 初始化
     *
     * @param configuration 配置
     */
    public void init(ExtendedProperties configuration) {
        Vector vector = configuration.getVector("path");
        if(null == vector) {
            roots = new File[0];
        }
        else {
            List<File> list = new List<File>();
            for(Object path : vector) {
                if(null == path) {
                    continue;
                }
                if(path instanceof String) {
                    list.add(new File((String) path));
                }
                else if(path instanceof File) {
                    list.add((File) path);
                }
            }
            roots = list.toArray(File.class);
        }
        cache = new Table<String, File>();
    }

    /**
     * 获取指定模版的输入流
     *
     * @param templateName 模版名称
     * @return InputStream containing the template
     */
    public InputStream getResourceStream(String templateName) throws ResourceNotFoundException {
        if(templateName.startsWith("/") || templateName.startsWith("\\")) {
            templateName = templateName.substring(1);
        }
        final String path = templateName;
        for(File root : roots) {
            if(!root.exists()) {
                continue;
            }
            File file = new File(root.getAbsolutePath() + File.separator + templateName);
            if(file.canRead()) {
                InputStream inputStream = fetchStreamFromFile(file);
                if(null != inputStream) {
                    cache.put(path, root);
                }
                return inputStream;
            }
        }
        for(File root : roots) {
            if(!root.exists()) {
                continue;
            }
            InputStream inputStream = visitDirectoryForJar(root, new IFilter<File, InputStream>() {
                /**
                 * 过滤
                 *
                 * @param origin 源数据
                 * @return 目标数据结构
                 */
                @Override
                public InputStream filter(File origin) {
                    InputStream inputStream =  fetchStreamFromJar(origin, path);
                    if(null != inputStream) {
                        cache.put(path, origin);
                    }
                    return inputStream;
                }
            });
            if(null != inputStream) {
                return inputStream;
            }
        }
        return null;
    }

    /**
     * 判断资源是否存在
     *
     * @param name 资源名称
     * @return 是否存在
     */
    public boolean resourceExists(String name) {
        InputStream inputStream = getResourceStream(name);
        if(null == inputStream) {
            return false;
        }
        try {
            inputStream.close();
        }
        catch (IOException e) { }
        return true;
    }

    /**
     * 资源是否发生变化
     *
     * @param resource 资源对象
     * @return 是否发生变化
     */
    public boolean isSourceModified(Resource resource) {
        return getLastModified(resource) != resource.getLastModified();
    }

    /**
     * 获取指定资源的最后变更时间
     *
     * @param resource 资源对象
     * @return 最后变更时间
     */
    public long getLastModified(Resource resource) {
        File file = cache.get(resource.getName());
        if(null == file || !file.exists()) {
            return 0;
        }
        if(file.isDirectory()) {
            File templateFile = new File(file.getAbsolutePath() + File.separator + resource.getName());
            if(!templateFile.canRead()) {
                return 0;
            }
            return templateFile.lastModified();
        }
        else {
            if(!file.canRead()) {
                return 0;
            }
            return file.lastModified();
        }
    }

    /**
     * 从文件中获取输入流
     *
     * @param file 文件
     * @return 输入流
     */
    private static InputStream fetchStreamFromFile(File file) {
        try {
            return new BufferedInputStream(new FileInputStream(file));
        }
        catch (FileNotFoundException e) {
            throw new RuntimeException("template file missing\nfile = " + file.getAbsolutePath(), e);
        }
    }

    /**
     * 从Jar包中获取输入流
     *
     * @param file Jar文件
     * @param path Jar内路径
     * @return 输入流
     */
    private static InputStream fetchStreamFromJar(File file, String path) {
        if(path.startsWith("/") || path.startsWith("\\")) {
            path = path.substring(1);
        }
        path = "action/" + path;
        try {
            JarFile jarFile = new JarFile(file);
            ZipEntry zipEntry = jarFile.getEntry(path);
            if(null == zipEntry) {
                return null;
            }
            return jarFile.getInputStream(zipEntry);
        }
        catch (IOException e) {
            throw new RuntimeException("load template file failed\nfile = " + file.getAbsolutePath() + "\npath = " + path, e);
        }
    }

    /**
     * 为查找jar遍历目录
     *
     * @param directory 待遍历目录
     * @param filter 过滤器
     * @return 目标jar文件
     */
    private static InputStream visitDirectoryForJar(File directory, IFilter<File, InputStream> filter) {
        if(!directory.isDirectory()) {
            return null;
        }
        for(File item : directory.listFiles()) {
            if(item.isDirectory()) {
                InputStream inputStream = visitDirectoryForJar(item, filter);
                if(null != inputStream) {
                    return inputStream;
                }
            }
            else {
                if(item.getName().endsWith(".jar")) {
                    InputStream inputStream = filter.filter(item);
                    if(null != inputStream) {
                        return inputStream;
                    }
                }
            }
        }
        return null;
    }
}
