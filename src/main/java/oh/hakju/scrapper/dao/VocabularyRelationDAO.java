package oh.hakju.scrapper.dao;

import oh.hakju.scrapper.DAOException;
import oh.hakju.scrapper.entity.Paragraph;
import oh.hakju.scrapper.entity.Vocabulary;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static java.sql.Statement.RETURN_GENERATED_KEYS;

public class VocabularyRelationDAO extends AbstractDAO {

    public void insertVocabularyRelation(Paragraph paragraph, Vocabulary vocabulary) {
        Connection conn = openConnection();
        setReadOnly(conn, false);

        String statement = "INSERT INTO vocabulary_relation (vocabulary_id, paragraph_id) VALUES (?, ?)";
        PreparedStatement ps = null;
        ResultSet rs = null;
        Long entityId = 0L;
        try {
            ps = conn.prepareStatement(statement, RETURN_GENERATED_KEYS);

            int parameterIndex = 1;
            ps.setLong(parameterIndex++, vocabulary.getVocabularyId());
            ps.setLong(parameterIndex++, paragraph.getParagraphId());
            ps.execute();

            conn.commit();
        } catch (SQLException e) {
            rollback(conn);
            throw new DAOException("'" + statement + "' statement execution is failed.", e);
        } finally {
            closeQuietly(rs, ps, conn);
        }
    }
}
