package com.xyzq.simpson.maggie.web.action;

import com.xyzq.simpson.maggie.framework.action.core.IAction;
import com.xyzq.simpson.maggie.framework.Context;
import com.xyzq.simpson.maggie.framework.Visitor;
import com.xyzq.simpson.maggie.component.service.ConsoleService;
import com.xyzq.simpson.base.io.net.http.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 删除动作
 */
public class DeleteAction implements IAction {
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
        if(null == path) {
            visitor.setContentType("application/json");
            visitor.write((new AjaxResult(-2, "参数丢失", null)).toString());
            return null;
        }
        visitor.setContentType("application/json");
        if(maggieConsoleService.delete(path)) {
            visitor.write((new AjaxResult(0, null, null)).toString());
        }
        else {
            visitor.write((new AjaxResult(-1, "内部错误", null)).toString());
        }
        return null;
    }
}
