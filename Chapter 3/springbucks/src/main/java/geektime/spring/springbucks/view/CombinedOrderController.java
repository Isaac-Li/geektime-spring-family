package geektime.spring.springbucks.view;

import geektime.spring.springbucks.model.*;
import geektime.spring.springbucks.repository.CoffeeOrderRepository;
import geektime.spring.springbucks.repository.TeaOrderRepository;
import geektime.spring.springbucks.service.CoffeeOrderService;
import geektime.spring.springbucks.service.TeaOrderService;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/combined-orders")
public class CombinedOrderController {

    @Autowired
    private CoffeeOrderService coffeeOrderService;
    @Autowired
    private TeaOrderService teaOrderService;
    @Autowired
    private CoffeeOrderRepository coffeeOrderRepository;
    @Autowired
    private TeaOrderRepository teaOrderRepository;

    // 同时下单咖啡和茶水的请求体
    public static class CombinedOrderRequest {
        private String customer;
        private List<Long> coffeeIds;
        private List<Long> teaIds;

        // Getters and Setters
        public String getCustomer() {
            return customer;
        }

        public void setCustomer(String customer) {
            this.customer = customer;
        }

        public List<Long> getCoffeeIds() {
            return coffeeIds;
        }

        public void setCoffeeIds(List<Long> coffeeIds) {
            this.coffeeIds = coffeeIds;
        }

        public List<Long> getTeaIds() {
            return teaIds;
        }

        public void setTeaIds(List<Long> teaIds) {
            this.teaIds = teaIds;
        }
    }

    // 同时下单咖啡和茶水的响应体
    public static class CombinedOrderResponse {
        private CoffeeOrder coffeeOrder;
        private TeaOrder teaOrder;

        public CombinedOrderResponse(CoffeeOrder coffeeOrder, TeaOrder teaOrder) {
            this.coffeeOrder = coffeeOrder;
            this.teaOrder = teaOrder;
        }

        // Getters and Setters
        public CoffeeOrder getCoffeeOrder() {
            return coffeeOrder;
        }

        public void setCoffeeOrder(CoffeeOrder coffeeOrder) {
            this.coffeeOrder = coffeeOrder;
        }

        public TeaOrder getTeaOrder() {
            return teaOrder;
        }

        public void setTeaOrder(TeaOrder teaOrder) {
            this.teaOrder = teaOrder;
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public CombinedOrderResponse createCombinedOrder(@RequestBody CombinedOrderRequest request) {
        CoffeeOrder coffeeOrder = null;
        TeaOrder teaOrder = null;

        // 创建咖啡订单（如果有咖啡ID）
        if (request.getCoffeeIds() != null && !request.getCoffeeIds().isEmpty()) {
            List<Coffee> coffees = new ArrayList<>();
            for (Long coffeeId : request.getCoffeeIds()) {
                coffees.add(Coffee.builder().id(coffeeId).build()); // 假设咖啡存在
            }
            coffeeOrder = coffeeOrderService.createOrder(request.getCustomer(), coffees.toArray(new Coffee[0]));
        }

        // 创建茶水订单（如果有茶水ID）
        if (request.getTeaIds() != null && !request.getTeaIds().isEmpty()) {
            List<Tea> teas = new ArrayList<>();
            for (Long teaId : request.getTeaIds()) {
                teas.add(Tea.builder().id(teaId).build()); // 假设茶水存在
            }
            teaOrder = teaOrderService.createOrder(request.getCustomer(), teas.toArray(new Tea[0]));
        }

        return new CombinedOrderResponse(coffeeOrder, teaOrder);
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('USER')")
    public CombinedOrderResponse searchCombinedOrders(
            @RequestParam(required = false) String customer,
            @RequestParam(required = false) OrderState state) {
        ExampleMatcher matcher = ExampleMatcher.matching()
                .withIgnoreCase()
                .withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING);

        // 搜索咖啡订单
        CoffeeOrder coffeeProbe = CoffeeOrder.builder()
                .customer(customer)
                .state(state)
                .build();
        List<CoffeeOrder> coffeeOrders = coffeeOrderRepository.findAll(Example.of(coffeeProbe, matcher));

        // 搜索茶水订单
        TeaOrder teaProbe = TeaOrder.builder()
                .customer(customer)
                .state(state)
                .build();
        List<TeaOrder> teaOrders = teaOrderRepository.findAll(Example.of(teaProbe, matcher));

        // 返回第一个匹配的咖啡订单和第一个匹配的茶水订单
        CoffeeOrder coffeeOrder = coffeeOrders.isEmpty() ? null : coffeeOrders.get(0);
        TeaOrder teaOrder = teaOrders.isEmpty() ? null : teaOrders.get(0);

        return new CombinedOrderResponse(coffeeOrder, teaOrder);
    }
}