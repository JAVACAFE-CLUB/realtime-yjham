package com.yjham.realtime.member.controller;

import com.yjham.realtime.common.response.ApiResponse;
import com.yjham.realtime.member.dto.MemberCreateRequest;
import com.yjham.realtime.member.dto.MemberResponse;
import com.yjham.realtime.member.dto.MemberUpdateRequest;
import com.yjham.realtime.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {
    
    private final MemberService memberService;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MemberResponse>> getMember(@PathVariable Long id) {
        MemberResponse member = memberService.getMember(id);
        return ResponseEntity.ok(ApiResponse.success(member));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MemberResponse>>> getAllMembers() {
        List<MemberResponse> members = memberService.getAllMembers();
        return ResponseEntity.ok(ApiResponse.success(members));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MemberResponse>> createMember(@Valid @RequestBody MemberCreateRequest request) {
        MemberResponse member = memberService.createMember(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "멤버가 성공적으로 생성되었습니다.", member));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MemberResponse>> updateMember(
            @PathVariable Long id,
            @Valid @RequestBody MemberUpdateRequest request) {
        MemberResponse member = memberService.updateMember(id, request);
        return ResponseEntity.ok(ApiResponse.success("멤버가 성공적으로 수정되었습니다.", member));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMember(@PathVariable Long id) {
        memberService.deleteMember(id);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "멤버가 성공적으로 삭제되었습니다.", null));
    }
} 