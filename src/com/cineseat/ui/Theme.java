package com.cineseat.ui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * 앱 전체가 공유하는 색/서체/컴포넌트 팩토리.
 *
 * <p>화면마다 색을 직접 지정하지 않고 이 클래스만 쓰도록 해서, 어느 화면에 들어가도 같은 톤이
 * 유지되게 한다.
 */
public final class Theme {

    // 배경 계열
    public static final Color BG = new Color(0x14161C);
    public static final Color SURFACE = new Color(0x1D212A);
    public static final Color SURFACE_HOVER = new Color(0x262B36);
    public static final Color BORDER = new Color(0x333A48);

    // 글자 계열
    public static final Color TEXT = new Color(0xF1F3F6);
    public static final Color TEXT_MUTED = new Color(0x99A1B0);
    public static final Color TEXT_ON_ACCENT = new Color(0x1A1405);

    // 강조 계열
    public static final Color ACCENT = new Color(0xE8B04B);
    public static final Color ACCENT_HOVER = new Color(0xF2C067);
    public static final Color DANGER = new Color(0xE0655F);
    public static final Color SUCCESS = new Color(0x5FBE87);

    private static final int ARC = 14;
    private static final String FAMILY = pickFontFamily();

    private Theme() {
    }

    // ---------------------------------------------------------------- 서체

    public static Font font(int style, int size) {
        return new Font(FAMILY, style, size);
    }

