package com.frezo.qtht.controller;

import com.frezo.common.constant.WebSocketChannels;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/websocket-channels")
public class WebSocketChannelController {

    @GetMapping
    public Map<String, List<WebSocketChannels.ChannelInfo>> listAllChannels() {
        return WebSocketChannels.getChannels();
    }
}
