package au.org.raid.api.factory.datacite;

import au.org.raid.api.model.datacite.doi.DataciteDate;
import au.org.raid.idl.raidv2.model.Date;
import org.springframework.stereotype.Component;

@Component
public class DataciteDateFactory {

    public DataciteDate create(final Date date) {
        var d = date.getStartDate();

        if (date.getEndDate() != null) {
            d = d.concat("/").concat(date.getEndDate());
        }

        return new DataciteDate()
                .setDate(d)
                .setDateType("Other");
    }
}
