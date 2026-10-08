package com.k12nt.k12netframe.utils.helper;

import android.webkit.URLUtil;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Picks the name a WebView download is saved under.
 *
 * <p>URLUtil.guessFileName is not given the Content-Disposition header: below Android 15 it takes
 * everything from "filename=" to the end of the header. FSCore sends both forms of the name,
 * "attachment; filename=x.pdf; filename*=UTF-8''x.pdf", which became the file name
 * "x.pdf; filename*=UTF-8''x.pdf"; devices that refuse the '*' in it failed the download.
 */
public final class K12NetFileNameHelper {

    /**
     * Names are limited to 255 bytes; this leaves room for the "-1", "-2", ... DownloadManager adds
     * when a name is taken.
     */
    private static final int MAX_NAME_BYTES = 200;

    private K12NetFileNameHelper() {
    }

    /**
     * The name to save a download under, in a form shared storage accepts: the link's "filename"
     * parameter, else the name in the Content-Disposition header, else one guessed from the URL
     * and the MIME type.
     */
    public static String forDownload(String url, String contentDisposition, String mimeType) {
        // The link's own name comes first: for Android, GetFile.aspx replaces letters such as
        // ü and ş with '_' in the header's name.
        String name = queryParameter(url, "filename");
        if (isBlank(name)) name = fromContentDisposition(contentDisposition);
        if (isBlank(name)) name = URLUtil.guessFileName(url, null, mimeType);

        return sanitize(name);
    }

    /** The decoded value of a URL's query parameter, its name matched ignoring case; null if absent. */
    static String queryParameter(String url, String name) {
        int start = url.indexOf('?');
        if (start < 0) return null;

        int end = url.indexOf('#', start);
        String query = url.substring(start + 1, end < 0 ? url.length() : end);

        for (String pair : query.split("&")) {
            int equals = pair.indexOf('=');
            String key = equals < 0 ? pair : pair.substring(0, equals);
            if (!key.equalsIgnoreCase(name)) continue;

            // '+' is a space in a query string; a real plus arrives as %2B.
            String value = equals < 0 ? "" : pair.substring(equals + 1).replace('+', ' ');
            return percentDecode(value, StandardCharsets.UTF_8);
        }
        return null;
    }

    /**
     * The file name in a Content-Disposition header, or null. The RFC 5987 form
     * (filename*=UTF-8''...) wins: it is the only one that carries non-ASCII names, and servers
     * sending both forms put the exact name there.
     */
    static String fromContentDisposition(String header) {
        if (isBlank(header)) return null;

        String extended = headerParameter(header, "filename*");
        if (extended != null) {
            // charset'language'percent-encoded-name
            int first = extended.indexOf('\'');
            int second = first < 0 ? -1 : extended.indexOf('\'', first + 1);
            Charset charset = second > 0
                    && "ISO-8859-1".equalsIgnoreCase(extended.substring(0, first).trim())
                    ? StandardCharsets.ISO_8859_1
                    : StandardCharsets.UTF_8;

            String name = percentDecode(second < 0 ? extended : extended.substring(second + 1), charset);
            if (!isBlank(name)) return name;
        }

        String plain = headerParameter(header, "filename");
        return isBlank(plain) ? null : plain;
    }

    /**
     * A parameter's value in a header: the name matched whole and ignoring case, a quoted value
     * read up to its closing quote, an unquoted one up to the next ';'.
     */
    private static String headerParameter(String header, String name) {
        for (int at = 0; at + name.length() <= header.length(); at++) {
            if (!header.regionMatches(true, at, name, 0, name.length())) continue;

            // Not the end of a longer name...
            char before = at == 0 ? ';' : header.charAt(at - 1);
            if (before != ';' && before != ' ' && before != '\t') continue;

            // ...nor the start of one: "filename" must not match "filename*".
            int index = skipSpaces(header, at + name.length());
            if (index >= header.length() || header.charAt(index) != '=') continue;
            index = skipSpaces(header, index + 1);

            if (index < header.length() && header.charAt(index) == '"') {
                StringBuilder value = new StringBuilder();
                for (index++; index < header.length() && header.charAt(index) != '"'; index++) {
                    // A backslash escapes the character after it, e.g. \" inside the name.
                    if (header.charAt(index) == '\\' && index + 1 < header.length()) index++;
                    value.append(header.charAt(index));
                }
                return value.toString();
            }

            int end = header.indexOf(';', index);
            return header.substring(index, end < 0 ? header.length() : end).trim();
        }
        return null;
    }

    private static int skipSpaces(String text, int index) {
        while (index < text.length() && (text.charAt(index) == ' ' || text.charAt(index) == '\t')) {
            index++;
        }
        return index;
    }

    /** Decodes %XX escapes as bytes of the charset; a malformed escape is kept as it is. */
    static String percentDecode(String value, Charset charset) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(value.length());

        int i = 0;
        while (i < value.length()) {
            if (value.charAt(i) == '%' && i + 2 < value.length()) {
                int high = Character.digit(value.charAt(i + 1), 16);
                int low = Character.digit(value.charAt(i + 2), 16);
                if (high >= 0 && low >= 0) {
                    bytes.write(high << 4 | low);
                    i += 3;
                    continue;
                }
            }

            // The characters up to the next '%', encoded together so surrogate pairs stay whole.
            int next = value.indexOf('%', i + 1);
            if (next < 0) next = value.length();
            byte[] raw = value.substring(i, next).getBytes(charset);
            bytes.write(raw, 0, raw.length);
            i = next;
        }

        return new String(bytes.toByteArray(), charset);
    }

    /**
     * Makes a name shared storage accepts, by Android's FAT rules (FileUtils.isValidFatFilenameChar):
     * '_' for control characters and " * / : < > ? \ |, and at most MAX_NAME_BYTES long.
     */
    static String sanitize(String fileName) {
        String name = fileName == null ? "" : fileName;

        StringBuilder cleaned = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            cleaned.append(c <= 0x1F || c == 0x7F || "\"*/:<>?\\|".indexOf(c) >= 0 ? '_' : c);
        }
        name = cleaned.toString().trim();

        if (utf8Length(name) > MAX_NAME_BYTES) {
            int dot = name.lastIndexOf('.');
            String extension = dot > 0 && name.length() - dot <= 10 ? name.substring(dot) : "";
            String base = name.substring(0, name.length() - extension.length());

            while (!base.isEmpty() && utf8Length(base + extension) > MAX_NAME_BYTES) {
                // Whole characters only, never half of a surrogate pair.
                base = base.substring(0, base.offsetByCodePoints(base.length(), -1));
            }
            name = base.trim() + extension;
        }

        // "." and ".." would point at the Downloads folder itself or above it.
        return name.isEmpty() || name.equals(".") || name.equals("..") ? "download" : name;
    }

    private static int utf8Length(String text) {
        return text.getBytes(StandardCharsets.UTF_8).length;
    }

    private static boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }
}
