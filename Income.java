package model;

public class Income extends Transaction {
    public Income(int transactionId, double amount, String description, String category, String date) {
        super(transactionId, amount, description, category, date);
    }

    @Override
    public String getType() {
        return "INCOME";
    }
}