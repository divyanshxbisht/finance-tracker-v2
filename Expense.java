package model;

public class Expense extends Transaction {
    public Expense(int transactionId, double amount, String description, String category, String date) {
        super(transactionId, amount, description, category, date);
    }

    @Override
    public String getType() {
        return "EXPENSE";
    }
}