package com.xyzq.simpson.maggie.access.servlet;

import com.xyzq.simpson.base.character.Encoding;
import com.xyzq.simpson.base.text.Text;
import com.xyzq.simpson.base.xml.XMLNode;
import com.xyzq.simpson.base.xml.core.IXMLNode;
import com.xyzq.simpson.maggie.Maggie;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.xyzq.simpson.maggie.framework.action.core.IAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * 过滤器
 */
public class MaggieFilter implements Filter {
    /**
     * 日志对象
     */
    private Logger logger = LoggerFactory.getLogger(MaggieFilter.class);

    /**
     * Called by the web container to indicate to a filter that it is
     * being placed into service.
     * <p>
     * <p>The servlet container calls the init
     * method exactly once after instantiating the filter. The init
     * method must complete successfully before the filter is asked to do any
     * filtering work.
     * <p>
     * <p>The web container cannot place the filter into service if the init
     * method either
     * <ol>
     * <li>Throws a ServletException
     * <li>Does not return within a time period defined by the web container
     * </ol>
     *
     * @param filterConfig
     */
    public void init(FilterConfig filterConfig) throws ServletException {
        String root = filterConfig.getInitParameter("root");
        if (Text.isBlank(root)) {
            String path = getClass().getClassLoader().getResource("/").getPath();
            int i = path.indexOf("WEB-INF");
            if (-1 == i) {
                throw new ServletException("lightning initialize failed");
            }
            root = path.substring(0, i) + "action";
        }
        File file = new File(root);
        if(!file.exists()) {
            file.mkdirs();
        }
        root = new File(root).getAbsolutePath();
        String upload = filterConfig.getInitParameter("upload");
        if (Text.isBlank(upload)) {
            file = new File("/apps/data/upload");
            if (!file.exists()) {
                file.mkdirs();
            }
            upload = file.getAbsolutePath();
        }
        else {
            upload = new File(upload).getAbsolutePath();
        }
        String actionScan = filterConfig.getInitParameter("actionScan");
        if (!Maggie.initialize(root, upload, actionScan, filterConfig.getInitParameter("packageBase"))) {
            this.logger.error("ServletFilter.init(?) execute failed");
            throw new ServletException("lightning initialize failed");
        }
        if (!Text.isBlank(actionScan)) {
            try {
                loadActionDefine();
            }
            catch (Exception e) {
                throw new RuntimeException("load action define failed", e);
            }
        }
        logger.info("maggie servlet filter initialized");
    }

    /**
     * 加载动作定义文件
     */
    private void loadActionDefine() throws Exception {
        org.springframework.core.io.Resource[] resources = (new PathMatchingResourcePatternResolver()).getResources(Maggie.actionScan());
        for(org.springframework.core.io.Resource resource : resources) {
            loadActionDefine(resource.getInputStream());
        }
    }

    /**
     * 加载动作定义输入流
     *
     * @param inputStream 输入流
     */
    private void loadActionDefine(InputStream inputStream) throws Exception {
        String text = Text.loadStream(inputStream, Encoding.ENCODING_UTF8);
        XMLNode xmlNode = XMLNode.convert(text);
        for(IXMLNode actionNode : xmlNode.visits("action")) {
            Maggie.dispatcher().loadAction(actionNode.get("uri"), (Class<IAction>) Class.forName(actionNode.get("class")));
        }
    }

    /**
     * The <code>doFilter</code> method of the Filter is called by the
     * container each time a request/response pair is passed through the
     * chain due to a client request for a resource at the end of the chain.
     * The FilterChain passed in to this method allows the Filter to pass
     * on the request and response to the next entity in the chain.
     * <p>
     * <p>A typical implementation of this method would follow the following
     * pattern:
     * <ol>
     * <li>Examine the request
     * <li>Optionally wrap the request object with a custom implementation to
     * filter content or headers for input filtering
     * <li>Optionally wrap the response object with a custom implementation to
     * filter content or headers for output filtering
     * <li>
     * <ul>
     * <li><strong>Either</strong> invoke the next entity in the chain
     * using the FilterChain object
     * (<code>chain.doFilter()</code>),
     * <li><strong>or</strong> not pass on the request/response pair to
     * the next entity in the filter chain to
     * block the request processing
     * </ul>
     * <li>Directly set headers on the response after invocation of the
     * next entity in the filter chain.
     * </ol>
     *
     * @param request
     * @param response
     * @param chain
     */
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (((HttpServletRequest) request).getRequestURI().equals("/")) {
            chain.doFilter(request, response);
            return;
        }
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        if (Maggie.doAction(((HttpServletRequest) request).getRequestURI().substring(1), (HttpServletRequest) request, (HttpServletResponse) response)) {
            return;
        }
        chain.doFilter(request, response);
    }

    /**
     * Called by the web container to indicate to a filter that it is being
     * taken out of service.
     * <p>
     * <p>This method is only called once all threads within the filter's
     * doFilter method have exited or after a timeout period has passed.
     * After the web container calls this method, it will not call the
     * doFilter method again on this instance of the filter.
     * <p>
     * <p>This method gives the filter an opportunity to clean up any
     * resources that are being held (for example, memory, file handles,
     * threads) and make sure that any persistent state is synchronized
     * with the filter's current state in memory.
     */
    public void destroy() {
        Maggie.terminate();
    }
}
