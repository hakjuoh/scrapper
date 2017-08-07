package oh.hakju.scrapper.dao;

import oh.hakju.scrapper.DAOException;
import oh.hakju.scrapper.entity.Content;

import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;

import static java.sql.Statement.RETURN_GENERATED_KEYS;

public class ContentDAO extends AbstractDAO {

    private String toString(URL u) {
        // pre-compute length of StringBuffer
        int len = u.getProtocol().length() + 1;
        if (u.getAuthority() != null && u.getAuthority().length() > 0)
            len += 2 + u.getAuthority().length();
        if (u.getPath() != null) {
            len += u.getPath().length();
        }
        if (u.getQuery() != null) {
            len += 1 + u.getQuery().length();
        }
        if (u.getRef() != null)
            len += 1 + u.getRef().length();

        StringBuffer result = new StringBuffer(len);
        result.append(u.getProtocol());
        result.append(":");
        if (u.getAuthority() != null && u.getAuthority().length() > 0) {
            result.append("//");
            result.append(u.getAuthority());
        }
        if (u.getPath() != null) {
            result.append(u.getPath());
        }
        return result.toString();
    }

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

    public Content findByUrl(URL url) throws DAOException {
        Connection conn = openConnection();

        String statement = "SELECT content_id, content FROM content WHERE url = ?";
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(statement);

            int parameterIndex = 1;
            ps.setString(parameterIndex++, toString(url));

            rs = ps.executeQuery();
            if (rs.next()) {
                Content entity = new Content();

                Long contentId = rs.getLong("content_id");
                entity.setContentId(contentId);
                String content = rs.getString("content");
                entity.setContent(content);
                entity.setUrl(toString(url));

                return entity;
            } else {
                return null;
            }

        } catch (SQLException e) {
            rollback(conn);
            throw new DAOException("'" + statement + "' statement execution is failed.", e);
        } finally {
            closeQuietly(rs, ps, conn);
        }
    }

    public boolean exists(URL url) throws DAOException {
        Connection conn = openConnection();

        String statement = "SELECT count(*) cnt FROM content WHERE url = ?";
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(statement);

            int parameterIndex = 1;
            ps.setString(parameterIndex++, toString(url));

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

    public Content insert(Content.ContentType contentType, URL url, String content) throws DAOException {
        Content entity = new Content();
        entity.setContentType(contentType);
        entity.setContent(content);
        entity.setUrl(toString(url));
        return insert(entity);
    }

    public Content insert(Content content) throws DAOException {
        Connection conn = openConnection();
        setReadOnly(conn, false);

        String statement = "INSERT INTO content (content_type, url, content, content_hash) VALUES (?, ?, ?, ?)";
        PreparedStatement ps = null;
        ResultSet rs = null;
        Long entityId = 0L;
        try {
            ps = conn.prepareStatement(statement, RETURN_GENERATED_KEYS);

            int parameterIndex = 1;
            ps.setString(parameterIndex++, content.getContentType().toString());
            ps.setString(parameterIndex++, content.getUrl());
            ps.setString(parameterIndex++, content.getContent());
            ps.setString(parameterIndex++, hash(content.getContent()));
            ps.execute();

            rs = ps.getGeneratedKeys();
            if (rs.next()) {
                entityId = rs.getLong(1);
            }

            conn.commit();

            content.setContentId(entityId);
            return content;
        } catch (SQLException e) {
            rollback(conn);
            throw new DAOException("'" + statement + "' statement execution is failed.", e);
        } finally {
            closeQuietly(rs, ps, conn);
        }
    }

    public Content update(URL url, String content) throws DAOException {
        Content entity = findByUrl(url);
        if (entity == null) {
            return entity;
        }

        entity.setContent(content);
        update(entity);
        return entity;
    }

    public void update(Content content) throws DAOException {
        Connection conn = openConnection();
        setReadOnly(conn, false);

        String statement = "UPDATE content SET content = ?, content_hash = ? WHERE content_id = ?";
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(statement);

            int parameterIndex = 1;
            ps.setString(parameterIndex++, content.getContent());
            ps.setString(parameterIndex++, hash(content.getContent()));
            ps.setLong(parameterIndex++, content.getContentId());
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
