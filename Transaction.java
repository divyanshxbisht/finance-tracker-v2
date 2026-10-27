package model;

public abstract class Transaction {
    private final int transactionId;
    private double amount;
    private String description;
    private String category;
    private String date;

    protected Transaction(int transactionId, double amount, String description, String category, String date) {
        this.transactionId = transactionId;
        this.amount = amount;
        this.description = description;
        this.category = category;
        this.date = date;
    }

    public int getTransactionId() {
        return transactionId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public abstract String getType();
}