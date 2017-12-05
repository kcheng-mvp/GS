#! /usr/bin/env groovy

@Grapes([
        @Grab(group = 'org.seleniumhq.selenium', module = 'selenium-chrome-driver', version = '3.7.1'),
        @Grab(group = 'org.seleniumhq.selenium', module = 'selenium-support', version = '3.7.1')
])

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.WebDriverWait
import org.openqa.selenium.support.ui.ExpectedConditions

def currentPath = new File(getClass().protectionDomain.codeSource.location.path).parent
GroovyShell groovyShell = new GroovyShell()
def cron4j = groovyShell.parse(new File(currentPath, "core/Cron4J.groovy"))


System.setProperty("webdriver.chrome.driver", "/Users/kcheng/tools/chromedriver")


def driver = new ChromeDriver()
driver.manage().window().maximize()
def wait = new WebDriverWait(driver, 5);
driver.get("http://120.78.49.192/")
def ele = wait.until(ExpectedConditions.presenceOfElementLocated(By.name("j_username")));
ele.sendKeys("kcheng.mvp")
ele = wait.until(ExpectedConditions.presenceOfElementLocated(By.name("j_password")));
ele.sendKeys("whoami@xly")
ele = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("yui-gen1-button")));
ele.click()

ele = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//a")))
ele = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.xpath("//a[starts-with(@href,'job/') and contains(@href,'delay=0sec')]")));


def click = {

    driver.navigate().refresh();
    ele = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.xpath("//a[starts-with(@href,'job/') and contains(@href,'delay=0sec')]")));
    def index = (System.currentTimeMillis() % 7) as Integer
    println ele.get(index).getAttribute("href")
    ele.get(index).click()

    driver.navigate().refresh();
    ele = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.xpath("//a[starts-with(@href,'job/') and contains(@href,'delay=0sec')]")));
    index = (System.currentTimeMillis() % 7) as Integer
    println ele.get(index).getAttribute("href")
    ele.get(index).click()


    driver.navigate().refresh();
    ele = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.xpath("//a[starts-with(@href,'job/') and contains(@href,'delay=0sec')]")));
    index = (System.currentTimeMillis() % 7) as Integer
    println ele.get(index).getAttribute("href")
    ele.get(index).click()

    driver.navigate().refresh();
    ele = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.xpath("//a[starts-with(@href,'job/') and contains(@href,'delay=0sec')]")));
    index = (System.currentTimeMillis() % 7) as Integer
    println ele.get(index).getAttribute("href")
    ele.get(index).click()


} as Runnable


def cron = "* * * * *"
cron4j.start(cron, click)

