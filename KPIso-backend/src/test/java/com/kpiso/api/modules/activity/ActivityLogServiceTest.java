package com.kpiso.api.modules.activity;

import com.kpiso.api.modules.activity.dto.ActivityLogResponse;
import com.kpiso.api.modules.house.House;
import com.kpiso.api.modules.user.User;
import com.kpiso.api.testsupport.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivityLogServiceTest {

    @Mock
    private ActivityLogRepository activityLogRepository;

    private ActivityLogService activityLogService;

    private House house;
    private User user;

    @BeforeEach
    void setUp() {
        activityLogService = new ActivityLogService(activityLogRepository);
        house = TestFixtures.house("Mi Piso", "ABC123");
        user = TestFixtures.user("john", "john@email.com");
    }

    @Test
    void logShouldSaveActivity() {
        activityLogService.log("User joined", "JOIN", house, user);

        verify(activityLogRepository).save(any(ActivityLog.class));
    }

    @Test
    void getHouseActivityShouldReturnPagedLogs() {
        ActivityLog logItem = ActivityLog.builder()
                .id(UUID.randomUUID())
                .description("Action desc")
                .actionType("CREATE")
                .user(user)
                .house(house)
                .build();

        Pageable pageable = PageRequest.of(0, 10);
        Page<ActivityLog> pagedLogs = new PageImpl<>(List.of(logItem));

        when(activityLogRepository.findByHouseIdOrderByCreatedAtDesc(house.getId(), pageable)).thenReturn(pagedLogs);

        Page<ActivityLogResponse> response = activityLogService.getHouseActivity(house.getId(), pageable);

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertEquals("Action desc", response.getContent().get(0).getDescription());
        assertEquals("john", response.getContent().get(0).getUsername());
    }

    @Test
    void purgeOldActivityLogsShouldCallRepositoryDelete() {
        activityLogService.purgeOldActivityLogs();

        verify(activityLogRepository).deleteOlderThan(any(LocalDateTime.class));
    }
}
