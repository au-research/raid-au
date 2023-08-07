package raido.apisvc.service.raid;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import raido.apisvc.exception.InvalidJsonException;
import raido.apisvc.exception.InvalidVersionException;
import raido.apisvc.exception.ResourceNotFoundException;
import raido.apisvc.exception.UnknownServicePointException;
import raido.apisvc.factory.IdFactory;
import raido.apisvc.factory.RaidDtoFactory;
import raido.apisvc.factory.RaidRecordFactory;
import raido.apisvc.repository.RaidRepository;
import raido.apisvc.repository.ServicePointRepository;
import raido.apisvc.service.apids.ApidsService;
import raido.apisvc.service.apids.model.ApidsMintResponse;
import raido.apisvc.service.raid.id.IdentifierHandle;
import raido.apisvc.service.raid.id.IdentifierParser;
import raido.apisvc.service.raid.id.IdentifierParser.ParseProblems;
import raido.apisvc.service.raid.id.IdentifierUrl;
import raido.apisvc.spring.security.raidv2.ApiToken;
import raido.apisvc.util.Log;
import raido.idl.raidv2.model.CreateRaidV1Request;
import raido.idl.raidv2.model.RaidDto;
import raido.idl.raidv2.model.UpdateRaidV1Request;

import java.util.List;

import static raido.apisvc.util.ExceptionUtil.runtimeException;
import static raido.apisvc.util.Log.to;

/* Be careful with usage of @Transactional, see db-transaction-guideline.md */
@Component
@RequiredArgsConstructor
public class RaidStableV1Service {
  private static final Log log = to(RaidStableV1Service.class);
  private final ApidsService apidsSvc;
  private final MetadataService metaSvc;
  private final TransactionTemplate tx;
  private final RaidRepository raidRepository;
  private final ServicePointRepository servicePointRepository;
  private final RaidRecordFactory raidRecordFactory;
  private final IdentifierParser idParser;
  private final ObjectMapper objectMapper;
  private final IdFactory idFactory;
  private final RaidChecksumService checksumService;
  private final RaidDtoFactory raidDtoFactory;


  public List<RaidDto> list(final Long servicePointId) {
    return raidRepository.findAllByServicePointId(servicePointId).stream()
        .map(raidDtoFactory::create)
        .toList();
  }

  private IdentifierHandle parseHandleFromApids(
    ApidsMintResponse apidsResponse
  ) {
    var parseResult = idParser.parseHandle(apidsResponse.identifier.handle);
    
    if( parseResult instanceof ParseProblems problems ){
      log.with("handle", apidsResponse.identifier.handle).
        with("problems", problems.getProblems()).
        error("APIDS service returned malformed handle");
      throw runtimeException("APIDS service returned malformed handle: %s",
        apidsResponse.identifier.handle);
    }
   
    return (IdentifierHandle) parseResult; 
  }

  public IdentifierUrl mintRaidSchemaV1(
    final CreateRaidV1Request request,
    final long servicePoint
  ) {
    /* this is the part where we want to make sure no TX is held open.
     * Maybe *this* should be marked tx.prop=never? */
    final var servicePointRecord =
      servicePointRepository.findById(servicePoint).orElseThrow(() -> 
        new UnknownServicePointException(servicePoint) );

    final var apidsResponse = apidsSvc.mintApidsHandleContentPrefix(
      metaSvc::formatRaidoLandingPageUrl);


    IdentifierHandle handle = parseHandleFromApids(apidsResponse);
    var id = new IdentifierUrl(metaSvc.getMetaProps().handleUrlPrefix, handle);
    request.setId(idFactory.create(id, servicePointRecord));

    final var raidRecord = raidRecordFactory.create(
      request, apidsResponse, servicePointRecord );

    tx.executeWithoutResult(status -> raidRepository.insert(raidRecord));

    return id;
  }

  @SneakyThrows
  public RaidDto update(
    final UpdateRaidV1Request raid
  ) {
    final Integer version = raid.getId().getVersion();
    if (version == null) {
      throw new InvalidVersionException(version);
    }

    final IdentifierUrl identifierUrl;
    try {
      identifierUrl = idParser.parseUrlWithException(raid.getId().getIdentifier());
    }
    catch( ValidationFailureException e ){
      // it was already validated, so this shouldn't happen
      throw new RuntimeException(e);
    }
    String handle = identifierUrl.handle().format();
    
    final var existing = raidRepository.findByHandleAndVersion(handle, version)
      .orElseThrow(() -> new ResourceNotFoundException(handle));


    final var existingChecksum = checksumService.create(existing);
    final var updateChecksum = checksumService.create(raid);

    if (existingChecksum.equals(updateChecksum)) {
      return objectMapper.readValue(
        existing.getMetadata().data(), RaidDto.class );
    }
    final var raidRecord = raidRecordFactory.merge(raid, existing);

    final Integer numRowsChanged = tx.execute(status -> raidRepository.updateByHandleAndVersion(raidRecord));

    if (numRowsChanged == null || numRowsChanged != 1) {
      throw new InvalidVersionException(version);
    }

    final var result = raidRepository.findByHandle(handle)
      .orElseThrow(() -> new ResourceNotFoundException(handle));

    try {
      return objectMapper.readValue(
        result.getMetadata().data(), RaidDto.class );
    } catch (JsonProcessingException e) {
      throw new InvalidJsonException();
    }
  }

  public RaidDto read(String handle){
    final var raidRecord = raidRepository.findByHandle(handle).
      orElseThrow(() -> new ResourceNotFoundException(handle));

    return raidDtoFactory.create(raidRecord);
  }

  public boolean isEditable(final ApiToken user, final long servicePointId) {
    final var servicePoint = servicePointRepository.findById(servicePointId)
      .orElseThrow(() -> new UnknownServicePointException(servicePointId));

    return user.getClientId().equals("RAIDO_API") || servicePoint.getAppWritesEnabled();
  }
}