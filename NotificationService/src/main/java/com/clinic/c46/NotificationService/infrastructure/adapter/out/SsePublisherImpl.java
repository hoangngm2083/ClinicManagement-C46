package com.clinic.c46.NotificationService.infrastructure.adapter.out;

import com.clinic.c46.NotificationService.application.port.out.SSEPublisher;
import com.clinic.c46.NotificationService.domain.projection.NotificationProjection;
import org.springframework.stereotype.Component;


@Component
public class SsePublisherImpl implements SSEPublisher {
    @Override
    public void publish(NotificationProjection notification) {

    }

//    private final Map<String, Sinks.Many<ServerSentEvent<NotificationDTO>>> userEmitters = new ConcurrentHashMap<>();
//
//    public Flux<ServerSentEvent<NotificationDTO>> subscribe(String userId) {
//        var sink = userEmitters.computeIfAbsent(userId, id -> Sinks.many().multicast().onBackpressureBuffer());
//        return sink.asFlux();
//    }
//
//    public void sendToUser(String userId, NotificationDTO notification) {
//        var sink = userEmitters.get(userId);
//        if (sink != null) {
//            sink.tryEmitNext(ServerSentEvent.builder(notification).build());
//        }
//    }
}
