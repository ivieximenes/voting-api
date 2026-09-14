package com.sicredi.voting.web.v1;

import com.sicredi.voting.domain.Topic;
import com.sicredi.voting.service.TopicService;
import com.sicredi.voting.web.dto.CreateTopicRequest;
import com.sicredi.voting.web.dto.TopicResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/topics")
@Tag(name = "Topics", description = "Assembly topics management")
public class TopicController {

    private final TopicService topicService;

    public TopicController(TopicService topicService) {
        this.topicService = topicService;
    }

    @PostMapping
    public ResponseEntity<TopicResponse> create(@Valid @RequestBody CreateTopicRequest request) {
        Topic topic = topicService.create(request.title(), request.description());
        URI location = URI.create("/api/v1/topics/" + topic.getId());
        return ResponseEntity.created(location).body(TopicResponse.from(topic));
    }

    @GetMapping("/{id}")
    public TopicResponse find(@PathVariable Long id) {
        return TopicResponse.from(topicService.findById(id));
    }

    @GetMapping
    public List<TopicResponse> list() {
        return topicService.findAll().stream().map(TopicResponse::from).toList();
    }
}