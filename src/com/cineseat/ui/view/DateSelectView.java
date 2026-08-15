package com.cineseat.ui.view;

import com.cineseat.dao.ScreeningDao;
import com.cineseat.db.DataAccessException;
import com.cineseat.ui.AppFrame;
import com.cineseat.ui.Dialogs;
import com.cineseat.ui.Theme;
import com.cineseat.ui.View;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Set;

/**
 * 예매할 날짜를 고르는 달력.
 *
 * <p>선택 가능한 날짜는 {@code screen} 테이블에서 읽어 오므로 상영 일정이 바뀌면 달력도 함께
 * 바뀐다.
 */
public class DateSelectView extends View {

    private static final String[] DAY_NAMES = {"일", "월", "화", "수", "목", "금", "토"};

    private final Set<LocalDate> availableDates;
    private final JPanel grid = Theme.panel(new GridLayout(0, 7, 8, 8));
    private final JLabel monthLabel = Theme.title("");
    private YearMonth currentMonth;

    public DateSelectView(AppFrame app) {
        super(app, "언제 보러 갈까요?", "상영 일정이 있는 날짜만 선택할 수 있습니다.");

        this.availableDates = loadAvailableDates();
        this.currentMonth = firstMonthWithScreening();

        setBody(buildCalendarCard());
        renderMonth();

        JButton back = Theme.ghost("← 메뉴로");
        back.addActionListener(e -> app.show(new DashboardView(app)));
        setBackAction(back);

        if (availableDates.isEmpty()) {
            setSubtitle("등록된 상영 일정이 없습니다. db/schema.sql 로 예시 데이터를 넣어 주세요.");
        }
    }

    private Set<LocalDate> loadAvailableDates() {
        try {
            return new ScreeningDao().availableDates();
        } catch (DataAccessException e) {
            Dialogs.error(this, e.getMessage());
            return Set.of();
        }
    }

    /** 오늘 이후로 가장 가까운 상영이 있는 달을 첫 화면으로 연다. */
    private YearMonth firstMonthWithScreening() {
        LocalDate today = LocalDate.now();
        return availableDates.stream()
                .filter(date -> !date.isBefore(today))
                .min(LocalDate::compareTo)
                .map(YearMonth::from)
                .orElse(YearMonth.now());
    }

    private JComponent buildCalendarCard() {
        JButton previous = Theme.ghost("‹");
        previous.addActionListener(e -> moveMonth(-1));
        JButton next = Theme.ghost("›");
        next.addActionListener(e -> moveMonth(1));

        monthLabel.setHorizontalAlignment(JLabel.CENTER);

        JPanel monthBar = Theme.panel(new BorderLayout());
        monthBar.setBorder(new EmptyBorder(0, 0, 18, 0));
        monthBar.add(previous, BorderLayout.WEST);
        monthBar.add(monthLabel, BorderLayout.CENTER);
        monthBar.add(next, BorderLayout.EAST);

        JPanel weekdayBar = Theme.panel(new GridLayout(1, 7, 8, 0));
        for (String dayName : DAY_NAMES) {
            JLabel label = Theme.muted(dayName);
            label.setHorizontalAlignment(JLabel.CENTER);
            weekdayBar.add(label);
        }

        JPanel calendar = Theme.panel(new BorderLayout(0, 10));
        calendar.add(weekdayBar, BorderLayout.NORTH);
        calendar.add(Theme.top(grid), BorderLayout.CENTER);

        JPanel card = Theme.card(new BorderLayout());
        card.setBorder(new EmptyBorder(20, 24, 24, 24));
        card.add(monthBar, BorderLayout.NORTH);
        card.add(calendar, BorderLayout.CENTER);
        return card;
    }

    private void moveMonth(int delta) {
        currentMonth = currentMonth.plusMonths(delta);
        renderMonth();
    }

    private void renderMonth() {
        monthLabel.setText(currentMonth.getYear() + "년 " + currentMonth.getMonthValue() + "월");
        grid.removeAll();

        LocalDate firstDay = currentMonth.atDay(1);
        int leadingBlanks = firstDay.getDayOfWeek() == DayOfWeek.SUNDAY
                ? 0
                : firstDay.getDayOfWeek().getValue();
        for (int i = 0; i < leadingBlanks; i++) {
            grid.add(Theme.panel(new BorderLayout()));
        }

        LocalDate today = LocalDate.now();
        for (int day = 1; day <= currentMonth.lengthOfMonth(); day++) {
            LocalDate date = currentMonth.atDay(day);
            boolean selectable = availableDates.contains(date) && !date.isBefore(today);
            grid.add(buildDayCell(date, selectable));
        }

        grid.revalidate();
        grid.repaint();
    }

    private JComponent buildDayCell(LocalDate date, boolean selectable) {
        String text = String.valueOf(date.getDayOfMonth());
        JButton cell = selectable && date.equals(LocalDate.now())
                ? Theme.primary(text)
                : Theme.secondary(text);

        cell.setFont(Theme.font(selectable ? Font.BOLD : Font.PLAIN, 13));
        cell.setBorder(new EmptyBorder(10, 0, 10, 0));
        cell.setPreferredSize(new Dimension(0, 40));

        if (!selectable) {
            cell.setEnabled(false);
        } else {
            if (!date.equals(LocalDate.now())) {
                cell.setForeground(Theme.ACCENT);
            }
            cell.addActionListener(e -> app.show(new MovieListView(app, date)));
        }
        return cell;
    }
}
