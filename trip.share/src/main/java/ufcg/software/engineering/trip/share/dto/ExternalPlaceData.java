package ufcg.software.engineering.trip.share.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ExternalPlaceData(
        @JsonProperty("fsq_place_id") String externalId,
        String name,
        String description,
        BigDecimal latitude,
        BigDecimal longitude,
        Location location,
        List<Category> categories,
        Hours hours,
        List<Photo> photos,
        Integer price,
        Double rating,
        String website,
        String tel,
        @JsonProperty("link") String sourceUrl,
        @JsonProperty("date_closed") String dateClosed) {

    public static final String SOURCE = "FOURSQUARE";

    public String source() {
        return SOURCE;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Location(
            String address,
            String locality,
            String region,
            String postcode,
            String country,
            List<String> neighborhood,
            @JsonProperty("formatted_address") String formattedAddress) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Category(
            @JsonProperty("fsq_category_id") String id,
            String name,
            @JsonProperty("short_name") String shortName,
            @JsonProperty("plural_name") String pluralName) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Hours(
            String display,
            @JsonProperty("is_local_holiday") Boolean isLocalHoliday,
            @JsonProperty("open_now") Boolean openNow,
            List<RegularHour> regular) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RegularHour(
            Integer day,
            String open,
            String close) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Photo(
            @JsonProperty("fsq_photo_id") String id,
            String prefix,
            String suffix,
            Integer width,
            Integer height) {

        public String url() {
            if (prefix == null || suffix == null) {
                return null;
            }
            String size = (width != null && height != null && width > 0 && height > 0)
                    ? width + "x" + height
                    : "original";
            return prefix + size + suffix;
        }
    }
}
