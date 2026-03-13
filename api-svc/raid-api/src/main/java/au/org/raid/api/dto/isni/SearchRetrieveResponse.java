package au.org.raid.api.dto.isni;

import au.org.raid.api.client.isni.dto.ResponseRecord;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@XmlRootElement(name = "searchRetrieveResponse", namespace = "http://www.loc.gov/zing/srw/")
@XmlAccessorType(XmlAccessType.FIELD)
public class SearchRetrieveResponse {

    @XmlElement(namespace = "http://www.loc.gov/zing/srw/")
    private String version;

    @XmlElement(namespace = "http://www.loc.gov/zing/srw/")
    private Integer numberOfRecords;

    @XmlElement(namespace = "http://www.loc.gov/zing/srw/")
    private String resultSetId;

    @XmlElement(namespace = "http://www.loc.gov/zing/srw/")
    private Records records;

    public ResponseRecord getFirstRecord() {
        if (records != null && records.getRecord() != null && !records.getRecord().isEmpty()) {
            RecordWrapper wrapper = records.getRecord().get(0);
            if (wrapper != null && wrapper.getRecordData() != null) {
                return wrapper.getRecordData().getResponseRecord();
            }
        }
        return null;
    }
}
