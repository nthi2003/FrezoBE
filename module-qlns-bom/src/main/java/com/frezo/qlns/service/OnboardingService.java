package com.frezo.qlns.service;

import com.frezo.qlns.dto.request.OnboardingAssignRequest;
import com.frezo.qlns.dto.request.OnboardingTemplateRequest;
import com.frezo.qlns.dto.response.OnboardingAssignmentResponse;
import com.frezo.qlns.dto.response.OnboardingTemplateResponse;

import java.util.List;

public interface OnboardingService {
    List<OnboardingTemplateResponse> listTemplates();
    OnboardingTemplateResponse createTemplate(OnboardingTemplateRequest req);
    OnboardingTemplateResponse updateTemplate(String id, OnboardingTemplateRequest req);
    List<OnboardingAssignmentResponse> listAssignments(String personId);
    OnboardingAssignmentResponse assign(OnboardingAssignRequest req);
    OnboardingAssignmentResponse completeItem(String assignmentId, String itemId);
}
