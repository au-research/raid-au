package raido.apisvc.spring.bean;

import org.springframework.stereotype.Component;

/**
 This is about centralising log statements that are used by AWS logs 
 insights queries (worst. product name. ever. 🙄)
 The idea is that centralising the messages here will remind and 
 encourage people to think them to think the flow on effects of changing these.
 
 So: developers - don't change these messages unnecessarily, unless you are the 
 person who will deal with the broken metrics and changed observability 
 characteristics in production.
 
 Why are we doing this instead of using a metrics package?  
 - Simplicity
 - Ease of initial implementation
 - Implementing metrics via logging usually results in better observability,
   at least at first
 - Avoid binding Raido into AWS metrics stuff
 */
@Component
public class MetricBean {

  public static final String APIDS_ADD_URL_VALUE = "APIDS addValue";
  public static final String APIDS_MINT_WITH_DESC = "APIDS mint with desc";
  public static final String VALIDATE_ORCID_EXISTS = "validate ORCID exists";
  public static final String VALIDATE_ROR_EXISTS = "validate ROR exists";
  public static final String VALIDATE_DOI_EXISTS = "validate DOI exists";

}
