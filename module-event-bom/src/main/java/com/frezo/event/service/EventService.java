package com.frezo.event.service;

import com.frezo.common.response.FePage;
import com.frezo.event.dto.request.EventSaveRequest;
import com.frezo.event.dto.request.RsvpRequest;
import com.frezo.event.dto.response.EventDto;
import com.frezo.event.dto.response.EventRegistrationDto;

import java.util.List;

public interface EventService {

    FePage<EventDto> listAdmin(String status);

    EventDto get(String id);

    EventDto create(EventSaveRequest req);

    EventDto update(String id, EventSaveRequest req);

    void delete(String id);

    EventDto publish(String id);

    EventDto cancel(String id);

    List<EventDto> calendar(String from, String to);

    List<EventRegistrationDto> listRegistrations(String eventId);

    // ---- Portal ----
    FePage<EventDto> listPortal();

    EventDto getPortal(String id);

    EventRegistrationDto rsvp(String eventId, RsvpRequest req);

    void cancelRsvp(String eventId);

    List<EventRegistrationDto> myRegistrations();
}
