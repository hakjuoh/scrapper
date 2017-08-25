package oh.hakju.scrapper.dao;

import oh.hakju.scrapper.DAOException;
import oh.hakju.scrapper.entity.Vocabulary;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static java.sql.Statement.RETURN_GENERATED_KEYS;

public class VocabularyDAO extends AbstractDAO {

    public Vocabulary insert(Vocabulary entity) throws DAOException {
        Connection conn = openConnection();
        setReadOnly(conn, false);

        String statement = "INSERT INTO vocabulary (word, pos) VALUES (?, ?)";
        PreparedStatement ps = null;
        ResultSet rs = null;
        Long entityId = 0L;
        try {
            ps = conn.prepareStatement(statement, RETURN_GENERATED_KEYS);

            int parameterIndex = 1;
            ps.setString(parameterIndex++, entity.getWord());
            ps.setString(parameterIndex++, entity.getPos());
            ps.execute();

            rs = ps.getGeneratedKeys();
            if (rs.next()) {
                entityId = rs.getLong(1);
            }

            conn.commit();

            entity.setVocabularyId(entityId);
            return entity;
        } catch (SQLException e) {
            rollback(conn);
            throw new DAOException("'" + statement + "' statement execution is failed.", e);
        } finally {
            closeQuietly(rs, ps, conn);
        }
    }

    public void update(Vocabulary entity) throws DAOException {
        Connection conn = openConnection();
        setReadOnly(conn, false);

        String statement = "UPDATE vocabulary SET word = ?, pos = ? WHERE vocabulary_id = ?";
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(statement, RETURN_GENERATED_KEYS);

            int parameterIndex = 1;
            ps.setString(parameterIndex++, entity.getWord());
            ps.setString(parameterIndex++, entity.getPos());
            ps.setLong(parameterIndex++, entity.getVocabularyId());
            ps.execute();

            conn.commit();
        } catch (SQLException e) {
            rollback(conn);
            throw new DAOException("'" + statement + "' statement execution is failed.", e);
        } finally {
            closeQuietly(rs, ps, conn);
        }
    }

    public List<Vocabulary> findAll() throws DAOException {
        Connection conn = openConnection();

        String statement = "SELECT vocabulary_id, word, pos FROM vocabulary";
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(statement);
            rs = ps.executeQuery();

            List<Vocabulary> entities = new ArrayList();
            while (rs.next()) {
                Vocabulary entity = new Vocabulary();

                Long vocabularyId = rs.getLong("vocabulary_id");
                entity.setVocabularyId(vocabularyId);

                String word = rs.getString("word");
                entity.setWord(word);

                String pos = rs.getString("pos");
                entity.setPos(pos);

                entities.add(entity);
            }

            return entities;
        } catch (SQLException e) {
            throw new DAOException("'" + statement + "' statement execution is failed.", e);
        } finally {
            closeQuietly(rs, ps, conn);
        }
    }

    public Vocabulary findByWordAndPos(String word, String pos) throws DAOException {
        Connection conn = openConnection();

        String statement = "SELECT vocabulary_id FROM vocabulary WHERE word = ? AND pos = ?";
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(statement);

            int parameterIndex = 1;
            ps.setString(parameterIndex++, word);
            ps.setString(parameterIndex++, pos);

            rs = ps.executeQuery();
            if (rs.next()) {
                Vocabulary entity = new Vocabulary();

                Long vocabularyId = rs.getLong("vocabulary_id");
                entity.setVocabularyId(vocabularyId);
                entity.setWord(word);
                entity.setPos(pos);

                return entity;
            } else {
                return null;
            }

        } catch (SQLException e) {
            throw new DAOException("'" + statement + "' statement execution is failed.", e);
        } finally {
            closeQuietly(rs, ps, conn);
        }
    }
}
