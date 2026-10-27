package service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import model.Expense;
import model.Income;
import model.Transaction;
import persistence.TransactionRepository;

public class FinanceManager {
    public static final int MAX_TRANSACTIONS = 500;

    private final TransactionRepository repository;
    private Transaction[] transactions = new Transaction[MAX_TRANSACTIONS];
    private int transactionCount;

    public FinanceManager() {
        this(new TransactionRepository());
    }

    public FinanceManager(TransactionRepository repository) {
        this.repository = repository;
    }

    public Transaction addTransaction(String type, double amount, String description,
                                      String category, String date) {
        validateTransaction(amount, description, category, date);
        if (transactionCount >= MAX_TRANSACTIONS) {
            throw new IllegalStateException("The transaction limit has been reached.");
        }

        int id = 1;
        for (int index = 0; index < transactionCount; index++) {
            id = Math.max(id, transactions[index].getTransactionId() + 1);
        }

        Transaction transaction;
        if ("INCOME".equalsIgnoreCase(type)) {
            transaction = new Income(id, amount, description.trim(), category.trim(), date);
        } else if ("EXPENSE".equalsIgnoreCase(type)) {
            transaction = new Expense(id, amount, description.trim(), category.trim(), date);
        } else {
            throw new IllegalArgumentException("Transaction type must be income or expense.");
        }
        transactions[transactionCount++] = transaction;
        return transaction;
    }

    public void updateTransaction(int id, double amount, String description, String category, String date) {
        Transaction transaction = findTransaction(id);
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction not found.");
        }
        validateTransaction(amount, description, category, date);
        transaction.setAmount(amount);
        transaction.setDescription(description.trim());
        transaction.setCategory(category.trim());
        transaction.setDate(date);
    }

    private void validateTransaction(double amount, String description, String category, String date) {
        if (!Double.isFinite(amount) || amount <= 0) {
            throw new IllegalArgumentException("Amount must be finite and greater than zero.");
        }
        if (description == null || description.trim().isEmpty()
                || category == null || category.trim().isEmpty()) {
            throw new IllegalArgumentException("Description and category are required.");
        }
        try {
            LocalDate.parse(date);
        } catch (DateTimeParseException | NullPointerException exception) {
            throw new IllegalArgumentException("Date must use YYYY-MM-DD format.", exception);
        }
    }

    public Transaction[] getTransactions() {
        return Arrays.copyOf(transactions, transactionCount);
    }

    public int getTransactionCount() {
        return transactionCount;
    }

    public double calculateTotalIncome() {
        return calculateTotalIncomeRecursive(transactions, transactionCount - 1);
    }

    private double calculateTotalIncomeRecursive(Transaction[] entries, int index) {
        if (index < 0) return 0;
        double amount = entries[index].getType().equals("INCOME") ? entries[index].getAmount() : 0;
        return amount + calculateTotalIncomeRecursive(entries, index - 1);
    }

    public double calculateTotalExpenses() {
        return calculateTotalExpensesRecursive(transactions, transactionCount - 1);
    }

    public double calculateBalance() {
        return calculateTotalIncome() - calculateTotalExpenses();
    }

    private double calculateTotalExpensesRecursive(Transaction[] entries, int index) {
        if (index < 0) return 0;
        double amount = entries[index].getType().equals("EXPENSE") ? entries[index].getAmount() : 0;
        return amount + calculateTotalExpensesRecursive(entries, index - 1);
    }

    public BudgetSummary getBudgetSummary(double monthlyBudget, LocalDate today) {
        if (!Double.isFinite(monthlyBudget) || monthlyBudget < 0) {
            throw new IllegalArgumentException("Monthly budget must be zero or greater.");
        }

        double spentToday = 0;
        double spentThisMonth = 0;
        for (Transaction transaction : getTransactions()) {
            if (!transaction.getType().equals("EXPENSE")) continue;
            try {
                LocalDate transactionDate = LocalDate.parse(transaction.getDate());
                if (transactionDate.equals(today)) spentToday += transaction.getAmount();
                if (transactionDate.getYear() == today.getYear()
                        && transactionDate.getMonth() == today.getMonth()) {
                    spentThisMonth += transaction.getAmount();
                }
            } catch (DateTimeParseException exception) {
                // Invalid legacy dates are excluded from date-based totals.
            }
        }

        double currentBalance = calculateTotalIncome() - calculateTotalExpenses();
        boolean hasBudget = monthlyBudget > 0;
        double amountRemaining = hasBudget ? monthlyBudget - spentThisMonth : currentBalance;
        int daysRemaining = today.lengthOfMonth() - today.getDayOfMonth() + 1;
        double spendable = Math.max(0, Math.min(amountRemaining, Math.max(0, currentBalance)));
        double dailyAllowance = spendable / daysRemaining;
        double weeklyAllowance = dailyAllowance * Math.min(7, daysRemaining);

        return new BudgetSummary(hasBudget, hasBudget && monthlyBudget > Math.max(0, currentBalance),
            monthlyBudget, currentBalance, spentToday, amountRemaining,
                dailyAllowance, weeklyAllowance, daysRemaining);
    }

    public void validateMonthlyBudget(double monthlyBudget) {
        if (!Double.isFinite(monthlyBudget) || monthlyBudget < 0) {
            throw new IllegalArgumentException("Enter a valid budget amount of zero or more.");
        }
        double availableBalance = Math.max(0, calculateTotalIncome() - calculateTotalExpenses());
        if (monthlyBudget > availableBalance) {
            throw new IllegalArgumentException(String.format(
                    "Budget cannot exceed your available balance of Rs. %,.2f.", availableBalance));
        }
    }

    public boolean deleteTransaction(int id) {
        for (int index = 0; index < transactionCount; index++) {
            if (transactions[index].getTransactionId() == id) {
                System.arraycopy(transactions, index + 1, transactions, index, transactionCount - index - 1);
                transactions[--transactionCount] = null;
                return true;
            }
        }
        return false;
    }

    public Transaction findTransaction(int id) {
        for (int index = 0; index < transactionCount; index++) {
            if (transactions[index].getTransactionId() == id) return transactions[index];
        }
        return null;
    }

    public boolean saveData() {
        return repository.save(getTransactions());
    }

    public boolean loadData() {
        try {
            Transaction[] loaded = repository.load();
            if (loaded == null || loaded.length > MAX_TRANSACTIONS) return false;
            transactions = new Transaction[MAX_TRANSACTIONS];
            System.arraycopy(loaded, 0, transactions, 0, loaded.length);
            transactionCount = loaded.length;
            return true;
        } catch (IOException exception) {
            return false;
        }
    }
}