package com.cineseat.ui.view;

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
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;

/** 로그인 전 첫 화면. 서비스 소개와 로그인 · 회원가입 진입점을 제공한다. */
public class HomeView extends View {

    public HomeView(AppFrame app) {
        super(app, "보고 싶은 좌석을, 세 단계 만에",
                "날짜를 고르고 회차를 정한 다음 좌석만 누르면 예매가 끝납니다.");

        JPanel columns = Theme.panel(new GridLayout(1, 2, 18, 0));
        columns.add(buildStepsCard());
        columns.add(buildNoticeCard());
        setBody(columns);

        JButton register = Theme.secondary("회원가입");
        register.addActionListener(e -> app.show(new RegisterView(app)));
        setBackAction(register);

        JButton login = Theme.primary("로그인");
        login.addActionListener(e -> app.show(new LoginView(app)));
        setPrimaryAction(login);
    }

    private JComponent buildStepsCard() {
        JPanel stack = Theme.stack();
        List<String[]> steps = List.of(
                new String[]{"1", "날짜 선택", "상영 일정이 있는 날짜만 달력에서 켜집니다."},
                new String[]{"2", "영화 · 회차 선택", "상영관과 시간대별 잔여 좌석을 함께 보여 줍니다."},
                new String[]{"3", "좌석 선택", "이미 팔린 좌석은 고를 수 없고 금액이 바로 계산됩니다."});

        for (String[] step : steps) {
            stack.add(buildStepRow(step[0], step[1], step[2]));
            stack.add(Box.createVerticalStrut(18));
        }
        return buildCard("예매 순서", stack);
    }

    private JComponent buildStepRow(String number, String title, String description) {
        JLabel badge = new JLabel(number);
        badge.setFont(Theme.font(Font.BOLD, 14));
        badge.setForeground(Theme.ACCENT);
        badge.setBorder(new EmptyBorder(1, 0, 0, 12));

        JPanel text = Theme.stack();
        JLabel heading = Theme.heading(title);
        JLabel detail = Theme.muted("<html><body style='width:260px'>" + description + "</body></html>");
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        detail.setAlignmentX(Component.LEFT_ALIGNMENT);
        text.add(heading);
        text.add(Box.createVerticalStrut(4));
        text.add(detail);

        JPanel row = Theme.panel(new BorderLayout());
        row.add(Theme.top(badge), BorderLayout.WEST);
        row.add(text, BorderLayout.CENTER);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        return row;
    }

    private JComponent buildNoticeCard() {
        JPanel stack = Theme.stack();
        List<String> notices = List.of(
                "가입한 계정으로만 로그인할 수 있습니다.",
                "비밀번호 재설정 기능은 아직 없으니 잊지 않도록 주의해 주세요.",
                "예매 취소는 예매 내역 화면에서 언제든 할 수 있습니다.",
                "학습용 프로젝트라 실제 결제는 이루어지지 않습니다.");

        for (String notice : notices) {
            stack.add(buildBullet(notice));
            stack.add(Box.createVerticalStrut(14));
        }
        return buildCard("알아두세요", stack);
    }

    private JComponent buildBullet(String text) {
        JLabel dot = Theme.accent("·");
        dot.setBorder(new EmptyBorder(0, 2, 0, 10));

        JPanel row = Theme.panel(new BorderLayout());
        row.add(Theme.top(dot), BorderLayout.WEST);
        row.add(Theme.muted("<html><body style='width:290px'>" + text + "</body></html>"),
                BorderLayout.CENTER);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        return row;
    }

    private JComponent buildCard(String title, JComponent content) {
        JLabel heading = Theme.heading(title);
        heading.setBorder(new EmptyBorder(0, 0, 18, 0));

        JPanel card = Theme.card(new BorderLayout());
        card.add(heading, BorderLayout.NORTH);
        card.add(Theme.top(content), BorderLayout.CENTER);
        return card;
    }
}
