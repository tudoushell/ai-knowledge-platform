package com.elliot.ai.identity.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.elliot.ai.identity.entity.User;
import com.elliot.ai.identity.entity.UserStatus;
import com.elliot.ai.identity.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.mockito.ArgumentCaptor;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserServiceImplTest {

    @BeforeAll
    static void initializeTableMetadata() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), User.class);
    }

    private final UserMapper mapper = mock(UserMapper.class);
    private final UserServiceImpl service = new UserServiceImpl(mapper);

    @Test
    void findsUsersByIdAndHandlesMissingUsers() {
        UUID id = UUID.randomUUID();
        User user = new User();
        when(mapper.selectById(id)).thenReturn(user).thenReturn(null);

        assertSame(user, service.findById(id).orElseThrow());
        assertTrue(service.findById(id).isEmpty());
    }

    @Test
    void usernameQueriesIgnoreCaseAndBindInput() {
        User user = new User();
        when(mapper.selectOne(any(Wrapper.class))).thenReturn(user);
        when(mapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        assertSame(user, service.findByUsername("Alice").orElseThrow());
        assertTrue(service.existsByUsername("alice"));

        ArgumentCaptor<Wrapper<User>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(mapper).selectOne(captor.capture());
        assertTrue(captor.getValue().getSqlSegment().contains("LOWER(username) = LOWER("));
        assertFalse(captor.getValue().getSqlSegment().contains("Alice"));
        assertFalse(service.existsByUsername(" "));
        assertTrue(service.findByUsername(" ").isEmpty());
    }

    @Test
    void createsUserWithGeneratedIdAndTimestamps() {
        User user = new User();
        user.setUsername("Alice");
        user.setNickname("Alice");
        user.setPasswordHash("encoded-password");
        user.setStatus(UserStatus.DISABLED);

        assertSame(user, service.create(user));
        assertNotNull(user.getId());
        assertEquals(UserStatus.DISABLED, user.getStatus());
        assertNotNull(user.getCreatedAt());
        assertEquals(user.getCreatedAt(), user.getUpdatedAt());
        verify(mapper).insert(user);
    }

    @Test
    void updatesStatusAndLoginTimeAndReportsMissingUsers() {
        UUID id = UUID.randomUUID();
        OffsetDateTime loginTime = OffsetDateTime.now().minusMinutes(1);
        when(mapper.update(isNull(), any(Wrapper.class))).thenReturn(1, 0);

        assertTrue(service.updateStatus(id, UserStatus.DISABLED));
        assertFalse(service.updateLastLoginAt(id, loginTime));

        ArgumentCaptor<LambdaUpdateWrapper<User>> captor = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(mapper, times(2)).update(isNull(), captor.capture());
        LambdaUpdateWrapper<User> statusUpdate = captor.getAllValues().get(0);
        assertTrue(statusUpdate.getSqlSegment().contains("id ="));
        assertTrue(statusUpdate.getSqlSet().contains("status="));
        assertTrue(statusUpdate.getSqlSet().contains("updated_at="));
        assertTrue(statusUpdate.getParamNameValuePairs().containsValue(id));
        assertTrue(statusUpdate.getParamNameValuePairs().containsValue(UserStatus.DISABLED));
        LambdaUpdateWrapper<User> loginUpdate = captor.getAllValues().get(1);
        assertTrue(loginUpdate.getSqlSegment().contains("id ="));
        assertTrue(loginUpdate.getSqlSet().contains("last_login_at="));
        assertTrue(loginUpdate.getSqlSet().contains("updated_at="));
        assertTrue(loginUpdate.getParamNameValuePairs().containsValue(id));
        assertTrue(loginUpdate.getParamNameValuePairs().containsValue(loginTime));
    }
}
