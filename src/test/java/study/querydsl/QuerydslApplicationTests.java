package study.querydsl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Hello;
import study.querydsl.entity.QHello;

import javax.persistence.EntityManager;

@SpringBootTest
@Transactional
@Commit
class QuerydslApplicationTests {

	@Autowired // @PersistContext 는 예전버전
	EntityManager em;

	@Test
	void contextLoads() {
		Hello hello = new Hello();
		em.persist(hello); // hello entity 저장

		// query를 쓰기 위해서는 jpaqueryfactory를 써야함.
		JPAQueryFactory query = new JPAQueryFactory(em);
		QHello qHello = new QHello("h");

		// ctrl + alt + v
		Hello result = query
				.selectFrom(qHello)
				.fetchOne();

		Assertions.assertThat(result).isEqualTo(hello); //쿼리dsl 잘 되는지 확인용
		Assertions.assertThat(result.getId()).isEqualTo(hello.getId()); // lombok잘 되는지 확인용
	}

}
