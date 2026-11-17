package ufcg.software.engineering.trip.share.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(

        @Size(max = 120) @Pattern(regexp = ".*\\S.*") String name,
        @Size(max = 32) @Pattern(regexp = "^\\+[1-9]\\d{1,14}$") String phone,
        @Size(max = 500) String bio,
        @Size(max = 120) String hometown) {

}
