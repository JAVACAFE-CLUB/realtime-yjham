package com.yjham.realtime.member.dto;

import com.yjham.realtime.member.model.Member;
import com.yjham.realtime.member.model.vo.Address;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
public class MemberCreateRequest {
    
    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;
    
    @NotBlank(message = "비밀번호는 필수입니다.")
    private String password;
    
    @NotBlank(message = "이름은 필수입니다.")
    private String name;
    
    @NotNull(message = "주소는 필수입니다.")
    private AddressRequest address;
    
    public Member toEntity() {
        Address address = Address.builder()
                .state(this.address.getState())
                .city(this.address.getCity())
                .district(this.address.getDistrict())
                .detail(this.address.getDetail())
                .build();
        
        return new Member(this.email, this.password, this.name, address);
    }
    
    @Getter
    @Setter
    public static class AddressRequest {
        @NotBlank(message = "시/도는 필수입니다.")
        private String state;
        
        @NotBlank(message = "시/군/구는 필수입니다.")
        private String city;
        
        @NotBlank(message = "동/읍/면은 필수입니다.")
        private String district;
        
        @NotBlank(message = "상세주소는 필수입니다.")
        private String detail;
    }
} 