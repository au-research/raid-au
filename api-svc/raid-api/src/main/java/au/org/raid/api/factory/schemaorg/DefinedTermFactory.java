package au.org.raid.api.factory.schemaorg;

import au.org.raid.idl.raidv2.model.DefinedTerm;
import au.org.raid.idl.raidv2.model.Subject;
import org.springframework.stereotype.Component;

@Component
public class DefinedTermFactory {
    public DefinedTerm create(final Subject subject) {

        return new DefinedTerm()
                .atType("DefinedTerm")
                .atId(subject.getId())
                .inDefinedTermSet(subject.getSchemaUri().getValue())
                ;// TODO: add name?
    }
}
