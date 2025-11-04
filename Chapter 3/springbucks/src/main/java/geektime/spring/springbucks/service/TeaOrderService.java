package geektime.spring.springbucks.service;

import geektime.spring.springbucks.model.OrderState;
import geektime.spring.springbucks.model.Tea;
import geektime.spring.springbucks.model.TeaOrder;
import geektime.spring.springbucks.repository.TeaOrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.Arrays;

@Slf4j
@Service
@Transactional
public class TeaOrderService {
    @Autowired
    private TeaOrderRepository teaOrderRepository;

    public TeaOrder createOrder(String customer, Tea... tea) {
        if (tea == null || tea.length < 1) {
            log.error("Tea items cannot be empty");
            throw new IllegalArgumentException("Tea items cannot be empty");
        }
        TeaOrder order = TeaOrder.builder()
                .customer(customer)
                .items(new ArrayList<>(Arrays.asList(tea)))
                .state(OrderState.INIT)
                .build();
        TeaOrder saved = teaOrderRepository.save(order);
        log.info("New Tea Order: {}", saved);
        return saved;
    }

    public boolean updateState(TeaOrder order, OrderState state) {
        if (state.compareTo(order.getState()) <= 0) {
            log.warn("Wrong State order: {}, {}", state, order.getState());
            return false;
        }
        order.setState(state);
        teaOrderRepository.save(order);
        log.info("Updated Tea Order: {}", order);
        return true;
    }
}