package iuh.fit.se.sebook_backend.controller;

import iuh.fit.se.sebook_backend.dto.AddToCartRequest;
import iuh.fit.se.sebook_backend.dto.CartDTO;
import iuh.fit.se.sebook_backend.dto.UpdateCartRequest;
import iuh.fit.se.sebook_backend.service.CartService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
public class CartController {
    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ResponseEntity<CartDTO> getCart() {
        return ResponseEntity.ok(cartService.getCart());
    }

    @PostMapping("/add")
    public ResponseEntity<String> addToCart(@RequestBody AddToCartRequest request) {
        cartService.addToCart(request);
        return ResponseEntity.ok("Book added to cart successfully");
    }

    @PutMapping("/items/{cartItemId}")
    public ResponseEntity<String> updateCartItem(@PathVariable Long cartItemId, @RequestBody UpdateCartRequest request) {
        cartService.updateCartItem(cartItemId, request);
        return ResponseEntity.ok("Cart item updated successfully");
    }

    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<String> removeCartItem(@PathVariable Long cartItemId) {
        cartService.removeCartItem(cartItemId);
        return ResponseEntity.ok("Cart item removed successfully");
    }
}