package com.frezo.server.component;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Đọc TOÀN BỘ body ngay lúc wrap request → cache vào byte[] → có thể đọc đi đọc lại
 * không giới hạn số lần.
 *
 * <p>Khác biệt so với Spring's {@link org.springframework.web.util.ContentCachingRequestWrapper}:
 * <ul>
 *   <li>Spring wrapper chỉ cache body <b>khi</b> downstream code đọc stream. Nếu request
 *       bị chặn ở tầng khác (Spring Security auth fail, permission deny, exception trước
 *       khi tới controller {@code @RequestBody}) thì cache rỗng.</li>
 *   <li>Wrapper này đọc byte NGAY lúc constructor → luôn có body kể cả khi request abort
 *       giữa chừng, ideal cho use case audit log / API log.</li>
 * </ul>
 *
 * <p>Trade-off: tăng memory cho request lớn (multipart upload). Filter caller nên skip
 * wrap cho content-type {@code multipart/*} để tránh copy file upload nhiều lần.
 */
public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

    private final byte[] cachedBody;

    public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
        super(request);
        this.cachedBody = request.getInputStream().readAllBytes();
    }

    /** Trả về bytes đã cache — dùng cho logging body sau khi request đã xử lý xong. */
    public byte[] getCachedBody() {
        return cachedBody;
    }

    @Override
    public ServletInputStream getInputStream() {
        return new CachedServletInputStream(this.cachedBody);
    }

    @Override
    public BufferedReader getReader() {
        Charset cs = getCharset();
        return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(this.cachedBody), cs));
    }

    private Charset getCharset() {
        String enc = getCharacterEncoding();
        if (enc == null || enc.isBlank()) return StandardCharsets.UTF_8;
        try {
            return Charset.forName(enc);
        } catch (Exception e) {
            return StandardCharsets.UTF_8;
        }
    }

    /** ServletInputStream đọc từ ByteArrayInputStream — chuẩn Servlet 6.0 API. */
    private static class CachedServletInputStream extends ServletInputStream {
        private final ByteArrayInputStream buffer;

        CachedServletInputStream(byte[] contents) {
            this.buffer = new ByteArrayInputStream(contents);
        }

        @Override
        public int read() {
            return buffer.read();
        }

        @Override
        public boolean isFinished() {
            return buffer.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener listener) {
            throw new UnsupportedOperationException();
        }
    }
}
