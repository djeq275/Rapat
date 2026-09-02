package com.example.vibe1.meeting.telegram;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.modulith.test.EnableScenarios;
import org.springframework.modulith.test.Scenario;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.example.vibe1.division.Division;
import com.example.vibe1.division.DivisionRepository;
import com.example.vibe1.meeting.CreateMeetingCommand;
import com.example.vibe1.meeting.MeetingService;
import com.example.vibe1.telegram.MeetingTelegramGroupRepository;
import com.example.vibe1.telegram.TelegramGateway;
import com.example.vibe1.telegram.TelegramGroup;
import com.example.vibe1.telegram.TelegramGroupRepository;
import com.example.vibe1.telegram.TelegramSendException;
import com.example.vibe1.telegram.TelegramSendStatus;
import com.example.vibe1.user.Role;
import com.example.vibe1.user.User;
import com.example.vibe1.user.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * Proves the whole cross-module round trip actually wired up end to end:
 * MeetingService.create() -> MeetingScheduledEvent published (durably, via
 * Modulith's event publication registry) -> meeting.telegram's internal
 * listener picks it up -> telegram.MeetingTelegramNotificationService calls
 * the (mocked) TelegramGateway -> the MeetingTelegramGroup row is updated
 * with the real send outcome. The listener runs async/after-commit (see
 * ApplicationModuleListener), so this needs a real transactional context and
 * Scenario's polling, not a plain synchronous assertion.
 */
@Testcontainers
@SpringBootTest
@EnableScenarios
class MeetingTelegramNotificationIntegrationTest {

    @Container
    @ServiceConnection
    static final MariaDBContainer<?> mariaDb = new MariaDBContainer<>("mariadb:11");

    @MockitoBean
    TelegramGateway telegramGateway;

    @Autowired
    MeetingService meetingService;
    @Autowired
    DivisionRepository divisionRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    TelegramGroupRepository telegramGroupRepository;
    @Autowired
    MeetingTelegramGroupRepository meetingTelegramGroupRepository;

    Division division;
    User ketua;
    TelegramGroup group;

    @BeforeEach
    void seed() {
        division = divisionRepository.save(new Division("Engineering " + System.nanoTime()));

        ketua = new User();
        ketua.setEmail("ketua-" + System.nanoTime() + "@company.local");
        ketua.setFullName("Ketua Divisi Test");
        ketua.setRole(Role.KETUA_DIVISI);
        ketua.setDivision(division);
        ketua = userRepository.save(ketua);

        group = telegramGroupRepository.save(new TelegramGroup("Grup Engineering", "-100" + System.nanoTime()));
    }

    private CreateMeetingCommand command() {
        Instant start = Instant.now();
        return new CreateMeetingCommand(
                "Rapat Mingguan", null, null, start, start.plus(1, ChronoUnit.HOURS),
                division.getId(), ketua.getId(), List.of(), List.of(group.getId()));
    }

    @Test
    void gatewaySuccessMarksRowSent(Scenario scenario) {
        AtomicLong meetingId = new AtomicLong();

        scenario.stimulate(() -> meetingId.set(meetingService.create(command()).getId()))
                .andWaitForStateChange(() -> meetingTelegramGroupRepository.findByMeetingId(meetingId.get()),
                        rows -> !rows.isEmpty() && rows.get(0).getSendStatus() != TelegramSendStatus.PENDING)
                .andVerify(rows -> {
                    assertThat(rows).hasSize(1);
                    assertThat(rows.get(0).getSendStatus()).isEqualTo(TelegramSendStatus.SENT);
                    assertThat(rows.get(0).getSentAt()).isNotNull();
                });

        verify(telegramGateway).sendMessage(eq(group.getChatId()), any());
    }

    @Test
    void gatewayFailureMarksRowFailedWithErrorMessage(Scenario scenario) {
        doThrow(new TelegramSendException("chat not found"))
                .when(telegramGateway).sendMessage(eq(group.getChatId()), any());

        AtomicLong meetingId = new AtomicLong();

        scenario.stimulate(() -> meetingId.set(meetingService.create(command()).getId()))
                .andWaitForStateChange(() -> meetingTelegramGroupRepository.findByMeetingId(meetingId.get()),
                        rows -> !rows.isEmpty() && rows.get(0).getSendStatus() != TelegramSendStatus.PENDING)
                .andVerify(rows -> {
                    assertThat(rows).hasSize(1);
                    assertThat(rows.get(0).getSendStatus()).isEqualTo(TelegramSendStatus.FAILED);
                    assertThat(rows.get(0).getSendError()).isEqualTo("chat not found");
                });
    }
}
