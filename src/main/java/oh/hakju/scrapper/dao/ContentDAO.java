package oh.hakju.scrapper.dao;

import oh.hakju.scrapper.DAOException;

import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;

public class ContentDAO extends AbstractDAO {

    private String hash(String content) {
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
        byte[] digest = md5.digest(content.getBytes());
        Base64.Encoder base64Encoder = Base64.getEncoder();
        return new String(base64Encoder.encode(digest));
    }

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

    public boolean exists(String content) throws DAOException {
        Connection conn = openConnection();

        String statement = "SELECT count(*) cnt FROM content WHERE content_hash = ?";
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(statement);

            int parameterIndex = 1;
            ps.setString(parameterIndex++, hash(content));

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

        String statement = "INSERT INTO content (url, content, content_hash) VALUES (?, ?, ?)";
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(statement);

            int parameterIndex = 1;
            ps.setString(parameterIndex++, url.toString());
            ps.setString(parameterIndex++, content);
            ps.setString(parameterIndex++, hash(content));
            ps.execute();

            conn.commit();
        } catch (SQLException e) {
            rollback(conn);
            throw new DAOException("'" + statement + "' statement execution is failed.", e);
        } finally {
            closeQuietly(ps, conn);
        }
    }

    public void update(URL url, String content) throws DAOException {
        Connection conn = openConnection();
        setReadOnly(conn, false);

        String statement = "UPDATE content SET content = ?, content_hash = ? WHERE URL = ?";
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(statement);

            int parameterIndex = 1;
            ps.setString(parameterIndex++, content);
            ps.setString(parameterIndex++, hash(content));
            ps.setString(parameterIndex++, url.toString());
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
