package au.org.raid.api.service;

import au.org.raid.api.exception.SubjectTypeNotFoundException;
import au.org.raid.api.exception.SubjectTypeSchemaNotFoundException;
import au.org.raid.api.factory.SubjectFactory;
import au.org.raid.api.factory.record.RaidSubjectKeywordRecordFactory;
import au.org.raid.api.factory.record.RaidSubjectRecordFactory;
import au.org.raid.api.repository.RaidSubjectKeywordRepository;
import au.org.raid.api.repository.RaidSubjectRepository;
import au.org.raid.api.repository.SubjectTypeRepository;
import au.org.raid.api.repository.SubjectTypeSchemaRepository;
import au.org.raid.idl.raidv2.model.Subject;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class SubjectService {
    private final LanguageService languageService;
    private final SubjectTypeRepository subjectTypeRepository;
    private final RaidSubjectRepository raidSubjectRepository;
    private final RaidSubjectRecordFactory raidSubjectRecordFactory;
    private final RaidSubjectKeywordRecordFactory raidSubjectKeywordRecordFactory;
    private final RaidSubjectKeywordRepository raidSubjectKeywordRepository;
    private final SubjectTypeSchemaRepository subjectTypeSchemaRepository;
    private final SubjectKeywordService subjectKeywordService;
    private final SubjectFactory subjectFactory;

    public void create(final List<Subject> subjects, final String handle) {
        if (subjects == null) {
            return;
        }

        for (final var subject : subjects) {
            assert subject.getSchemaUri() != null;
            assert subject.getId() != null;

            final var subjectId = subject.getId().substring(subject.getId().lastIndexOf('/') + 1);
            final var subjectType = subjectTypeRepository.findBySubjectTypeIdAndSchemaUri(subjectId, subject.getSchemaUri().getValue())
                    .orElseThrow(() -> new SubjectTypeNotFoundException(subjectId));

            final var raidSubjectRecord = raidSubjectRecordFactory.create(handle, subjectType.getId());

            final var raidSubject = raidSubjectRepository.create(raidSubjectRecord);

            if (subject.getKeyword() != null) {
                for (final var keyword : subject.getKeyword()) {
                    Integer languageId = null;
                    if (keyword.getLanguage() != null) {
                        languageId = languageService.findLanguageId(keyword.getLanguage());
                    }

                    final var raidSubjectKeywordRecord =
                            raidSubjectKeywordRecordFactory.create(raidSubject.getId(), keyword.getText(), languageId);

                    raidSubjectKeywordRepository.create(raidSubjectKeywordRecord);
                }
            }
        }
    }

    public List<Subject> findAllByHandle(final String handle) {
        final var subjects = new ArrayList<Subject>();
        final var records = raidSubjectRepository.findAllByHandle(handle);

        for (final var record : records) {
            final var typeRecord = subjectTypeRepository.findById(record.getSubjectTypeId())
                    .orElseThrow(() -> new SubjectTypeNotFoundException(record.getSubjectTypeId()));

            final var keywords = subjectKeywordService.findAllByRaidSubjectId(record.getId());

            subjects.add(subjectFactory.create(typeRecord.getSubjectTypeId(), typeRecord.getSchemaUri(), keywords));
        }
        return subjects;
    }

    public void update(final List<Subject> subjects, final String handle) {
        final var raidSubjects = raidSubjectRepository.findAllByHandle(handle);

        raidSubjects.forEach(s -> raidSubjectKeywordRepository.deleteByRaidSubjectId(s.getId()));

        raidSubjectRepository.deleteAllByHandle(handle);
        create(subjects, handle);
    }
}
