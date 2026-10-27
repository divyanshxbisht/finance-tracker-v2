package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import model.Transaction;
import persistence.BudgetPreferences;
import service.BudgetSummary;
import service.FinanceManager;

public class FinanceTrackerFrame extends JFrame {
    private static final Color BACKGROUND = new Color(245, 247, 244);
    private static final Color INK = new Color(30, 43, 39);
    private static final Color MUTED = new Color(112, 126, 119);
    private static final Color GREEN = new Color(47, 112, 83);
    private static final Color LINE = new Color(222, 229, 222);

    private final FinanceManager financeManager;
    private final BudgetPreferences budgetPreferences;
    private final DefaultTableModel tableModel;
    private final JTable transactionTable;
    private final TableRowSorter<DefaultTableModel> sorter;
    private final JLabel incomeValue = new JLabel();
    private final JLabel expenseValue = new JLabel();
    private final JLabel balanceValue = new JLabel();
    private final JLabel countValue = new JLabel();
    private final JLabel todaySpentValue = new JLabel();
    private final JLabel currentDateValue = new JLabel();
    private final JLabel monthRemainingValue = new JLabel();
    private final JLabel dailyAllowanceValue = new JLabel();
    private final JLabel weeklyAllowanceValue = new JLabel();
    private final JLabel weeklyAllowanceTitle = new JLabel("SAFE TO SPEND / WEEK");
    private final JLabel budgetStatus = new JLabel();
    private final JTextField searchField = new JTextField(18);
    private final JComboBox<String> typeFilter = new JComboBox<>(new String[] {"All types", "INCOME", "EXPENSE"});

