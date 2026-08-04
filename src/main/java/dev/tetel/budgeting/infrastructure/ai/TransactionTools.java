package dev.tetel.budgeting.infrastructure.ai;

import dev.tetel.budgeting.application.ListTransactionsByCategoryInput;
import dev.tetel.budgeting.application.ListTransactionsByCategoryUseCase;
import dev.tetel.budgeting.application.PersistTransactionInput;
import dev.tetel.budgeting.application.PersistTransactionUseCase;
import dev.tetel.budgeting.application.TransactionOutput;
import dev.tetel.budgeting.domain.Category;
import dev.tetel.budgeting.domain.TransactionSource;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * The only surface the LLM can reach. Every method here treats its String/long arguments as
 * untrusted model output: they are parsed defensively and never thrown back into the model as
 * exceptions. Identity ({@code ownerUsername}) and the idempotency key ({@code requestId}) are
 * never accepted as tool arguments - they are read from {@link ToolContext}, which is populated
 * exclusively by {@code VoiceAssistantOrchestrationService} from the authenticated HTTP principal
 * and a server-generated UUID, so the model can never choose or override them.
 *
 * <p>These two tools - persist and list - are the entire allowlist. There is no delete, no raw
 * SQL, no shell, no filesystem, no arbitrary URL fetch, and no way to read the API key: none of
 * that capability exists anywhere for the model to call.
 */
@Component
public class TransactionTools {

    private final PersistTransactionUseCase persistTransactionUseCase;
    private final ListTransactionsByCategoryUseCase listTransactionsByCategoryUseCase;

    public TransactionTools(PersistTransactionUseCase persistTransactionUseCase,
            ListTransactionsByCategoryUseCase listTransactionsByCategoryUseCase) {
        this.persistTransactionUseCase = Objects.requireNonNull(persistTransactionUseCase);
        this.listTransactionsByCategoryUseCase = Objects.requireNonNull(listTransactionsByCategoryUseCase);
    }

    @Tool(name = "persistTransaction",
            description = "Records a new financial transaction for the authenticated user. "
                    + "Use this when the user describes a purchase or expense they made.")
    public String persistTransaction(
            @ToolParam(description = "Short description of the purchase, e.g. 'Grocery shopping'") String description,
            @ToolParam(description = "Amount in integer cents, e.g. 6000 for 60.00") long amountInCents,
            @ToolParam(description = "One of: GROCERIES, PHARMA, AUTO, RESTAURANT, ENTERTAINMENT, OTHER") String category,
            ToolContext toolContext) {

        Category parsedCategory = parseCategory(category);
        if (parsedCategory == null) {
            return "Could not record the transaction: unknown category '" + category + "'.";
        }

        try {
            TransactionOutput output = persistTransactionUseCase.persist(new PersistTransactionInput(
                    description,
                    amountInCents,
                    parsedCategory,
                    TransactionSource.VOICE,
                    ownerUsername(toolContext),
                    requestId(toolContext)));
            return "Recorded transaction: " + output.description() + ", " + output.amount() + " (" + output.category() + ").";
        } catch (IllegalArgumentException ex) {
            return "Could not record the transaction: the provided data is invalid.";
        }
    }

    @Tool(name = "listTransactionsByCategory",
            description = "Lists the authenticated user's past transactions filtered by category. "
                    + "Use this when the user asks about their spending history.")
    public String listTransactionsByCategory(
            @ToolParam(description = "One of: GROCERIES, PHARMA, AUTO, RESTAURANT, ENTERTAINMENT, OTHER") String category,
            ToolContext toolContext) {

        Category parsedCategory = parseCategory(category);
        if (parsedCategory == null) {
            return "Could not list transactions: unknown category '" + category + "'.";
        }

        List<TransactionOutput> results = listTransactionsByCategoryUseCase.list(
                new ListTransactionsByCategoryInput(ownerUsername(toolContext), parsedCategory));

        if (results.isEmpty()) {
            return "No transactions found for category " + parsedCategory + ".";
        }
        return results.stream()
                .map(t -> t.description() + ": " + t.amount())
                .collect(Collectors.joining("; "));
    }

    private static Category parseCategory(String rawCategory) {
        if (rawCategory == null) {
            return null;
        }
        try {
            return Category.valueOf(rawCategory.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static String ownerUsername(ToolContext toolContext) {
        return trustedValue(toolContext, "ownerUsername");
    }

    private static String requestId(ToolContext toolContext) {
        return trustedValue(toolContext, "requestId");
    }

    private static String trustedValue(ToolContext toolContext, String key) {
        Object value = toolContext.getContext().get(key);
        if (!(value instanceof String stringValue) || stringValue.isBlank()) {
            throw new IllegalStateException("Trusted tool context is missing '" + key + "'");
        }
        return stringValue;
    }
}
