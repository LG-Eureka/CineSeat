package com.cineseat.ui.view;

import com.cineseat.dao.ReservationDao;
import com.cineseat.db.DataAccessException;
import com.cineseat.model.Reservation;
import com.cineseat.model.User;
import com.cineseat.ui.AppFrame;
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
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.util.List;

/** 계정 정보와 간단한 예매 통계. */
public class ProfileView extends View {

    public ProfileView(AppFrame app) {
        super(app, "내 정보", "계정 정보와 예매 기록을 한눈에 볼 수 있습니다.");

        User user = app.currentUser();
        List<Reservation> reservations = loadReservations(app);

        JPanel content = Theme.panel(new BorderLayout(18, 0));
        content.add(buildAccountCard(user), BorderLayout.WEST);
        content.add(buildStatsCard(reservations), BorderLayout.CENTER);
        setBody(content);

        JButton back = Theme.ghost("← 메뉴로");
        back.addActionListener(e -> app.show(new DashboardView(app)));
        setBackAction(back);
    }

    private List<Reservation> loadReservations(AppFrame app) {
        try {
            return new ReservationDao().findByUsername(app.currentUser().username());
        } catch (DataAccessException e) {
            return List.of();
        }
    }

    private JComponent buildAccountCard(User user) {
        JComponent avatar = buildAvatar(user.username());
        avatar.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel name = Theme.title(user.username());
        name.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel age = Theme.muted(user.age() + "세");
        age.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel stack = Theme.stack();
        stack.add(avatar);
        stack.add(Box.createVerticalStrut(18));
        stack.add(name);
        stack.add(Box.createVerticalStrut(6));
        stack.add(age);

        JPanel card = Theme.card(new BorderLayout());
        card.setPreferredSize(new Dimension(250, 0));
        card.setBorder(new EmptyBorder(36, 20, 20, 20));
        card.add(Theme.top(stack), BorderLayout.CENTER);
        return card;
    }

    /** 아이디 첫 글자를 딴 원형 아바타. 별도 이미지 파일에 의존하지 않는다. */
    private JComponent buildAvatar(String username) {
        String initial = username.isEmpty() ? "?" : username.substring(0, 1).toUpperCase();

        JLabel avatar = new JLabel(initial, JLabel.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.SURFACE_HOVER);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.setColor(Theme.ACCENT);
                g2.drawOval(0, 0, getWidth() - 1, getHeight() - 1);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        avatar.setFont(Theme.font(Font.BOLD, 30));
        avatar.setForeground(Theme.ACCENT);
        avatar.setPreferredSize(new Dimension(88, 88));
        avatar.setMaximumSize(new Dimension(88, 88));
        return avatar;
    }

    private JComponent buildStatsCard(List<Reservation> reservations) {
        int ticketCount = reservations.stream().mapToInt(Reservation::seatCount).sum();
        int totalPaid = reservations.stream().mapToInt(Reservation::price).sum();

        JPanel tiles = Theme.panel(new GridLayout(1, 3, 12, 0));
        tiles.add(buildStatTile(String.valueOf(reservations.size()), "예매 건수"));
        tiles.add(buildStatTile(String.valueOf(ticketCount), "예매한 좌석"));
        tiles.add(buildStatTile(String.format("%,d", totalPaid), "총 결제 금액 (원)"));

        JLabel recentTitle = Theme.heading("최근 예매");
        recentTitle.setBorder(new EmptyBorder(0, 0, 14, 0));

        JPanel recent = Theme.stack();
        if (reservations.isEmpty()) {
            recent.add(Theme.muted("아직 예매한 표가 없습니다."));
        } else {
            reservations.stream().limit(4).forEach(reservation -> {
                recent.add(buildRecentRow(reservation));
                recent.add(Box.createVerticalStrut(10));
            });
        }

        JPanel recentBlock = Theme.panel(new BorderLayout());
        recentBlock.setBorder(new EmptyBorder(24, 0, 0, 0));
        recentBlock.add(recentTitle, BorderLayout.NORTH);
        recentBlock.add(Theme.top(recent), BorderLayout.CENTER);

        JPanel card = Theme.card(new BorderLayout());
        card.add(tiles, BorderLayout.NORTH);
        card.add(recentBlock, BorderLayout.CENTER);
        return card;
    }

    private JComponent buildStatTile(String value, String caption) {
        JLabel valueLabel = new JLabel(value, JLabel.CENTER);
        valueLabel.setFont(Theme.font(Font.BOLD, 24));
        valueLabel.setForeground(Theme.ACCENT);

        JLabel captionLabel = Theme.muted(caption);
        captionLabel.setHorizontalAlignment(JLabel.CENTER);

        Theme.RoundPanel tile = Theme.card(new BorderLayout(0, 8));
        tile.setColors(Theme.SURFACE_HOVER, null);
        tile.setBorder(new EmptyBorder(18, 10, 18, 10));
        tile.add(valueLabel, BorderLayout.CENTER);
        tile.add(captionLabel, BorderLayout.SOUTH);
        return tile;
    }

    private JComponent buildRecentRow(Reservation reservation) {
        JLabel title = Theme.body(reservation.movieTitle());
        JLabel schedule = Theme.muted(reservation.scheduleLabel() + " · " + reservation.seats());
        schedule.setHorizontalAlignment(JLabel.RIGHT);

        JPanel row = Theme.panel(new BorderLayout(12, 0));
        row.add(title, BorderLayout.WEST);
        row.add(schedule, BorderLayout.EAST);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        return row;
    }
}
