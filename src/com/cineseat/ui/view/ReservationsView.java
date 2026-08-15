package com.cineseat.ui.view;

import com.cineseat.dao.ReservationDao;
import com.cineseat.db.DataAccessException;
import com.cineseat.model.Reservation;
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
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** 내 예매 내역. 한 줄에 필요한 정보를 모두 보여 주고 그 자리에서 취소할 수 있다. */
public class ReservationsView extends View {

    private static final DateTimeFormatter MONTH_DAY = DateTimeFormatter.ofPattern("M/d");
    private static final DateTimeFormatter WEEKDAY = DateTimeFormatter.ofPattern("E");

    private final ReservationDao reservationDao = new ReservationDao();

    public ReservationsView(AppFrame app) {
        super(app, "예매 내역", "예매한 표를 확인하고 취소할 수 있습니다.");

        reload();

        JButton back = Theme.ghost("← 메뉴로");
        back.addActionListener(e -> app.show(new DashboardView(app)));
        setBackAction(back);

        JButton book = Theme.primary("새로 예매하기");
        book.addActionListener(e -> app.show(new DateSelectView(app)));
        setPrimaryAction(book);
    }

    private void reload() {
        List<Reservation> reservations;
        try {
            reservations = reservationDao.findByUsername(app.currentUser().username());
        } catch (DataAccessException e) {
            Dialogs.error(this, e.getMessage());
            reservations = List.of();
        }

        if (reservations.isEmpty()) {
            setSubtitle("아직 예매한 표가 없습니다.");
            setBody(EmptyState.of("예매한 표가 없습니다. 새로 예매해 보세요."));
            return;
        }

        setSubtitle(reservations.size() + "건의 예매가 있습니다. 예매한 표를 확인하고 취소할 수 있습니다.");

        JPanel stack = Theme.stack();
        for (Reservation reservation : reservations) {
            stack.add(buildReservationRow(reservation));
            stack.add(Box.createVerticalStrut(10));
        }
        setBody(Theme.scrollPane(Theme.top(stack)));
    }

    private JComponent buildReservationRow(Reservation reservation) {
        RowCard row = new RowCard();
        row.add(buildDateBlock(reservation), BorderLayout.WEST);
        row.add(buildInfoBlock(reservation), BorderLayout.CENTER);
        row.add(buildActionBlock(reservation), BorderLayout.EAST);
        return row;
    }

    /** 표 왼쪽의 날짜 조각. */
    private JComponent buildDateBlock(Reservation reservation) {
        LocalDate date = reservation.reserveDate();

        JLabel day = new JLabel(date.format(MONTH_DAY), JLabel.CENTER);
        day.setFont(Theme.font(Font.BOLD, 17));
        day.setForeground(past(reservation) ? Theme.TEXT_MUTED : Theme.ACCENT);
        day.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel weekday = Theme.muted(date.format(WEEKDAY));
        weekday.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel block = Theme.stack();
        block.add(day);
        block.add(Box.createVerticalStrut(2));
        block.add(weekday);
        block.setPreferredSize(new Dimension(58, 46));
        return block;
    }

    private JComponent buildInfoBlock(Reservation reservation) {
        JLabel title = Theme.heading(reservation.movieTitle());
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        String detail = reservation.placeName() + " · "
                + reservation.reserveTime().format(DateTimeFormatter.ofPattern("HH:mm")) + " · "
                + reservation.seats() + " (" + reservation.seatCount() + "명)";
        JLabel meta = Theme.muted(detail);
        meta.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel booked = Theme.muted("예매일 " + reservation.createdAtLabel());
        booked.setFont(Theme.font(Font.PLAIN, 12));
        booked.setForeground(Theme.BORDER);
        booked.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel block = Theme.stack();
        block.add(title);
        block.add(Box.createVerticalStrut(5));
        block.add(meta);
        block.add(Box.createVerticalStrut(3));
        block.add(booked);
        return block;
    }

    private JComponent buildActionBlock(Reservation reservation) {
        JLabel price = Theme.accent(reservation.priceLabel());
        price.setFont(Theme.font(Font.BOLD, 15));
        price.setHorizontalAlignment(JLabel.RIGHT);

        JPanel block = Theme.panel(new BorderLayout(0, 10));
        block.add(price, BorderLayout.NORTH);

        if (past(reservation)) {
            JLabel done = Theme.muted("관람 완료");
            done.setHorizontalAlignment(JLabel.RIGHT);
            block.add(done, BorderLayout.SOUTH);
        } else {
            JButton cancel = Theme.danger("예매 취소");
            cancel.setFont(Theme.font(Font.BOLD, 12));
            cancel.setBorder(new EmptyBorder(7, 14, 7, 14));
            cancel.addActionListener(e -> cancel(reservation));
            block.add(cancel, BorderLayout.SOUTH);
        }
        return block;
    }

    private boolean past(Reservation reservation) {
        return reservation.reserveDate().isBefore(LocalDate.now());
    }

    private void cancel(Reservation reservation) {
        String message = reservation.movieTitle() + "\n"
                + reservation.scheduleLabel() + " · " + reservation.seats() + "\n\n"
                + "이 예매를 취소할까요?";
        if (!Dialogs.confirm(this, message, "예매 취소")) {
            return;
        }

        try {
            if (reservationDao.cancel(reservation.id(), app.currentUser().username())) {
                Dialogs.info(this, "예매가 취소되었습니다.");
            } else {
                Dialogs.error(this, "이미 취소된 예매입니다.");
            }
            reload();
        } catch (DataAccessException e) {
            Dialogs.error(this, e.getMessage());
        }
    }
}
