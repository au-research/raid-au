package au.org.raid.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Replaces legacy github.com vocabulary URIs with their modern equivalents
 * (vocabulary.raid.org / COAR) in raw JSON strings. This allows legacy raid
 * data to be deserialized into RaidDto which uses enum types that only accept
 * the new URIs.
 *
 * Also handles the structural transformation of legacy contributor positions
 * (leader/contact) into boolean flags on the contributor object.
 *
 * URI mappings sourced from RaidUpgradeService.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VocabularyUriReplacementService {

    private static final String LEAD_CONTRIBUTOR_POSITION_ID =
            "https://github.com/au-research/raid-metadata/blob/main/scheme/contributor/position/v1/leader.json";

    private static final String CONTACT_CONTRIBUTOR_POSITION_ID =
            "https://github.com/au-research/raid-metadata/blob/main/scheme/contributor/position/v1/contact.json";

    private static final Map<String, String> URI_REPLACEMENTS = new LinkedHashMap<>();

    static {
        // Access type
        URI_REPLACEMENTS.put(
                "https://github.com/au-research/raid-metadata/blob/main/scheme/access/type/v1/closed.json",
                "https://vocabularies.coar-repositories.org/access_rights/c_f1cf/");
        URI_REPLACEMENTS.put(
                "https://github.com/au-research/raid-metadata/blob/main/scheme/access/type/v1/embargoed.json",
                "https://vocabularies.coar-repositories.org/access_rights/c_f1cf/");
        URI_REPLACEMENTS.put(
                "https://github.com/au-research/raid-metadata/blob/main/scheme/access/type/v1/open.json",
                "https://vocabularies.coar-repositories.org/access_rights/c_abf2/");
        URI_REPLACEMENTS.put(
                "https://github.com/au-research/raid-metadata/tree/main/scheme/access/type/v1/",
                "https://vocabularies.coar-repositories.org/access_rights/");

        // Title type
        URI_REPLACEMENTS.put(
                "https://github.com/au-research/raid-metadata/blob/main/scheme/title/type/v1/alternative.json",
                "https://vocabulary.raid.org/title.type.schema/4");
        URI_REPLACEMENTS.put(
                "https://github.com/au-research/raid-metadata/blob/main/scheme/title/type/v1/primary.json",
                "https://vocabulary.raid.org/title.type.schema/5");
        URI_REPLACEMENTS.put(
                "https://github.com/au-research/raid-metadata/tree/main/scheme/title/type/v1/",
                "https://vocabulary.raid.org/title.type.schema/376");

        // Description type
        URI_REPLACEMENTS.put(
                "https://github.com/au-research/raid-metadata/blob/main/scheme/description/type/v1/alternative.json",
                "https://vocabulary.raid.org/description.type.schema/319");
        URI_REPLACEMENTS.put(
                "https://github.com/au-research/raid-metadata/blob/main/scheme/description/type/v1/primary.json",
                "https://vocabulary.raid.org/description.type.schema/318");
        URI_REPLACEMENTS.put(
                "https://github.com/au-research/raid-metadata/tree/main/scheme/description/type/v1/",
                "https://vocabulary.raid.org/description.type.schema/320");

        // Contributor position (non-leader/contact)
        URI_REPLACEMENTS.put(
                "https://github.com/au-research/raid-metadata/blob/main/scheme/contributor/position/v1/co-investigator.json",
                "https://vocabulary.raid.org/contributor.position.schema/308");
        URI_REPLACEMENTS.put(
                "https://github.com/au-research/raid-metadata/blob/main/scheme/contributor/position/v1/other-participant.json",
                "https://vocabulary.raid.org/contributor.position.schema/311");
        URI_REPLACEMENTS.put(
                "https://github.com/au-research/raid-metadata/blob/main/scheme/contributor/position/v1/principal-investigator.json",
                "https://vocabulary.raid.org/contributor.position.schema/307");
        URI_REPLACEMENTS.put(
                "https://github.com/au-research/raid-metadata/tree/main/scheme/contributor/position/v1/",
                "https://vocabulary.raid.org/contributor.position.schema/305");

        // Organisation role
        URI_REPLACEMENTS.put(
                "https://github.com/au-research/raid-metadata/blob/main/scheme/organisation/role/v1/contractor.json",
                "https://vocabulary.raid.org/organisation.role.schema/185");
        URI_REPLACEMENTS.put(
                "https://github.com/au-research/raid-metadata/blob/main/scheme/organisation/role/v1/lead-research-organisation.json",
                "https://vocabulary.raid.org/organisation.role.schema/182");
        URI_REPLACEMENTS.put(
                "https://github.com/au-research/raid-metadata/blob/main/scheme/organisation/role/v1/other-organisation.json",
                "https://vocabulary.raid.org/organisation.role.schema/188");
        URI_REPLACEMENTS.put(
                "https://github.com/au-research/raid-metadata/blob/main/scheme/organisation/role/v1/other-research-organisation.json",
                "https://vocabulary.raid.org/organisation.role.schema/183");
        URI_REPLACEMENTS.put(
                "https://github.com/au-research/raid-metadata/blob/main/scheme/organisation/role/v1/partner-organisation.json",
                "https://vocabulary.raid.org/organisation.role.schema/184");
        URI_REPLACEMENTS.put(
                "https://github.com/au-research/raid-metadata/tree/main/scheme/organisation/role/v1/",
                "https://vocabulary.raid.org/organisation.role.schema/359");

        // Related object type
        URI_REPLACEMENTS.put(
                "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/audiovisual.json",
                "https://vocabulary.raid.org/relatedObject.type.schema/273");
        URI_REPLACEMENTS.put(
                "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/book-chapter.json",
                "https://vocabulary.raid.org/relatedObject.type.schema/271");
        URI_REPLACEMENTS.put(
                "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/book.json",
                "https://vocabulary.raid.org/relatedObject.type.schema/258");
        URI_REPLACEMENTS.put(
                "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/computational-notebook.json",
                "https://vocabulary.raid.org/relatedObject.type.schema/256");
        URI_REPLACEMENTS.put(
                "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/conference-paper.json",
                "https://vocabulary.raid.org/relatedObject.type.schema/264");
        URI_REPLACEMENTS.put(
                "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/conference-poster.json",
                "https://vocabulary.raid.org/relatedObject.type.schema/248");
        URI_REPLACEMENTS.put(
                "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/conference-proceeding.json",
                "https://vocabulary.raid.org/relatedObject.type.schema/262");
        URI_REPLACEMENTS.put(
                "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/data-paper.json",
                "https://vocabulary.raid.org/relatedObject.type.schema/255");
        URI_REPLACEMENTS.put(
                "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/dataset.json",
                "https://vocabulary.raid.org/relatedObject.type.schema/269");
        URI_REPLACEMENTS.put(
                "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/dissertation.json",
                "https://vocabulary.raid.org/relatedObject.type.schema/253");
        URI_REPLACEMENTS.put(
                "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/educational-material.json",
                "https://vocabulary.raid.org/relatedObject.type.schema/267");
        URI_REPLACEMENTS.put(
                "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/event.json",
                "https://vocabulary.raid.org/relatedObject.type.schema/260");
        URI_REPLACEMENTS.put(
                "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/funding.json",
                "https://vocabulary.raid.org/relatedObject.type.schema/272");
        URI_REPLACEMENTS.put(
                "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/image.json",
                "https://vocabulary.raid.org/relatedObject.type.schema/257");
        URI_REPLACEMENTS.put(
                "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/instrument.json",
                "https://vocabulary.raid.org/relatedObject.type.schema/266");
        URI_REPLACEMENTS.put(
                "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/journal-article.json",
                "https://vocabulary.raid.org/relatedObject.type.schema/250");
        URI_REPLACEMENTS.put(
                "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/model.json",
                "https://vocabulary.raid.org/relatedObject.type.schema/263");
        URI_REPLACEMENTS.put(
                "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/output-management-plan.json",
                "https://vocabulary.raid.org/relatedObject.type.schema/247");
        URI_REPLACEMENTS.put(
                "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/physical-object.json",
                "https://vocabulary.raid.org/relatedObject.type.schema/270");
        URI_REPLACEMENTS.put(
                "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/preprint.json",
                "https://vocabulary.raid.org/relatedObject.type.schema/254");
        URI_REPLACEMENTS.put(
                "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/prize.json",
                "https://vocabulary.raid.org/relatedObject.type.schema/268");
        URI_REPLACEMENTS.put(
                "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/report.json",
                "https://vocabulary.raid.org/relatedObject.type.schema/252");
        URI_REPLACEMENTS.put(
                "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/service.json",
                "https://vocabulary.raid.org/relatedObject.type.schema/274");
        URI_REPLACEMENTS.put(
                "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/software.json",
                "https://vocabulary.raid.org/relatedObject.type.schema/259");
        URI_REPLACEMENTS.put(
                "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/sound.json",
                "https://vocabulary.raid.org/relatedObject.type.schema/261");
        URI_REPLACEMENTS.put(
                "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/standard.json",
                "https://vocabulary.raid.org/relatedObject.type.schema/251");
        URI_REPLACEMENTS.put(
                "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/text.json",
                "https://vocabulary.raid.org/relatedObject.type.schema/265");
        URI_REPLACEMENTS.put(
                "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/workflow.json",
                "https://vocabulary.raid.org/relatedObject.type.schema/249");
        URI_REPLACEMENTS.put(
                "https://github.com/au-research/raid-metadata/tree/main/scheme/related-object/type/v1/",
                "https://vocabulary.raid.org/relatedObject.type.schema/329");

        // Related object category
        URI_REPLACEMENTS.put(
                "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/category/v1/input.json",
                "https://vocabulary.raid.org/relatedObject.category.id/191");
        URI_REPLACEMENTS.put(
                "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/category/v1/internal.json",
                "https://vocabulary.raid.org/relatedObject.category.id/192");
        URI_REPLACEMENTS.put(
                "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/category/v1/output.json",
                "https://vocabulary.raid.org/relatedObject.category.id/190");
        URI_REPLACEMENTS.put(
                "https://github.com/au-research/raid-metadata/tree/main/scheme/related-object/category/v1/",
                "https://vocabulary.raid.org/relatedObject.category.schemaUri/386");

        // Related raid type
        URI_REPLACEMENTS.put(
                "https://github.com/au-research/raid-metadata/blob/main/scheme/related-raid/type/v1/has-part.json",
                "https://vocabulary.raid.org/relatedRaid.type.schema/201");
        URI_REPLACEMENTS.put(
                "https://github.com/au-research/raid-metadata/blob/main/scheme/related-raid/type/v1/is-continued-by.json",
                "https://vocabulary.raid.org/relatedRaid.type.schema/203");
        URI_REPLACEMENTS.put(
                "https://github.com/au-research/raid-metadata/blob/main/scheme/related-raid/type/v1/is-derived-from.json",
                "https://vocabulary.raid.org/relatedRaid.type.schema/200");
        URI_REPLACEMENTS.put(
                "https://github.com/au-research/raid-metadata/blob/main/scheme/related-raid/type/v1/is-identical-to.json",
                "https://vocabulary.raid.org/relatedRaid.type.schema/204");
        URI_REPLACEMENTS.put(
                "https://github.com/au-research/raid-metadata/blob/main/scheme/related-raid/type/v1/is-obsoleted-by.json",
                "https://vocabulary.raid.org/relatedRaid.type.schema/205");
        URI_REPLACEMENTS.put(
                "https://github.com/au-research/raid-metadata/blob/main/scheme/related-raid/type/v1/is-part-of.json",
                "https://vocabulary.raid.org/relatedRaid.type.schema/202");
        URI_REPLACEMENTS.put(
                "https://github.com/au-research/raid-metadata/blob/main/scheme/related-raid/type/v1/is-source-of.json",
                "https://vocabulary.raid.org/relatedRaid.type.schema/199");
        URI_REPLACEMENTS.put(
                "https://github.com/au-research/raid-metadata/blob/main/scheme/related-raid/type/v1/obsoletes.json",
                "https://vocabulary.raid.org/relatedRaid.type.schema/198");
        URI_REPLACEMENTS.put(
                "https://github.com/au-research/raid-metadata/tree/main/scheme/related-raid/type/v1/",
                "https://vocabulary.raid.org/relatedRaid.type.schema/367");
    }

    private final ObjectMapper objectMapper;

    /**
     * Replaces all legacy github.com vocabulary URIs in the given JSON string
     * with their modern equivalents. Also transforms legacy leader/contact
     * contributor positions into boolean flags.
     *
     * @param json raw JSON string that may contain legacy vocabulary URIs
     * @return transformed JSON string safe for deserialization into RaidDto
     */
    public String upgradeVocabularyUris(final String json) {
        if (json == null || !json.contains("github.com/au-research/raid-metadata")) {
            return json;
        }

        var result = replaceUris(json);
        result = upgradeContributorPositions(result);
        return result;
    }

    private String replaceUris(final String json) {
        var result = json;
        for (final var entry : URI_REPLACEMENTS.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private String upgradeContributorPositions(final String json) {
        if (!json.contains(LEAD_CONTRIBUTOR_POSITION_ID) && !json.contains(CONTACT_CONTRIBUTOR_POSITION_ID)) {
            return json;
        }

        try {
            final var raid = objectMapper.readValue(json, Map.class);
            final var contributors = (List<Map<String, Object>>) raid.get("contributor");

            if (contributors == null) {
                return json;
            }

            for (final var contributor : contributors) {
                final var positions = (List<Map<String, Object>>) contributor.get("position");
                if (positions == null) {
                    continue;
                }

                final var positionsToRemove = new ArrayList<Map<String, Object>>();

                for (final var position : positions) {
                    final var positionId = (String) position.get("id");

                    if (LEAD_CONTRIBUTOR_POSITION_ID.equals(positionId)) {
                        contributor.put("leader", true);
                        positionsToRemove.add(position);
                    } else if (CONTACT_CONTRIBUTOR_POSITION_ID.equals(positionId)) {
                        contributor.put("contact", true);
                        positionsToRemove.add(position);
                    }
                }

                positions.removeAll(positionsToRemove);

                // If all positions were leader/contact, add a default "Other" position
                if (positions.isEmpty()) {
                    final var remaining = positionsToRemove.get(0);
                    final var defaultPosition = new LinkedHashMap<String, Object>();
                    defaultPosition.put("id", "https://vocabulary.raid.org/contributor.position.schema/311");
                    defaultPosition.put("schemaUri", "https://vocabulary.raid.org/contributor.position.schema/305");
                    defaultPosition.put("startDate", remaining.getOrDefault("startDate", ""));
                    defaultPosition.put("endDate", remaining.getOrDefault("endDate", ""));
                    positions.add(defaultPosition);
                }
            }

            return objectMapper.writeValueAsString(raid);
        } catch (JsonProcessingException e) {
            log.warn("Failed to upgrade contributor positions for legacy raid, returning URI-replaced JSON", e);
            return json;
        }
    }
}
