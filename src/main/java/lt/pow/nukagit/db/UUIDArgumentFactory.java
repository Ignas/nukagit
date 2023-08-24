package lt.pow.nukagit.db;

import java.sql.Types;
import java.util.UUID;
import org.jdbi.v3.core.argument.AbstractArgumentFactory;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.config.ConfigRegistry;

class UUIDArgumentFactory extends AbstractArgumentFactory<UUID> {
  UUIDArgumentFactory() {
    super(Types.VARCHAR);
  }

  @Override
  protected Argument build(UUID value, ConfigRegistry config) {
    return (position, statement, ctx) -> statement.setString(position, value.toString());
  }
}
