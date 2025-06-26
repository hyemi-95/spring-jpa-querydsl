package study.querydsl.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;

import java.util.List;

public interface MemberRepositoryCustom {//사용자 정의 리포지토리 인터페이스

    List<MemberTeamDto> search(MemberSearchCondition condition);

    //스프링 데이터 페이징 활용1 - Querydsl 페이징 연동
    Page<MemberTeamDto> searchPageSimple(MemberSearchCondition condition, Pageable pageable);//페이징


}
