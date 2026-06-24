package com.frezo.email.service;

import com.frezo.email.dto.response.EmailInboxResponse;

import java.util.List;

public interface EmailInboxService {

    List<EmailInboxResponse> fetchInbox(String configId, int page, int size);

    EmailInboxResponse fetchEmailById(String configId, long uid);

    void markAsRead(String configId, long uid);
}
