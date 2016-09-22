package org.ssproj;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

public class ChromeTraverser extends Traverser {
    private final long scrollWait = 100;

    public ChromeTraverser(TraverserContext context) {
        super(context);
    }

    protected WebDriver startDriver() {
        LOGGER.info("starting chrome driver");
        return new ChromeDriver();
    }

    protected void saveScreenShot(URL url) throws IOException, InterruptedException {
        JavascriptExecutor jexec = (JavascriptExecutor) getDriver();

        //画面サイズで必要なものを取得
        int innerH = Integer.parseInt(String.valueOf(jexec.executeScript("return window.innerHeight")));
        int innerW =Integer.parseInt(String.valueOf(jexec.executeScript("return window.innerWidth")));
        int scrollH = Integer.parseInt(String.valueOf(jexec.executeScript("return document.documentElement.scrollHeight")));
        int devicePixelRatio = Integer.parseInt(String.valueOf(jexec.executeScript("return window.devicePixelRatio")));

        //スクロールを行うかの判定
        if (innerH > scrollH) {
            BufferedImage img = ImageIO.read(((TakesScreenshot) getDriver()).getScreenshotAs(OutputType.FILE));
            ImageIO.write(img, "png", getOutputFile());
        } else {
            //イメージを扱うための準備
            BufferedImage img = new BufferedImage(innerW * devicePixelRatio, scrollH * devicePixelRatio, BufferedImage.TYPE_INT_ARGB);
            Graphics g = img.getGraphics();

            // int scrollableH = scrollH;
            int i = 0;
            int y = 0;

            //スクロールしながらなんどもイメージを結合していく
            while (scrollH > y + innerH) {
                BufferedImage imageParts = ImageIO.read(((TakesScreenshot) getDriver()).getScreenshotAs(OutputType.FILE));
                //ImageIO.write(imageParts, "PNG", getOutputFile(url, i));

                g.drawImage(imageParts, 0, y * devicePixelRatio, innerW * devicePixelRatio, innerH * devicePixelRatio, null);
                y += innerH - 20;
                i++;
                jexec.executeScript("window.scrollTo(0," + y + ")");

                try {
                    Thread.sleep(this.scrollWait);
                } catch (InterruptedException e) {
                    break;
                }
            }

            //一番下まで行ったときは、下から埋めるように貼り付け
            BufferedImage imageParts = ImageIO.read(((TakesScreenshot) getDriver()).getScreenshotAs(OutputType.FILE));
            //ImageIO.write(imageParts, "PNG", getOutputFile(url, i));
            g.drawImage(imageParts, 0, (scrollH - innerH) * devicePixelRatio, innerW * devicePixelRatio, innerH * devicePixelRatio, null);

            ImageIO.write(img, "png", getOutputFile());
        }
        compressPng(getOutputFile());
    }
}
