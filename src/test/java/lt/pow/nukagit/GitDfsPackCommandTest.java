package lt.pow.nukagit;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class GitDfsPackCommandTest {

  @Test
  void parseQuotedRepositoryName() {
    assertThat(List.of(GitDfsPackCommand.parseQuotedRepositoryName("git-upload-pack '/home/git/repositories/test.git'")))
        .containsExactly("git-upload-pack", "/home/git/repositories/test.git");
    // This seems to be wrong, but I have not been able to confirm it.
    assertThat(List.of(GitDfsPackCommand.parseQuotedRepositoryName("git-upload-pack '/home/git/r'\\''epositories/test.git'")))
        .containsExactly("git-upload-pack", "/home/git/r'''epositories/test.git");
    // I think git would strip the inner quotes too
    assertThat(List.of(GitDfsPackCommand.parseQuotedRepositoryName("git-upload-pack '/home/git/''repositories/test.git'")))
        .containsExactly("git-upload-pack", "/home/git/''repositories/test.git");
    assertThat(List.of(GitDfsPackCommand.parseQuotedRepositoryName("git-upload-pack '/home/git/re\"positories/test.git'")))
        .containsExactly("git-upload-pack", "/home/git/re\"positories/test.git");
  }
}
