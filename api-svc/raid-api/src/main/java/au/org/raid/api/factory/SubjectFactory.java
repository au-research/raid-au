package au.org.raid.api.factory;

import au.org.raid.api.util.SchemaValues;
import au.org.raid.api.util.SubjectSchemaUriMapper;
import au.org.raid.idl.raidv2.model.Subject;
import au.org.raid.idl.raidv2.model.SubjectKeyword;
import au.org.raid.idl.raidv2.model.SubjectSchemaURIEnum;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SubjectFactory {
    public Subject create(final String id, final String schemaUri, final List<SubjectKeyword> keywords) {
        // schemaUri is a DB URI — map it to the enum constant
        final var schemaUriEnum = SubjectSchemaUriMapper.fromDbUri(schemaUri);
        if (schemaUriEnum == null) {
            throw new IllegalArgumentException("Unrecognised subject schema URI: " + schemaUri);
        }
        return new Subject()
                .id(SchemaValues.SUBJECT_ID_PREFIX.getUri() + id)
                .schemaUri(schemaUriEnum)
                .keyword(keywords);
    }
}