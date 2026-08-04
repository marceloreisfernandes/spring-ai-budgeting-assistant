package dev.tetel.budgeting.infrastructure.web;

import dev.tetel.budgeting.application.ListTransactionsByCategoryInput;
import dev.tetel.budgeting.application.ListTransactionsByCategoryUseCase;
import dev.tetel.budgeting.application.PersistTransactionInput;
import dev.tetel.budgeting.application.PersistTransactionUseCase;
import dev.tetel.budgeting.application.TransactionOutput;
import dev.tetel.budgeting.domain.Category;
import dev.tetel.budgeting.domain.TransactionSource;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Traditional, non-AI REST endpoints for transactions. Both handlers derive {@code source},
 * {@code ownerUsername}, and {@code requestId} from trusted server context (never from the
 * request body), and delegate all validation and persistence to the application use cases.
 */
@RestController
@RequestMapping("/api/v1/transactions")
class TransactionController {

    private final PersistTransactionUseCase persistTransactionUseCase;
    private final ListTransactionsByCategoryUseCase listTransactionsByCategoryUseCase;

    TransactionController(PersistTransactionUseCase persistTransactionUseCase,
            ListTransactionsByCategoryUseCase listTransactionsByCategoryUseCase) {
        this.persistTransactionUseCase = Objects.requireNonNull(persistTransactionUseCase);
        this.listTransactionsByCategoryUseCase = Objects.requireNonNull(listTransactionsByCategoryUseCase);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    TransactionOutput create(@Valid @RequestBody CreateTransactionRequest request, Authentication authentication) {
        PersistTransactionInput input = new PersistTransactionInput(
                request.description(),
                request.amountInCents(),
                request.category(),
                TransactionSource.REST,
                authentication.getName(),
                UUID.randomUUID().toString());
        return persistTransactionUseCase.persist(input);
    }

    @GetMapping
    List<TransactionOutput> list(@RequestParam Category category, Authentication authentication) {
        return listTransactionsByCategoryUseCase.list(
                new ListTransactionsByCategoryInput(authentication.getName(), category));
    }
}
