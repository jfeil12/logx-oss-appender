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
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 测试Servlet，用于生成各种级别的业务日志消息
 */
@WebServlet("/test-log")
public class TestLogServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(TestLogServlet.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String level = request.getParameter("level");
        String countParam = request.getParameter("count");
        String category = request.getParameter("category");

        if (level == null) {
            level = "all";
        }
        int count = 10;
        if (countParam != null) {
            try {
                count = Integer.parseInt(countParam);
            } catch (NumberFormatException e) {
                count = 10;
            }
        }
        if (category == null) {
            category = "general";
        }

        String sessionId = UUID.randomUUID().toString().substring(0, 8);
        String userAgent = request.getHeader("User-Agent");
        String remoteAddr = request.getRemoteAddr();

        logger.info("[{}] Servlet业务日志开始 - 客户端: {}, IP: {}, 级别: {}, 数量: {}, 分类: {}",
                   sessionId, userAgent, remoteAddr, level, count, category);

        long startTime = System.currentTimeMillis();

        // 生成业务日志
        for (int i = 1; i <= count; i++) {
            String requestId = UUID.randomUUID().toString().substring(0, 8);

            if ("all".equals(level) || "trace".equals(level)) {
                logger.trace("[{}][{}] Servlet跟踪 - 请求处理第{}步: 参数解析完成", sessionId, requestId, i);
            }

            if ("all".equals(level) || "debug".equals(level)) {
                logger.debug("[{}][{}] Servlet调试 - 处理请求{}: URI={}, 方法={}, 参数数量={}",
                           sessionId, requestId, i, request.getRequestURI(), request.getMethod(),
                           request.getParameterMap().size());
            }

            if ("all".equals(level) || "info".equals(level)) {
                // 模拟电商业务日志
                if ("order".equals(category)) {
                    logger.info("[{}][{}] 订单业务 - 订单查询: 订单号=ORD{}, 用户ID={}, 商品数量={}, 总金额={}.{}元",
                              sessionId, requestId, String.format("%06d", ThreadLocalRandom.current().nextInt(100000, 999999)),
                              ThreadLocalRandom.current().nextInt(1000, 9999),
                              ThreadLocalRandom.current().nextInt(1, 10),
                              ThreadLocalRandom.current().nextInt(10, 999),
                              ThreadLocalRandom.current().nextInt(10, 99));
                } else if ("security".equals(category)) {
                    logger.info("[{}][{}] 安全审计 - 用户登录: 用户名=user{}, IP={}, 登录时间={}, 状态={}",
                              sessionId, requestId, ThreadLocalRandom.current().nextInt(1000, 9999),
                              remoteAddr, LocalDateTime.now(),
                              ThreadLocalRandom.current().nextBoolean() ? "成功" : "失败");
                } else {
                    logger.info("[{}][{}] 业务操作 - 数据查询: 表名=tb_product, 查询条件=id > {}, 返回记录数={}, 耗时={}ms",
                              sessionId, requestId, ThreadLocalRandom.current().nextInt(1000, 9999),
                              ThreadLocalRandom.current().nextInt(1, 100),
                              ThreadLocalRandom.current().nextInt(10, 200));
                }

                // 性能监控日志
                logger.info("[{}][{}] 性能监控 - Servlet容器状态: 活跃线程={}, 内存使用={}MB, 响应时间={}ms",
                          sessionId, requestId, ThreadLocalRandom.current().nextInt(10, 50),
                          ThreadLocalRandom.current().nextInt(100, 800),
                          ThreadLocalRandom.current().nextInt(5, 100));
            }

            if ("all".equals(level) || "warn".equals(level)) {
                if (i % 4 == 0) {
                    logger.warn("[{}][{}] 性能警告 - Servlet响应慢: 处理时间{}ms超过阈值50ms, 建议优化数据库查询",
                              sessionId, requestId, ThreadLocalRandom.current().nextInt(50, 200));
                }
                if (i % 6 == 0) {
                    logger.warn("[{}][{}] 业务警告 - 库存预警: 商品ID={}, 当前库存={}, 安全库存={}",
                              sessionId, requestId, ThreadLocalRandom.current().nextInt(1000, 9999),
                              ThreadLocalRandom.current().nextInt(1, 20),
                              ThreadLocalRandom.current().nextInt(50, 100));
                }
            }

            if ("all".equals(level) || "error".equals(level)) {
                if (i % 7 == 0) {
                    logger.error("[{}][{}] 业务异常 - 支付失败: 订单号=ORD{}, 支付金额={}.{}元, 错误码={}, 错误信息={}",
                               sessionId, requestId, String.format("%06d", ThreadLocalRandom.current().nextInt(100000, 999999)),
                               ThreadLocalRandom.current().nextInt(10, 999),
                               ThreadLocalRandom.current().nextInt(10, 99),
                               ThreadLocalRandom.current().nextInt(1000, 9999),
                               "第三方支付接口超时");
                }
                if (i % 8 == 0) {
                    logger.error("[{}][{}] 系统异常 - 数据库连接失败: 连接池={}, 活跃连接数={}, 最大连接数={}",
                               sessionId, requestId, "main-pool",
                               ThreadLocalRandom.current().nextInt(80, 100),
                               ThreadLocalRandom.current().nextInt(100, 120));
                }
            }

            // 模拟处理延迟
            try {
                Thread.sleep(ThreadLocalRandom.current().nextInt(1, 5));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        long endTime = System.currentTimeMillis();
        logger.info("[{}] Servlet业务日志完成 - 总耗时: {}ms, 生成日志数量: 约{}条, 分类: {}",
                   sessionId, endTime - startTime, count * 3, category);

        // 返回HTML响应
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.println("<!DOCTYPE html>");
        out.println("<html><head><title>日志生成结果</title></head><body>");
        out.println("<h1>业务日志生成完成</h1>");
        out.println("<p><strong>会话ID:</strong> " + sessionId + "</p>");
        out.println("<p><strong>日志级别:</strong> " + level + "</p>");
        out.println("<p><strong>生成数量:</strong> " + count + " 组</p>");
        out.println("<p><strong>业务分类:</strong> " + category + "</p>");
        out.println("<p><strong>总耗时:</strong> " + (endTime - startTime) + " ms</p>");
        out.println("<p><em>日志已异步上传到MinIO，请检查OSS控制台确认上传情况</em></p>");
        out.println("</body></html>");
    }
}