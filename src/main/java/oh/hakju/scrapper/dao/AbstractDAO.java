package oh.hakju.scrapper.dao;

import oh.hakju.scrapper.DAOException;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public abstract class AbstractDAO {

    static {
        try {
            DriverManager.registerDriver(new com.mysql.jdbc.Driver());
        } catch (SQLException e) {
            throw new RuntimeException("Can't register driver!");
        }
    }

    Connection openConnection() throws DAOException {
        String url = "jdbc:mysql://docker:3306/scrapper?useSSL=false";
        String username = "oagi";
        String password = "oagi";

        Connection conn;
        try {
            conn = DriverManager.getConnection(url, username, password);
        } catch (SQLException e) {
            throw new DAOException("Can't open new connection.", e);
        }

        try {
            setAutoCommit(conn, false);
            setReadOnly(conn, true);
        } catch (DAOException e) {
            closeQuietly(conn);
            throw e;
        }

        return conn;
    }

    void setAutoCommit(Connection conn, boolean autoCommit) {
        try {
            conn.setAutoCommit(autoCommit);
        } catch (SQLException e) {
            throw new DAOException("Failed to set auto-commit parameter to " + autoCommit, e);
        }
    }

    void setReadOnly(Connection conn, boolean readOnly) {
        try {
            conn.setReadOnly(readOnly);
        } catch (SQLException e) {
            throw new DAOException("Failed to set read-only parameter to " + readOnly, e);
        }
    }

    void rollback(Connection conn) {
        try {
            conn.rollback();
        } catch (SQLException e) {
            throw new DAOException("Failed a roll-back of the transaction", e);
        }
    }

    void closeQuietly(Object... objs) {
        for (Object obj : objs) {
            closeQuietly(obj);
        }
    }

    void closeQuietly(Object object) {
        if (object == null) {
            return;
        }

        Class<?> clazz = object.getClass();
        Method closeMethod;
        try {
            closeMethod = clazz.getMethod("close", new Class[0]);
        } catch (NoSuchMethodException e) {
            return;
        }

        try {
            closeMethod.invoke(object, new Object[0]);
        } catch (Exception ignore) {
        }
    }
}
