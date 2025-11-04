package geektime.spring.springbucks.view;

import geektime.spring.springbucks.model.OrderState;
import geektime.spring.springbucks.model.TeaOrder;
import geektime.spring.springbucks.repository.TeaOrderRepository;
import geektime.spring.springbucks.service.TeaOrderService;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/tea-orders")
public class TeaOrderController {

    @Autowired
    private TeaOrderRepository teaOrderRepository;
    @Autowired
    private TeaOrderService teaOrderService;

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public List<TeaOrder> getAllTeaOrders() {
        return teaOrderRepository.findAll();
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('USER')")
    public List<TeaOrder> searchTeaOrders(
            @RequestParam(required = false) String customer,
            @RequestParam(required = false) OrderState state) {
        ExampleMatcher matcher = ExampleMatcher.matching()
                .withIgnoreCase()
                .withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING);

        TeaOrder probe = TeaOrder.builder()
                .customer(customer)
                .state(state)
                .build();

        return teaOrderRepository.findAll(Example.of(probe, matcher));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public Optional<TeaOrder> getTeaOrderById(@PathVariable Long id) {
        return teaOrderRepository.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public TeaOrder createTeaOrder(@RequestBody TeaOrder teaOrder) {
        return teaOrderService.createOrder(
                teaOrder.getCustomer(),
                teaOrder.getItems().toArray(new geektime.spring.springbucks.model.Tea[0])
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public TeaOrder updateTeaOrder(@PathVariable Long id, @RequestBody TeaOrder teaOrderDetails) {
        return teaOrderRepository.findById(id)
                .map(teaOrder -> {
                    teaOrder.setCustomer(teaOrderDetails.getCustomer());
                    teaOrder.setItems(teaOrderDetails.getItems());
                    teaOrder.setState(teaOrderDetails.getState());
                    return teaOrderRepository.save(teaOrder);
                })
                .orElseGet(() -> {
                    teaOrderDetails.setId(id);
                    return teaOrderService.createOrder(
                            teaOrderDetails.getCustomer(),
                            teaOrderDetails.getItems().toArray(new geektime.spring.springbucks.model.Tea[0])
                    );
                });
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteTeaOrder(@PathVariable Long id) {
        teaOrderRepository.deleteById(id);
    }

    @PutMapping("/{id}/state")
    @PreAuthorize("hasRole('ADMIN')")
    public boolean updateTeaOrderState(@PathVariable Long id, @RequestBody OrderState state) {
        Optional<TeaOrder> teaOrder = teaOrderRepository.findById(id);
        return teaOrder.map(order -> teaOrderService.updateState(order, state)).orElse(false);
    }
}