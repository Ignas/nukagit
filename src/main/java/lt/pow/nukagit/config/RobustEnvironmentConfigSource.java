package lt.pow.nukagit.config;

import org.github.gestalt.config.source.EnvironmentConfigSource;
import org.github.gestalt.config.utils.Pair;

import java.util.ArrayList;
import java.util.List;

public class RobustEnvironmentConfigSource extends EnvironmentConfigSource {
  public RobustEnvironmentConfigSource(String prefix, boolean stripPrefix) {
    super(prefix, stripPrefix);
  }

  @Override
  public List<Pair<String, String>> loadList() {
    List<Pair<String, String>> originalList = super.loadList();
    List<Pair<String, String>> modifiedList = new ArrayList<>(originalList);
    modifiedList.add(new Pair<>("PLEASE_IGNORE_ME", "NONE"));
    return modifiedList;
  }
}
