package ru.pet.project.parsing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Что делает парсер: скачивает HTML страницы товара и ищет цену в разметке schema.org,
// которую магазины добавляют для поисковиков:
// 1) <script type="application/ld+json"> с объектом "offers": {"price": ...};
// 2) <meta itemprop="price" content="..."> или <meta property="product:price:amount" ...>.

@Component
public class SchemaOrgPriceParser implements PriceParser {
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);
    private static final String USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/130.0 Safari/537.36";

    //регулярные выражения
    private static final Pattern LD_JSON = Pattern.compile(
            "<script[^>]*type\\s*=\\s*[\"']application/ld\\+json[\"'][^>]*>(.*?)</script>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern META_TAG = Pattern.compile("<meta\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern META_IS_PRICE = Pattern.compile(
            "(?:itemprop|property)\\s*=\\s*[\"'](?:price|product:price:amount)[\"']", Pattern.CASE_INSENSITIVE);
    private static final Pattern META_CONTENT = Pattern.compile(
            "content\\s*=\\s*[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .cookieHandler(new CookieManager())   // хранит cookie между переадресациями, как браузер
            .build();
    private final ObjectMapper objectMapper; //превращает текст JSON в Java-объекты

    public SchemaOrgPriceParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public BigDecimal fetchPrice(ParseTarget target) throws ParseFailedException {
        String html = download(target.url());
        return priceFromJsonLd(html)
                .or(() -> priceFromMeta(html))
                .orElseThrow(() -> new ParseFailedException(looksLikeBotCheck(html)
                        ? "Сайт показал проверку «вы не робот»"
                        : "Цена не найдена на странице"));
    }

    private String download(String url) throws ParseFailedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml")
                .header("Accept-Language", "ru-RU,ru;q=0.9")
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new ParseFailedException("Сайт ответил кодом " + response.statusCode());
            }
            return response.body();
        } catch (IOException e) {
            throw new ParseFailedException("Не удалось открыть страницу: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ParseFailedException("Загрузка страницы прервана", e);
        }
    }

    // Самая низкая цена среди всех блоков ld+json на странице
    private Optional<BigDecimal> priceFromJsonLd(String html) {
        Optional<BigDecimal> best = Optional.empty();
        Matcher matcher = LD_JSON.matcher(html);
        while (matcher.find()) {
            try {
                JsonNode root = objectMapper.readTree(matcher.group(1).trim());
                best = min(best, findOfferPrice(root));
            } catch (JsonProcessingException e) {
                // Блок с ошибкой в JSON пропускаем и смотрим следующие
            }
        }
        return best;
    }

    // Ищет "offers" на любой глубине: товар бывает внутри @graph, массивов и вложенных объектов
    private Optional<BigDecimal> findOfferPrice(JsonNode node) {
        Optional<BigDecimal> best = Optional.empty();
        if (node.isObject() && node.has("offers")) {
            best = priceOfOffers(node.get("offers"));
        }
        if (node.isContainerNode()) {
            for (JsonNode child : node) {
                best = min(best, findOfferPrice(child));
            }
        }
        return best;
    }

    // offers бывает одним объектом, массивом или AggregateOffer с lowPrice
    private Optional<BigDecimal> priceOfOffers(JsonNode offers) {
        if (offers.isArray()) {
            Optional<BigDecimal> best = Optional.empty();
            for (JsonNode offer : offers) {
                best = min(best, priceOfOffers(offer));
            }
            return best;
        }
        if (!offers.isObject()) {
            return Optional.empty();
        }
        Optional<BigDecimal> price = parsePrice(offers.path("price").asText(""))
                .or(() -> parsePrice(offers.path("lowPrice").asText("")));
        if (price.isEmpty() && offers.has("offers")) {
            return priceOfOffers(offers.get("offers"));
        }
        return price;
    }

    private Optional<BigDecimal> priceFromMeta(String html) {
        Matcher tags = META_TAG.matcher(html);
        while (tags.find()) {
            String tag = tags.group();
            if (!META_IS_PRICE.matcher(tag).find()) {
                continue;
            }
            Matcher content = META_CONTENT.matcher(tag);
            if (content.find()) {
                Optional<BigDecimal> price = parsePrice(content.group(1));
                if (price.isPresent()) {
                    return price;
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<BigDecimal> parsePrice(String text) {
        String cleaned = text.replaceAll("[\\s\\u00a0\\u202f]", "").replace(',', '.');
        if (!cleaned.matches("\\d+(\\.\\d+)?")) {
            return Optional.empty();
        }
        BigDecimal price = new BigDecimal(cleaned).setScale(2, RoundingMode.HALF_UP);
        return price.signum() > 0 ? Optional.of(price) : Optional.empty();
    }

    private static Optional<BigDecimal> min(Optional<BigDecimal> a, Optional<BigDecimal> b) {
        if (a.isEmpty()) {
            return b;
        }
        if (b.isEmpty()) {
            return a;
        }
        return Optional.of(a.get().min(b.get()));
    }

    private static boolean looksLikeBotCheck(String html) {
        String lower = html.toLowerCase(Locale.ROOT);
        return lower.contains("captcha") || lower.contains("antibot") || lower.contains("challenge");
    }
}