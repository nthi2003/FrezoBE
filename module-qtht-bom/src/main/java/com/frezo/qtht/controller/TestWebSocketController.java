package com.frezo.qtht.controller;

import com.frezo.common.security.CheckPermission;
import com.frezo.common.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/qtht/test-ws")
@RequiredArgsConstructor
public class TestWebSocketController {

    private final NotificationService notificationService;

    @PostMapping("/topic/{topicName}")
    @CheckPermission(api = "/qtht/test-ws/topic/{topicName}", action = "CREATE")
    public String testTopic(@PathVariable String topicName, @RequestBody String message) {
        notificationService.sendToTopic("/topic/" + topicName, message);
        return "Sent to /topic/" + topicName;
    }

    @PostMapping("/user/{username}/{destination}")
    @CheckPermission(api = "/qtht/test-ws/user/{username}/{destination}", action = "CREATE")
    public String testUser(@PathVariable String username, @PathVariable String destination, @RequestBody String message) {
        notificationService.sendToUser(username, "/" + destination, message);
        return "Sent to /user/" + username + "/" + destination;
    }
}
