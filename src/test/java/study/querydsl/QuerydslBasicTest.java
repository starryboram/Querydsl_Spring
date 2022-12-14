package study.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

/*
 * JPQL 같은 경우에는 파라미터를 BINDING 해줘야함. 하지만 Querydsl 같은 경우에는 자동으로 binding 해줌.
 * JPQL 같은 경우에는 사용자가 실행해서 이 메서드를 호출 했을 때 오류를 발견할 수 있음 -> 런타임 오류(최악의 오류)
 * Querydsl 같은 경우에는 그냥 컴파일 시에 잡힘.
 * */

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;
    JPAQueryFactory queryFactory; // 필드로 빼서 쓸 것을 권장함

    @BeforeEach
    public void before(){ // 테스트 전에 각각을 실행해줌
        queryFactory = new JPAQueryFactory(em); // 리포지토리 작성할때 이렇게 써도 됨. -> 동시성 문제 고민하지 않아도 됨
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

    @Test
    public void startJPQL(){
        //member1을 찾아라.
        Member findMember = em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertThat(findMember.getUsername()).isEqualTo("member1"); // Assertions.~ 쓰는거를 alt + enter => add ~ 클릭하면줄어듦.
    }

    @Test
    public void startQuerydslFirst(){
        // JPAQueryFactory queryFactory = new JPAQueryFactory(em); // entityManager을 같이 넣어줘야 함. -> 얘는 앞으로 뺄 수도 있음(주석처리함)
        QMember m1 = new QMember("m"); // 이렇게 쓰는거는 같은 테이블을 조인해야하는 경우에만 m1이런식으로 별칭 쓰고 그냥 그 외에는 member로 넣어주기
        Member findMember = queryFactory
                .select(m1)
                .from(m1)
                .where(m1.username.eq("member1")) // 파라미터 바인딩 처리
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void startQuerydsl(){
        Member findMember = queryFactory
                .select(member) // static 멤버로 뽑아낼 수 있음 -> 권장하는 방법
                .from(member)
                .where(member.username.eq("member1")) // 파라미터 바인딩 처리
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void search(){
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.eq(10))) //이름이 member1이고,나이가 10인 사람을 조회해
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void searchAndParam(){
        Member findMember = queryFactory
                .selectFrom(member)
                .where(
                        member.username.eq("member1"),
                        member.age.between(10,30) // 근데 얘는 ,null 이렇게 하면 null인애들을 무시함
                ) //이름이 member1이고,나이가 10 ~ 30인 사람을 조회해(and = ,)
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void resultFetch(){
        List<Member> fetch = queryFactory // 검색 결과를 리스트 형식으로(다건) 조회
                .selectFrom(member)
                .fetch();

        Member fetchOne = queryFactory // 검색 결과 단건 조회
                .selectFrom(member)
                .fetchOne();

        Member fetchFirst = queryFactory // 검색 결과 제일 위의 1건 조회
                .selectFrom(member)
                .fetchFirst();

        QueryResults<Member> results = queryFactory
                .selectFrom(member)
                .fetchResults(); // total count를 가져와야 해서 쿼리가 2번 실행됨 -> 성능이 중요할 떄는 사용하면 안된다...?

        results.getTotal();
        List<Member> content = results.getResults();

        long total = queryFactory
                .selectFrom(member)
                .fetchCount(); // 얘는 countquery로 바꿔주는 거를 의미함.
    }

    /* 회원 정렬 순서
    1. 회원 나이 내림차순(desc)
    2. 회원 이름 올림차순(asc)
    3. 단 2에서 회원 이름이 없으면 마지막에 출력(nulls last)
     */
    @Test
    public void sort(){
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast()) // nullFirst도 있음
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);
        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }

    @Test
    public void paging1(){
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1) // offset, limit로 paging을 지원해줌
                .limit(2)
                .fetch();

        assertThat(result.size()).isEqualTo(2);
    }

    public void paging2(){
        QueryResults<Member> queryResults = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1) // offset, limit로 paging을 지원해줌
                .limit(2)
                .fetchResults();

        assertThat(queryResults.getTotal()).isEqualTo(4);
        assertThat(queryResults.getLimit()).isEqualTo(2);
        assertThat(queryResults.getOffset()).isEqualTo(1);
        assertThat(queryResults.getResults().size()).isEqualTo(2);
    } // count 쿼리가 나가고, content 쿼리가 나감.
    // 실무에서는 content 쿼리는 복잡하고, count 쿼리는 단순할 때 쿼리를 따로 작성해야함.

    @Test
    public void aggregation(){
        List<Tuple> result = queryFactory // 이런 집계관련된 부분은 튜플 형태로 조회가 됨
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min() // 실무에서는 이렇게 tuple로 뽑진 않고 DTO로 뽑아내는 방법을 많이 씀.
                )
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);

    }
    //group by 예제
    // 팀의 이름과 각 팀의 평균 연령을 구해라.
    @Test
    public void group() throws Exception{
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15); // (10+20)/2
        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35); // (30+40)/2
    }

    // 팀A에 소속된 모든 회원
    @Test
    public void join(){
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");
    }

    // 참고로 연관관계 없는 것끼리도 조인을 할 수가 있음.
    // 회원의 이름이 팀 이름과 같은 회원을 조회하기
    @Test
    public void theta_join(){
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Member> result = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB"); // theta join 같은 경우에는 외부 조인이 안됨 -> on을 사용하면 사용 가능
    }

    /**
     * 예) 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
     * JPQL: SELECT m, t FROM Member m LEFT JOIN m.team t on t.name = 'teamA'
     * SQL: SELECT m.*, t.* FROM Member m LEFT JOIN Team t ON m.TEAM_ID=t.id and
     t.name='teamA'
     */
    @Test
    public void join_on_filtering() throws Exception {
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team)
                .on(team.name.eq("teamA"))
                // .where(team.name.eq("teamA")) on대신 join 쓰고 where쓸 수도 있음.(웬만하면 where쓰세요)
                .fetch();
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }
    /* 결과
    t=[Member(id=3, username=member1, age=10), Team(id=1, name=teamA)]
    t=[Member(id=4, username=member2, age=20), Team(id=1, name=teamA)]
    t=[Member(id=5, username=member3, age=30), null]
    t=[Member(id=6, username=member4, age=40), null]
     */

    /**
     * 2. 연관관계 없는 엔티티 외부 조인
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
                .leftJoin(team).on(member.username.eq(team.name)) // 마구잡이로 넣을 때는 그냥 team으로 조인에다 넣음(매칭하는게 없으니까 이렇게 넣음)
                .fetch(); // join으로 하면 null인거는 안나옴!
        for (Tuple tuple : result) {
            System.out.println("t=" + tuple);
        }
    }
    /* 결과
    t=[Member(id=3, username=member1, age=10), null]
    t=[Member(id=4, username=member2, age=20), null]
    t=[Member(id=5, username=member3, age=30), null]
    t=[Member(id=6, username=member4, age=40), null]
    t=[Member(id=7, username=teamA, age=0), Team(id=1, name=teamA)]
    t=[Member(id=8, username=teamB, age=0), Team(id=2, name=teamB)]
    */

    @PersistenceUnit // 증명할 때 사용함
    EntityManagerFactory emf;
    // 영속성 컨텍스트에 내용이 제대로 안 지워져 있으면 결과를 제대로 보기 어려움
    @Test
    public void fetchJoinNo(){
        em.flush();
        em.clear();

        Member findMember = queryFactory // fetch _ lazy로 설정해놨으니까 member로 조회됨.
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());//로딩된 entity인지 초기화 안 된 entity인지 알려줌
        assertThat(loaded).as("페치 조인 미적용").isFalse();
    }

    @Test
    public void fetchJoinUse(){
        em.flush();
        em.clear();

        Member findMember = queryFactory // fetch _ lazy로 설정해놨으니까 member로 조회됨.
                .selectFrom(member) // member을 조회하는데, 연관된 애를 다 끌어오고 싶을때 join 씀
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());//로딩된 entity인지 초기화 안 된 entity인지 알려줌
        assertThat(loaded).as("페치 조인 미적용").isTrue();
    }
    // 나이가 가장 많은 회원 조회
    @Test
    public void subQuery(){
        // member가 겹치면 안되기 때문에 QMember로 따로 생성해줌
        QMember memberSub = new QMember("memberSub");
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        JPAExpressions // 서브쿼리 쓰는 방법
                                .select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(40);
    }

    // 나이가 평균 이상인 회원 조회
    @Test
    public void subQueryGoe(){
        // member가 겹치면 안되기 때문에 QMember로 따로 생성해줌
        QMember memberSub = new QMember("memberSub");
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        JPAExpressions // 서브쿼리 쓰는 방법
                                .select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(30, 40);
    }

    // 나이가 평균 이상인 회원 조회(In 사용해보기)
    @Test
    public void subQueryIn(){
        // member가 겹치면 안되기 때문에 QMember로 따로 생성해줌
        QMember memberSub = new QMember("memberSub");
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        JPAExpressions // 서브쿼리 쓰는 방법
                                .select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(20, 30, 40);
    }

    @Test
    public void selectSubQuery(){
        QMember memberSub = new QMember("memberSub");

        List<Tuple> result = queryFactory
                .select(member.username,
                        JPAExpressions // static으로 뽑을 수 있음
                                .select(memberSub.age.avg())
                                .from(memberSub))
                .from(member)
                .fetch();

        for (Tuple tuple : result){
            System.out.println("tuple = " + tuple);
        }
    }
    // from절의 서브쿼리 한계: jpa jpql 서브쿼리의 한계로 from절의 서브쿼리는 지원하지 않음.(querydsl도 지원하지 않음)
    // 하이버네이트 구현체를 사용하면 select절의 서브쿼리는 지원
    // querydsl도 하이버네이트 구현체를 사용하면 select절의 서브쿼리 지원

    /* 서브쿼리 해결 방안
    1. 서브쿼리를 join으로 변경( case by case )
    2. 애플리케이션에서 쿼리를 2번 분리해서 실행
    3. nativeSQL 사용
     */

    @Test // 간단한 예제
    public void basicCase(){
        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();
        for (String s: result){
            System.out.println("s= " +s);
        }
    }

    @Test // 복잡한 예제
    public void complexCase(){
        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 30)).then("21~30살")
                        .otherwise("기타"))
                .from(member)
                .fetch();
        for (String s: result){
            System.out.println("s= " +s);
        }
    }
    /* 가급적 DB에서 건들지 말자 -> Application logic에서 처리하자 10살 20살~~ */

    // 상수가 필요할 때
    @Test
    public void constant(){
        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();

        for(Tuple tuple : result){
            System.out.println("tuple = " + tuple);
        }
    }
    // 문자 + 숫자 더할 때 -> {username}_{age} 이렇게 쓰고싶을때
    // enum같은 경우에도 값이 안나오니까 그때도 stringValue 이용하면 됨
    @Test
    public void concat(){
        List<String> result = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue())) // string value는 숫자를 스트링으로 변환
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();

        for(String s : result){
            System.out.println("s = " + s);
        }
    }
}

