package geektime.spring.springbucks.service;

import geektime.spring.springbucks.model.Tea;
import geektime.spring.springbucks.repository.TeaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static org.springframework.data.domain.ExampleMatcher.GenericPropertyMatchers.exact;

@Slf4j
@Service
public class TeaService {
    @Autowired
    private TeaRepository teaRepository;

    public Optional<Tea> findOneTea(String name) {
        ExampleMatcher matcher = ExampleMatcher.matching()
                .withMatcher("name", exact().ignoreCase());
        Optional<Tea> tea = teaRepository.findOne(
                Example.of(Tea.builder().name(name).build(), matcher));
        log.info("Tea Found: {}", tea);
        return tea;
    }
}