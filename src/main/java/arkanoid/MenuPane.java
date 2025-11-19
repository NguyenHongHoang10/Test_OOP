package arkanoid;


import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Duration;


public class MenuPane extends VBox {
    private final Game game;


    private final Button startBtn;
    private final Button continueBtn;
    private final Button exitBtn;
    private final Button howToPlayBtn;
    private final Button leaderboardBtn;
    private final Button settingBtn;


    private final Text continueErrorText;

    // Constructor 4 tham số
    public MenuPane(Game game, Runnable startCallback, Runnable continueCallback, Runnable exitCallback) {
        this(game, startCallback, continueCallback, null, null, null, exitCallback);
    }


    // Constructor 5 tham số
    public MenuPane(Game game,
                    Runnable startCallback,
                    Runnable continueCallback,
                    Runnable continueSavedCallback,
                    Runnable exitCallback) {
        this(game, startCallback, continueCallback, continueSavedCallback, null, exitCallback);
    }


    // Constructor 6 tham số
    public MenuPane(Game game,
                    Runnable startCallback,
                    Runnable continueCallback,
                    Runnable continueSavedCallback,
                    Runnable leaderboardCallback,
                    Runnable exitCallback) {
        this(game, startCallback, continueCallback, continueSavedCallback,
                leaderboardCallback, null, null, exitCallback);
    }


    // Constructor đầy đủ với 7 tham số
    public MenuPane(Game game,
                    Runnable startCallback,
                    Runnable continueCallback,
                    Runnable continueSavedCallback,
                    Runnable leaderboardCallback,
                    Runnable howToPlayCallback,
                    Runnable exitCallback) {
        this(game, startCallback, continueCallback, continueSavedCallback,
                leaderboardCallback, howToPlayCallback, null, exitCallback);
    }


    //Constructor đầy đủ với 8 tham số
    public MenuPane(Game game,
                    Runnable startCallback,
                    Runnable continueCallback,
                    Runnable continueSavedCallback,
                    Runnable leaderboardCallback,
                    Runnable howToPlayCallback,
                    Runnable settingsCallback,
                    Runnable exitCallback) {
        super(12);
        this.game = game;
        setAlignment(Pos.CENTER);
        setPadding(new Insets(20));
        setPrefSize(800, 600);
        try {
            Image bgImage = new Image(getClass().getResourceAsStream("/Image/Background/MenuBackground.jpg"));
            if (bgImage.isError()) throw new Exception("Lỗi tải ảnh");
            BackgroundSize bgSize = new BackgroundSize(1.0, 1.0, true, true, false, true);
            BackgroundImage backgroundImage = new BackgroundImage(bgImage, BackgroundRepeat.NO_REPEAT,
                    BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER, bgSize);
            setBackground(new Background(backgroundImage));
        } catch (Exception e) {
            setStyle("-fx-background-color: linear-gradient(#283048, #859398);");
        }


        Text title = new Text("ARKANOID");
        title.setFill(Color.web("#FFD966"));
        title.setFont(Font.font(48));


        startBtn = new Button("▶ Start New Game");
        styleMainButton(startBtn);
        startBtn.setOnAction(e -> {
            startCallback.run();
            SoundManager.get().play(SoundManager.Sfx.PAUSE);
        });


        continueBtn = new Button("Continue");
        styleMainButton(continueBtn);
        continueBtn.setOnAction(e -> {
            SoundManager.get().play(SoundManager.Sfx.PAUSE);

            if (canContinue()) {
                continueCallback.run();
                return;
            } else if (continueSavedCallback != null && SaveLoad.get().hasSave()) {
                continueSavedCallback.run();
                return;
            } else {
                showContinueError();
            }
        });


        // Nút How to Play
        howToPlayBtn = new Button("How to play");
        styleMainButton(howToPlayBtn);
        howToPlayBtn.setOnAction(e -> {
            if (howToPlayCallback != null) {
                howToPlayCallback.run();
            } else {
                System.out.println("How to play clicked - Not implemented");
            }
            SoundManager.get().play(SoundManager.Sfx.PAUSE);
        });


        // Nút Leaderboard
        leaderboardBtn = new Button("🏆 Leaderboard");
        styleMainButton(leaderboardBtn);
        leaderboardBtn.setOnAction(e -> {
            if (leaderboardCallback != null) leaderboardCallback.run();
            else System.out.println("Leaderboard clicked - Not implemented");
            SoundManager.get().play(SoundManager.Sfx.PAUSE);
        });


        // Nút Setting
        settingBtn = new Button("⚙ Setting");
        styleMainButton(settingBtn);
        settingBtn.setOnAction(e -> {
            if (settingsCallback != null) {
                settingsCallback.run();
            } else {
                System.out.println("Setting clicked - Not implemented");
            }
            SoundManager.get().play(SoundManager.Sfx.PAUSE);
        });


        exitBtn = new Button("❌ Exit");
        styleMainButton(exitBtn);
        exitBtn.setOnAction(e -> exitCallback.run());


        continueErrorText = new Text("Nothing to continue");
        continueErrorText.setFont(Font.font(16));
        continueErrorText.setFill(Color.ORANGERED);
        continueErrorText.setVisible(false);


        getChildren().addAll(title, startBtn, continueBtn, howToPlayBtn,
                leaderboardBtn, settingBtn, exitBtn, continueErrorText);
    }

    // Kiểm tra trạng thái của Game để quyết định có thể "Continue" hay không
    private boolean canContinue() {
        // Nếu game chưa bao giờ bắt đầu thì không thể
        if (!game.isGameStarted()) {
            return false;
        }

        GameState state = game.getGameState();

        // Nếu đang ở màn hình "Game Over" hoặc "Level Complete"
        if (state.isShowMessage() || state.isLevelComplete()) {
            return false;
        }


        // Nếu game đã bắt đầu VÀ không ở trạng thái kết thúc thì có thể
        return true;
    }


    //Hiển thị thông báo lỗi "Nothing to continue" trong 3 giây.
    private void showContinueError() {
        continueErrorText.setVisible(true);
        // Tạo một đối tượng PauseTransition để ẩn text sau 3 giây
        PauseTransition visiblePause = new PauseTransition(Duration.seconds(3));
        visiblePause.setOnFinished(event -> continueErrorText.setVisible(false));
        visiblePause.play();
    }


    private void styleMainButton(Button b) {
        b.setFont(Font.font(22));
        b.setPrefWidth(260);
        b.setStyle("-fx-background-color: linear-gradient(#26a0da, #0077b6);"
                + "-fx-text-fill: white; -fx-background-radius: 12; -fx-padding: 8 18 8 18;");
        b.setOnMouseEntered(e -> {
            b.setScaleX(1.03);
            b.setScaleY(1.03);
        });
        b.setOnMouseExited(e -> {
            b.setScaleX(1.0);
            b.setScaleY(1.0);
        });

        b.setEffect(new DropShadow(6, Color.rgb(0, 0, 0, 0.45)));
    }
}



