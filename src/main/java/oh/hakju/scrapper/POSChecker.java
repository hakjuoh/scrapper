package oh.hakju.scrapper;

import oh.hakju.scrapper.dao.VocabularyDAO;
import oh.hakju.scrapper.entity.Vocabulary;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class POSChecker implements Runnable, Closeable {

    private VocabularyDAO vocabularyDAO = new VocabularyDAO();

    private ExecutorService executor = Executors.newFixedThreadPool(1);

    @Override
    public void close() throws IOException {
        executor.shutdown();
    }

    @Override
    public void run() {
        for (Vocabulary vocabulary : vocabularyDAO.findAll()) {
            executor.execute(new Worker(vocabulary));
        }
    }

    private static final String CHROME_DRIVER_PROPERTY_KEY = "webdriver.chrome.driver";

    private WebDriver webDriver() {
        String webDriverFilePath;
        try {
            webDriverFilePath = new File(getClass().getClassLoader().getResource("chromedriver").getFile()).getCanonicalPath();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        System.setProperty(CHROME_DRIVER_PROPERTY_KEY, webDriverFilePath);

        ChromeDriver driver = new ChromeDriver();

        driver.manage().window().maximize();
        WebDriver.Timeouts timeouts = driver.manage().timeouts();
        timeouts.pageLoadTimeout(10L, TimeUnit.SECONDS);

        return driver;
    }

    private class Worker implements Runnable {

        private Vocabulary vocabulary;

        public Worker(Vocabulary vocabulary) {
            this.vocabulary = vocabulary;
        }

        @Override
        public void run() {
            WebDriver webDriver = webDriver();
            try {
                webDriver.get("https://www.google.com/search?site=async/dictw&q=Dictionary");
                WebElement webElement = webDriver.findElement(By.cssSelector("input[type=text].dw-sbi"));
                webElement.sendKeys(vocabulary.getWord());

                WebElement button = webDriver.findElement(By.cssSelector("div.dw-sb-btn"));
                button.click();

                WebDriverWait wait = new WebDriverWait(webDriver, 5);
                List<WebElement> elements = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector("div.lr_dct_sf_h")));

                String pos = elements.get(0).getText();

                if (Arrays.asList("adjective", "preposition").contains(pos)) {
                    vocabulary.setPos(Character.toUpperCase(pos.charAt(0)) + pos.substring(1));
                    vocabularyDAO.update(vocabulary);
                } else if (!vocabulary.getPos().equalsIgnoreCase(pos)) {
                    System.out.println(vocabulary + " -> " + pos);
                }
            } finally {
                webDriver.close();
            }

        }
    }

    public static void main(String[] args) throws Exception {
        try (POSChecker posChecker = new POSChecker()) {
            posChecker.run();
        }
    }
}
