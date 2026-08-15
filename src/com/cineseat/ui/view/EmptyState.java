package com.cineseat.ui.view;

import com.cineseat.ui.Theme;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagLayout;

/** 보여 줄 항목이 없을 때 화면 가운데에 놓는 안내. */
final class EmptyState {

    private EmptyState() {
    }

    static JComponent of(String message) {
        JLabel glyph = new JLabel("◌");
        glyph.setFont(Theme.font(Font.PLAIN, 30));
        glyph.setForeground(Theme.BORDER);
        glyph.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel text = Theme.muted(message);
        text.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel column = Theme.stack();
        column.add(glyph);
        column.add(Box.createVerticalStrut(12));
        column.add(text);

        JPanel centered = Theme.panel(new GridBagLayout());
        centered.add(column);
        return centered;
    }
}
