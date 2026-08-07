package au.org.raid.api.vocabularies.datacite;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RelatedIdentifierType {
    ARK("ARK"),
    AR_XIV("arXiv"),
    BIBCODE("bibcode"),
    DOI("DOI"),
    EAN13("EAN13"),
    EISSN("EISSN"),
    HANDLE("Handle"),
    IGSN("IGSN"),
    ISBN("ISBN"),
    ISTC("ISTC"),
    LISSN("LISSN"),
    LSID("LSID"),
    PMID("PMID"),
    PURL("PURL"),
    RRID("RRID"),
    UPC("UPC"),
    URL("URL"),
    URN("URN"),
    W3ID("w3id");

    private final String name;
}
