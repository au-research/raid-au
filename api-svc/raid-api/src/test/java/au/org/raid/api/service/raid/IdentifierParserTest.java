package au.org.raid.api.service.raid;

import au.org.raid.api.service.raid.id.IdentifierHandle;
import au.org.raid.api.service.raid.id.IdentifierParser;
import au.org.raid.api.service.raid.id.IdentifierUrl;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.fail;

public class IdentifierParserTest {

    private IdentifierParser parser = new IdentifierParser();

    @Test
    public void validHandlePathsShouldPass() {

        parser.parseHandle("/prefix/suffix",
                handle -> assertThat(handle).isEqualTo(
                        new IdentifierHandle("prefix", "suffix")),
                error -> fail(String.join(", ", error.getProblems())));

        parser.parseHandle("prefix/suffix",
                handle -> assertThat(handle).isEqualTo(
                        new IdentifierHandle("prefix", "suffix")),
                error -> fail(String.join(", ", error.getProblems())));

        parser.parseHandle("prefix/suffix/description",
                handle -> assertThat(handle).isEqualTo(
                        new IdentifierHandle("prefix", "suffix", "description")),
                error -> fail(String.join(", ", error.getProblems())));

        parser.parseHandle("prefix/suffix/description/extra",
                handle -> assertThat(handle).isEqualTo(
                        new IdentifierHandle("prefix", "suffix", "description/extra")),
                error -> fail(String.join(", ", error.getProblems())));

        parser.parseHandle("prefix/suffix/description-extra",
                handle -> assertThat(handle).isEqualTo(
                        new IdentifierHandle("prefix", "suffix", "description-extra")),
                error -> fail(String.join(", ", error.getProblems())));

        parser.parseHandle("prefix/suffix/description-extra?param=value",
                handle -> assertThat(handle).isEqualTo(
                        new IdentifierHandle("prefix", "suffix", "description-extra?param=value")),
                error -> fail(String.join(", ", error.getProblems())));
    }

    @Test
    public void inValidHandlePathsShouldFail() {
        parser.parseHandle("/prefix",
                handle -> fail("parsing should have failed: " + handle),
                error -> assertThat(error.getProblems()).
                        anySatisfy(i -> assertThat(i).contains("not enough parts"))
        );

        parser.parseHandle("prefix",
                handle -> fail("parsing should have failed: " + handle),
                error -> assertThat(error.getProblems()).
                        anySatisfy(i -> assertThat(i).contains("not enough parts"))
        );

        parser.parseHandle("/",
                handle -> fail("parsing should have failed: " + handle),
                error -> assertThat(error.getProblems()).
                        anySatisfy(i -> assertThat(i).contains("not enough parts"))
        );

        parser.parseHandle("",
                handle -> fail("parsing should have failed: " + handle),
                error -> assertThat(error.getProblems()).
                        anySatisfy(i -> assertThat(i).contains("may not be blank"))
        );

        parser.parseHandle(" ",
                handle -> fail("parsing should have failed: " + handle),
                error -> assertThat(error.getProblems()).
                        anySatisfy(i -> assertThat(i).contains("may not be blank"))
        );

        parser.parseHandle(null,
                handle -> fail("parsing should have failed: " + handle),
                error -> assertThat(error.getProblems()).
                        anySatisfy(i -> assertThat(i).contains("may not be blank"))
        );
    }

    /* DOES is stuff that happens given the current implementation, but not
    actually sure if it SHOULD.

    I dislike that these are valid encodings that we fail to parse.
    But I didn't want to add redundant decode() call.
    */
    @Test
    public void validHandlePathsDoesFail() {
        /*  */
        parser.parseHandle("/prefix%2Fsuffix",
                handle -> fail("parsing should have failed: " + handle),
                error -> assertThat(error.getProblems()).
                        anySatisfy(i -> assertThat(i).contains("not enough parts"))
        );

        parser.parseHandle("prefix%2Fsuffix",
                handle -> fail("parsing should have failed: " + handle),
                error -> assertThat(error.getProblems()).
                        anySatisfy(i -> assertThat(i).contains("not enough parts"))
        );

        parser.parseHandle("prefix%2Fsuffix/description",
                handle -> assertThat(handle).isEqualTo(
                        new IdentifierHandle("prefix%2Fsuffix", "description")),
                error -> fail(String.join(", ", error.getProblems())));
    }

