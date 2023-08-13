package lt.pow.nukagit;

public class Main {
  public static void main(String[] args) {
    var component = DaggerMainComponent.create();
    component.server().run();
  }
}
