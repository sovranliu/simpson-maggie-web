package com.xyzq.simpson.maggie.component.action;

import com.xyzq.simpson.base.io.net.http.HttpClient;
import com.xyzq.simpson.base.io.net.http.HttpRequest;
import com.xyzq.simpson.base.io.net.http.body.HttpFormBody;
import com.xyzq.simpson.base.io.net.http.body.HttpRawBody;
import com.xyzq.simpson.base.io.net.http.body.HttpURLEncodedBody;
import com.xyzq.simpson.base.io.net.http.filter.HttpTextFilter;
import com.xyzq.simpson.base.type.Set;
import com.xyzq.simpson.base.type.Table;
import com.xyzq.simpson.base.type.core.ILink;
import com.xyzq.simpson.base.type.core.ISet;
import com.xyzq.simpson.maggie.framework.action.core.IAction;
import com.xyzq.simpson.maggie.framework.Context;
import com.xyzq.simpson.maggie.framework.Visitor;

/**
 * 反向代理
 */
public class ProxyAction implements IAction {
    /**
     * 代理目标地址键
     */
    public final static String CONTEXT_KEY_PROXY = "proxy";


    /**
     * 调用
     *
     * @param visitor 访问者
     * @param context 上下文
     * @return 下一步动作，null表示结束
     */
    @Override
    public String execute(Visitor visitor, Context context) throws Exception {
        HttpClient client = new HttpClient();
        HttpRequest request = new HttpRequest();
        request.method = context.method();
        request.headers = new Table<String, String>();
        for(ILink<String, String> link : context.header()) {
            request.headers.put(link.origin(), link.destination());
        }
        String url = (String) context.get(CONTEXT_KEY_PROXY);
        if(null != context.queryString()) {
            url += "?" + context.queryString();
        }
        if(null != context.form()) {
            if(null != context.request().getContentType() && context.request().getContentType().startsWith("text/")) {
                HttpRawBody body = new HttpRawBody();
                if(context.form().get("").length > 0) {
                    body.content = (String) (context.form().get("")[0]);
                }
                request.body = body;
            }
            else if("application/x-www-form-urlencoded".equalsIgnoreCase(context.request().getContentType())) {
                HttpURLEncodedBody body = new HttpURLEncodedBody();
                for(ILink<String, Object[]> link : context.form()) {
                    body.put(link.origin(), link.destination()[0]);
                }
                request.body = body;
            }
            else if(null != context.request().getContentType() && context.request().getContentType().contains("multipart")) {
                HttpFormBody body = new HttpFormBody();
                for(ILink<String, Object[]> link : context.form()) {
                    body.put(link.origin(), link.destination()[0]);
                }
                request.body = body;
            }
        }
        Table<String, ISet<String>> headers = new Table<String, ISet<String>>();
        String result = client.invoke(url, request, headers, new HttpTextFilter());
        for(ILink<String, ISet<String>> link : headers) {
            Set<String> set = new Set<String>();
            for(String value : link.destination()) {
                // visitor.response().addHeader(link.origin(), value);
            }
        }
        visitor.write(result);
        return null;
    }
}
