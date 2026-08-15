package com.cineseat;

import com.cineseat.db.DataAccessException;
import com.cineseat.db.Database;
import com.cineseat.ui.AppFrame;
import com.cineseat.ui.Theme;
import com.cineseat.ui.view.HomeView;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.nio.file.Files;
import java.nio.file.Path;

/** 애플리케이션 진입점. */
public final class CineSeatApp {

    private static final Path APP_ICON = Path.of("assets", "film-reel.png");

    private CineSeatApp() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            applyLookAndFeel();

            if (!checkDatabase()) {
                return;
            }

            AppFrame frame = new AppFrame();
            loadIcon(frame);
            frame.show(new HomeView(frame));
            frame.setVisible(true);
        });
    }

    /** 플랫폼별 기본 모양 대신 어디서나 같게 보이는 기본 룩앤필을 쓴다. */
    private static void applyLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
            // 실패하면 기본 룩앤필을 그대로 쓴다.
        }
        Theme.applyGlobalDefaults();
    }

    /** DB 에 연결되지 않으면 빈 화면 대신 원인을 알려 주고 종료한다. */
    private static boolean checkDatabase() {
        try {
            Database.verifyConnection();
            return true;
        } catch (DataAccessException e) {
            JOptionPane.showMessageDialog(null,
                    """
                    데이터베이스에 연결하지 못했습니다.

                    · MySQL 이 실행 중인지 확인해 주세요.
                    · db/schema.sql 을 실행해 테이블과 예시 데이터를 만들어 주세요.
                    · config/config.properties 의 접속 정보를 확인해 주세요.

                    접속 대상: %s""".formatted(Database.describeTarget()),
                    "연결 실패", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private static void loadIcon(AppFrame frame) {
        if (Files.exists(APP_ICON)) {
            frame.setIconImage(new ImageIcon(APP_ICON.toString()).getImage());
        }
    }
}
