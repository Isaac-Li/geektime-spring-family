package geektime.spring.springbucks.view;

import geektime.spring.springbucks.model.Tea;
import geektime.spring.springbucks.repository.TeaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/teas")
public class TeaController {

    @Autowired
    private TeaRepository teaRepository;

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public List<Tea> getAllTeas() {
        return teaRepository.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public Optional<Tea> getTeaById(@PathVariable Long id) {
        return teaRepository.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Tea createTea(@RequestBody Tea tea) {
        return teaRepository.save(tea);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Tea updateTea(@PathVariable Long id, @RequestBody Tea teaDetails) {
        return teaRepository.findById(id)
                .map(tea -> {
                    tea.setName(teaDetails.getName());
                    tea.setPrice(teaDetails.getPrice());
                    return teaRepository.save(tea);
                })
                .orElseGet(() -> {
                    teaDetails.setId(id);
                    return teaRepository.save(teaDetails);
                });
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteTea(@PathVariable Long id) {
        teaRepository.deleteById(id);
    }
}