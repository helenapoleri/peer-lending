package representations;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ActiveAuction extends Auction {

    public String company;

    @JsonCreator
    public ActiveAuction(@JsonProperty("value") long value, @JsonProperty("maxRate") float maxRate, @JsonProperty("startingDateTime") String startingDateTime, @JsonProperty("duration") long duration, @JsonProperty("companyName") String company) {
        super(value, maxRate, startingDateTime, duration);
        this.company = company;
    }

    public String getCompany() {
        return this.company;
    }
}