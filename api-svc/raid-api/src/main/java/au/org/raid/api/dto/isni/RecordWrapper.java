package au.org.raid.api.dto.isni;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
public class RecordWrapper {
    @XmlElement(namespace = "http://www.loc.gov/zing/srw/")
    private String recordSchema;

    @XmlElement(namespace = "http://www.loc.gov/zing/srw/")
    private String recordPacking;

    @XmlElement(namespace = "http://www.loc.gov/zing/srw/")
    private RecordData recordData;

    @XmlElement(namespace = "http://www.loc.gov/zing/srw/")
    private Integer recordPosition;
}
