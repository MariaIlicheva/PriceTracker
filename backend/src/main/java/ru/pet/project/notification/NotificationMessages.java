package ru.pet.project.notification;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Component;
import ru.pet.project.product.Link;
import ru.pet.project.product.Product;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

// Собирает письмо «цена снизилась» из уведомления.
// Вызывается внутри транзакции: читает ленивые связи
@Component
public class NotificationMessages {

    private static final Locale RUSSIAN = Locale.forLanguageTag("ru-RU");
    private final NotificationProperties properties;

    public NotificationMessages(NotificationProperties properties) {
        this.properties = properties;
    }

    public SimpleMailMessage priceDropped(Notification notification) {
        Product product = notification.getProduct();
        Link link = notification.getLink();
        String price = formatPrice(notification.getPrice());

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.from());
        message.setTo(product.getUser().getEmail());
        message.setSubject("Цена снизилась: " + product.getName() + " — " + price);
        message.setText("""
                Здравствуйте! Хорошая новость!

                Цена на «%s» опустилась ниже порога, который вы задали.

                Магазин: %s
                Цена: %s (порог %s)
                Ссылка на товар: %s

                Это письмо отправил PriceTracker. Если Вы не хотите получать письма по этому товару,
                поставьте его на паузу на странице товара.
                """.formatted(
                product.getName(),
                link.getMarketplace().getName(),
                price,
                formatPrice(product.getTargetPrice()),
                link.getUrl()));

        return message;
    }

    private static String formatPrice(BigDecimal value) {
        NumberFormat format = NumberFormat.getNumberInstance(RUSSIAN);
        format.setMaximumFractionDigits(2);
        return format.format(value) + " ₽";
    }
}