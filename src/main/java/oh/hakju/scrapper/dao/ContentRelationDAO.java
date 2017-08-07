package oh.hakju.scrapper.dao;

import oh.hakju.scrapper.DAOException;
import oh.hakju.scrapper.entity.ContentRelation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static java.sql.Statement.RETURN_GENERATED_KEYS;

public class ContentRelationDAO extends AbstractDAO {

    public ContentRelation insert(ContentRelation entity) throws DAOException {
        Connection conn = openConnection();
        setReadOnly(conn, false);

        String statement = "INSERT INTO content_relation (source_content_id, target_content_id, link_text) VALUES (?, ?, ?)";
        PreparedStatement ps = null;
        ResultSet rs = null;
        Long entityId = 0L;
        try {
            ps = conn.prepareStatement(statement, RETURN_GENERATED_KEYS);

            int parameterIndex = 1;
            ps.setLong(parameterIndex++, entity.getSourceContentId());
            ps.setLong(parameterIndex++, entity.getTargetContentId());
            ps.setString(parameterIndex++, entity.getLinkText());
            ps.execute();

            rs = ps.getGeneratedKeys();
            if (rs.next()) {
                entityId = rs.getLong(1);
            }

            conn.commit();

            entity.setContentRelationId(entityId);
            return entity;
        } catch (SQLException e) {
            rollback(conn);
            throw new DAOException("'" + statement + "' statement execution is failed.", e);
        } finally {
            closeQuietly(rs, ps, conn);
        }
    }
}
