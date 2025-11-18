<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="org.slf4j.Logger" %>
<%@ page import="org.slf4j.LoggerFactory" %>
<html>
<head>
    <title>JSP日志测试</title>
</head>
<body>
<h1>JSP日志测试</h1>
<%
    Logger logger = LoggerFactory.getLogger("org.logx.compatibility.jsp.servlet.TestJsp");
    
    // 生成不同级别的日志消息
    logger.trace("这是一条TRACE级别的日志消息");
    logger.debug("这是一条DEBUG级别的日志消息");
    logger.info("这是一条INFO级别的日志消息");
    logger.warn("这是一条WARN级别的日志消息");
    logger.error("这是一条ERROR级别的日志消息");
    
    out.println("<p>日志消息已生成</p>");
%>
</body>
</html>