package com.yjham.realtime.member.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.Email;

@Getter
@Setter
public class MemberUpdateRequest {
    
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    private String password;

    private String name;

    private AddressRequest address;
    
    @Getter
    @Builder
    public static class AddressRequest {
        private String state;

        private String city;

        private String district;

        private String detail;
    }
} 