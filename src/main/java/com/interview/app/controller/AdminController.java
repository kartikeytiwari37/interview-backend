package com.interview.app.controller;

import com.interview.app.dto.UserDto;
import com.interview.app.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class AdminController {

    private final UserService userService;

    @PostMapping("/promote/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> promoteToAdmin(@PathVariable String userId) {
        try {
            UserDto promotedUser = userService.promoteToAdmin(userId);
            log.info("User {} promoted to admin by admin", promotedUser.getEmail());
            return ResponseEntity.ok(promotedUser);
        } catch (Exception e) {
            log.error("Error promoting user to admin: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(new AuthController.ApiResponse(false, e.getMessage()));
        }
    }

    // Endpoint to promote by email (easier to use)
    @PostMapping("/promote-by-email")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> promoteToAdminByEmail(@RequestBody PromoteRequest request) {
        try {
            // Find user by email first
            var user = userService.findByEmail(request.getEmail());
            if (user.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(new AuthController.ApiResponse(false, "User not found with email: " + request.getEmail()));
            }
            
            UserDto promotedUser = userService.promoteToAdmin(user.get().getId());
            log.info("User {} promoted to admin by admin", promotedUser.getEmail());
            return ResponseEntity.ok(promotedUser);
        } catch (Exception e) {
            log.error("Error promoting user to admin: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(new AuthController.ApiResponse(false, e.getMessage()));
        }
    }

    // Simple DTO for promote request
    public static class PromoteRequest {
        private String email;
        
        public String getEmail() {
            return email;
        }
        
        public void setEmail(String email) {
            this.email = email;
        }
    }
}
