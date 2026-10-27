package model;

public class RecurringExpense extends Expense {
    private final String frequency;

    public RecurringExpense(int transactionId, double amount, String description, String category,
                            String date, String frequency) {
        super(transactionId, amount, description, category, date);
        this.frequency = frequency;
    }

    public String getFrequency() {
        return frequency;
    }
}