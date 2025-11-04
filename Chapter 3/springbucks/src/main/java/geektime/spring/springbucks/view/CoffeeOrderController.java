package geektime.spring.springbucks.view;

import geektime.spring.springbucks.model.CoffeeOrder;
import geektime.spring.springbucks.model.OrderState;
import geektime.spring.springbucks.repository.CoffeeOrderRepository;
import geektime.spring.springbucks.service.CoffeeOrderService;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/coffee-orders")
public class CoffeeOrderController {

    @Autowired
    private CoffeeOrderRepository coffeeOrderRepository;
    @Autowired
    private CoffeeOrderService coffeeOrderService;

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public List<CoffeeOrder> getAllCoffeeOrders() {
        return coffeeOrderRepository.findAll();
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('USER')")
    public List<CoffeeOrder> searchCoffeeOrders(
            @RequestParam(required = false) String customer,
            @RequestParam(required = false) OrderState state) {
        ExampleMatcher matcher = ExampleMatcher.matching()
                .withIgnoreCase()
                .withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING);

        CoffeeOrder probe = CoffeeOrder.builder()
                .customer(customer)
                .state(state)
                .build();

        return coffeeOrderRepository.findAll(Example.of(probe, matcher));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public Optional<CoffeeOrder> getCoffeeOrderById(@PathVariable Long id) {
        return coffeeOrderRepository.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public CoffeeOrder createCoffeeOrder(@RequestBody CoffeeOrder coffeeOrder) {
        return coffeeOrderService.createOrder(
                coffeeOrder.getCustomer(),
                coffeeOrder.getItems().toArray(new geektime.spring.springbucks.model.Coffee[0])
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CoffeeOrder updateCoffeeOrder(@PathVariable Long id, @RequestBody CoffeeOrder coffeeOrderDetails) {
        return coffeeOrderRepository.findById(id)
                .map(coffeeOrder -> {
                    coffeeOrder.setCustomer(coffeeOrderDetails.getCustomer());
                    coffeeOrder.setItems(coffeeOrderDetails.getItems());
                    coffeeOrder.setState(coffeeOrderDetails.getState());
                    return coffeeOrderRepository.save(coffeeOrder);
                })
                .orElseGet(() -> {
                    coffeeOrderDetails.setId(id);
                    return coffeeOrderService.createOrder(
                            coffeeOrderDetails.getCustomer(),
                            coffeeOrderDetails.getItems().toArray(new geektime.spring.springbucks.model.Coffee[0])
                    );
                });
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteCoffeeOrder(@PathVariable Long id) {
        coffeeOrderRepository.deleteById(id);
    }

    @PutMapping("/{id}/state")
    @PreAuthorize("hasRole('ADMIN')")
    public boolean updateCoffeeOrderState(@PathVariable Long id, @RequestBody OrderState state) {
        Optional<CoffeeOrder> coffeeOrder = coffeeOrderRepository.findById(id);
        return coffeeOrder.map(order -> coffeeOrderService.updateState(order, state)).orElse(false);
    }
}