package org.example;

import com.microsoft.playwright.*;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class LoginWithSmsTest {
    static Playwright playwright;
    static Browser browser;
    BrowserContext context;
    Page page;

    @BeforeAll
    static void setUpAll() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(false));
    }

    @BeforeEach
    void setUp() {
        context = browser.newContext();
        page = context.newPage();
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @AfterAll
    static void tearDownAll() {
        browser.close();
        playwright.close();
    }

    @Test
    void loginWithSms() {
        System.out.println("Открываем сайт 1xbet.kz");
        page.navigate("https://1xbet.kz/");

        // ====== ШАГ 1: кнопка "Войти" в шапке ======
        System.out.println("Жмём 'Войти' в шапке");
        page.locator("#login-form-call").click();

        // ====== ШАГ 2: вводим ID ======
        System.out.println("Вводим ID");
        page.fill("#auth_id_email", "168715375");

        // ====== ШАГ 3: вводим пароль ======
        System.out.println("Вводим пароль");
        page.fill("#auth-form-password", "Aezakmi11+");

        // ====== ШАГ 4: кнопка "Войти" в форме авторизации ======
        System.out.println("Жмём 'Войти' в форме авторизации");
        page.locator("form button.auth-button.auth-button--block").click();

        // ====== ШАГ 5: модальное окно SMS ======
        System.out.println("Ждём модальное окно SMS");
        page.waitForSelector(".phone-sms-modal-content__send");

        // ====== ШАГ 6: жмём "Выслать код" ======
        System.out.println("Жмём 'Выслать код'");
        page.locator("button.phone-sms-modal-content__send").click();

        // Даем 10 секунд на капчу
        page.waitForTimeout(10000);

        // ====== ШАГ 7: открываем Google Messages ======
        System.out.println("Открываем Google Messages");
        Page smsPage = context.newPage();
        smsPage.navigate("https://messages.google.com/web/conversations");

        // ====== ШАГ 8: закрываем всплывающее "Нет, не нужно" ======
        System.out.println("Закрываем уведомление 'Нет, не нужно' (если есть)");
        Locator noButton = smsPage.locator("button[data-e2e-remember-this-computer-cancel]");
        if (noButton.isVisible()) {
            noButton.click();
        }

        // ====== ШАГ 9: жмём "Подключить, отсканировав QR-код" ======
        System.out.println("Жмём кнопку 'Подключить, отсканировав QR-код'");
        smsPage.locator("span.qr-text").click();

        // ====== ШАГ 10: открываем последнее непрочитанное SMS ======
        System.out.println("Ищем последнее непрочитанное сообщение");
        smsPage.waitForSelector("div.text-content.unread", new Page.WaitForSelectorOptions().setTimeout(60000));
        Locator unreadMessages = smsPage.locator("div.text-content.unread")
                .locator("xpath=ancestor::a[contains(@class,'list-item')]");
        unreadMessages.last().click();

        // ====== ШАГ 11: берём текст сообщения ======
        smsPage.waitForSelector("//div[contains(@class,'text-msg')]");
        String smsText = smsPage.locator("//div[contains(@class,'text-msg')]").last().innerText();
        System.out.println("СМС сообщение: " + smsText);

        // Извлекаем буквенно-цифровой код
        String code = smsText.replaceAll("[^a-zA-Z0-9]", "");
        System.out.println("Извлечённый код подтверждения: " + code);

        // ====== ШАГ 12: возвращаемся на 1xbet.kz ======
        System.out.println("Возвращаемся на сайт 1xbet.kz");
        page.bringToFront();

        System.out.println("Вставляем код");
        page.fill("input.phone-sms-modal-code__input", code);

        System.out.println("Жмём 'Подтвердить'");
        page.locator("button.phone-sms-modal-content__send").last().click();

        // ====== Проверка ======
        System.out.println("Проверяем успешный вход");
        page.waitForTimeout(3000);
        String bodyText = page.locator("body").innerText();

        assertTrue(
                bodyText.contains("Мой аккаунт") || bodyText.contains("Баланс"),
                "Не удалось войти в аккаунт!"
        );
    }
}
