package iuh.fit.se.sebook_backend.service;

import iuh.fit.se.sebook_backend.dto.*;
import iuh.fit.se.sebook_backend.entity.Account;
import iuh.fit.se.sebook_backend.entity.Book;
import iuh.fit.se.sebook_backend.entity.Cart;
import iuh.fit.se.sebook_backend.repository.BookRepository;
import iuh.fit.se.sebook_backend.repository.CartRepository;
import iuh.fit.se.sebook_backend.utils.SecurityUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CartService {
    private final CartRepository cartRepository;
    private final BookRepository bookRepository;
    private final SecurityUtil securityUtil;

    public CartService(CartRepository cartRepository, BookRepository bookRepository, SecurityUtil securityUtil) {
        this.cartRepository = cartRepository;
        this.bookRepository = bookRepository;
        this.securityUtil = securityUtil;
    }

    public CartDTO getCart() {
        Account currentUser = securityUtil.getLoggedInAccount();
        List<Cart> cartItems = cartRepository.findByAccountId(currentUser.getId());

        List<CartItemDTO> items = cartItems.stream().map(cart -> CartItemDTO.builder()
                .cartItemId(cart.getId())
                .bookId(cart.getBook().getId())
                .bookTitle(cart.getBook().getTitle())
                .bookImageUrl(cart.getBook().getImageUrl())
                .bookPrice(cart.getBook().getPrice())
                .quantity(cart.getQuantity())
                .build()).collect(Collectors.toList());

        double totalPrice = items.stream()
                .mapToDouble(item -> item.getBookPrice() * item.getQuantity())
                .sum();

        return CartDTO.builder()
                .items(items)
                .totalPrice(totalPrice)
                .build();
    }

    public void addToCart(AddToCartRequest request) {
        Account currentUser = securityUtil.getLoggedInAccount();
        Book book = bookRepository.findById(request.getBookId())
                .orElseThrow(() -> new IllegalArgumentException("Book not found"));

        if (book.getQuantity() < request.getQuantity()) {
            throw new IllegalStateException("Not enough stock available");
        }

        cartRepository.findByAccountIdAndBookId(currentUser.getId(), request.getBookId())
                .ifPresentOrElse(
                        cartItem -> {
                            int newQuantity = cartItem.getQuantity() + request.getQuantity();
                            if (book.getQuantity() < newQuantity) {
                                throw new IllegalStateException("Not enough stock available");
                            }
                            cartItem.setQuantity(newQuantity);
                            cartRepository.save(cartItem);
                        },
                        () -> {
                            Cart newCartItem = new Cart();
                            newCartItem.setAccount(currentUser);
                            newCartItem.setBook(book);
                            newCartItem.setQuantity(request.getQuantity());
                            cartRepository.save(newCartItem);
                        }
                );
    }

    public void updateCartItem(Long cartItemId, UpdateCartRequest request) {
        Cart cartItem = cartRepository.findById(cartItemId)
                .orElseThrow(() -> new IllegalArgumentException("Cart item not found"));

        if (cartItem.getBook().getQuantity() < request.getQuantity()) {
            throw new IllegalStateException("Not enough stock available");
        }
        cartItem.setQuantity(request.getQuantity());
        cartRepository.save(cartItem);
    }

    public void removeCartItem(Long cartItemId) {
        if (!cartRepository.existsById(cartItemId)) {
            throw new IllegalArgumentException("Cart item not found");
        }
        cartRepository.deleteById(cartItemId);
    }
}