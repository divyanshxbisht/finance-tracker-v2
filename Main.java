import javax.swing.SwingUtilities;
import persistence.BudgetPreferences;
import service.FinanceManager;
import ui.FinanceTrackerFrame;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FinanceManager financeManager = new FinanceManager();
            financeManager.loadData();
            new FinanceTrackerFrame(financeManager, new BudgetPreferences()).setVisible(true);
        });
    }
}