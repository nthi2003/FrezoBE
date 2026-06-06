package com.frezo.qtht.controller;

import com.frezo.common.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test-ws")
@RequiredArgsConstructor
public class TestWebSocketController {

    private final NotificationService notificationService;

    @PostMapping("/topic/{topicName}")
    public String testTopic(@PathVariable String topicName, @RequestBody String message) {
        notificationService.sendToTopic("/topic/" + topicName, message);
        return "Sent to /topic/" + topicName;
    }

    @PostMapping("/user/{username}/{destination}")
    public String testUser(@PathVariable String username, @PathVariable String destination, @RequestBody String message) {
        notificationService.sendToUser(username, "/" + destination, message);
        return "Sent to /user/" + username + "/" + destination;
    }
}
