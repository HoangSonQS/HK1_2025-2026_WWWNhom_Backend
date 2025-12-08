package iuh.fit.se.sebook_backend.controller;

import iuh.fit.se.sebook_backend.dto.CustomerDetailDTO;
import iuh.fit.se.sebook_backend.dto.CustomerSummaryDTO;
import iuh.fit.se.sebook_backend.service.CustomerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public ResponseEntity<List<CustomerSummaryDTO>> getCustomers() {
        return ResponseEntity.ok(customerService.getCustomers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerDetailDTO> getCustomerDetail(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.getCustomerDetail(id));
    }
}

