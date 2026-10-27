package persistence;

import java.util.prefs.Preferences;

public class BudgetPreferences {
    private static final String MONTHLY_BUDGET_KEY = "monthlyBudget";
    private final Preferences preferences = Preferences.userRoot().node("PersonalFinanceTracker");
    private final Preferences previousPreferences = Preferences.userRoot().node("<unnamed>");

    public double getMonthlyBudget() {
        migratePreviousBudget();
        return preferences.getDouble(MONTHLY_BUDGET_KEY, 0);
    }

    public void setMonthlyBudget(double amount) {
        migratePreviousBudget();
        if (amount == 0) {
            preferences.remove(MONTHLY_BUDGET_KEY);
        } else {
            preferences.putDouble(MONTHLY_BUDGET_KEY, amount);
        }
    }

    private void migratePreviousBudget() {
        if (preferences.get(MONTHLY_BUDGET_KEY, null) == null
            && previousPreferences.get(MONTHLY_BUDGET_KEY, null) != null) {
            preferences.putDouble(MONTHLY_BUDGET_KEY,
                    previousPreferences.getDouble(MONTHLY_BUDGET_KEY, 0));
            previousPreferences.remove(MONTHLY_BUDGET_KEY);
        }
    }
}