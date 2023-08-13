package lt.pow.nukagit;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class GitDfsPackCommandTest {

  private static Stream<Arguments> quotedCommandProvider() {
    return Stream.of(
        Arguments.of(
            "git-upload-pack '/home/git/repositories/test.git'",
            List.of("git-upload-pack", "/home/git/repositories/test.git")),
        Arguments.of("'test.git'", List.of("test.git")),
        Arguments.of("'tes''t.git'", List.of("test.git")),
        Arguments.of("'tes'\\''t.git'", List.of("tes't.git")),
        Arguments.of("'te s''t.gi t'", List.of("te st.gi t")));
  }

  @ParameterizedTest
  @MethodSource("quotedCommandProvider")
  void extractQuotedStrings(String input, List<String> expected) {
    assertThat(List.of(GitDfsPackCommand.extractQuotedStrings(input))).containsExactlyElementsIn(expected);
  }
}
