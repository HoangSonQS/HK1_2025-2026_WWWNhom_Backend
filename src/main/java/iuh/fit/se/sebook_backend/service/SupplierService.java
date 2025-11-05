package iuh.fit.se.sebook_backend.service;

import iuh.fit.se.sebook_backend.dto.SupplierDTO;
import iuh.fit.se.sebook_backend.entity.Supplier;
import iuh.fit.se.sebook_backend.repository.SupplierRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SupplierService {
    private final SupplierRepository supplierRepository;

    public SupplierService(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    public List<SupplierDTO> getAllSuppliers() {
        return supplierRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    public SupplierDTO createSupplier(SupplierDTO dto) {
        Optional<Supplier> existingSupplier = supplierRepository.findByNameIgnoreCase(dto.getName());
        if (existingSupplier.isPresent()) {
            throw new IllegalArgumentException("Nhà cung cấp với tên '" + dto.getName() + "' đã tồn tại.");
        }
        Supplier supplier = new Supplier();
        supplier.setName(dto.getName());
        supplier.setEmail(dto.getEmail());
        supplier.setPhone(dto.getPhone());
        supplier.setAddress(dto.getAddress());
        supplier.setActive(true); // Mặc định là active
        Supplier savedSupplier = supplierRepository.save(supplier);
        return toDto(savedSupplier);
    }

    public SupplierDTO updateSupplier(Long id, SupplierDTO dto) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found"));

        if (!supplier.getName().equalsIgnoreCase(dto.getName())) {
            Optional<Supplier> existingSupplier = supplierRepository.findByNameIgnoreCase(dto.getName());
            if (existingSupplier.isPresent()) {
                throw new IllegalArgumentException("Tên nhà cung cấp '" + dto.getName() + "' đã được sử dụng.");
            }
        }

        supplier.setName(dto.getName());
        supplier.setEmail(dto.getEmail());
        supplier.setPhone(dto.getPhone());
        supplier.setAddress(dto.getAddress());
        supplier.setActive(dto.isActive());
        Supplier updatedSupplier = supplierRepository.save(supplier);
        return toDto(updatedSupplier);
    }

    public void deleteSupplier(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found"));
        supplier.setActive(false);
        supplierRepository.save(supplier);
    }

    private SupplierDTO toDto(Supplier supplier) {
        SupplierDTO dto = new SupplierDTO();
        dto.setId(supplier.getId());
        dto.setName(supplier.getName());
        dto.setEmail(supplier.getEmail());
        dto.setPhone(supplier.getPhone());
        dto.setAddress(supplier.getAddress());
        dto.setActive(supplier.isActive());
        return dto;
    }
}