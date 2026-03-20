package au.org.raid.api.validator;

import au.org.raid.api.repository.RelatedObjectTypeRepository;
import au.org.raid.api.repository.RelatedObjectTypeSchemaRepository;
import au.org.raid.idl.raidv2.model.RelatedObjectType;
import au.org.raid.idl.raidv2.model.ValidationFailure;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static au.org.raid.api.endpoint.message.ValidationMessage.*;
import static au.org.raid.api.util.StringUtil.isBlank;

@Component
public class RelatedObjectTypeValidator {
    private final RelatedObjectTypeRepository relatedObjectTypeRepository;
    private final RelatedObjectTypeSchemaRepository relatedObjectTypeSchemaRepository;

    public RelatedObjectTypeValidator(final RelatedObjectTypeRepository relatedObjectTypeRepository, final RelatedObjectTypeSchemaRepository relatedObjectTypeSchemaRepository) {
        this.relatedObjectTypeRepository = relatedObjectTypeRepository;
        this.relatedObjectTypeSchemaRepository = relatedObjectTypeSchemaRepository;
    }


    public List<ValidationFailure> validate(final RelatedObjectType relatedObjectType, final int index) {
        var failures = new ArrayList<ValidationFailure>();

        return failures;
    }
}
