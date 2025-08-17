package com.interview.app.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
@CompoundIndexes({
    @CompoundIndex(name = "role_active_idx", def = "{'role': 1, 'isActive': 1}"),
    @CompoundIndex(name = "active_created_idx", def = "{'isActive': 1, 'createdAt': -1}"),
    @CompoundIndex(name = "role_lastlogin_idx", def = "{'role': 1, 'lastLogin': -1}")
})
public class User {
    
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String email;
    
    private String password;
    
    private String firstName;
    
    private String lastName;
    
    private UserRole role = UserRole.USER;
    
    private boolean isActive = true;
    
    private LocalDateTime createdAt = LocalDateTime.now();
    
    private LocalDateTime lastLogin;
    
    public enum UserRole {
        USER, ADMIN
    }
    
    public String getFullName() {
        return firstName + " " + lastName;
    }
}
