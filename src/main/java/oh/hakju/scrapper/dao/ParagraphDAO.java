package oh.hakju.scrapper.dao;

import oh.hakju.scrapper.DAOException;
import oh.hakju.scrapper.entity.Paragraph;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static java.sql.Statement.RETURN_GENERATED_KEYS;

public class ParagraphDAO extends AbstractDAO {

    public List<Paragraph> findParagraph() throws DAOException {
        Connection conn = openConnection();

        String statement = "SELECT paragraph_id, article_id, paragraph_seq, text FROM paragraph";
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(statement);
            rs = ps.executeQuery();

            List<Paragraph> entities = new ArrayList();
            while (rs.next()) {
                Paragraph entity = new Paragraph();
                entity.setParagraphId(rs.getLong("paragraph_id"));
                entity.setArticleId(rs.getLong("article_id"));
                entity.setParagraphSeq(rs.getInt("paragraph_seq"));
                entity.setText(rs.getString("text"));
                entities.add(entity);
            }

            return entities;
        } catch (SQLException e) {
            throw new DAOException("'" + statement + "' statement execution is failed.", e);
        } finally {
            closeQuietly(rs, ps, conn);
        }
    }

    public Long findMaxArticleId() throws DAOException {
        Connection conn = openConnection();

        String statement = "SELECT IFNULL(MAX(article_id), 0) FROM paragraph";
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(statement);
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getLong(1);
            }

            throw new DAOException("Retrieval the result from '" + statement + "' statement is failed.");
        } catch (SQLException e) {
            throw new DAOException("'" + statement + "' statement execution is failed.", e);
        } finally {
            closeQuietly(rs, ps, conn);
        }
    }

    public Paragraph insert(Paragraph entity) throws DAOException {
        Connection conn = openConnection();
        setReadOnly(conn, false);

        String statement = "INSERT INTO paragraph (article_id, paragraph_seq, text) VALUES (?, ?, ?)";
        PreparedStatement ps = null;
        ResultSet rs = null;
        Long entityId = 0L;
        try {
            ps = conn.prepareStatement(statement, RETURN_GENERATED_KEYS);

            int parameterIndex = 1;
            ps.setLong(parameterIndex++, entity.getArticleId());
            ps.setInt(parameterIndex++, entity.getParagraphSeq());
            ps.setString(parameterIndex++, entity.getText());
            ps.execute();

            rs = ps.getGeneratedKeys();
            if (rs.next()) {
                entityId = rs.getLong(1);
            }

            conn.commit();

            entity.setParagraphId(entityId);
            return entity;
        } catch (SQLException e) {
            rollback(conn);
            throw new DAOException("'" + statement + "' statement execution is failed.", e);
        } finally {
            closeQuietly(rs, ps, conn);
        }
    }

}
