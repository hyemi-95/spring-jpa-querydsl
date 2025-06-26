package study.querydsl.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;

@Data
public class MemberTeamDto {

    private Long memberId;
    private String username;
    private int age;

    private Long teamId;
    private String teamName;

    @QueryProjection //DTO가 Querydsl을 의존하게 됨, 해당방식 외에  `Projection.bean(), fields(), constructor()` 사용할 수 있음
    public MemberTeamDto(Long memberId, String username, int age, Long teamId, String teamName) {
        this.memberId = memberId;
        this.username = username;
        this.age = age;
        this.teamId = teamId;
        this.teamName = teamName;
    }
}
