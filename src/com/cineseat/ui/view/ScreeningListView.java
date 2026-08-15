package com.cineseat.ui.view;

import com.cineseat.dao.ScreeningDao;
import com.cineseat.db.DataAccessException;
import com.cineseat.model.Movie;
import com.cineseat.model.Screening;
import com.cineseat.ui.AppFrame;
import com.cineseat.ui.Dialogs;
import com.cineseat.ui.RowCard;
import com.cineseat.ui.Theme;
import com.cineseat.ui.View;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Font;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** 고른 영화의 상영관 · 시간대 목록. */
public class ScreeningListView extends View {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("M월 d일 (E)");

    private final LocalDate date;
    private final Movie movie;

    public ScreeningListView(AppFrame app, LocalDate date, Movie movie) {
        super(app, movie.title(), date.format(DATE_FORMAT) + " · 상영관과 시간을 선택해 주세요.");
        this.date = date;
        this.movie = movie;

        setBody(buildScreeningList());

        JButton back = Theme.ghost("← 영화 다시 선택");
        back.addActionListener(e -> app.show(new MovieListView(app, date)));
        setBackAction(back);
    }

    private JComponent buildScreeningList() {
        List<Screening> screenings;
        try {
            screenings = new ScreeningDao().findByMovieAndDate(movie.id(), date);
        } catch (DataAccessException e) {
            Dialogs.error(this, e.getMessage());
            screenings = List.of();
        }

        if (screenings.isEmpty()) {
            return EmptyState.of("이 날짜에 예정된 회차가 없습니다.");
        }

        JPanel stack = Theme.stack();
        for (Screening screening : screenings) {
            stack.add(buildScreeningRow(screening));
            stack.add(Box.createVerticalStrut(10));
        }
        return Theme.scrollPane(Theme.top(stack));
    }

    private JComponent buildScreeningRow(Screening screening) {
        JLabel time = new JLabel(screening.startTimeLabel());
        time.setFont(Theme.font(Font.BOLD, 18));
        time.setForeground(screening.soldOut() ? Theme.TEXT_MUTED : Theme.ACCENT);
        time.setPreferredSize(new java.awt.Dimension(64, 28));

        JPanel info = Theme.stack();
        info.add(Theme.heading(screening.placeName()));
        info.add(Box.createVerticalStrut(4));
        info.add(Theme.muted(screening.totalSeats() + "석 상영관"));

        JLabel seats = new JLabel(screening.soldOut()
                ? "매진"
                : "잔여 " + screening.remainingSeats() + "석");
        seats.setFont(Theme.font(Font.BOLD, 13));
        seats.setForeground(seatColor(screening));

        RowCard row = new RowCard();
        row.add(time, BorderLayout.WEST);
        row.add(info, BorderLayout.CENTER);
        row.add(seats, BorderLayout.EAST);

        if (screening.soldOut()) {
            return row;
        }
        return row.onClick(() -> app.show(new SeatSelectView(app, movie, screening)));
    }

    private java.awt.Color seatColor(Screening screening) {
        if (screening.soldOut()) {
            return Theme.DANGER;
        }
        double ratio = (double) screening.remainingSeats() / screening.totalSeats();
        return ratio < 0.2 ? Theme.DANGER : Theme.SUCCESS;
    }
}
