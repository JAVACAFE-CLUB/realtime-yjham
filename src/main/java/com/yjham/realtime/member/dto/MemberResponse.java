package com.yjham.realtime.member.dto;

import com.yjham.realtime.member.model.Member;
import com.yjham.realtime.member.model.vo.Address;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Builder
public class MemberResponse {
    
    private Long id;
    private String email;
    private String name;
    private AddressResponse address;

    public static MemberResponse toResponse(Member member) {
        Address address = member.getAddress();

        return MemberResponse.builder()
                .id(member.getId())
                .email(member.getEmail())
                .name(member.getName())
                .address(
                        AddressResponse.builder()
                                .state(address.getState())
                                .city(address.getCity())
                                .district(address.getDistrict())
                                .detail(address.getDetail())
                                .build()
                )
                .build();
    }
    
    @Getter
    @Builder
    public static class AddressResponse {
        private String state;
        private String city;
        private String district;
        private String detail;
    }
} 