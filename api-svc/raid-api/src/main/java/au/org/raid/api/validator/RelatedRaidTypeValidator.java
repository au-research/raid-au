package au.org.raid.api.validator;

import au.org.raid.api.repository.RelatedRaidTypeRepository;
import au.org.raid.api.repository.RelatedRaidTypeSchemaRepository;
import au.org.raid.idl.raidv2.model.RelatedRaidType;
import au.org.raid.idl.raidv2.model.ValidationFailure;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static au.org.raid.api.endpoint.message.ValidationMessage.*;
import static au.org.raid.api.util.StringUtil.isBlank;

@Component
@RequiredArgsConstructor
public class RelatedRaidTypeValidator {
    private final RelatedRaidTypeSchemaRepository relatedRaidTypeSchemaRepository;
    private final RelatedRaidTypeRepository relatedRaidTypeRepository;

    public List<ValidationFailure> validate(final RelatedRaidType relatedRaidType, final int index) {
        final var failures = new ArrayList<ValidationFailure>();


        return failures;
    }

}