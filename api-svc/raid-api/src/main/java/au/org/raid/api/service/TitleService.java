package au.org.raid.api.service;

import au.org.raid.api.exception.TitleTypeNotFoundException;
import au.org.raid.api.exception.TitleTypeSchemaNotFoundException;
import au.org.raid.api.factory.TitleFactory;
import au.org.raid.api.factory.record.RaidTitleRecordFactory;
import au.org.raid.api.repository.RaidTitleRepository;
import au.org.raid.api.repository.TitleTypeRepository;
import au.org.raid.api.repository.TitleTypeSchemaRepository;
import au.org.raid.idl.raidv2.model.Title;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class TitleService {
    private final RaidTitleRepository raidTitleRepository;
    private final TitleTypeRepository titleTypeRepository;
    private final TitleTypeSchemaRepository titleTypeSchemaRepository;
    private final LanguageService languageService;
    private final RaidTitleRecordFactory raidTitleRecordFactory;
    private final TitleFactory titleFactory;

    public void create(final List<Title> titles, final String handle) {

        for (final var title : titles) {
            final var titleTypeSchema = titleTypeSchemaRepository.findByUri(title.getType().getSchemaUri())
                    .orElseThrow(() -> new TitleTypeSchemaNotFoundException(title.getType().getSchemaUri()));

            final var titleType = titleTypeRepository.findByUriAndSchemaId(title.getType().getId(), titleTypeSchema.getId())
                    .orElseThrow(() -> new TitleTypeNotFoundException(title.getType().getId(), title.getType().getSchemaUri()));

            final var languageId = languageService.findLanguageId(title.getLanguage());

            final var raidTitleRecord = raidTitleRecordFactory.create(title, handle, titleType.getId(), languageId);

            raidTitleRepository.create(raidTitleRecord);
        }
    }

    public List<Title> findAllByHandle(final String handle) {
        final var titles = new ArrayList<Title>();
        final var records = raidTitleRepository.findAllByHandle(handle);

        for (final var record : records) {
            final var titleTypeId = record.getTitleTypeId();

            final var titleTypeRecord = titleTypeRepository.findById(titleTypeId)
                    .orElseThrow(() -> new TitleTypeNotFoundException(titleTypeId));

            final var titleTypeSchemaId = titleTypeRecord.getSchemaId();
            final var titleTypeSchemaRecord = titleTypeSchemaRepository.findById(titleTypeSchemaId)
                    .orElseThrow(() -> new TitleTypeSchemaNotFoundException(titleTypeSchemaId));

            final var language = languageService.findById(record.getLanguageId());

            titles.add(titleFactory.create(record, titleTypeRecord.getUri(), titleTypeSchemaRecord.getUri(), language));
        }

        return titles;
    }

    public void update(final List<Title> title, final String handle) {
        raidTitleRepository.deleteAllByHandle(handle);
        create(title, handle);
    }
}
