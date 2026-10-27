package ru.pet.project.marketplace;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

// Приводит ссылку к единому виду, чтобы одна и та же страница товара не попадала в таблицу links дважды:

public final class UrlNormalizer {

    // Как в CHECK-ограничении таблицы links
    public static final int MAX_LENGTH = 2048;

    private UrlNormalizer() {
    }
    public record NormalizedUrl(String url, String host) {
    }

    // @throws IllegalArgumentException с понятным текстом, если это не ссылка на сайт
    public static NormalizedUrl normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Ссылка не заполнена");
        }
        String text = raw.trim();
        // ссылка без схемы: «ozon.ru/product/123»
        if (!text.contains("://")) {
            text = "https://" + text;
        }

        URI uri;
        try {
            uri = new URI(text);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Это не похоже на ссылку");
        }

        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("https") || scheme.equalsIgnoreCase("http"))) {
            throw new IllegalArgumentException("Ссылка должна вести на сайт и начинаться с https://");
        }
        if (uri.getHost() == null) {
            throw new IllegalArgumentException("В ссылке нет адреса сайта");
        }

        String host = uri.getHost().toLowerCase(Locale.ROOT);
        if (host.startsWith("www.")) {
            host = host.substring("www.".length());
        }

        String path = uri.getRawPath();
        if (path == null || path.isEmpty()) {
            path = "/";
        }

        String query = withoutTrackingParams(uri.getRawQuery());
        String url = "https://" + host + path + (query.isEmpty() ? "" : "?" + query);

        if (url.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Ссылка слишком длинная");
        }
        return new NormalizedUrl(url, host);
    }

    private static String withoutTrackingParams(String rawQuery) {
        if (rawQuery == null || rawQuery.isEmpty()) {
            return "";
        }
        return Arrays.stream(rawQuery.split("&"))
                .filter(param -> !param.isEmpty())
                .filter(param -> !param.toLowerCase(Locale.ROOT).startsWith("utm_"))
                .collect(Collectors.joining("&"));
    }
}