package au.org.raid.api.dto.isni;

import au.org.raid.api.client.isni.dto.ResponseRecord;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@XmlAccessorType(XmlAccessType.FIELD)
public class RecordData {
    private ResponseRecord responseRecord;
}
