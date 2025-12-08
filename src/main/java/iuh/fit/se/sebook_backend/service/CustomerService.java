package iuh.fit.se.sebook_backend.service;

import iuh.fit.se.sebook_backend.dto.*;
import iuh.fit.se.sebook_backend.entity.Account;
import iuh.fit.se.sebook_backend.entity.Order;
import iuh.fit.se.sebook_backend.repository.AccountRepository;
import iuh.fit.se.sebook_backend.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@SuppressWarnings("DataFlowIssue")
public class CustomerService {

    private final AccountRepository accountRepository;
    private final OrderRepository orderRepository;
    public CustomerService(AccountRepository accountRepository,
                           OrderRepository orderRepository) {
        this.accountRepository = accountRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public List<CustomerSummaryDTO> getCustomers() {
        List<Account> customers = accountRepository.findCustomers();
        return customers.stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CustomerDetailDTO getCustomerDetail(Long customerId) {
        Account account = accountRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        CustomerSummaryDTO summary = toSummary(account);
        List<Order> orders = orderRepository.findByAccountIdWithDetails(customerId);
        List<OrderDTO> orderDTOs = orders.stream()
                .map(this::mapOrderToDto)
                .collect(Collectors.toList());

        return CustomerDetailDTO.builder()
                .summary(summary)
                .orders(orderDTOs)
                .build();
    }

    @SuppressWarnings("DataFlowIssue")
    private CustomerSummaryDTO toSummary(Account account) {
        Long customerIdObj = Objects.requireNonNull(account.getId(), "Customer id is null");
        long customerId = customerIdObj.longValue();
        long totalOrders = orderRepository.countByAccountId(customerId);
        double totalSpending = orderRepository.sumTotalAmountByAccountAndStatus(customerId, Order.COMPLETED);
        var lastOrderDate = orderRepository.findLastOrderDateByAccountId(customerId);

        String fullName = String.format("%s %s",
                account.getFirstName() != null ? account.getFirstName() : "",
                account.getLastName() != null ? account.getLastName() : "").trim();
        if (fullName.isEmpty()) {
            fullName = account.getUsername();
        }

        return CustomerSummaryDTO.builder()
                .id(account.getId())
                .fullName(fullName)
                .email(account.getEmail())
                .phoneNumber(account.getPhoneNumber())
                .totalOrders(totalOrders)
                .totalSpending(totalSpending)
                .lastOrderDate(lastOrderDate)
                .build();
    }

    private OrderDTO mapOrderToDto(Order order) {
        var detailDTOs = order.getOrderDetails().stream()
                .map(detail -> OrderDetailDTO.builder()
                        .bookId(detail.getBook().getId())
                        .bookTitle(detail.getBook().getTitle())
                        .bookImageUrl(detail.getBook().getImageUrl())
                        .quantity(detail.getQuantity())
                        .priceAtPurchase(detail.getPriceAtPurchase())
                        .build())
                .collect(Collectors.toList());

        double subtotal = order.getOrderDetails().stream()
                .mapToDouble(d -> d.getPriceAtPurchase() * d.getQuantity())
                .sum();
        double discountAmount = subtotal - order.getTotalAmount();

        PromotionInfoDTO promotionInfo = null;
        if (order.getAppliedPromotion() != null) {
            var promo = order.getAppliedPromotion();
            promotionInfo = PromotionInfoDTO.builder()
                    .code(promo.getCode())
                    .name(promo.getName())
                    .discountPercent(promo.getDiscountPercent())
                    .build();
        }

        AddressDTO addressDTO = null;
        if (order.getDeliveryAddress() != null) {
            var addr = order.getDeliveryAddress();
            addressDTO = AddressDTO.builder()
                    .id(addr.getId())
                    .addressType(addr.getAddressType())
                    .isDefault(addr.isDefault())
                    .street(addr.getStreet())
                    .ward(addr.getWard())
                    .district(addr.getDistrict())
                    .city(addr.getCity())
                    .phoneNumber(addr.getPhoneNumber())
                    .recipientName(addr.getRecipientName())
                    .build();
        }

        Account account = order.getAccount();
        CustomerInfoDTO customerInfo = CustomerInfoDTO.builder()
                .firstName(account.getFirstName())
                .lastName(account.getLastName())
                .email(account.getEmail())
                .phoneNumber(account.getPhoneNumber())
                .build();

        return OrderDTO.builder()
                .id(order.getId())
                .orderDate(order.getOrderDate())
                .totalAmount(order.getTotalAmount())
                .subtotal(subtotal)
                .discountAmount(discountAmount)
                .status(order.getStatus())
                .paymentMethod(order.getPaymentMethod())
                .deliveryAddress(addressDTO)
                .appliedPromotion(promotionInfo)
                .customerInfo(customerInfo)
                .orderDetails(detailDTOs)
                .build();
    }
}

