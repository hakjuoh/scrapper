package oh.hakju.scrapper.dao;

import oh.hakju.scrapper.DAOException;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ContentDAO extends AbstractDAO {

    public boolean exists(URL url) throws DAOException {
        Connection conn = openConnection();

        String statement = "SELECT count(*) cnt FROM content WHERE url = ?";
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(statement);

            int parameterIndex = 1;
            ps.setString(parameterIndex++, url.toString());

            rs = ps.executeQuery();
            int cnt = 0;
            if (rs.next()) {
                cnt = rs.getInt("cnt");
            }

            return (cnt > 0) ? true : false;
        } catch (SQLException e) {
            rollback(conn);
            throw new DAOException("'" + statement + "' statement execution is failed.", e);
        } finally {
            closeQuietly(rs, ps, conn);
        }
    }

    public void insert(URL url, String content) throws DAOException {
        Connection conn = openConnection();
        setReadOnly(conn, false);

        String statement = "INSERT INTO content (url, content) VALUES (?, ?)";
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(statement);

            int parameterIndex = 1;
            ps.setString(parameterIndex++, url.toString());
            ps.setString(parameterIndex++, content);
            ps.execute();

            conn.commit();
        } catch (SQLException e) {
            rollback(conn);
            throw new DAOException("'" + statement + "' statement execution is failed.", e);
        } finally {
            closeQuietly(ps, conn);
        }
    }
}
