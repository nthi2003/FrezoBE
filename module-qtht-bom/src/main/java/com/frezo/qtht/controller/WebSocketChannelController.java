package com.frezo.qtht.controller;

import com.frezo.common.constant.WebSocketChannels;
import com.frezo.common.security.CheckPermission;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/qtht/websocket-channel")
public class WebSocketChannelController {

    @GetMapping
    @CheckPermission(api = "/qtht/websocket-channel", action = "VIEW")
    public Map<String, List<WebSocketChannels.ChannelInfo>> listAllChannels() {
        return WebSocketChannels.getChannels();
    }
}
