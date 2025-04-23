package com.example.simplescheduleapp.tutor.domain.entity;

import com.example.simplescheduleapp.consultation.domain.entity.Consultation;
import com.example.simplescheduleapp.lecture.domain.entity.Lecture;
import com.example.simplescheduleapp.member.domain.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.List;

@SQLRestriction("deleted_at is null")
@SQLDelete(sql = "UPDATE tutor SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@PrimaryKeyJoinColumn(name = "member_id")
@Table(name = "tutor")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
public class Tutor extends Member {

    @Column(name = "career_period", nullable = false)
    private int careerPeriod;

    @OneToMany(mappedBy = "tutor", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Lecture> lectures;

    @OneToMany(mappedBy = "tutor", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Consultation> consultations;

    public Tutor(String username, String password, String name, int age, String phoneNumber, int careerPeriod) {
        super(username, password, name, age, phoneNumber);
        this.careerPeriod = careerPeriod;
    }
}
