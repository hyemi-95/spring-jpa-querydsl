package study.querydsl.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.repository.MemberJpaRepository;
import study.querydsl.repository.MemberRepository;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class Membercontroller {

    private final MemberJpaRepository memberJpaRepository;
    private final MemberRepository memberRepository;

    //순수JPA
    @GetMapping("/v1/members") //예시 : localhost:8080/v1/members?teamName=teamB&ageGoe=31&ageLoe=35
    public List<MemberTeamDto> searchMemberV1(MemberSearchCondition condition){
        return memberJpaRepository.search(condition);
    }

    //querydsl
    @GetMapping("/v2/members")
    public Page<MemberTeamDto> searchMemberV2(MemberSearchCondition condition, Pageable pageable) {//localhost:8080/v2/members?size=5&page=2
        return memberRepository.searchPageSimple(condition, pageable);
    }
}
