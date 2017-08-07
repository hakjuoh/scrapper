package oh.hakju.scrapper.dao;

import oh.hakju.scrapper.DAOException;
import oh.hakju.scrapper.entity.ContentCategory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static java.sql.Statement.RETURN_GENERATED_KEYS;

public class ContentCategoryDAO extends AbstractDAO {

    public ContentCategory insert(ContentCategory entity) throws DAOException {
        Connection conn = openConnection();
        setReadOnly(conn, false);

        String statement = "INSERT INTO content_category (content_id, top_category, sub_category) VALUES (?, ?, ?)";
        PreparedStatement ps = null;
        ResultSet rs = null;
        Long entityId = 0L;
        try {
            ps = conn.prepareStatement(statement, RETURN_GENERATED_KEYS);

            int parameterIndex = 1;
            ps.setLong(parameterIndex++, entity.getContentId());
            ps.setString(parameterIndex++, entity.getTopCategory());
            ps.setString(parameterIndex++, entity.getSubCategory());
            ps.execute();

            rs = ps.getGeneratedKeys();
            if (rs.next()) {
                entityId = rs.getLong(1);
            }

            conn.commit();

            entity.setCategoryId(entityId);
            return entity;
        } catch (SQLException e) {
            rollback(conn);
            throw new DAOException("'" + statement + "' statement execution is failed.", e);
        } finally {
            closeQuietly(rs, ps, conn);
        }
    }
}
