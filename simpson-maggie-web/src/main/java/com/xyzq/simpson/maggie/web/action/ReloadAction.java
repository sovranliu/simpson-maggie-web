package com.xyzq.simpson.maggie.web.action;

import com.xyzq.simpson.base.io.net.http.AjaxResult;
import com.xyzq.simpson.maggie.framework.action.core.IAction;
import com.xyzq.simpson.maggie.framework.Context;
import com.xyzq.simpson.maggie.framework.Visitor;
import com.xyzq.simpson.maggie.component.service.ConsoleService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 重载动作
 */
public class ReloadAction implements IAction {
    @Autowired
    private ConsoleService maggieConsoleService;


    /**
     * 调用
     *
     * @param visitor 访问者
     * @param context 上下文
     * @return 下一步动作，不包括后缀名称，null表示结束
     */
    @Override
    public String execute(Visitor visitor, Context context) throws Exception {
        String path = (String) context.parameter("path");
        visitor.setContentType("application/json");
        if(maggieConsoleService.reload(path)) {
            visitor.write((new AjaxResult(0, null, null)).toString());
        }
        else {
            visitor.write((new AjaxResult(-1, "内部错误", null)).toString());
        }
        return null;
    }
}
