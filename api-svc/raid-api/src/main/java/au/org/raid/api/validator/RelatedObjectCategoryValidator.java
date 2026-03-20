package au.org.raid.api.validator;

import au.org.raid.api.repository.RelatedObjectCategoryRepository;
import au.org.raid.api.repository.RelatedObjectCategorySchemaRepository;
import au.org.raid.idl.raidv2.model.RelatedObjectCategory;
import au.org.raid.idl.raidv2.model.ValidationFailure;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static au.org.raid.api.endpoint.message.ValidationMessage.*;
import static au.org.raid.api.util.StringUtil.isBlank;

@Component
public class RelatedObjectCategoryValidator {
    private final RelatedObjectCategoryRepository relatedObjectCategoryRepository;
    private final RelatedObjectCategorySchemaRepository relatedObjectCategorySchemaRepository;

    public RelatedObjectCategoryValidator(final RelatedObjectCategoryRepository relatedObjectCategoryRepository, final RelatedObjectCategorySchemaRepository relatedObjectCategorySchemaRepository) {
        this.relatedObjectCategoryRepository = relatedObjectCategoryRepository;
        this.relatedObjectCategorySchemaRepository = relatedObjectCategorySchemaRepository;
    }

    public List<ValidationFailure> validate(final List<RelatedObjectCategory> categories, final int index) {
        var failures = new ArrayList<ValidationFailure>();


        return failures;
    }
}
