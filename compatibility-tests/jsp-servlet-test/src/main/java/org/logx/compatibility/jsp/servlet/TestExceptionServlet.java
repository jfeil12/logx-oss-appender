package org.logx.compatibility.jsp.servlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * 测试Servlet，用于生成异常日志消息
 */
@WebServlet("/test-exception")
public class TestExceptionServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(TestExceptionServlet.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            // 模拟一个异常
            throw new RuntimeException("测试异常");
        } catch (Exception e) {
            logger.error("测试错误日志 - 捕获到异常: " + e.toString());
        }

        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.println("<html><body>");
        out.println("<h1>异常日志已生成</h1>");
        out.println("</body></html>");
    }
}
