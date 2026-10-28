package br.com.petshop.web.http;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Multipart {

    public static final class Part {
        private final String name;
        private final String fileName;
        private final String contentType;
        private final byte[] data;

        public Part(String name, String fileName, String contentType, byte[] data) {
            this.name = name;
            this.fileName = fileName;
            this.contentType = contentType;
            this.data = data;
        }

        public String name() {
            return name;
        }

        public String fileName() {
            return fileName;
        }

        public String contentType() {
            return contentType;
        }

        public byte[] data() {
            return data;
        }

        public InputStream inputStream() {
            return new ByteArrayInputStream(data);
        }

        public String asString() {
            return new String(data, StandardCharsets.UTF_8);
        }
    }

    private Multipart() {
    }

    public static List<Part> parse(byte[] body, String boundary) {
        List<Part> parts = new ArrayList<>();
        if (body == null || boundary == null || boundary.isBlank()) {
            return parts;
        }
        byte[] delimiter = ("--" + boundary).getBytes(StandardCharsets.UTF_8);
        byte[] endDelimiter = ("--" + boundary + "--").getBytes(StandardCharsets.UTF_8);
        int index = indexOf(body, delimiter, 0);
        while (index >= 0) {
            int start = index + delimiter.length;
            if (start + 1 < body.length && body[start] == '\r' && body[start + 1] == '\n') {
                start += 2;
            } else if (start < body.length && body[start] == '\n') {
                start += 1;
            }
            int next = indexOf(body, delimiter, start);
            if (next < 0) {
                break;
            }
            byte[] part = slice(body, start, next);
            boolean ultimaParte = endsWith(body, next, endDelimiter);
            Part p = parsePart(part);
            if (p != null) {
                parts.add(p);
            }
            if (ultimaParte) {
                break;
            }
            index = next;
        }
        return parts;
    }

    private static Part parsePart(byte[] part) {
        int headerEnd = indexOfSequence(part, new byte[]{'\r', '\n', '\r', '\n'});
        if (headerEnd < 0) {
            headerEnd = indexOfSequence(part, new byte[]{'\n', '\n'});
            if (headerEnd < 0) {
                return null;
            }
            byte[] headers = slice(part, 0, headerEnd);
            byte[] data = slice(part, headerEnd + 2, part.length);
            if (data.length >= 2 && data[data.length - 2] == '\r' && data[data.length - 1] == '\n') {
                data = slice(data, 0, data.length - 2);
            }
            return parseHeaders(headers, data);
        } else {
            byte[] headers = slice(part, 0, headerEnd);
            byte[] data = slice(part, headerEnd + 4, part.length);
            if (data.length >= 2 && data[data.length - 2] == '\r' && data[data.length - 1] == '\n') {
                data = slice(data, 0, data.length - 2);
            }
            return parseHeaders(headers, data);
        }
    }

    private static Part parseHeaders(byte[] headers, byte[] data) {
        String headerStr = new String(headers, StandardCharsets.UTF_8);
        String name = null;
        String fileName = null;
        String contentType = null;
        for (String line : headerStr.split("\r?\n")) {
            String lower = line.toLowerCase();
            if (lower.startsWith("content-disposition")) {
                name = extractParam(line, "name");
                fileName = extractParam(line, "filename");
            }
            if (lower.startsWith("content-type")) {
                int idx = line.indexOf(':');
                if (idx >= 0) {
                    contentType = line.substring(idx + 1).trim();
                }
            }
        }
        if (name == null) {
            return null;
        }
        return new Part(name, fileName, contentType, data);
    }

    private static String extractParam(String line, String key) {
        String lower = line.toLowerCase();
        String search = key.toLowerCase() + "=";
        int idx = lower.indexOf(search);
        if (idx < 0) {
            return null;
        }
        int start = idx + search.length();
        if (start < line.length() && line.charAt(start) == '"') {
            start++;
            int end = line.indexOf('"', start);
            if (end < 0) {
                return null;
            }
            return line.substring(start, end);
        } else {
            int end = line.indexOf(';', start);
            if (end < 0) {
                end = line.length();
            }
            return line.substring(start, end).trim();
        }
    }

    private static int indexOf(byte[] array, byte[] target, int from) {
        if (target.length == 0) {
            return from;
        }
        for (int i = from; i <= array.length - target.length; i++) {
            boolean match = true;
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                return i;
            }
        }
        return -1;
    }

    private static int indexOfSequence(byte[] array, byte[] target) {
        return indexOf(array, target, 0);
    }

    private static byte[] slice(byte[] array, int start, int end) {
        if (start >= end || start < 0) {
            return new byte[0];
        }
        if (end > array.length) {
            end = array.length;
        }
        byte[] result = new byte[end - start];
        System.arraycopy(array, start, result, 0, end - start);
        return result;
    }

    private static boolean endsWith(byte[] array, int index, byte[] target) {
        if (index + target.length > array.length) {
            return false;
        }
        for (int i = 0; i < target.length; i++) {
            if (array[index + i] != target[i]) {
                return false;
            }
        }
        return true;
    }

    public static Map<String, Part> toMap(List<Part> parts) {
        Map<String, Part> map = new HashMap<>();
        if (parts == null) {
            return map;
        }
        for (Part p : parts) {
            map.put(p.name(), p);
        }
        return map;
    }
}
