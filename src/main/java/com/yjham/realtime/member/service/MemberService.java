package com.yjham.realtime.member.service;

import com.yjham.realtime.common.exception.BusinessException;
import com.yjham.realtime.common.exception.ErrorCode;
import com.yjham.realtime.member.dto.MemberCreateRequest;
import com.yjham.realtime.member.dto.MemberResponse;
import com.yjham.realtime.member.dto.MemberUpdateRequest;
import com.yjham.realtime.member.model.Member;
import com.yjham.realtime.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {
    
    private final MemberRepository memberRepository;

    public MemberResponse getMember(Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        
        return MemberResponse.toResponse(member);
    }

    public List<MemberResponse> getAllMembers() {
        List<Member> members = memberRepository.findAll();
        
        return members.stream()
                .map(MemberResponse::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public MemberResponse createMember(MemberCreateRequest request) {
        validateEmail(request.getEmail());
        
        Member member = request.toEntity();
        Member savedMember = memberRepository.save(member);
        
        return MemberResponse.toResponse(savedMember);
    }

    @Transactional
    public MemberResponse updateMember(Long id, MemberUpdateRequest request) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        String afterEmail = request.getEmail();
        String beforeEmail = member.getEmail();

        if (afterEmail != null && !afterEmail.equals(beforeEmail)) {
            validateEmail(afterEmail);
        }
        
        member.update(request);
        
        return MemberResponse.toResponse(member);
    }

    @Transactional
    public void deleteMember(Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        memberRepository.delete(member);
    }

    private void validateEmail(String email) {
        if (memberRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
    }
}