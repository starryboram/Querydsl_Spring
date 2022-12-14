package study.querydsl.entity;

import lombok.*;

import javax.persistence.*;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 기본 생성자 넣던지 요거 넣던지 해주셈.
@ToString(of = {"id", "username", "age"}) // tostring 형태로 바꿔주는데, 얘는 연관관계있는애들은 해주지 말기(override 에러 남)
public class Member { // shift + ctrl + T 누르고 test만들기

    @Id @GeneratedValue
    @Column(name = "member_id")
    private Long id;
    private String username;
    private int age;

    @ManyToOne(fetch = FetchType.LAZY) // 연관관계 주인이라고 할 수 있음
    @JoinColumn(name = "team_id")
    private Team team;

    // 나중에 쓸거여서 생성자 만드는거임. 실무에서는 쓸것만(필요한 것만)생성자 만들자
    public Member(String username, int age){
        this(username, age, null);
    }

    public Member(String username){
        this(username, 0);
    }

    public Member(String username, int age, Team team) {
        this.username = username;
        this.age = age;
        if (team != null) { // team이 null값이 아니면 team을 바꾸게끔 구현
            changeTeam(team);
        }
    }

    public void changeTeam(Team team){
        this.team = team;
        team.getMembers().add(this); // 양쪽 관계니까, 팀을 바꾸게되면 members에게도 넣어줘야지.
    }
}
