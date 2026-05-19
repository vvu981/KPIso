package com.kpiso.api.testsupport;

import com.kpiso.api.modules.house.House;
import com.kpiso.api.modules.house.HouseMember;
import com.kpiso.api.modules.house.HouseRole;
import com.kpiso.api.modules.user.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public final class TestFixtures {

    private TestFixtures() {
    }

    public static User user(String username, String email, String password) {
        return User.builder()
                .id(UUID.randomUUID())
                .username(username)
                .email(email)
                .password(password)
                .profilePictureUrl(null)
                .build();
    }

    public static User user(String username, String email) {
        return user(username, email, "encoded-password");
    }

    public static House house(String name, String inviteCode) {
        return House.builder()
                .id(UUID.randomUUID())
                .name(name)
                .inviteCode(inviteCode)
                .profilePictureUrl(null)
                .deletedAt(null)
                .build();
    }

    public static HouseMember houseMember(House house, User user, HouseRole role, String color) {
        return HouseMember.builder()
                .id(UUID.randomUUID())
                .house(house)
                .user(user)
                .role(role)
                .color(color)
                .build();
    }

    public static LocalDateTime localDateTime(int year, int month, int day, int hour, int minute) {
        return LocalDateTime.of(year, month, day, hour, minute);
    }

    public static BigDecimal money(String amount) {
        return new BigDecimal(amount);
    }
}