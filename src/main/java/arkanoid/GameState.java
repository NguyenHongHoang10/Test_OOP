package arkanoid;

import java.util.ArrayList;
import java.util.List;

public class GameState {
    private int lives = 3;
    private int score = 0;
    private double scoreMultiplier = 1.0;
    private boolean running = false; // bắt đầu false để hiển thị menu
    private boolean win = false; // thắng hay thua
    private boolean showMessage = false; // hiển thị overlay thông báo
    private boolean gameStarted = false; // true nếu đã từng bắt đầu 1 trận
    private boolean pauseOverlay = false; // true khi đang pause (P)
    private boolean confirmOverlay = false; // true khi ESC hiện confirm (Y/N)
    private boolean settingsOverlay = false;
    private boolean barrierActive = false;
    private double barrierY = -1;
    private final double barrierThickness = 6;
    private int currentLevelIndex = 0;
    private boolean levelComplete = false; // Cờ hiệu báo đã qua màn
    private boolean gameComplete = false; // Cờ hiệu báo đã thắng toàn bộ game

    public GameState() {
    }

    // Getters
    public int getLives() {
        return lives;
    }

    public int getScore() {
        return score;
    }

    public double getScoreMultiplier() {
        return scoreMultiplier;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isWin() {
        return win;
    }

    public boolean isShowMessage() {
        return showMessage;
    }

    public boolean isGameStarted() {
        return gameStarted;
    }

    public boolean isPauseOverlay() {
        return pauseOverlay;
    }

    public boolean isConfirmOverlay() {
        return confirmOverlay;
    }

    public boolean isSettingsOverlay() { return settingsOverlay; }

    public boolean isBarrierActive() {
        return barrierActive;
    }

    public double getBarrierY() {
        return barrierY;
    }

    public double getBarrierThickness() {
        return barrierThickness;
    }

    public int getCurrentLevelIndex() {
        return currentLevelIndex;
    }

    public boolean isLevelComplete() {
        return levelComplete;
    }

    public boolean isGameComplete() {
        return gameComplete;
    }

    // Setters & Modifiers
    public void setLives(int lives) {
        this.lives = lives;
    }

    public void decrementLives() {
        this.lives--;
    }

    public void incrementLives() {
        this.lives++;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public void addScore(int base) {
        int added = (int) Math.round(base * this.scoreMultiplier);
        this.score += added;
    }

    public void setScoreMultiplier(double multiplier) {
        this.scoreMultiplier = multiplier;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public void setWin(boolean win) {
        this.win = win;
    }

    public void setShowMessage(boolean showMessage) {
        this.showMessage = showMessage;
    }

    public void setPauseOverlay(boolean pauseOverlay) {
        this.pauseOverlay = pauseOverlay;
    }

    public void setConfirmOverlay(boolean confirmOverlay) {
        this.confirmOverlay = confirmOverlay;
    }

    public void setSettingsOverlay(boolean settingsOverlay) { this.settingsOverlay = settingsOverlay; }

    public void setBarrierActive(boolean barrierActive, double yPosition) {
        this.barrierActive = barrierActive;
        if (barrierActive) this.barrierY = yPosition;
    }

    public void consumeBarrier() {
        this.barrierActive = false;
    }

    public void setCurrentLevelIndex(int index) {
        this.currentLevelIndex = index;
    }

    public void setLevelComplete(boolean complete) {
        this.levelComplete = complete;
    }

    public void setGameComplete(boolean complete) {
        this.gameComplete = complete;
    }

    public void incrementLevelIndex() {
        this.currentLevelIndex++;
    }

    public void setGameStarted(boolean gameStarted) {
        this.gameStarted = gameStarted;
    }

    public void resetForNewGame() {
        scoreMultiplier = 1.0;
        win = false;
        showMessage = false;
        running = true;
        gameStarted = true;
        pauseOverlay = false;
        confirmOverlay = false;
        barrierActive = false;
        currentLevelIndex = 0;
        levelComplete = false; // Reset cờ
        gameComplete = false; // Reset cờ
    }
}
