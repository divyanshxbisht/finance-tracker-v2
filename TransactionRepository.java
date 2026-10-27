package persistence;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import model.Expense;
import model.Income;
import model.RecurringExpense;
import model.Transaction;

public class TransactionRepository {
    private final Path file;

    public TransactionRepository() {
        this(Path.of("data", "transactions.txt"));
    }

    public TransactionRepository(Path file) {
        this.file = file;
    }

    public boolean save(Transaction[] transactions) {
        Path parent = file.toAbsolutePath().getParent();
        Path temporaryFile = null;
        try {
            Files.createDirectories(parent);
            temporaryFile = Files.createTempFile(parent, "transactions-", ".tmp");
            try (BufferedWriter writer = Files.newBufferedWriter(temporaryFile, StandardCharsets.UTF_8)) {
                for (Transaction transaction : transactions) {
                    boolean recurring = transaction instanceof RecurringExpense;
                    String type = recurring ? "RECURRING_EXPENSE" : transaction.getType();
                    String frequency = recurring ? ((RecurringExpense) transaction).getFrequency() : "";
                    writer.write(String.join("|", "v2", String.valueOf(transaction.getTransactionId()), type,
                            String.valueOf(transaction.getAmount()), encode(transaction.getDescription()),
                            encode(transaction.getCategory()), transaction.getDate(), encode(frequency)));
                    writer.write(System.lineSeparator());
                }
            }
            try {
                Files.move(temporaryFile, file, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporaryFile, file, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException exception) {
            if (temporaryFile != null) {
                try {
                    Files.deleteIfExists(temporaryFile);
                } catch (IOException ignored) {
                    // Preserve the original data file if cleanup fails.
                }
            }
            return false;
        }
    }

    public Transaction[] load() throws IOException {
        if (!Files.exists(file)) return null;
        List<Transaction> transactions = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isEmpty()) continue;
                try {
                    transactions.add(parseTransaction(line));
                } catch (IllegalArgumentException exception) {
                    throw new IOException("Invalid transaction at line " + lineNumber + ".", exception);
                }
            }
        }
        return transactions.toArray(new Transaction[0]);
    }

    private Transaction parseTransaction(String line) {
        String[] parts = line.split("\\|", -1);
        if (parts.length == 8 && parts[0].equals("v2")) {
            int id = Integer.parseInt(parts[1]);
            double amount = Double.parseDouble(parts[3]);
            String description = decode(parts[4]);
            String category = decode(parts[5]);
            String type = parts[2];
            validateLoadedValues(id, amount);
            if (type.equalsIgnoreCase("INCOME")) {
                return new Income(id, amount, description, category, parts[6]);
            }
            if (type.equalsIgnoreCase("RECURRING_EXPENSE")) {
                return new RecurringExpense(id, amount, description, category, parts[6], decode(parts[7]));
            }
            if (type.equalsIgnoreCase("EXPENSE")) {
                return new Expense(id, amount, description, category, parts[6]);
            }
            throw new IllegalArgumentException("Unknown transaction type.");
        }
        if (parts.length != 6) throw new IllegalArgumentException("Unrecognized transaction format.");

        int id = Integer.parseInt(parts[0]);
        double amount = Double.parseDouble(parts[2]);
        validateLoadedValues(id, amount);
        if (parts[1].equalsIgnoreCase("INCOME")) {
            return new Income(id, amount, parts[3], parts[4], parts[5]);
        }
        if (parts[1].equalsIgnoreCase("EXPENSE")) {
            return new Expense(id, amount, parts[3], parts[4], parts[5]);
        }
        throw new IllegalArgumentException("Unknown transaction type.");
    }

    private void validateLoadedValues(int id, double amount) {
        if (id <= 0 || !Double.isFinite(amount) || amount <= 0) {
            throw new IllegalArgumentException("Invalid transaction ID or amount.");
        }
    }

    private String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String decode(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }
}