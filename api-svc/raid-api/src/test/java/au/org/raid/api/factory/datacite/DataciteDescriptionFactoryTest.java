package au.org.raid.api.factory.datacite;

import au.org.raid.idl.raidv2.model.Description;
import au.org.raid.idl.raidv2.model.DescriptionType;
import au.org.raid.idl.raidv2.model.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DataciteDescriptionFactoryTest {

    private DataciteDescriptionFactory dataciteDescriptionFactory = new DataciteDescriptionFactory();

    @Test
    @DisplayName("Create with 'Primary' description")
    public void primaryDescription(){
        final var text = "_text";

        final var description = new Description()
                .text(text)
                .type(new DescriptionType().id("https://vocabulary.raid.org/description.type.schema/318"))
                .language(new Language()
                        .id("eng"));

        final var result = dataciteDescriptionFactory.create(description);

        assertEquals(result.getDescription(), text);
        assertEquals(result.getDescriptionType(), "Abstract");
        assertEquals(result.getLang(), "eng");
    }
    @Test
    @DisplayName("Create with 'Alternative' description")
    public void alternativeDescription(){
        final var text = "_text";

        final var description = new Description()
                .text(text)
                .type(new DescriptionType().id("https://vocabulary.raid.org/description.type.schema/319"))
                .language(new Language()
                        .id("eng"));

        final var result = dataciteDescriptionFactory.create(description);

        assertEquals(result.getDescription(), text);
        assertEquals(result.getDescriptionType(), "Other");
        assertEquals(result.getLang(), "eng");
    }
    @Test
    @DisplayName("Create with 'Brief' description")
    public void briefDescription(){
        final var text = "_text";

        final var description = new Description()
                .text(text)
                .type(new DescriptionType().id("https://vocabulary.raid.org/description.type.schema/3"));

        final var result = dataciteDescriptionFactory.create(description);

        assertEquals(result.getDescription(), text);
        assertEquals(result.getDescriptionType(), "Other");
    }

    @Test
    @DisplayName("Create with 'Significance Statement' description")
    public void significanceStatementDescription(){
        final var text = "_text";

        final var description = new Description()
                .text(text)
                .type(new DescriptionType().id("https://vocabulary.raid.org/description.type.schema/9"));

        final var result = dataciteDescriptionFactory.create(description);

        assertEquals(result.getDescription(), text);
        assertEquals(result.getDescriptionType(), "Other");
    }
    @Test
    @DisplayName("Create with 'Methods' description")
    public void methodsDescription(){
        final var text = "_text";

        final var description = new Description()
                .text(text)
                .type(new DescriptionType().id("https://vocabulary.raid.org/description.type.schema/8"));

        final var result = dataciteDescriptionFactory.create(description);

        assertEquals(result.getDescription(), text);
        assertEquals(result.getDescriptionType(), "Methods");
    }

    @Test
    @DisplayName("Create with 'Objectives' description")
    public void objectivesDescription(){
        final var text = "_text";

        final var description = new Description()
                .text(text)
                .type(new DescriptionType().id("https://vocabulary.raid.org/description.type.schema/7"));

        final var result = dataciteDescriptionFactory.create(description);

        assertEquals(result.getDescription(), text);
        assertEquals(result.getDescriptionType(), "Other");
    }

    @Test
    @DisplayName("Create with 'Other' description")
    public void otherDescription(){
        final var text = "_text";

        final var description = new Description()
                .text(text)
                .type(new DescriptionType().id("https://vocabulary.raid.org/description.type.schema/6"));

        final var result = dataciteDescriptionFactory.create(description);

        assertEquals(result.getDescription(), text);
        assertEquals(result.getDescriptionType(), "Other");
    }
}
