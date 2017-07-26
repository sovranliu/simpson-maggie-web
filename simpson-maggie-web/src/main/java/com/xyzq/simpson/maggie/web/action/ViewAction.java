package com.xyzq.simpson.maggie.web.action;

import com.xyzq.simpson.maggie.framework.action.core.IAction;
import com.xyzq.simpson.maggie.framework.Context;
import com.xyzq.simpson.maggie.framework.Visitor;
import com.xyzq.simpson.maggie.component.service.ConsoleService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 查看动作内容
 */
public class ViewAction implements IAction {
    @Autowired
    private ConsoleService maggieConsoleService;


    /**
     * 调用
     *
     * @param visitor 访问者
     * @param context 上下文
     * @return 下一步动作，null表示结束
     */
    public String execute(Visitor visitor, Context context) throws Exception {
        String path = (String) context.parameter("path");
        visitor.setContentType("text/plain");
        String content = maggieConsoleService.view(path);
        visitor.write(content);
        return null;
    }
}
