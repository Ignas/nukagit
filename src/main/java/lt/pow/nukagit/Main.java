package lt.pow.nukagit;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
    name = "nukagit",
    mixinStandardHelpOptions = true,
    version = "nukagit 0.1",
    description = "GitDFS server")
public class Main implements Runnable {
  static MainComponent component = DaggerMainComponent.create();

  public void run() {
    CommandLine.usage(this, System.out);
  }

  public static void main(String[] args) {
    var commandLine = new CommandLine(new Main());
    component.commands().forEach(commandLine::addSubcommand);
    System.exit(commandLine.execute(args));
  }
}
