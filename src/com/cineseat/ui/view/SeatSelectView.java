package com.cineseat.ui.view;

import com.cineseat.dao.ReservationDao;
import com.cineseat.db.DataAccessException;
import com.cineseat.model.Movie;
import com.cineseat.model.Screening;
import com.cineseat.ui.AppFrame;
import com.cineseat.ui.Dialogs;
import com.cineseat.ui.Theme;
import com.cineseat.ui.View;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 좌석을 고르는 화면.
 *
 * <p>이미 팔린 좌석은 눌리지 않고, 고른 좌석 수에 따라 금액이 바로 계산된다. 인원 수를 따로
 * 입력받지 않기 때문에 "선택한 좌석 수와 인원이 다르다" 같은 상황 자체가 생기지 않는다.
 */
public class SeatSelectView extends View {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("M월 d일 (E)");
    private static final int SEATS_PER_ROW = 8;
    private static final int MAX_SEATS_PER_BOOKING = 8;

    private final ReservationDao reservationDao = new ReservationDao();
    private final Movie movie;
    private final Screening screening;
    private final Set<String> selectedSeats = new LinkedHashSet<>();
    private final List<SeatButton> seatButtons = new ArrayList<>();

    private final JLabel selectionLabel = Theme.body("-");
    private final JLabel countLabel = Theme.body("0명");
    private final JLabel totalLabel = Theme.accent("0원");
    private final JButton reserveButton = Theme.primary("예매하기");

    private Set<String> reservedSeats;

    public SeatSelectView(AppFrame app, Movie movie, Screening screening) {
        super(app, "좌석을 선택해 주세요",
                movie.title() + " · " + screening.placeName() + " · "
                        + screening.showDate().format(DATE_FORMAT) + " " + screening.startTimeLabel());
        this.movie = movie;
        this.screening = screening;
        this.reservedSeats = loadReservedSeats();

        JPanel content = Theme.panel(new BorderLayout(18, 0));
        content.add(buildSeatCard(), BorderLayout.CENTER);
        content.add(buildSummaryCard(), BorderLayout.EAST);
        setBody(content);

        JButton back = Theme.ghost("← 회차 다시 선택");
        back.addActionListener(e -> app.show(new ScreeningListView(app, screening.showDate(), movie)));
        setBackAction(back);

        reserveButton.addActionListener(e -> reserve());
        reserveButton.setEnabled(false);
        setPrimaryAction(reserveButton);
    }

    private Set<String> loadReservedSeats() {
        try {
            return reservationDao.reservedSeats(screening);
        } catch (DataAccessException e) {
            Dialogs.error(this, e.getMessage());
            return Set.of();
        }
    }

    // ---------------------------------------------------------------- 좌석 배치

    private JComponent buildSeatCard() {
        JPanel rows = Theme.stack();
        int rowCount = (int) Math.ceil(screening.totalSeats() / (double) SEATS_PER_ROW);
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            rows.add(buildSeatRow(rowIndex));
            rows.add(Box.createVerticalStrut(8));
        }

        JPanel centered = Theme.panel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        centered.add(rows);

