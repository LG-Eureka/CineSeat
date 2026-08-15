package com.cineseat.ui.view;

import com.cineseat.dao.UserDao;
import com.cineseat.db.DataAccessException;
import com.cineseat.model.User;
import com.cineseat.ui.AppFrame;
import com.cineseat.ui.Theme;
import com.cineseat.ui.View;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Optional;

/** 로그인 화면. */
public class LoginView extends View {

    private final UserDao userDao = new UserDao();
    private final JTextField usernameField = Theme.textField();
    private final JPasswordField passwordField = Theme.passwordField();
    private final JLabel message = Theme.muted(" ");

    public LoginView(AppFrame app) {
        super(app, "로그인", "가입할 때 사용한 아이디와 비밀번호를 입력해 주세요.");

        setBody(buildForm());

        JButton back = Theme.ghost("← 처음으로");
        back.addActionListener(e -> app.show(new HomeView(app)));
        setBackAction(back);

        JButton submit = Theme.primary("로그인");
        submit.addActionListener(e -> attemptLogin());
        setPrimaryAction(submit);

        // 엔터로도 다음 칸 이동 / 로그인이 되도록 한다.
        passwordField.addActionListener(e -> attemptLogin());
        usernameField.addActionListener(e -> passwordField.requestFocusInWindow());
    }

    private JComponent buildForm() {
        JPanel fields = Theme.stack();
        fields.add(Theme.labeledField("아이디", usernameField));
        fields.add(Box.createVerticalStrut(16));
        fields.add(Theme.labeledField("비밀번호", passwordField));
        fields.add(Box.createVerticalStrut(14));
        message.setBorder(new EmptyBorder(0, 2, 0, 0));
        fields.add(message);

        JPanel card = Theme.card(new BorderLayout());
        card.add(Theme.top(fields), BorderLayout.CENTER);
        card.setPreferredSize(new Dimension(400, card.getPreferredSize().height));

        JPanel centered = Theme.panel(new java.awt.GridBagLayout());
        centered.add(card);
        return centered;
    }

    private void attemptLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            showMessage("아이디와 비밀번호를 모두 입력해 주세요.", Theme.DANGER);
            return;
        }

        try {
            Optional<User> user = userDao.login(username, password);
            if (user.isEmpty()) {
                showMessage("아이디 또는 비밀번호가 올바르지 않습니다.", Theme.DANGER);
                passwordField.setText("");
                return;
            }
            app.signIn(user.get());
            app.show(new DashboardView(app));
        } catch (DataAccessException e) {
            showMessage(e.getMessage(), Theme.DANGER);
        }
    }

    private void showMessage(String text, java.awt.Color color) {
        message.setText(text);
        message.setForeground(color);
    }
}
