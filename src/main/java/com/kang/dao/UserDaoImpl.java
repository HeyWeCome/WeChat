package com.kang.dao;

import com.kang.bean.ServerUser;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * PersonDao的具体实现类
 * PreparedStatement是预编译的,对于批量处理可以大大提高效率. 也叫JDBC存储过程
 * @author heywecome
 */
public class UserDaoImpl implements UserDao {

    private UserDaoImpl() {
        super();
    }
    private static UserDaoImpl userDao;

    // 同一时刻只有一个方法可以进入到临界区，同时它还可以保证共享变量的内存可见性
    public static UserDaoImpl getInstance(){
        if(userDao == null){
            synchronized (UserDaoImpl.class){
                if(userDao==null){
                    userDao = new UserDaoImpl();
                }
            }
        }
        return userDao;
    }

    /**
     * 实现添加用户方法
     */
    @Override
    public void add(ServerUser person) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        String sql = "insert into ChatUser(id,name,password)values(?,?,?)";
        try{
            conn = DBUtils.getConnection();
            System.out.println("get connect");
            ps = conn.prepareStatement(sql);
            ps.setInt(1, person.getId());
            ps.setString(2, person.getUserName());
            ps.setString(3, person.getPassword());
            ps.executeUpdate();
        }catch(SQLException e){
            e.printStackTrace();
            System.out.println("add error");
            throw new SQLException("添加数据失败");
        }finally{
            // 关闭连接
            DBUtils.close(null, ps, conn);
        }
    }

    /**
     * 更新用户方法
     */
    @Override
    public void update(ServerUser person) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        String sql = "update ChatUser set name=?,age=?,password=? where id=?";

        try{
            conn = DBUtils.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, person.getId());
            ps.setString(2, person.getUserName());
            ps.setString(3, person.getPassword());
            ps.executeUpdate();
        }catch(SQLException e){
            e.printStackTrace();
            throw new SQLException("更新数据失败");
        }finally{
            // 关闭连接
            DBUtils.close(null, ps, conn);
        }
    }

    /**
     * 删除方法
     */
    @Override
    public void delete(int id) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        String sql = "delete from ChatUser where id=?";
        try{
            conn = DBUtils.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1,id);
            ps.executeUpdate();
        }catch(SQLException e){
            e.printStackTrace();
            throw new SQLException(" 删除数据失败");
        }finally{
            DBUtils.close(null, ps, conn);
        }
    }

    /**
     * 根据ID查询一个对象
     */
    @Override
    public ServerUser findById(int id) throws SQLException {
        Connection conn = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;         // 查询的结果
        ServerUser serverUser = null;       // 用户
        String sql = "select name,password from ChatUser where id=?";

        try{
            conn = DBUtils.getConnection();
            preparedStatement = conn.prepareStatement(sql);
            preparedStatement.setInt(1, id);    // 设置ID
            resultSet = preparedStatement.executeQuery();       // 执行查询之后的结果

            if(resultSet.next()){
                serverUser = new ServerUser();
                serverUser.setId(id);
                serverUser.setUserName(resultSet.getString(1));
                serverUser.setPassword(resultSet.getString(2));
            }
        }catch(SQLException e){
            e.printStackTrace();
            System.out.println("根据ID查询数据失败");
        }finally{
            DBUtils.close(resultSet, preparedStatement, conn);
        }
        return serverUser;
    }

    /**
     * 根据姓名查找用户
     * @param username
     * @return
     * @throws SQLException
     */
    @Override
    public ServerUser findByName(String username) throws SQLException {
        Connection conn = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        ServerUser serverUser = null;
        String sql = "select id,password from ChatUser where name=?";

        try{
            conn = DBUtils.getConnection();
            preparedStatement = conn.prepareStatement(sql);
            preparedStatement.setString(1,username );
            resultSet = preparedStatement.executeQuery();
            if(resultSet.next()){
                serverUser = new ServerUser(resultSet.getInt(1),username,resultSet.getString(2));
            }
        }catch(SQLException e){
            e.printStackTrace();
            throw new SQLException("根据ID查询数据失败");
        }finally{
            DBUtils.close(resultSet, preparedStatement, conn);
        }
        return serverUser;
    }

    /**
     * 查询数据库中的所有用户
     */
    @Override
    public List<ServerUser> findAll() throws SQLException {
        Connection conn = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        ServerUser person = null;
        List<ServerUser> people = new ArrayList<ServerUser>();
        String sql = "select id,name,password from ChatUser";

        try{
            conn = DBUtils.getConnection();
            preparedStatement = conn.prepareStatement(sql);
            resultSet = preparedStatement.executeQuery();
            while(resultSet.next()){
                person = new ServerUser(resultSet.getInt(1),resultSet.getString(2),resultSet.getString(3));
                people.add(person);
            }
        }catch(SQLException e){
            e.printStackTrace();
        }finally{
            DBUtils.close(resultSet, preparedStatement, conn);
        }
        return people;
    }

}
