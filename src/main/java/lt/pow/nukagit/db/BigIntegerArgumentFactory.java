package lt.pow.nukagit.db;

import org.jdbi.v3.core.argument.AbstractArgumentFactory;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.config.ConfigRegistry;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.sql.Types;

class BigIntegerArgumentFactory extends AbstractArgumentFactory<BigInteger> {
  BigIntegerArgumentFactory() {
    super(Types.BINARY);
  }

  @Override
  protected Argument build(BigInteger value, ConfigRegistry config) {
    return (position, statement, ctx) -> statement.setBinaryStream(position, new ByteArrayInputStream(value.toByteArray()));
  }
}
