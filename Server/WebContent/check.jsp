<%@ page import="java.sql.*" language="java" contentType="text/html; charset=utf-8"
    pageEncoding="utf-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8">
<title>Insert title here</title>
</head>
<body>
<jsp:useBean id="db" class="Bean.DBBean" scope="page"/>
<%
    request.setCharacterEncoding("UTF-8");
    String username=(String)request.getParameter("username");//获取login页面输入的用户名和密码
    String password=(String)request.getParameter("password");


    String sql="select * from logindb where username="+"'"+username+"'";//定义一个查询语句
    ResultSet rs=db.executeQuery(sql);//执行查询语句
    if(rs.next())
    {
        //将输入的密码与数据库密码相比对，执行相应操作
        if(password.equals(rs.getObject("pwd"))){
        	int id = Integer.parseInt(rs.getObject("id").toString());
        	String temp = "success.jsp";
        	session.setAttribute("Id",id);  
        	session.setAttribute("Username",username);  
            response.sendRedirect(temp);
        }
        else{
            out.print("<script language='javaScript'> alert('Wrong Password');</script>");
            response.setHeader("refresh", "0;url=login.jsp");
        }
    }
    else 
    {
        out.print("<script language='javaScript'> alert('Username does not exist. Please try again.');</script>");
        response.setHeader("refresh", "0;url=login.jsp");
    }

%>
</body>
</html>