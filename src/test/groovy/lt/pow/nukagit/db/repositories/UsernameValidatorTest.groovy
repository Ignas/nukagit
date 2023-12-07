package lt.pow.nukagit.db.repositories

import spock.lang.Shared
import spock.lang.Specification

class UsernameValidatorTest extends Specification {

    @Shared
    def validator = new UsernameValidator()

    def "only valid unix usernames are accepted"(String username) {
        expect:
        validator.isValid(username)
        where:
        username << ["example", "example1", "example_1", "example-1", "foo-bar", "foo_bar", "foo1bar"]
    }

    def "invalid unix usernames are rejected"(String username) {
        expect:
        !validator.isValid(username)
        where:
        // Invalid usernames
        username << [
                "", "3", "3foo", "Wat?", "foo bar", "foo\tbar", "foo\nbar", "foo\rbar", "foo\\bar", "foo/bar",
                "foo*bar", "foo?bar", "foo<bar", "foo>bar", "foo|bar", "foo\"bar", "foo'bar", "foo`bar", "foo;bar",
                "foo&bar", "foo\$bar", "foo#bar", "foo%bar", "foo{bar", "foo}bar", "foo(bar", "foo)bar", "foo[bar",
                "foo]bar", "foo^bar", "foo=bar", "foo+bar", "foo,bar", "foo~bar", "foo@bar", "foo:bar", "foo!bar"]
    }

}
