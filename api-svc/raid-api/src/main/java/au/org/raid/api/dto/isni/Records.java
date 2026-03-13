package au.org.raid.api.dto.isni;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@XmlAccessorType(XmlAccessType.FIELD)
public class Records {
    @XmlElement(name = "record", namespace = "http://www.loc.gov/zing/srw/")
    private List<RecordWrapper> record;
}