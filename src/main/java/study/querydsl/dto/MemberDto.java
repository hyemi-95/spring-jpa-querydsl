package study.querydsl.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
public class MemberDto {

    private String username;
    private int age;

    @QueryProjection // 결과 반환이 QueryProjection 일 때( DTO도 Q파일로 생성 됨 : QMemberDto)
    public MemberDto(String username, int age) {
        this.username = username;
        this.age = age;
    }
}
