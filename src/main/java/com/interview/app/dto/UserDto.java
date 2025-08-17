package com.interview.app.dto;

import com.interview.app.model.User;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    
    private String id;
    private String email;
    private String firstName;
    private String lastName;
    private User.UserRole role;
    private boolean isActive;
    
    public static UserDto fromUser(User user) {
        return new UserDto(
            user.getId(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getRole(),
            user.isActive()
        );
    }
    
    public String getFullName() {
        return firstName + " " + lastName;
    }
}
