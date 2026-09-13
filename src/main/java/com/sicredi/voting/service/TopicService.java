package com.sicredi.voting.service;

import com.sicredi.voting.domain.Topic;
import com.sicredi.voting.repository.TopicRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class TopicService {

    private final TopicRepository topicRepository;

    public TopicService(TopicRepository topicRepository) {
        this.topicRepository = topicRepository;
    }

    @Transactional
    public Topic create(String title, String description) {
        var topic = new Topic(title, description);
        return topicRepository.save(topic);
    }

    @Transactional(readOnly = true)
    public Topic findById(Long id) {
        return topicRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Pauta não encontrada: id=" + id));
    }

    @Transactional(readOnly = true)
    public List<Topic> findAll() {
        return topicRepository.findAll();
    }
}