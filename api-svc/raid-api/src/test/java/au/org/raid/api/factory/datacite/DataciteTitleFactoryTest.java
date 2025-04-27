package au.org.raid.api.factory.datacite;

import au.org.raid.idl.raidv2.model.Title;
import au.org.raid.idl.raidv2.model.TitleType;
import au.org.raid.idl.raidv2.model.TitleTypeIdEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class DataciteTitleFactoryTest {

    private DataciteTitleFactory dataciteTitleFactory = new DataciteTitleFactory();

    @Test
    @DisplayName("Create Title with 'Alternative' type")
    public void alternativeTitleType(){
        final var text = "_text";

        final var title = new Title()
                .text(text)
                .type(new TitleType().id(TitleTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_TITLE_TYPE_SCHEMA_4));

        final var result = dataciteTitleFactory.create(title);

        assertThat(result.getTitleType(), is("AlternativeTitle"));
        assertThat(result.getTitle(), is(text));
    }
    @Test
    @DisplayName("Create Title with 'Short' type")
    public void shortTitleType(){
        final var text = "_text";

        final var title = new Title()
                .text(text)
                .type(new TitleType().id(TitleTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_TITLE_TYPE_SCHEMA_157));

        final var result = dataciteTitleFactory.create(title);

        assertThat(result.getTitleType(), is("Other"));
        assertThat(result.getTitle(), is(text));
    }

    @Test
    @DisplayName("Create Title with 'Acronym' type")
    public void acronymTitleType(){
        final var text = "_text";

        final var title = new Title()
                .text(text)
                .type(new TitleType().id(TitleTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_TITLE_TYPE_SCHEMA_156));

        final var result = dataciteTitleFactory.create(title);

        assertThat(result.getTitleType(), is("Other"));
        assertThat(result.getTitle(), is(text));
    }
}
