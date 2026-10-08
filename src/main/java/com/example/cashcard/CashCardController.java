package com.example.cashcard;


import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/cashcards")
class CashCardController {
    private static final Logger log = LoggerFactory.getLogger(CashCardController.class);

    @GetMapping("/{id}")
    private ResponseEntity<CashCard> findCardById(@PathVariable Long id) {
        if(id.equals(99L)) {
            CashCard cashCard = new CashCard(100L, 0.0);
            log.debug("Found cash card with id: {}", cashCard);
            return ResponseEntity.ok(cashCard);
        } else {
            log.debug("Cash card with id {} not found", id);
            return ResponseEntity.notFound().build();
        }
    }
}
