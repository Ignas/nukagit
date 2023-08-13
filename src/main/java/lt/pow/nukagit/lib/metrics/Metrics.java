package lt.pow.nukagit.lib.metrics;

import io.opencensus.stats.Aggregation;
import io.opencensus.stats.Measure;
import io.opencensus.stats.Stats;
import io.opencensus.stats.View;

import java.util.List;

public class Metrics {
  public static Measure.MeasureLong registerCounter(String name, String description) {
    var measure = Measure.MeasureLong.create(name, description, "1");
    registerView(measure);
    return measure;
  }

  private static void registerView(Measure.MeasureLong measure) {
    Stats.getViewManager()
        .registerView(
            View.create(
                View.Name.create(measure.getName()),
                measure.getDescription(),
                measure,
                Aggregation.Count.create(),
                List.of()));
  }

  public static void count(Measure.MeasureLong measure) {
    Stats.getStatsRecorder().newMeasureMap().put(measure, 1).record();
  }
}
