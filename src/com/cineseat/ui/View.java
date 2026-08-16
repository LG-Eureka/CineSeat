package com.cineseat.ui;

import com.cineseat.model.User;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;

/**
 * 모든 화면이 공유하는 뼈대.
 *
 * <p>상단에는 브랜드 · 화면 제목 · 로그인 정보가, 하단에는 이동 버튼이 늘 같은 자리에 놓인다.
 * 각 화면은 가운데 본문만 채우면 된다.
 */
public abstract class View extends JPanel {

    protected final AppFrame app;

    private final JPanel headerActions = Theme.panel(new BorderLayout());
    private final JPanel body = Theme.panel(new BorderLayout());
    private final JPanel footerLeft = Theme.panel(new BorderLayout());
    private final JPanel footerRight = Theme.panel(new BorderLayout());
    private final JLabel titleLabel;
    private final JLabel subtitleLabel;

    protected View(AppFrame app, String title, String subtitle) {
        this.app = app;
        this.titleLabel = Theme.title(title);
        this.subtitleLabel = Theme.muted(subtitle);

        setLayout(new BorderLayout());
        setBackground(Theme.BG);

        add(buildHeader(), BorderLayout.NORTH);

        body.setBorder(new EmptyBorder(4, 32, 4, 32));
        add(body, BorderLayout.CENTER);

        add(buildFooter(), BorderLayout.SOUTH);
    }

    // ---------------------------------------------------------------- 뼈대

    private JComponent buildHeader() {
        // 원본 화면들이 공통으로 갖고 있던 상단 로열 블루 띠.
        JPanel brandBar = Theme.panel(new BorderLayout());
        brandBar.setOpaque(true);
        brandBar.setBackground(Theme.ACCENT);
        brandBar.setBorder(new EmptyBorder(13, 32, 13, 32));
        brandBar.add(buildBrand(), BorderLayout.WEST);
        brandBar.add(buildAccountChip(), BorderLayout.EAST);

        JPanel titleBlock = Theme.panel(null);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleBlock.add(titleLabel);
        titleBlock.add(Box.createVerticalStrut(4));
        titleBlock.add(subtitleLabel);
        titleBlock.setBorder(new EmptyBorder(22, 32, 18, 32));

        JPanel wrapper = Theme.panel(new BorderLayout());
        wrapper.add(brandBar, BorderLayout.NORTH);
        wrapper.add(titleBlock, BorderLayout.CENTER);
        wrapper.add(Theme.divider(), BorderLayout.SOUTH);
        return wrapper;
    }

    private JComponent buildBrand() {
        JLabel mark = new JLabel("◗ CINESEAT");
        mark.setFont(Theme.font(Font.BOLD, 15));
        mark.setForeground(Theme.TEXT_ON_ACCENT);
        return mark;
    }

    /** 로그인한 사용자에게만 계정 이름과 로그아웃 버튼을 보여 준다. */
    private JComponent buildAccountChip() {
        User user = app.currentUser();
        if (user == null) {
            return headerActions;
        }

        JLabel name = new JLabel(user.username() + " 님");
        name.setFont(Theme.font(Font.PLAIN, 13));
        name.setForeground(Theme.TEXT_ON_ACCENT);

        JPanel chip = Theme.panel(new BorderLayout(12, 0));
        chip.add(name, BorderLayout.WEST);

        JButton signOut = Theme.onAccentGhost("로그아웃");
        signOut.setBorder(new EmptyBorder(3, 10, 3, 10));
        signOut.setFont(Theme.font(Font.PLAIN, 13));
        signOut.addActionListener(e -> app.signOut());
        chip.add(signOut, BorderLayout.EAST);

        headerActions.add(chip, BorderLayout.EAST);
        return headerActions;
    }

    private JComponent buildFooter() {
        JPanel footer = Theme.panel(new BorderLayout());
        footer.setBorder(new EmptyBorder(16, 32, 24, 32));
        footer.add(footerLeft, BorderLayout.WEST);
        footer.add(footerRight, BorderLayout.EAST);

        JPanel wrapper = Theme.panel(new BorderLayout());
        wrapper.add(Theme.divider(), BorderLayout.NORTH);
        wrapper.add(footer, BorderLayout.CENTER);
        return wrapper;
    }

    // ---------------------------------------------------------------- 각 화면에서 채우는 부분

    /** 화면 본문을 채운다. */
    protected void setBody(JComponent content) {
        body.removeAll();
        body.add(content, BorderLayout.CENTER);
        body.revalidate();
        body.repaint();
    }

    /** 하단 왼쪽(주로 뒤로 가기)에 놓을 버튼. */
    protected void setBackAction(JComponent component) {
        footerLeft.removeAll();
        footerLeft.add(component, BorderLayout.CENTER);
    }

    /** 하단 오른쪽(주로 다음 단계)에 놓을 버튼. */
    protected void setPrimaryAction(JComponent component) {
        footerRight.removeAll();
        footerRight.add(component, BorderLayout.CENTER);
    }

    protected void setSubtitle(String subtitle) {
        subtitleLabel.setText(subtitle);
    }
}
