package com.cineseat.ui;

import javax.swing.JOptionPane;
import java.awt.Component;

/** 알림 · 확인 대화 상자를 한 가지 방식으로만 띄우기 위한 도우미. */
public final class Dialogs {

    private Dialogs() {
    }

    public static void info(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "안내", JOptionPane.PLAIN_MESSAGE);
    }

    public static void error(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "오류", JOptionPane.ERROR_MESSAGE);
    }

    /** 되돌릴 수 없는 행동을 하기 전에 한 번 더 묻는다. */
    public static boolean confirm(Component parent, String message, String confirmLabel) {
        Object[] options = {confirmLabel, "닫기"};
        int choice = JOptionPane.showOptionDialog(
                parent, message, "확인",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, options, options[1]);
        return choice == 0;
    }
}
