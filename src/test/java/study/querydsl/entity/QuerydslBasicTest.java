package study.querydsl.entity;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {
    @PersistenceContext EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    public void before() {
        queryFactory = new JPAQueryFactory(em);

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    //JPQL: 문자(실행 시점 오류)
    @Test
    public void startJPQL() {
    //member1을 찾아라.
        String qlString =
                "select m from Member m " +
                        "where m.username = :username";
        Member findMember = em.createQuery(qlString, Member.class)
                .setParameter("username", "member1")
                .getSingleResult();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    //Querydsl: 코드(컴파일 시점 오류)
    @Test
    public void startQuerydsl() {
    //member1을 찾아라.
    // JPAQueryFactory queryFactory = new JPAQueryFactory(em); -> 필드로 빼도 됨

//        QMember m = new QMember("m"); //별칭 직접 지정 -> 같은 테이블을 조인해야할때 m / m2이런식으로 이름을 바꿔서 사용하면 됨 그 외에는 그닥..
//        QMember m = QMember.member; //기본 인스턴스 사용
        //또는 아래처럼 쿼리에 직접 QMember.member를 넣고 static import해도됨
        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))//파라미터 바인딩 처리
                .fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    //검색조건
    @Test
    public void search() { //and 또는 or 등 체인을 걸어서 사용할 수 있음
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.eq(10)))
                .fetchOne();//단건 조회 -> 결과없으면 null, 둘 이상이면 excepion발생
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void searchAndParam() {
        List<Member> result1 = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"),
                        member.age.eq(10)) //where() 에 파라미터로 검색조건을 추가하면 `AND` 조건이 추가됨 , 이 경우 `null` 값은 무시
                .fetch();//리스트 조회, 데이터 없으면 빈 리스트 반환
        assertThat(result1.size()).isEqualTo(1);
    }

    //결과조회
    @Test
    public void resultFetch(){
    //List
    List<Member> fetch = queryFactory
            .selectFrom(member)
            .fetch();

    //단 건
    Member findMember1 = queryFactory
            .selectFrom(member)
            .fetchOne();

    //처음 한 건 조회
    Member findMember2 = queryFactory
            .selectFrom(member)
            .fetchFirst(); //limit(1).fetchOne() ->쿼리를 실행해서 첫 번째 결과 하나만 가져옴(하나 이상이면 excepion 발생)

//    //페이징에서 사용
//    QueryResults<Member> results = queryFactory
//            .selectFrom(member)
//            .fetchResults(); //페이징 정보 포함, total count 쿼리 추가 실행
//
//    //count 쿼리로 변경
//    long count = queryFactory
//            .selectFrom(member)
//            .fetchCount(); //count 쿼리로 변경해서 count 수 조회


    //Querydsl 5.0 이상
    //위의 .fetchResults(), .fetchCount() 두개는 Querydsl 5.0 이상에서 deprecated 되었음 실무에서는 아래의 코드가 최신
    Pageable pageable = PageRequest.of(0, 10); // 0번 페이지, 10개씩

    List<Member> content = queryFactory
            .selectFrom(member)
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

    Long total = queryFactory
            .select(member.count())
            .from(member)
            .fetchOne();

    //Page<Member> page = new PageImpl<>(content, pageable, total); => Spring Data에서 "페이지 단위 결과"를 표준 형식으로 다루기 위한 포장(wrapper)

    //Querydsl 5.0 이상 end

    }

    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순(desc)
     * 2. 회원 이름 올림차순(asc)
     * 단 2에서 회원 이름이 없으면 마지막에 출력(nulls last)
     */
    @Test
    public void sort() {
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast()) //`desc()` , `asc()` : 일반 정렬 / nullsLast()` , `nullsFirst()` : null 데이터 순서 부여
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);
        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }

    //페이징
    @Test
    public void paging1() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1) //0부터 시작(zero index)
                .limit(2) //최대 2건 조회
                .fetch();

        assertThat(result.size()).isEqualTo(2);
    }
    
    //전체 조회 수
    @Test
    public void paging2() { //*orderBy/offset/limit 절대 넣지 말기*
        Long total  = queryFactory
                .select(member.count())
                .from(member)
//                .orderBy(member.username.desc())
//                .offset(1) //0부터 시작(zero index)
//                .limit(2) //최대 2건 조회
                .fetchOne();
        assertThat(total).isEqualTo(4);
    }

    //위 페이징과 전체 수 합친 것(실무용)
    @Test
    void pagingQuerydslTest() {
        QMember member = QMember.member;

        Pageable pageable = PageRequest.of(0, 2); // 0페이지, 2건

        // 데이터 목록 쿼리
        List<Member> content = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // Page 객체 생성 (최신 방식)
        Page<Member> result = PageableExecutionUtils.getPage(
                content,
                pageable,
                () -> queryFactory
                        .select(member.count())
                        .from(member)
                        .fetchOne()
        );

        // ✅ 검증
        assertThat(result.getContent()).hasSize(2);          // 2건
        assertThat(result.getTotalElements()).isEqualTo(4);  // 전체 4건
        assertThat(result.getTotalPages()).isEqualTo(2);     // 2페이지 존재
    }

    /** 집합
     * JPQL
     * select
     * COUNT(m), //회원수
     * SUM(m.age), //나이 합
     * AVG(m.age), //평균 나이
     * MAX(m.age), //최대 나이
     * MIN(m.age) //최소 나이
     * from Member m
     */

    //Tuple 사용 이유 : 여러 필드를 반환할 때는 단일 타입 (List<Member> 처럼)으로는 받을 수 없음 ,대신 Querydsl에서 제공하는 Tuple 을 사용해야 함
    @Test
    public void aggregation() throws Exception {
        List<Tuple> result = queryFactory
                .select(member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min())
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);

        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    /** GroupBy 사용
     * 팀의 이름과 각 팀의 평균 연령을 구해라.
     */
    @Test
    public void group() throws Exception { //having도 가능
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);
        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }


    /** 기본조인 //join(조인 대상, 별칭으로 사용할 Q타입)
     * 팀 A에 소속된 모든 회원
     */
    @Test
    public void join() throws Exception {

        QMember member = QMember.member;
        QTeam team = QTeam.team;

        //join, leftJoin, rightJoin 모두 가능
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team) //연관있는 것을 걸고 조인
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");

        //extracting : 리스트 안의 객체들에서 .getUsername() 값을 꺼냄
        //containsExactly : 꺼낸 값들이 순서까지 포함해서 정확히 일치해야 테스트 통과
    }

    /**
     * 세타 조인(연관관계가 없는 필드로 조인) - 외부조인 안됨
     * 회원의 이름이 팀 이름과 같은 회원 조회
     */
    @Test
    public void theta_join() throws Exception {

        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Member> result = queryFactory
                .select(member)
                .from(member, team) //연관없는걸 그냥 나열
                .where(member.username.eq(team.name))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }

    /** 1. ON절을 활용한 조인 - 조인 대상 필터링
     *
     * 예) 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
     * JPQL: SELECT m, t FROM Member m LEFT JOIN m.team t on t.name = 'teamA'
     * SQL: SELECT m.*, t.* FROM Member m LEFT JOIN Team t ON m.TEAM_ID=t.id and t.name='teamA'
     */
    @Test
    public void join_on_filtering() throws Exception {

        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team).on(team.name.eq("teamA"))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }

        //항목                          | `on`                                                           | `where`
        //적용 시점                     | 조인할 때 조건 제한                                               | 조인 다 끝난 후** 필터링
        //SQL 위치                      | `LEFT JOIN ... ON 조건`                                         | `WHERE 조건`
        //외부조인에서의 차이             | 🔸 조건에 맞지 않아도 **왼쪽(member)은 살고**, 오른쪽(team)은 `null` |🔸 조건에 맞지 않으면 **그 행 전체가 버려짐** (→ 내부조인처럼 됨)
        //연관관계 없는 조인에도 사용 가능  | 가능                                                            | ❌ 불가 (join 구문 아님)
    }

    /**
     * 2. ON절을 활용한 조인 - 연관관계 없는 엔티티 외부 조인
     * 예) 회원의 이름과 팀의 이름이 같은 대상 외부 조인
     * JPQL: SELECT m, t FROM Member m LEFT JOIN Team t on m.username = t.name
     * SQL: SELECT m.*, t.* FROM Member m LEFT JOIN Team t ON m.username = t.name
     */
    @Test
    public void join_on_no_relation() throws Exception {

        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team).on(member.username.eq(team.name))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("t=" + tuple);
        }
    }

    //페치 조인 미적용 - 지연로딩으로 Member, Team SQL 쿼리 각각 실행
    @PersistenceUnit
    EntityManagerFactory emf;
    @Test
    public void fetchJoinNo() throws Exception {

        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());// .isLoaded이미 로딩이(초기화) 된 엔티티인지 알려줌

        assertThat(loaded).as("페치 조인 미적용").isFalse();
    }

    //페치 조인 적용 - 즉시로딩으로 Member, Team SQL 쿼리 조인으로 한번에 조회
    @Test
    public void fetchJoinUse() throws Exception {

        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());

        assertThat(loaded).as("페치 조인 적용").isTrue();
    }

    /**
     * eq 사용 //같다
     * 나이가 가장 많은 회원 조회
     */
    @Test
    public void subQuery() throws Exception {
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(40);
    }

    /**
     * goe 사용 //이상
     * 나이가 평균 나이 이상인 회원
     */
    @Test
    public void subQueryGoe() throws Exception {
        QMember memberSub = new QMember("memberSub");
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        JPAExpressions
                                .select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();
        assertThat(result).extracting("age")
                .containsExactly(30,40);
    }

    /**
     * 서브쿼리 여러 건 처리, in 사용
     */
    @Test
    public void subQueryIn() throws Exception {
        QMember memberSub = new QMember("memberSub");
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        JPAExpressions
                                .select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))//gt : 초과
                ))
                .fetch();
        assertThat(result).extracting("age")
                .containsExactly(20, 30, 40);
    }
    /**
     * select 서브쿼리
     * */
    @Test
    public void selectSubQueryIn() throws Exception {

        QMember memberSub = new QMember("memberSub");

        List<Tuple> fetch = queryFactory
                .select(member.username,
                        JPAExpressions
                                .select(memberSub.age.avg())
                                .from(memberSub)
                ).from(member)
                .fetch();

        for (Tuple tuple : fetch) {
            System.out.println("username = " + tuple.get(member.username));
            System.out.println("age = " + tuple.get(JPAExpressions.select(memberSub.age.avg()).from(memberSub)));
        }
    }


    /** case문
     * 단순한 조건
     * */
    @Test
    public void baiscCase() throws Exception {

        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();
    }

    /** case문
     * 복잡한 조건
     * */
    @Test
    public void complexCase() throws Exception {
        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 30)).then("21~30살")
                        .otherwise("기타"))
                .from(member)
                .fetch();
    }

    /**
     * 1. 0 ~ 30살이 아닌 회원을 가장 먼저 출력
     * 2. 0 ~ 20살 회원 출력
     * 3. 21 ~ 30살 회원 출력
     */
    @Test
    public void orderByCase() throws Exception {

        NumberExpression<Integer> rankPath = new CaseBuilder()
                .when(member.age.between(0, 20)).then(2)
                .when(member.age.between(21, 30)).then(1)
                .otherwise(3);

        List<Tuple> result = queryFactory
                .select(member.username, member.age, rankPath)
                .from(member)
                .orderBy(rankPath.desc())
                .fetch();

        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            Integer rank = tuple.get(rankPath);
            System.out.println("username = " + username + " age = " + age + " rank = " +
                    rank);
        }
    }

    /**
     * 상수 :  Expressions.constant 사용
     */
    @Test
    public void constant() throws Exception {
        Tuple result = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetchFirst();

            System.out.println("result = "+result);
    }

    //문자 더하기
    @Test
    public void concat() throws Exception {
        String result = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue())) // stringValue 로 뽑아줘야함 쿼리보면 char로 케스팅 됨을 확인
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        System.out.println("result = "+result);
    }
}

