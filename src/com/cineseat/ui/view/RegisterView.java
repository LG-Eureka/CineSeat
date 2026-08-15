package com.cineseat.ui.view;

import com.cineseat.dao.UserDao;
import com.cineseat.db.DataAccessException;
import com.cineseat.ui.AppFrame;
import com.cineseat.ui.Dialogs;
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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagLayout;

/** 회원가입 화면. */
public class RegisterView extends View {

    private static final int MIN_PASSWORD_LENGTH = 4;

    private final UserDao userDao = new UserDao();
    private final JTextField usernameField = Theme.textField();
    private final JPasswordField passwordField = Theme.passwordField();
    private final JPasswordField confirmField = Theme.passwordField();
    private final JTextField ageField = Theme.textField();
    private final JLabel message = Theme.muted(" ");

    public RegisterView(AppFrame app) {
        super(app, "회원가입", "아이디는 나중에 바꿀 수 없으니 신중하게 정해 주세요.");

        setBody(buildForm());

        JButton back = Theme.ghost("← 처음으로");
        back.addActionListener(e -> app.show(new HomeView(app)));
        setBackAction(back);

        JButton submit = Theme.primary("가입하기");
        submit.addActionListener(e -> attemptRegister());
        setPrimaryAction(submit);

        ageField.addActionListener(e -> attemptRegister());
    }

    private JComponent buildForm() {
        JPanel fields = Theme.stack();
        fields.add(Theme.labeledField("아이디", usernameField));
        fields.add(Box.createVerticalStrut(14));
        fields.add(Theme.labeledField("비밀번호 (" + MIN_PASSWORD_LENGTH + "자 이상)", passwordField));
        fields.add(Box.createVerticalStrut(14));
        fields.add(Theme.labeledField("비밀번호 확인", confirmField));
        fields.add(Box.createVerticalStrut(14));
        fields.add(Theme.labeledField("나이", ageField));
        fields.add(Box.createVerticalStrut(14));
        message.setBorder(new EmptyBorder(0, 2, 0, 0));
        fields.add(message);

        JPanel card = Theme.card(new BorderLayout());
        card.add(Theme.top(fields), BorderLayout.CENTER);
        card.setPreferredSize(new Dimension(400, card.getPreferredSize().height));

        JPanel centered = Theme.panel(new GridBagLayout());
        centered.add(card);
        return centered;
    }

    private void attemptRegister() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirm = new String(confirmField.getPassword());
        String ageText = ageField.getText().trim();

        if (username.isEmpty()) {
            showMessage("아이디를 입력해 주세요.", Theme.DANGER);
            return;
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            showMessage("비밀번호는 " + MIN_PASSWORD_LENGTH + "자 이상이어야 합니다.", Theme.DANGER);
            return;
        }
        if (!password.equals(confirm)) {
            showMessage("비밀번호가 서로 다릅니다.", Theme.DANGER);
            return;
        }

        int age;
        try {
            age = Integer.parseInt(ageText);
        } catch (NumberFormatException e) {
            showMessage("나이는 숫자로 입력해 주세요.", Theme.DANGER);
            return;
        }
        if (age < 1 || age > 120) {
            showMessage("나이를 1에서 120 사이로 입력해 주세요.", Theme.DANGER);
            return;
        }

        try {
            if (userDao.exists(username)) {
                showMessage("이미 사용 중인 아이디입니다.", Theme.DANGER);
                return;
            }
            if (!userDao.register(username, password, age)) {
                showMessage("이미 사용 중인 아이디입니다.", Theme.DANGER);
                return;
            }
            Dialogs.info(this, "가입이 완료되었습니다. 이제 로그인해 주세요.");
            app.show(new LoginView(app));
        } catch (DataAccessException e) {
            showMessage(e.getMessage(), Theme.DANGER);
        }
    }

    private void showMessage(String text, Color color) {
        message.setText(text);
        message.setForeground(color);
    }
}
