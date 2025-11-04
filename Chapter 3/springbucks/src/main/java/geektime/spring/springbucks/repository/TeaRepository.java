package geektime.spring.springbucks.repository;

import geektime.spring.springbucks.model.Tea;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeaRepository extends JpaRepository<Tea, Long> {
}