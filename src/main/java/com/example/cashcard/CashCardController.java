package com.example.cashcard;


import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.bind.annotation.RequestParam;

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

    @PostMapping()
    public ResponseEntity<CashCard> createCashCard(@RequestBody CashCard cashCard, UriComponentsBuilder ucb) {
        log.debug("createCashCard() invoked: {}", cashCard);
        CashCard savedCard = cardRepository.save(cashCard);
        URI location = ucb.path("/cashcards/{id}").buildAndExpand(savedCard.id()).toUri();
        return ResponseEntity.created(location).body(savedCard);
    }

    @GetMapping
    private ResponseEntity<List<CashCard>> findAll(Pageable pageable) {
        Page<CashCard> page = cardRepository.findAll(
                PageRequest.of(
                        pageable.getPageNumber(),
                        pageable.getPageSize(),
                        pageable.getSortOr(Sort.by(Sort.Direction.ASC, "amount"))
                )
        );
        return ResponseEntity.ok(page.getContent());
    }
}
