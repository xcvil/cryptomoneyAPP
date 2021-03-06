package Bean;
import java.sql.*;
public class DBBean {
    private String driverStr = "com.mysql.jdbc.Driver";
    private String connStr = "jdbc:mysql://127.0.0.1:3306/test";
    private String dbusername = "user1";
    private String dbpassword = "User1234!";
    private Connection conn = null;
    private Statement stmt = null;

    public DBBean()
    {
        try
        {
            Class.forName(driverStr);
            conn = DriverManager.getConnection(connStr, dbusername, dbpassword);
            stmt = conn.createStatement();
        } 
        catch (Exception ex) {
            System.out.println(ex.getMessage());
            System.out.println("connection fail");
        } 

    }

    public int executeUpdate(String s) {
        int result = 0;
        System.out.println("--�������:"+s+"\n");
        try {
            result = stmt.executeUpdate(s);
        } catch (Exception ex) {
            System.out.println("ִ�и��´���");
        }
        return result;
    }

    public ResultSet executeQuery(String s) {
        ResultSet rs = null;
        System.out.print("--��ѯ���:"+s+"\n");
        try {
            rs = stmt.executeQuery(s);
        } catch (Exception ex) {
            System.out.println("ִ�в�ѯ����");
        }
        return rs;
    }
    public void execQuery(String s){
        try {
            stmt.executeUpdate(s);
        } catch (SQLException e) {

            System.out.println("ִ�в������");
        }
    }

    public void close() {
        try {
            stmt.close();
            conn.close();
        } catch (Exception e) {
        }
    }
}