        JPanel card = Theme.card(new BorderLayout(0, 16));
        card.setBorder(new EmptyBorder(20, 24, 20, 24));
        card.add(buildScreenBar(), BorderLayout.NORTH);
        card.add(Theme.top(centered), BorderLayout.CENTER);
        card.add(buildLegend(), BorderLayout.SOUTH);
        return card;
    }

    /** 스크린 위치를 알려 주는 막대. */
    private JComponent buildScreenBar() {
        JLabel label = new JLabel("S C R E E N", JLabel.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.SURFACE_HOVER);
                g2.fillRoundRect(getWidth() / 6, 4, getWidth() * 2 / 3, getHeight() - 8, 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        label.setFont(Theme.font(Font.BOLD, 11));
        label.setForeground(Theme.TEXT_MUTED);
        label.setPreferredSize(new Dimension(0, 30));
        return label;
    }

    private JComponent buildSeatRow(int rowIndex) {
        char rowLetter = (char) ('A' + rowIndex);

        JLabel rowLabel = Theme.muted(String.valueOf(rowLetter));
        rowLabel.setPreferredSize(new Dimension(22, 34));

        JPanel leftBlock = Theme.panel(new GridLayout(1, SEATS_PER_ROW / 2, 8, 0));
        JPanel rightBlock = Theme.panel(new GridLayout(1, SEATS_PER_ROW / 2, 8, 0));

        for (int column = 1; column <= SEATS_PER_ROW; column++) {
            int seatNumber = rowIndex * SEATS_PER_ROW + column;
            if (seatNumber > screening.totalSeats()) {
                (column <= SEATS_PER_ROW / 2 ? leftBlock : rightBlock).add(Theme.panel(new BorderLayout()));
                continue;
            }
            SeatButton seat = new SeatButton(rowLetter + String.valueOf(column));
            seatButtons.add(seat);
            (column <= SEATS_PER_ROW / 2 ? leftBlock : rightBlock).add(seat);
        }

        JPanel blocks = Theme.panel(new GridLayout(1, 2, 26, 0));
        blocks.add(leftBlock);
        blocks.add(rightBlock);

        JPanel row = Theme.panel(new BorderLayout(6, 0));
        row.add(rowLabel, BorderLayout.WEST);
        row.add(blocks, BorderLayout.CENTER);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        return row;
    }

    private JComponent buildLegend() {
        JPanel legend = Theme.panel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        legend.add(buildLegendItem(Theme.SURFACE_HOVER, "선택 가능"));
        legend.add(buildLegendItem(Theme.ACCENT, "선택한 좌석"));
        legend.add(buildLegendItem(Theme.SURFACE, "예매 완료"));
        return legend;
    }

    private JComponent buildLegendItem(Color color, String text) {
        // 배경과 같은 색이라도 구분되도록 테두리를 함께 그린다.
        JLabel swatch = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.fillRoundRect(0, 3, 16, 16, 6, 6);
                g2.setColor(Theme.BORDER);
                g2.drawRoundRect(0, 3, 15, 15, 6, 6);
                g2.dispose();
            }
        };
        swatch.setPreferredSize(new Dimension(16, 22));

        JPanel item = Theme.panel(new BorderLayout(8, 0));
        item.add(swatch, BorderLayout.WEST);
        item.add(Theme.muted(text), BorderLayout.CENTER);
        return item;
    }

    // ---------------------------------------------------------------- 요약

    private JComponent buildSummaryCard() {
        JPanel stack = Theme.stack();
        stack.add(summaryRow("영화", Theme.body(movie.title())));
        stack.add(Box.createVerticalStrut(14));
        stack.add(summaryRow("상영관", Theme.body(screening.placeName())));
        stack.add(Box.createVerticalStrut(14));
        stack.add(summaryRow("일시", Theme.body(
                screening.showDate().format(DATE_FORMAT) + " " + screening.startTimeLabel())));
        stack.add(Box.createVerticalStrut(14));
        stack.add(Theme.divider());
        stack.add(Box.createVerticalStrut(14));
        stack.add(summaryRow("선택 좌석", selectionLabel));
        stack.add(Box.createVerticalStrut(14));
        stack.add(summaryRow("인원", countLabel));
        stack.add(Box.createVerticalStrut(14));
        stack.add(summaryRow("결제 금액", totalLabel));

        JLabel note = Theme.muted("<html><body style='width:170px'>1인 "
                + String.format("%,d원", movie.price()) + " · 한 번에 최대 "
                + MAX_SEATS_PER_BOOKING + "석까지 예매할 수 있습니다.</body></html>");

        JPanel card = Theme.card(new BorderLayout(0, 16));
        card.setPreferredSize(new Dimension(250, 0));
        card.add(Theme.top(stack), BorderLayout.CENTER);
        card.add(note, BorderLayout.SOUTH);
        return card;
    }

    private JComponent summaryRow(String label, JLabel value) {
        value.setHorizontalAlignment(JLabel.RIGHT);

        JPanel row = Theme.panel(new BorderLayout(10, 0));
        row.add(Theme.muted(label), BorderLayout.WEST);
        row.add(value, BorderLayout.EAST);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        return row;
    }

    // ---------------------------------------------------------------- 동작

    private void toggleSeat(SeatButton seat) {
        if (selectedSeats.contains(seat.seatName)) {
            selectedSeats.remove(seat.seatName);
        } else if (selectedSeats.size() >= MAX_SEATS_PER_BOOKING) {
            seat.setSelected(false);
            Dialogs.info(this, "한 번에 최대 " + MAX_SEATS_PER_BOOKING + "석까지 예매할 수 있습니다.");
            return;
        } else {
            selectedSeats.add(seat.seatName);
        }
        updateSummary();
    }

    private void updateSummary() {
        selectionLabel.setText(selectedSeats.isEmpty() ? "-" : String.join(", ", selectedSeats));
        countLabel.setText(selectedSeats.size() + "명");
        totalLabel.setText(String.format("%,d원", selectedSeats.size() * movie.price()));
        reserveButton.setEnabled(!selectedSeats.isEmpty());
    }

    private void reserve() {
        List<String> seats = new ArrayList<>(selectedSeats);
        int price = seats.size() * movie.price();
        String summary = String.join(", ", seats) + " (" + seats.size() + "명)\n"
                + "결제 금액 " + String.format("%,d원", price);

        if (!Dialogs.confirm(this, summary + "\n\n이대로 예매할까요?", "예매하기")) {
            return;
        }

        try {
            ReservationDao.Outcome outcome =
                    reservationDao.reserve(app.currentUser().username(), screening, seats, price);
            switch (outcome) {
                case ReservationDao.Outcome.Success ignored -> {
                    Dialogs.info(this, "예매가 완료되었습니다.");
                    app.show(new ReservationsView(app));
                }
                case ReservationDao.Outcome.SeatsTaken taken -> {
                    Dialogs.error(this, String.join(", ", taken.seats())
                            + " 좌석은 방금 다른 분이 예매했습니다. 좌석을 다시 선택해 주세요.");
                    refreshSeats();
                }
            }
        } catch (DataAccessException e) {
            Dialogs.error(this, e.getMessage());
        }
    }

    /** 좌석 상태를 다시 읽어 화면에 반영한다. */
    private void refreshSeats() {
        reservedSeats = loadReservedSeats();
        selectedSeats.clear();
        for (SeatButton seat : seatButtons) {
            seat.setSelected(false);
            seat.refreshState();
        }
        updateSummary();
    }

    // ---------------------------------------------------------------- 좌석 버튼

    /** 좌석 한 자리. 상태에 따라 색이 달라진다. */
    private class SeatButton extends JToggleButton {

        private final String seatName;

        SeatButton(String seatName) {
            super(seatName);
            this.seatName = seatName;

            setUI(new BasicButtonUI());
            setFont(Theme.font(Font.PLAIN, 11));
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setBorder(new EmptyBorder(8, 0, 8, 0));
            setPreferredSize(new Dimension(40, 34));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addActionListener(e -> toggleSeat(this));
            refreshState();
        }

        void refreshState() {
            boolean taken = reservedSeats.contains(seatName);
            setEnabled(!taken);
            setToolTipText(taken ? seatName + " · 이미 예매된 좌석" : seatName);
            setForeground(taken ? Theme.BORDER : Theme.TEXT);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color fill;
            if (!isEnabled()) {
                fill = Theme.SURFACE;
            } else if (isSelected()) {
                fill = Theme.ACCENT;
            } else if (getModel().isRollover()) {
                fill = Theme.BORDER;
            } else {
                fill = Theme.SURFACE_HOVER;
            }

            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);

            if (!isEnabled()) {
                // 이미 팔린 좌석: 테두리만 남기고 좌석 번호를 흐리게 그린다.
                g2.setColor(Theme.BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.setFont(getFont());
                FontMetrics metrics = g2.getFontMetrics();
                int x = (getWidth() - metrics.stringWidth(seatName)) / 2;
                int y = (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
                g2.drawString(seatName, x, y);
                g2.dispose();
                return;
            }
            g2.dispose();

            setForeground(isSelected() ? Theme.TEXT_ON_ACCENT : Theme.TEXT);
            super.paintComponent(g);
        }
    }
}
