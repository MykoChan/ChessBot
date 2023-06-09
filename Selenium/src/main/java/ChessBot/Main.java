package ChessBot;

import net.andreinc.neatchess.client.UCI;
import net.andreinc.neatchess.client.UCIResponse;
import net.andreinc.neatchess.client.model.EngineInfo;
import net.andreinc.neatchess.client.model.option.EngineOption;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;
import java.util.Map;

public class Main {

    static String PLAYER = "white";
    static String OPPONENT = "black";

    public static void main(String[] args) throws InterruptedException {
        retrieveInfo();

        boolean checkmate = false;
        String bestMove, to, from;
        String fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

        System.setProperty("webdriver.chrome.driver", "C:/Users/Michael/Downloads/chromedriver_win32/chromedriver.exe");
        WebDriver driver = new ChromeDriver();
        Actions action = new Actions(driver);

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        driver.get("https://www.chess.com/play/computer");
        click(action, driver.findElement(By.className("ui_outside-close-icon")));
        Thread.sleep(1000);
        click(action, driver.findElement(By.cssSelector("button.ui_v5-button-component.ui_v5-button-primary.ui_v5-button-large.selection-menu-button")));

        Thread.sleep(1000);
        click(action, driver.findElement(By.cssSelector("button.ui_v5-button-component.ui_v5-button-primary.ui_v5-button-large.ui_v5-button-full")));

        while (!checkmate) {

            // continuously get the status of the board each second until it's our turn
            do {
                Thread.sleep(2000);
                fen = getFen(driver, wait, action);
            } while (getTurn(fen) != PLAYER);


            // given the current state of the board, play the best move generated by stockfish
            bestMove = getBestMove(fen);

            // translate the best move into two squares to click.
            from = ".square-" + convertMoveToCoordinate(bestMove.substring(0, 2));
            to = ".square-" + convertMoveToCoordinate(bestMove.substring(2));

            Thread.sleep(2000);
            playMove(action, driver, wait, from, to);

            if (to.contains("#")){
                checkmate = true;
            }
        }



        driver.close();
    }

    public static void retrieveInfo(){
        var uci = new UCI(5000l); // default timeout 5 seconds
        uci.startStockfish();
        uci.setOption("Ponder", "false");
        UCIResponse<EngineInfo> response = uci.getEngineInfo();
        if (response.success()) {

            // Engine name
            EngineInfo engineInfo = response.getResult();
            System.out.println("Engine name:" + engineInfo.getName());

            // Supported engine options
            System.out.println("Supported engine options:");
            Map<String, EngineOption> engineOptions = engineInfo.getOptions();
            engineOptions.forEach((key, value) -> {
                System.out.println("\t" + key);
                System.out.println("\t\t" + value);
            });
        }
        uci.close();
    }

    public static String getTurn(String fen) {
        if (fen.split(" ")[1].equals("w")) {
            return "white";
        }
        return "black";
    }

    public static String getFen(WebDriver driver, WebDriverWait wait, Actions action) {
        // Get fen from download option
        click(action, driver.findElement(By.cssSelector(".small-controls-icon.icon-font-chess.download")));

        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("div.share-menu-tab-pgn-section > input.ui_v5-input-component")));
        String fen = driver.findElement((By.cssSelector("div.share-menu-tab-pgn-section > input.ui_v5-input-component"))).getAttribute("value");

        // close modal
        click(action, driver.findElement(By.cssSelector(".icon-font-chess.x.ui_outside-close-icon")));
        return fen;
    }

    public static void playMove(Actions action, WebDriver driver, WebDriverWait wait, String from, String to) {
        click(action, driver.findElement(By.cssSelector(from)));
//        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".hint")));
        click(action, driver.findElement(By.cssSelector(to)));
    }

    public static void click(Actions action, WebElement target) {
        action.moveToElement(target).click().perform();
    }
    public static String convertMoveToCoordinate(String move) {
        char firstChar = move.charAt(0);
        int firstCharOrd = (int) firstChar;
        firstCharOrd -= 96;
        String result = firstCharOrd + Character.toString(move.charAt(1));
        return result;
    }

    public static String getBestMove(String fen) {
        var uci = new UCI();
        uci.startStockfish();

        uci.uciNewGame();
        uci.positionFen(fen);

        String result10depth = uci.bestMove(20).getResultOrThrow().toString().split("=")[1].split(",")[0];
        uci.close();

        return result10depth;
    }
}
