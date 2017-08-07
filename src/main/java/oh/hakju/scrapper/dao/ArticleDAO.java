package oh.hakju.scrapper.dao;

import oh.hakju.scrapper.DAOException;
import oh.hakju.scrapper.entity.Article;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static java.sql.Statement.RETURN_GENERATED_KEYS;

public class ArticleDAO extends AbstractDAO {

    public Article insert(Article entity) throws DAOException {
        Connection conn = openConnection();
        setReadOnly(conn, false);

        String statement = "INSERT INTO article (content_id, headline, text) VALUES (?, ?, ?)";
        PreparedStatement ps = null;
        ResultSet rs = null;
        Long entityId = 0L;
        try {
            ps = conn.prepareStatement(statement, RETURN_GENERATED_KEYS);

            int parameterIndex = 1;
            ps.setLong(parameterIndex++, entity.getContentId());
            ps.setString(parameterIndex++, entity.getHeadline());
            ps.setString(parameterIndex++, entity.getText());
            ps.execute();

            rs = ps.getGeneratedKeys();
            if (rs.next()) {
                entityId = rs.getLong(1);
            }

            conn.commit();

            entity.setArticleId(entityId);
            return entity;
        } catch (SQLException e) {
            rollback(conn);
            throw new DAOException("'" + statement + "' statement execution is failed.", e);
        } finally {
            closeQuietly(rs, ps, conn);
        }
    }
}