    @Test
    public void validHandleUrlShouldPass() {
        parser.parseUrl("https://raid.org.au/prefix/suffix",
                handle -> assertThat(handle).isEqualTo(
                        new IdentifierUrl("https://raid.org.au", "prefix", "suffix")),
                error -> fail(String.join(", ", error.getProblems())));
    }

    @Test
    public void parseInvalidUrlsShouldFail() {
        parser.parseUrl("prefix/suffix",
                parsedUrl -> fail("parsing should have failed: " + parsedUrl),
                error -> assertThat(error.getProblems()).
                        anySatisfy(i -> assertThat(i).contains("no protocol"))
        );

        parser.parseUrl("prefixUrl/prefix/suffix",
                parsedUrl -> fail("parsing should have failed: " + parsedUrl),
                error -> assertThat(error.getProblems()).
                        anySatisfy(i -> assertThat(i).contains("no protocol"))
        );

        parser.parseUrl("/prefixUrl/prefix/suffix",
                parsedUrl -> fail("parsing should have failed: " + parsedUrl),
                error -> assertThat(error.getProblems()).
                        anySatisfy(i -> assertThat(i).contains("no protocol"))
        );

        parser.parseUrl("prefixUrl.com/prefix/suffix",
                parsedUrl -> fail("parsing should have failed: " + parsedUrl),
                error -> assertThat(error.getProblems()).
                        anySatisfy(i -> assertThat(i).contains("no protocol"))
        );

        parser.parseUrl("/prefixUrl.com/prefix/suffix",
                parsedUrl -> fail("parsing should have failed: " + parsedUrl),
                error -> assertThat(error.getProblems()).
                        anySatisfy(i -> assertThat(i).contains("no protocol"))
        );

        parser.parseUrl("https://prefixUrl//suffix",
                parsedUrl -> fail("parsing should have failed: " + parsedUrl),
                error -> assertThat(error.getProblems()).
                        anySatisfy(i -> assertThat(i).contains("prefix may not be blank"))
        );

        parser.parseUrl("https:///prefixUrl/suffix",
                parsedUrl -> fail("parsing should have failed: " + parsedUrl),
                error -> assertThat(error.getProblems()).
                        anySatisfy(i -> assertThat(i).contains("host may not be blank"))
        );

    }

    @Test
    public void formatValidUrlsShouldSucceed() {
        assertThat(
                new IdentifierUrl("https://prefixUrl", "prefix", "suffix").formatUrl()
        ).isEqualTo("https://prefixUrl/prefix/suffix");
        assertThat(
                new IdentifierUrl("https://prefixUrl/", "prefix", "suffix").formatUrl()
        ).isEqualTo("https://prefixUrl/prefix/suffix");

        // note that urlPrefix is not required to be a full url
        assertThat(
                new IdentifierUrl("prefixUrl/", "prefix", "suffix").formatUrl()
        ).isEqualTo("prefixUrl/prefix/suffix");
        assertThat(
                new IdentifierUrl("prefixUrl", "prefix", "suffix").formatUrl()
        ).isEqualTo("prefixUrl/prefix/suffix");
    }

    @Test
    public void formatAndParseShouldBeReciprocal() throws Exception {
        parser.parseUrl(
                "https://prefixUrl/prefix/suffix",
                parsedUrl -> assertThat(parsedUrl.formatUrl()).isEqualTo(
                        "https://prefixUrl/prefix/suffix"),
                error -> fail(String.join(", ", error.getProblems())));

        parser.parseUrl(
                "https://prefixUrl/prefix/suffix/description",
                parsedUrl -> assertThat(parsedUrl.formatUrl()).isEqualTo(
                        "https://prefixUrl/prefix/suffix/description"),
                error -> fail(String.join(", ", error.getProblems())));

        parser.parseUrl("https://prefixUrl/prefix/suffix?param=value",
                parsedUrl -> assertThat(parsedUrl.formatUrl()).isEqualTo(
                        "https://prefixUrl/prefix/suffix"),
                error -> fail(String.join(", ", error.getProblems())));

        parser.parseUrl("https://prefixUrl/prefix/suffix#anchor",
                parsedUrl -> assertThat(parsedUrl.formatUrl()).isEqualTo(
                        "https://prefixUrl/prefix/suffix"),
                error -> fail(String.join(", ", error.getProblems())));

        parser.parseUrl("https://prefixUrl/prefix/suffix?param1=value1&param2=value2#anchor",
                parsedUrl -> assertThat(parsedUrl.formatUrl()).isEqualTo(
                        "https://prefixUrl/prefix/suffix"),
                error -> fail(String.join(", ", error.getProblems())));
    }
}