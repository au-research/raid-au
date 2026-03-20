package au.org.raid.api.validator;

import au.org.raid.api.repository.TitleTypeRepository;
import au.org.raid.api.repository.TitleTypeSchemaRepository;
import au.org.raid.idl.raidv2.model.TitleType;
import au.org.raid.idl.raidv2.model.ValidationFailure;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static au.org.raid.api.endpoint.message.ValidationMessage.*;
import static au.org.raid.api.util.StringUtil.isBlank;

@Component
public class TitleTypeValidator {
    private final TitleTypeSchemaRepository titleTypeSchemaRepository;
    private final TitleTypeRepository titleTypeRepository;

    public TitleTypeValidator(final TitleTypeSchemaRepository titleTypeSchemaRepository, final TitleTypeRepository titleTypeRepository) {
        this.titleTypeSchemaRepository = titleTypeSchemaRepository;
        this.titleTypeRepository = titleTypeRepository;
    }

    public List<ValidationFailure> validate(final TitleType titleType, final int index) {
        final var failures = new ArrayList<ValidationFailure>();



        return failures;
    }
}