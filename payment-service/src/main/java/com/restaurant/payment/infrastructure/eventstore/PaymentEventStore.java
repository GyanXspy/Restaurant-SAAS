package com.restaurant.payment.infrastructure.eventstore;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.events.DomainEvent;
import com.restaurant.payment.infrastructure.repository.EventStoreRepository;

@Component
public class PaymentEventStore {

    private final EventStoreRepository eventStoreRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public PaymentEventStore(EventStoreRepository eventStoreRepository, ObjectMapper objectMapper) {
        this.eventStoreRepository = eventStoreRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void saveEvents(String aggregateId, List<DomainEvent> events, int expectedVersion) {
        Integer currentVersion = eventStoreRepository.findMaxVersionByAggregateId(aggregateId);
        if (currentVersion == null) {
            currentVersion = 0;
        }

        if (currentVersion != expectedVersion) {
            throw new ConcurrentModificationException(
                String.format("Expected version %d but was %d for aggregate %s", 
                    expectedVersion, currentVersion, aggregateId));
        }

        for (int i = 0; i < events.size(); i++) {
            DomainEvent event = events.get(i);
            try {
                String eventData = objectMapper.writeValueAsString(event);
                EventStoreEntity entity = new EventStoreEntity(
                    aggregateId,
                    event.getClass().getSimpleName(),
                    eventData,
                    currentVersion + i + 1
                );
                eventStoreRepository.save(entity);
            } catch (Exception e) {
                throw new RuntimeException("Failed to serialize event", e);
            }
        }
    }

    public List<DomainEvent> getEvents(String aggregateId) {
        List<EventStoreEntity> entities = eventStoreRepository.findByAggregateIdOrderByEventVersionAsc(aggregateId);
        
        return entities.stream()
            .map(this::deserializeEvent)
            .collect(Collectors.toList());
    }

    private DomainEvent deserializeEvent(EventStoreEntity entity) {
        try {
            Class<?> eventClass = Class.forName("com.restaurant.payment.domain.events." + entity.getEventType());
            return (DomainEvent) objectMapper.readValue(entity.getEventData(), eventClass);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize event", e);
        }
    }

    public static class ConcurrentModificationException extends RuntimeException {
        public ConcurrentModificationException(String message) {
            super(message);
        }
    }
}