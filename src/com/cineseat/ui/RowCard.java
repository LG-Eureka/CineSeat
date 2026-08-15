package com.cineseat.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.border.EmptyBorder;

/**
 * 목록의 한 줄을 나타내는 카드.
 *
 * <p>영화 · 회차 · 예매 내역 목록이 같은 모양과 같은 반응을 갖도록 한 곳에 모아 두었다.
 */
public class RowCard extends Theme.RoundPanel {

    public RowCard() {
        super(new BorderLayout(16, 0), Theme.SURFACE, Theme.BORDER);
        setBorder(new EmptyBorder(15, 18, 15, 18));
        setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    /** 세로로 쌓았을 때 남는 공간만큼 늘어나지 않게 높이를 고정한다. */
    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

    /** 카드 전체를 누를 수 있게 만든다. */
    public RowCard onClick(Runnable action) {
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                setColors(Theme.SURFACE_HOVER, Theme.ACCENT);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setColors(Theme.SURFACE, Theme.BORDER);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                action.run();
            }
        });
        return this;
    }
}
