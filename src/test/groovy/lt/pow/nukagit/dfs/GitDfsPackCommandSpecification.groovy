package lt.pow.nukagit.dfs

import spock.lang.Specification

class GitDfsPackCommandSpecification extends Specification {
    def "test quoted string parsing"(String input, String[] expected) {
        expect:
        GitDfsPackCommand.extractQuotedStrings(input) == expected
        where:
        input                                               | expected
        "git-upload-pack '/home/git/repositories/test.git'" | ["git-upload-pack", "/home/git/repositories/test.git"]
        "'test.git'"                                        | ["test.git"]
        "'tes''t.git'"                                      | ["test.git"]
        "'tes'\\''t.git'"                                   | ["tes't.git"]
        "'te s''t.gi t'"                                    | ["te st.gi t"]
    }
}
