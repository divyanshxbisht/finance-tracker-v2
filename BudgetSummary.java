package service;

public class BudgetSummary {
    private final boolean hasMonthlyBudget;
    private final boolean balanceLimited;
    private final double monthlyBudget;
    private final double currentBalance;
    private final double spentToday;
    private final double amountRemaining;
    private final double dailyAllowance;
    private final double weeklyAllowance;
    private final int daysRemaining;

    public BudgetSummary(boolean hasMonthlyBudget, boolean balanceLimited, double monthlyBudget,
                         double currentBalance, double spentToday,
                         double amountRemaining, double dailyAllowance, double weeklyAllowance,
                         int daysRemaining) {
        this.hasMonthlyBudget = hasMonthlyBudget;
        this.balanceLimited = balanceLimited;
        this.monthlyBudget = monthlyBudget;
        this.currentBalance = currentBalance;
        this.spentToday = spentToday;
        this.amountRemaining = amountRemaining;
        this.dailyAllowance = dailyAllowance;
        this.weeklyAllowance = weeklyAllowance;
        this.daysRemaining = daysRemaining;
    }

    public boolean hasMonthlyBudget() { return hasMonthlyBudget; }
    public boolean isBalanceLimited() { return balanceLimited; }
    public double getMonthlyBudget() { return monthlyBudget; }
    public double getCurrentBalance() { return currentBalance; }
    public double getSpentToday() { return spentToday; }
    public double getAmountRemaining() { return amountRemaining; }
    public double getDailyAllowance() { return dailyAllowance; }
    public double getWeeklyAllowance() { return weeklyAllowance; }
    public int getDaysRemaining() { return daysRemaining; }
}