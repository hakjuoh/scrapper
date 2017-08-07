package oh.hakju.scrapper.dao;

import oh.hakju.scrapper.DAOException;
import oh.hakju.scrapper.entity.Sentence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static java.sql.Statement.RETURN_GENERATED_KEYS;

public class SentenceDAO extends AbstractDAO {

    public Long findMinArticleId() throws DAOException {
        Connection conn = openConnection();

        String statement = "SELECT IFNULL(MIN(article_id), 0) FROM sentence";
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

    public Sentence insert(Sentence entity) throws DAOException {
        Connection conn = openConnection();
        setReadOnly(conn, false);

        String statement = "INSERT INTO sentence (article_id, paragraph_seq, sentence_seq, text) VALUES (?, ?, ?, ?)";
        PreparedStatement ps = null;
        ResultSet rs = null;
        Long entityId = 0L;
        try {
            ps = conn.prepareStatement(statement, RETURN_GENERATED_KEYS);

            int parameterIndex = 1;
            ps.setLong(parameterIndex++, entity.getArticleId());
            ps.setInt(parameterIndex++, entity.getParagraphSeq());
            ps.setInt(parameterIndex++, entity.getSentenceSeq());
            ps.setString(parameterIndex++, entity.getText());
            ps.execute();

            rs = ps.getGeneratedKeys();
            if (rs.next()) {
                entityId = rs.getLong(1);
            }

            conn.commit();

            entity.setSentenceId(entityId);
            return entity;
        } catch (SQLException e) {
            rollback(conn);
            throw new DAOException("'" + statement + "' statement execution is failed.", e);
        } finally {
            closeQuietly(rs, ps, conn);
        }
    }

}
