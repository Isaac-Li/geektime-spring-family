package geektime.spring.springbucks.repository;

import geektime.spring.springbucks.model.TeaOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeaOrderRepository extends JpaRepository<TeaOrder, Long> {
}