package com.example.cashcard;


import org.slf4j.LoggerFactory;

import java.util.Optional;

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
    private final CashCardRepository cardRepository ;

    private  CashCardController(CashCardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    @GetMapping("/{id}")
    private ResponseEntity<CashCard> findCardById(@PathVariable Long id) {
        log.debug("findCardById() invoked: " + id);
        Optional<CashCard> cashCard = cardRepository.findById(id);
        if(cashCard.isPresent()) {
            log.debug("Cash card found: " + cashCard.get());
            return ResponseEntity.ok(cashCard.get());
        }

        log.debug("Cash card not found: " + id);
        return ResponseEntity.notFound().build();
    }
}
