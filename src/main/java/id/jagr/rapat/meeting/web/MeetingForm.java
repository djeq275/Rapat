package id.jagr.rapat.meeting.web;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MeetingForm {

    private String title;
    private String description;
    private String materialLink;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime startTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime endTime;

    private List<Long> participantUserIds = List.of();
    private List<Long> telegramGroupIds = List.of();
}
