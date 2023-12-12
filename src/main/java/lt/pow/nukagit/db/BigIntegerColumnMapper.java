package lt.pow.nukagit.db;

import org.jdbi.v3.core.statement.StatementContext;

import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.SQLException;

public class BigIntegerColumnMapper implements org.jdbi.v3.core.mapper.ColumnMapper<BigInteger> {
    @Override
    public BigInteger map(ResultSet r, int columnNumber, StatementContext ctx) throws SQLException {
        if (r.getBytes(columnNumber) == null) {
            return null;
        }
        return new BigInteger(r.getBytes(columnNumber));
    }
}
