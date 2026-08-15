package com.cineseat.ui;

import com.cineseat.model.User;
import com.cineseat.ui.view.HomeView;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;

/**
 * 애플리케이션 창 하나를 유지하면서 화면만 갈아 끼운다.
 *
 * <p>화면마다 새 창을 띄우지 않기 때문에 창이 쌓이지 않고, 로그인한 사용자 정보도 한 곳에서
 * 관리된다.
 */
public class AppFrame extends JFrame {

    private final JPanel host = new JPanel(new BorderLayout());
    private User currentUser;

    public AppFrame() {
        setTitle("CineSeat · 영화 좌석 예매");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(880, 620));
        setSize(940, 680);

        host.setBackground(Theme.BG);
        setContentPane(host);
        setLocationRelativeTo(null);
    }

    /** 지금 보이는 화면을 새 화면으로 바꾼다. */
    public void show(JPanel view) {
        host.removeAll();
        host.add(view, BorderLayout.CENTER);
        host.revalidate();
        host.repaint();
    }

    public User currentUser() {
        return currentUser;
    }

    public void signIn(User user) {
        this.currentUser = user;
    }

    public void signOut() {
        this.currentUser = null;
        show(new HomeView(this));
    }
}