    public FinanceTrackerFrame(FinanceManager financeManager, BudgetPreferences budgetPreferences) {
        super("Personal Finance Tracker");
        this.financeManager = financeManager;
        this.budgetPreferences = budgetPreferences;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(900, 600));
        setSize(1120, 740);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BACKGROUND);

        tableModel = new DefaultTableModel(new Object[] {"ID", "Type", "Description", "Category", "Date", "Amount"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        transactionTable = new JTable(tableModel);
        sorter = new TableRowSorter<>(tableModel);
        transactionTable.setRowSorter(sorter);
        buildLayout();
        refreshDashboard();
        new javax.swing.Timer(60_000, event -> refreshDashboard()).start();
    }

    private void buildLayout() {
        JPanel sidebar = new JPanel();
        sidebar.setBackground(INK);
        sidebar.setPreferredSize(new Dimension(205, 0));
        sidebar.setBorder(BorderFactory.createEmptyBorder(26, 18, 20, 18));
        sidebar.setLayout(new javax.swing.BoxLayout(sidebar, javax.swing.BoxLayout.Y_AXIS));

        JLabel brand = new JLabel("FINANCE / DESK");
        brand.setForeground(Color.WHITE);
        brand.setFont(new Font("SansSerif", Font.BOLD, 17));
        brand.setBorder(BorderFactory.createEmptyBorder(4, 3, 30, 0));
        sidebar.add(brand);
        sidebar.add(sidebarButton("+  Add income", true, () -> openTransactionDialog("INCOME", null)));
        sidebar.add(sidebarButton("+  Add expense", false, () -> openTransactionDialog("EXPENSE", null)));
        sidebar.add(javax.swing.Box.createVerticalStrut(14));
        sidebar.add(sidebarButton("Save data", false, this::saveData));
        sidebar.add(sidebarButton("Load data", false, this::loadData));
        sidebar.add(javax.swing.Box.createVerticalGlue());
        JLabel footer = new JLabel("PERSONAL FINANCE");
        footer.setForeground(new Color(166, 183, 173));
        footer.setFont(new Font("SansSerif", Font.PLAIN, 10));
        sidebar.add(footer);

        JPanel content = new JPanel(new BorderLayout(0, 20));
        content.setBackground(BACKGROUND);
        content.setBorder(BorderFactory.createEmptyBorder(30, 32, 28, 32));
        content.add(buildHeader(), BorderLayout.NORTH);
        JPanel mainArea = new JPanel(new BorderLayout(0, 18));
        mainArea.setOpaque(false);
        mainArea.add(buildBudgetPanel(), BorderLayout.NORTH);
        mainArea.add(buildTransactionsPanel(), BorderLayout.CENTER);
        content.add(mainArea, BorderLayout.CENTER);
        add(sidebar, BorderLayout.WEST);
        add(content, BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(0, 22));
        header.setOpaque(false);
        JPanel titleBlock = new JPanel(new BorderLayout(0, 5));
        titleBlock.setOpaque(false);
        JLabel eyebrow = new JLabel("YOUR MONEY, AT A GLANCE");
        eyebrow.setFont(new Font("SansSerif", Font.BOLD, 10));
        eyebrow.setForeground(GREEN);
        JLabel title = new JLabel("Overview");
        title.setFont(new Font("SansSerif", Font.BOLD, 28));
        title.setForeground(INK);
        titleBlock.add(eyebrow, BorderLayout.NORTH);
        titleBlock.add(title, BorderLayout.CENTER);
        currentDateValue.setFont(new Font("SansSerif", Font.PLAIN, 11));
        currentDateValue.setForeground(MUTED);
        titleBlock.add(currentDateValue, BorderLayout.SOUTH);
        JPanel todayCard = metricCard("SPENT TODAY", todaySpentValue, new Color(176, 89, 66));
        todayCard.setPreferredSize(new Dimension(200, 68));
        titleBlock.add(todayCard, BorderLayout.EAST);
        header.add(titleBlock, BorderLayout.NORTH);

        JPanel metrics = new JPanel(new GridLayout(1, 4, 12, 0));
        metrics.setOpaque(false);
        metrics.add(metricCard("TOTAL INCOME", incomeValue, GREEN));
        metrics.add(metricCard("TOTAL EXPENSES", expenseValue, new Color(176, 89, 66)));
        metrics.add(metricCard("AVAILABLE BALANCE", balanceValue, INK));
        metrics.add(metricCard("TRANSACTIONS", countValue, new Color(77, 112, 123)));
        header.add(metrics, BorderLayout.CENTER);
        return header;
    }

    private JPanel buildBudgetPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 9));
        panel.setOpaque(false);
        JPanel heading = new JPanel(new BorderLayout(12, 0));
        heading.setOpaque(false);
        JLabel title = new JLabel("Spending plan");
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        title.setForeground(INK);
        JButton budgetButton = actionButton("Set monthly budget", false);
        budgetButton.addActionListener(event -> setMonthlyBudget());
        heading.add(title, BorderLayout.WEST);
        heading.add(budgetButton, BorderLayout.EAST);
        panel.add(heading, BorderLayout.NORTH);

        JPanel metrics = new JPanel(new GridLayout(1, 3, 12, 0));
        metrics.setOpaque(false);
        metrics.add(metricCard("LEFT FOR THIS MONTH", monthRemainingValue, INK));
        metrics.add(metricCard("SAFE TO SPEND / DAY", dailyAllowanceValue, GREEN));
        metrics.add(metricCard(weeklyAllowanceTitle, weeklyAllowanceValue, new Color(77, 112, 123)));
        panel.add(metrics, BorderLayout.CENTER);
        budgetStatus.setFont(new Font("SansSerif", Font.PLAIN, 10));
        budgetStatus.setForeground(MUTED);
        panel.add(budgetStatus, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel metricCard(String title, JLabel value, Color accent) {
        JLabel label = new JLabel(title);
        return metricCard(label, value, accent);
    }

    private JPanel metricCard(JLabel label, JLabel value, Color accent) {
        JPanel card = new JPanel(new BorderLayout(0, 12));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 3, 0, 0, accent),
                BorderFactory.createEmptyBorder(13, 14, 13, 12)));
        label.setFont(new Font("SansSerif", Font.BOLD, 9));
        label.setForeground(MUTED);
        value.setFont(new Font("SansSerif", Font.BOLD, 20));
        value.setForeground(accent);
        card.add(label, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildTransactionsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setOpaque(false);
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel title = new JLabel("Transactions");
        title.setFont(new Font("SansSerif", Font.BOLD, 17));
        title.setForeground(INK);
        top.add(title, BorderLayout.WEST);

        JPanel actions = new JPanel();
        actions.setOpaque(false);
        styleInput(searchField);
        searchField.setToolTipText("Search description or category");
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent event) { applyFilters(); }
            public void removeUpdate(javax.swing.event.DocumentEvent event) { applyFilters(); }
            public void changedUpdate(javax.swing.event.DocumentEvent event) { applyFilters(); }
        });
        styleInput(typeFilter);
        typeFilter.addActionListener(event -> applyFilters());
        JButton editButton = actionButton("Edit", false);
        editButton.addActionListener(event -> editSelectedTransaction());
        JButton deleteButton = actionButton("Delete", false);
        deleteButton.addActionListener(event -> deleteSelectedTransaction());
        actions.add(searchField);
        actions.add(typeFilter);
        actions.add(editButton);
        actions.add(deleteButton);
        top.add(actions, BorderLayout.EAST);
        panel.add(top, BorderLayout.NORTH);

        transactionTable.setRowHeight(39);
        transactionTable.setFont(new Font("SansSerif", Font.PLAIN, 12));
        transactionTable.setForeground(INK);
        transactionTable.setBackground(Color.WHITE);
        transactionTable.setGridColor(new Color(237, 240, 236));
        transactionTable.setSelectionBackground(new Color(224, 238, 228));
        transactionTable.setSelectionForeground(INK);
        transactionTable.setFillsViewportHeight(true);
        transactionTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 10));
        transactionTable.getTableHeader().setForeground(MUTED);
        transactionTable.getTableHeader().setBackground(new Color(249, 250, 248));
        transactionTable.getTableHeader().setPreferredSize(new Dimension(0, 36));
        transactionTable.getColumnModel().getColumn(0).setPreferredWidth(45);
        transactionTable.getColumnModel().getColumn(1).setPreferredWidth(80);
        transactionTable.getColumnModel().getColumn(2).setPreferredWidth(220);
        transactionTable.getColumnModel().getColumn(3).setPreferredWidth(130);
        transactionTable.getColumnModel().getColumn(4).setPreferredWidth(100);
        transactionTable.getColumnModel().getColumn(5).setPreferredWidth(110);
        JScrollPane scrollPane = new JScrollPane(transactionTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(LINE));
        scrollPane.getViewport().setBackground(Color.WHITE);
        panel.add(scrollPane, BorderLayout.CENTER);
        transactionTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent event) {
                if (event.getClickCount() == 2) editSelectedTransaction();
            }
        });
        return panel;
    }

    private JButton sidebarButton(String text, boolean primary, Runnable action) {
        JButton button = new JButton(text);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setFont(new Font("SansSerif", Font.BOLD, 12));
        button.setForeground(Color.WHITE);
        button.setBackground(primary ? GREEN : INK);
        button.setBorder(BorderFactory.createEmptyBorder(0, 11, 0, 8));
        button.setFocusPainted(false);
        button.addActionListener(event -> action.run());
        return button;
    }

    private JButton actionButton(String text, boolean primary) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 11));
        button.setForeground(primary ? Color.WHITE : INK);
        button.setBackground(primary ? GREEN : Color.WHITE);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(primary ? GREEN : LINE),
                BorderFactory.createEmptyBorder(7, 11, 7, 11)));
        button.setFocusPainted(false);
        return button;
    }

    private void styleInput(javax.swing.JComponent component) {
        component.setFont(new Font("SansSerif", Font.PLAIN, 11));
        component.setForeground(INK);
        component.setBackground(Color.WHITE);
        component.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
    }

    private void refreshDashboard() {
        tableModel.setRowCount(0);
        for (Transaction transaction : financeManager.getTransactions()) {
            tableModel.addRow(new Object[] {transaction.getTransactionId(), transaction.getType(),
                    transaction.getDescription(), transaction.getCategory(), transaction.getDate(),
                    String.format("Rs. %,.2f", transaction.getAmount())});
        }
        incomeValue.setText(String.format("Rs. %,.2f", financeManager.calculateTotalIncome()));
        expenseValue.setText(String.format("Rs. %,.2f", financeManager.calculateTotalExpenses()));
        balanceValue.setText(String.format("Rs. %,.2f", financeManager.calculateBalance()));
        countValue.setText(String.valueOf(financeManager.getTransactionCount()));
        refreshBudgetSummary();
        applyFilters();
    }

    private void refreshBudgetSummary() {
        LocalDate today = LocalDate.now();
        currentDateValue.setText(today.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")));
        BudgetSummary summary = financeManager.getBudgetSummary(budgetPreferences.getMonthlyBudget(), today);
        todaySpentValue.setText(String.format("Rs. %,.2f", summary.getSpentToday()));
        monthRemainingValue.setText(String.format("Rs. %,.2f", summary.getAmountRemaining()));
        monthRemainingValue.setForeground(summary.getAmountRemaining() < 0 ? new Color(176, 59, 49) : INK);
        dailyAllowanceValue.setText(String.format("Rs. %,.2f", summary.getDailyAllowance()));
        weeklyAllowanceValue.setText(String.format("Rs. %,.2f", summary.getWeeklyAllowance()));
        weeklyAllowanceTitle.setText(summary.getDaysRemaining() < 7 ? "SAFE UNTIL MONTH-END" : "SAFE TO SPEND / WEEK");

        String period = formatRemainingPeriod(summary.getDaysRemaining());
        if (!summary.hasMonthlyBudget()) {
            budgetStatus.setText("No monthly budget set; guidance uses your current balance.  " + period);
        } else if (summary.isBalanceLimited()) {
            budgetStatus.setText(String.format("Monthly budget: Rs. %,.2f  |  Available balance: Rs. %,.2f  |  %s",
                    summary.getMonthlyBudget(), summary.getCurrentBalance(), period));
        } else {
            budgetStatus.setText(String.format("Monthly budget: Rs. %,.2f  |  %s", summary.getMonthlyBudget(), period));
        }
    }

    private String formatRemainingPeriod(int daysRemaining) {
        return String.format("%d %s left including today", daysRemaining, daysRemaining == 1 ? "day" : "days");
    }

    private void setMonthlyBudget() {
        double currentBudget = budgetPreferences.getMonthlyBudget();
        String initialValue = currentBudget > 0 ? String.valueOf(currentBudget) : "";
        String input = JOptionPane.showInputDialog(this,
                "Enter your monthly spending budget in rupees (0 removes the budget).", initialValue);
        if (input == null) return;
        try {
            double monthlyBudget = Double.parseDouble(input.trim());
            financeManager.validateMonthlyBudget(monthlyBudget);
            budgetPreferences.setMonthlyBudget(monthlyBudget);
            refreshBudgetSummary();
        } catch (NumberFormatException exception) {
            showError("Enter a valid budget amount.");
        } catch (IllegalArgumentException exception) {
            showError(exception.getMessage());
        }
    }

    private void applyFilters() {
        String query = searchField.getText().trim().toLowerCase();
        String selectedType = (String) typeFilter.getSelectedItem();
        sorter.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                String type = entry.getStringValue(1);
                String description = entry.getStringValue(2).toLowerCase();
                String category = entry.getStringValue(3).toLowerCase();
                return ("All types".equals(selectedType) || selectedType.equals(type))
                        && (query.isEmpty() || description.contains(query) || category.contains(query));
            }
        });
    }

    private void openTransactionDialog(String type, Transaction existing) {
        JTextField amountField = new JTextField(existing == null ? "" : String.valueOf(existing.getAmount()), 18);
        JTextField descriptionField = new JTextField(existing == null ? "" : existing.getDescription(), 18);
        JTextField categoryField = new JTextField(existing == null ? "" : existing.getCategory(), 18);
        JTextField dateField = new JTextField(existing == null ? LocalDate.now().toString() : existing.getDate(), 18);
        JPanel form = new JPanel(new GridLayout(0, 2, 10, 10));
        form.setBorder(BorderFactory.createEmptyBorder(10, 4, 4, 4));
        form.add(new JLabel("Amount")); form.add(amountField);
        form.add(new JLabel("Description")); form.add(descriptionField);
        form.add(new JLabel("Category")); form.add(categoryField);
        form.add(new JLabel("Date (YYYY-MM-DD)")); form.add(dateField);

        int result = JOptionPane.showConfirmDialog(this, form,
                (existing == null ? "Add " : "Edit ") + type.toLowerCase(),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;
        try {
            double amount = Double.parseDouble(amountField.getText().trim());
            String description = descriptionField.getText();
            String category = categoryField.getText();
            String date = dateField.getText().trim();
            if (existing == null) {
                financeManager.addTransaction(type, amount, description, category, date);
            } else {
                financeManager.updateTransaction(existing.getTransactionId(), amount, description, category, date);
            }
            refreshDashboard();
        } catch (NumberFormatException exception) {
            showError("Enter a valid amount.");
        } catch (IllegalArgumentException exception) {
            showError(exception.getMessage());
        }
    }

    private void editSelectedTransaction() {
        Transaction transaction = getSelectedTransaction();
        if (transaction != null) openTransactionDialog(transaction.getType(), transaction);
    }

    private void deleteSelectedTransaction() {
        Transaction transaction = getSelectedTransaction();
        if (transaction == null) return;
        int choice = JOptionPane.showConfirmDialog(this, "Delete '" + transaction.getDescription() + "'?",
                "Delete transaction", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            financeManager.deleteTransaction(transaction.getTransactionId());
            refreshDashboard();
        }
    }

    private Transaction getSelectedTransaction() {
        int selectedRow = transactionTable.getSelectedRow();
        if (selectedRow < 0) {
            showError("Select a transaction first.");
            return null;
        }
        int modelRow = transactionTable.convertRowIndexToModel(selectedRow);
        int id = (Integer) tableModel.getValueAt(modelRow, 0);
        return financeManager.findTransaction(id);
    }

    private void saveData() {
        if (financeManager.saveData()) {
            JOptionPane.showMessageDialog(this, "Transactions saved.", "Saved", JOptionPane.INFORMATION_MESSAGE);
        } else {
            showError("Could not save transactions. Check the data folder and try again.");
        }
    }

    private void loadData() {
        if (financeManager.loadData()) {
            refreshDashboard();
            JOptionPane.showMessageDialog(this, "Transactions loaded.", "Loaded", JOptionPane.INFORMATION_MESSAGE);
        } else {
            showError("Could not load transactions. The current data was left unchanged; check the data file.");
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Check transaction details", JOptionPane.ERROR_MESSAGE);
    }
}