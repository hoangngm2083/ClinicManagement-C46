package com.clinic.c46.CommonService.event.auth;


import lombok.Builder;

@Builder
public record PhoneVerifiedEvent(

        String causalId, String phone

) {
}
