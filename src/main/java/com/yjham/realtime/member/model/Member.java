package com.yjham.realtime.member.model;

import com.yjham.realtime.member.dto.MemberUpdateRequest;
import com.yjham.realtime.member.model.vo.Address;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Embedded
    private Address address;

    public Member(String email, String password, String name, Address address) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.address = address;
    }

    public void update(MemberUpdateRequest request) {
        if (request.getEmail() != null) {
            this.email = request.getEmail();
        }

        if (request.getPassword() != null) {
            this.password = request.getPassword();
        }

        if (request.getName() != null) {
            this.name = request.getName();
        }

        if (request.getAddress() != null) {
            this.address = Address.builder()
                    .state(request.getAddress().getState())
                    .city(request.getAddress().getCity())
                    .district(request.getAddress().getDistrict())
                    .detail(request.getAddress().getDetail())
                    .build();
        }
    }
}
