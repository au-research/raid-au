package au.org.raid.api.config;

import au.org.raid.api.client.contributor.isni.IsniClient;
import au.org.raid.api.client.contributor.orcid.OrcidClient;
import au.org.raid.api.config.properties.ContributorValidationProperties;
import au.org.raid.api.validator.ContributorPositionValidator;
import au.org.raid.api.validator.ContributorRoleValidator;
import au.org.raid.api.validator.ContributorTypeValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class ContributorValidationConfig {
    private final ContributorValidationProperties validationProperties;
    private final OrcidClient orcidClient;
    private final IsniClient isniClient;
    private final ContributorRoleValidator roleValidator;
    private final ContributorPositionValidator positionValidator;

    @Bean
    public ContributorTypeValidator orcidValidator() {
        return new ContributorTypeValidator(
                validationProperties.getOrcid(),
                orcidClient,
                roleValidator,
                positionValidator
        );
    }

    @Bean
    public ContributorTypeValidator isniValidator() {
        return new ContributorTypeValidator(
                validationProperties.getIsni(),
                isniClient,
                roleValidator,
                positionValidator
        );
    }
}
