package au.org.raid.api.validator;

import au.org.raid.api.repository.DescriptionTypeRepository;
import au.org.raid.api.repository.DescriptionTypeSchemaRepository;
import au.org.raid.idl.raidv2.model.DescriptionType;
import au.org.raid.idl.raidv2.model.ValidationFailure;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static au.org.raid.api.endpoint.message.ValidationMessage.*;
import static au.org.raid.api.util.StringUtil.isBlank;

@Component
@RequiredArgsConstructor
public class DescriptionTypeValidator {
    private final DescriptionTypeSchemaRepository descriptionTypeSchemaRepository;
    private final DescriptionTypeRepository descriptionTypeRepository;

    public List<ValidationFailure> validate(final DescriptionType descriptionType, final int index) {
        final var failures = new ArrayList<ValidationFailure>();

        return failures;
    }
}