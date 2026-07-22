package com.frezo.crm.service;

import com.frezo.crm.dto.EmailSequenceEnrollRequest;
import com.frezo.crm.dto.EmailSequenceEnrollmentResponse;
import com.frezo.crm.dto.EmailSequenceRequest;
import com.frezo.crm.dto.EmailSequenceResponse;

import java.util.List;

public interface EmailSequenceService {
    List<EmailSequenceResponse> list();
    EmailSequenceResponse create(EmailSequenceRequest req);
    EmailSequenceResponse update(String id, EmailSequenceRequest req);
    EmailSequenceEnrollmentResponse enroll(String sequenceId, EmailSequenceEnrollRequest req);
    void processDueSteps();
}
