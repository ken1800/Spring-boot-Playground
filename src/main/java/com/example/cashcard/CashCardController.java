package com.example.cashcard;

import org.slf4j.LoggerFactory;

import java.net.URI;
import java.security.Principal;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.bind.annotation.PutMapping;

@RestController
@RequestMapping("/cashcards")
class CashCardController {
    private static final Logger log = LoggerFactory.getLogger(CashCardController.class);
    private final CashCardRepository cardRepository;

    private CashCardController(CashCardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    @GetMapping("/{id}")
    private ResponseEntity<CashCard> findCardById(@PathVariable Long id, Principal principal) {
        log.debug("findCardById() invoked: " + id);
        Optional<CashCard> cashCardOptional = Optional
                .ofNullable(cardRepository.findByIdAndOwner(id, principal.getName()));
        if (cashCardOptional.isPresent()) {
            log.debug("Cash card found: " + cashCardOptional.get());
            return ResponseEntity.ok(cashCardOptional.get());
        }

        log.debug("Cash card not found: " + id);
        return ResponseEntity.notFound().build();
    }

    @PostMapping()
    public ResponseEntity<CashCard> createCashCard(@RequestBody CashCard cashCard, UriComponentsBuilder ucb,
            Principal principal) {
        log.debug("createCashCard() invoked: {}", cashCard);
        CashCard cashCardWithOwner = new CashCard(null, cashCard.amount(), principal.getName());
        CashCard savedCard = cardRepository.save(cashCardWithOwner);
        URI location = ucb.path("/cashcards/{id}").buildAndExpand(savedCard.id()).toUri();
        return ResponseEntity.created(location).body(savedCard);
    }

    @GetMapping
    private ResponseEntity<List<CashCard>> findAll(Pageable pageable, Principal principal) {
        Page<CashCard> page = cardRepository.findByOwner(principal.getName(),
                PageRequest.of(
                        pageable.getPageNumber(),
                        pageable.getPageSize(),
                        pageable.getSortOr(Sort.by(Sort.Direction.ASC, "amount"))));
        return ResponseEntity.ok(page.getContent());
    }

    @PutMapping("/{id}")
    public ResponseEntity<CashCard> updateCashCard(@PathVariable Long id, @RequestBody CashCard cashCard,
            Principal principal) {
        log.debug("updateCashCard() invoked: {}", cashCard);
        CashCard existingCard = cardRepository.findByIdAndOwner(id, principal.getName());
        if (existingCard != null) {
            CashCard updatedCard = new CashCard(existingCard.id(), cashCard.amount(), principal.getName());
            cardRepository.save(updatedCard);
            return ResponseEntity.noContent().build();
        }
        log.debug("Cash card not found: " + id);
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCashCard(@PathVariable Long id, Principal principal) {
        log.debug("deleteCashCard() invoked: {}", id);
        if (cardRepository.existsByIdAndOwner(id, principal.getName())) {
            cardRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
