package com.example.simplescheduleapp.lecture.application;

import com.example.simplescheduleapp.lecture.domain.repository.LectureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class LectureService {

    private final LectureRepository lectureRepository;

    public Long createLecture() {
        return null;
    }
}
