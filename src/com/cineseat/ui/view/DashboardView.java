package com.cineseat.ui.view;

import com.cineseat.dao.ReservationDao;
import com.cineseat.db.DataAccessException;
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
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** 로그인 후 메뉴 화면. */
public class DashboardView extends View {

    public DashboardView(AppFrame app) {
        super(app, "무엇을 해 볼까요?", "예매를 시작하거나 지난 예매 내역을 확인할 수 있습니다.");

        setSubtitle(buildSubtitle(app));

        JPanel menu = Theme.panel(new GridLayout(1, 3, 16, 0));
        menu.add(buildMenuCard("▶", "영화 예매", "날짜 · 회차 · 좌석을 골라 예매합니다.",
                () -> app.show(new DateSelectView(app))));
        menu.add(buildMenuCard("▤", "예매 내역", "예매를 확인하고 취소할 수 있습니다.",
                () -> app.show(new ReservationsView(app))));
        menu.add(buildMenuCard("☺", "내 정보", "계정 정보와 예매 통계를 봅니다.",
                () -> app.show(new ProfileView(app))));
        setBody(menu);

        JButton start = Theme.primary("예매 시작하기");
        start.addActionListener(e -> app.show(new DateSelectView(app)));
        setPrimaryAction(start);
    }

    /** 예매 건수를 부제목에 함께 보여 준다. DB 가 없으면 조용히 기본 문구를 쓴다. */
    private String buildSubtitle(AppFrame app) {
        try {
            int count = new ReservationDao().findByUsername(app.currentUser().username()).size();
            return count == 0
                    ? "아직 예매한 내역이 없습니다. 첫 예매를 시작해 보세요."
                    : "지금까지 " + count + "건을 예매했습니다.";
        } catch (DataAccessException e) {
            return "예매를 시작하거나 지난 예매 내역을 확인할 수 있습니다.";
        }
    }

    private JComponent buildMenuCard(String glyph, String title, String description, Runnable action) {
        JLabel icon = new JLabel(glyph);
        icon.setFont(Theme.font(Font.PLAIN, 26));
        icon.setForeground(Theme.ACCENT);
        icon.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel titleLabel = Theme.heading(title);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel descriptionLabel = Theme.muted(
                "<html><body style='width:150px'>" + description + "</body></html>");
        descriptionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel content = Theme.stack();
        content.add(icon);
        content.add(Box.createVerticalStrut(16));
        content.add(titleLabel);
        content.add(Box.createVerticalStrut(6));
        content.add(descriptionLabel);

        Theme.RoundPanel card = Theme.card(new BorderLayout());
        card.setBorder(new EmptyBorder(22, 20, 22, 20));
        card.add(Theme.middle(content), BorderLayout.CENTER);
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                card.setColors(Theme.SURFACE_HOVER, Theme.ACCENT);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                card.setColors(Theme.SURFACE, Theme.BORDER);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                action.run();
            }
        });
        return card;
    }
}