    /** 한글이 깨지지 않는 서체를 우선순위대로 찾는다. */
    private static String pickFontFamily() {
        List<String> preferred = List.of(
                "Pretendard", "Apple SD Gothic Neo", "Malgun Gothic", "NanumGothic", "Noto Sans KR");
        Set<String> installed = Set.of(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        return preferred.stream().filter(installed::contains).findFirst().orElse(Font.SANS_SERIF);
    }

    // ---------------------------------------------------------------- 라벨

    public static JLabel display(String text) {
        return label(text, font(Font.BOLD, 30), TEXT);
    }

    public static JLabel title(String text) {
        return label(text, font(Font.BOLD, 20), TEXT);
    }

    public static JLabel heading(String text) {
        return label(text, font(Font.BOLD, 15), TEXT);
    }

    public static JLabel body(String text) {
        return label(text, font(Font.PLAIN, 14), TEXT);
    }

    public static JLabel muted(String text) {
        return label(text, font(Font.PLAIN, 13), TEXT_MUTED);
    }

    public static JLabel accent(String text) {
        return label(text, font(Font.BOLD, 13), ACCENT);
    }

    private static JLabel label(String text, Font font, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        label.setForeground(color);
        return label;
    }

    // ---------------------------------------------------------------- 버튼

    /** 화면마다 하나씩만 두는 주요 행동 버튼. */
    public static JButton primary(String text) {
        return new RoundButton(text, ACCENT, ACCENT_HOVER, TEXT_ON_ACCENT, null);
    }

    /** 보조 행동 버튼. */
    public static JButton secondary(String text) {
        return new RoundButton(text, SURFACE, SURFACE_HOVER, TEXT, BORDER);
    }

    /** 배경 없이 글자만 있는 버튼. 뒤로 가기처럼 가벼운 이동에 쓴다. */
    public static JButton ghost(String text) {
        return new RoundButton(text, null, SURFACE, TEXT_MUTED, null);
    }

    /** 취소처럼 되돌릴 수 없는 행동에 쓰는 버튼. */
    public static JButton danger(String text) {
        return new RoundButton(text, null, SURFACE_HOVER, DANGER, DANGER);
    }

    /** 둥근 배경을 직접 그리는 버튼. 플랫폼별 기본 버튼 모양에 영향을 받지 않는다. */
    public static class RoundButton extends JButton {

        private final Color fill;
        private final Color fillHover;
        private final Color line;

        private final Color enabledForeground;

        RoundButton(String text, Color fill, Color fillHover, Color foreground, Color line) {
            super(text);
            this.fill = fill;
            this.fillHover = fillHover;
            this.line = line;
            this.enabledForeground = foreground;

            setUI(new BasicButtonUI());
            setFont(font(Font.BOLD, 14));
            setForeground(foreground);
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setBorder(new EmptyBorder(11, 22, 11, 22));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        /** 비활성 상태에서도 글자가 배경에 묻히지 않도록 색을 함께 바꾼다. */
        @Override
        public void setEnabled(boolean enabled) {
            super.setEnabled(enabled);
            setForeground(enabled ? enabledForeground : TEXT_MUTED);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color background = getModel().isRollover() && isEnabled() ? fillHover : fill;
            if (background != null) {
                g2.setColor(isEnabled() ? background : SURFACE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), ARC, ARC);
            }
            if (line != null) {
                g2.setColor(isEnabled() ? line : BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, ARC, ARC);
            }

            if (!isEnabled()) {
                // 기본 구현은 비활성 글자색을 배경색에서 계산하기 때문에 직접 그린다.
                g2.setFont(getFont());
                g2.setColor(TEXT_MUTED);
                FontMetrics metrics = g2.getFontMetrics();
                int x = (getWidth() - metrics.stringWidth(getText())) / 2;
                int y = (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
                g2.drawString(getText(), x, y);
                g2.dispose();
                return;
            }

            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ---------------------------------------------------------------- 패널

    /** 배경이 투명한 기본 패널. */
    public static JPanel panel(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setOpaque(false);
        return panel;
    }

    /** 내용을 감싸는 둥근 카드. */
    public static RoundPanel card(LayoutManager layout) {
        return new RoundPanel(layout, SURFACE, BORDER);
    }

    /** 자식을 위에서부터 세로로 쌓는 패널. */
    public static JPanel stack() {
        JPanel panel = panel(null);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        return panel;
    }

    /**
     * 자식을 위쪽에 붙여 놓는다. 세로로 쌓은 내용이 남는 공간만큼 늘어나지 않게 할 때 쓴다.
     */
    public static JPanel top(Component child) {
        JPanel panel = panel(new BorderLayout());
        panel.add(child, BorderLayout.NORTH);
        return panel;
    }

    /** 자식을 세로 가운데에 두되 가로로는 왼쪽에 맞춰 늘린다. */
    public static JPanel middle(Component child) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;

        JPanel panel = panel(new GridBagLayout());
        panel.add(child, constraints);
        return panel;
    }

    /** 둥근 배경과 테두리를 직접 그리는 패널. */
    public static class RoundPanel extends JPanel {

        private Color fill;
        private Color line;

        RoundPanel(LayoutManager layout, Color fill, Color line) {
            super(layout);
            this.fill = fill;
            this.line = line;
            setOpaque(false);
            setBorder(new EmptyBorder(18, 18, 18, 18));
        }

        public void setColors(Color fill, Color line) {
            this.fill = fill;
            this.line = line;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (fill != null) {
                g2.setColor(fill);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), ARC, ARC);
            }
            if (line != null) {
                g2.setColor(line);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, ARC, ARC);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** 관람 등급처럼 짧은 상태 값을 표시하는 둥근 배지. */
    public static JLabel badge(String text, Color background, Color foreground) {
        JLabel label = new JLabel(text, JLabel.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(background);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        label.setFont(font(Font.BOLD, 12));
        label.setForeground(foreground);
        label.setOpaque(false);
        label.setPreferredSize(new Dimension(44, 28));
        return label;
    }

    /** 얇은 구분선. */
    public static JComponent divider() {
        JPanel line = new JPanel();
        line.setBackground(BORDER);
        line.setPreferredSize(new Dimension(1, 1));
        line.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return line;
    }

    // ---------------------------------------------------------------- 입력

    public static JTextField textField() {
        return style(new JTextField());
    }

    public static JPasswordField passwordField() {
        return style(new JPasswordField());
    }

    /** 설명 라벨과 입력 칸을 세로로 묶는다. */
    public static JPanel labeledField(String labelText, JComponent input) {
        JLabel label = muted(labelText);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        input.setAlignmentX(Component.LEFT_ALIGNMENT);
        input.setMaximumSize(new Dimension(Integer.MAX_VALUE, input.getPreferredSize().height));

        JPanel group = stack();
        group.add(label);
        group.add(Box.createVerticalStrut(6));
        group.add(input);
        group.setAlignmentX(Component.LEFT_ALIGNMENT);
        return group;
    }

    private static <T extends JTextField> T style(T field) {
        field.setFont(font(Font.PLAIN, 14));
        field.setForeground(TEXT);
        field.setBackground(SURFACE_HOVER);
        field.setCaretColor(ACCENT);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(9, 12, 9, 12)));
        return field;
    }

    // ---------------------------------------------------------------- 스크롤

    /** 배경과 스크롤바까지 테마에 맞춘 스크롤 영역. */
    public static JScrollPane scrollPane(Component content) {
        JScrollPane pane = new JScrollPane(content);
        pane.setBorder(null);
        pane.setOpaque(false);
        pane.getViewport().setOpaque(false);
        pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        pane.getVerticalScrollBar().setUnitIncrement(16);
        pane.getVerticalScrollBar().setUI(new ThinScrollBarUI());
        pane.getVerticalScrollBar().setOpaque(false);
        pane.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
        return pane;
    }

    /** 화살표 버튼이 없는 얇은 스크롤바. */
    private static class ThinScrollBarUI extends BasicScrollBarUI {

        @Override
        protected void configureScrollBarColors() {
            thumbColor = BORDER;
            trackColor = BG;
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return zeroSizeButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return zeroSizeButton();
        }

        private JButton zeroSizeButton() {
            JButton button = new JButton();
            button.setPreferredSize(new Dimension(0, 0));
            button.setBorder(null);
            return button;
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, java.awt.Rectangle bounds) {
            // 트랙은 그리지 않는다.
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, java.awt.Rectangle bounds) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(isThumbRollover() ? TEXT_MUTED : BORDER);
            g2.fillRoundRect(bounds.x + 1, bounds.y, bounds.width - 2, bounds.height, 8, 8);
            g2.dispose();
        }
    }

    // ---------------------------------------------------------------- 전역 설정

    /** 대화 상자처럼 직접 그리지 않는 기본 컴포넌트에도 같은 톤을 적용한다. */
    public static void applyGlobalDefaults() {
        Arrays.asList("Panel.background", "OptionPane.background").forEach(key -> UIManager.put(key, SURFACE));
        UIManager.put("OptionPane.messageForeground", TEXT);
        UIManager.put("OptionPane.messageFont", font(Font.PLAIN, 14));
        UIManager.put("OptionPane.buttonFont", font(Font.BOLD, 13));
        UIManager.put("Button.disabledText", TEXT_MUTED);
        UIManager.put("ComboBox.background", SURFACE_HOVER);
        UIManager.put("ComboBox.foreground", TEXT);
        UIManager.put("ComboBox.font", font(Font.PLAIN, 14));
        UIManager.put("ToolTip.background", SURFACE_HOVER);
        UIManager.put("ToolTip.foreground", TEXT);
        UIManager.put("ToolTip.font", font(Font.PLAIN, 12));
    }
}
