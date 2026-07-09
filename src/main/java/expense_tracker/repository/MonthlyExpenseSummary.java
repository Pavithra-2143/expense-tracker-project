package expense_tracker.repository;

import java.math.BigDecimal;

public interface MonthlyExpenseSummary {

    Integer getYear();

    Integer getMonth();

    BigDecimal getTotal();
}