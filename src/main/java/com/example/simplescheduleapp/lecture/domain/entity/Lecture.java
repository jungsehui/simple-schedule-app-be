package com.example.simplescheduleapp.lecture.domain.entity;

import com.example.simplescheduleapp.schedule.domain.entity.Schedule;
import com.example.simplescheduleapp.tutor.domain.entity.Tutor;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Table(name = "lecture")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
public class Lecture extends Schedule {

    @ManyToOne
    @JoinColumn(name = "tutor_id")
    private Tutor tutor;

    @OneToMany(mappedBy = "lecture", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LectureEnrollment> lectureEnrollments;

    public Lecture(String title, LocalDateTime startTime, LocalDateTime endTime, String memo) {
        super(title, startTime, endTime, memo);
    }

    public Lecture(Tutor tutor, String title, LocalDateTime startTime, LocalDateTime endTime, String memo) {
        super(title, startTime, endTime, memo);
        this.tutor = tutor;
    }

    // 강의 콘텐츠를 관리한다
    // 정원과 모집 상태에 따라 수강 신청을 받는다.
    // 수강생과 수강 대기자, 리뷰어 관리한다
    // 강의는 미션과 상품의 단위가 되기도 한다.
}
