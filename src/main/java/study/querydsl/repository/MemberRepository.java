package study.querydsl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import study.querydsl.entity.Member;

import java.util.List;

public interface MemberRepository extends JpaRepository<Member,Long> , MemberRepositoryCustom{ //사용자정의 리포지토리 인터페이스도 함께 상속

    List<Member> findByUsername(String username);
}
