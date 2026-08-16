package com.cineseat.ui.view;

import com.cineseat.dao.MovieDao;
import com.cineseat.db.DataAccessException;
import com.cineseat.model.Movie;
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
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** 고른 날짜에 상영하는 영화 목록. */
public class MovieListView extends View {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("M월 d일 (E)");

    private final LocalDate date;

    public MovieListView(AppFrame app, LocalDate date) {
        super(app, "어떤 영화를 볼까요?", date.format(DATE_FORMAT) + " 상영작입니다.");
        this.date = date;

        setBody(buildMovieList());

        JButton back = Theme.ghost("← 날짜 다시 선택");
        back.addActionListener(e -> app.show(new DateSelectView(app)));
        setBackAction(back);
    }

    private JComponent buildMovieList() {
        List<Movie> movies;
        try {
            movies = new MovieDao().findByDate(date);
        } catch (DataAccessException e) {
            Dialogs.error(this, e.getMessage());
            movies = List.of();
        }

        if (movies.isEmpty()) {
            return EmptyState.of("이 날짜에는 상영하는 영화가 없습니다.");
        }

        JPanel stack = Theme.stack();
        for (Movie movie : movies) {
            stack.add(buildMovieRow(movie));
            stack.add(Box.createVerticalStrut(10));
        }
        return Theme.scrollPane(Theme.top(stack));
    }

    private JComponent buildMovieRow(Movie movie) {
        JLabel badge = Theme.badge(movie.ageLimitLabel(), ageLimitColor(movie.ageLimit()),
                Theme.TEXT_ON_ACCENT);

        JLabel title = Theme.heading(movie.title());
        title.setFont(Theme.font(Font.BOLD, 16));

        JPanel meta = Theme.panel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        meta.add(Theme.muted(movie.runningTimeLabel()));
        meta.add(Theme.accent(String.format("%,d원", movie.price())));

        JLabel chevron = Theme.muted("›");
        chevron.setFont(Theme.font(Font.BOLD, 18));

        JPanel right = Theme.panel(new BorderLayout(12, 0));
        right.add(meta, BorderLayout.CENTER);
        right.add(chevron, BorderLayout.EAST);

        JPanel left = Theme.panel(new BorderLayout(14, 0));
        left.add(badge, BorderLayout.WEST);
        left.add(wrapCenter(title), BorderLayout.CENTER);

        RowCard row = new RowCard();
        row.add(left, BorderLayout.CENTER);
        row.add(right, BorderLayout.EAST);
        return row.onClick(() -> app.show(new ScreeningListView(app, date, movie)));
    }

    /** 라벨을 세로 가운데에 맞춘다. */
    private JComponent wrapCenter(JComponent component) {
        JPanel panel = Theme.panel(new BorderLayout());
        component.setAlignmentY(Component.CENTER_ALIGNMENT);
        panel.add(component, BorderLayout.CENTER);
        return panel;
    }

    private Color ageLimitColor(int ageLimit) {
        return switch (ageLimit) {
            case 0 -> Theme.SUCCESS;
            case 12 -> new Color(0x4C8DD8);
            case 15 -> new Color(0xD8894C);
            default -> Theme.DANGER;
        };
    }
}
